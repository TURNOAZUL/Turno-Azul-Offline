package com.calendarioazulpixel.util

import android.content.Context
import android.util.Log
import com.calendarioazulpixel.data.local.PreferencesRepository
import com.calendarioazulpixel.domain.model.AlarmConfig
import com.calendarioazulpixel.domain.model.FutureAlarmDates
import com.calendarioazulpixel.domain.usecase.Compania
import com.calendarioazulpixel.domain.usecase.TipoTurno
import kotlinx.coroutines.flow.first
import java.util.Calendar

object AlarmSyncManager {

    /**
     * Convierte una fecha en formato dd/MM/yyyy y una configuración de hora a un Timestamp en milisegundos.
     * Respeta la zona horaria del dispositivo.
     */
    fun createTimestamp(dateStr: String, config: AlarmConfig): Long {
        return try {
            val parts = dateStr.split("/")
            if (parts.size == 3) {
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_MONTH, parts[0].toInt())
                cal.set(Calendar.MONTH, parts[1].toInt() - 1) // Calendar usa meses 0-11
                cal.set(Calendar.YEAR, parts[2].toInt())
                
                cal.set(Calendar.HOUR_OF_DAY, config.hour)
                cal.set(Calendar.MINUTE, config.minute)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                
                cal.timeInMillis
            } else {
                0L
            }
        } catch (e: Exception) {
            Log.e("AlarmSyncManager", "Error convirtiendo fecha $dateStr", e)
            0L
        }
    }

    /**
     * Extrae la categoría y la fecha de una firma.
     * Ejemplo de firma: "general|general|25/12/2026" o "12x24|dia|25/12/2026"
     */
    fun parseSignature(signature: String): Pair<String, String>? {
        val parts = signature.split("|")
        if (parts.size == 3) {
            return Pair(parts[1], parts[2])
        }
        return null
    }

    /**
     * Helper para disparar la sincronización leyendo el estado actual.
     */
    suspend fun syncCurrentState(context: Context) {
        val repository = PreferencesRepository(context)

        // 1. Sincronizar Alarmas independientes (General y Personalizadas)
        val alarmIds = repository.alarmIds.first()
        for (alarmId in alarmIds) {
            if (alarmId != "dia" && alarmId != "noche") {
                syncNonShiftAlarm(context, repository, alarmId)
            }
        }

        // 2. Sincronizar Sistema 12x24 (Turno Día y Turno Noche) si aplica
        val tipoTurnoStr = repository.tipoTurnoActivo.first()
        val tipoTurno = try { TipoTurno.valueOf(tipoTurnoStr) } catch(e: Exception) { TipoTurno.DOCE_VEINTICUATRO }

        if (tipoTurno != TipoTurno.DOCE_VEINTICUATRO) {
            syncAlarms12x24(context, repository, FutureAlarmDates(false, emptyList(), emptyList(), emptyList()))
            return
        }
        
        val companiaStr = repository.companiaSeleccionada.first()
        val compania = try { Compania.valueOf(companiaStr) } catch(e:Exception) { Compania.AMARILLA }
        val dEntrada = repository.diaEntrada.first()
        val dSalida = repository.diaSalida.first()
        val nEntrada = repository.nocheEntrada.first()
        val nSalida = repository.nocheSalida.first()
        val subDia = repository.subturnoDia.first()
        val subNoche = repository.subturnoNoche.first()
        val refDia = repository.fechaRefSubturnoDia.first()
        val refNoche = repository.fechaRefSubturnoNoche.first()

        val futureDates = TurnoAlarmUtils.calculateFutureAlarms(
            tipoTurno = tipoTurno,
            compania = compania,
            dEntrada = dEntrada, dSalida = dSalida, nEntrada = nEntrada, nSalida = nSalida,
            entrada24 = "", salida24 = "", entrada10 = "", salida10 = "",
            subDia = subDia, subNoche = subNoche,
            refDia = refDia, refNoche = refNoche,
            sub24 = "", ref24 = 0L
        )

        syncAlarms12x24(context, repository, futureDates)
    }

    /**
     * Sincronización independiente para la Alarma General (funciona todos los días a la hora configurada).
     */
    suspend fun syncGeneralAlarm(context: Context, repository: PreferencesRepository) {
        syncNonShiftAlarm(context, repository, "general")
    }

    /**
     * Sincronización general para alarmas no basadas en turnos (general y personalizadas).
     */
    suspend fun syncNonShiftAlarm(context: Context, repository: PreferencesRepository, type: String) {
        val config = repository.getAlarmConfig(type).first()
        val allSigs = repository.scheduledAlarmsRegistry.first()
        val oldSigs = allSigs.filter { it.contains("|$type|") || it.startsWith("$type|") }.toSet()

        if (!config.enabled) {
            oldSigs.forEach { sig ->
                val parsed = parseSignature(sig)
                if (parsed != null) {
                    AlarmScheduler.cancelAlarm(context, parsed.first, parsed.second)
                }
            }
            val cleanRegistry = repository.scheduledAlarmsRegistry.first().filterNot { it.contains("|$type|") }.toSet()
            repository.saveScheduledAlarmsRegistry(cleanRegistry)
            Log.d("AlarmSyncManager", "Alarma $type desactivada y cancelada.")
            return
        }

        val ts = calculateNextTriggerTime(config)
        val formatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("es", "ES"))
        val cal = Calendar.getInstance().apply { timeInMillis = ts }
        val dateStr = formatter.format(cal.time)
        val desiredSig = "$type|$type|$dateStr"

        oldSigs.filter { it != desiredSig }.forEach { sig ->
            val parsed = parseSignature(sig)
            if (parsed != null) {
                AlarmScheduler.cancelAlarm(context, parsed.first, parsed.second)
            }
        }

        val result = AlarmScheduler.scheduleExactAlarm(context, type, dateStr, ts)
        val confirmedSigs = mutableSetOf<String>()
        if (result == ScheduleResult.SCHEDULED) {
            confirmedSigs.add(desiredSig)
            Log.d("AlarmSyncManager", "Alarma $type programada exitosamente para el $dateStr a las ${config.hour}:${config.minute}")
        } else {
            Log.w("AlarmSyncManager", "Fallo al programar Alarma $type. Resultado: $result")
        }

        val currentReg = repository.scheduledAlarmsRegistry.first()
        val cleanedReg = currentReg.filterNot { it.contains("|$type|") }.toSet()
        repository.saveScheduledAlarmsRegistry(cleanedReg + confirmedSigs)
    }

    fun calculateNextTriggerTime(config: AlarmConfig): Long {
        val cal = Calendar.getInstance()
        val now = System.currentTimeMillis()
        
        cal.set(Calendar.HOUR_OF_DAY, config.hour)
        cal.set(Calendar.MINUTE, config.minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        when (config.repeatMode) {
            "everyday", "once" -> {
                if (cal.timeInMillis <= now) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            "custom" -> {
                if (config.repeatDays.isNotEmpty()) {
                    fun toCalendarDay(d: Int): Int = when(d) {
                        1 -> Calendar.MONDAY
                        2 -> Calendar.TUESDAY
                        3 -> Calendar.WEDNESDAY
                        4 -> Calendar.THURSDAY
                        5 -> Calendar.FRIDAY
                        6 -> Calendar.SATURDAY
                        7 -> Calendar.SUNDAY
                        else -> Calendar.MONDAY
                    }
                    val targetCalendarDays = config.repeatDays.map { toCalendarDay(it) }.toSet()
                    
                    var found = false
                    for (i in 0..7) {
                        val currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                        if (targetCalendarDays.contains(currentDayOfWeek) && cal.timeInMillis > now) {
                            found = true
                            break
                        }
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    if (!found) {
                        cal.timeInMillis = now + 24 * 3600 * 1000L
                    }
                } else {
                    if (cal.timeInMillis <= now) {
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
            }
            else -> {
                if (cal.timeInMillis <= now) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
        }
        return cal.timeInMillis
    }

    /**
     * Función principal de Reconciliación Inteligente para Sistema 12x24 (Turno Día y Turno Noche).
     */
    suspend fun syncAlarms12x24(
        context: Context,
        repository: PreferencesRepository,
        futureDates: FutureAlarmDates
    ): Boolean {
        
        if (!futureDates.isApplicable) {
            Log.d("AlarmSyncManager", "Sincronización 12x24 abortada: El modelo no aplica.")
            val currentSignatures = repository.scheduledAlarmsRegistry.first()
            val old12x24Signatures = currentSignatures.filter { it.startsWith("12x24|") }
            old12x24Signatures.forEach { sig ->
                val parsed = parseSignature(sig)
                if (parsed != null) {
                    AlarmScheduler.cancelAlarm(context, parsed.first, parsed.second)
                }
            }
            repository.clearScheduledAlarmsForModality("12x24")
            return true
        }

        // 1. Leer Configuraciones individuales del usuario (Día y Noche)
        val diaConfig = repository.getAlarmConfig("dia").first()
        val nocheConfig = repository.getAlarmConfig("noche").first()

        // 2. Construir Firmas Deseadas
        val desiredSignatures = mutableSetOf<String>()
        val desiredTimestamps = mutableMapOf<String, Long>()
        val now = System.currentTimeMillis()

        fun getNextValid(dates: List<String>, config: AlarmConfig): Pair<String, Long>? {
            for (date in dates) {
                val ts = createTimestamp(date, config)
                if (ts > now) return Pair(date, ts)
            }
            return null
        }

        if (diaConfig.enabled) {
            getNextValid(futureDates.diaAlarms, diaConfig)?.let { (date, ts) ->
                val sig = "12x24|dia|$date"
                desiredSignatures.add(sig)
                desiredTimestamps[sig] = ts
            }
        }
        if (nocheConfig.enabled) {
            getNextValid(futureDates.nocheAlarms, nocheConfig)?.let { (date, ts) ->
                val sig = "12x24|noche|$date"
                desiredSignatures.add(sig)
                desiredTimestamps[sig] = ts
            }
        }

        // 3. Leer las Firmas Anteriores desde DataStore
        val allOldSignatures = repository.scheduledAlarmsRegistry.first()
        val old12x24Signatures = allOldSignatures.filter { it.startsWith("12x24|") }.toSet()

        // 4. Calcular Diferencias
        val signaturesToCancel = old12x24Signatures - desiredSignatures
        val signaturesToSchedule = desiredSignatures

        // 5. Cancelar Obsoletas
        signaturesToCancel.forEach { sig ->
            val parsed = parseSignature(sig)
            if (parsed != null) {
                AlarmScheduler.cancelAlarm(context, parsed.first, parsed.second)
            }
        }

        // 6. Programar Nuevas y Actualizar Vigentes
        val confirmedSignatures = mutableSetOf<String>()
        var hasPermissionError = false

        signaturesToSchedule.forEach { sig ->
            val parsed = parseSignature(sig)
            val ts = desiredTimestamps[sig] ?: 0L

            if (parsed != null && ts > 0L) {
                val result = AlarmScheduler.scheduleExactAlarm(context, parsed.first, parsed.second, ts)
                
                when (result) {
                    ScheduleResult.SCHEDULED -> {
                        confirmedSignatures.add(sig)
                    }
                    ScheduleResult.PERMISSION_REQUIRED -> {
                        Log.w("AlarmSyncManager", "Permiso denegado para: $sig")
                        hasPermissionError = true
                    }
                    ScheduleResult.INVALID_TRIGGER_TIME -> {
                        Log.d("AlarmSyncManager", "Tiempo pasado ignorado para: $sig")
                    }
                    ScheduleResult.ERROR -> {
                        Log.e("AlarmSyncManager", "Error programando: $sig")
                    }
                }
            }
        }

        // 7. Actualizar el Registro Persistente
        repository.updateScheduledAlarmsForModality("12x24", confirmedSignatures)

        return !hasPermissionError
    }
}
