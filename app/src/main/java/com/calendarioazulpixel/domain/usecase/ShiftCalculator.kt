package com.calendarioazulpixel.domain.usecase

import java.util.Calendar

enum class TipoTurno { DOCE_VEINTICUATRO, VEINTICUATRO_CUARENTA_OCHO, DIEZ_CINCO }
enum class TurnoType { DIA, NOCHE, TRABAJO, LIBRE, DESCANSO }
enum class Compania { AMARILLA, ROJA, AZUL, ORION, BRAVO }

object ShiftCalculator {

    /**
     * Calcula qué turno toca para el ciclo 12H x 24H
     */
    fun calcularTurno(targetTimeInMillis: Long, compania: Compania): TurnoType {
        val calTarget = Calendar.getInstance().apply { timeInMillis = targetTimeInMillis }
        
        // REGLA ABSOLUTA DEL SISTEMA 12H x 24H:
        // C. Amarilla el 13 de octubre de 2026 es NOCHE (1).
        // Por lo tanto, el 12 de octubre de 2026 para C. Amarilla fue DÍA (0).
        val calAnchor = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 12, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        fun zeroTime(c: Calendar) {
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
        }
        
        zeroTime(calTarget)
        zeroTime(calAnchor)
        
        val diffMillis = calTarget.timeInMillis - calAnchor.timeInMillis
        val diffDays = Math.round(diffMillis.toDouble() / (1000 * 60 * 60 * 24)).toLong()

        val companyOffset = when (compania) {
            Compania.AMARILLA -> 0
            Compania.AZUL -> 1
            Compania.ROJA -> 2
            else -> 0
        }

        val effectiveDiff = diffDays - companyOffset
        val modulo = ((effectiveDiff % 3) + 3) % 3

        return when (modulo.toInt()) {
            0 -> TurnoType.DIA
            1 -> TurnoType.NOCHE
            2 -> TurnoType.LIBRE
            else -> TurnoType.LIBRE
        }
    }

    /**
     * Calcula qué turno toca para el ciclo 24H x 48H
     */
    fun calcularTurno24x48(targetTimeInMillis: Long, compania: Compania): TurnoType {
        val calTarget = Calendar.getInstance().apply { timeInMillis = targetTimeInMillis }
        
        // FECHA DE REFERENCIA 24H x 48H: Miércoles 14 de Octubre de 2026.
        // Ese día, C. Azul tiene turno de TRABAJO.
        val calAnchor = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 14, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        fun zeroTime(c: Calendar) {
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
        }
        
        zeroTime(calTarget)
        zeroTime(calAnchor)
        
        val diffMillis = calTarget.timeInMillis - calAnchor.timeInMillis
        val diffDays = Math.round(diffMillis.toDouble() / (1000 * 60 * 60 * 24)).toLong()

        val companyOffset = when (compania) {
            Compania.AZUL -> 0
            Compania.ROJA -> 1
            Compania.AMARILLA -> 2
            else -> 0
        }

        val effectiveDiff = diffDays - companyOffset
        val modulo = ((effectiveDiff % 3) + 3) % 3

        return if (modulo.toInt() == 0) TurnoType.TRABAJO else TurnoType.LIBRE
    }
    
    /**
     * Calcula el subturno para un día específico de 24H x 48H.
     * Solo válido si calcularTurno24x48(...) == TurnoType.TRABAJO.
     */
    fun calcularSubturno24(
        targetTimeInMillis: Long, 
        subturnoInicial: String, 
        refMillis: Long,
        compania: Compania
    ): String {
        // Si no hay referencia inicial guardada, devolvemos el valor por defecto seleccionado
        if (refMillis == 0L) return subturnoInicial
        
        val calAnchor = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 14, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        fun getCycleIndex(millis: Long): Long {
            if (millis == 0L) return 0L
            val cal = Calendar.getInstance().apply { timeInMillis = millis }
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            
            val diffDays = Math.round((cal.timeInMillis - calAnchor.timeInMillis).toDouble() / (1000 * 60 * 60 * 24)).toLong()
            val companyOffset = when (compania) {
                Compania.AZUL -> 0
                Compania.ROJA -> 1
                Compania.AMARILLA -> 2
                else -> 0
            }
            val effectiveDiff = diffDays - companyOffset
            // Cada ciclo de 24Hx48H dura 3 días exactos.
            return Math.floorDiv(effectiveDiff, 3L)
        }

        val targetCycle = getCycleIndex(targetTimeInMillis)
        val recentCycle = getCycleIndex(refMillis)
        
        val deltaCycles = targetCycle - recentCycle
        val isSameSub = (deltaCycles % 2L == 0L)
        
        return if (isSameSub) {
            subturnoInicial // Mismo subturno que en el ciclo de referencia
        } else {
            // Alterna
            if (subturnoInicial == "2/4") "3/1" else "2/4"
        }
    }
    
    /**
     * Calcula qué turno toca para el ciclo 10D x 5D
     */
    fun calcularTurno10x5(targetTimeInMillis: Long, compania: Compania): TurnoType {
        val calTarget = Calendar.getInstance().apply { timeInMillis = targetTimeInMillis }
        
        // FECHA DE REFERENCIA 10D x 5D: 14 de Octubre de 2026.
        // Ese día, C. ORION es el día 0 de su ciclo de 15 días (10 trabajo, 5 descanso).
        // C. BRAVO está en su día 10 del ciclo, que corresponde al inicio de sus 5 días de descanso.
        val calAnchor = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 14, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        fun zeroTime(c: Calendar) {
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
        }
        
        zeroTime(calTarget)
        zeroTime(calAnchor)
        
        val diffMillis = calTarget.timeInMillis - calAnchor.timeInMillis
        val diffDays = Math.round(diffMillis.toDouble() / (1000 * 60 * 60 * 24)).toLong()

        // Desfases: Orion=0, Bravo=10 (Bravo lleva 10 días de diferencia)
        val companyOffset = when (compania) {
            Compania.ORION -> 0
            Compania.BRAVO -> 10
            else -> 0
        }

        val effectiveDiff = diffDays + companyOffset
        val modulo = ((effectiveDiff % 15) + 15) % 15

        // Los primeros 10 días (0 a 9) son DÍA LABORAL, los últimos 5 (10 a 14) son DESCANSO
        return if (modulo.toInt() < 10) TurnoType.TRABAJO else TurnoType.DESCANSO
    }
    
    /**
     * Calcula el subturno para un día específico de 12H x 24H.
     * Solo válido si calcularTurno(...) == TurnoType.DIA o NOCHE.
     */
    fun calcularSubturno(
        targetTimeInMillis: Long, 
        tipo: TurnoType, 
        subDia: String, refDia: Long,
        subNoche: String, refNoche: Long,
        compania: Compania
    ): String {
        if (tipo != TurnoType.DIA && tipo != TurnoType.NOCHE) return ""
        
        val useDiaRef = refDia >= refNoche
        val recentRef = if (useDiaRef) refDia else refNoche
        val recentSub = if (useDiaRef) subDia else subNoche
        val recentIsDia = useDiaRef
        
        val calAnchor = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 12, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        fun getCycleIndex(millis: Long): Long {
            if (millis == 0L) return 0L
            val cal = Calendar.getInstance().apply { timeInMillis = millis }
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val diffDays = Math.round((cal.timeInMillis - calAnchor.timeInMillis).toDouble() / (1000 * 60 * 60 * 24)).toLong()
            val companyOffset = when (compania) {
                Compania.AMARILLA -> 0
                Compania.AZUL -> 1
                Compania.ROJA -> 2
                else -> 0
            }
            val effectiveDiff = diffDays - companyOffset
            return Math.floorDiv(effectiveDiff, 3L)
        }

        val targetCycle = getCycleIndex(targetTimeInMillis)
        
        if (recentRef == 0L) {
            return if (tipo == TurnoType.DIA) "2" else "4"
        }
        
        val recentCycle = getCycleIndex(recentRef)
        val isPairAAtRecent = if (recentIsDia) recentSub == "2" else recentSub == "4"
        
        val deltaCycles = targetCycle - recentCycle
        val isSamePair = (deltaCycles % 2L == 0L)
        val isTargetPairA = if (isSamePair) isPairAAtRecent else !isPairAAtRecent
        
        return if (tipo == TurnoType.DIA) {
            if (isTargetPairA) "2" else "3"
        } else {
            if (isTargetPairA) "4" else "1"
        }
    }
}