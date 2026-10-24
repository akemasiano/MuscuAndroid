package fr.muscu.app.ui.week

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.muscu.app.data.Exercise
import fr.muscu.app.data.MuscuRepository
import fr.muscu.app.data.WorkoutEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class WeekUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val daysWithEntries: Set<LocalDate> = emptySet(),
    val daysWithWeightIncrease: Set<LocalDate> = emptySet(),
    val dayEntries: List<DayEntryUi> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val entryHistory: List<WorkoutEntry> = emptyList(),
    val weeklyStreak: Int = 0,
    val today: LocalDate = LocalDate.now(),
) {
    val weekStart: LocalDate
        get() = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}

class WeekViewModel(private val repo: MuscuRepository) : ViewModel() {

    private val selected = MutableStateFlow(LocalDate.now())
    private val today = flow {
        while (true) {
            emit(LocalDate.now())
            delay(60_000)
        }
    }.distinctUntilChanged()

    val state = combine(selected, repo.exercises, repo.entries, today) { date, exercises, entries, today ->
        val weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val range = weekStart.toEpochDay()..weekStart.plusDays(6).toEpochDay()
        val weekEntries = weekEntries(entries, exercises, weekStart)
        val day = date.toEpochDay()

        WeekUiState(
            selectedDate = date,
            daysWithEntries = entries.asSequence()
                .filter { it.epochDay in range }
                .map { LocalDate.ofEpochDay(it.epochDay) }
                .toSet(),
            daysWithWeightIncrease = weekEntries.asSequence()
                .filter { it.hasWeightIncrease }
                .map { LocalDate.ofEpochDay(it.entry.epochDay) }
                .toSet(),
            dayEntries = weekEntries.filter { it.entry.epochDay == day },
            exercises = exercises,
            entryHistory = entries,
            weeklyStreak = weeklyStreak(entries, date, today),
            today = today,
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
