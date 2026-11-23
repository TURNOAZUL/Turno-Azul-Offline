package com.calendarioazulpixel.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calendarioazulpixel.data.local.PreferencesRepository
import com.calendarioazulpixel.domain.model.AppFeature
import com.calendarioazulpixel.domain.model.FeatureCatalog
import com.calendarioazulpixel.ui.theme.BackgroundLight
import com.calendarioazulpixel.ui.theme.BlueLight
import com.calendarioazulpixel.ui.theme.BluePrimary
import com.calendarioazulpixel.ui.theme.TextPrimary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalizarAccesosScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { PreferencesRepository(context) }
    val scope = rememberCoroutineScope()

    var selectedFeatures by remember { mutableStateOf<List<AppFeature>>(emptyList()) }
    var availableFeatures by remember { mutableStateOf<List<AppFeature>>(emptyList()) }
    var isLoaded by remember { mutableStateOf(false) }

    // Cargar las preferencias iniciales
    LaunchedEffect(Unit) {
        val currentIds = repository.shortcuts.first()
        selectedFeatures = currentIds.mapNotNull { FeatureCatalog.getById(it) }
        availableFeatures = FeatureCatalog.features.filter { it !in selectedFeatures }
        isLoaded = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personalizar Accesos", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BluePrimary)
            )
        },
        bottomBar = {
            BottomActionBar(
                onCancel = onNavigateBack,
                onSave = {
                    if (selectedFeatures.size != 5) {
                        Toast.makeText(context, "Debes seleccionar exactamente 5 accesos", Toast.LENGTH_SHORT).show()
                    } else {
                        scope.launch {
                            repository.saveShortcuts(selectedFeatures.map { it.id })
                            Toast.makeText(context, "Cambios guardados", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        }
                    }
                },
                isSaveEnabled = selectedFeatures.size == 5
            )
        }
    ) { innerPadding ->
        if (!isLoaded) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BluePrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundLight)
                    .padding(innerPadding)
            ) {
                item {
                    SectionHeader(
                        title = "Accesos Seleccionados",
                        subtitle = "Elige exactamente 5. Usa las flechas para ordenar. (${selectedFeatures.size}/5)"
                    )
                }

                items(selectedFeatures) { feature ->
                    SelectedFeatureRow(
                        feature = feature,
                        onMoveUp = {
                            val index = selectedFeatures.indexOf(feature)
                            if (index > 0) {
                                val mutable = selectedFeatures.toMutableList()
                                mutable[index] = mutable[index - 1]
                                mutable[index - 1] = feature
                                selectedFeatures = mutable
                            }
                        },
                        onMoveDown = {
                            val index = selectedFeatures.indexOf(feature)
                            if (index < selectedFeatures.size - 1) {
                                val mutable = selectedFeatures.toMutableList()
                                mutable[index] = mutable[index + 1]
                                mutable[index + 1] = feature
                                selectedFeatures = mutable
                            }
                        },
                        onRemove = {
                            selectedFeatures = selectedFeatures - feature
                            availableFeatures = availableFeatures + feature
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionHeader(
                        title = "Funciones Disponibles",
                        subtitle = "Toca el botón + para agregarlos a tu pantalla de Inicio."
                    )
                }

                items(availableFeatures) { feature ->
                    AvailableFeatureRow(
                        feature = feature,
                        onAdd = {
                            if (selectedFeatures.size < 5) {
                                availableFeatures = availableFeatures - feature
                                selectedFeatures = selectedFeatures + feature
                            } else {
                                Toast.makeText(context, "Ya tienes 5 accesos seleccionados", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
                
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)) {
        Text(text = title, fontWeight = FontWeight.Bold, color = BluePrimary, fontSize = 18.sp)
        Text(text = subtitle, color = TextPrimary, fontSize = 14.sp)
    }
}

@Composable
fun SelectedFeatureRow(
    feature: AppFeature,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(BlueLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = feature.icon, contentDescription = null, tint = BluePrimary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = feature.title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            
            IconButton(onClick = onMoveUp) { Icon(Icons.Default.ArrowUpward, tint = TextPrimary, contentDescription = "Subir") }
            IconButton(onClick = onMoveDown) { Icon(Icons.Default.ArrowDownward, tint = TextPrimary, contentDescription = "Bajar") }
            IconButton(onClick = onRemove) { Icon(Icons.Default.Close, tint = Color.Red, contentDescription = "Quitar") }
        }
    }
}

@Composable
fun AvailableFeatureRow(
    feature: AppFeature,
    onAdd: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = feature.icon, contentDescription = null, tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = feature.title, modifier = Modifier.weight(1f), color = TextPrimary)
            
            IconButton(onClick = onAdd) { Icon(Icons.Default.Add, tint = BluePrimary, contentDescription = "Añadir") }
        }
    }
}

@Composable
fun BottomActionBar(onCancel: () -> Unit, onSave: () -> Unit, isSaveEnabled: Boolean) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancelar", color = TextPrimary)
            }
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                enabled = isSaveEnabled
            ) {
                Text("Guardar cambios", color = Color.White)
            }
        }
    }
}
