package com.calendarioazulpixel.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.calendarioazulpixel.ui.screens.*
import com.calendarioazulpixel.ui.screens.calendar.CalendarioScreen
import com.calendarioazulpixel.ui.screens.home.InicioScreen
import com.calendarioazulpixel.ui.screens.profile.PerfilScreen
import com.calendarioazulpixel.ui.screens.shifts.MisTurnosScreen
import com.calendarioazulpixel.ui.theme.BluePrimary
import com.calendarioazulpixel.ui.theme.BlueSecondary

import com.calendarioazulpixel.ui.screens.ConfiguracionScreen
import com.calendarioazulpixel.ui.screens.AuthScreen
import com.google.firebase.auth.FirebaseAuth

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Auth : Screen("auth", "Autenticación")
    // Bottom Navigation Screens
    object Inicio : Screen("inicio", "Inicio", Icons.Default.Home)
    object Calendario : Screen("calendario", "Calendario", Icons.Default.DateRange)
    object Turnos : Screen("turnos", "Mis turnos", Icons.Default.List)
    object Perfil : Screen("perfil", "Mi perfil", Icons.Default.Person)
    
    // Otras pantallas secundarias (accesibles desde "Más")
    object Compensatorios : Screen("compensatorios", "Compensatorios")
    object Novedades : Screen("novedades", "Novedades")
    object Servicios : Screen("servicios", "Servicios")
    object Contactos : Screen("contactos", "Contactos")
    object Chats : Screen("chats", "Chats")
    object Alarmas : Screen("alarmas", "Alarmas")
    object Clima : Screen("clima", "Clima")
    object Ayuda : Screen("ayuda", "Ayuda")
    object Configuracion : Screen("configuracion", "Configuración")
    object Administracion : Screen("administracion", "Administración")
    object Permutas : Screen("permutas", "Permutas")
    object RegistroPermuta : Screen("registro_permuta", "Registro de Permuta")
    object PermutasDisponibles : Screen("permutas_disponibles", "Permutas Disponibles")
    object PermutasRegistradas : Screen("permutas_registradas", "Permutas Registradas")
    object MisConversaciones : Screen("mis_conversaciones", "Mis Conversaciones")
    object Chat : Screen("chat/{permutaId}/{creatorUid}/{origen}/{destino}/{fecha}/{info}", "Chat") {
        fun createRoute(permutaId: String, creatorUid: String, origen: String, destino: String, fecha: String, info: String): String {
            val encPermutaId = android.net.Uri.encode(permutaId)
            val encCreatorUid = android.net.Uri.encode(creatorUid)
            val encOrigen = android.net.Uri.encode(origen)
            val encDestino = android.net.Uri.encode(destino)
            val encFecha = android.net.Uri.encode(fecha)
            val encInfo = if (info.isBlank()) "sin_info" else android.net.Uri.encode(info)
            return "chat/$encPermutaId/$encCreatorUid/$encOrigen/$encDestino/$encFecha/$encInfo"
        }
    }
    
    // Pantalla de configuración de accesos rápidos
    object PersonalizarAccesos : Screen("personalizar_accesos", "Personalizar Accesos")
}

val itemsNav = listOf(
    Screen.Inicio,
    Screen.Calendario,
    Screen.Turnos,
    Screen.Perfil
)

@Composable
fun AppNavigation(initialRoute: String? = null) {
    val navController = rememberNavController()
    val auth = remember { FirebaseAuth.getInstance() }
    
    // Si hay una ruta inicial desde una notificación, arrancamos ahí. Si no, usamos Inicio o Auth
    val startDestination = initialRoute ?: (if (auth.currentUser != null) Screen.Inicio.route else Screen.Auth.route)

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            if (currentRoute != Screen.Auth.route) {
                NavigationBar(
                    containerColor = androidx.compose.ui.graphics.Color.White
                ) {
                    val currentDestination = navBackStackEntry?.destination

                    itemsNav.forEach { screen ->
                        NavigationBarItem(
                            icon = { screen.icon?.let { Icon(it, contentDescription = screen.title) } },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BluePrimary,
                                selectedTextColor = BluePrimary,
                                indicatorColor = BlueSecondary.copy(alpha = 0.2f),
                                unselectedIconColor = androidx.compose.ui.graphics.Color.Gray,
                                unselectedTextColor = androidx.compose.ui.graphics.Color.Gray
                            ),
                            onClick = {
                                navController.navigate(screen.route) {
                                    if (screen.route == Screen.Inicio.route) {
                                        // Si toca Inicio, limpiamos toda la pila de navegación para no apilar subpantallas
                                        popUpTo(Screen.Inicio.route) {
                                            inclusive = true
                                        }
                                        launchSingleTop = true
                                        // No restauramos estado para forzar una pantalla limpia
                                        restoreState = false
                                    } else {
                                        // Comportamiento normal para las demás pestañas
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Auth.route) {
                AuthScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Inicio.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }
                )
            }
            // Pantallas Principales
            composable(Screen.Inicio.route) { 
                InicioScreen(onNavigate = { ruta -> navController.navigate(ruta) }) 
            }
            composable(Screen.Calendario.route) { CalendarioScreen() }
            composable(Screen.Turnos.route) { MisTurnosScreen() }
            composable(Screen.Perfil.route) { 
                PerfilScreen(
                    onSignOut = {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) {
                                inclusive = true
                            }
                        }
                    }
                ) 
            }
            
            // Pantallas Secundarias (Provisionales)
            composable(Screen.Compensatorios.route) { 
                com.calendarioazulpixel.ui.screens.CompensatoriosScreen(
                    onNavigateBack = { navController.popBackStack() }
                ) 
            }
            composable(Screen.Novedades.route) { NovedadesScreen() }
            composable(Screen.Servicios.route) { ServiciosScreen() }
            composable(Screen.Contactos.route) { ContactosScreen() }
            composable(Screen.Chats.route) { ChatsScreen() }
            composable(Screen.Alarmas.route) { 
                com.calendarioazulpixel.ui.screens.AlarmasScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToConfig = { tipoAlarma ->
                        navController.navigate("configurar_alarma/$tipoAlarma")
                    }
                ) 
            }
            
            composable(
                route = "configurar_alarma/{tipoAlarma}",
                arguments = listOf(navArgument("tipoAlarma") { type = NavType.StringType })
            ) { backStackEntry ->
                val tipoAlarma = backStackEntry.arguments?.getString("tipoAlarma") ?: "general"
                com.calendarioazulpixel.ui.screens.ConfigurarAlarmaScreen(
                    tipoAlarma = tipoAlarma,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Clima.route) { ClimaScreen() }
            composable(Screen.Ayuda.route) { AyudaScreen() }
            composable(Screen.Permutas.route) { com.calendarioazulpixel.ui.screens.PermutasScreen(onNavigate = { ruta -> navController.navigate(ruta) }, onNavigateBack = { navController.popBackStack() }) }
            composable(Screen.RegistroPermuta.route) { 
                com.calendarioazulpixel.ui.screens.RegistroPermutaScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDisponibles = { 
                        navController.navigate(Screen.PermutasDisponibles.route) {
                            popUpTo(Screen.Permutas.route)
                        }
                    }
                ) 
            }
            composable(Screen.PermutasDisponibles.route) { 
                com.calendarioazulpixel.ui.screens.PermutasDisponiblesScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToChat = { route -> navController.navigate(route) }
                )
            }
            composable(Screen.PermutasRegistradas.route) {
                com.calendarioazulpixel.ui.screens.PermutasRegistradasScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.MisConversaciones.route) {
                com.calendarioazulpixel.ui.screens.MisConversacionesScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToChat = { route -> navController.navigate(route) }
                )
            }
            composable(
                route = Screen.Chat.route,
                arguments = listOf(
                    navArgument("permutaId") { type = NavType.StringType },
                    navArgument("creatorUid") { type = NavType.StringType },
                    navArgument("origen") { type = NavType.StringType },
                    navArgument("destino") { type = NavType.StringType },
                    navArgument("fecha") { type = NavType.StringType },
                    navArgument("info") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val permutaId = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("permutaId") ?: "", "UTF-8")
                val creatorUid = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("creatorUid") ?: "", "UTF-8")
                val origen = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("origen") ?: "", "UTF-8")
                val destino = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("destino") ?: "", "UTF-8")
                val fecha = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("fecha") ?: "", "UTF-8")
                val infoRaw = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("info") ?: "", "UTF-8")
                val info = if (infoRaw == "sin_info") "" else infoRaw
                
                com.calendarioazulpixel.ui.screens.ChatScreen(
                    onNavigateBack = { navController.popBackStack() },
                    permutaId = permutaId,
                    creatorUid = creatorUid,
                    origen = origen,
                    destino = destino,
                    fecha = fecha,
                    info = info
                )
            }
            composable(Screen.Configuracion.route) { 
                ConfiguracionScreen(onNavigateBack = { navController.popBackStack() }) 
            }
            composable(Screen.Administracion.route) { AdministracionScreen() }
            
            // Pantalla de configuración de accesos rápidos
            composable(Screen.PersonalizarAccesos.route) { 
                com.calendarioazulpixel.ui.screens.home.PersonalizarAccesosScreen(
                    onNavigateBack = { navController.popBackStack() }
                ) 
            }
        }
    }
}
