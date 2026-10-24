package fr.muscu.app.ui.week

import fr.muscu.app.data.Exercise
import fr.muscu.app.data.WorkoutEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WeekEntriesTest {
    private val monday = LocalDate.of(2026, 10, 19)
    private val exercises = listOf(
        Exercise(id = 1, name = "Squat", colorIndex = 0),
        Exercise(id = 2, name = "Curl", colorIndex = 1),
    )

    private fun entry(id: Long, day: Long, weight: Double, exerciseId: Long = 1) =
        WorkoutEntry(
            id = id,
            exerciseId = exerciseId,
            epochDay = monday.plusDays(day).toEpochDay(),
            sets = 3,
            reps = 10,
            weightKg = weight,
        )

    private fun week(entries: List<WorkoutEntry>) = weekEntries(entries, exercises, monday)

    @Test
    fun emptyHistoryHasNoEntries() {
        assertTrue(week(emptyList()).isEmpty())
    }

    @Test
    fun firstEntryDoesNotCountAsAnIncrease() {
        val item = week(listOf(entry(1, 0, 20.0))).single()
        assertEquals(null, item.previous)
        assertFalse(item.hasWeightIncrease)
    }

    @Test
    fun comparesWithPreviousWeekAndDetectsSmallestWeightStep() {
        val previous = entry(1, -7, 20.0)
        val item = week(listOf(previous, entry(2, 0, 20.25))).single()
        assertEquals(previous, item.previous)
        assertTrue(item.hasWeightIncrease)
    }

    @Test
    fun equalOrLowerWeightDoesNotCountAsAnIncrease() {
        val items = week(listOf(entry(1, -1, 20.0), entry(2, 0, 20.0), entry(3, 1, 19.75)))
        assertTrue(items.none { it.hasWeightIncrease })
    }

    @Test
    fun comparesWithImmediatePreviousEntryNotPersonalRecord() {
        val items = week(listOf(entry(1, -2, 30.0), entry(2, -1, 20.0), entry(3, 0, 25.0)))
        assertTrue(items.single().hasWeightIncrease)
    }

    @Test
    fun eachExerciseHasItsOwnHistory() {
        val items = week(listOf(
            entry(1, -1, 20.0),
            entry(2, -1, 50.0, exerciseId = 2),
            entry(3, 0, 20.25),
            entry(4, 0, 45.0, exerciseId = 2),
        ))
        assertEquals(listOf(true, false), items.map { it.hasWeightIncrease })
    }

    @Test
    fun sameDayEntriesFollowIdOrderEvenWithUnsortedInput() {
        val entries = listOf(entry(1, 0, 20.0), entry(2, 0, 25.0), entry(3, 0, 25.0))
        val items = week(entries.reversed())
        assertEquals(listOf(1L, 2L, 3L), items.map { it.entry.id })
        assertEquals(listOf(false, true, false), items.map { it.hasWeightIncrease })
    }

    @Test
    fun weekIncludesMondayAndSundayButNotAdjacentWeeks() {
        val items = week(listOf(
            entry(1, -1, 10.0),
            entry(2, 0, 20.0),
            entry(3, 6, 30.0),
            entry(4, 7, 40.0),
        ).reversed())
        assertEquals(listOf(2L, 3L), items.map { it.entry.id })
        assertTrue(items.all { it.hasWeightIncrease })
    }

    @Test
    fun onlyDaysContainingAnIncreaseAreMarked() {
        val items = week(listOf(
            entry(1, -1, 20.0),
            entry(2, 0, 25.0),
            entry(3, 0, 30.0),
            entry(4, 1, 30.0),
            entry(5, 2, 35.0),
        ))
        val days = items.filter { it.hasWeightIncrease }
            .map { LocalDate.ofEpochDay(it.entry.epochDay) }.toSet()
        assertEquals(setOf(monday, monday.plusDays(2)), days)
    }

    @Test
    fun editingOrDeletingHistoryRecalculatesIncrease() {
        val previous = entry(1, -1, 20.0)
        val current = entry(2, 0, 25.0)
        assertTrue(week(listOf(previous, current)).single().hasWeightIncrease)
        assertFalse(week(listOf(previous, current.copy(weightKg = 20.0))).single().hasWeightIncrease)
        assertFalse(week(listOf(previous.copy(weightKg = 30.0), current)).single().hasWeightIncrease)
        assertFalse(week(listOf(current)).single().hasWeightIncrease)
    }

    @Test
    fun removedExercisesAreNotDisplayed() {
        assertTrue(week(listOf(entry(1, 0, 20.0, exerciseId = 3))).isEmpty())
    }
}
