package com.calendarioazulpixel.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AlarmSchedulerTest {

    @Test
    fun `generateRequestId produces distinct IDs for different categories`() {
        val idGeneral = AlarmScheduler.generateRequestId("general", "25/12/2023")
        val idDia = AlarmScheduler.generateRequestId("dia", "25/12/2023")
        val idNoche = AlarmScheduler.generateRequestId("noche", "25/12/2023")

        assertNotEquals(idGeneral, idDia)
        assertNotEquals(idDia, idNoche)
        assertNotEquals(idGeneral, idNoche)
    }

    @Test
    fun `generateRequestId produces distinct IDs for different dates`() {
        val id1 = AlarmScheduler.generateRequestId("dia", "01/01/2024")
        val id2 = AlarmScheduler.generateRequestId("dia", "02/01/2024")

        assertNotEquals(id1, id2)
    }

    @Test
    fun `generateRequestId is deterministic`() {
        val id1 = AlarmScheduler.generateRequestId("noche", "15/08/2024")
        val id2 = AlarmScheduler.generateRequestId("noche", "15/08/2024")

        assertEquals(id1, id2)
    }
    
    @Test
    fun `generateRequestId returns expected math result`() {
        // "25/12/2023" -> dateInt: 20231225
        // "dia" -> offset 200000000
        // result -> 220231225
        val id = AlarmScheduler.generateRequestId("dia", "25/12/2023")
        assertEquals(220231225, id)
    }
}