package com.calendarioazulpixel.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreferencesRepositoryTest {

    private lateinit var repository: PreferencesRepository

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        repository = PreferencesRepository(context)
    }

    @Test
    fun saveScheduledAlarmsRegistry_savesAndRetrievesSignatures() = runBlocking {
        val testSignatures = setOf("12x24|dia|01/01/2026", "12x24|noche|02/01/2026")
        repository.saveScheduledAlarmsRegistry(testSignatures)
        
        val retrieved = repository.scheduledAlarmsRegistry.first()
        assertEquals(2, retrieved.size)
        assertTrue(retrieved.contains("12x24|dia|01/01/2026"))
        assertTrue(retrieved.contains("12x24|noche|02/01/2026"))
    }

    @Test
    fun updateScheduledAlarmsForModality_doesNotDeleteOtherModalities() = runBlocking {
        // Set an initial state with mixed modalities
        val initialSet = setOf(
            "12x24|dia|01/01/2026",
            "24x48|laboral|05/01/2026", // Otra modalidad
            "10x5|descanso|06/01/2026"  // Otra modalidad
        )
        repository.saveScheduledAlarmsRegistry(initialSet)

        // Update ONLY the 12x24 modality
        val new12x24Set = setOf("12x24|noche|10/01/2026")
        repository.updateScheduledAlarmsForModality("12x24", new12x24Set)

        // Verify result
        val retrieved = repository.scheduledAlarmsRegistry.first()
        
        // The old 12x24 ("dia|01") should be gone, the new ("noche|10") should be there.
        // But the 24x48 and 10x5 MUST survive.
        assertEquals(3, retrieved.size)
        assertTrue(retrieved.contains("12x24|noche|10/01/2026"))
        assertTrue(retrieved.contains("24x48|laboral|05/01/2026"))
        assertTrue(retrieved.contains("10x5|descanso|06/01/2026"))
    }

    @Test
    fun clearScheduledAlarmsForModality_removesOnlyTargetModality() = runBlocking {
        val initialSet = setOf(
            "12x24|general|10/10/2026",
            "24x48|general|11/10/2026"
        )
        repository.saveScheduledAlarmsRegistry(initialSet)

        // Clear 12x24
        repository.clearScheduledAlarmsForModality("12x24")

        val retrieved = repository.scheduledAlarmsRegistry.first()
        
        // Should only contain 24x48 now
        assertEquals(1, retrieved.size)
        assertTrue(retrieved.contains("24x48|general|11/10/2026"))
    }
}