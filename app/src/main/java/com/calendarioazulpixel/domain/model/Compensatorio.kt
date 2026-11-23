package com.calendarioazulpixel.domain.model

data class Compensatorio(
    val id: String,
    val nombre: String,
    val icono: String,
    val isUtilizado: Boolean,
    val esFijo: Boolean,
    val anioNavidad: Int = 0
) {
    fun toStringData(): String {
        return "$id~$nombre~$icono~$isUtilizado~$esFijo~$anioNavidad"
    }

    companion object {
        fun fromStringData(data: String): Compensatorio? {
            val parts = data.split("~")
            if (parts.size >= 5) {
                return Compensatorio(
                    id = parts[0],
                    nombre = parts[1],
                    icono = parts[2],
                    isUtilizado = parts[3].toBooleanStrictOrNull() ?: false,
                    esFijo = parts[4].toBooleanStrictOrNull() ?: false,
                    anioNavidad = if (parts.size > 5) parts[5].toIntOrNull() ?: 0 else 0
                )
            }
            return null
        }
    }
}