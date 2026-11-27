package com.calendarioazulpixel.util

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Clase auxiliar encargada exclusivamente de evaluar el estado de los permisos de Alarmas Exactas
 * (introducidos en Android 12) y proveer el Intent para solicitarlos si es necesario.
 * Se justifica su creación para mantener limpia la UI y centralizar el manejo de API levels.
 */
object AlarmPermissionHelper {

    /**
     * Comprueba si la aplicación tiene permiso para programar alarmas exactas.
     * En Android 11 o inferior, siempre devuelve true.
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = ContextCompat.getSystemService(context, AlarmManager::class.java)
            alarmManager?.canScheduleExactAlarms() ?: false
        } else {
            true
        }
    }

    /**
     * Proporciona el Intent que el usuario debe seguir para otorgar el permiso de alarmas exactas.
     * Este intent se usará en la interfaz gráfica (UI) sólo cuando el usuario decida encender una alarma
     * y se detecte que no tiene el permiso.
     */
    fun createExactAlarmSettingsIntent(context: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
            }
        } else {
            null
        }
    }
}