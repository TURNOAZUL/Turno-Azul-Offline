package com.calendarioazulpixel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calendarioazulpixel.data.local.PreferencesRepository
import com.calendarioazulpixel.domain.model.Compensatorio
import com.calendarioazulpixel.ui.theme.BluePrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompensatoriosScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { PreferencesRepository(context) }
    val scope = rememberCoroutineScope()
    
    val rawCompensatorios by repository.compensatoriosFlow.collectAsState(initial = emptyList())
    val compensatorios = rawCompensatorios.mapNotNull { Compensatorio.fromStringData(it) }
    
    var hasCheckedDefaults by remember { mutableStateOf(false) }
    
    LaunchedEffect(rawCompensatorios) {
        if (!hasCheckedDefaults && rawCompensatorios.isNotEmpty()) {
            val defaults = repository.getDefaultCompensatorios()
            var needsSave = false
            val updatedList = compensatorios.toMutableList()

            defaults.forEach { defaultItem ->
                // Recupera los predeterminados que falten, comparando por nombre para no duplicar
                if (updatedList.none { it.nombre == defaultItem.nombre }) {
                    updatedList.add(defaultItem)
                    needsSave = true
                }
            }

            if (needsSave) {
                repository.saveCompensatorios(updatedList.map { it.toStringData() })
            }
            hasCheckedDefaults = true
        }
    }
    
    val utilizadosCount = compensatorios.count { it.isUtilizado }
    val pendientesCount = compensatorios.count { !it.isUtilizado }

    var showAddDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var itemToToggleState by remember { mutableStateOf<Compensatorio?>(null) }
    var itemToEdit by remember { mutableStateOf<Compensatorio?>(null) }
    var itemToDelete by remember { mutableStateOf<Compensatorio?>(null) }

    fun saveList(list: List<Compensatorio>) {
        scope.launch {
            repository.saveCompensatorios(list.map { it.toStringData() })
        }
    }

    if (showAddDialog) {
        var nombre by remember { mutableStateOf("") }
        var icono by remember { mutableStateOf("🌟") }
        
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Agregar Compensatorio", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre", color = Color.White) },
                        textStyle = LocalTextStyle.current.copy(color = Color.White),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = icono,
                        onValueChange = { icono = it },
                        label = { Text("Icono (Emoji)", color = Color.White) },
                        textStyle = LocalTextStyle.current.copy(color = Color.White),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (nombre.isNotBlank()) {
                        val nuevo = Compensatorio(
                            id = java.util.UUID.randomUUID().toString(),
                            nombre = nombre,
                            icono = icono,
                            isUtilizado = false,
                            esFijo = false
                        )
                        val newList = compensatorios.toMutableList()
                        newList.add(nuevo)
                        saveList(newList)
                        showAddDialog = false
                    }
                }) { Text("Agregar", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancelar", color = Color.White) }
            }
        )
    }

    if (itemToEdit != null) {
        val comp = itemToEdit!!
        var nombre by remember { mutableStateOf(comp.nombre) }
        var icono by remember { mutableStateOf(comp.icono) }
        
        AlertDialog(
            onDismissRequest = { itemToEdit = null },
            title = { Text("Editar Compensatorio", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre", color = Color.White) },
                        textStyle = LocalTextStyle.current.copy(color = Color.White),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = icono,
                        onValueChange = { icono = it },
                        label = { Text("Icono (Emoji)", color = Color.White) },
                        textStyle = LocalTextStyle.current.copy(color = Color.White),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (nombre.isNotBlank()) {
                        val newList = compensatorios.map { 
                            if (it.id == comp.id) it.copy(nombre = nombre, icono = icono) else it 
                        }
                        saveList(newList)
                        itemToEdit = null
                    }
                }) { Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { itemToEdit = null }) { Text("Cancelar", color = Color.White) }
            }
        )
    }

    if (itemToToggleState != null) {
        val comp = itemToToggleState!!
        val newStateText = if (comp.isUtilizado) "Pendiente" else "Utilizado"
        AlertDialog(
            onDismissRequest = { itemToToggleState = null },
            title = { Text("Cambiar Estado", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { 
                Text("¿Deseas cambiar el estado de '${comp.nombre}' a $newStateText?", color = Color.White) 
            },
            confirmButton = {
                TextButton(onClick = {
                    val newList = compensatorios.map { if (it.id == comp.id) it.copy(isUtilizado = !it.isUtilizado) else it }
                    saveList(newList)
                    itemToToggleState = null
                }) { Text("Confirmar", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { itemToToggleState = null }) { Text("Cancelar", color = Color.White) }
            }
        )
    }

    if (itemToDelete != null) {
        val comp = itemToDelete!!
        val isDefault = comp.esFijo
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Eliminar Compensatorio", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { 
                Text(
                    text = if (isDefault) 
                        "¿Estás seguro de que deseas eliminar '${comp.nombre}'?\n\nPodrás recuperarlo más adelante usando el botón Restablecer."
                    else 
                        "¿Estás seguro de que deseas eliminar '${comp.nombre}'?", 
                    color = Color.White
                ) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newList = compensatorios.filter { it.id != comp.id }
                        saveList(newList)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) { Text("Eliminar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) { Text("Cancelar", color = Color.White) }
            }
        )
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restablecer", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { 
                Text("¿Deseas recuperar los compensatorios predeterminados que faltan?", color = Color.White) 
            },
            confirmButton = {
                TextButton(onClick = {
                    val defaults = repository.getDefaultCompensatorios()
                    val defaultNames = defaults.map { it.nombre }
                    
                    val finalList = mutableListOf<Compensatorio>()
                    
                    // 1. Agregar los predeterminados en su orden original estricto
                    defaults.forEach { defaultItem ->
                        val existingItem = compensatorios.find { it.nombre == defaultItem.nombre }
                        if (existingItem != null) {
                            // Si existe, conservar el que ya tiene el usuario (con su estado y ID)
                            finalList.add(existingItem)
                        } else {
                            // Si falta, insertarlo como pendiente
                            finalList.add(defaultItem.copy(isUtilizado = false))
                        }
                    }
                    
                    // 2. Agregar los personalizados del usuario al final de la lista
                    val customItems = compensatorios.filter { it.nombre !in defaultNames }
                    finalList.addAll(customItems)

                    // 3. Guardar la lista reorganizada
                    saveList(finalList)
                    showRestoreDialog = false
                }) { Text("Confirmar", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) { Text("Cancelar", color = Color.White) }
            }
        )
    }

    Scaffold(
        containerColor = Color(0xFFF4F6FA),
        topBar = {
            TopAppBar(
                title = { Text("Compensatorios", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                actions = {
                    TextButton(onClick = { showRestoreDialog = true }) {
                        Text("Restablecer", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BluePrimary)
            )
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Agregar más compensatorio", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE5F2FC)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Resumen anual", fontWeight = FontWeight.Bold, color = BluePrimary, fontSize = 15.sp, maxLines = 1)
                            Text("Compensatorios programados", fontSize = 11.sp, color = Color.Gray, lineHeight = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .background(Color.White, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Utilizados", fontSize = 11.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                                }
                                Text(utilizadosCount.toString(), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BluePrimary)
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .background(Color.White, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pendientes", fontSize = 11.sp, color = BluePrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                                }
                                Text(pendientesCount.toString(), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BluePrimary)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(compensatorios) { item ->
                CompensatorioItem(
                    item = item,
                    onToggle = { itemToToggleState = item },
                    onEdit = { itemToEdit = item },
                    onDelete = { itemToDelete = item }
                )
            }
        }
    }
}

@Composable
fun CompensatorioItem(
    item: Compensatorio,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFF0F4F8), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = item.icono, fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = item.nombre,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = BluePrimary,
                lineHeight = 16.sp,
                maxLines = 2
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            val statusColor = if (item.isUtilizado) Color(0xFF4CAF50) else BluePrimary
            val statusBgColor = if (item.isUtilizado) Color(0xFFE8F5E9) else Color(0xFFE3F2FD)
            val statusIcon = if (item.isUtilizado) Icons.Default.CheckCircle else Icons.Default.Schedule
            
            Box(
                modifier = Modifier
                    .background(statusBgColor, RoundedCornerShape(12.dp))
                    .clickable { onToggle() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (item.isUtilizado) "Utilizado" else "Pendiente",
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(6.dp))
            
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFFF4F6FA), RoundedCornerShape(8.dp))
                    .clickable { onEdit() }
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = BluePrimary, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp))
                    .clickable { onDelete() }
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red, modifier = Modifier.size(18.dp))
            }
        }
    }
}
