package fr.muscu.app.ui.week

import fr.muscu.app.data.WorkoutEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WeeklyStreakTest {
    private val today = LocalDate.of(2026, 10, 21)
    private val monday = LocalDate.of(2026, 10, 19)

    private fun entry(date: LocalDate, exerciseId: Long) = WorkoutEntry(
        exerciseId = exerciseId,
        epochDay = date.toEpochDay(),
        sets = 3,
        reps = 10,
        weightKg = 20.0,
    )

    private fun validWeek(start: LocalDate) = (1L..3L).map { entry(start, it) }

    @Test
    fun emptyHistoryHasNoStreak() {
        assertEquals(0, weeklyStreak(emptyList(), today, today))
    }

    @Test
    fun weekRequiresThreeDistinctExercises() {
        val entries = validWeek(monday)
        assertEquals(0, weeklyStreak(entries.take(2), today, today))
        assertEquals(1, weeklyStreak(entries, today, today))
        assertEquals(1, weeklyStreak(entries + entry(today, 4), today, today))
    }

    @Test
    fun repeatedExerciseAcrossDaysCountsOnlyOnce() {
        val entries = listOf(
            entry(monday, 1),
            entry(monday.plusDays(1), 1),
            entry(today, 1),
            entry(today, 2),
        )
        assertEquals(0, weeklyStreak(entries, today, today))
        assertEquals(1, weeklyStreak(entries + entry(today, 3), today, today))
    }

    @Test
    fun distinctExercisesCanBeSpreadAcrossTheWeek() {
        val entries = listOf(entry(monday, 1), entry(monday.plusDays(1), 2), entry(today, 3))
        assertEquals(1, weeklyStreak(entries, today, today))
    }

    @Test
    fun currentIncompleteWeekPreservesPreviousStreak() {
        val entries = validWeek(monday.minusWeeks(1)) + validWeek(monday.minusWeeks(2)) +
            validWeek(monday).take(2)
        assertEquals(2, weeklyStreak(entries, today, today))
        assertEquals(3, weeklyStreak(entries + entry(today, 3), today, today))
    }

    @Test
    fun missingCompletedWeekBreaksStreak() {
        val entries = validWeek(monday) + validWeek(monday.minusWeeks(2))
        assertEquals(1, weeklyStreak(entries, today, today))
        assertEquals(0, weeklyStreak(entries.filter { it.epochDay < monday.toEpochDay() }, today, today))
    }

    @Test
    fun completedIncompleteWeekDoesNotHaveGracePeriod() {
        val previous = monday.minusWeeks(1)
        val entries = validWeek(previous).take(2) + validWeek(previous.minusWeeks(1))
        assertEquals(0, weeklyStreak(entries, previous, today))
        assertEquals(0, weeklyStreak(entries, today, today))
    }

    @Test
    fun historicalWeekShowsStreakAtThatTime() {
        val previous = monday.minusWeeks(1)
        val entries = validWeek(monday) + validWeek(previous) + validWeek(previous.minusWeeks(1))
        assertEquals(2, weeklyStreak(entries, previous.plusDays(6), today))
        assertEquals(3, weeklyStreak(entries, today, today))
    }

    @Test
    fun sundayBelongsToPreviousWeekAndMondayStartsNewWeek() {
        val sunday = monday.minusDays(1)
        val entries = validWeek(sunday).take(2) + entry(monday, 3)
        assertEquals(0, weeklyStreak(entries, today, today))
        assertEquals(0, weeklyStreak(entries, sunday, today))
        assertEquals(1, weeklyStreak(entries + entry(sunday, 3), today, today))
    }

    @Test
    fun streakContinuesAcrossIsoYearBoundary() {
        val newYearWeek = LocalDate.of(2025, 12, 29)
        val entries = validWeek(newYearWeek) + validWeek(newYearWeek.minusWeeks(1))
        assertEquals(2, weeklyStreak(entries, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 4)))
    }

    @Test
    fun futureEntriesDoNotQualifyCurrentWeek() {
        val entries = validWeek(monday).take(2) + entry(today.plusDays(1), 3)
        assertEquals(0, weeklyStreak(entries, today, today))
        assertEquals(1, weeklyStreak(entries, today, today.plusDays(1)))
    }

    @Test
    fun futureWeekHasNoStreakEvenWithEntries() {
        val future = monday.plusWeeks(1)
        val entries = validWeek(monday) + validWeek(future)
        assertEquals(0, weeklyStreak(entries, future, today))
    }

    @Test
    fun incompleteCurrentWeekPreservesStreakUntilSundayThenBreaksIt() {
        val entries = validWeek(monday.minusWeeks(1)) + validWeek(monday).take(2)
        val sunday = monday.plusDays(6)
        assertEquals(1, weeklyStreak(entries, sunday, sunday))
        assertEquals(0, weeklyStreak(entries, sunday.plusDays(1), sunday.plusDays(1)))
    }

    @Test
    fun deletingAnExerciseRecalculatesStreak() {
        val entries = validWeek(monday.minusWeeks(1)) + validWeek(monday)
        assertEquals(2, weeklyStreak(entries, today, today))
        val deleted = entries.filterNot { it.epochDay == monday.toEpochDay() && it.exerciseId == 3L }
        assertEquals(1, weeklyStreak(deleted, today, today))
        assertEquals(0, weeklyStreak(deleted.filterNot { it.exerciseId == 3L }, today, today))
    }

    @Test
    fun entryOrderDoesNotAffectStreak() {
        val entries = validWeek(monday) + validWeek(monday.minusWeeks(1))
        assertEquals(2, weeklyStreak(entries.reversed(), today, today))
    }
}
