package fr.muscu.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import androidx.room.Room
import androidx.lifecycle.ViewModelStore
import fr.muscu.app.ui.exercises.ExerciseEditViewModel
import kotlinx.coroutines.CompletableDeferred
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
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ExerciseImagesTest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repo: MuscuRepository
    private val sources = mutableListOf<File>()
    private val imports = mutableListOf<String>()

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repo = MuscuRepository(context, db)
    }

    @After
    fun tearDown() {
        db.close()
        sources.forEach { it.delete() }
        imports.forEach(repo::deleteImage)
        context.deleteDatabase("muscu.db")
    }

    private fun source(width: Int, height: Int, transparent: Boolean = false): File {
        val file = File.createTempFile("phase-", if (transparent) ".png" else ".jpg", context.cacheDir)
        sources += file
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setHasAlpha(transparent)
        bitmap.eraseColor(if (transparent) 0x4080FF00 else 0xFF80FF00.toInt())
        file.outputStream().use {
            assertTrue(bitmap.compress(
                if (transparent) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, 95, it,
            ))
        }
        bitmap.recycle()
        return file
    }

    private suspend fun import(file: File): File {
        val path = repo.importImage(Uri.fromFile(file))
        assertNotNull(path)
        imports += requireNotNull(path)
        return File(path)
    }

    private fun dimensions(file: File): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        return options.outWidth to options.outHeight
    }

    @Test
    fun limitsBothLandscapeAndPortraitWithoutUpscaling() = runBlocking {
        for ((width, height, expected) in listOf(
            Triple(2400, 1200, 1600 to 800),
            Triple(1200, 2400, 800 to 1600),
            Triple(1600, 800, 1600 to 800),
            Triple(320, 200, 320 to 200),
        )) {
            val output = import(source(width, height))
            assertEquals(expected, dimensions(output))
            assertEquals("jpg", output.extension)
        }
    }

    @Test
    fun preservesTransparencyInPng() = runBlocking {
        val output = import(source(2400, 1200, transparent = true))
        assertEquals("png", output.extension)
        assertEquals(1600 to 800, dimensions(output))
        val bitmap = BitmapFactory.decodeFile(output.absolutePath)
        try {
            assertTrue(bitmap.hasAlpha())
            assertEquals(0x40, bitmap.getPixel(100, 100).ushr(24))
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun appliesExifOrientation() = runBlocking {
        val input = source(2400, 1200)
        ExifInterface(input.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }
        assertEquals(800 to 1600, dimensions(import(input)))
    }

    @Test
    fun rejectsInvalidImageWithoutLeavingOutput() = runBlocking {
        val invalid = File.createTempFile("invalid-phase-", ".jpg", context.cacheDir)
        sources += invalid
        invalid.writeText("not an image")
        val dir = File(context.filesDir, "exercise_images")
        val before = dir.listFiles()?.map { it.name }?.toSet().orEmpty()
        assertNull(repo.importImage(Uri.fromFile(invalid)))
        assertEquals(before, dir.listFiles()?.map { it.name }?.toSet().orEmpty())
    }

    @Test
    fun savesBothImagesAndDeletesBothWithHistory() = runBlocking {
        val start = import(source(100, 200))
        val end = import(source(100, 200))
        val id = repo.saveExercise(Exercise(name = "Test", colorIndex = 1,
            imagePath = start.absolutePath, endImagePath = end.absolutePath))
        repo.saveEntry(0, id, java.time.LocalDate.now(), 3, 10, 20.0)
        val saved = requireNotNull(repo.getExercise(id))
        assertEquals(start.absolutePath, saved.imagePath)
        assertEquals(end.absolutePath, saved.endImagePath)
        repo.deleteExercise(saved)
        assertNull(repo.getExercise(id))
        assertNull(repo.latestEntryFor(id, -1))
        assertFalse(start.exists())
        assertFalse(end.exists())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun swappingAndReplacingKeepsOriginalsUntilSave() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val start = import(source(100, 200))
            val end = import(source(100, 200))
            val id = repo.saveExercise(Exercise(name = "Test", colorIndex = 1,
                imagePath = start.absolutePath, endImagePath = end.absolutePath))
            val vm = ExerciseEditViewModel(repo, id)
            store.put("exercise", vm)
            vm.state.first { it.loaded }
            vm.swapImages()
            assertEquals(end.absolutePath, vm.state.value.imagePath)
            assertEquals(start.absolutePath, vm.state.value.endImagePath)
            vm.selectImagePhase(isEnd = true)
            vm.onGalleryPicked(Uri.fromFile(source(200, 100)))
            vm.state.first { !it.importing }
            val replacement = requireNotNull(vm.state.value.endImagePath)
            imports += replacement
            assertTrue(start.exists())
            assertTrue(end.exists())
            val saved = CompletableDeferred<Unit>()
            vm.save { saved.complete(Unit) }
            saved.await()
            val exercise = requireNotNull(repo.getExercise(id))
            assertEquals(end.absolutePath, exercise.imagePath)
            assertEquals(replacement, exercise.endImagePath)
            assertFalse(start.exists())
            store.clear()
            assertTrue(end.exists())
            assertTrue(File(replacement).exists())
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun abandoningEditDeletesOnlyTemporaryImages() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val start = import(source(100, 200))
            val end = import(source(100, 200))
            val id = repo.saveExercise(Exercise(name = "Test", colorIndex = 1,
                imagePath = start.absolutePath, endImagePath = end.absolutePath))
            val vm = ExerciseEditViewModel(repo, id)
            store.put("exercise", vm)
            vm.state.first { it.loaded }
            vm.swapImages()
            vm.selectImagePhase(isEnd = false)
            vm.onGalleryPicked(Uri.fromFile(source(200, 100)))
            vm.state.first { !it.importing }
            val temporary = requireNotNull(vm.state.value.imagePath)
            imports += temporary
            vm.removeImage(isEnd = true)
            store.clear()
            assertFalse(File(temporary).exists())
            assertTrue(start.exists())
            assertTrue(end.exists())
            assertEquals(start.absolutePath, requireNotNull(repo.getExercise(id)).imagePath)
            assertEquals(end.absolutePath, requireNotNull(repo.getExercise(id)).endImagePath)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun migratesOldImagesAndPreservesWorkoutHistory() = runBlocking {
        val file = context.getDatabasePath("muscu.db")
        file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            old.execSQL("CREATE TABLE exercises (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, colorIndex INTEGER NOT NULL, imagePath TEXT, createdAt INTEGER NOT NULL)")
            old.execSQL("CREATE TABLE workout_entries (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, exerciseId INTEGER NOT NULL, epochDay INTEGER NOT NULL, sets INTEGER NOT NULL, reps INTEGER NOT NULL, weightKg REAL NOT NULL, createdAt INTEGER NOT NULL, FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            old.execSQL("CREATE INDEX index_workout_entries_exerciseId ON workout_entries(exerciseId)")
            old.execSQL("CREATE INDEX index_workout_entries_epochDay ON workout_entries(epochDay)")
            old.execSQL("INSERT INTO exercises VALUES (1, 'Test', 2, 'old.jpg', 123)")
            old.execSQL("INSERT INTO workout_entries VALUES (1, 1, 20000, 3, 10, 20, 123)")
            old.version = 1
        }
        val migrated = AppDatabase.create(context)
        try {
            val exercise = requireNotNull(migrated.exerciseDao().getById(1))
            assertEquals("old.jpg", exercise.imagePath)
            assertNull(exercise.endImagePath)
            assertEquals(123L, exercise.createdAt)
            assertEquals(20.0, requireNotNull(migrated.entryDao().latestFor(1, -1)).weightKg, 0.0)
            assertEquals(1L, exercise.id)
        } finally {
            migrated.close()
        }
    }
}
