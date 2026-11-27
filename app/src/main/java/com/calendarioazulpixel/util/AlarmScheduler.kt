package com.calendarioazulpixel.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.calendarioazulpixel.receiver.AlarmReceiver

enum class ScheduleResult {
    SCHEDULED,
    PERMISSION_REQUIRED,
    INVALID_TRIGGER_TIME,
    ERROR
}

object AlarmScheduler {

    /**
     * Genera un identificador único y determinista para cada alarma basado en su categoría y fecha.
     * Ejemplo: "dia" y "25/12/2023" -> Convierte la fecha a 20231225 y le suma el offset 200,000,000.
     * Resultado = 220231225.
     * Esto evita colisiones, garantiza que la misma alarma tenga siempre el mismo ID
     * y facilita su cancelación o reemplazo sin barridos masivos.
     */
    fun generateRequestId(category: String, dateStr: String): Int {
        try {
            val parts = dateStr.split("/")
            if (parts.size == 3) {
                val d = parts[0].padStart(2, '0')
                val m = parts[1].padStart(2, '0')
                val y = parts[2]
                val dateInt = "$y$m$d".toIntOrNull() ?: 0

                val categoryOffset = when (category) {
                    "general" -> 100000000
                    "dia"     -> 200000000
                    "noche"   -> 300000000
                    else      -> 900000000
                }
                return categoryOffset + dateInt
            }
        } catch (e: Exception) {
            Log.e("AlarmScheduler", "Error generando ID", e)
        }
        return (category + dateStr).hashCode()
    }

    /**
     * Construye un PendingIntent idéntico. Es vital para cancelar o actualizar alarmas previamente programadas.
     * Utiliza FLAG_IMMUTABLE por seguridad (obligatorio desde Android 12).
     */
    private fun createPendingIntent(context: Context, category: String, dateStr: String, flags: Int): PendingIntent? {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_CATEGORY", category)
            putExtra("ALARM_DATE", dateStr)
        }
        val reqCode = generateRequestId(category, dateStr)
        return PendingIntent.getBroadcast(context, reqCode, intent, flags)
    }

    /**
     * Programa una alarma exacta. Evalúa los permisos primero.
     * Utiliza FLAG_UPDATE_CURRENT para evitar duplicados reemplazando la anterior si ya existía.
     */
    fun scheduleExactAlarm(context: Context, category: String, dateStr: String, triggerAtMillis: Long): ScheduleResult {
        // 1. Validar que la hora no esté en el pasado ni sea el instante exacto actual
        val currentTime = System.currentTimeMillis()
        if (triggerAtMillis <= currentTime) {
            Log.w("AlarmScheduler", "Hora de alarma inválida (pasada o igual a la actual): $triggerAtMillis <= $currentTime")
            return ScheduleResult.INVALID_TRIGGER_TIME
        }

        if (!AlarmPermissionHelper.canScheduleExactAlarms(context)) {
            Log.w("AlarmScheduler", "Permiso de alarma exacta no concedido.")
            return ScheduleResult.PERMISSION_REQUIRED
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return ScheduleResult.ERROR

        val pendingIntent = createPendingIntent(
            context = context,
            category = category,
            dateStr = dateStr,
            flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ) ?: return ScheduleResult.ERROR

        return try {
            // Usa RTC_WAKEUP para encender la pantalla/procesador incluso si el dispositivo duerme.
            // AllowWhileIdle permite saltarse el Modo Doze (ahorro de batería agresivo).
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
            Log.d("AlarmScheduler", "Alarma programada exitosamente: $category - $dateStr")
            ScheduleResult.SCHEDULED
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "Falta permiso de alarma exacta capturado por SecurityException", e)
            ScheduleResult.PERMISSION_REQUIRED
        } catch (e: Exception) {
            Log.e("AlarmScheduler", "Error inesperado al programar alarma", e)
            ScheduleResult.ERROR
        }
    }

    /**
     * Cancela una alarma específica apuntando exactamente a su ID determinista,
     * sin hacer barridos ni eliminar alarmas ajenas.
     */
    fun cancelAlarm(context: Context, category: String, dateStr: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        
        val pendingIntent = createPendingIntent(
            context = context,
            category = category,
            dateStr = dateStr,
            // FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE para capturar el PendingIntent existente.
            flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel() // Liberar el recurso del sistema operativo
            Log.d("AlarmScheduler", "Alarma cancelada: $category - $dateStr")
        }
    }
}