package com.calendarioazulpixel

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.calendarioazulpixel.data.local.PreferencesRepository
import com.calendarioazulpixel.domain.model.ScreenSizeOption
import com.calendarioazulpixel.ui.navigation.AppNavigation
import com.calendarioazulpixel.ui.theme.CalendarioAzulTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicializar Firebase explícitamente al abrir la aplicación
        FirebaseApp.initializeApp(this)
        
        // Extraer la ruta profunda si la app fue abierta desde una notificación
        val initialRoute = extractRouteFromIntent(intent)
        
        // Solicitar permisos de notificación (Android 13+) y actualizar token FCM
        NotificationHelper.askForNotificationPermission(this)
        
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val repository = remember { PreferencesRepository(context) }
            val screenSizeName by repository.screenSize.collectAsState(initial = ScreenSizeOption.NORMAL.name)
            
            val scaleFactor = try {
                ScreenSizeOption.valueOf(screenSizeName).scaleFactor
            } catch (e: Exception) {
                1.0f
            }

            // Calculamos la densidad base del sistema para no multiplicar sobre multiplicaciones
            val baseDensityObj = Density(context)
            val customDensity = Density(
                density = baseDensityObj.density * scaleFactor,
                fontScale = baseDensityObj.fontScale // Se conserva original para que DP y SP crezcan de forma idéntica
            )

            CalendarioAzulTheme {
                CompositionLocalProvider(LocalDensity provides customDensity) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation(initialRoute = initialRoute)
                    }
                }
            }
        }
    }
    
    // Función para manejar intentos que abren la app cuando ya estaba en segundo plano (SINGLE_TOP)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // En una app más compleja, aquí actualizaríamos un estado (Flow) para forzar la navegación 
        // cuando la app ya está abierta, pero este patrón simple inicialRoute funciona bien
        // para el arranque. (Para robustez completa se recomienda usar Navigation DeepLinks nativos)
    }

    private fun extractRouteFromIntent(intent: Intent?): String? {
        if (intent == null) return null
        
        val navigateTo = intent.getStringExtra("navigate_to")
        if (navigateTo == "chat") {
            val permutaId = intent.getStringExtra("permutaId") ?: return null
            val creatorUid = intent.getStringExtra("creatorUid") ?: return null
            val origen = intent.getStringExtra("origen") ?: return null
            val destino = intent.getStringExtra("destino") ?: return null
            val fecha = intent.getStringExtra("fecha") ?: ""
            val info = intent.getStringExtra("info") ?: ""
            
            val safePermutaId = android.net.Uri.encode(permutaId)
            val safeCreatorUid = android.net.Uri.encode(creatorUid)
            val safeOrigen = android.net.Uri.encode(origen)
            val safeDestino = android.net.Uri.encode(destino)
            val safeFecha = android.net.Uri.encode(fecha)
            val safeInfo = if (info.isBlank()) "sin_info" else android.net.Uri.encode(info)
            
            return "chat/$safePermutaId/$safeCreatorUid/$safeOrigen/$safeDestino/$safeFecha/$safeInfo"
        }
        return null
    }
}
