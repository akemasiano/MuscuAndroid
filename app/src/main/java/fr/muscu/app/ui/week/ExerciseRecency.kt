package fr.muscu.app.ui.week

import fr.muscu.app.data.WorkoutEntry
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

internal fun weeksSinceLastUse(
    entries: List<WorkoutEntry>,
    date: LocalDate,
    excludeEntryId: Long? = null,
): Map<Long, Long> {
    val monday = TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
    val selectedWeek = date.with(monday)
    return entries.asSequence()
        .filter { it.id != excludeEntryId && it.epochDay <= date.toEpochDay() }
        .groupBy { it.exerciseId }
        .mapValues { (_, history) ->
            val lastDate = LocalDate.ofEpochDay(history.maxOf { it.epochDay })
            ChronoUnit.WEEKS.between(lastDate.with(monday), selectedWeek)
        }
}

internal fun exerciseRecencyLabel(weeks: Long?): String = when (weeks) {
    null -> "Jamais utilisé"
    0L -> "Utilisé cette semaine"
    1L -> "Dernière utilisation : la semaine dernière"
    else -> "Dernière utilisation : il y a $weeks semaines"
}
