package fr.muscu.app.ui.exercises

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.muscu.app.data.Exercise
import fr.muscu.app.data.MuscuRepository
import fr.muscu.app.data.WorkoutEntry
import fr.muscu.app.ui.theme.MuscuColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.Normalizer

data class ExerciseItem(val exercise: Exercise, val last: WorkoutEntry?)

private fun String.normalized() =
    Normalizer.normalize(this, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase().trim()

class ExercisesViewModel(repo: MuscuRepository) : ViewModel() {
    val query = MutableStateFlow("")

    val items = combine(query, repo.exercises, repo.entries) { q, exercises, entries ->
        val lastByExercise = entries.groupBy { it.exerciseId }.mapValues { it.value.last() }
        val needle = q.normalized()
        exercises
            .filter { needle.isEmpty() || it.name.normalized().contains(needle) }
            .map { ExerciseItem(it, lastByExercise[it.id]) }
            .sortedWith(
                compareByDescending<ExerciseItem> { it.last?.epochDay ?: Long.MIN_VALUE }
                    .thenByDescending { it.last?.id ?: 0 }
                    .thenBy { it.exercise.name.normalized() }
            )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

data class ExerciseEditState(
    val loaded: Boolean = false,
    val isNew: Boolean = true,
    val name: String = "",
    val colorIndex: Int = 0,
    val imagePath: String? = null,
    val error: String? = null,
    val busy: Boolean = false,
    val endImagePath: String? = null,
    val imageError: String? = null,
    val importing: Boolean = false,
)

class ExerciseEditViewModel(
    private val repo: MuscuRepository,
    private val exerciseId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(ExerciseEditState())
    val state = _state.asStateFlow()

    private var original: Exercise? = null
    private var pendingCameraFile: File? = null
    private var committed = false
    private var pickingEnd = false
    private var cleared = false

    init {
        viewModelScope.launch {
            val existing = if (exerciseId > 0) repo.getExercise(exerciseId) else null
            original = existing
            _state.value = if (existing != null) {
                ExerciseEditState(
                    loaded = true, isNew = false, name = existing.name,
                    colorIndex = existing.colorIndex, imagePath = existing.imagePath,
                    endImagePath = existing.endImagePath,
                )
            } else {
                ExerciseEditState(loaded = true, colorIndex = (0 until MuscuColors.Palette.size).random())
            }
        }
    }

    fun setName(name: String) = _state.update { it.copy(name = name, error = null) }

    fun setColor(index: Int) = _state.update { it.copy(colorIndex = index) }

    fun selectImagePhase(isEnd: Boolean) {
        pickingEnd = isEnd
        _state.update { it.copy(imageError = null) }
    }

    fun removeImage(isEnd: Boolean) = replaceImage(null, isEnd)

    fun swapImages() {
        if (_state.value.importing || _state.value.busy) return
        _state.update { it.copy(imagePath = it.endImagePath, endImagePath = it.imagePath) }
    }

    fun onGalleryPicked(uri: Uri) {
        if (_state.value.importing || _state.value.busy) return
        val isEnd = pickingEnd
        _state.update { it.copy(importing = true, imageError = null) }
        viewModelScope.launch {
            try {
                val path = repo.importImage(uri)
                if (path != null) replaceImage(path, isEnd)
                else _state.update { it.copy(imageError = "Impossible d’importer cette image") }
            } finally {
                _state.update { it.copy(importing = false) }
            }
        }
    }

    fun prepareCameraUri(): Uri? {
        return try {
            val file = repo.newImageFile()
            pendingCameraFile = file
            repo.uriForFile(file)
        } catch (e: IOException) {
            cameraFailed(e)
            null
        } catch (e: IllegalArgumentException) {
            cameraFailed(e)
            null
        } catch (e: SecurityException) {
            cameraFailed(e)
            null
        }
    }

    private fun cameraFailed(e: Exception) {
        Log.e("MuscuImages", "Unable to prepare camera", e)
        repo.deleteImage(pendingCameraFile?.absolutePath)
        pendingCameraFile = null
        _state.update { it.copy(imageError = "Impossible d’ouvrir l’appareil photo") }
    }

    fun onCameraResult(success: Boolean) {
        val file = pendingCameraFile ?: return
        pendingCameraFile = null
        if (!success) {
            repo.deleteImage(file.absolutePath)
            return
        }
        val isEnd = pickingEnd
        _state.update { it.copy(importing = true, imageError = null) }
        viewModelScope.launch {
            try {
                val path = repo.importImage(Uri.fromFile(file))
                if (path != null) replaceImage(path, isEnd)
                else _state.update { it.copy(imageError = "Impossible d’importer la photo") }
            } finally {
                repo.deleteImage(file.absolutePath)
                _state.update { it.copy(importing = false) }
            }
        }
    }

    /** Remplace l'image affichée en supprimant l'image temporaire précédente (jamais l'originale). */
    private fun replaceImage(newPath: String?, isEnd: Boolean) {
        val current = if (isEnd) _state.value.endImagePath else _state.value.imagePath
        _state.update {
            if (isEnd) it.copy(endImagePath = newPath, imageError = null)
            else it.copy(imagePath = newPath, imageError = null)
        }
        if (current != null && current !in originalPaths() && current !in currentPaths()) repo.deleteImage(current)
    }

    private fun originalPaths() = listOfNotNull(original?.imagePath, original?.endImagePath).toSet()

    private fun currentPaths() = listOfNotNull(_state.value.imagePath, _state.value.endImagePath).toSet()

    fun save(onDone: () -> Unit) {
        val s = _state.value
        if (!s.loaded || s.busy || s.importing) return
        val name = s.name.trim()
        if (name.isEmpty()) {
            _state.update { it.copy(error = "Donne un nom à l’exercice") }
            return
        }
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                if (repo.nameExists(name, exerciseId)) {
                    _state.update { it.copy(busy = false, error = "Un exercice porte déjà ce nom") }
                    return@launch
                }
                val base = original ?: Exercise(name = name, colorIndex = s.colorIndex)
                // The database commit and image ownership must finish together, even on navigation.
                withContext(NonCancellable) {
                    repo.saveExercise(base.copy(
                        name = name, colorIndex = s.colorIndex,
                        imagePath = s.imagePath, endImagePath = s.endImagePath,
                    ))
                    (originalPaths() - currentPaths()).forEach(repo::deleteImage)
                    committed = true
                }
                if (!cleared) onDone()
            } catch (e: android.database.sqlite.SQLiteException) {
                Log.e("MuscuExercises", "Unable to save exercise", e)
                _state.update { it.copy(busy = false, error = "Impossible d’enregistrer l’exercice") }
            } finally {
                if (cleared && !committed) cleanupImages()
            }
        }
    }

    fun delete(onDone: () -> Unit) {
        val exercise = original ?: return
        if (_state.value.busy || _state.value.importing) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                withContext(NonCancellable) {
                    repo.deleteExercise(exercise)
                    (currentPaths() - originalPaths()).forEach(repo::deleteImage)
                    committed = true
                }
                if (!cleared) onDone()
            } catch (e: android.database.sqlite.SQLiteException) {
                Log.e("MuscuExercises", "Unable to delete exercise", e)
                _state.update { it.copy(busy = false, error = "Impossible de supprimer l’exercice") }
            } finally {
                if (cleared && !committed) cleanupImages()
            }
        }
    }

    override fun onCleared() {
        cleared = true
        if (!committed && !_state.value.busy) cleanupImages()
    }

    private fun cleanupImages() {
        (currentPaths() - originalPaths()).forEach(repo::deleteImage)
        repo.deleteImage(pendingCameraFile?.absolutePath)
    }
}
