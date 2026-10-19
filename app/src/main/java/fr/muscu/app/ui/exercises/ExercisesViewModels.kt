package fr.muscu.app.ui.exercises

import android.net.Uri
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
import java.io.File
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

    init {
        viewModelScope.launch {
            val existing = if (exerciseId > 0) repo.getExercise(exerciseId) else null
            original = existing
            _state.value = if (existing != null) {
                ExerciseEditState(true, false, existing.name, existing.colorIndex, existing.imagePath)
            } else {
                ExerciseEditState(loaded = true, colorIndex = (0 until MuscuColors.Palette.size).random())
            }
        }
    }

    fun setName(name: String) = _state.update { it.copy(name = name, error = null) }

    fun setColor(index: Int) = _state.update { it.copy(colorIndex = index) }

    fun removeImage() = replaceImage(null)

    fun onGalleryPicked(uri: Uri) {
        viewModelScope.launch {
            val path = repo.importImage(uri)
            if (path != null) replaceImage(path)
            else _state.update { it.copy(error = "Impossible d’importer cette image") }
        }
    }

    fun prepareCameraUri(): Uri {
        val file = repo.newImageFile()
        pendingCameraFile = file
        return repo.uriForFile(file)
    }

    fun onCameraResult(success: Boolean) {
        val file = pendingCameraFile ?: return
        pendingCameraFile = null
        if (success && file.length() > 0) replaceImage(file.absolutePath) else file.delete()
    }

    /** Remplace l'image affichée en supprimant l'image temporaire précédente (jamais l'originale). */
    private fun replaceImage(newPath: String?) {
        val current = _state.value.imagePath
        if (current != null && current != original?.imagePath) repo.deleteImage(current)
        _state.update { it.copy(imagePath = newPath) }
    }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        val name = s.name.trim()
        if (name.isEmpty()) {
            _state.update { it.copy(error = "Donne un nom à l’exercice") }
            return
        }
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            if (repo.nameExists(name, exerciseId)) {
                _state.update { it.copy(busy = false, error = "Un exercice porte déjà ce nom") }
                return@launch
            }
            val base = original ?: Exercise(name = name, colorIndex = s.colorIndex)
            repo.saveExercise(base.copy(name = name, colorIndex = s.colorIndex, imagePath = s.imagePath))
            original?.imagePath?.let { if (it != s.imagePath) repo.deleteImage(it) }
            committed = true
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val exercise = original ?: return
        viewModelScope.launch {
            val current = _state.value.imagePath
            if (current != exercise.imagePath) repo.deleteImage(current)
            repo.deleteExercise(exercise)
            committed = true
            onDone()
        }
    }

    override fun onCleared() {
        // Abandon : on supprime les images importées mais non enregistrées.
        if (!committed) {
            val current = _state.value.imagePath
            if (current != original?.imagePath) repo.deleteImage(current)
            pendingCameraFile?.delete()
        }
    }
}
