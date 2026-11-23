package com.calendarioazulpixel.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.calendarioazulpixel.data.local.PreferencesRepository
import com.calendarioazulpixel.domain.model.AppFeature
import com.calendarioazulpixel.domain.model.FeatureCatalog
import com.calendarioazulpixel.domain.usecase.Compania
import com.calendarioazulpixel.domain.usecase.TipoTurno
import com.calendarioazulpixel.ui.components.ShiftUiData
import com.calendarioazulpixel.ui.components.getShiftUiData
import com.calendarioazulpixel.ui.navigation.Screen
import com.calendarioazulpixel.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioScreen(onNavigate: (String) -> Unit = {}) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    
    // Calcular mensajes sin leer globales
    var globalUnreadCount by remember { mutableStateOf(0L) }
    val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val db = remember { FirebaseFirestore.getInstance() }
    
    DisposableEffect(currentUid) {
        if (currentUid.isEmpty()) return@DisposableEffect onDispose {}
        val listener = db.collection("chats")
            .whereArrayContains("participants", currentUid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    var totalUnread = 0L
                    for (doc in snapshot.documents) {
                        val unreadCountsMap = doc.get("unreadCounts") as? Map<String, Long>
                        val myUnreadCount = unreadCountsMap?.get(currentUid) ?: 0L
                        totalUnread += myUnreadCount
                    }
                    globalUnreadCount = totalUnread
                }
            }
        onDispose { listener.remove() }
    }
    val preferencesRepository = remember { PreferencesRepository(context) }
    
    // Configuración de Accesos Rápidos
    val savedShortcutsIds by preferencesRepository.shortcuts.collectAsState(initial = emptyList())
    val quickAccessFeatures = remember(savedShortcutsIds) {
        if (savedShortcutsIds.isEmpty()) emptyList() else savedShortcutsIds.mapNotNull { FeatureCatalog.getById(it) }
    }

    val tipoTurnoActivoStr by preferencesRepository.tipoTurnoActivo.collectAsState(initial = "DOCE_VEINTICUATRO")
    val tipoTurno = TipoTurno.valueOf(tipoTurnoActivoStr)

    // Configuración 12Hx24H
    val compania12Str by preferencesRepository.companiaSeleccionada.collectAsState(initial = "AMARILLA")
    val compania12 = Compania.valueOf(compania12Str)
    val dEntrada by preferencesRepository.diaEntrada.collectAsState(initial = "6:00 AM")
    val dSalida by preferencesRepository.diaSalida.collectAsState(initial = "6:00 PM")
    val nEntrada by preferencesRepository.nocheEntrada.collectAsState(initial = "6:00 PM")
    val nSalida by preferencesRepository.nocheSalida.collectAsState(initial = "6:00 AM")
    
    val subDia by preferencesRepository.subturnoDia.collectAsState(initial = "2")
    val subNoche by preferencesRepository.subturnoNoche.collectAsState(initial = "4")
    val refDia by preferencesRepository.fechaRefSubturnoDia.collectAsState(initial = 0L)
    val refNoche by preferencesRepository.fechaRefSubturnoNoche.collectAsState(initial = 0L)
    
    // Configuración 24Hx48H
    val compania24Str by preferencesRepository.compania24Seleccionada.collectAsState(initial = "AZUL")
    val compania24 = Compania.valueOf(compania24Str)
    val entrada24 by preferencesRepository.entrada24.collectAsState(initial = "6:00 AM")
    val salida24 by preferencesRepository.salida24.collectAsState(initial = "6:00 AM")
    
    val sub24 by preferencesRepository.subturno24.collectAsState(initial = "2/4")
    val ref24 by preferencesRepository.fechaRefSubturno24.collectAsState(initial = 0L)
    
    // Configuración 10Dx5D
    val compania10Str by preferencesRepository.compania10Seleccionada.collectAsState(initial = "ORION")
    val compania10 = Compania.valueOf(compania10Str)
    val entrada10 by preferencesRepository.entrada10.collectAsState(initial = "7:00 AM")
    val salida10 by preferencesRepository.salida10.collectAsState(initial = "7:00 PM")
    
    // Compañía activa según el tipo de turno
    val compania = when (tipoTurno) {
        TipoTurno.DOCE_VEINTICUATRO -> compania12
        TipoTurno.VEINTICUATRO_CUARENTA_OCHO -> compania24
        TipoTurno.DIEZ_CINCO -> compania10
    }

    // Calcular el turno de hoy
    // Usamos el verdadero día "hoy" del calendario
    val todayCal = Calendar.getInstance()
    // Aseguramos de quitar horas para cálculo consistente de "hoy" independientemente de la hora actual
    todayCal.set(Calendar.HOUR_OF_DAY, 0)
    todayCal.set(Calendar.MINUTE, 0)
    todayCal.set(Calendar.SECOND, 0)
    todayCal.set(Calendar.MILLISECOND, 0)
    
    val todayMillis = todayCal.timeInMillis
    
    val todayUiData = getShiftUiData(
        todayMillis, tipoTurno, compania, 
        dEntrada, dSalida, nEntrada, nSalida, 
        entrada24, salida24,
        entrada10, salida10,
        subDia, subNoche, refDia, refNoche,
        sub24, ref24
    )
    
    // Calcular próximos 3 días
    val proximosTurnos = remember(todayMillis, tipoTurno, compania, dEntrada, dSalida, nEntrada, nSalida, entrada24, salida24, entrada10, salida10, subDia, subNoche, refDia, refNoche, sub24, ref24) {
        val list = mutableListOf<Pair<String, ShiftUiData>>()
        // Usamos clon de todayCal para que empiece exactamente "hoy"
        val cal = todayCal.clone() as Calendar
        val format = SimpleDateFormat("EEE, d MMM", Locale("es", "ES"))
        
        cal.add(Calendar.DAY_OF_YEAR, 1)
        list.add(Pair("Mañana", getShiftUiData(cal.timeInMillis, tipoTurno, compania, dEntrada, dSalida, nEntrada, nSalida, entrada24, salida24, entrada10, salida10, subDia, subNoche, refDia, refNoche, sub24, ref24)))
        
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val name2 = format.format(cal.timeInMillis).replaceFirstChar { it.uppercase() }
        list.add(Pair(name2, getShiftUiData(cal.timeInMillis, tipoTurno, compania, dEntrada, dSalida, nEntrada, nSalida, entrada24, salida24, entrada10, salida10, subDia, subNoche, refDia, refNoche, sub24, ref24)))
        
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val name3 = format.format(cal.timeInMillis).replaceFirstChar { it.uppercase() }
        list.add(Pair(name3, getShiftUiData(cal.timeInMillis, tipoTurno, compania, dEntrada, dSalida, nEntrada, nSalida, entrada24, salida24, entrada10, salida10, subDia, subNoche, refDia, refNoche, sub24, ref24)))
        
        list
    }

    val esAdministrador = false 

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp) 
        ) {
            HeaderSection()
            Spacer(modifier = Modifier.height(16.dp)) 
            CompaniaIndicador(compania = compania, tipoTurno = tipoTurno)
            Spacer(modifier = Modifier.height(8.dp)) 
            TuTurnoDeHoySection(uiData = todayUiData, todayMillis = todayMillis)
            Spacer(modifier = Modifier.height(24.dp)) 
            ProximosTurnosSection(turnos = proximosTurnos)
            Spacer(modifier = Modifier.height(24.dp)) 
            
            // Pasamos la lista dinámica de accesos rápidos
            if (quickAccessFeatures.isNotEmpty()) {
                AccesosRapidosSection(
                    accesosConfigurados = quickAccessFeatures,
                    globalUnreadCount = globalUnreadCount,
                    onNavigate = onNavigate,
                    onShowMore = { showBottomSheet = true }
                )
            }
        }

        if (showBottomSheet) {
            val currentDensity = androidx.compose.ui.platform.LocalDensity.current
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                // Forzamos el LocalDensity aquí adentro para que las letras y botones de "Más" también adopten la escala elegida
                CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides currentDensity) {
                    OpcionesBottomSheet(
                        esAdministrador = esAdministrador,
                        onNavigate = { route ->
                            showBottomSheet = false
                            onNavigate(route)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HeaderSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Brush.verticalGradient(colors = listOf(Color(0xFF81D4FA), Color(0xFFE3F2FD))))
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(BluePrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Avatar",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AssignmentInd, contentDescription = null, modifier = Modifier.size(16.dp), tint = BluePrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Código de usuario: 104578", color = BluePrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = BluePrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Patio de Seguridad", color = BluePrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun CompaniaIndicador(compania: Compania, tipoTurno: TipoTurno) {
    val color = when (compania) {
        Compania.AMARILLA -> Color(0xFFFFD54F)
        Compania.ROJA -> Color(0xFFEF5350)
        Compania.AZUL -> Color(0xFF42A5F5)
        Compania.ORION -> Color(0xFF9575CD)
        Compania.BRAVO -> Color(0xFF4DB6AC)
    }
    val nombre = "C. ${compania.name.lowercase().replaceFirstChar { it.uppercase() }}"

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("COMPAÑÍA CONFIGURADA:", fontSize = 12.sp, color = BluePrimary, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.width(4.dp))
            Text(nombre, fontSize = 14.sp, color = BluePrimary, fontWeight = FontWeight.Bold)
        }
        TipoTurnoIndicador(tipoTurno = tipoTurno)
    }
}

@Composable
fun TipoTurnoIndicador(tipoTurno: TipoTurno) {
    val nombre = when (tipoTurno) {
        TipoTurno.DOCE_VEINTICUATRO -> "12H × 24H"
        TipoTurno.VEINTICUATRO_CUARENTA_OCHO -> "24H × 48H"
        TipoTurno.DIEZ_CINCO -> "10D × 5D"
    }
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(BlueSecondary)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = nombre, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TuTurnoDeHoySection(uiData: ShiftUiData, todayMillis: Long) {
    val todayStr = remember(todayMillis) {
        SimpleDateFormat("EEE., d MMM.", Locale("es", "ES")).format(todayMillis).uppercase()
    }

    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "TU TURNO DE HOY", 
                fontWeight = FontWeight.ExtraBold, 
                color = BluePrimary, 
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(text = todayStr, fontWeight = FontWeight.Bold, color = BluePrimary, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        when {
                            uiData.title.contains("NOCHE") -> Brush.horizontalGradient(colors = listOf(NightShiftBg1, NightShiftBg2, NightShiftBg3))
                            uiData.title.contains("LABORAL 24H") -> Brush.horizontalGradient(colors = listOf(Trabajo24Bg1, Trabajo24Bg3))
                            uiData.title.contains("PRIMER TURNO") -> Brush.horizontalGradient(colors = listOf(Color(0xFF5B9BE8), BlueSecondary))
                            else -> Brush.horizontalGradient(colors = listOf(uiData.bgColor, uiData.bgColor))
                        }
                    )
                    .padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(72.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) { Text(uiData.icon, fontSize = 36.sp) }
                    Spacer(modifier = Modifier.width(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        val isLaboral24 = uiData.title == "LABORAL 24H" || uiData.title == "DÍA LABORAL"
                        
                        if (isLaboral24) {
                            val fontSizeTitle = if (uiData.title == "DÍA LABORAL") 26.sp else 32.sp 
                            Text(text = "LABORAL 24H", fontWeight = FontWeight.ExtraBold, color = uiData.textColor, fontSize = fontSizeTitle)
                            Spacer(modifier = Modifier.height(4.dp))
                            if (uiData.subturnoPrefix.isNotEmpty()) {
                                Text(text = uiData.subturnoPrefix, fontWeight = FontWeight.Bold, color = uiData.textColor, fontSize = 20.sp)
                            }
                        } else {
                            val is12H = uiData.title == "DÍA" || uiData.title == "NOCHE"
                            val isPrimerTurno = uiData.title.contains("PRIMER TURNO")
                            val displayTitle = if (is12H && uiData.subturnoPrefix.isNotEmpty()) "${uiData.subturnoPrefix}${uiData.title}" else if (uiData.subturnoPrefix.isNotEmpty()) "${uiData.subturnoPrefix}\n${uiData.title}" else uiData.title
                            
                            val fs = when {
                                isPrimerTurno -> 20.sp
                                is12H -> 20.sp
                                else -> 32.sp
                            }
                            
                            Text(
                                text = displayTitle, 
                                fontWeight = FontWeight.ExtraBold, 
                                color = uiData.textColor, 
                                fontSize = fs, 
                                maxLines = if (isPrimerTurno) 2 else 1,
                                lineHeight = 24.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        if (uiData.isLibre) {
                            Text(text = "Día de descanso", color = uiData.textColor.copy(alpha = 0.9f), fontSize = 16.sp)
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "${uiData.entradaHora} - ${uiData.salidaHora}", color = uiData.textColor.copy(alpha = 0.9f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = uiData.textColor.copy(alpha = 0.6f))
                            }
                            if (uiData.entradaFecha != uiData.salidaFecha) {
                                Text(text = "(${uiData.salidaFecha})", color = uiData.textColor.copy(alpha = 0.7f), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProximosTurnosSection(turnos: List<Pair<String, ShiftUiData>>) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "PRÓXIMOS TURNOS", fontWeight = FontWeight.ExtraBold, color = BluePrimary, fontSize = 16.sp)
            Text(text = "Ver todos >", color = BlueSecondary, fontSize = 14.sp, modifier = Modifier.clickable { })
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            turnos.forEach { (nombreDia, uiData) ->
                // Usamos el degradado pastel azul si es NOCHE o DIA LABORAL, sino el color predeterminado
                val cardBgColor = when {
                    uiData.title.contains("NOCHE") -> Brush.verticalGradient(
                        colors = listOf(
                            NightShiftBg1.copy(alpha = 0.9f), 
                            NightShiftBg2.copy(alpha = 0.95f), 
                            NightShiftBg3
                        )
                    )
                    uiData.title.contains("LABORAL 24H") -> Brush.verticalGradient(
                        colors = listOf(
                            Trabajo24Bg1.copy(alpha = 0.9f), 
                            Trabajo24Bg3.copy(alpha = 0.95f)
                        )
                    )
                    else -> Brush.verticalGradient(colors = listOf(uiData.bgColor, uiData.bgColor))
                }
                
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(cardBgColor)
                            .padding(vertical = 16.dp, horizontal = 8.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            val isDarkTitle = uiData.title.contains("NOCHE") || uiData.title.contains("LABORAL 24H") || uiData.title.contains("DÍA LABORAL")
                            
                            Text(text = nombreDia, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isDarkTitle) Color.White else BluePrimary)
                            val fechaCorta = uiData.entradaFecha.substringBefore(",").plus(", ").plus(uiData.entradaFecha.substringAfter(" ").substringBefore(" "))
                            Text(text = fechaCorta, fontSize = 10.sp, color = if (isDarkTitle) Color.White.copy(alpha = 0.7f) else TextSecondary)
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(if (isDarkTitle) Color.Transparent else Color.White.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                                Text(text = uiData.icon, fontSize = 28.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            val tituloColor = if (isDarkTitle) Color.White else uiData.textColor
                            
                            if (uiData.title == "LABORAL 24H" || uiData.title == "DÍA LABORAL") {
                                Text(text = "LABORAL 24H", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = tituloColor, textAlign = TextAlign.Center)
                                if (uiData.subturnoPrefix.isNotEmpty()) {
                                    Text(text = uiData.subturnoPrefix, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = tituloColor, textAlign = TextAlign.Center)
                                }
                            } else {
                                val is12H = uiData.title == "DÍA" || uiData.title == "NOCHE"
                                val displayTitle = if (is12H && uiData.subturnoPrefix.isNotEmpty()) "${uiData.subturnoPrefix}${uiData.title}" else if (uiData.subturnoPrefix.isNotEmpty()) "${uiData.subturnoPrefix}\n${uiData.title}" else uiData.title
                                val fs = if (is12H) 11.sp else 14.sp
                                Text(text = displayTitle, fontWeight = FontWeight.ExtraBold, fontSize = fs, color = tituloColor, textAlign = TextAlign.Center, maxLines = 1)
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            val horaStr = if (uiData.isLibre) "Sin turno" else "${uiData.entradaHora} - ${uiData.salidaHora}"
                            Text(text = horaStr, fontSize = 11.sp, color = if (isDarkTitle) Color.White.copy(alpha = 0.9f) else TextPrimary, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}



@Composable
fun AccesosRapidosSection(
    accesosConfigurados: List<AppFeature>,
    globalUnreadCount: Long = 0L,
    onNavigate: (String) -> Unit, 
    onShowMore: () -> Unit
) {
    // Tomamos los primeros 5 para tener un total de 6 con el botón "Más"
    val safeAccesos = accesosConfigurados.take(5)
    
    val itemsMostrar = mutableListOf<AppFeature>().apply {
        addAll(safeAccesos)
        add(AppFeature("more_action", "+ Más", Icons.Default.Add, "more_action"))
    }

    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        itemsMostrar.chunked(2).forEach { fila ->
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                fila.forEach { feature ->
                    val isMoreAction = feature.id == "more_action"
                    val backgroundColor = if (isMoreAction) Color(0xFF5B9BE8) else SurfaceWhite
                    val iconColor = BluePrimary
                    val textColor = if (isMoreAction) Color.White else BluePrimary

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = if (isMoreAction) Alignment.CenterEnd else Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(if (isMoreAction) 0.6f else 1f)
                                .clickable { 
                                    if (isMoreAction) onShowMore() else onNavigate(feature.route) 
                                },
                            shape = RoundedCornerShape(if (isMoreAction) 12.dp else 16.dp),
                            colors = CardDefaults.cardColors(containerColor = backgroundColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isMoreAction) 0.dp else 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = if (isMoreAction) 10.dp else 14.dp, horizontal = 8.dp).fillMaxWidth(), 
                                horizontalArrangement = Arrangement.Center, 
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isMoreAction) {
                                    Text(text = "+ Más", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textColor)
                                } else {
                                    Icon(imageVector = feature.icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = feature.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textColor)
                                    
                                    if (feature.id == "permutas" && globalUnreadCount > 0) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .background(Color.Red, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (globalUnreadCount > 99) "99+" else globalUnreadCount.toString(),
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (fila.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

data class BottomSheetOption(val title: String, val subtitle: String, val icon: ImageVector, val route: String)

@Composable
fun OpcionesBottomSheet(esAdministrador: Boolean, onNavigate: (String) -> Unit) {
    val opciones = mutableListOf(
        BottomSheetOption("Permutas", "Solicita o busca permutas disponibles.", Icons.Default.SyncAlt, Screen.Permutas.route),
        BottomSheetOption("Mi calendario", "Consultar fechas y turnos.", Icons.Default.CalendarMonth, Screen.Calendario.route),
        BottomSheetOption("Compensatorios", "Permisos, solicitudes y estados.", Icons.Default.Assignment, Screen.Compensatorios.route),
        BottomSheetOption("Novedades", "Avisos y novedades laborales.", Icons.Default.Notifications, Screen.Novedades.route),
        BottomSheetOption("Mis turnos", "Configurar ciclos y horarios.", Icons.Default.FormatListBulleted, Screen.Turnos.route),
        BottomSheetOption("Mis servicios", "Hospitales, garitas y otros puestos.", Icons.Default.Security, Screen.Servicios.route),
        BottomSheetOption("Contactos", "Jefes y compañeros.", Icons.Default.Group, Screen.Contactos.route),
        BottomSheetOption("Chats", "Conversaciones entre usuarios autorizados.", Icons.Default.Chat, Screen.Chats.route),
        BottomSheetOption("Alarmas y recordatorios", "Recordar turnos, firmas y permisos.", Icons.Default.Alarm, Screen.Alarmas.route),
        BottomSheetOption("Clima", "Consultar el pronóstico de la ubicación elegida.", Icons.Default.Cloud, Screen.Clima.route),
        BottomSheetOption("Ayuda", "Consultar asistencia y enviar alertas.", Icons.Default.HelpOutline, Screen.Ayuda.route),
        BottomSheetOption("Configuración", "Preferencias, notificaciones y privacidad.", Icons.Default.Settings, Screen.Configuracion.route),
        // Botón especial para personalizar accesos rápidos
        BottomSheetOption("Personalizar accesos rápidos", "Elige las 5 opciones de Inicio.", Icons.Default.Tune, Screen.PersonalizarAccesos.route)
    )

    if (esAdministrador) {
        opciones.add(BottomSheetOption("Administración", "Gestionar usuarios y accesos.", Icons.Default.AdminPanelSettings, Screen.Administracion.route))
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Más opciones",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = BluePrimary,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp)
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp) // Espacio al final
        ) {
            items(opciones) { opcion ->
                BottomSheetItemRow(opcion) {
                    onNavigate(opcion.route)
                }
            }
        }
    }
}

@Composable
fun BottomSheetItemRow(opcion: BottomSheetOption, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BlueLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = opcion.icon, contentDescription = null, tint = BluePrimary)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = opcion.title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = opcion.subtitle, color = TextSecondary, fontSize = 14.sp)
        }
    }
}
