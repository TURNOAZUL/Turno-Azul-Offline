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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.calendarioazulpixel.ui.theme.BackgroundLight
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
fun PermutasRegistradasScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { PreferencesRepository(context) }
    var permutasGuardadas by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    val scope = rememberCoroutineScope()
    var itemToDelete by remember { mutableStateOf<Map<String, Any>?>(null) }
    
    val currentUid = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val db = remember { FirebaseFirestore.getInstance() }

    // Cargar la lista al iniciar la pantalla desde Firestore
    LaunchedEffect(Unit) {
        if (currentUid.isNotEmpty()) {
            db.collection("permutas")
                .whereEqualTo("creatorUid", currentUid)
                .addSnapshotListener { snapshot, e ->
                    if (e != null || snapshot == null) {
                        return@addSnapshotListener
                    }
                    val lista = snapshot.documents.mapNotNull { doc ->
                        doc.data
                    }.sortedByDescending { (it["timestamp"] as? Long) ?: 0L }
                    permutasGuardadas = lista
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permutas registradas", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
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
                        text = "Todavía no has registrado ninguna permuta.",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Esta pantalla muestra únicamente las permutas que has publicado desde este dispositivo (almacenamiento local).",
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
                    Text(
                        text = "Mis permutas activas:",
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary,
                        fontSize = 16.sp
                    )
                    Box(
                        modifier = Modifier
                            .background(com.calendarioazulpixel.ui.theme.BlueLight, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${permutasGuardadas.size} / 5",
                            fontWeight = FontWeight.ExtraBold,
                            color = BluePrimary,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            itemsIndexed(permutasGuardadas) { index, permutaData ->
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
                                    Text(
                                        text = "${permutasGuardadas.size - index}",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                                        Spacer(modifier = Modifier.width(22.dp))
                                        Box(
                                            modifier = Modifier.background(com.calendarioazulpixel.ui.theme.BlueLight, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp)
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
                                            modifier = Modifier.background(com.calendarioazulpixel.ui.theme.BlueLight, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp)
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
                            
                            // Pie: Fecha
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DateRange, contentDescription = null, tint = TextSecondary.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Publicado el $fechaFormateada",
                                    color = TextSecondary.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Botón de Eliminar
                            Button(
                                onClick = {
                                    itemToDelete = permutaData
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE))
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Eliminar permuta", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                            }
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
        
        if (itemToDelete != null) {
            AlertDialog(
                onDismissRequest = { itemToDelete = null },
                title = { Text("Eliminar permuta", fontWeight = FontWeight.Bold) },
                text = { Text("¿Deseas eliminar esta permuta? Esta acción no se puede deshacer.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val pId = itemToDelete?.get("permutaId") as? String
                            if (pId != null) {
                                db.collection("permutas").document(pId).delete().addOnSuccessListener {
                                    Toast.makeText(context, "Permuta eliminada", Toast.LENGTH_SHORT).show()
                                }
                            }
                            itemToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD32F2F))
                    ) {
                        Text("Eliminar", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemToDelete = null }) {
                        Text("Cancelar", color = BluePrimary)
                    }
                }
            )
        }
    }
}
