package com.calendarioazulpixel

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Guardar el token de FCM en Firestore si el usuario está autenticado
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val db = FirebaseFirestore.getInstance()
            val tokenData = hashMapOf(
                "fcmToken" to token,
                "lastUpdated" to System.currentTimeMillis()
            )
            db.collection("users").document(currentUser.uid)
                .set(tokenData, SetOptions.merge())
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Comprobar si el mensaje contiene datos ("data payload")
        if (remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.data["title"] ?: "Nuevo mensaje en Turno Azul"
            val body = remoteMessage.data["body"] ?: "Tienes un mensaje nuevo"
            
            // Verificamos si el mensaje viene del mismo usuario (para no autonotificarse)
            val senderUid = remoteMessage.data["senderUid"]
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            
            if (senderUid != currentUid) {
                sendNotification(
                    messageTitle = title, 
                    messageBody = body,
                    permutaId = remoteMessage.data["permutaId"],
                    creatorUid = remoteMessage.data["creatorUid"],
                    origen = remoteMessage.data["origen"],
                    destino = remoteMessage.data["destino"],
                    fecha = remoteMessage.data["fecha"],
                    info = remoteMessage.data["info"]
                )
            }
        } 
        // Comprobar si el mensaje contiene una notificación directa ("notification payload")
        else if (remoteMessage.notification != null) {
            val title = remoteMessage.notification?.title ?: "Nuevo mensaje en Turno Azul"
            val body = remoteMessage.notification?.body ?: "Tienes un mensaje nuevo"
            sendNotification(messageTitle = title, messageBody = body)
        }
    }

    private fun sendNotification(
        messageTitle: String, 
        messageBody: String, 
        permutaId: String? = null,
        creatorUid: String? = null,
        origen: String? = null,
        destino: String? = null,
        fecha: String? = null,
        info: String? = null
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            
            // Inyectamos la ruta del chat si existen todos los parámetros
            if (permutaId != null && creatorUid != null && origen != null && destino != null) {
                putExtra("navigate_to", "chat")
                putExtra("permutaId", permutaId)
                putExtra("creatorUid", creatorUid)
                putExtra("origen", origen)
                putExtra("destino", destino)
                putExtra("fecha", fecha ?: "")
                putExtra("info", info ?: "")
            }
        }
        
        // Configurar el PendingIntent para abrir la app.
        // Podrías pasar extras para que MainActivity navegue directo al chat.
        val pendingIntent = PendingIntent.getActivity(
            this, 0 /* Request code */, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        )

        val channelId = "turno_azul_chat_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        // Usamos un icono por defecto del sistema si no tienes un drawable específico configurado
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) 
            .setContentTitle(messageTitle)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Desde Android Oreo (API 26) es obligatorio crear un NotificationChannel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Mensajes de Chat",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de mensajes de permutas"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Mostrar la notificación con un ID único basado en el tiempo
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}