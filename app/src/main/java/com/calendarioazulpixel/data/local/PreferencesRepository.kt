package com.calendarioazulpixel.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "calendario_azul_prefs")

class PreferencesRepository(private val context: Context) {

    companion object {
        val ACCESOS_RAPIDOS_KEY = stringPreferencesKey("accesos_rapidos")
        const val DEFAULT_SHORTCUTS = "compensatorios,alarmas,contactos,novedades,chats"

        // Tipo de turno activo
        val TIPO_TURNO_ACTIVO_KEY = stringPreferencesKey("tipo_turno_activo")

        // Claves para el Horario 12H x 24H
        val COMPANIA_KEY = stringPreferencesKey("compania_seleccionada")
        val FECHA_INICIO_KEY = longPreferencesKey("fecha_inicio_ciclo")
        val DIA_ENTRADA_KEY = stringPreferencesKey("dia_entrada")
        val DIA_SALIDA_KEY = stringPreferencesKey("dia_salida")
        val NOCHE_ENTRADA_KEY = stringPreferencesKey("noche_entrada")
        val NOCHE_SALIDA_KEY = stringPreferencesKey("noche_salida")
        
        // Claves para Subturnos 12Hx24H
        val SUBTURNO_DIA_KEY = stringPreferencesKey("subturno_dia")
        val SUBTURNO_NOCHE_KEY = stringPreferencesKey("subturno_noche")
        val SUBTURNO_DIA_REF_KEY = longPreferencesKey("subturno_dia_ref")
        val SUBTURNO_NOCHE_REF_KEY = longPreferencesKey("subturno_noche_ref")
        
        // Claves para el Horario 24H x 48H
        val COMPANIA_24_KEY = stringPreferencesKey("compania_24_seleccionada")
        val ENTRADA_24_KEY = stringPreferencesKey("entrada_24")
        val SALIDA_24_KEY = stringPreferencesKey("salida_24")
        val SUBTURNO_24_KEY = stringPreferencesKey("subturno_24")
        val SUBTURNO_24_REF_KEY = longPreferencesKey("subturno_24_ref")
        
        // Claves para el Horario 10D x 5D
        val COMPANIA_10_KEY = stringPreferencesKey("compania_10_seleccionada")
        val ENTRADA_10_KEY = stringPreferencesKey("entrada_10")
        val SALIDA_10_KEY = stringPreferencesKey("salida_10")
        
        // Clave para tamaño de pantalla
        val SCREEN_SIZE_KEY = stringPreferencesKey("screen_size_config")

        // Claves para el Perfil
        val PERFIL_NOMBRE_KEY = stringPreferencesKey("perfil_nombre")
        val PERFIL_GRADO_KEY = stringPreferencesKey("perfil_grado")
        val PERFIL_ESTABLECIMIENTO_KEY = stringPreferencesKey("perfil_establecimiento")
        
        // Claves para Alarmas
        val TIME_FORMAT_24H_KEY = booleanPreferencesKey("time_format_24h")
        fun getAlarmEnabledKey(type: String) = booleanPreferencesKey("${type}_alarm_enabled")
        fun getAlarmHourKey(type: String) = intPreferencesKey("${type}_alarm_hour")
        fun getAlarmMinuteKey(type: String) = intPreferencesKey("${type}_alarm_minute")
        fun getAlarmSoundUriKey(type: String) = stringPreferencesKey("${type}_alarm_sound_uri")
        fun getAlarmSoundNameKey(type: String) = stringPreferencesKey("${type}_alarm_sound_name")
        fun getAlarmVibrateKey(type: String) = booleanPreferencesKey("${type}_alarm_vibrate")
    }

    val shortcuts: Flow<List<String>> = context.dataStore.data
        .map { preferences ->
            val shortcutsString = preferences[ACCESOS_RAPIDOS_KEY] ?: DEFAULT_SHORTCUTS
            shortcutsString.split(",").filter { it.isNotBlank() }
        }

    suspend fun saveShortcuts(shortcutsList: List<String>) {
        withContext(NonCancellable) {
            context.dataStore.edit { preferences ->
                preferences[ACCESOS_RAPIDOS_KEY] = shortcutsList.joinToString(",")
            }
        }
    }
    
    val tipoTurnoActivo: Flow<String> = context.dataStore.data.map { it[TIPO_TURNO_ACTIVO_KEY] ?: "DOCE_VEINTICUATRO" }

    // Leer la configuración del turno 12Hx24H
    val companiaSeleccionada: Flow<String> = context.dataStore.data.map { it[COMPANIA_KEY] ?: "AMARILLA" }
    val diaEntrada: Flow<String> = context.dataStore.data.map { it[DIA_ENTRADA_KEY] ?: "6:00 AM" }
    val diaSalida: Flow<String> = context.dataStore.data.map { it[DIA_SALIDA_KEY] ?: "6:00 PM" }
    val nocheEntrada: Flow<String> = context.dataStore.data.map { it[NOCHE_ENTRADA_KEY] ?: "6:00 PM" }
    val nocheSalida: Flow<String> = context.dataStore.data.map { it[NOCHE_SALIDA_KEY] ?: "6:00 AM" }
    
    // Leer subturnos
    val subturnoDia: Flow<String> = context.dataStore.data.map { it[SUBTURNO_DIA_KEY] ?: "2" }
    val subturnoNoche: Flow<String> = context.dataStore.data.map { it[SUBTURNO_NOCHE_KEY] ?: "4" }
    val fechaRefSubturnoDia: Flow<Long> = context.dataStore.data.map { it[SUBTURNO_DIA_REF_KEY] ?: 0L }
    val fechaRefSubturnoNoche: Flow<Long> = context.dataStore.data.map { it[SUBTURNO_NOCHE_REF_KEY] ?: 0L }

    // Leer configuración del turno 24Hx48H
    val compania24Seleccionada: Flow<String> = context.dataStore.data.map { it[COMPANIA_24_KEY] ?: "AZUL" }
    val entrada24: Flow<String> = context.dataStore.data.map { it[ENTRADA_24_KEY] ?: "6:00 AM" }
    val salida24: Flow<String> = context.dataStore.data.map { it[SALIDA_24_KEY] ?: "6:00 AM" }
    val subturno24: Flow<String> = context.dataStore.data.map { it[SUBTURNO_24_KEY] ?: "2/4" }
    val fechaRefSubturno24: Flow<Long> = context.dataStore.data.map { it[SUBTURNO_24_REF_KEY] ?: 0L }
    
    // Leer configuración del turno 10Dx5D
    val compania10Seleccionada: Flow<String> = context.dataStore.data.map { it[COMPANIA_10_KEY] ?: "ORION" }
    val entrada10: Flow<String> = context.dataStore.data.map { it[ENTRADA_10_KEY] ?: "7:00 AM" }
    val salida10: Flow<String> = context.dataStore.data.map { it[SALIDA_10_KEY] ?: "7:00 PM" }
    
    // Leer configuración de tamaño de pantalla
    val screenSize: Flow<String> = context.dataStore.data.map { it[SCREEN_SIZE_KEY] ?: "NORMAL" }

    // Guardar el tamaño de pantalla
    suspend fun saveScreenSize(size: String) {
        withContext(NonCancellable) {
            context.dataStore.edit { prefs ->
                prefs[SCREEN_SIZE_KEY] = size
            }
        }
    }

    // Leer datos del Perfil
    val perfilNombre: Flow<String> = context.dataStore.data.map { it[PERFIL_NOMBRE_KEY] ?: "" }
    val perfilGrado: Flow<String> = context.dataStore.data.map { it[PERFIL_GRADO_KEY] ?: "" }
    val perfilEstablecimiento: Flow<String> = context.dataStore.data.map { it[PERFIL_ESTABLECIMIENTO_KEY] ?: "" }

    // Guardar datos del Perfil
    suspend fun savePerfil(nombre: String, grado: String, establecimiento: String) {
        withContext(NonCancellable) {
            context.dataStore.edit { prefs ->
                prefs[PERFIL_NOMBRE_KEY] = nombre
                prefs[PERFIL_GRADO_KEY] = grado
                prefs[PERFIL_ESTABLECIMIENTO_KEY] = establecimiento
            }
        }
    }

    // Guardar la configuración del turno 12Hx24H
    suspend fun saveTurnoConfig(
        tipoTurno: String,
        compania: String,
        dEntrada: String,
        dSalida: String,
        nEntrada: String,
        nSalida: String,
        subDia: String,
        subNoche: String,
        refDia: Long,
        refNoche: Long
    ) {
        withContext(NonCancellable) {
            context.dataStore.edit { prefs ->
                prefs[TIPO_TURNO_ACTIVO_KEY] = tipoTurno
                prefs[COMPANIA_KEY] = compania
                prefs[DIA_ENTRADA_KEY] = dEntrada
                prefs[DIA_SALIDA_KEY] = dSalida
                prefs[NOCHE_ENTRADA_KEY] = nEntrada
                prefs[NOCHE_SALIDA_KEY] = nSalida
                prefs[SUBTURNO_DIA_KEY] = subDia
                prefs[SUBTURNO_NOCHE_KEY] = subNoche
                prefs[SUBTURNO_DIA_REF_KEY] = refDia
                prefs[SUBTURNO_NOCHE_REF_KEY] = refNoche
            }
        }
    }
    
    // Guardar la configuración del turno 24Hx48H
    suspend fun saveTurno24Config(
        tipoTurno: String,
        compania: String,
        entrada: String,
        salida: String,
        sub24: String,
        ref24: Long
    ) {
        withContext(NonCancellable) {
            context.dataStore.edit { prefs ->
                prefs[TIPO_TURNO_ACTIVO_KEY] = tipoTurno
                prefs[COMPANIA_24_KEY] = compania
                prefs[ENTRADA_24_KEY] = entrada
                prefs[SALIDA_24_KEY] = salida
                prefs[SUBTURNO_24_KEY] = sub24
                prefs[SUBTURNO_24_REF_KEY] = ref24
            }
        }
    }
    
    suspend fun savePermutas(permutas: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey("permutas_list")] = permutas.joinToString("||")
        }
    }

    val permutasGuardadas: Flow<List<String>> = context.dataStore.data.map {
        it[stringPreferencesKey("permutas_list")]?.split("||")?.filter { s -> s.isNotBlank() } ?: emptyList()
    }
    
    // Función para obtener los chats ocultos por usuario (UID)
    fun getHiddenChats(uid: String): Flow<List<String>> {
        val key = stringPreferencesKey("hidden_chats_$uid")
        return context.dataStore.data.map { prefs ->
            prefs[key]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        }
    }
    
    // Función para ocultar un chat de forma persistente y vinculada al usuario
    suspend fun hideChat(uid: String, chatId: String) {
        val key = stringPreferencesKey("hidden_chats_$uid")
        context.dataStore.edit { prefs ->
            val current = prefs[key]?.split(",")?.filter { it.isNotBlank() }?.toMutableSet() ?: mutableSetOf()
            current.add(chatId)
            prefs[key] = current.joinToString(",")
        }
    }

    // Función para obtener las permutas ocultas por usuario (UID)
    fun getHiddenPermutas(uid: String): Flow<List<String>> {
        val key = stringPreferencesKey("hidden_permutas_$uid")
        return context.dataStore.data.map { prefs ->
            prefs[key]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        }
    }

    // Función para ocultar una permuta de forma persistente
    suspend fun hidePermuta(uid: String, permutaId: String) {
        val key = stringPreferencesKey("hidden_permutas_$uid")
        context.dataStore.edit { prefs ->
            val current = prefs[key]?.split(",")?.filter { it.isNotBlank() }?.toMutableSet() ?: mutableSetOf()
            current.add(permutaId)
            prefs[key] = current.joinToString(",")
        }
    }

    suspend fun saveTurno10Config(
        tipoTurno: String,
        compania: String,
        entrada: String,
        salida: String
    ) {
        withContext(NonCancellable) {
            context.dataStore.edit { prefs ->
                prefs[TIPO_TURNO_ACTIVO_KEY] = tipoTurno
                prefs[COMPANIA_10_KEY] = compania
                prefs[ENTRADA_10_KEY] = entrada
                prefs[SALIDA_10_KEY] = salida
            }
        }
    }
    
    // Guardar lista de compensatorios
    suspend fun saveCompensatorios(compensatorios: List<String>) {
        withContext(NonCancellable) {
            context.dataStore.edit { prefs ->
                prefs[stringPreferencesKey("compensatorios_list")] = compensatorios.joinToString("||")
            }
        }
    }

    // Función auxiliar para obtener la lista de compensatorios fijos/predeterminados (limpios, sin utilizar)
    fun getDefaultCompensatorios(): List<com.calendarioazulpixel.domain.model.Compensatorio> {
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        return listOf(
            com.calendarioazulpixel.domain.model.Compensatorio(java.util.UUID.randomUUID().toString(), "Navidad año anterior", "🎄", false, true, currentYear),
            com.calendarioazulpixel.domain.model.Compensatorio(java.util.UUID.randomUUID().toString(), "Semana Santa", "✝️", false, true),
            com.calendarioazulpixel.domain.model.Compensatorio(java.util.UUID.randomUUID().toString(), "Día del INPEC", "🏛️", false, true),
            com.calendarioazulpixel.domain.model.Compensatorio(java.util.UUID.randomUUID().toString(), "Día del Trabajo", "👷", false, true),
            com.calendarioazulpixel.domain.model.Compensatorio(java.util.UUID.randomUUID().toString(), "Familia 1.er semestre", "👨‍👩‍👧‍👦", false, true),
            com.calendarioazulpixel.domain.model.Compensatorio(java.util.UUID.randomUUID().toString(), "Familia 2.º semestre", "👨‍👩‍👧‍👦", false, true),
            com.calendarioazulpixel.domain.model.Compensatorio(java.util.UUID.randomUUID().toString(), "Día de las Mercedes", "⛪", false, true),
            com.calendarioazulpixel.domain.model.Compensatorio(java.util.UUID.randomUUID().toString(), "Cumpleaños", "🎂", false, true),
            com.calendarioazulpixel.domain.model.Compensatorio(java.util.UUID.randomUUID().toString(), "Día de la Mujer", "♀️", false, true),
            com.calendarioazulpixel.domain.model.Compensatorio(java.util.UUID.randomUUID().toString(), "Personaje del mes", "⭐", false, true)
        )
    }

    // Leer lista de compensatorios, inyectando la configuración por defecto si está vacía, y gestionando la regla de Navidad
    val compensatoriosFlow: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val rawData = prefs[stringPreferencesKey("compensatorios_list")]
        
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        
        if (rawData == null || rawData.isBlank()) {
            // Primera vez: Crear la lista inicial
            getDefaultCompensatorios().map { it.toStringData() }
        } else {
            val list = rawData.split("||").filter { it.isNotBlank() }.toMutableList()
            
            // Regla de Navidad: Verificar si ya existe un registro de Navidad para el año en curso
            val parsedList = list.mapNotNull { com.calendarioazulpixel.domain.model.Compensatorio.fromStringData(it) }
            val hasCurrentYearNavidad = parsedList.any { it.nombre.contains("Navidad") && it.anioNavidad == currentYear }
            
            if (!hasCurrentYearNavidad) {
                // Agregar la nueva Navidad al principio
                val newNavidad = com.calendarioazulpixel.domain.model.Compensatorio(
                    id = java.util.UUID.randomUUID().toString(), 
                    nombre = "Navidad año anterior", 
                    icono = "🎄", 
                    isUtilizado = false, 
                    esFijo = true, 
                    anioNavidad = currentYear
                )
                list.add(0, newNavidad.toStringData())
            }
            list
        }
    }
    
    // Preferencia global de formato de hora
    val is24HourFormat: Flow<Boolean> = context.dataStore.data.map { it[TIME_FORMAT_24H_KEY] ?: false }
    
    suspend fun saveTimeFormat(is24h: Boolean) {
        withContext(NonCancellable) {
            context.dataStore.edit { prefs ->
                prefs[TIME_FORMAT_24H_KEY] = is24h
            }
        }
    }

    // Configuración individual de Alarmas
    fun getAlarmConfig(type: String): Flow<com.calendarioazulpixel.domain.model.AlarmConfig> = context.dataStore.data.map { prefs ->
        val defaultHour = when (type) {
            "noche" -> 16 // 04:00 PM
            "dia" -> 4    // 04:30 AM
            else -> 5     // 05:00 AM
        }
        val defaultMin = if (type == "dia") 30 else 0
        
        com.calendarioazulpixel.domain.model.AlarmConfig(
            type = type,
            enabled = prefs[getAlarmEnabledKey(type)] ?: false,
            hour = prefs[getAlarmHourKey(type)] ?: defaultHour,
            minute = prefs[getAlarmMinuteKey(type)] ?: defaultMin,
            soundUri = prefs[getAlarmSoundUriKey(type)] ?: "",
            soundName = prefs[getAlarmSoundNameKey(type)] ?: "Tono predeterminado",
            vibrate = prefs[getAlarmVibrateKey(type)] ?: true
        )
    }

    suspend fun saveAlarmConfig(config: com.calendarioazulpixel.domain.model.AlarmConfig) {
        withContext(NonCancellable) {
            context.dataStore.edit { prefs ->
                prefs[getAlarmEnabledKey(config.type)] = config.enabled
                prefs[getAlarmHourKey(config.type)] = config.hour
                prefs[getAlarmMinuteKey(config.type)] = config.minute
                prefs[getAlarmSoundUriKey(config.type)] = config.soundUri
                prefs[getAlarmSoundNameKey(config.type)] = config.soundName
                prefs[getAlarmVibrateKey(config.type)] = config.vibrate
            }
        }
    }
    
    suspend fun toggleAlarmEnabled(type: String, enabled: Boolean) {
        withContext(NonCancellable) {
            context.dataStore.edit { prefs ->
                prefs[getAlarmEnabledKey(type)] = enabled
            }
        }
    }
}
