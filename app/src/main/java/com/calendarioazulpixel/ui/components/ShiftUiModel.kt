package com.calendarioazulpixel.ui.components

import androidx.compose.ui.graphics.Color
import com.calendarioazulpixel.domain.usecase.Compania
import com.calendarioazulpixel.domain.usecase.ShiftCalculator
import java.util.Calendar
import com.calendarioazulpixel.domain.usecase.TipoTurno
import com.calendarioazulpixel.domain.usecase.TurnoType
import com.calendarioazulpixel.ui.theme.*

// Clase de datos que encapsula toda la UI de un turno específico para no duplicar código
data class ShiftUiData(
    val title: String, 
    val icon: String, 
    val bgColor: Color, 
    val textColor: Color, 
    val shortHours: String, 
    val longHours: String,
    val entradaHora: String = "",
    val salidaHora: String = "",
    val entradaFecha: String = "",
    val salidaFecha: String = "",
    val isLibre: Boolean = false,
    val subturnoPrefix: String = "" // Añadido para guardar información de subturno
)

// Helper para saber si una hora es menor que otra (cruza la medianoche)
fun parseTimeToMinutes(timeStr: String): Int {
    val isPm = timeStr.contains("PM", ignoreCase = true)
    val parts = timeStr.replace(Regex("[^0-9:]"), "").split(":")
    var h = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    if (isPm && h < 12) h += 12
    if (!isPm && h == 12) h = 0
    return h * 60 + m
}

// Función centralizada que usan Inicio y Calendario para dibujar los turnos
fun getShiftUiData(
    timeInMillis: Long, 
    tipoTurno: TipoTurno,
    compania: Compania,
    dEntrada: String, dSalida: String, nEntrada: String, nSalida: String,
    entrada24: String = "6:00 AM", salida24: String = "6:00 AM",
    entrada10: String = "7:00 AM", salida10: String = "7:00 PM",
    subDia: String = "2", subNoche: String = "4",
    refDia: Long = 0L, refNoche: Long = 0L,
    sub24: String = "2/4", ref24: Long = 0L
): ShiftUiData {
    
    val cal = java.util.Calendar.getInstance().apply { this.timeInMillis = timeInMillis }
    val dateFormater = java.text.SimpleDateFormat("EEE., d MMM.", java.util.Locale("es", "ES"))
    val fechaEntrada = dateFormater.format(cal.time).replaceFirstChar { it.uppercase() }
    
    return when (tipoTurno) {
        TipoTurno.DOCE_VEINTICUATRO -> {
            val turno = ShiftCalculator.calcularTurno(timeInMillis, compania)
            when (turno) {
                TurnoType.DIA -> {
                    val crossesMidnight = parseTimeToMinutes(dSalida) <= parseTimeToMinutes(dEntrada)
                    val calSalida = cal.clone() as java.util.Calendar
                    if (crossesMidnight) calSalida.add(java.util.Calendar.DAY_OF_YEAR, 1)
                    val fechaSalida = dateFormater.format(calSalida.time).replaceFirstChar { it.uppercase() }
                    
                    val sub = ShiftCalculator.calcularSubturno(timeInMillis, TurnoType.DIA, subDia, refDia, subNoche, refNoche, compania)
                    val subTxt = if (sub.isNotEmpty()) "$sub.º TURNO " else ""
                    
                    ShiftUiData(
                        title = "DÍA", 
                        icon = "☀️", 
                        bgColor = DayShiftBg, 
                        textColor = DayShiftText, 
                        shortHours = "$dEntrada\n$dSalida", 
                        longHours = "$dEntrada - $dSalida",
                        entradaHora = dEntrada,
                        salidaHora = dSalida,
                        entradaFecha = fechaEntrada,
                        salidaFecha = fechaSalida,
                        isLibre = false,
                        subturnoPrefix = subTxt
                    )
                }
                TurnoType.NOCHE -> {
                    val crossesMidnight = parseTimeToMinutes(nSalida) <= parseTimeToMinutes(nEntrada)
                    val calSalida = cal.clone() as java.util.Calendar
                    if (crossesMidnight) calSalida.add(java.util.Calendar.DAY_OF_YEAR, 1)
                    val fechaSalida = dateFormater.format(calSalida.time).replaceFirstChar { it.uppercase() }
                    
                    val sub = ShiftCalculator.calcularSubturno(timeInMillis, TurnoType.NOCHE, subDia, refDia, subNoche, refNoche, compania)
                    val prefixStr = if (sub == "1") "1.er" else "$sub.º"
                    val subTxt = if (sub.isNotEmpty()) "$prefixStr TURNO " else ""
                    
                    // Lógica normal para la etiqueta "HOY me toca Noche"
                    ShiftUiData(
                        title = "NOCHE", 
                        icon = "🌙", 
                        bgColor = NightShiftBg1, 
                        textColor = Color.White, 
                        shortHours = "$nEntrada\n$nSalida", 
                        longHours = "$nEntrada  →  $nSalida",
                        entradaHora = nEntrada,
                        salidaHora = nSalida,
                        entradaFecha = fechaEntrada,
                        salidaFecha = fechaSalida,
                        isLibre = false,
                        subturnoPrefix = subTxt
                    )
                }
                else -> {
                    // Si el cálculo da LIBRE, comprobamos si estamos en la franja de 00:00 a 05:59 a.m. 
                    // del día inmediatamente posterior a haber tenido un turno de NOCHE.
                    val ayerMillis = cal.timeInMillis - (1000 * 60 * 60 * 24)
                    val turnoAyer = ShiftCalculator.calcularTurno(ayerMillis, compania)
                    
                    val horaActual = Calendar.getInstance().apply { this.timeInMillis = System.currentTimeMillis() }.get(Calendar.HOUR_OF_DAY)
                    
                    // Aseguramos que solo aplique si estamos viendo el día actual (no un mes en el futuro/pasado)
                    val isToday = Calendar.getInstance().let { hoy ->
                        hoy.get(Calendar.YEAR) == cal.get(Calendar.YEAR) && hoy.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
                    }

                    if (isToday && turnoAyer == TurnoType.NOCHE && horaActual < 6) {
                        // El guardián está saliendo del turno de Noche (Primer turno del día de 12:00 a 6:00)
                        ShiftUiData(
                            title = "PRIMER TURNO – EN SERVICIO", 
                            icon = "🌙", 
                            bgColor = NightShiftBg1, 
                            textColor = Color.White, 
                            shortHours = "12:00 AM\n6:00 AM", 
                            longHours = "12:00 AM  →  6:00 AM",
                            entradaHora = "12:00 AM",
                            salidaHora = "6:00 AM",
                            entradaFecha = fechaEntrada,
                            salidaFecha = fechaEntrada,
                            isLibre = false,
                            subturnoPrefix = ""
                        )
                    } else {
                        ShiftUiData(
                            title = "LIBRE", 
                            icon = "🧍", 
                            bgColor = FreeShiftBg, 
                            textColor = FreeShiftText, 
                            shortHours = "Sin turno\n", 
                            longHours = "Día de descanso",
                            entradaHora = "",
                            salidaHora = "",
                            entradaFecha = "",
                            salidaFecha = "",
                            isLibre = true
                        )
                    }
                }
            }
        }
        TipoTurno.VEINTICUATRO_CUARENTA_OCHO -> {
            val turno = ShiftCalculator.calcularTurno24x48(timeInMillis, compania)
            when (turno) {
                TurnoType.TRABAJO -> {
                    val crossesMidnight = parseTimeToMinutes(salida24) <= parseTimeToMinutes(entrada24)
                    val calSalida = cal.clone() as java.util.Calendar
                    if (crossesMidnight || entrada24 == salida24) calSalida.add(java.util.Calendar.DAY_OF_YEAR, 1)
                    val fechaSalida = dateFormater.format(calSalida.time).replaceFirstChar { it.uppercase() }
                    
                    val sub = ShiftCalculator.calcularSubturno24(timeInMillis, sub24, ref24, compania)
                    val subTxt = if (sub.isNotEmpty()) "☀️🌙 $sub Turno" else ""
                    
                    ShiftUiData(
                        title = "LABORAL 24H", 
                        icon = "💼", 
                        bgColor = Trabajo24Bg1, 
                        textColor = Trabajo24Text, 
                        shortHours = "$entrada24\n$salida24", 
                        longHours = "$entrada24 - $salida24",
                        entradaHora = entrada24,
                        salidaHora = salida24,
                        entradaFecha = fechaEntrada,
                        salidaFecha = fechaSalida,
                        isLibre = false,
                        subturnoPrefix = subTxt
                    )
                }
                else -> ShiftUiData(
                    title = "LIBRE", 
                    icon = "🧍", 
                    bgColor = Libre24Bg, 
                    textColor = Libre24Text, 
                    shortHours = "Sin turno\n", 
                    longHours = "Día de descanso",
                    entradaHora = "",
                    salidaHora = "",
                    entradaFecha = "",
                    salidaFecha = "",
                    isLibre = true
                )
            }
        }
        TipoTurno.DIEZ_CINCO -> {
            val turno = ShiftCalculator.calcularTurno10x5(timeInMillis, compania)
            when (turno) {
                TurnoType.TRABAJO -> {
                    val crossesMidnight = parseTimeToMinutes(salida10) <= parseTimeToMinutes(entrada10)
                    val calSalida = cal.clone() as java.util.Calendar
                    if (crossesMidnight && entrada10 != salida10) calSalida.add(java.util.Calendar.DAY_OF_YEAR, 1)
                    val fechaSalida = dateFormater.format(calSalida.time).replaceFirstChar { it.uppercase() }
                    
                    ShiftUiData(
                        title = "DÍA LABORAL", 
                        icon = "💼", 
                        bgColor = Trabajo24Bg1, 
                        textColor = Trabajo24Text, 
                        shortHours = "$entrada10\n$salida10", 
                        longHours = "$entrada10 - $salida10",
                        entradaHora = entrada10,
                        salidaHora = salida10,
                        entradaFecha = fechaEntrada,
                        salidaFecha = fechaSalida,
                        isLibre = false
                    )
                }
                else -> ShiftUiData(
                    title = "DESCANSO", 
                    icon = "🧍", 
                    bgColor = Libre24Bg, 
                    textColor = Libre24Text, 
                    shortHours = "Sin turno\n", 
                    longHours = "Día de descanso",
                    entradaHora = "",
                    salidaHora = "",
                    entradaFecha = "",
                    salidaFecha = "",
                    isLibre = true
                )
            }
        }
    }
}
