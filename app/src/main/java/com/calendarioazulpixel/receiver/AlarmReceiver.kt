package com.calendarioazulpixel.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.calendarioazulpixel.data.local.PreferencesRepository
import com.calendarioazulpixel.util.AlarmScheduler
import com.calendarioazulpixel.util.AlarmSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Receptor diseñado para atrapar los PendingIntent disparados por AlarmManager.
 * Ejecuta la alarma real e invoca a AlarmSyncManager para programar la siguiente.
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val category = intent.getStringExtra("ALARM_CATEGORY") ?: "general"
        val dateStr = intent.getStringExtra("ALARM_DATE") ?: "general_date"
        Log.d("ALARM_SYSTEM", "Alarma recibida: $category - $dateStr")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = PreferencesRepository(context)
                val config = repository.getAlarmConfig(category).first()

                // Si está configurada la repetición y no es un evento de repetición recurrente ya disparado
                if (config.repeatInterval > 0 && !dateStr.endsWith("_repeat")) {
                    val repeatTriggerTime = System.currentTimeMillis() + config.repeatInterval * 60 * 1000L
                    AlarmScheduler.scheduleExactAlarm(context, category, "${dateStr}_repeat", repeatTriggerTime)
                }

                // Iniciar Servicio de Alarma
                val serviceIntent = Intent(context, AlarmRingService::class.java).apply {
                    putExtra("ALARM_CATEGORY", category)
                    putExtra("ALARM_DATE", dateStr)
                    putExtra("SOUND_URI", config.soundUri)
                    putExtra("VIBRATE", config.vibrate)
                    putExtra("DURATION_MINUTES", config.durationMinutes)
                    putExtra("REPEAT_INTERVAL", config.repeatInterval)
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }

                // Reprogramar automáticamente la SIGUIENTE alarma de todo el sistema (solo si no es repetición)
                if (!dateStr.endsWith("_repeat")) {
                    AlarmSyncManager.syncCurrentState(context)
                }
            } catch (e: Exception) {
                Log.e("ALARM_SYSTEM", "Error procesando alarma", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
