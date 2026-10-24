package fr.muscu.app.ui.week

import fr.muscu.app.data.WorkoutEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExerciseRecencyTest {
    private val monday = LocalDate.of(2026, 10, 19)

    private fun entry(id: Long, date: LocalDate, exerciseId: Long = 1) = WorkoutEntry(
        id = id,
        exerciseId = exerciseId,
        epochDay = date.toEpochDay(),
        sets = 3,
        reps = 10,
        weightKg = 20.0,
    )

    @Test
    fun emptyHistoryMeansNeverUsed() {
        val recency = weeksSinceLastUse(emptyList(), monday)
        assertTrue(recency.isEmpty())
        assertEquals("Jamais utilisé", exerciseRecencyLabel(recency[1]))
    }

    @Test
    fun earlierDayInSameWeekCountsAsThisWeek() {
        val recency = weeksSinceLastUse(listOf(entry(1, monday)), monday.plusDays(6))
        assertEquals(0L, recency[1])
        assertEquals("Utilisé cette semaine", exerciseRecencyLabel(recency[1]))
    }

    @Test
    fun sundayToMondayCountsAsOneWeek() {
        val recency = weeksSinceLastUse(listOf(entry(1, monday.minusDays(1))), monday)
        assertEquals(1L, recency[1])
        assertEquals("Dernière utilisation : la semaine dernière", exerciseRecencyLabel(recency[1]))
    }

    @Test
    fun severalCalendarWeeksAreShown() {
        val recency = weeksSinceLastUse(listOf(entry(1, monday.minusWeeks(3).plusDays(4))), monday)
        assertEquals(3L, recency[1])
        assertEquals("Dernière utilisation : il y a 3 semaines", exerciseRecencyLabel(recency[1]))
    }

    @Test
    fun mostRecentDateWinsRegardlessOfInputOrderOrId() {
        val recency = weeksSinceLastUse(listOf(
            entry(2, monday.minusWeeks(3)),
            entry(1, monday.minusWeeks(1)),
            entry(3, monday.minusWeeks(5)),
        ), monday)
        assertEquals(1L, recency[1])
    }

    @Test
    fun eachExerciseHasIndependentHistory() {
        val recency = weeksSinceLastUse(listOf(
            entry(1, monday.minusWeeks(3)),
            entry(2, monday.minusWeeks(1), exerciseId = 2),
        ), monday)
        assertEquals(mapOf(1L to 3L, 2L to 1L), recency)
        assertEquals(null, recency[3])
    }

    @Test
    fun entriesAfterSelectedDateAreIgnoredEvenInSameWeek() {
        val recency = weeksSinceLastUse(listOf(
            entry(1, monday.minusWeeks(3)),
            entry(2, monday.plusDays(1)),
            entry(3, monday.plusWeeks(1), exerciseId = 2),
        ), monday)
        assertEquals(mapOf(1L to 3L), recency)
    }

    @Test
    fun sameDayEntriesAreIncluded() {
        val recency = weeksSinceLastUse(listOf(entry(1, monday), entry(2, monday)), monday)
        assertEquals(mapOf(1L to 0L), recency)
    }

    @Test
    fun editedEntryDoesNotCountAsPreviousUse() {
        val entries = listOf(entry(1, monday.minusWeeks(2)), entry(2, monday))
        assertEquals(mapOf(1L to 2L), weeksSinceLastUse(entries, monday, excludeEntryId = 2))
        assertTrue(weeksSinceLastUse(entries.takeLast(1), monday, excludeEntryId = 2).isEmpty())
    }

    @Test
    fun handlesIsoYearBoundary() {
        val recency = weeksSinceLastUse(
            listOf(entry(1, LocalDate.of(2025, 12, 28))),
            LocalDate.of(2026, 1, 5),
        )
        assertEquals(2L, recency[1])
    }

    @Test
    fun editingOrDeletingHistoryUpdatesRecency() {
        val old = entry(1, monday.minusWeeks(4))
        val recent = entry(2, monday.minusWeeks(1))
        assertEquals(1L, weeksSinceLastUse(listOf(old, recent), monday)[1])
        assertEquals(4L, weeksSinceLastUse(listOf(old), monday)[1])
        assertEquals(0L, weeksSinceLastUse(listOf(old, recent.copy(epochDay = monday.toEpochDay())), monday)[1])
    }
}
