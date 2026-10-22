package fr.muscu.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
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
        deleteImage(exercise.endImagePath)
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
        get() = File(context.filesDir, "exercise_images").apply {
            if (!isDirectory && !mkdirs()) throw IOException("Unable to create image directory")
        }

    fun newImageFile(): File = File(imagesDir, "${UUID.randomUUID()}.jpg")

    fun uriForFile(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    suspend fun importImage(uri: Uri): String? {
        var target: File? = null
        return try {
            withContext(Dispatchers.IO) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val longest = maxOf(info.size.width, info.size.height)
                    if (longest > 1600) {
                        decoder.setTargetSize(
                            maxOf(1, (info.size.width * 1600L / longest).toInt()),
                            maxOf(1, (info.size.height * 1600L / longest).toInt()),
                        )
                    }
                }
                try {
                    val transparent = bitmap.hasAlpha()
                    val output = File(imagesDir, "${UUID.randomUUID()}.${if (transparent) "png" else "jpg"}")
                    target = output
                    output.outputStream().use { stream ->
                        val format = if (transparent) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                        if (!bitmap.compress(format, 90, stream)) throw IOException("Image encoding failed")
                    }
                    output.absolutePath
                } finally {
                    bitmap.recycle()
                }
            }
        } catch (e: CancellationException) {
            deleteImage(target?.absolutePath)
            throw e
        } catch (e: IOException) {
            Log.e("MuscuImages", "Unable to import image", e)
            deleteImage(target?.absolutePath)
            null
        } catch (e: SecurityException) {
            Log.e("MuscuImages", "Image access denied", e)
            deleteImage(target?.absolutePath)
            null
        } catch (e: IllegalArgumentException) {
            Log.e("MuscuImages", "Unsupported image", e)
            deleteImage(target?.absolutePath)
            null
        }
    }

    fun deleteImage(path: String?) {
        if (path == null || path.startsWith(EXERCISE_ASSET_PREFIX)) return
        try {
            val file = File(path)
            if (file.exists() && !file.delete()) Log.w("MuscuImages", "Unable to delete image")
        } catch (e: SecurityException) {
            Log.e("MuscuImages", "Image deletion denied", e)
        }
    }
}
