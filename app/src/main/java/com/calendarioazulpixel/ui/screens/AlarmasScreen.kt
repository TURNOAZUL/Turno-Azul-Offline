package com.calendarioazulpixel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.ModeNight
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calendarioazulpixel.data.local.PreferencesRepository
import com.calendarioazulpixel.domain.model.AlarmConfig
import com.calendarioazulpixel.ui.theme.BluePrimary
import com.calendarioazulpixel.util.AlarmSyncManager
import kotlinx.coroutines.launch

fun formatTime(hour: Int, minute: Int, is24h: Boolean): String {
    if (is24h) {
        return String.format("%02d:%02d", hour, minute)
    } else {
        val amPm = if (hour >= 12) "PM" else "AM"
        var h12 = hour % 12
        if (h12 == 0) h12 = 12
        return String.format("%02d:%02d %s", h12, minute, amPm)
    }
}

fun formatRepeatSummary(config: AlarmConfig): String {
    if (config.type == "dia") return "2.º - 3.º Turno (Día)"
    if (config.type == "noche") return "4.º - 1.º Turno (Noche)"
    
    return when (config.repeatMode) {
        "everyday" -> "Todos los días"
        "once" -> "Una sola vez"
        "custom" -> {
            if (config.repeatDays.isEmpty()) "Una sola vez"
            else if (config.repeatDays.size == 7) "Todos los días"
            else {
                val dayNames = mapOf(1 to "Dom", 2 to "Lun", 3 to "Mar", 4 to "Mié", 5 to "Jue", 6 to "Vie", 7 to "Sáb")
                val count = config.repeatDays.size
                val names = config.repeatDays.sorted().mapNotNull { dayNames[it] }.joinToString(", ")
                "Días específicos ($count días): $names"
            }
        }
        else -> if (config.type == "general") "Todos los días" else "Una sola vez"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmasScreen(
    onNavigateBack: () -> Unit,
    onNavigateToConfig: (String) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { PreferencesRepository(context) }
    val scope = rememberCoroutineScope()
    
    val is24HourFormat by repository.is24HourFormat.collectAsState(initial = false)
    
    // Configuraciones de las tres tarjetas principales de la pantalla principal
    val generalConfig by repository.getAlarmConfig("general").collectAsState(initial = AlarmConfig("general", false, 5, 0, "", "Tono predeterminado", true))
    val diaConfig by repository.getAlarmConfig("dia").collectAsState(initial = AlarmConfig("dia", false, 4, 30, "", "Alarma Turno Día", true))
    val nocheConfig by repository.getAlarmConfig("noche").collectAsState(initial = AlarmConfig("noche", false, 16, 0, "", "Alarma Turno Noche", true))

    Scaffold(
        containerColor = Color(0xFFF4F6FA),
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Alarmas de Turno 12x24", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Configura tus avisos según tu rotación y horarios", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Default.Alarm, contentDescription = "Alarmas", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BluePrimary)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TimeFormatCard(
                        modifier = Modifier.weight(1f),
                        isSelected = !is24HourFormat,
                        icon = Icons.Default.WbSunny,
                        title = "12 horas",
                        example = "06:00 AM",
                        description = "Formato AM / PM",
                        onClick = { 
                            scope.launch { 
                                repository.saveTimeFormat(false) 
                                AlarmSyncManager.syncCurrentState(context)
                            } 
                        }
                    )
                    
                    TimeFormatCard(
                        modifier = Modifier.weight(1f),
                        isSelected = is24HourFormat,
                        icon = Icons.Default.Schedule,
                        title = "24 horas",
                        example = "24:00",
                        description = "Hora militar",
                        onClick = { 
                            scope.launch { 
                                repository.saveTimeFormat(true) 
                                AlarmSyncManager.syncCurrentState(context)
                            } 
                        }
                    )
                }
            }

            // 1. Tarjeta Alarma General
            item {
                AlarmaCard(
                    title = "Alarma General",
                    subtitle = formatRepeatSummary(generalConfig),
                    icon = Icons.Default.Alarm,
                    iconTint = BluePrimary,
                    iconBg = Color(0xFFE3F2FD),
                    isEnabled = generalConfig.enabled,
                    onToggle = { enabled ->
                        scope.launch {
                            repository.toggleAlarmEnabled("general", enabled)
                            AlarmSyncManager.syncCurrentState(context)
                        }
                    },
                    time = formatTime(generalConfig.hour, generalConfig.minute, is24HourFormat),
                    onConfigClick = { onNavigateToConfig("general") },
                    onDelete = null
                )
            }

            // 2. Tarjeta Alarma Turno Día
            item {
                AlarmaCard(
                    title = "Alarma Turno Día",
                    subtitle = formatRepeatSummary(diaConfig),
                    icon = Icons.Default.WbSunny,
                    iconTint = Color(0xFFF57F17),
                    iconBg = Color(0xFFFFF9C4),
                    isEnabled = diaConfig.enabled,
                    onToggle = { enabled ->
                        scope.launch {
                            repository.toggleAlarmEnabled("dia", enabled)
                            AlarmSyncManager.syncCurrentState(context)
                        }
                    },
                    time = formatTime(diaConfig.hour, diaConfig.minute, is24HourFormat),
                    onConfigClick = { onNavigateToConfig("dia") },
                    onDelete = null
                )
            }

            // 3. Tarjeta Alarma Turno Noche
            item {
                AlarmaCard(
                    title = "Alarma Turno Noche",
                    subtitle = formatRepeatSummary(nocheConfig),
                    icon = Icons.Default.ModeNight,
                    iconTint = Color(0xFF4527A0),
                    iconBg = Color(0xFFEDE7F6),
                    isEnabled = nocheConfig.enabled,
                    onToggle = { enabled ->
                        scope.launch {
                            repository.toggleAlarmEnabled("noche", enabled)
                            AlarmSyncManager.syncCurrentState(context)
                        }
                    },
                    time = formatTime(nocheConfig.hour, nocheConfig.minute, is24HourFormat),
                    onConfigClick = { onNavigateToConfig("noche") },
                    onDelete = null
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE5F2FC)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Información de Alarmas", fontWeight = FontWeight.Bold, color = BluePrimary, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("• Toca la tarjeta 'Alarma General' para configurarla y agregar hasta 4 alarmas personales adicionales (5 en total).", fontSize = 13.sp, color = Color.DarkGray, lineHeight = 18.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("• Las alarmas de Turno Día y Turno Noche funcionan de forma independiente según tu calendario de rotación 12x24.", fontSize = 13.sp, color = Color.DarkGray, lineHeight = 18.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun TimeFormatCard(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    icon: ImageVector,
    title: String,
    example: String,
    description: String,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) Color(0xFFE5F2FC) else Color.White
    val borderColor = if (isSelected) BluePrimary else Color(0xFFE0E0E0)
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(width = if (isSelected) 2.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BluePrimary)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = example, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = description, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
fun AlarmaCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    time: String,
    onConfigClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onConfigClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BluePrimary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = subtitle, fontSize = 12.sp, color = Color.Gray, lineHeight = 16.sp)
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar alarma", tint = Color.Gray)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color(0xFFF4F6FA), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE5F2FC), RoundedCornerShape(8.dp))
                        .padding(horizontal = this@Row.hashCode().let { 12 }.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = time, 
                        fontSize = 18.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = if (isEnabled) BluePrimary else Color.Gray
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = BluePrimary,
                            uncheckedThumbColor = Color.LightGray,
                            uncheckedTrackColor = Color(0xFFECEFF1)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onConfigClick, 
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFF4F6FA), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Configurar", tint = BluePrimary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
