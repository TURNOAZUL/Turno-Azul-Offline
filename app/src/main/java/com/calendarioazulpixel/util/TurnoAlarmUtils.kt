package com.calendarioazulpixel.util

import android.util.Log
import com.calendarioazulpixel.domain.model.FutureAlarmDates
import com.calendarioazulpixel.domain.usecase.Compania
import com.calendarioazulpixel.domain.usecase.TipoTurno
import com.calendarioazulpixel.ui.components.getShiftUiData
import java.util.Calendar

object TurnoAlarmUtils {

    /**
     * Calcula las fechas en las que deben sonar las alarmas en los próximos 30 días,
     * basado estrictamente en el motor matemático existente del calendario de turnos.
     */
    fun calculateFutureAlarms(
        tipoTurno: TipoTurno,
        compania: Compania,
        dEntrada: String, dSalida: String, nEntrada: String, nSalida: String,
        entrada24: String, salida24: String,
        entrada10: String, salida10: String,
        subDia: String, subNoche: String,
        refDia: Long, refNoche: Long,
        sub24: String, ref24: Long
    ): FutureAlarmDates {
        // En esta Etapa 2.1, sólo evaluamos el sistema 12/24 según las reglas.
        if (tipoTurno != TipoTurno.DOCE_VEINTICUATRO) {
            Log.d("ALARM_DEBUG", "Cálculo omitido: La configuración actual no es 12/24.")
            return FutureAlarmDates(isApplicable = false, emptyList(), emptyList(), emptyList())
        }

        val generalAlarms = mutableListOf<String>()
        val diaAlarms = mutableListOf<String>()
        val nocheAlarms = mutableListOf<String>()

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val formatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("es", "ES"))

        Log.d("ALARM_DEBUG", "Iniciando cálculo lógico de alarmas para los próximos 30 días...")
        
        for (i in 0 until 30) {
            val evalTime = calendar.timeInMillis
            val dateStr = formatter.format(calendar.time)

            // Usamos el motor oficial sin reescribir la fórmula.
            val uiData = getShiftUiData(
                timeInMillis = evalTime,
                tipoTurno = tipoTurno,
                compania = compania,
                dEntrada = dEntrada, dSalida = dSalida, nEntrada = nEntrada, nSalida = nSalida,
                entrada24 = entrada24, salida24 = salida24,
                entrada10 = entrada10, salida10 = salida10,
                subDia = subDia, subNoche = subNoche,
                refDia = refDia, refNoche = refNoche,
                sub24 = sub24, ref24 = ref24
            )

            // La alarma General suena todos los días
            generalAlarms.add(dateStr)

            // Analizamos el texto mágico que produce la máquina original.
            // Para el Día 1: el prefijo dice "1.er TURNO "
            // Para el Día 2: el prefijo dice "2.º TURNO "
            // Para el Día 3: el prefijo dice "3.º TURNO "
            // Para el Día 4: el prefijo dice "4.º TURNO "
            val prefix = uiData.subturnoPrefix

            if (prefix.contains("2.º") || prefix.contains("3.º")) {
                // Segundo o Tercer Turno -> Alarma Turno Día
                diaAlarms.add(dateStr)
            } else if (prefix.contains("4.º") || prefix.contains("1.er")) {
                // Cuarto o Primer Turno -> Alarma Turno Noche
                nocheAlarms.add(dateStr)
            }

            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // --- Impresión de las pruebas en Logcat ---
        Log.d("ALARM_DEBUG", "--------------------------------------------------")
        Log.d("ALARM_DEBUG", "ALARMAS GENERAL (Próximos 5 días de muestra):")
        generalAlarms.take(5).forEach { Log.d("ALARM_DEBUG", " - $it") }
        
        Log.d("ALARM_DEBUG", "--------------------------------------------------")
        Log.d("ALARM_DEBUG", "ALARMAS TURNO DÍA (2.º y 3.º):")
        diaAlarms.take(15).forEach { Log.d("ALARM_DEBUG", " - $it") }

        Log.d("ALARM_DEBUG", "--------------------------------------------------")
        Log.d("ALARM_DEBUG", "ALARMAS TURNO NOCHE (4.º y 1.º):")
        nocheAlarms.take(15).forEach { Log.d("ALARM_DEBUG", " - $it") }
        Log.d("ALARM_DEBUG", "--------------------------------------------------")
        
        return FutureAlarmDates(
            isApplicable = true,
            generalAlarms = generalAlarms,
            diaAlarms = diaAlarms,
            nocheAlarms = nocheAlarms
        )
    }
}