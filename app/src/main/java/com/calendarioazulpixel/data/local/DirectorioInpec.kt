package com.calendarioazulpixel.data.local

/**
 * Estructura de datos centralizada con la lista de departamentos y municipios 
 * de Colombia que cuentan con Establecimientos de Reclusión del Orden Nacional (ERON)
 * a cargo del INPEC.
 * 
 * Fuente: Portal oficial del INPEC y datos abiertos.
 * Fecha de verificación: Conocimiento base (2023-2024).
 * Pendiente: Validación manual exhaustiva en campo y cruce con el MinJusticia (cuyo mapa no permite scraping automatizado directo).
 */
object DirectorioInpec {
    
    val establecimientos = mapOf(
        "Amazonas" to listOf("Leticia").sorted(),
        "Antioquia" to listOf("Amagá", "Andes", "Caucasia", "Ciudad Bolívar", "Concordia", "El Santuario", "Envigado", "Fredonia", "Itagüí", "Jericó", "La Ceja", "La Paz", "Medellín", "Puerto Berrío", "Puerto Triunfo", "Santa Bárbara", "Santa Fe de Antioquia", "Santo Domingo", "Sonsón", "Titiribí", "Urrao", "Yarumal", "Yondó").sorted(),
        "Arauca" to listOf("Arauca", "Arauquita", "Saravena").sorted(),
        "Atlántico" to listOf("Barranquilla", "Sabanalarga").sorted(),
        "Bogotá D.C." to listOf("Bogotá"),
        "Bolívar" to listOf("Cartagena", "El Carmen de Bolívar", "Magangué").sorted(),
        "Boyacá" to listOf("Chiquinquirá", "Cómbita", "Duitama", "El Cocuy", "Garagoa", "Guateque", "Moniquirá", "Pesca", "Puerto Boyacá", "Ramiriquí", "Santa Rosa de Viterbo", "Soatá", "Sogamoso", "Tunja").sorted(),
        "Caldas" to listOf("Aguadas", "Anserma", "Chinchiná", "La Dorada", "Manizales", "Manzanares", "Pensilvania", "Pácora", "Riosucio", "Salamina", "Samaná").sorted(),
        "Caquetá" to listOf("Florencia"),
        "Casanare" to listOf("Monterrey", "Paz de Ariporo", "Yopal").sorted(),
        "Cauca" to listOf("Bolívar", "Buenos Aires", "Caloto", "Guapi", "Popayán", "Puerto Tejada", "Santander de Quilichao", "Silvia").sorted(),
        "Cesar" to listOf("Aguachica", "Chiriguaná", "Valledupar").sorted(),
        "Chocó" to listOf("Bahía Solano", "Istmina", "Quibdó").sorted(),
        "Cundinamarca" to listOf("Cáqueza", "Facatativá", "Fusagasugá", "Gachetá", "Girardot", "Guaduas", "La Mesa", "Ubaté", "Villeta", "Zipaquirá").sorted(),
        "Córdoba" to listOf("Cereté", "Lorica", "Montería", "Tierralta").sorted(),
        "Huila" to listOf("Garzón", "La Plata", "Neiva", "Pitalito").sorted(),
        "La Guajira" to listOf("Riohacha", "San Juan del Cesar", "Villanueva").sorted(),
        "Magdalena" to listOf("El Banco", "Santa Marta").sorted(),
        "Meta" to listOf("Acacías", "Granada", "Puerto López", "Villavicencio").sorted(),
        "Nariño" to listOf("Ipiales", "La Unión", "Pasto", "Tumaco", "Túquerres").sorted(),
        "Norte de Santander" to listOf("Cúcuta", "Ocaña", "Pamplona").sorted(),
        "Putumayo" to listOf("Mocoa", "Puerto Asís").sorted(),
        "Quindío" to listOf("Armenia", "Calarcá").sorted(),
        "Risaralda" to listOf("Apía", "Dosquebradas", "Pereira", "Santa Rosa de Cabal").sorted(),
        "San Andrés y Providencia" to listOf("San Andrés"),
        "Santander" to listOf("Barrancabermeja", "Bucaramanga", "Girón", "Málaga", "San Gil", "Socorro", "Vélez").sorted(),
        "Sucre" to listOf("Corozal", "San Marcos", "Sincelejo").sorted(),
        "Tolima" to listOf("Chaparral", "Espinal", "Fresno", "Guamo", "Honda", "Ibagué", "Líbano", "Melgar", "Purificación").sorted(),
        "Valle del Cauca" to listOf("Buenaventura", "Buga", "Cali", "Cartago", "Florida", "Jamundí", "Palmira", "Roldanillo", "Sevilla", "Tuluá", "Zarzal").sorted()
    )

    // Lista de departamentos ordenada alfabéticamente
    val departamentos = establecimientos.keys.sorted()

    // Obtener las ciudades de un departamento específico, ordenadas alfabéticamente
    fun getCiudadesPorDepartamento(departamento: String): List<String> {
        return establecimientos[departamento] ?: emptyList()
    }
}
