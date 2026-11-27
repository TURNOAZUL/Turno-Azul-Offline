package com.calendarioazulpixel.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.calendarioazulpixel.util.AlarmSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receptor diseñado para atrapar el evento ACTION_BOOT_COMPLETED de Android.
 * Reprograma las alarmas tras reiniciar el teléfono.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("ALARM_SYSTEM", "Dispositivo reiniciado. BootReceiver activado.")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    AlarmSyncManager.syncCurrentState(context)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}