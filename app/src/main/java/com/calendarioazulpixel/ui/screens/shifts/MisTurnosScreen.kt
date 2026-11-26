package com.calendarioazulpixel.ui.screens.shifts

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calendarioazulpixel.data.local.PreferencesRepository
import com.calendarioazulpixel.domain.usecase.Compania
import com.calendarioazulpixel.domain.usecase.ShiftCalculator
import com.calendarioazulpixel.domain.usecase.TipoTurno
import com.calendarioazulpixel.domain.usecase.TurnoType
import com.calendarioazulpixel.ui.theme.BackgroundLight
import com.calendarioazulpixel.ui.theme.BluePrimary
import com.calendarioazulpixel.ui.theme.BlueSecondary
import com.calendarioazulpixel.ui.theme.TextPrimary
import com.calendarioazulpixel.ui.theme.TextSecondary
import com.calendarioazulpixel.util.TurnoAlarmUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class TimePickerType { NONE, DIA_ENTRADA, DIA_SALIDA, NOCHE_ENTRADA, NOCHE_SALIDA, ENTRADA_24, SALIDA_24, ENTRADA_10, SALIDA_10 }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisTurnosScreen() {
    val context = LocalContext.current
    val repository = remember { PreferencesRepository(context) }
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf(TipoTurno.DOCE_VEINTICUATRO) }

    // Estado 12H x 24H
    var selectedCompania by remember { mutableStateOf(Compania.AMARILLA) }
    var diaEntrada by remember { mutableStateOf("6:00 AM") }
    var diaSalida by remember { mutableStateOf("6:00 PM") }
    var nocheEntrada by remember { mutableStateOf("6:00 PM") }
    var nocheSalida by remember { mutableStateOf("6:00 AM") }
    
    // Estado 24H x 48H
    var selectedCompania24 by remember { mutableStateOf(Compania.AZUL) }
    var entrada24 by remember { mutableStateOf("6:00 AM") }
    var salida24 by remember { mutableStateOf("6:00 AM") }
    
    // Estado 10D x 5D
    var selectedCompania10 by remember { mutableStateOf(Compania.ORION) }
    var entrada10 by remember { mutableStateOf("7:00 AM") }
    var salida10 by remember { mutableStateOf("7:00 PM") }

    var activeTimePicker by remember { mutableStateOf(TimePickerType.NONE) }
    
    // Subturnos 12H x 24H
    var selectedSubturnoDia by remember { mutableStateOf("2") }
    var selectedSubturnoNoche by remember { mutableStateOf("4") }
    var refDateDia by remember { mutableStateOf(0L) }
    var refDateNoche by remember { mutableStateOf(0L) }
    
    // Subturno 24H x 48H
    var selectedSubturno24 by remember { mutableStateOf("2/4") }
    var refDate24 by remember { mutableStateOf(0L) }

    // Cargar los datos al abrir la pantalla
    LaunchedEffect(Unit) {
        val activeStr = repository.tipoTurnoActivo.first()
        activeTab = TipoTurno.valueOf(activeStr)
        
        // 12x24
        selectedCompania = Compania.valueOf(repository.companiaSeleccionada.first())
        diaEntrada = repository.diaEntrada.first()
        diaSalida = repository.diaSalida.first()
        nocheEntrada = repository.nocheEntrada.first()
        nocheSalida = repository.nocheSalida.first()
        
        selectedSubturnoDia = repository.subturnoDia.first()
        selectedSubturnoNoche = repository.subturnoNoche.first()
        refDateDia = repository.fechaRefSubturnoDia.first()
        refDateNoche = repository.fechaRefSubturnoNoche.first()
        
        // 24x48
        selectedCompania24 = Compania.valueOf(repository.compania24Seleccionada.first())
        entrada24 = repository.entrada24.first()
        salida24 = repository.salida24.first()
        
        selectedSubturno24 = repository.subturno24.first()
        refDate24 = repository.fechaRefSubturno24.first()
        
        // 10x5
        selectedCompania10 = Compania.valueOf(repository.compania10Seleccionada.first())
        entrada10 = repository.entrada10.first()
        salida10 = repository.salida10.first()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Mis turnos", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Configura tu horario laboral", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BluePrimary)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            // Pestañas
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TabItem(
                        title = "12H x 24H", icon = "☀️🌙", 
                        isSelected = activeTab == TipoTurno.DOCE_VEINTICUATRO, 
                        modifier = Modifier.weight(1f).clickable { activeTab = TipoTurno.DOCE_VEINTICUATRO }
                    )
                    TabItem(
                        title = "24H × 48H", icon = "🕒", 
                        isSelected = activeTab == TipoTurno.VEINTICUATRO_CUARENTA_OCHO, 
                        modifier = Modifier.weight(1f).clickable { activeTab = TipoTurno.VEINTICUATRO_CUARENTA_OCHO }
                    )
                    TabItem(
                        title = "10D × 5D", icon = "📅", 
                        isSelected = activeTab == TipoTurno.DIEZ_CINCO, 
                        modifier = Modifier.weight(1f).clickable { activeTab = TipoTurno.DIEZ_CINCO }
                    )
                    TabItem(title = "Oficina", icon = "💼", isSelected = false, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (activeTab == TipoTurno.DOCE_VEINTICUATRO) {
                // Encabezado 12x24
                item {
                    Text(text = "12H x 24H", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = BluePrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Turno de 12 horas de trabajo por 24 horas de descanso.\nSe alterna entre turno de día, turno de noche y un día libre.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Sección Compañía 12x24
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = BluePrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Selecciona tu compañía", fontWeight = FontWeight.Bold, color = BluePrimary)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CompaniaCard(Compania.AMARILLA, Color(0xFFFFD54F), selectedCompania == Compania.AMARILLA) { selectedCompania = it }
                                CompaniaCard(Compania.ROJA, Color(0xFFEF5350), selectedCompania == Compania.ROJA) { selectedCompania = it }
                                CompaniaCard(Compania.AZUL, Color(0xFF42A5F5), selectedCompania == Compania.AZUL) { selectedCompania = it }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Configuración de Subturnos 12x24
                item {
                    val todayCal = remember {
                        Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                    }
                    val todayZeroMillis = todayCal.timeInMillis
                    val turnoHoy = remember(selectedCompania) {
                        ShiftCalculator.calcularTurno(todayZeroMillis, selectedCompania)
                    }
                    
                    val targetInfo = remember(todayZeroMillis, selectedCompania) {
                        var t = turnoHoy
                        var offset = 0
                        // Si hoy es LIBRE o DESCANSO, buscamos el próximo día de trabajo (que será DÍA)
                        while (t != TurnoType.DIA && t != TurnoType.NOCHE && offset < 5) {
                            offset++
                            t = ShiftCalculator.calcularTurno(todayZeroMillis + offset * 86400000L, selectedCompania)
                        }
                        Pair(t, todayZeroMillis + offset * 86400000L)
                    }
                    val targetTurno = targetInfo.first
                    val targetMillis = targetInfo.second

                    Text("CONFIGURACIÓN DE SUBTURNOS", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = BluePrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Selecciona el subturno que te asignaron. Turno Azul alternará automáticamente cuando vuelvas a realizar ese servicio.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (turnoHoy == TurnoType.LIBRE || turnoHoy == TurnoType.DESCANSO) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)) // FreeShiftBg aproximado
                        ) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("DÍA DE DESCANSO", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Hoy no tienes que seleccionar ningún subturno laboral.", color = Color(0xFF2E7D32).copy(alpha = 0.8f), fontSize = 12.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }

                    when (targetTurno) {
                        TurnoType.DIA -> {
                            Text("☀️ TURNO DE DÍA", fontWeight = FontWeight.Bold, color = BluePrimary, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SubturnoCard(title = "2.º TURNO", isSelected = selectedSubturnoDia == "2") { 
                                    selectedSubturnoDia = "2"
                                    refDateDia = targetMillis
                                }
                                SubturnoCard(title = "3.º TURNO", isSelected = selectedSubturnoDia == "3") { 
                                    selectedSubturnoDia = "3"
                                    refDateDia = targetMillis
                                }
                            }
                        }
                        TurnoType.NOCHE -> {
                            Text("🌙 TURNO DE NOCHE", fontWeight = FontWeight.Bold, color = BluePrimary, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SubturnoCard(title = "4.º TURNO", isSelected = selectedSubturnoNoche == "4") { 
                                    selectedSubturnoNoche = "4"
                                    refDateNoche = targetMillis
                                }
                                SubturnoCard(title = "1.er TURNO", isSelected = selectedSubturnoNoche == "1") { 
                                    selectedSubturnoNoche = "1"
                                    refDateNoche = targetMillis
                                }
                            }
                        }
                        else -> {}
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Horarios editables con selectores
                item {
                    Text("☀️ Horario del turno de día", fontWeight = FontWeight.Bold, color = BluePrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SimpleDropdown(label = "Entrada", value = diaEntrada, onClick = { activeTimePicker = TimePickerType.DIA_ENTRADA }, modifier = Modifier.weight(1f))
                        SimpleDropdown(label = "Salida", value = diaSalida, onClick = { activeTimePicker = TimePickerType.DIA_SALIDA }, modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("🌙 Horario del turno de noche", fontWeight = FontWeight.Bold, color = BluePrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SimpleDropdown(label = "Entrada", value = nocheEntrada, onClick = { activeTimePicker = TimePickerType.NOCHE_ENTRADA }, modifier = Modifier.weight(1f))
                        SimpleDropdown(label = "Salida", value = nocheSalida, onClick = { activeTimePicker = TimePickerType.NOCHE_SALIDA }, modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
                
                // Botón Guardar 12x24
                item {
                    Button(
                        onClick = {
                            scope.launch {
                                repository.saveTurnoConfig(
                                    tipoTurno = activeTab.name,
                                    compania = selectedCompania.name,
                                    dEntrada = diaEntrada,
                                    dSalida = diaSalida,
                                    nEntrada = nocheEntrada,
                                    nSalida = nocheSalida,
                                    subDia = selectedSubturnoDia,
                                    subNoche = selectedSubturnoNoche,
                                    refDia = refDateDia,
                                    refNoche = refDateNoche
                                )
                                // Llamamos al utilitario lógico de las alarmas (Etapa 2.1)
                                val alarmDates = TurnoAlarmUtils.calculateFutureAlarms(
                                    tipoTurno = activeTab,
                                    compania = selectedCompania,
                                    dEntrada = diaEntrada, dSalida = diaSalida, nEntrada = nocheEntrada, nSalida = nocheSalida,
                                    entrada24 = entrada24, salida24 = salida24,
                                    entrada10 = entrada10, salida10 = salida10,
                                    subDia = selectedSubturnoDia, subNoche = selectedSubturnoNoche,
                                    refDia = refDateDia, refNoche = refDateNoche,
                                    sub24 = selectedSubturno24, ref24 = refDate24
                                )
                                // Por ahora el resultado no se usa (Etapa 3 será el destino)
                                Toast.makeText(context, "Configuración guardada", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BlueSecondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Guardar configuración", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            } else if (activeTab == TipoTurno.VEINTICUATRO_CUARENTA_OCHO) {
                // UI PARA 24H x 48H
                item {
                    Text(text = "24H x 48H", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = BluePrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Trabaja 24 horas y descansa las siguientes 48 horas.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Sección Compañía 24x48
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = BluePrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Selecciona tu compañía", fontWeight = FontWeight.Bold, color = BluePrimary)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CompaniaCard(Compania.AMARILLA, Color(0xFFFFD54F), selectedCompania24 == Compania.AMARILLA) { selectedCompania24 = it }
                                CompaniaCard(Compania.ROJA, Color(0xFFEF5350), selectedCompania24 == Compania.ROJA) { selectedCompania24 = it }
                                CompaniaCard(Compania.AZUL, Color(0xFF2875D7), selectedCompania24 == Compania.AZUL) { selectedCompania24 = it }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Configuración de Subturnos 24x48
                item {
                    val todayCal = remember {
                        Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                    }
                    val todayZeroMillis = todayCal.timeInMillis
                    val turnoHoy = remember(selectedCompania24) {
                        ShiftCalculator.calcularTurno24x48(todayZeroMillis, selectedCompania24)
                    }
                    
                    val targetInfo = remember(todayZeroMillis, selectedCompania24) {
                        var t = turnoHoy
                        var offset = 0
                        // Si hoy es LIBRE buscamos el próximo día de TRABAJO
                        while (t != TurnoType.TRABAJO && offset < 5) {
                            offset++
                            t = ShiftCalculator.calcularTurno24x48(todayZeroMillis + offset * 86400000L, selectedCompania24)
                        }
                        Pair(t, todayZeroMillis + offset * 86400000L)
                    }
                    val targetMillis = targetInfo.second

                    Text("CONFIGURACIÓN DE SUBTURNOS", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = BluePrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Selecciona el subturno que te corresponde. La aplicación alternará automáticamente en cada nuevo día laboral.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (turnoHoy == TurnoType.LIBRE) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                        ) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("DÍA DE DESCANSO", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Hoy no tienes que seleccionar ningún subturno laboral.", color = Color(0xFF2E7D32).copy(alpha = 0.8f), fontSize = 12.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SubturnoCard(title = "☀️🌙 2/4 Turno", isSelected = selectedSubturno24 == "2/4") { 
                            selectedSubturno24 = "2/4"
                            refDate24 = targetMillis
                        }
                        SubturnoCard(title = "☀️🌙 3/1 Turno", isSelected = selectedSubturno24 == "3/1") { 
                            selectedSubturno24 = "3/1"
                            refDate24 = targetMillis
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Horarios editables con selectores
                item {
                    Text("💼 Horario de trabajo", fontWeight = FontWeight.Bold, color = BluePrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SimpleDropdown(label = "Entrada", value = entrada24, onClick = { activeTimePicker = TimePickerType.ENTRADA_24 }, modifier = Modifier.weight(1f))
                        SimpleDropdown(label = "Salida (Día siguiente)", value = salida24, onClick = { activeTimePicker = TimePickerType.SALIDA_24 }, modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }

                // Botón Guardar 24x48
                item {
                    Button(
                        onClick = {
                            scope.launch {
                                repository.saveTurno24Config(
                                    tipoTurno = activeTab.name,
                                    compania = selectedCompania24.name,
                                    entrada = entrada24,
                                    salida = salida24,
                                    sub24 = selectedSubturno24,
                                    ref24 = refDate24
                                )
                                Toast.makeText(context, "Configuración 24H x 48H guardada", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BlueSecondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Guardar configuración", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            } else if (activeTab == TipoTurno.DIEZ_CINCO) {
                // UI PARA 10D x 5D
                item {
                    Text(text = "10D × 5D", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = BluePrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Trabaja 10 días y descansa 5 días.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Sección Compañía 10Dx5D
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = BluePrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Selecciona tu compañía", fontWeight = FontWeight.Bold, color = BluePrimary)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            // Opciones ORION y BRAVO
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CompaniaCard(Compania.ORION, Color(0xFF9575CD), selectedCompania10 == Compania.ORION) { selectedCompania10 = it }
                                CompaniaCard(Compania.BRAVO, Color(0xFF4DB6AC), selectedCompania10 == Compania.BRAVO) { selectedCompania10 = it }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Horarios editables con selectores
                item {
                    Text("💼 Horario de trabajo", fontWeight = FontWeight.Bold, color = BluePrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SimpleDropdown(label = "Entrada", value = entrada10, onClick = { activeTimePicker = TimePickerType.ENTRADA_10 }, modifier = Modifier.weight(1f))
                        SimpleDropdown(label = "Salida", value = salida10, onClick = { activeTimePicker = TimePickerType.SALIDA_10 }, modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }

                // Botón Guardar 10Dx5D
                item {
                    Button(
                        onClick = {
                            scope.launch {
                                repository.saveTurno10Config(
                                    tipoTurno = activeTab.name,
                                    compania = selectedCompania10.name,
                                    entrada = entrada10,
                                    salida = salida10
                                )
                                Toast.makeText(context, "Configuración 10D × 5D guardada", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BlueSecondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Guardar configuración", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // Manejador del Selector de Horas
        if (activeTimePicker != TimePickerType.NONE) {
            val initialTime = when (activeTimePicker) {
                TimePickerType.DIA_ENTRADA -> diaEntrada
                TimePickerType.DIA_SALIDA -> diaSalida
                TimePickerType.NOCHE_ENTRADA -> nocheEntrada
                TimePickerType.NOCHE_SALIDA -> nocheSalida
                TimePickerType.ENTRADA_24 -> entrada24
                TimePickerType.SALIDA_24 -> salida24
                TimePickerType.ENTRADA_10 -> entrada10
                TimePickerType.SALIDA_10 -> salida10
                else -> "6:00 AM"
            }
            
            CustomTimePickerDialog(
                initialTime = initialTime,
                onDismiss = { activeTimePicker = TimePickerType.NONE },
                onConfirm = { newTime ->
                    when (activeTimePicker) {
                        TimePickerType.DIA_ENTRADA -> diaEntrada = newTime
                        TimePickerType.DIA_SALIDA -> diaSalida = newTime
                        TimePickerType.NOCHE_ENTRADA -> nocheEntrada = newTime
                        TimePickerType.NOCHE_SALIDA -> nocheSalida = newTime
                        TimePickerType.ENTRADA_24 -> entrada24 = newTime
                        TimePickerType.SALIDA_24 -> salida24 = newTime
                        TimePickerType.ENTRADA_10 -> entrada10 = newTime
                        TimePickerType.SALIDA_10 -> salida10 = newTime
                        else -> {}
                    }
                    activeTimePicker = TimePickerType.NONE
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTimePickerDialog(
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val isPm = initialTime.contains("PM", ignoreCase = true)
    val timeParts = initialTime.replace(Regex("[^0-9:]"), "").split(":")
    var hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 6
    val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
    
    if (isPm && hour < 12) hour += 12
    if (!isPm && hour == 12) hour = 0

    val timeState = rememberTimePickerState(
        initialHour = hour,
        initialMinute = minute,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                var selectedHour = timeState.hour
                val amPm = if (selectedHour >= 12) "PM" else "AM"
                if (selectedHour > 12) selectedHour -= 12
                if (selectedHour == 0) selectedHour = 12
                
                val minuteStr = timeState.minute.toString().padStart(2, '0')
                onConfirm("$selectedHour:$minuteStr $amPm")
            }) { Text("Aceptar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
        text = {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                TimePicker(state = timeState)
            }
        }
    )
}

@Composable
fun DateSelectorCard(dateMillis: Long, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = BluePrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Fecha de inicio del ciclo", fontWeight = FontWeight.Bold, color = BluePrimary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            val dateFormat = SimpleDateFormat("EEE, d 'de' MMM. 'de' yyyy", Locale("es", "ES"))
            val dateString = dateFormat.format(Date(dateMillis))
            
            OutlinedTextField(
                value = dateString,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Seleccionar fecha", tint = BlueSecondary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = TextPrimary,
                    disabledBorderColor = BlueSecondary.copy(alpha = 0.5f),
                    disabledTrailingIconColor = BlueSecondary
                )
            )
        }
    }
}

@Composable
fun RowScope.SubturnoCard(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.weight(1f).clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) BluePrimary else Color.White),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
    ) {
        Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = title, 
                fontWeight = FontWeight.Bold, 
                fontSize = 16.sp, 
                color = if (isSelected) Color.White else BluePrimary
            )
        }
    }
}

@Composable
fun RowScope.CompaniaCard(compania: Compania, color: Color, isSelected: Boolean, onClick: (Compania) -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) BluePrimary else Color.LightGray,
                shape = RoundedCornerShape(12.dp)
            )
            .background(if (isSelected) BluePrimary.copy(alpha = 0.1f) else Color.White)
            .clickable { onClick(compania) }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "C.${compania.name.lowercase().replaceFirstChar { it.uppercase() }}",
                color = if (isSelected) BluePrimary else TextPrimary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun TabItem(title: String, icon: String, isSelected: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) BluePrimary else Color.White)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                color = if (isSelected) Color.White else BluePrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SimpleDropdown(label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                enabled = false,
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = BlueSecondary) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = TextPrimary,
                    disabledBorderColor = BlueSecondary.copy(alpha = 0.5f),
                    disabledTrailingIconColor = BlueSecondary
                )
            )
        }
    }
}
