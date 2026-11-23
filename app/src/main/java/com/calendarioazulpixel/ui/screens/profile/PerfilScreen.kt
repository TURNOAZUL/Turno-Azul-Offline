package com.calendarioazulpixel.ui.screens.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calendarioazulpixel.ui.theme.BackgroundLight
import com.calendarioazulpixel.ui.theme.BlueLight
import com.calendarioazulpixel.ui.theme.BluePrimary
import com.calendarioazulpixel.ui.theme.TextPrimary
import com.calendarioazulpixel.ui.theme.TextSecondary
import com.calendarioazulpixel.data.local.PreferencesRepository
import kotlinx.coroutines.launch

import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(onSignOut: () -> Unit) {
    val context = LocalContext.current
    val preferencesRepository = remember { PreferencesRepository(context) }
    val scope = rememberCoroutineScope()
    
    val savedNombre by preferencesRepository.perfilNombre.collectAsState(initial = null)
    val savedGrado by preferencesRepository.perfilGrado.collectAsState(initial = null)
    val savedEstablecimiento by preferencesRepository.perfilEstablecimiento.collectAsState(initial = null)

    var nombre by remember { mutableStateOf("") }
    var grado by remember { mutableStateOf("") }
    var establecimiento by remember { mutableStateOf("") }
    var dataInitialized by remember { mutableStateOf(false) }
    
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(savedNombre, savedGrado, savedEstablecimiento) {
        if (!dataInitialized && savedNombre != null && savedGrado != null && savedEstablecimiento != null) {
            nombre = savedNombre ?: ""
            grado = savedGrado ?: ""
            establecimiento = savedEstablecimiento ?: ""
            dataInitialized = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi perfil", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BluePrimary)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Avatar de guardián predeterminado
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(BlueLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security, 
                    contentDescription = "Avatar predeterminado",
                    tint = BluePrimary,
                    modifier = Modifier.size(50.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            TextButton(onClick = {
                Toast.makeText(context, "Función de cambiar foto disponible próximamente", Toast.LENGTH_SHORT).show()
            }) {
                Text("Cambiar foto", color = BluePrimary, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Campos Editables
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BlueLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = BluePrimary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedTextField(
                            value = nombre,
                            onValueChange = { nombre = it },
                            label = { Text("Nombre completo") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BluePrimary,
                                unfocusedBorderColor = Color.LightGray
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BlueLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.KeyboardDoubleArrowUp, contentDescription = null, tint = BluePrimary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedTextField(
                            value = grado,
                            onValueChange = { grado = it },
                            label = { Text("Grado y rango") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BluePrimary,
                                unfocusedBorderColor = Color.LightGray
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BlueLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = BluePrimary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedTextField(
                            value = establecimiento,
                            onValueChange = { establecimiento = it },
                            label = { Text("Establecimiento") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BluePrimary,
                                unfocusedBorderColor = Color.LightGray
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                preferencesRepository.savePerfil(nombre, grado, establecimiento)
                                Toast.makeText(context, "Cambios guardados exitosamente", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardar cambios", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Secciones visuales
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text(
                    text = "Ajustes",
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column {
                        ProfileSectionItem(icon = Icons.Default.AccountCircle, title = "Mi cuenta")
                        HorizontalDivider(color = BackgroundLight)
                        ProfileSectionItem(icon = Icons.Default.Tune, title = "Mis preferencias")
                        HorizontalDivider(color = BackgroundLight)
                        ProfileSectionItem(icon = Icons.Default.Lock, title = "Seguridad")
                        HorizontalDivider(color = BackgroundLight)
                        ProfileSectionItem(icon = Icons.Default.Info, title = "Acerca de Turno Azul")
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Cerrar sesión
            Button(
                onClick = {
                    showLogoutDialog = true
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, tint = Color(0xFFD32F2F))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar sesión", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Cerrar sesión", fontWeight = FontWeight.Bold) },
                text = { Text("¿Deseas cerrar sesión en Turno Azul? Tendrás que volver a ingresar tu correo y contraseña la próxima vez.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            FirebaseAuth.getInstance().signOut()
                            showLogoutDialog = false
                            onSignOut()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD32F2F))
                    ) {
                        Text("Cerrar sesión", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancelar", color = BluePrimary)
                    }
                }
            )
        }
    }
}

@Composable
fun ProfileSectionItem(icon: ImageVector, title: String) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { Toast.makeText(context, "Sección: $title", Toast.LENGTH_SHORT).show() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(BlueLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = BluePrimary)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium, color = TextPrimary, fontSize = 15.sp)
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary.copy(alpha = 0.5f))
    }
}
