package com.calendarioazulpixel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calendarioazulpixel.ui.theme.BackgroundLight
import com.calendarioazulpixel.ui.theme.BluePrimary
import com.calendarioazulpixel.ui.theme.BlueSecondary
import com.calendarioazulpixel.ui.theme.TextPrimary
import com.calendarioazulpixel.ui.theme.TextSecondary
import com.calendarioazulpixel.data.local.DirectorioInpec
import androidx.compose.ui.platform.LocalContext
import com.calendarioazulpixel.data.local.PreferencesRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroPermutaScreen(onNavigateBack: () -> Unit, onNavigateToDisponibles: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { PreferencesRepository(context) }
    val scope = rememberCoroutineScope()
    val db = remember { FirebaseFirestore.getInstance() }
    val currentUserUid = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    
    // Migración de datos antiguos de DataStore a Firestore
    LaunchedEffect(Unit) {
        if (currentUserUid.isNotEmpty()) {
            val antiguas = repository.permutasGuardadas.first()
            if (antiguas.isNotEmpty()) {
                val batch = db.batch()
                var hasValidData = false
                for (permutaStr in antiguas) {
                    val partes = permutaStr.split(";;")
                    val ruta = partes.getOrNull(0) ?: continue
                    val info = partes.getOrNull(1) ?: ""
                    val time = partes.getOrNull(2)?.toLongOrNull() ?: System.currentTimeMillis()
                    val cUid = partes.getOrNull(3) ?: currentUserUid
                    val pId = partes.getOrNull(4) ?: java.util.UUID.randomUUID().toString()

                    val rutaPartes = ruta.split(" -> ")
                    val origen = rutaPartes.getOrNull(0) ?: ""
                    val destino = rutaPartes.getOrNull(1) ?: ""

                    val oCiudad = origen.substringBefore(",").trim()
                    val oDepto = origen.substringAfter(",", "").trim()
                    val dCiudad = destino.substringBefore(",").trim()
                    val dDepto = destino.substringAfter(",", "").trim()

                    val data = mapOf(
                        "permutaId" to pId,
                        "creatorUid" to cUid,
                        "origenCiudad" to oCiudad,
                        "origenDepartamento" to oDepto,
                        "destinoCiudad" to dCiudad,
                        "destinoDepartamento" to dDepto,
                        "infoAdicional" to info,
                        "timestamp" to time
                    )
                    val docRef = db.collection("permutas").document(pId)
                    batch.set(docRef, data)
                    hasValidData = true
                }
                if (hasValidData) {
                    batch.commit().addOnSuccessListener {
                        scope.launch {
                            repository.savePermutas(emptyList())
                        }
                    }
                }
            }
        }
    }

    // Estado de los selectores (por ahora estructura vacía)
    var origenDepartamento by remember { mutableStateOf("") }
    var origenCiudad by remember { mutableStateOf("") }
    var destinoDepartamento by remember { mutableStateOf("") }
    var destinoCiudad by remember { mutableStateOf("") }
    
    // Texto adicional
    var infoAdicional by remember { mutableStateOf("") }

    // Mensajes de error
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showSuccessDialog by remember { mutableStateOf(false) }

    // Menús desplegables
    var origenDepExpanded by remember { mutableStateOf(false) }
    var destinoDepExpanded by remember { mutableStateOf(false) }
    var origenCiudadExpanded by remember { mutableStateOf(false) }
    var destinoCiudadExpanded by remember { mutableStateOf(false) }

    // Datos oficiales INPEC
    val departamentosList = DirectorioInpec.departamentos
    val ciudadesOrigenList = DirectorioInpec.getCiudadesPorDepartamento(origenDepartamento)
    val ciudadesDestinoList = DirectorioInpec.getCiudadesPorDepartamento(destinoDepartamento)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registro de permuta", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White)
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

            if (errorMessage != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Text(text = errorMessage!!, color = Color(0xFFD32F2F), modifier = Modifier.padding(12.dp))
                    }
                }
            }

            // --- DEPARTAMENTO ORIGEN ---
            item {
                Text("Departamento donde te encuentras", fontWeight = FontWeight.Bold, color = BluePrimary)
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = origenDepExpanded,
                    onExpandedChange = { origenDepExpanded = it }
                ) {
                    OutlinedTextField(
                        value = if (origenDepartamento.isEmpty()) "Selecciona un departamento" else origenDepartamento,
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = BlueSecondary) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = BlueSecondary) },
                        modifier = Modifier.fillMaxWidth().menuAnchor().clickable { origenDepExpanded = true },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = TextPrimary,
                            disabledBorderColor = BlueSecondary.copy(alpha = 0.5f)
                        ),
                        enabled = false // Bloqueado para simular Click como botón
                    )
                    ExposedDropdownMenu(
                        expanded = origenDepExpanded,
                        onDismissRequest = { origenDepExpanded = false }
                    ) {
                        departamentosList.forEach { dep ->
                            DropdownMenuItem(
                                text = { Text(dep) },
                                onClick = {
                                    origenDepartamento = dep
                                    origenCiudad = "" // Limpiar ciudad al cambiar departamento
                                    origenDepExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- CIUDAD ORIGEN ---
            item {
                Text("Ciudad donde te encuentras", fontWeight = FontWeight.Bold, color = BluePrimary)
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = origenCiudadExpanded && origenDepartamento.isNotEmpty(),
                    onExpandedChange = { if (origenDepartamento.isNotEmpty()) origenCiudadExpanded = it }
                ) {
                    OutlinedTextField(
                        value = if (origenDepartamento.isEmpty()) "Primero selecciona un departamento" else if (origenCiudad.isEmpty()) "Selecciona una ciudad" else origenCiudad,
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = if (origenDepartamento.isEmpty()) Color.Gray else BlueSecondary) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = if (origenDepartamento.isEmpty()) Color.Gray else BlueSecondary) },
                        modifier = Modifier.fillMaxWidth().menuAnchor().clickable(enabled = origenDepartamento.isNotEmpty()) { origenCiudadExpanded = true },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = if (origenDepartamento.isEmpty()) Color.Gray else TextPrimary,
                            disabledBorderColor = if (origenDepartamento.isEmpty()) Color.LightGray else BlueSecondary.copy(alpha = 0.5f)
                        ),
                        enabled = false
                    )
                    ExposedDropdownMenu(
                        expanded = origenCiudadExpanded,
                        onDismissRequest = { origenCiudadExpanded = false }
                    ) {
                        ciudadesOrigenList.forEach { ciu ->
                            DropdownMenuItem(
                                text = { Text(ciu) },
                                onClick = {
                                    origenCiudad = ciu
                                    origenCiudadExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- DEPARTAMENTO DESTINO ---
            item {
                Text("Departamento de destino", fontWeight = FontWeight.Bold, color = BluePrimary)
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = destinoDepExpanded,
                    onExpandedChange = { destinoDepExpanded = it }
                ) {
                    OutlinedTextField(
                        value = if (destinoDepartamento.isEmpty()) "Selecciona un departamento" else destinoDepartamento,
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = BlueSecondary) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = BlueSecondary) },
                        modifier = Modifier.fillMaxWidth().menuAnchor().clickable { destinoDepExpanded = true },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = TextPrimary,
                            disabledBorderColor = BlueSecondary.copy(alpha = 0.5f)
                        ),
                        enabled = false
                    )
                    ExposedDropdownMenu(
                        expanded = destinoDepExpanded,
                        onDismissRequest = { destinoDepExpanded = false }
                    ) {
                        departamentosList.forEach { dep ->
                            DropdownMenuItem(
                                text = { Text(dep) },
                                onClick = {
                                    destinoDepartamento = dep
                                    destinoCiudad = "" // Limpiar ciudad
                                    destinoDepExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- CIUDAD DESTINO ---
            item {
                Text("Ciudad de destino", fontWeight = FontWeight.Bold, color = BluePrimary)
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = destinoCiudadExpanded && destinoDepartamento.isNotEmpty(),
                    onExpandedChange = { if (destinoDepartamento.isNotEmpty()) destinoCiudadExpanded = it }
                ) {
                    OutlinedTextField(
                        value = if (destinoDepartamento.isEmpty()) "Primero selecciona un departamento" else if (destinoCiudad.isEmpty()) "Selecciona una ciudad" else destinoCiudad,
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = if (destinoDepartamento.isEmpty()) Color.Gray else BlueSecondary) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = if (destinoDepartamento.isEmpty()) Color.Gray else BlueSecondary) },
                        modifier = Modifier.fillMaxWidth().menuAnchor().clickable(enabled = destinoDepartamento.isNotEmpty()) { destinoCiudadExpanded = true },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = if (destinoDepartamento.isEmpty()) Color.Gray else TextPrimary,
                            disabledBorderColor = if (destinoDepartamento.isEmpty()) Color.LightGray else BlueSecondary.copy(alpha = 0.5f)
                        ),
                        enabled = false
                    )
                    ExposedDropdownMenu(
                        expanded = destinoCiudadExpanded,
                        onDismissRequest = { destinoCiudadExpanded = false }
                    ) {
                        ciudadesDestinoList.forEach { ciu ->
                            DropdownMenuItem(
                                text = { Text(ciu) },
                                onClick = {
                                    destinoCiudad = ciu
                                    destinoCiudadExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- INFORMACIÓN ADICIONAL ---
            item {
                Text("Información adicional (opcional)", fontWeight = FontWeight.Bold, color = BluePrimary)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = infoAdicional,
                    onValueChange = { if (it.length <= 200) infoAdicional = it },
                    placeholder = { Text("Ejemplo: Busco permuta del establecimiento de (Nombre establecimiento) para el establecimiento de (Nombre establecimiento). Dejar contacto.", fontSize = 13.sp, color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        cursorColor = BluePrimary,
                        focusedBorderColor = BluePrimary,
                        unfocusedBorderColor = Color.LightGray,
                        focusedContainerColor = BackgroundLight,
                        unfocusedContainerColor = BackgroundLight
                    )
                )
                Text(
                    text = "${infoAdicional.length}/200",
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            // --- BOTÓN PUBLICAR ---
            item {
                Button(
                    onClick = {
                        errorMessage = null
                        if (origenDepartamento.isEmpty() || origenCiudad.isEmpty() || destinoDepartamento.isEmpty() || destinoCiudad.isEmpty()) {
                            errorMessage = "Por favor, completa todos los campos de ubicación."
                        } else if (origenDepartamento == destinoDepartamento && origenCiudad == destinoCiudad) {
                            errorMessage = "El origen y el destino no pueden ser exactamente el mismo lugar."
                        } else {
                            scope.launch {
                                if (currentUserUid.isEmpty()) {
                                    errorMessage = "Error: Usuario no autenticado."
                                    return@launch
                                }
                                
                                db.collection("permutas")
                                    .whereEqualTo("creatorUid", currentUserUid)
                                    .get()
                                    .addOnSuccessListener { snapshot ->
                                        if (snapshot.size() >= 5) {
                                            errorMessage = "Ya tienes 5 permutas registradas. Elimina una de tus solicitudes antes de registrar otra."
                                        } else {
                                            val infoNormalized = infoAdicional.trim().lowercase()
                                            var isDuplicate = false
                                            for (doc in snapshot.documents) {
                                                val oC = doc.getString("origenCiudad") ?: ""
                                                val oD = doc.getString("origenDepartamento") ?: ""
                                                val dC = doc.getString("destinoCiudad") ?: ""
                                                val dD = doc.getString("destinoDepartamento") ?: ""
                                                val info = (doc.getString("infoAdicional") ?: "").trim().lowercase()
                                                
                                                if (oC == origenCiudad && oD == origenDepartamento && 
                                                    dC == destinoCiudad && dD == destinoDepartamento &&
                                                    info == infoNormalized) {
                                                    isDuplicate = true
                                                    break
                                                }
                                            }
                                            
                                            if (isDuplicate) {
                                                errorMessage = "Esta permuta ya está registrada con los mismos datos. Puedes consultarla en Permutas registradas o modificar la información si necesitas publicar una solicitud diferente."
                                            } else {
                                                val permutaId = java.util.UUID.randomUUID().toString()
                                                val data = mapOf(
                                                    "permutaId" to permutaId,
                                                    "creatorUid" to currentUserUid,
                                                    "origenCiudad" to origenCiudad,
                                                    "origenDepartamento" to origenDepartamento,
                                                    "destinoCiudad" to destinoCiudad,
                                                    "destinoDepartamento" to destinoDepartamento,
                                                    "infoAdicional" to infoAdicional,
                                                    "timestamp" to System.currentTimeMillis()
                                                )
                                                db.collection("permutas").document(permutaId).set(data)
                                                    .addOnSuccessListener {
                                                        onNavigateToDisponibles()
                                                    }
                                                    .addOnFailureListener { e ->
                                                        errorMessage = "Error al publicar: ${e.message}"
                                                    }
                                            }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        errorMessage = "Error al verificar permutas: ${e.message}"
                                    }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Publicar permuta", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
