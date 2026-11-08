package fr.muscu.app.data

import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MeasurementRepositoryTest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        db = AppDatabase.create(context)
    }

    @After
    fun tearDown() {
        db.close()
        context.deleteDatabase("muscu.db")
    }

    @Test
    fun freshInstallStartsWithoutMeasurements() = runBlocking {
        assertTrue(MuscuRepository(context, db).measurements.first().isEmpty())
    }

    @Test
    fun measurementsCanBeAddedEditedAndDeleted() = runBlocking {
        val repo = MuscuRepository(context, db)
        val day = LocalDate.of(2026, 11, 1)

        repo.saveMeasurement(0, day, bicepsCm = 38.5, shouldersCm = null)
        repo.saveMeasurement(0, day.plusDays(7), bicepsCm = null, shouldersCm = 112.0)
        val saved = repo.measurements.first()
        assertEquals(listOf(day.toEpochDay(), day.plusDays(7).toEpochDay()), saved.map { it.epochDay })
        assertEquals(38.5, requireNotNull(saved[0].bicepsCm), 0.0)
        assertNull(saved[0].shouldersCm)
        assertNull(saved[1].bicepsCm)

        repo.saveMeasurement(saved[0].id, day, bicepsCm = 39.0, shouldersCm = 110.5)
        val edited = repo.measurements.first()
        assertEquals(2, edited.size)
        assertEquals(39.0, requireNotNull(edited[0].bicepsCm), 0.0)
        assertEquals(110.5, requireNotNull(edited[0].shouldersCm), 0.0)

        repo.deleteMeasurement(saved[1].id)
        assertEquals(listOf(saved[0].id), repo.measurements.first().map { it.id })
    }

    @Test
    fun versionThreeMigrationCreatesTheTableAndKeepsExistingData() = runBlocking {
        val repo = MuscuRepository(context, db)
        val exerciseId = repo.exercises.first().first().id
        repo.saveEntry(0, exerciseId, LocalDate.of(2026, 10, 1), 3, 10, 20.0)

        // Simule une base créée avant l'ajout des mensurations.
        val sqlite = db.openHelper.writableDatabase
        sqlite.execSQL("DROP TABLE measurements")
        sqlite.version = 3
        db.close()
        db = AppDatabase.create(context)

        val migrated = MuscuRepository(context, db)
        assertEquals(20.0, requireNotNull(migrated.latestEntryFor(exerciseId)).weightKg, 0.0)
        assertTrue(migrated.measurements.first().isEmpty())
        migrated.saveMeasurement(0, LocalDate.of(2026, 11, 8), bicepsCm = 38.0, shouldersCm = 111.0)
        assertEquals(1, migrated.measurements.first().size)
    }
}
