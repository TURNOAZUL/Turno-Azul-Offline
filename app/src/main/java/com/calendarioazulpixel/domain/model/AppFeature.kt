package com.calendarioazulpixel.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.calendarioazulpixel.ui.navigation.Screen

/**
 * Catálogo centralizado de todas las funciones disponibles en la aplicación.
 * Permite agregar nuevas pantallas fácilmente y utilizarlas en la personalización.
 */
data class AppFeature(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val route: String
)

object FeatureCatalog {
    val features = listOf(
        AppFeature("calendario", "Mi calendario", Icons.Default.CalendarMonth, Screen.Calendario.route),
        AppFeature("turnos", "Mis turnos", Icons.Default.FormatListBulleted, Screen.Turnos.route),
        AppFeature("compensatorios", "Compensatorios", Icons.Default.Assignment, Screen.Compensatorios.route),
        AppFeature("servicios", "Mis servicios", Icons.Default.Security, Screen.Servicios.route),
        AppFeature("contactos", "Contactos", Icons.Default.Group, Screen.Contactos.route),
        AppFeature("novedades", "Novedades", Icons.Default.Notifications, Screen.Novedades.route),
        AppFeature("chats", "Chats", Icons.Default.Chat, Screen.Chats.route),
        AppFeature("alarmas", "Alarmas", Icons.Default.Alarm, Screen.Alarmas.route),
        AppFeature("clima", "Clima", Icons.Default.Cloud, Screen.Clima.route),
        AppFeature("ayuda", "Ayuda", Icons.Default.HelpOutline, Screen.Ayuda.route),
        AppFeature("permutas", "Permutas", Icons.Default.SyncAlt, Screen.Permutas.route),
        AppFeature("configuracion", "Configuración", Icons.Default.Settings, Screen.Configuracion.route)
    )

    // Obtener una función por su ID
    fun getById(id: String): AppFeature? {
        return features.find { it.id == id }
    }
}
