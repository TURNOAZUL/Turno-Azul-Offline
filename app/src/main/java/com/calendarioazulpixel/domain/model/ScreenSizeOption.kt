package com.calendarioazulpixel.domain.model

enum class ScreenSizeOption(val label: String, val scaleFactor: Float) {
    PEQUENO("Pequeño", 0.85f),
    NORMAL("Normal (predeterminado)", 1.0f),
    MEDIANO("Mediano", 1.15f)
}
