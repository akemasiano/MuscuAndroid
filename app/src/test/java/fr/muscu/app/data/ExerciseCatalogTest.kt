package fr.muscu.app.data

import android.content.Context
import android.graphics.BitmapFactory
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ExerciseCatalogTest {
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
    fun freshInstallIncludesTwentyIllustratedExercisesWithoutDuplicates() = runBlocking {
        val exercises = db.exerciseDao().observeAll().first()
        assertEquals(20, ExerciseCatalog.entries.size)
        assertEquals(23, exercises.size)
        ExerciseCatalog.entries.forEach { entry ->
            val exercise = exercises.single { it.name == entry.name }
            assertEquals(entry.startImagePath, exercise.imagePath)
            assertEquals(entry.endImagePath, exercise.endImagePath)
        }
    }

    @Test
    fun allFortyBundledImagesDecodeAndEachPairHasMatchingDimensions() {
        val paths = mutableSetOf<String>()
        ExerciseCatalog.entries.forEach { entry ->
            val sizes = listOf(entry.startImagePath, entry.endImagePath).map { path ->
                assertTrue(paths.add(path))
                val asset = path.removePrefix("file:///android_asset/")
                val bitmap = context.assets.open(asset).use(BitmapFactory::decodeStream)
                assertNotNull("Missing or invalid asset: $asset", bitmap)
                requireNotNull(bitmap)
                try {
                    assertTrue(bitmap.width in 1..1600)
                    assertTrue(bitmap.height in 1..1600)
                    bitmap.width to bitmap.height
                } finally {
                    bitmap.recycle()
                }
            }
            assertEquals(sizes[0], sizes[1])
        }
        assertEquals(40, paths.size)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun coilLoadsTheBundledUrisUsedByBothImageComponents() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val loader = ImageLoader(context)
        try {
            val entry = ExerciseCatalog.entries.first()
            for (path in listOf(entry.startImagePath, entry.endImagePath)) {
                val result = loader.execute(
                    ImageRequest.Builder(context).data(path).size(72).allowHardware(false).build(),
                )
                assertTrue("Coil failed to load $path: $result", result is SuccessResult)
            }
        } finally {
            loader.shutdown()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun versionTwoMigrationPreservesCustomImagesIdsAndHistory() = runBlocking {
        val sqlite = db.openHelper.writableDatabase
        sqlite.execSQL("DELETE FROM exercises")
        sqlite.execSQL("INSERT INTO exercises VALUES (7, ' DÉVELOPPÉ COUCHÉ ', 5, 'custom-start.jpg', 123, 'custom-end.jpg')")
        sqlite.execSQL("INSERT INTO exercises VALUES (8, 'Rowing barre', 1, NULL, 124, NULL)")
        sqlite.execSQL("INSERT INTO exercises VALUES (9, 'Tractions', 1, 'custom-pull-up.jpg', 125, NULL)")
        sqlite.execSQL("INSERT INTO exercises VALUES (10, 'Mon exercice', 4, NULL, 126, NULL)")
        sqlite.execSQL("INSERT INTO workout_entries VALUES (1, 7, 20000, 3, 10, 20, 123)")
        sqlite.version = 2
        db.close()
        db = AppDatabase.create(context)

        val exercises = db.exerciseDao().observeAll().first()
        assertEquals(21, exercises.size)
        val custom = requireNotNull(db.exerciseDao().getById(7))
        assertEquals("custom-start.jpg", custom.imagePath)
        assertEquals("custom-end.jpg", custom.endImagePath)
        assertEquals(5, custom.colorIndex)
        assertEquals(123L, custom.createdAt)
        val rowing = requireNotNull(db.exerciseDao().getById(8))
        assertEquals("${EXERCISE_ASSET_PREFIX}barbell_row_start.png", rowing.imagePath)
        assertEquals("${EXERCISE_ASSET_PREFIX}barbell_row_end.png", rowing.endImagePath)
        val pullUp = requireNotNull(db.exerciseDao().getById(9))
        assertEquals("custom-pull-up.jpg", pullUp.imagePath)
        assertNotNull(pullUp.endImagePath)
        assertEquals("Mon exercice", requireNotNull(db.exerciseDao().getById(10)).name)
        assertEquals(20.0, requireNotNull(db.entryDao().latestFor(7, -1)).weightKg, 0.0)
    }

    @Test
    fun deletionAndCustomizationPersistAfterReopening() = runBlocking {
        val repo = MuscuRepository(context, db)
        val exercises = repo.exercises.first()
        val deleted = exercises.single { it.name == "Développé militaire" }
        repo.saveEntry(0, deleted.id, LocalDate.now(), 3, 10, 20.0)
        repo.deleteExercise(deleted)
        val customized = exercises.single { it.name == "Rowing barre" }
        repo.saveExercise(customized.copy(imagePath = null, endImagePath = null))
        db.close()
        db = AppDatabase.create(context)
        assertNull(db.exerciseDao().getById(deleted.id))
        assertNull(db.entryDao().latestFor(deleted.id, -1))
        val saved = requireNotNull(db.exerciseDao().getById(customized.id))
        assertNull(saved.imagePath)
        assertNull(saved.endImagePath)
        assertEquals(22, db.exerciseDao().observeAll().first().size)
        val asset = requireNotNull(deleted.imagePath).removePrefix("file:///android_asset/")
        context.assets.open(asset).use { assertTrue(it.read() >= 0) }
    }
}
