package fr.muscu.app.ui.week

import fr.muscu.app.data.Exercise
import fr.muscu.app.data.WorkoutEntry
import java.time.LocalDate

data class DayEntryUi(
    val entry: WorkoutEntry,
    val exercise: Exercise,
    val previous: WorkoutEntry?,
) {
    val hasWeightIncrease: Boolean
        get() = previous?.let { entry.weightKg > it.weightKg } ?: false
}

internal fun weekEntries(
    entries: List<WorkoutEntry>,
    exercises: List<Exercise>,
    weekStart: LocalDate,
): List<DayEntryUi> {
    val exercisesById = exercises.associateBy { it.id }
    val range = weekStart.toEpochDay()..weekStart.plusDays(6).toEpochDay()
    val previousByExercise = mutableMapOf<Long, WorkoutEntry>()
    return entries.sortedWith(compareBy<WorkoutEntry> { it.epochDay }.thenBy { it.id })
        .mapNotNull { entry ->
            val previous = previousByExercise.put(entry.exerciseId, entry)
            if (entry.epochDay !in range) return@mapNotNull null
            val exercise = exercisesById[entry.exerciseId] ?: return@mapNotNull null
            DayEntryUi(entry, exercise, previous)
        }
}
