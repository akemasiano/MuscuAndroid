package fr.muscu.app.ui.measurements

import fr.muscu.app.data.Measurement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasurementProgressTest {
    private fun measurement(id: Long, day: Long, biceps: Double? = null, shoulders: Double? = null) =
        Measurement(id = id, epochDay = day, bicepsCm = biceps, shouldersCm = shoulders)

    @Test
    fun emptyHistoryHasNoPointsAndNoDeltas() {
        val progress = measureProgress(emptyList(), MeasureType.BICEPS)
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
            measurePoints(all, MeasureType.BICEPS),
        )
        assertEquals(
            listOf(MeasurePoint(100, 110.0), MeasurePoint(150, 111.0)),
            measurePoints(all, MeasureType.SHOULDERS),
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
            measurePoints(all, MeasureType.BICEPS),
        )
    }

    @Test
    fun aSingleMeasureHasNoDelta() {
        val progress = measureProgress(listOf(measurement(1, 100, biceps = 38.0)), MeasureType.BICEPS)
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
        val progress = measureProgress(all, MeasureType.BICEPS)
        assertEquals(-0.5, requireNotNull(progress.deltaFromPrevious), 1e-9)
        assertEquals(1.0, requireNotNull(progress.deltaFromFirst), 1e-9)
    }

    @Test
    fun withTwoMeasuresBothDeltasAreIdentical() {
        val all = listOf(measurement(1, 100, biceps = 37.0), measurement(2, 110, biceps = 38.0))
        val progress = measureProgress(all, MeasureType.BICEPS)
        assertEquals(1.0, requireNotNull(progress.deltaFromPrevious), 1e-9)
        assertEquals(1.0, requireNotNull(progress.deltaFromFirst), 1e-9)
    }

    @Test
    fun deltaIsRoundedToAvoidFloatingPointNoise() {
        val all = listOf(measurement(1, 100, biceps = 38.5), measurement(2, 110, biceps = 38.6))
        assertEquals(0.1, requireNotNull(measureProgress(all, MeasureType.BICEPS).deltaFromPrevious), 0.0)
    }

    @Test
    fun chartBoundsAddAMarginAndNeverCollapse() {
        assertEquals(37.0 to 41.0, chartBounds(listOf(MeasurePoint(1, 38.0), MeasurePoint(2, 40.0))))
        assertEquals(37.0 to 39.0, chartBounds(listOf(MeasurePoint(1, 38.0), MeasurePoint(2, 38.0))))
        assertEquals(98.0 to 112.0, chartBounds(listOf(MeasurePoint(1, 100.0), MeasurePoint(2, 110.0))))
    }

    @Test
    fun blankInputIsEmptyAndAcceptsCommaOrDot() {
        assertEquals(MeasureInput.Empty, parseMeasure("", MeasureType.BICEPS))
        assertEquals(MeasureInput.Empty, parseMeasure("   ", MeasureType.SHOULDERS))
        assertEquals(MeasureInput.Valid(38.5), parseMeasure("38,5", MeasureType.BICEPS))
        assertEquals(MeasureInput.Valid(38.5), parseMeasure("38.5", MeasureType.BICEPS))
        assertEquals(MeasureInput.Valid(38.0), parseMeasure("38,04", MeasureType.BICEPS))
    }

    @Test
    fun implausibleOrMalformedValuesAreInvalid() {
        assertEquals(MeasureInput.Invalid, parseMeasure("abc", MeasureType.BICEPS))
        assertEquals(MeasureInput.Invalid, parseMeasure("5", MeasureType.BICEPS))
        assertEquals(MeasureInput.Invalid, parseMeasure("385", MeasureType.BICEPS))
        assertEquals(MeasureInput.Invalid, parseMeasure("30", MeasureType.SHOULDERS))
        assertEquals(MeasureInput.Invalid, parseMeasure("300", MeasureType.SHOULDERS))
        assertEquals(MeasureInput.Valid(100.0), parseMeasure("100", MeasureType.BICEPS))
        assertEquals(MeasureInput.Valid(10.0), parseMeasure("10", MeasureType.BICEPS))
    }
}
