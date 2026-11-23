package com.calendarioazulpixel.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import com.calendarioazulpixel.ui.theme.BackgroundLight
import com.calendarioazulpixel.ui.theme.BlueLight
import com.calendarioazulpixel.ui.theme.BluePrimary
import com.calendarioazulpixel.ui.theme.TextPrimary
import com.calendarioazulpixel.ui.theme.TextSecondary
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermutasDisponiblesScreen(onNavigateBack: () -> Unit, onNavigateToChat: (String) -> Unit) {
    val context = LocalContext.current
    val repository = remember { PreferencesRepository(context) }
    var permutasGuardadas by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var orderDescending by remember { mutableStateOf(false) } // false = Más antiguas primero, true = Más recientes primero
    var orderMenuExpanded by remember { mutableStateOf(false) }
    
    val currentUid = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val hiddenPermutas by repository.getHiddenPermutas(currentUid).collectAsState(initial = emptyList())
    
    val scope = rememberCoroutineScope()
    val db = remember { FirebaseFirestore.getInstance() }
    var permutaToHide by remember { mutableStateOf<String?>(null) }

    // Cargar la lista al iniciar la pantalla desde Firestore
    LaunchedEffect(Unit) {
        db.collection("permutas")
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) {
                    return@addSnapshotListener
                }
                val lista = snapshot.documents.mapNotNull { doc ->
                    doc.data
                }
                permutasGuardadas = lista
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permutas disponibles", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BluePrimary)
            )
        }
    ) { innerPadding ->
        if (permutasGuardadas.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundLight)
                    .padding(innerPadding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Todavía no hay permutas registradas.",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Regresa a la pantalla anterior y registra la primera permuta para encontrar compañeros interesados.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundLight)
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // HEADER CONTADOR
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Total de permutas disponibles:",
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(BlueLight, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${permutasGuardadas.size}",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BluePrimary,
                                    fontSize = 16.sp
                                )
                            }
                        }
                        
                        Box {
                            OutlinedButton(
                                onClick = { orderMenuExpanded = true },
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = BlueLight),
                                border = null,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (orderDescending) "Más recientes primero" else "Más antiguas primero", 
                                    color = BluePrimary, 
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            DropdownMenu(
                                expanded = orderMenuExpanded,
                                onDismissRequest = { orderMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Más antiguas primero") },
                                    onClick = { 
                                        orderDescending = false 
                                        orderMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Más recientes primero") },
                                    onClick = { 
                                        orderDescending = true 
                                        orderMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                val permutasList = if (orderDescending) permutasGuardadas.sortedByDescending { 
                    (it["timestamp"] as? Long) ?: 0L
                } else permutasGuardadas.sortedBy { 
                    (it["timestamp"] as? Long) ?: 0L
                }
                
                // Filtramos las permutas ocultas por el usuario actual
                val visiblePermutas = permutasList.filter { permutaData -> 
                    val permutaId = permutaData["permutaId"] as? String
                    permutaId == null || !hiddenPermutas.contains(permutaId)
                }
                
                itemsIndexed(visiblePermutas) { index, permutaData ->
                    val origenCiudad = permutaData["origenCiudad"] as? String ?: ""
                    val origenDepto = permutaData["origenDepartamento"] as? String ?: ""
                    val destinoCiudad = permutaData["destinoCiudad"] as? String ?: ""
                    val destinoDepto = permutaData["destinoDepartamento"] as? String ?: ""
                    val infoExtra = permutaData["infoAdicional"] as? String ?: ""
                    val timeMillis = (permutaData["timestamp"] as? Long) ?: System.currentTimeMillis()
                    val creatorUid = permutaData["creatorUid"] as? String ?: "unknown_uid"
                    val permutaId = permutaData["permutaId"] as? String ?: "legacy_${timeMillis}"
                    
                    val fechaFormateada = SimpleDateFormat("d MMM. yyyy", Locale("es", "ES")).format(Date(timeMillis))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                            // Cabecera: Icono y Ruta
                            Row(verticalAlignment = Alignment.Top) {
                                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(BluePrimary), contentAlignment = Alignment.Center) {
                                    val dispIndex = index + 1
                                    Text(
                                        text = "$dispIndex",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = origenCiudad,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = BluePrimary,
                                            fontSize = 18.sp
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = destinoCiudad,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = BluePrimary,
                                            fontSize = 18.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        // Para centrar los departamentos debajo de sus ciudades, empujamos el origen con el mismo ancho que el icono de LocationOn
                                        Spacer(modifier = Modifier.width(22.dp))
                                        Box(
                                            modifier = Modifier.background(BlueLight, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = origenDepto,
                                                fontWeight = FontWeight.Medium,
                                                color = BluePrimary,
                                                fontSize = 12.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                        Box(
                                            modifier = Modifier.background(BlueLight, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = destinoDepto,
                                                fontWeight = FontWeight.Medium,
                                                color = BluePrimary,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Info adicional
                            if (infoExtra.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = infoExtra,
                                    color = TextSecondary,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Pie: Fecha y Etiqueta Disponible
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DateRange, contentDescription = null, tint = TextSecondary.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Publicado el $fechaFormateada",
                                        color = TextSecondary.copy(alpha = 0.8f),
                                        fontSize = 12.sp
                                    )
                                }
                                
                                var cardMenuExpanded by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(onClick = { cardMenuExpanded = true }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = TextSecondary)
                                    }
                                    DropdownMenu(
                                        expanded = cardMenuExpanded,
                                        onDismissRequest = { cardMenuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Ocultar permuta", color = Color(0xFFD32F2F)) },
                                            onClick = {
                                                cardMenuExpanded = false
                                                permutaToHide = permutaId
                                            }
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Botón de Chat (oculto si la permuta es tuya)
                            if (currentUid != creatorUid) {
                                Button(
                                    onClick = {
                                        val safePermutaId = android.net.Uri.encode(permutaId)
                                        val safeCreatorUid = android.net.Uri.encode(creatorUid)
                                        val safeOrigen = android.net.Uri.encode(origenCiudad)
                                        val safeDestino = android.net.Uri.encode(destinoCiudad)
                                        val safeFecha = android.net.Uri.encode(fechaFormateada)
                                        val safeInfo = if (infoExtra.isBlank()) "sin_info" else android.net.Uri.encode(infoExtra)
                                        onNavigateToChat("chat/$safePermutaId/$safeCreatorUid/$safeOrigen/$safeDestino/$safeFecha/$safeInfo")
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Abrir chat", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Esta es tu publicación", color = TextSecondary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
        
        if (permutaToHide != null) {
            AlertDialog(
                onDismissRequest = { permutaToHide = null },
                title = { Text("Ocultar permuta", fontWeight = FontWeight.Bold) },
                text = { Text("¿Quieres ocultar esta publicación de tu lista? Seguirá siendo visible para los demás compañeros.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val pId = permutaToHide
                            if (pId != null && currentUid.isNotEmpty()) {
                                scope.launch {
                                    repository.hidePermuta(currentUid, pId)
                                    Toast.makeText(context, "Publicación oculta", Toast.LENGTH_SHORT).show()
                                    permutaToHide = null
                                }
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD32F2F))
                    ) {
                        Text("Ocultar", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { permutaToHide = null }) {
                        Text("Cancelar", color = BluePrimary)
                    }
                }
            )
        }
    }
}