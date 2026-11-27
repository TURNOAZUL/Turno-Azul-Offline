package com.calendarioazulpixel.domain.model

data class AlarmConfig(
    val type: String,
    val enabled: Boolean,
    val hour: Int,
    val minute: Int,
    val soundUri: String,
    val soundName: String,
    val vibrate: Boolean,
    val repeatInterval: Int = 0, // 0 = Una sola vez, 1, 5, 10 minutos
    val durationMinutes: Int = 5, // 5, 10, 15 minutos, 0 = Manual (infinito)
    val name: String = when (type) {
        "general" -> "Alarma General"
        "dia" -> "Alarma Turno Día"
        "noche" -> "Alarma Turno Noche"
        "custom_1" -> "Alarma Personalizada 1"
        "custom_2" -> "Alarma Personalizada 2"
        else -> "Alarma"
    },
    val repeatMode: String = "once", // "once", "everyday", "custom"
    val repeatDays: List<Int> = emptyList(), // 1=Lun .. 7=Dom
    val snoozeEnabled: Boolean = false,
    val snoozeIntervalMinutes: Int = 5, // 5, 10, 15, 20, 25, 30
    val snoozeMaxCount: Int = 3 // 2, 3, 5, 10
)
