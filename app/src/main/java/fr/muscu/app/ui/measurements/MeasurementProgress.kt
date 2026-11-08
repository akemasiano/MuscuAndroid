package fr.muscu.app.ui.measurements

import fr.muscu.app.data.Measurement
import fr.muscu.app.data.parseWeight
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToLong

/** Les deux mensurations suivies, avec la plage de valeurs jugée plausible pour détecter les fautes de frappe. */
enum class MeasureType(
    val label: String,
    val min: Double,
    val max: Double,
    /** Valeur proposée au premier appui sur « + » quand aucune mesure n'existe encore. */
    val startValue: Double,
) {
    BICEPS("Tour de biceps", min = 10.0, max = 100.0, startValue = 35.0),
    SHOULDERS("Tour d’épaules", min = 50.0, max = 250.0, startValue = 110.0);

    fun valueOf(measurement: Measurement): Double? = when (this) {
        BICEPS -> measurement.bicepsCm
        SHOULDERS -> measurement.shouldersCm
    }
}

data class MeasurePoint(val epochDay: Long, val cm: Double)

data class MeasureProgress(
    val type: MeasureType,
    /** Une valeur par jour, du plus ancien au plus récent. */
    val points: List<MeasurePoint>,
) {
    val first: MeasurePoint? get() = points.firstOrNull()
    val latest: MeasurePoint? get() = points.lastOrNull()

    /** Variation entre la dernière mesure et la précédente (null s'il n'y a qu'une mesure). */
    val deltaFromPrevious: Double?
        get() = if (points.size >= 2) round1(points.last().cm - points[points.size - 2].cm) else null

    /** Variation entre la dernière mesure et la toute première (null s'il n'y a qu'une mesure). */
    val deltaFromFirst: Double?
        get() = if (points.size >= 2) round1(points.last().cm - points.first().cm) else null
}

internal fun round1(value: Double): Double = (value * 10).roundToLong() / 10.0

/**
 * Série chronologique d'une mensuration. Les mesures sans valeur pour ce type sont ignorées ;
 * si plusieurs saisies tombent le même jour, la dernière créée l'emporte.
 */
internal fun measurePoints(measurements: List<Measurement>, type: MeasureType): List<MeasurePoint> {
    val byDay = LinkedHashMap<Long, Double>()
    measurements
        .sortedWith(compareBy<Measurement> { it.epochDay }.thenBy { it.id })
        .forEach { m -> type.valueOf(m)?.let { byDay[m.epochDay] = it } }
    return byDay.map { (day, cm) -> MeasurePoint(day, cm) }
}

internal fun measureProgress(measurements: List<Measurement>, type: MeasureType) =
    MeasureProgress(type, measurePoints(measurements, type))

/** Bornes verticales du graphique : les données plus une marge, arrondies à l'entier. Toujours min < max. */
internal fun chartBounds(points: List<MeasurePoint>): Pair<Double, Double> {
    if (points.isEmpty()) return 0.0 to 1.0
    val min = points.minOf { it.cm }
    val max = points.maxOf { it.cm }
    val margin = maxOf(0.5, (max - min) * 0.15)
    return floor(min - margin) to ceil(max + margin)
}

internal sealed interface MeasureInput {
    /** Champ laissé vide : cette mesure n'est pas enregistrée. */
    data object Empty : MeasureInput
    data class Valid(val cm: Double) : MeasureInput
    data object Invalid : MeasureInput
}

internal fun parseMeasure(text: String, type: MeasureType): MeasureInput {
    if (text.isBlank()) return MeasureInput.Empty
    val value = parseWeight(text) ?: return MeasureInput.Invalid
    return if (value in type.min..type.max) MeasureInput.Valid(round1(value)) else MeasureInput.Invalid
}
