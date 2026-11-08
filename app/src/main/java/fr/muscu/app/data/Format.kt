package fr.muscu.app.data

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

val FR: Locale = Locale.FRANCE

private val weightFormat = DecimalFormat("0.##", DecimalFormatSymbols(FR))

fun formatWeight(kg: Double): String = weightFormat.format(kg)

private val cmFormat = DecimalFormat("0.#", DecimalFormatSymbols(FR))
private val cmDeltaFormat = DecimalFormat("+0.#;-0.#", DecimalFormatSymbols(FR))

/** Mensuration en centimètres, une décimale au plus (ex. « 38,5 »). */
fun formatCm(cm: Double): String = cmFormat.format(cm)

/** Variation signée en centimètres (« +0,5 », « -1 »), « 0 » si elle est négligeable. */
fun formatCmDelta(delta: Double): String =
    if (kotlin.math.abs(delta) < 0.05) "0" else cmDeltaFormat.format(delta)

fun formatPerf(sets: Int, reps: Int, kg: Double) = "$sets×$reps @ ${formatWeight(kg)} kg"

fun WorkoutEntry.perf() = formatPerf(sets, reps, weightKg)

fun parseWeight(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

fun relativeDay(epochDay: Long, today: LocalDate = LocalDate.now()): String {
    val diff = today.toEpochDay() - epochDay
    return when {
        diff == 0L -> "Aujourd’hui"
        diff == 1L -> "Hier"
        diff > 1L -> "Il y a $diff jours"
        diff == -1L -> "Demain"
        else -> "Le " + LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("d MMMM", FR))
    }
}

fun String.capitalizeFr() = replaceFirstChar { it.titlecase(FR) }
