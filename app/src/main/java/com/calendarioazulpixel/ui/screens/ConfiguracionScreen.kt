package com.calendarioazulpixel.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calendarioazulpixel.data.local.PreferencesRepository
import com.calendarioazulpixel.domain.model.ScreenSizeOption
import com.calendarioazulpixel.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracionScreen(onNavigateBack: () -> Unit = {}) {
    val context = LocalContext.current
    val repository = remember { PreferencesRepository(context) }
    val scope = rememberCoroutineScope()

    val savedSizeName by repository.screenSize.collectAsState(initial = ScreenSizeOption.NORMAL.name)
    var previewSize by remember { mutableStateOf<ScreenSizeOption?>(null) }
    
    val activeSize = previewSize ?: try {
        ScreenSizeOption.valueOf(savedSizeName)
    } catch(e: Exception) {
        ScreenSizeOption.NORMAL
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BluePrimary)
            )
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { previewSize = null }) {
                        Text("Cancelar", color = TextPrimary)
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                repository.saveScreenSize(activeSize.name)
                                previewSize = null
                                Toast.makeText(context, "Tamaño guardado", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        enabled = previewSize != null && previewSize?.name != savedSizeName
                    ) {
                        Text("Aplicar tamaño", color = Color.White)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(innerPadding)
        ) {
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Accesibilidad y visualización",
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tamaño de pantalla",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }

            // Vista previa en tiempo real
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.LightGray.copy(alpha = 0.3f))
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val baseDensityObj = Density(context)
                    val customPreviewDensity = Density(
                        density = baseDensityObj.density * activeSize.scaleFactor,
                        fontScale = baseDensityObj.fontScale // Corrección para que botones y textos crezcan proporcionalmente
                    )

                    CompositionLocalProvider(LocalDensity provides customPreviewDensity) {
                        // Tarjeta de Ejemplo (Simulando la principal de Inicio)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .padding(horizontal = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "TU TURNO DE HOY", fontWeight = FontWeight.Bold, color = BluePrimary, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Box(
                                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(BlueSecondary).padding(horizontal = 8.dp, vertical = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = "24H × 48H", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "MIE., 14 OCT.", fontWeight = FontWeight.Bold, color = BluePrimary, fontSize = 14.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Brush.horizontalGradient(colors = listOf(Azul24x48Bg, Azul24x48Bg)), shape = RoundedCornerShape(12.dp))
                                        .padding(20.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.4f)),
                                            contentAlignment = Alignment.Center
                                        ) { Text("💼", fontSize = 32.sp) }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = "DÍA LABORAL", fontWeight = FontWeight.ExtraBold, color = Azul24x48, fontSize = 26.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(horizontalAlignment = Alignment.Start) {
                                                    Text(text = "6:00 AM", fontWeight = FontWeight.Bold, color = Azul24x48, fontSize = 14.sp)
                                                    Text(text = "Mié., 14 oct.", color = Azul24x48.copy(alpha = 0.8f), fontSize = 11.sp)
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(text = "6:00 AM", fontWeight = FontWeight.Bold, color = Azul24x48, fontSize = 14.sp)
                                                    Text(text = "Jue., 15 oct.", color = Azul24x48.copy(alpha = 0.8f), fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {},
                                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Botón de ejemplo", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Opciones
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Elige una opción:",
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ScreenSizeOption.values().forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { previewSize = option }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = activeSize == option,
                                onClick = { previewSize = option },
                                colors = RadioButtonDefaults.colors(selectedColor = BluePrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = option.label,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                            if (savedSizeName == option.name) {
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "Aplicado",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
