package com.calendarioazulpixel.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import com.calendarioazulpixel.util.AlarmScheduler

class AlarmRingService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var autoStopHandler: Handler? = null
    private var autoStopRunnable: Runnable? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val category = intent?.getStringExtra("ALARM_CATEGORY") ?: "general"
        val dateStr = intent?.getStringExtra("ALARM_DATE") ?: ""

        if (action == "STOP_ALARM" || action == "STOP_REPEAT") {
            if (dateStr.isNotEmpty()) {
                AlarmScheduler.cancelAlarm(this, category, "${dateStr}_repeat")
            }
            stopAlarm()
            return START_NOT_STICKY
        }

        val soundUri = intent?.getStringExtra("SOUND_URI") ?: ""
        val vibrate = intent?.getBooleanExtra("VIBRATE", true) ?: true
        val durationMinutes = intent?.getIntExtra("DURATION_MINUTES", 5) ?: 5

        createNotificationChannel()

        val stopIntent = Intent(this, AlarmRingService::class.java).apply {
            this.action = "STOP_ALARM"
            putExtra("ALARM_CATEGORY", category)
            putExtra("ALARM_DATE", dateStr)
        }
        val stopPendingIntent = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val stopRepeatIntent = Intent(this, AlarmRingService::class.java).apply {
            this.action = "STOP_REPEAT"
            putExtra("ALARM_CATEGORY", category)
            putExtra("ALARM_DATE", dateStr)
        }
        val stopRepeatPendingIntent = PendingIntent.getService(this, 2, stopRepeatIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationName = when (category) {
            "dia" -> "Alarma Turno Día"
            "noche" -> "Alarma Turno Noche"
            else -> "Alarma General"
        }

        val notification = NotificationCompat.Builder(this, "ALARM_CHANNEL")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(notificationName)
            .setContentText("¡Es hora de tu turno!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Apagar", stopPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Detener repetición", stopRepeatPendingIntent)
            .setOngoing(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1998, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(1998, notification)
            }
        } catch (e: Exception) {
            startForeground(1998, notification)
        }

        // Programar auto-stop por duración (si durationMinutes > 0)
        if (durationMinutes > 0) {
            autoStopHandler = Handler(Looper.getMainLooper())
            autoStopRunnable = Runnable {
                if (dateStr.isNotEmpty()) {
                    AlarmScheduler.cancelAlarm(this, category, "${dateStr}_repeat")
                }
                stopAlarm()
            }
            autoStopHandler?.postDelayed(autoStopRunnable!!, durationMinutes * 60 * 1000L)
        }

        if (soundUri.isNotEmpty()) {
            try {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(this@AlarmRingService, Uri.parse(soundUri))
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (e: Exception) { Log.e("AlarmRingService", "Error reproduciendo sonido", e) }
        }

        if (vibrate) {
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            val pattern = longArrayOf(0, 1000, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                vibrator?.vibrate(pattern, 0)
            }
        }

        return START_STICKY
    }

    private fun stopAlarm() {
        autoStopRunnable?.let { autoStopHandler?.removeCallbacks(it) }
        autoStopHandler = null
        autoStopRunnable = null

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {}
        mediaPlayer = null
        
        try {
            vibrator?.cancel()
        } catch(e:Exception){}
        vibrator = null
        
        stopForeground(true)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "ALARM_CHANNEL",
                "Alarmas de Turnos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de alarmas activas"
                setSound(null, null) 
                enableVibration(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
    }
}
