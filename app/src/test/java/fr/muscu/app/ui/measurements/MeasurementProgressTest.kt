package fr.muscu.app.ui.measurements

import fr.muscu.app.data.Measurement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasurementProgressTest {
    private fun measurement(
        id: Long,
        day: Long,
        biceps: Double? = null,
        shoulders: Double? = null,
        left: Double? = null,
    ) = Measurement(id = id, epochDay = day, bicepsLeftCm = left, bicepsRightCm = biceps, shouldersCm = shoulders)

    @Test
    fun emptyHistoryHasNoPointsAndNoDeltas() {
        val progress = measureProgress(emptyList(), MeasureType.BICEPS_RIGHT)
        assertTrue(progress.points.isEmpty())
        assertNull(progress.latest)
        assertNull(progress.deltaFromPrevious)
        assertNull(progress.deltaFromFirst)
    }

    @Test
    fun pointsAreSortedByDateAndIgnoreMissingValues() {
        val all = listOf(
            measurement(1, day = 200, biceps = 38.5),
            measurement(2, day = 100, biceps = 37.0, shoulders = 110.0),
            measurement(3, day = 150, shoulders = 111.0),
        )
        assertEquals(
            listOf(MeasurePoint(100, 37.0), MeasurePoint(200, 38.5)),
            measurePoints(all, MeasureType.BICEPS_RIGHT),
        )
        assertEquals(
            listOf(MeasurePoint(100, 110.0), MeasurePoint(150, 111.0)),
            measurePoints(all, MeasureType.SHOULDERS),
        )
    }

    @Test
    fun leftAndRightBicepsAreTrackedSeparately() {
        val all = listOf(
            measurement(1, day = 100, biceps = 38.0, left = 37.0),
            measurement(2, day = 110, left = 37.5),
            measurement(3, day = 120, biceps = 39.0),
        )
        assertEquals(
            listOf(MeasurePoint(100, 37.0), MeasurePoint(110, 37.5)),
            measurePoints(all, MeasureType.BICEPS_LEFT),
        )
        assertEquals(
            listOf(MeasurePoint(100, 38.0), MeasurePoint(120, 39.0)),
            measurePoints(all, MeasureType.BICEPS_RIGHT),
        )
    }

    @Test
    fun lastEntryOfTheDayWins() {
        val all = listOf(
            measurement(1, day = 100, biceps = 37.0),
            measurement(3, day = 100, biceps = 38.0),
            measurement(2, day = 100, biceps = 36.0),
            measurement(4, day = 101, biceps = 39.0),
        )
        assertEquals(
            listOf(MeasurePoint(100, 38.0), MeasurePoint(101, 39.0)),
            measurePoints(all, MeasureType.BICEPS_RIGHT),
        )
    }

    @Test
    fun aSingleMeasureHasNoDelta() {
        val progress = measureProgress(listOf(measurement(1, 100, biceps = 38.0)), MeasureType.BICEPS_RIGHT)
        assertEquals(38.0, requireNotNull(progress.latest).cm, 0.0)
        assertNull(progress.deltaFromPrevious)
        assertNull(progress.deltaFromFirst)
    }

    @Test
    fun deltasCompareWithThePreviousAndTheFirstMeasure() {
        val all = listOf(
            measurement(1, 100, biceps = 37.0),
            measurement(2, 110, biceps = 38.5),
            measurement(3, 120, biceps = 38.0),
        )
        val progress = measureProgress(all, MeasureType.BICEPS_RIGHT)
        assertEquals(-0.5, requireNotNull(progress.deltaFromPrevious), 1e-9)
        assertEquals(1.0, requireNotNull(progress.deltaFromFirst), 1e-9)
    }

    @Test
    fun withTwoMeasuresBothDeltasAreIdentical() {
        val all = listOf(measurement(1, 100, biceps = 37.0), measurement(2, 110, biceps = 38.0))
        val progress = measureProgress(all, MeasureType.BICEPS_RIGHT)
        assertEquals(1.0, requireNotNull(progress.deltaFromPrevious), 1e-9)
        assertEquals(1.0, requireNotNull(progress.deltaFromFirst), 1e-9)
    }

    @Test
    fun deltaIsRoundedToAvoidFloatingPointNoise() {
        val all = listOf(measurement(1, 100, biceps = 38.5), measurement(2, 110, biceps = 38.6))
        assertEquals(0.1, requireNotNull(measureProgress(all, MeasureType.BICEPS_RIGHT).deltaFromPrevious), 0.0)
    }

    @Test
    fun chartBoundsAddAMarginAndNeverCollapse() {
        assertEquals(37.0 to 41.0, chartBounds(listOf(MeasurePoint(1, 38.0), MeasurePoint(2, 40.0))))
        assertEquals(37.0 to 39.0, chartBounds(listOf(MeasurePoint(1, 38.0), MeasurePoint(2, 38.0))))
        assertEquals(98.0 to 112.0, chartBounds(listOf(MeasurePoint(1, 100.0), MeasurePoint(2, 110.0))))
    }

    @Test
    fun blankInputIsEmptyAndAcceptsCommaOrDot() {
        assertEquals(MeasureInput.Empty, parseMeasure("", MeasureType.BICEPS_RIGHT))
        assertEquals(MeasureInput.Empty, parseMeasure("   ", MeasureType.SHOULDERS))
        assertEquals(MeasureInput.Valid(38.5), parseMeasure("38,5", MeasureType.BICEPS_RIGHT))
        assertEquals(MeasureInput.Valid(38.5), parseMeasure("38.5", MeasureType.BICEPS_RIGHT))
        assertEquals(MeasureInput.Valid(38.0), parseMeasure("38,04", MeasureType.BICEPS_RIGHT))
    }

    @Test
    fun leftBicepsUsesTheSamePlausibleRangeAsTheRightOne() {
        assertEquals(MeasureInput.Valid(36.5), parseMeasure("36,5", MeasureType.BICEPS_LEFT))
        assertEquals(MeasureInput.Invalid, parseMeasure("5", MeasureType.BICEPS_LEFT))
        assertEquals(MeasureInput.Invalid, parseMeasure("101", MeasureType.BICEPS_LEFT))
        assertEquals(MeasureInput.Empty, parseMeasure("", MeasureType.BICEPS_LEFT))
    }

    @Test
    fun implausibleOrMalformedValuesAreInvalid() {
        assertEquals(MeasureInput.Invalid, parseMeasure("abc", MeasureType.BICEPS_RIGHT))
        assertEquals(MeasureInput.Invalid, parseMeasure("5", MeasureType.BICEPS_RIGHT))
        assertEquals(MeasureInput.Invalid, parseMeasure("385", MeasureType.BICEPS_RIGHT))
        assertEquals(MeasureInput.Invalid, parseMeasure("30", MeasureType.SHOULDERS))
        assertEquals(MeasureInput.Invalid, parseMeasure("300", MeasureType.SHOULDERS))
        assertEquals(MeasureInput.Valid(100.0), parseMeasure("100", MeasureType.BICEPS_RIGHT))
        assertEquals(MeasureInput.Valid(10.0), parseMeasure("10", MeasureType.BICEPS_RIGHT))
    }
}
