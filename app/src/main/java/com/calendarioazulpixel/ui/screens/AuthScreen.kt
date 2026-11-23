package com.calendarioazulpixel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calendarioazulpixel.ui.theme.BackgroundLight
import com.calendarioazulpixel.ui.theme.BluePrimary
import com.google.firebase.auth.FirebaseAuth
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import com.calendarioazulpixel.NotificationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(onLoginSuccess: () -> Unit) {
    var isLogin by remember { mutableStateOf(true) } // true para Login, false para Registro
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val activityContext = LocalContext.current as? Activity
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val auth = remember { FirebaseAuth.getInstance() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Título o Logo
        Text(
            text = "Turno Azul",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = BluePrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isLogin) "Inicia sesión para continuar" else "Crea una cuenta nueva",
            fontSize = 16.sp,
            color = Color.DarkGray
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Tarjeta de Formulario
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Selector de modo
                TabRow(
                    selectedTabIndex = if (isLogin) 0 else 1,
                    containerColor = Color.Transparent,
                    indicator = { tabPositions ->
                        SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[if (isLogin) 0 else 1]),
                            color = BluePrimary
                        )
                    }
                ) {
                    Tab(
                        selected = isLogin,
                        onClick = { 
                            isLogin = true
                            errorMessage = null 
                        },
                        text = { Text("Ingresar", color = if (isLogin) BluePrimary else Color.DarkGray, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = !isLogin,
                        onClick = { 
                            isLogin = false
                            errorMessage = null 
                        },
                        text = { Text("Registrar Datos", color = if (!isLogin) BluePrimary else Color.DarkGray, fontWeight = FontWeight.Bold) }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))

                // Campo Correo
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo electrónico", color = Color.DarkGray) },
                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = BluePrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Campo Contraseña
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña", color = Color.DarkGray) },
                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = BluePrimary) },
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(icon, contentDescription = "Ver contraseña", tint = Color.Gray)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )

                // Confirmar contraseña (sólo registro)
                if (!isLogin) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirmar contraseña", color = Color.DarkGray) },
                        textStyle = LocalTextStyle.current.copy(color = Color.Black),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = BluePrimary) },
                        trailingIcon = {
                            val icon = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(icon, contentDescription = "Ver contraseña", tint = Color.Gray)
                            }
                        },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Mensaje de Error
                if (errorMessage != null) {
                    Text(text = errorMessage!!, color = Color.Red, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Botón Acción
                Button(
                    onClick = {
                        errorMessage = null
                        if (email.isBlank() || password.isBlank()) {
                            errorMessage = "Por favor, completa todos los campos."
                            return@Button
                        }
                        if (!isLogin && password != confirmPassword) {
                            errorMessage = "Las contraseñas no coinciden."
                            return@Button
                        }
                        if (password.length < 6) {
                            errorMessage = "La contraseña debe tener al menos 6 caracteres."
                            return@Button
                        }

                        isLoading = true
                        if (isLogin) {
                            auth.signInWithEmailAndPassword(email.trim(), password)
                                .addOnCompleteListener { task ->
                                    isLoading = false
                                    if (task.isSuccessful) {
                                        try {
                                            activityContext?.let { NotificationHelper.askForNotificationPermission(it) }
                                        } catch (e: Exception) {}
                                        onLoginSuccess()
                                    } else {
                                        errorMessage = task.exception?.localizedMessage ?: "Error al iniciar sesión."
                                    }
                                }
                        } else {
                            auth.createUserWithEmailAndPassword(email.trim(), password)
                                .addOnCompleteListener { task ->
                                    isLoading = false
                                    if (task.isSuccessful) {
                                        try {
                                            activityContext?.let { NotificationHelper.askForNotificationPermission(it) }
                                        } catch (e: Exception) {}
                                        onLoginSuccess()
                                    } else {
                                        errorMessage = task.exception?.localizedMessage ?: "Error al crear cuenta."
                                    }
                                }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (isLogin) "Ingresar" else "Registrar Datos", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
