package fr.muscu.app.ui.week

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.muscu.app.data.Exercise
import fr.muscu.app.data.MuscuRepository
import fr.muscu.app.data.WorkoutEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class DayEntryUi(
    val entry: WorkoutEntry,
    val exercise: Exercise,
    /** Entrée précédente du même exercice (avant celle-ci). */
    val previous: WorkoutEntry?,
)

data class WeekUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val daysWithEntries: Set<LocalDate> = emptySet(),
    val dayEntries: List<DayEntryUi> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
) {
    val weekStart: LocalDate
        get() = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}

class WeekViewModel(private val repo: MuscuRepository) : ViewModel() {

    private val selected = MutableStateFlow(LocalDate.now())

    val state = combine(selected, repo.exercises, repo.entries) { date, exercises, entries ->
        val exercisesById = exercises.associateBy { it.id }
        val weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val range = weekStart.toEpochDay()..weekStart.plusDays(6).toEpochDay()
        // entries est trié par (epochDay, id) : l'élément précédent est donc la séance précédente.
        val byExercise = entries.groupBy { it.exerciseId }
        val day = date.toEpochDay()

        WeekUiState(
            selectedDate = date,
            daysWithEntries = entries.asSequence()
                .filter { it.epochDay in range }
                .map { LocalDate.ofEpochDay(it.epochDay) }
                .toSet(),
            dayEntries = entries.filter { it.epochDay == day }.mapNotNull { entry ->
                val exercise = exercisesById[entry.exerciseId] ?: return@mapNotNull null
                val history = byExercise.getValue(entry.exerciseId)
                DayEntryUi(entry, exercise, history.getOrNull(history.indexOf(entry) - 1))
            },
            exercises = exercises,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeekUiState())

    fun selectDay(date: LocalDate) {
        selected.value = date
    }

    fun previousWeek() {
        selected.value = selected.value.minusWeeks(1)
    }

    fun nextWeek() {
        selected.value = selected.value.plusWeeks(1)
    }

    fun goToToday() {
        selected.value = LocalDate.now()
    }

    suspend fun latestFor(exerciseId: Long, excludeEntryId: Long) =
        repo.latestEntryFor(exerciseId, excludeEntryId)

    fun saveEntry(id: Long, exerciseId: Long, date: LocalDate, sets: Int, reps: Int, weight: Double) {
        viewModelScope.launch { repo.saveEntry(id, exerciseId, date, sets, reps, weight) }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch { repo.deleteEntry(id) }
    }
}
