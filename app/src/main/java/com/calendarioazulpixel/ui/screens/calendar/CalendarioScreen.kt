package com.calendarioazulpixel.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calendarioazulpixel.data.local.PreferencesRepository
import com.calendarioazulpixel.domain.usecase.Compania
import com.calendarioazulpixel.domain.usecase.TipoTurno
import com.calendarioazulpixel.ui.components.ShiftUiData
import com.calendarioazulpixel.ui.components.getShiftUiData
import com.calendarioazulpixel.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class CalendarDay(
    val timeInMillis: Long,
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean
)

fun getCalendarDays(displayMonth: Calendar): List<CalendarDay> {
    val days = mutableListOf<CalendarDay>()
    val cal = displayMonth.clone() as Calendar
    cal.set(Calendar.DAY_OF_MONTH, 1)
    
    // Obtener día de la semana para el 1er día del mes (1 = Domingo, 2 = Lunes...)
    // Ajustamos para que la semana empiece en Lunes
    var firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY
    if (firstDayOfWeek < 0) firstDayOfWeek += 7
    
    // Retroceder al Lunes anterior
    cal.add(Calendar.DAY_OF_MONTH, -firstDayOfWeek)
    
    val todayCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    
    val currentMonthInt = displayMonth.get(Calendar.MONTH)
    
    for (i in 0 until 42) { // 6 semanas * 7 días = 42 días en la cuadrícula
        val isCurrentMonth = cal.get(Calendar.MONTH) == currentMonthInt
        val isToday = cal.timeInMillis == todayCal.timeInMillis
        days.add(
            CalendarDay(
                timeInMillis = cal.timeInMillis,
                dayOfMonth = cal.get(Calendar.DAY_OF_MONTH),
                isCurrentMonth = isCurrentMonth,
                isToday = isToday
            )
        )
        cal.add(Calendar.DAY_OF_MONTH, 1)
    }
    return days
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarioScreen() {
    val context = LocalContext.current
    val repository = remember { PreferencesRepository(context) }
    
    val tipoTurnoActivoStr by repository.tipoTurnoActivo.collectAsState(initial = "DOCE_VEINTICUATRO")
    val tipoTurno = TipoTurno.valueOf(tipoTurnoActivoStr)

    // Leer configuración 12Hx24H
    val compania12Str by repository.companiaSeleccionada.collectAsState(initial = "AMARILLA")
    val compania12 = Compania.valueOf(compania12Str)
    val dEntrada by repository.diaEntrada.collectAsState(initial = "6:00 AM")
    val dSalida by repository.diaSalida.collectAsState(initial = "6:00 PM")
    val nEntrada by repository.nocheEntrada.collectAsState(initial = "6:00 PM")
    val nSalida by repository.nocheSalida.collectAsState(initial = "6:00 AM")

    val subDia by repository.subturnoDia.collectAsState(initial = "2")
    val subNoche by repository.subturnoNoche.collectAsState(initial = "4")
    val refDia by repository.fechaRefSubturnoDia.collectAsState(initial = 0L)
    val refNoche by repository.fechaRefSubturnoNoche.collectAsState(initial = 0L)

    // Leer configuración 24Hx48H
    val compania24Str by repository.compania24Seleccionada.collectAsState(initial = "AZUL")
    val compania24 = Compania.valueOf(compania24Str)
    val entrada24 by repository.entrada24.collectAsState(initial = "6:00 AM")
    val salida24 by repository.salida24.collectAsState(initial = "6:00 AM")
    
    val sub24 by repository.subturno24.collectAsState(initial = "2/4")
    val ref24 by repository.fechaRefSubturno24.collectAsState(initial = 0L)
    
    // Leer configuración 10Dx5D
    val compania10Str by repository.compania10Seleccionada.collectAsState(initial = "ORION")
    val compania10 = Compania.valueOf(compania10Str)
    val entrada10 by repository.entrada10.collectAsState(initial = "7:00 AM")
    val salida10 by repository.salida10.collectAsState(initial = "7:00 PM")
    
    val compania = when (tipoTurno) {
        TipoTurno.DOCE_VEINTICUATRO -> compania12
        TipoTurno.VEINTICUATRO_CUARENTA_OCHO -> compania24
        TipoTurno.DIEZ_CINCO -> compania10
    }

    // Estado del mes actual a mostrar
    var currentDisplayMonth by remember { 
        mutableStateOf(Calendar.getInstance().apply { 
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_MONTH, 1) 
        }) 
    }
    
    // Estado del día seleccionado
    var selectedDateMillis by remember { 
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis)
    }

    val daysList = remember(currentDisplayMonth.timeInMillis) { getCalendarDays(currentDisplayMonth) }

    val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
    val selectedDateFormat = SimpleDateFormat("EEE, d MMM 'de' yyyy", Locale("es", "ES"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi calendario", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BluePrimary)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Header del calendario (Mes + Controles)
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { 
                        currentDisplayMonth = (currentDisplayMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) } 
                    },
                    modifier = Modifier.background(BlueSecondary, RoundedCornerShape(8.dp)).size(36.dp)
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior", tint = Color.White)
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = monthYearFormat.format(currentDisplayMonth.time).replaceFirstChar { it.uppercase() },
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = BluePrimary
                    )
                    Text("Calendario de mis turnos", fontSize = 12.sp, color = TextSecondary)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            val today = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            currentDisplayMonth = (today.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
                            selectedDateMillis = today.timeInMillis
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BlueSecondary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hoy", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { 
                            currentDisplayMonth = (currentDisplayMonth.clone() as Calendar).apply { add(Calendar.MONTH, 1) } 
                        },
                        modifier = Modifier.background(BlueSecondary, RoundedCornerShape(8.dp)).size(36.dp)
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente", tint = Color.White)
                    }
                }
            }

            // Grid del calendario
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Días de la semana
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        listOf("L", "M", "X", "J", "V", "S", "D").forEach { dayName ->
                            Text(
                                text = dayName, 
                                fontWeight = FontWeight.Bold, 
                                color = TextSecondary,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Días
                    daysList.chunked(7).forEach { week ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            week.forEach { day ->
                                val uiData = getShiftUiData(
                                    day.timeInMillis, tipoTurno, compania, 
                                    dEntrada, dSalida, nEntrada, nSalida, 
                                    entrada24, salida24,
                                    entrada10, salida10,
                                    subDia, subNoche, refDia, refNoche,
                                    sub24, ref24
                                )
                                
                                CalendarDayCell(
                                    day = day,
                                    uiData = uiData,
                                    isSelected = day.timeInMillis == selectedDateMillis,
                                    onClick = { selectedDateMillis = day.timeInMillis },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Tarjeta del día seleccionado
            val selectedUiData = getShiftUiData(
                selectedDateMillis, tipoTurno, compania, 
                dEntrada, dSalida, nEntrada, nSalida, 
                entrada24, salida24,
                entrada10, salida10,
                subDia, subNoche, refDia, refNoche,
                sub24, ref24
            )
            val selectedDateStr = selectedDateFormat.format(selectedDateMillis).replaceFirstChar { it.uppercase() }
            
            SelectedDayCard(
                uiData = selectedUiData, 
                dateString = selectedDateStr, 
                compania = compania,
                tipoTurno = tipoTurno
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun CalendarDayCell(
    day: CalendarDay,
    uiData: ShiftUiData,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = uiData.title.contains("NOCHE") || uiData.title.contains("LABORAL 24H")
    val alpha = if (day.isCurrentMonth) 1f else 0.3f
    
    val bgModifier = if (isDark) {
        val colors = if (uiData.title.contains("NOCHE")) listOf(NightShiftBg1, NightShiftBg2, NightShiftBg3)
                     else listOf(Trabajo24Bg1, Trabajo24Bg3)
        Modifier.background(Brush.horizontalGradient(colors), RoundedCornerShape(8.dp))
    } else {
        Modifier.background(uiData.bgColor, RoundedCornerShape(8.dp))
    }
    
    val textColor = if (isDark) Color.White else uiData.textColor
    
    // Doble borde para selección similar a la referencia
    val selectionModifier = if (isSelected) {
        Modifier
            .border(2.dp, BluePrimary, RoundedCornerShape(10.dp))
            .padding(2.dp) // espacio interior blanco
            .border(1.dp, Color.White, RoundedCornerShape(8.dp))
    } else {
        Modifier.padding(3.dp) // Mismo espaciado para que no salte la cuadrícula
    }

    Box(
        modifier = modifier
            .padding(2.dp)
            .aspectRatio(0.85f)
            .then(selectionModifier)
            .clip(RoundedCornerShape(8.dp))
            .then(bgModifier)
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Text(
            text = day.dayOfMonth.toString(),
            color = textColor.copy(alpha = alpha),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 2.dp, top = 2.dp)
        )
        Text(
            text = uiData.icon,
            fontSize = 18.sp,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 2.dp, bottom = 2.dp).alpha(alpha)
        )
    }
}

@Composable
fun SelectedDayCard(
    uiData: ShiftUiData,
    dateString: String,
    compania: Compania,
    tipoTurno: TipoTurno
) {
    val isDark = uiData.title.contains("NOCHE") || uiData.title.contains("LABORAL 24H") || uiData.title.contains("PRIMER TURNO")
    val bgGradient = if (isDark) {
        val colors = when {
            uiData.title.contains("NOCHE") -> listOf(NightShiftBg1, NightShiftBg2, NightShiftBg3)
            uiData.title.contains("LABORAL 24H") -> listOf(Trabajo24Bg1, Trabajo24Bg3)
            uiData.title.contains("PRIMER TURNO") -> listOf(Color(0xFF5B9BE8), BlueSecondary)
            else -> listOf(uiData.bgColor, uiData.bgColor)
        }
        Brush.horizontalGradient(colors)
    } else {
        Brush.horizontalGradient(listOf(uiData.bgColor, uiData.bgColor))
    }

    val textColor = if (isDark) Color.White else uiData.textColor
    val compNombre = "Compañía " + compania.name.lowercase().replaceFirstChar { it.uppercase() }
    
    val turnoNombreStr = when (tipoTurno) {
        TipoTurno.DOCE_VEINTICUATRO -> "12H × 24H"
        TipoTurno.VEINTICUATRO_CUARENTA_OCHO -> "24H × 48H"
        TipoTurno.DIEZ_CINCO -> "10D × 5D"
    }
    
    Card(
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().background(bgGradient).padding(20.dp)) {
            Column(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                Row(modifier = Modifier.fillMaxWidth().wrapContentHeight(), verticalAlignment = Alignment.Top) {
                    Box(modifier = Modifier.padding(top = 4.dp).size(60.dp).clip(CircleShape).background(Color.White.copy(alpha = if (isDark) 0.3f else 1f)), contentAlignment = Alignment.Center) {
                        Text(uiData.icon, fontSize = 32.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(dateString, color = textColor.copy(alpha = 0.9f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // Recuadro blanco pequeño y compacto (sin negrilla)
                            Box(
                                modifier = Modifier
                                    .background(Color.White, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(turnoNombreStr, color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Normal, maxLines = 1)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val isLaboral24 = uiData.title.contains("LABORAL 24H")
                        
                        if (isLaboral24) {
                            Text("LABORAL 24H", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                            if (uiData.subturnoPrefix.isNotEmpty()) {
                                Text(uiData.subturnoPrefix, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        } else {
                            val is12H = uiData.title == "DÍA" || uiData.title == "NOCHE"
                            val isPrimerTurno = uiData.title.contains("PRIMER TURNO")
                            val displayTitle = if (is12H && uiData.subturnoPrefix.isNotEmpty()) "${uiData.subturnoPrefix}${uiData.title}" else if (uiData.subturnoPrefix.isNotEmpty()) "${uiData.subturnoPrefix}\n${uiData.title}" else uiData.title
                            
                            val fs = when {
                                isPrimerTurno -> 16.sp
                                is12H -> 18.sp
                                else -> 20.sp
                            }
                            
                            Text(
                                text = displayTitle, 
                                color = textColor, 
                                fontSize = fs, 
                                fontWeight = FontWeight.ExtraBold, 
                                maxLines = if (isPrimerTurno || displayTitle.contains("\n")) 2 else 1,
                                lineHeight = 20.sp
                            )
                        }
                        
                        if (!uiData.isLibre) {
                            Spacer(modifier = Modifier.height(4.dp))
                            // Pequeña reducción preventiva de fuente para asegurar que quepa
                            Text("${uiData.entradaHora} - ${uiData.salidaHora}", color = textColor.copy(alpha = 0.9f), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            if (uiData.entradaFecha != uiData.salidaFecha) {
                                Text("(Día siguiente)", color = textColor.copy(alpha = 0.8f), fontSize = 14.sp)
                            }
                        } else {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Día de descanso", color = textColor.copy(alpha = 0.9f), fontSize = 16.sp)
                        }
                        
                        // Margen extra final
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                
                if (!uiData.isLibre) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = textColor.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(compNombre, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Entrada: ${uiData.entradaHora}", color = textColor, fontSize = 12.sp)
                            Text("Salida: ${uiData.salidaHora}", color = textColor, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
