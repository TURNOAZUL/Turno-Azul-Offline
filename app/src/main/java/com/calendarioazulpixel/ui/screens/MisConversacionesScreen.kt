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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Delete
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
import com.calendarioazulpixel.ui.theme.BackgroundLight
import com.calendarioazulpixel.ui.theme.BlueLight
import com.calendarioazulpixel.ui.theme.BluePrimary
import com.calendarioazulpixel.ui.theme.TextPrimary
import com.calendarioazulpixel.ui.theme.TextSecondary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

data class ChatData(
    val chatId: String,
    val permutaId: String,
    val otherUid: String,
    val origen: String,
    val destino: String,
    val fecha: String,
    val info: String,
    val unreadCount: Long = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisConversacionesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { PreferencesRepository(context) }
    val scope = rememberCoroutineScope()
    
    val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val db = remember { FirebaseFirestore.getInstance() }
    
    var chatsList by remember { mutableStateOf<List<ChatData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val hiddenChats by repository.getHiddenChats(currentUid).collectAsState(initial = emptyList())
    var itemToDelete by remember { mutableStateOf<ChatData?>(null) }

    DisposableEffect(currentUid) {
        if (currentUid.isEmpty()) {
            errorMessage = "Inicia sesión para ver tus conversaciones."
            isLoading = false
            return@DisposableEffect onDispose { }
        }

        val listener = db.collection("chats")
            .whereArrayContains("participants", currentUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    errorMessage = "Error al cargar las conversaciones: ${error.localizedMessage}"
                    isLoading = false
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val loadedChats = snapshot.documents.mapNotNull { doc ->
                        val permutaId = doc.getString("permutaId")
                        val creatorUid = doc.getString("creatorUid")
                        val origen = doc.getString("origen")
                        val destino = doc.getString("destino")
                        
                        val participants = doc.get("participants") as? List<String> ?: emptyList()
                        val otherUid = participants.firstOrNull { it != currentUid } ?: creatorUid ?: currentUid
                        
                        // Evitar mostrar los chats duplicados corruptos (donde el usuario chatea consigo mismo)
                        if (otherUid == currentUid) return@mapNotNull null
                        
                        val unreadCountsMap = doc.get("unreadCounts") as? Map<String, Long>
                        val myUnreadCount = unreadCountsMap?.get(currentUid) ?: 0L

                        // Si es un chat muy viejo sin estos campos, proveemos valores por defecto para que aparezca
                        ChatData(
                            chatId = doc.id,
                            permutaId = permutaId ?: "Desconocido",
                            otherUid = otherUid,
                            origen = origen ?: "Origen desconocido",
                            destino = destino ?: "Destino desconocido",
                            fecha = doc.getString("fecha") ?: "Fecha desconocida",
                            info = doc.getString("info") ?: "",
                            unreadCount = myUnreadCount
                        )
                    }
                    chatsList = loadedChats
                    isLoading = false
                }
            }

        onDispose {
            listener.remove()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis conversaciones", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BluePrimary)
            )
        }
    ) { innerPadding ->
        val visibleChats = chatsList.filter { !hiddenChats.contains(it.chatId) }
        
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
                    textAlign = TextAlign.Center
                )
            } else if (visibleChats.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp)
                ) {
                    Icon(
                        Icons.Default.Email, 
                        contentDescription = null, 
                        tint = TextSecondary.copy(alpha = 0.5f), 
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No tienes conversaciones activas.",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Responde a una permuta disponible o espera a que alguien contacte con la tuya para comenzar a chatear.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    
                    items(visibleChats, key = { it.chatId }) { chat ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val safePermutaId = android.net.Uri.encode(chat.permutaId)
                                    val safeCreatorUid = android.net.Uri.encode(chat.otherUid)
                                    val safeOrigen = android.net.Uri.encode(chat.origen)
                                    val safeDestino = android.net.Uri.encode(chat.destino)
                                    val safeFecha = android.net.Uri.encode(chat.fecha)
                                    val safeInfo = if (chat.info.isBlank()) "sin_info" else android.net.Uri.encode(chat.info)
                                    onNavigateToChat("chat/$safePermutaId/$safeCreatorUid/$safeOrigen/$safeDestino/$safeFecha/$safeInfo")
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(48.dp).clip(CircleShape).background(BluePrimary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Email, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(24.dp))
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${chat.origen.substringBefore(",")} → ${chat.destino.substringBefore(",")}",
                                            fontWeight = if (chat.unreadCount > 0) FontWeight.ExtraBold else FontWeight.Bold,
                                            color = BluePrimary,
                                            fontSize = 16.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (chat.unreadCount > 0) "Tienes mensajes nuevos" else "Abre para leer los mensajes.",
                                            color = if (chat.unreadCount > 0) BluePrimary else TextSecondary,
                                            fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp,
                                            maxLines = 1
                                        )
                                    }
                                    
                                    if (chat.unreadCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color.Red),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(),
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    
                                    var cardMenuExpanded by remember { mutableStateOf(false) }
                                    Box {
                                        IconButton(onClick = { cardMenuExpanded = true }) {
                                            Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = TextSecondary)
                                        }
                                        DropdownMenu(
                                            expanded = cardMenuExpanded,
                                            onDismissRequest = { cardMenuExpanded = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Ocultar conversación", color = Color(0xFFD32F2F)) },
                                                onClick = {
                                                    cardMenuExpanded = false
                                                    itemToDelete = chat
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
        
        if (itemToDelete != null) {
            AlertDialog(
                onDismissRequest = { itemToDelete = null },
                title = { Text("Ocultar conversación", fontWeight = FontWeight.Bold) },
                text = { Text("¿Quieres ocultar esta conversación de tu lista? El otro participante conservará su copia.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val chatId = itemToDelete?.chatId
                            if (chatId != null && currentUid.isNotEmpty()) {
                                scope.launch {
                                    repository.hideChat(currentUid, chatId)
                                    Toast.makeText(context, "Conversación oculta", Toast.LENGTH_SHORT).show()
                                    itemToDelete = null
                                }
                            } else {
                                Toast.makeText(context, "Error al ocultar", Toast.LENGTH_SHORT).show()
                                itemToDelete = null
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD32F2F))
                    ) {
                        Text("Ocultar", fontWeight = FontWeight.Bold)
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