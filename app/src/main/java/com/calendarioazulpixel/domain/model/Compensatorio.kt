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
        // Reemplazar caracteres especiales temporalmente para no corromper la serialización
        val safeNombre = nombre.replace("~", "[TILDE]").replace("||", "[OR]")
        val safeIcono = icono.replace("~", "[TILDE]").replace("||", "[OR]")
        return "$id~$safeNombre~$safeIcono~$isUtilizado~$esFijo~$anioNavidad"
    }

    companion object {
        fun fromStringData(data: String): Compensatorio? {
            val parts = data.split("~")
            if (parts.size >= 5) {
                // Restaurar caracteres originales al leer
                val originalNombre = parts[1].replace("[TILDE]", "~").replace("[OR]", "||")
                val originalIcono = parts[2].replace("[TILDE]", "~").replace("[OR]", "||")
                return Compensatorio(
                    id = parts[0],
                    nombre = originalNombre,
                    icono = originalIcono,
                    isUtilizado = parts[3].toBooleanStrictOrNull() ?: false,
                    esFijo = parts[4].toBooleanStrictOrNull() ?: false,
                    anioNavidad = if (parts.size > 5) parts[5].toIntOrNull() ?: 0 else 0
                )
            }
            return null
        }
    }
}