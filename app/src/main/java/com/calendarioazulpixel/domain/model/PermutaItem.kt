package com.calendarioazulpixel.domain.model

data class PermutaItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val origenCiudad: String,
    val destinoCiudad: String,
    val informacionAdicional: String,
    val fechaPublicacionMillis: Long
)
