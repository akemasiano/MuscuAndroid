package fr.muscu.app.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.util.UUID

class MuscuRepository(
    private val context: Context,
    private val db: AppDatabase,
) {
    private val exerciseDao = db.exerciseDao()
    private val entryDao = db.entryDao()

    val exercises = exerciseDao.observeAll()
    val entries = entryDao.observeAll()

    suspend fun getExercise(id: Long) = exerciseDao.getById(id)

    suspend fun nameExists(name: String, excludeId: Long) =
        exerciseDao.countWithName(name, excludeId) > 0

    suspend fun saveExercise(exercise: Exercise): Long =
        if (exercise.id == 0L) exerciseDao.insert(exercise)
        else exerciseDao.update(exercise).let { exercise.id }

    suspend fun deleteExercise(exercise: Exercise) {
        db.withTransaction {
            entryDao.deleteForExercise(exercise.id)
            exerciseDao.delete(exercise.id)
        }
        deleteImage(exercise.imagePath)
    }

    /** Dernière valeur saisie pour cet exercice (pour pré-remplir la saisie). */
    suspend fun latestEntryFor(exerciseId: Long, excludeEntryId: Long = -1) =
        entryDao.latestFor(exerciseId, excludeEntryId)

    suspend fun saveEntry(
        id: Long,
        exerciseId: Long,
        date: LocalDate,
        sets: Int,
        reps: Int,
        weightKg: Double,
    ) {
        val entry = WorkoutEntry(
            id = id,
            exerciseId = exerciseId,
            epochDay = date.toEpochDay(),
            sets = sets,
            reps = reps,
            weightKg = weightKg,
        )
        if (id == 0L) entryDao.insert(entry) else entryDao.update(entry)
    }

    suspend fun deleteEntry(id: Long) = entryDao.delete(id)

    // --- Images d'exercice (stockées dans le stockage interne de l'app) ---

    private val imagesDir: File
        get() = File(context.filesDir, "exercise_images").apply { mkdirs() }

    fun newImageFile(): File = File(imagesDir, "${UUID.randomUUID()}.jpg")

    fun uriForFile(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    suspend fun importImage(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val target = newImageFile()
            context.contentResolver.openInputStream(uri)!!.use { input ->
                target.outputStream().use { input.copyTo(it) }
            }
            target.absolutePath
        }.getOrNull()
    }

    fun deleteImage(path: String?) {
        if (path != null) runCatching { File(path).delete() }
    }
}
