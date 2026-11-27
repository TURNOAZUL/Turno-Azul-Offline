package com.calendarioazulpixel.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pruebas instrumentadas (requieren emulador) para validar el comportamiento real
 * de AlarmScheduler con el contexto y las APIs del sistema Android (AlarmManager).
 */
@RunWith(AndroidJUnit4::class)
class AlarmSchedulerInstrumentedTest {

    @Test
    fun scheduleExactAlarm_withFutureDate_returnsScheduledOrPermissionRequired() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val futureTime = System.currentTimeMillis() + 60000 // 1 min en el futuro

        val result = AlarmScheduler.scheduleExactAlarm(context, "general", "01/01/2030", futureTime)

        // Dependiendo de si el emulador de prueba tiene concedido el permiso (Android 12+)
        // retornará SCHEDULED o PERMISSION_REQUIRED, pero JAMÁS INVALID_TRIGGER_TIME o ERROR.
        assertTrue(
            "Expected SCHEDULED or PERMISSION_REQUIRED but got $result",
            result == ScheduleResult.SCHEDULED || result == ScheduleResult.PERMISSION_REQUIRED
        )
    }

    @Test
    fun scheduleExactAlarm_withPastDate_returnsInvalidTriggerTime() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val pastTime = System.currentTimeMillis() - 60000 // 1 min en el pasado

        val result = AlarmScheduler.scheduleExactAlarm(context, "general", "01/01/2000", pastTime)

        // Debe ser interceptado antes de tocar AlarmManager
        assertEquals(ScheduleResult.INVALID_TRIGGER_TIME, result)
    }

    @Test
    fun scheduleExactAlarm_withCurrentTime_returnsInvalidTriggerTime() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val currentTime = System.currentTimeMillis()

        val result = AlarmScheduler.scheduleExactAlarm(context, "dia", "15/05/2024", currentTime)

        // Igual a actual debe ser rechazado
        assertEquals(ScheduleResult.INVALID_TRIGGER_TIME, result)
    }
}