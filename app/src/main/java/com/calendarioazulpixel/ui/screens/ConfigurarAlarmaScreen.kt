package com.calendarioazulpixel.ui.screens

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ModeNight
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calendarioazulpixel.data.local.PreferencesRepository
import com.calendarioazulpixel.domain.model.AlarmConfig
import com.calendarioazulpixel.ui.theme.BluePrimary
import com.calendarioazulpixel.ui.theme.BluePrimary
import com.calendarioazulpixel.util.AlarmSyncManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurarAlarmaScreen(
    tipoAlarma: String,
    onNavigateBack: () -> Unit,
    onNavigateToConfig: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val repository = remember { PreferencesRepository(context) }
    val scope = rememberCoroutineScope()
    
    val is24HourFormat by repository.is24HourFormat.collectAsState(initial = false)
    val initialConfig = repository.getAlarmConfig(tipoAlarma).collectAsState(
        initial = AlarmConfig(tipoAlarma, false, if(tipoAlarma=="noche") 16 else if(tipoAlarma=="dia") 4 else 5, if(tipoAlarma=="dia") 30 else 0, "", "Tono predeterminado", true)
    ).value

    val alarmIds by repository.alarmIds.collectAsState(initial = listOf("general"))

    var name by remember(initialConfig) { mutableStateOf(if (tipoAlarma == "general") "Alarma General" else initialConfig.name) }
    var hour by remember(initialConfig) { mutableStateOf(initialConfig.hour) }
    var minute by remember(initialConfig) { mutableStateOf(initialConfig.minute) }
    var soundUri by remember(initialConfig) { mutableStateOf(initialConfig.soundUri) }
    var soundName by remember(initialConfig) { mutableStateOf(initialConfig.soundName) }
    var vibrate by remember(initialConfig) { mutableStateOf(initialConfig.vibrate) }
    var repeatMode by remember(initialConfig) { mutableStateOf(initialConfig.repeatMode) }
    var repeatDays by remember(initialConfig) { mutableStateOf(initialConfig.repeatDays) }
    var snoozeEnabled by remember(initialConfig) { mutableStateOf(initialConfig.snoozeEnabled) }
    var snoozeIntervalMinutes by remember(initialConfig) { mutableStateOf(initialConfig.snoozeIntervalMinutes) }
    var snoozeMaxCount by remember(initialConfig) { mutableStateOf(initialConfig.snoozeMaxCount) }

    var showRepeatDialog by remember { mutableStateOf(false) }
    var showSnoozeIntervalDialog by remember { mutableStateOf(false) }
    var showSnoozeMaxDialog by remember { mutableStateOf(false) }

    val ringtoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                soundUri = uri.toString()
                val ringtone = RingtoneManager.getRingtone(context, uri)
                soundName = ringtone.getTitle(context) ?: "Tono seleccionado"
            } else {
                soundUri = ""
                soundName = "Silencioso"
            }
        }
    }

    val timePickerDialog = android.app.TimePickerDialog(
        context,
        { _, h, m ->
            hour = h
            minute = m
        },
        hour,
        minute,
        is24HourFormat
    )

    val titulo = when (tipoAlarma) {
        "dia" -> "Alarma Turno Día"
        "noche" -> "Alarma Turno Noche"
        "general" -> "Alarma General"
        else -> name.ifBlank { "Configurar Alarma" }
    }

    val iconInfo = when (tipoAlarma) {
        "dia" -> Triple(Icons.Default.WbSunny, Color(0xFFF57F17), Color(0xFFFFF9C4))
        "noche" -> Triple(Icons.Default.ModeNight, Color(0xFF4527A0), Color(0xFFEDE7F6))
        else -> Triple(Icons.Default.Alarm, BluePrimary, Color(0xFFE3F2FD))
    }

    val descripcion = when (tipoAlarma) {
        "dia" -> "Esta alarma sonará de forma automática únicamente los días que te corresponda trabajar en turno de día (2.º y 3.º)."
        "noche" -> "Esta alarma sonará de forma automática únicamente las tardes o noches que te corresponda entrar al turno de noche (4.º y 1.º)."
        "general" -> "Administra tu Alarma General principal y tus alarmas personales adicionales desde esta misma pantalla."
        else -> "Configura el nombre, hora, repetición y opciones de posponer para tu alarma personal."
    }

    Scaffold(
        containerColor = Color(0xFFF4F6FA),
        topBar = {
            TopAppBar(
                title = { Text(titulo, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BluePrimary)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                val config = AlarmConfig(
                                    type = tipoAlarma,
                                    enabled = initialConfig.enabled,
                                    hour = hour,
                                    minute = minute,
                                    soundUri = soundUri,
                                    soundName = soundName,
                                    vibrate = vibrate,
                                    name = if (tipoAlarma == "general") "Alarma General" else name.ifBlank { initialConfig.name },
                                    repeatMode = repeatMode,
                                    repeatDays = repeatDays,
                                    snoozeEnabled = snoozeEnabled,
                                    snoozeIntervalMinutes = snoozeIntervalMinutes,
                                    snoozeMaxCount = snoozeMaxCount
                                )
                                repository.saveAlarmConfig(config)
                                AlarmSyncManager.syncCurrentState(context)
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Guardar configuración", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Tarjeta de Descripción
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(iconInfo.third),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = iconInfo.first, contentDescription = null, tint = iconInfo.second, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = descripcion,
                        fontSize = 12.sp,
                        color = Color.DarkGray,
                        lineHeight = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Nombre de la Alarma (Solo editable para alarmas personales, fijo para General y Turnos)
            if (tipoAlarma != "general" && tipoAlarma != "dia" && tipoAlarma != "noche") {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                    Text("NOMBRE DE LA ALARMA", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black, fontSize = 16.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = BluePrimary,
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            cursorColor = BluePrimary
                        )
                    )
                }
            }

            // Selector Visual de Hora Grande
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                Text("HORA DE LA ALARMA", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { timePickerDialog.show() },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 22.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = formatTime(hour, minute, is24HourFormat),
                            fontSize = 42.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BluePrimary
                        )
                    }
                }
            }

            // Tarjeta de Opciones de Configuración
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Repetición por días
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRepeatDialog = true }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Repetición", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BluePrimary)
                            Text(formatRepeatSummary(AlarmConfig(tipoAlarma, initialConfig.enabled, hour, minute, soundUri, soundName, vibrate, repeatMode = repeatMode, repeatDays = repeatDays)), fontSize = 12.sp, color = Color.Gray)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
                    }

                    HorizontalDivider(color = Color(0xFFF4F6FA), thickness = 1.dp)

                    // Sonido
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                                    if (soundUri.isNotEmpty()) {
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(soundUri))
                                    }
                                }
                                ringtoneLauncher.launch(intent)
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sonido de alarma", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BluePrimary)
                            Text(soundName, fontSize = 12.sp, color = Color.Gray, maxLines = 1)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
                    }

                    HorizontalDivider(color = Color(0xFFF4F6FA), thickness = 1.dp)

                    // Vibración
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Vibration, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Vibración", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BluePrimary)
                        }
                        Switch(
                            checked = vibrate,
                            onCheckedChange = { vibrate = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = BluePrimary,
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = Color(0xFFECEFF1)
                            ),
                            modifier = Modifier.scale(0.85f)
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF4F6FA), thickness = 1.dp)

                    // Posponer (Snooze) Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Snooze, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Posponer", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BluePrimary)
                        }
                        Switch(
                            checked = snoozeEnabled,
                            onCheckedChange = { snoozeEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = BluePrimary,
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = Color(0xFFECEFF1)
                            ),
                            modifier = Modifier.scale(0.85f)
                        )
                    }

                    if (snoozeEnabled) {
                        HorizontalDivider(color = Color(0xFFF4F6FA), thickness = 1.dp)
                        
                        // Intervalo de posponer
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showSnoozeIntervalDialog = true }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Intervalo de posponer", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray)
                                Text("$snoozeIntervalMinutes minutos", fontSize = 12.sp, color = Color.Gray)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
                        }

                        HorizontalDivider(color = Color(0xFFF4F6FA), thickness = 1.dp)

                        // Veces máximas
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showSnoozeMaxDialog = true }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Veces máximas", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray)
                                Text("$snoozeMaxCount veces", fontSize = 12.sp, color = Color.Gray)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
                        }
                    }
                }
            }

            // =========================================================================
            // SI ES ALARMA GENERAL, MOSTRAR AQUÍ LAS ALARMAS PERSONALES Y EL BOTÓN "+ Agregar alarma"
            // =========================================================================
            if (tipoAlarma == "general") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Alarmas Personales Adicionales",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = BluePrimary,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Text(
                    text = "Puedes agregar hasta 4 alarmas personales adicionales. La Alarma General es fija y no se puede eliminar.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

                val customAlarmIds = alarmIds.filter { it != "general" }
                if (customAlarmIds.isEmpty()) {
                    Text(
                        text = "No hay alarmas personales adicionales creadas.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                } else {
                    customAlarmIds.forEach { customId ->
                        val customConfig by repository.getAlarmConfig(customId).collectAsState(initial = AlarmConfig(customId, false, 6, 0, "", "Tono predeterminado", true))
                        AlarmaCard(
                            title = customConfig.name,
                            subtitle = formatRepeatSummary(customConfig),
                            icon = Icons.Default.Alarm,
                            iconTint = BluePrimary,
                            iconBg = Color(0xFFE3F2FD),
                            isEnabled = customConfig.enabled,
                            onToggle = { enabled ->
                                scope.launch {
                                    repository.toggleAlarmEnabled(customId, enabled)
                                    AlarmSyncManager.syncCurrentState(context)
                                }
                            },
                            time = formatTime(customConfig.hour, customConfig.minute, is24HourFormat),
                            onConfigClick = { onNavigateToConfig?.invoke(customId) },
                            onDelete = {
                                scope.launch {
                                    repository.deleteAlarm(customId)
                                    AlarmSyncManager.syncCurrentState(context)
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Botón "+ Agregar alarma" exclusivo dentro de la pantalla Alarma General
                val isLimitReached = alarmIds.size >= 5
                Button(
                    onClick = {
                        if (isLimitReached) {
                            Toast.makeText(context, "Has alcanzado el límite máximo de 5 alarmas", Toast.LENGTH_SHORT).show()
                        } else {
                            val nextId = if (!alarmIds.contains("custom_1")) "custom_1" else if (!alarmIds.contains("custom_2")) "custom_2" else if (!alarmIds.contains("custom_3")) "custom_3" else "custom_4"
                            scope.launch {
                                val added = repository.addCustomAlarm(nextId)
                                if (added) {
                                    onNavigateToConfig?.invoke(nextId)
                                } else {
                                    Toast.makeText(context, "Has alcanzado el límite máximo de 5 alarmas", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLimitReached) Color.LightGray else BluePrimary
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isLimitReached) "Límite de 5 alarmas alcanzado" else "Agregar alarma",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showRepeatDialog) {
        AlertDialog(
            onDismissRequest = { showRepeatDialog = false },
            title = { Text("Repetición de la alarma") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { repeatMode = "once"; showRepeatDialog = false }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = repeatMode == "once", onClick = { repeatMode = "once"; showRepeatDialog = false })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Timbrar una vez", fontSize = 15.sp)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { repeatMode = "everyday"; showRepeatDialog = false }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = repeatMode == "everyday", onClick = { repeatMode = "everyday"; showRepeatDialog = false })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Repetir todos los días", fontSize = 15.sp)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Días específicos de la semana:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BluePrimary)
                    
                    val daysOfWeek = listOf(
                        1 to "Domingo",
                        2 to "Lunes",
                        3 to "Martes",
                        4 to "Miércoles",
                        5 to "Jueves",
                        6 to "Viernes",
                        7 to "Sábado"
                    )
                    daysOfWeek.forEach { (dayNum, dayName) ->
                        val isSelected = repeatDays.contains(dayNum)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    repeatMode = "custom"
                                    repeatDays = if (isSelected) repeatDays - dayNum else repeatDays + dayNum
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    repeatMode = "custom"
                                    repeatDays = if (checked) repeatDays + dayNum else repeatDays - dayNum
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(dayName, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRepeatDialog = false }) { Text("Aceptar") }
            }
        )
    }

    if (showSnoozeIntervalDialog) {
        AlertDialog(
            onDismissRequest = { showSnoozeIntervalDialog = false },
            title = { Text("Intervalo de posponer") },
            text = {
                Column {
                    val intervals = listOf(5, 10, 15, 20, 25, 30)
                    intervals.forEach { minutes ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    snoozeIntervalMinutes = minutes
                                    showSnoozeIntervalDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = snoozeIntervalMinutes == minutes,
                                onClick = {
                                    snoozeIntervalMinutes = minutes
                                    showSnoozeIntervalDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$minutes minutos", fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSnoozeIntervalDialog = false }) { Text("Cerrar") }
            }
        )
    }

    if (showSnoozeMaxDialog) {
        AlertDialog(
            onDismissRequest = { showSnoozeMaxDialog = false },
            title = { Text("Veces máximas de posponer") },
            text = {
                Column {
                    val counts = listOf(2, 3, 5, 10)
                    counts.forEach { count ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    snoozeMaxCount = count
                                    showSnoozeMaxDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = snoozeMaxCount == count,
                                onClick = {
                                    snoozeMaxCount = count
                                    showSnoozeMaxDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$count veces", fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSnoozeMaxDialog = false }) { Text("Cerrar") }
            }
        )
    }
}
