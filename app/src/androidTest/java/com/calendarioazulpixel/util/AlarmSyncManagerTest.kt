package com.calendarioazulpixel.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.calendarioazulpixel.data.local.PreferencesRepository
import com.calendarioazulpixel.domain.model.AlarmConfig
import com.calendarioazulpixel.domain.model.FutureAlarmDates
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar

@RunWith(AndroidJUnit4::class)
class AlarmSyncManagerTest {

    private lateinit var repository: PreferencesRepository
    private lateinit var context: android.content.Context

    @Before
    fun setup() = runBlocking {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        repository = PreferencesRepository(context)
        // Limpiamos el registro de la modalidad antes de empezar cada prueba
        repository.clearScheduledAlarmsForModality("12x24")
    }

    @Test
    fun parseSignature_returnsCorrectPair() {
        val parsed = AlarmSyncManager.parseSignature("12x24|dia|25/12/2026")
        assertNotNull(parsed)
        assertEquals("dia", parsed!!.first)
        assertEquals("25/12/2026", parsed.second)
    }

    @Test
    fun createTimestamp_returnsValidMillisForFutureDate() {
        // Tomamos el año actual para no fallar
        val cal = Calendar.getInstance()
        val nextYear = cal.get(Calendar.YEAR) + 1
        val dateStr = "25/12/$nextYear"
        
        val config = AlarmConfig("general", true, 10, 30, "", "", true)
        
        val ts = AlarmSyncManager.createTimestamp(dateStr, config)
        
        assertTrue(ts > System.currentTimeMillis())
        
        val checkCal = Calendar.getInstance().apply { timeInMillis = ts }
        assertEquals(25, checkCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(11, checkCal.get(Calendar.MONTH)) // 11 es Diciembre en Calendar (0-11)
        assertEquals(nextYear, checkCal.get(Calendar.YEAR))
        assertEquals(10, checkCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, checkCal.get(Calendar.MINUTE))
        assertEquals(0, checkCal.get(Calendar.SECOND))
    }

    @Test
    fun syncAlarms_handlesNotApplicableSystem_andCleansOldAlarms() = runBlocking {
        // Guardamos una firma simulando que en el pasado éramos 12x24
        repository.updateScheduledAlarmsForModality("12x24", setOf("12x24|dia|01/01/2050"))
        
        // Enviamos modelo que simula estar en 24/48 o 10x5
        val futureDates = FutureAlarmDates(isApplicable = false, emptyList(), emptyList(), emptyList())
        
        val result = AlarmSyncManager.syncAlarms12x24(context, repository, futureDates)
        
        // Debería vaciar las firmas de 12x24
        val remaining = repository.scheduledAlarmsRegistry.first()
        
        assertTrue(result)
        assertEquals(0, remaining.size)
    }

    @Test
    fun syncAlarms_generatesCorrectSignaturesAndPreservesOtherModalities() = runBlocking {
        // 1. Estado inicial
        val oldSet = setOf("24x48|general|01/01/2050", "12x24|dia|02/01/2050")
        repository.saveScheduledAlarmsRegistry(oldSet)
        
        // Aseguramos que la alarma de día esté activa
        repository.toggleAlarmEnabled("dia", true)
        
        // Creamos una fecha lo suficientemente lejos en el futuro para que no devuelva INVALID_TRIGGER_TIME
        val cal = Calendar.getInstance()
        val futureYear = cal.get(Calendar.YEAR) + 5
        
        val futureDates = FutureAlarmDates(
            isApplicable = true,
            generalAlarms = emptyList(),
            diaAlarms = listOf("15/05/$futureYear", "16/05/$futureYear"),
            nocheAlarms = emptyList()
        )
        
        // Sincronizamos
        val result = AlarmSyncManager.syncAlarms12x24(context, repository, futureDates)
        
        // Comprobamos el registro resultante
        val finalSet = repository.scheduledAlarmsRegistry.first()
        
        // - "24x48" debió sobrevivir.
        // - "12x24|dia|02/01/2050" debió borrarse (se mandó a cancelar) por obsoleta.
        // - Las dos nuevas ("15/05" y "16/05") debieron agregarse.
        
        // Dependiendo del permiso del emulador (Android 12+) result puede ser true o false
        // Pero las firmas de otras modalidades NUNCA deben tocarse
        assertTrue(finalSet.contains("24x48|general|01/01/2050"))
        assertFalse(finalSet.contains("12x24|dia|02/01/2050"))
        
        // Si el permiso estaba concedido en el emulador, las dos nuevas estarán ahí
        if (result) {
            assertTrue(finalSet.contains("12x24|dia|15/05/$futureYear"))
            assertTrue(finalSet.contains("12x24|dia|16/05/$futureYear"))
        }
    }
}