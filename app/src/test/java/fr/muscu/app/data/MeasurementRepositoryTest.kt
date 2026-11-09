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

        repo.saveMeasurement(0, day, bicepsLeftCm = 37.5, bicepsRightCm = 38.5, shouldersCm = null)
        repo.saveMeasurement(0, day.plusDays(7), bicepsLeftCm = null, bicepsRightCm = null, shouldersCm = 112.0)
        val saved = repo.measurements.first()
        assertEquals(listOf(day.toEpochDay(), day.plusDays(7).toEpochDay()), saved.map { it.epochDay })
        assertEquals(37.5, requireNotNull(saved[0].bicepsLeftCm), 0.0)
        assertEquals(38.5, requireNotNull(saved[0].bicepsRightCm), 0.0)
        assertNull(saved[0].shouldersCm)
        assertNull(saved[1].bicepsLeftCm)
        assertNull(saved[1].bicepsRightCm)

        repo.saveMeasurement(saved[0].id, day, bicepsLeftCm = 38.0, bicepsRightCm = 39.0, shouldersCm = 110.5)
        val edited = repo.measurements.first()
        assertEquals(2, edited.size)
        assertEquals(38.0, requireNotNull(edited[0].bicepsLeftCm), 0.0)
        assertEquals(39.0, requireNotNull(edited[0].bicepsRightCm), 0.0)
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
        migrated.saveMeasurement(
            0, LocalDate.of(2026, 11, 8),
            bicepsLeftCm = 37.0, bicepsRightCm = 38.0, shouldersCm = 111.0,
        )
        assertEquals(1, migrated.measurements.first().size)
    }

    @Test
    fun versionFourMigrationKeepsOldBicepsAsRightBiceps() = runBlocking {
        // Simule une base en version 4 : un seul champ « bicepsCm » sans distinction de côté.
        val sqlite = db.openHelper.writableDatabase
        sqlite.execSQL("DROP TABLE measurements")
        sqlite.execSQL(
            "CREATE TABLE measurements (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "epochDay INTEGER NOT NULL, " +
                "bicepsCm REAL, " +
                "shouldersCm REAL, " +
                "createdAt INTEGER NOT NULL)"
        )
        sqlite.execSQL("CREATE INDEX index_measurements_epochDay ON measurements (epochDay)")
        sqlite.execSQL("INSERT INTO measurements (epochDay, bicepsCm, shouldersCm, createdAt) VALUES (20000, 38.5, 111.0, 1)")
        sqlite.execSQL("INSERT INTO measurements (epochDay, bicepsCm, shouldersCm, createdAt) VALUES (20007, NULL, 112.0, 2)")
        sqlite.version = 4
        db.close()
        db = AppDatabase.create(context)

        val migrated = MuscuRepository(context, db)
        val rows = migrated.measurements.first()
        assertEquals(2, rows.size)
        assertNull(rows[0].bicepsLeftCm)
        assertEquals(38.5, requireNotNull(rows[0].bicepsRightCm), 0.0)
        assertEquals(111.0, requireNotNull(rows[0].shouldersCm), 0.0)
        assertNull(rows[1].bicepsLeftCm)
        assertNull(rows[1].bicepsRightCm)
        assertEquals(112.0, requireNotNull(rows[1].shouldersCm), 0.0)

        // La table migrée reste utilisable.
        migrated.saveMeasurement(0, LocalDate.of(2026, 11, 9), bicepsLeftCm = 37.0, bicepsRightCm = null, shouldersCm = null)
        assertEquals(3, migrated.measurements.first().size)
    }
}
