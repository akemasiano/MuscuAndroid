package fr.muscu.app.ui.week

import fr.muscu.app.data.WorkoutEntry
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

internal fun weeklyStreak(entries: List<WorkoutEntry>, date: LocalDate, today: LocalDate): Int {
    val monday = TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
    val currentWeek = today.with(monday)
    var week = date.with(monday)
    if (week > currentWeek) return 0

    val validWeeks = entries.asSequence()
        .filter { it.epochDay <= today.toEpochDay() }
        .groupBy { LocalDate.ofEpochDay(it.epochDay).with(monday) }
        .filterValues { weekEntries -> weekEntries.map { it.exerciseId }.toSet().size >= 3 }
        .keys

    // The current week has until Sunday to qualify; completed weeks have no grace period.
    if (week == currentWeek && week !in validWeeks) {
        week = week.minusWeeks(1)
    }

    var streak = 0
    while (week in validWeeks) {
        streak++
        week = week.minusWeeks(1)
    }
    return streak
}
