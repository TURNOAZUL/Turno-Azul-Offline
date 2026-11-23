package com.calendarioazulpixel.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calendarioazulpixel.ui.theme.BackgroundLight
import com.calendarioazulpixel.ui.theme.BlueLight
import com.calendarioazulpixel.ui.theme.BluePrimary
import com.calendarioazulpixel.ui.theme.TextPrimary
import com.calendarioazulpixel.ui.theme.TextSecondary

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions

data class ChatMessage(val id: String = "", val text: String, val time: String, val isSentByUser: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateBack: () -> Unit,
    permutaId: String,
    creatorUid: String,
    origen: String,
    destino: String,
    fecha: String,
    info: String
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(true) }
    var isSending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val messages = remember { mutableStateListOf<ChatMessage>() }

    val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val db = remember { FirebaseFirestore.getInstance() }
    
    // ID único del chat basado en la permuta y los dos participantes
    val chatId = remember(permutaId, creatorUid, currentUid) {
        if (currentUid < creatorUid) {
            "${permutaId}_${currentUid}_${creatorUid}"
        } else {
            "${permutaId}_${creatorUid}_${currentUid}"
        }
    }

    DisposableEffect(chatId) {
        if (currentUid.isEmpty()) {
            errorMessage = "Error: Usuario no autenticado."
            isLoading = false
            return@DisposableEffect onDispose { }
        }
        
        // Al abrir el chat, reiniciamos el contador de mensajes no leídos del usuario actual
        db.collection("chats").document(chatId)
            .set(hashMapOf("unreadCounts" to hashMapOf(currentUid to 0)), SetOptions.merge())

        val listener = db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        // El chat es nuevo y aún no existe la cabecera en Firestore.
                        // Esto es normal antes del primer mensaje, no bloqueamos la interfaz.
                        errorMessage = null
                    } else {
                        errorMessage = "Error al conectar con el chat: ${error.localizedMessage}"
                    }
                    isLoading = false
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val fetchedMessages = snapshot.documents.map { doc ->
                        val text = doc.getString("text") ?: ""
                        val senderId = doc.getString("senderId") ?: ""
                        val timestamp = doc.getTimestamp("timestamp")?.toDate() ?: Date()
                        val timeFormatted = SimpleDateFormat("h:mm a", Locale("es", "ES")).format(timestamp)
                        ChatMessage(doc.id, text, timeFormatted, senderId == currentUid)
                    }
                    messages.clear()
                    messages.addAll(fetchedMessages)
                    isLoading = false
                }
            }
            
        onDispose {
            listener.remove()
        }
    }

    fun sendMessage() {
        if (inputText.isNotBlank() && currentUid.isNotEmpty() && !isSending) {
            isSending = true
            val textToSend = inputText.trim()
            inputText = "" // Limpiar UI rápido
            
            // Ordenamos los UIDs alfabéticamente para que la lista sea siempre idéntica sin importar quién envíe el mensaje
            val sortedParticipants = if (currentUid < creatorUid) {
                listOf(currentUid, creatorUid)
            } else {
                listOf(creatorUid, currentUid)
            }
            
            val otherUid = if (currentUid == creatorUid) {
                // Si el que envía es el creador de la permuta, el otro es el segundo participante.
                sortedParticipants.firstOrNull { it != currentUid } ?: creatorUid
            } else {
                // Si el que envía NO es el creador, el creador es el otro.
                creatorUid
            }
            
            val chatData = hashMapOf(
                "permutaId" to permutaId,
                "participants" to sortedParticipants,
                "origen" to origen,
                "destino" to destino,
                "fecha" to fecha,
                "info" to info,
                "unreadCounts" to hashMapOf(otherUid to FieldValue.increment(1))
            )
            
            val msgData = hashMapOf(
                "text" to textToSend,
                "senderId" to currentUid,
                "timestamp" to FieldValue.serverTimestamp()
            )
            
            val batch = db.batch()
            // El restartListenerToggle ya no es necesario porque Firebase 
            // leerá el chatId y no bloqueará el listener gracias a las nuevas reglas
            val chatRef = db.collection("chats").document(chatId)
            batch.set(chatRef, chatData, SetOptions.merge())
            
            val msgRef = chatRef.collection("messages").document()
            batch.set(msgRef, msgData)
            
            batch.commit()
                .addOnSuccessListener {
                    isSending = false
                }
                .addOnFailureListener { e ->
                    isSending = false
                    inputText = textToSend // Restaurar el texto si falla
                    Toast.makeText(context, "Error al enviar: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Generar nombre de usuario basado en ciudad de origen
    val ciudadOrigenNombre = origen.substringBefore(",").trim()
    val primeraLetra = if (ciudadOrigenNombre.isNotEmpty()) ciudadOrigenNombre.take(1).uppercase() else "U"
    val nombreUsuario = "Usuario de $ciudadOrigenNombre"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(primeraLetra, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(nombreUsuario, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF4CAF50))) // Indicador verde
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("En línea (Demostración)", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Eliminar chat", color = Color(0xFFD32F2F)) },
                                onClick = { 
                                    menuExpanded = false
                                    // Aquí podríamos agregar la misma lógica de ocultar chat con el DataStore que usamos en MisConversacionesScreen
                                    Toast.makeText(context, "Puedes eliminar este chat desde la pantalla Mis conversaciones", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        // Para evitar que el teclado oculte el campo: navigation / ime padding handled by Scaffold + window soft input mode
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { if (it.length <= 250) inputText = it },
                        placeholder = { Text("Escribe un mensaje...", color = TextSecondary) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = BluePrimary,
                            unfocusedBorderColor = Color.LightGray,
                            focusedContainerColor = BackgroundLight,
                            unfocusedContainerColor = BackgroundLight
                        ),
                        maxLines = 4,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Send),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = { sendMessage() })
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank() && !isSending) BluePrimary else Color.LightGray)
                            .clickable(enabled = inputText.isNotBlank() && !isSending) {
                                sendMessage()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = Color.White)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(innerPadding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = BluePrimary,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    reverseLayout = true // Para que el scroll empiece abajo como en los chats reales
                ) {
                    item { Spacer(modifier = Modifier.height(16.dp)) } // Margen inferior antes de la barra

                    items(messages, key = { it.id.ifEmpty { java.util.UUID.randomUUID().toString() } }) { msg ->
                if (msg.isSentByUser) {
                    // Burbuja enviada
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Column(horizontalAlignment = Alignment.End) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp))
                                    .background(BlueLight)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    color = TextPrimary,
                                    fontSize = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(msg.time, color = TextSecondary, fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("✔️✔️", color = BluePrimary, fontSize = 10.sp) // Leído (Demostración visual)
                            }
                        }
                    }
                } else {
                    // Burbuja recibida
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp))
                                    .background(Color.White)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    color = TextPrimary,
                                    fontSize = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(msg.time, color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Resumen de la Permuta anclada
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BlueLight),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(BluePrimary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "$origen → $destino",
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Permuta publicada el $fecha",
                                color = BluePrimary.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
        } // Fin del else contenedor
        } // Fin del Box principal
    }
}