package fr.muscu.app.ui.measurements

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.muscu.app.data.FR
import fr.muscu.app.data.Measurement
import fr.muscu.app.data.formatCm
import fr.muscu.app.data.formatCmDelta
import fr.muscu.app.data.relativeDay
import fr.muscu.app.ui.components.CircleIconButton
import fr.muscu.app.ui.components.ExtendedPrimaryFab
import fr.muscu.app.ui.components.PerfChip
import fr.muscu.app.ui.components.PrimaryButton
import fr.muscu.app.ui.components.SectionLabel
import fr.muscu.app.ui.muscuViewModel
import fr.muscu.app.ui.theme.MuscuColors
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private const val STEP_CM = 0.1

/** Largeur de la colonne des graduations (valeur + « cm »). */
private val CHART_AXIS_WIDTH = 56.dp

private val shortDate = DateTimeFormatter.ofPattern("d MMM yyyy", FR)
private val longDate = DateTimeFormatter.ofPattern("d MMMM yyyy", FR)

private fun dayLabel(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(shortDate)

/** `editing == null` : nouvelle prise de mensurations. */
private data class SheetRequest(val editing: Measurement?)

@Composable
fun MeasurementsScreen() {
    val vm = muscuViewModel { MeasurementsViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    var sheet by remember { mutableStateOf<SheetRequest?>(null) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column {
                    SectionLabel("Suivi")
                    Text("Mensurations", style = MaterialTheme.typography.headlineLarge)
                }
            }
            item { ProgressCard(state.bicepsLeft, MuscuColors.Primary) }
            item { ProgressCard(state.bicepsRight, MuscuColors.gradient(2).first) }
            item { ProgressCard(state.shoulders, MuscuColors.gradient(5).first) }
            item { SectionLabel("Historique", Modifier.padding(top = 12.dp, bottom = 4.dp)) }
            if (state.history.isEmpty()) {
                item {
                    Text(
                        "Aucune mesure enregistrée.\nAppuie sur « Ajouter » pour noter tes biceps (gauche et droit) et tes épaules.",
                        color = MuscuColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                    )
                }
            }
            items(state.history, key = { it.id }) { measurement ->
                HistoryCard(measurement) { sheet = SheetRequest(measurement) }
            }
        }

        ExtendedPrimaryFab(
            text = "Ajouter",
            icon = Icons.Default.Add,
            onClick = { sheet = SheetRequest(null) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        )
    }

    sheet?.let { request ->
        MeasurementSheet(
            editing = request.editing,
            latestBicepsLeft = state.bicepsLeft.latest?.cm,
            latestBicepsRight = state.bicepsRight.latest?.cm,
            latestShoulders = state.shoulders.latest?.cm,
            onDismiss = { sheet = null },
            onSave = { date, bicepsLeft, bicepsRight, shoulders ->
                vm.save(request.editing?.id ?: 0, date, bicepsLeft, bicepsRight, shoulders)
            },
            onDelete = request.editing?.let { m -> { vm.delete(m.id) } },
        )
    }
}

// --- Cartes de synthèse et graphique ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProgressCard(progress: MeasureProgress, color: Color) {
    val latest = progress.latest
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MuscuColors.Surface)
            .border(1.dp, MuscuColors.Border, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(progress.type.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (latest != null) {
                Text(
                    relativeDay(latest.epochDay),
                    color = MuscuColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (latest == null) {
            Text("Aucune mesure pour l’instant", color = MuscuColors.TextSecondary)
            return@Column
        }

        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(formatCm(latest.cm), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = color)
            Text("cm", color = MuscuColors.TextSecondary, modifier = Modifier.padding(bottom = 7.dp))
        }

        val firstDay = progress.points.first().epochDay
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            progress.deltaFromPrevious?.let { delta ->
                // Avec deux mesures seulement, « depuis la précédente » et « depuis le début » sont identiques.
                val label = if (progress.points.size == 2) "depuis le ${dayLabel(firstDay)}" else "depuis la dernière mesure"
                DeltaChip(delta, label)
            }
            if (progress.points.size >= 3) {
                progress.deltaFromFirst?.let { delta -> DeltaChip(delta, "depuis le ${dayLabel(firstDay)}") }
            }
        }

        if (progress.points.size >= 2) {
            MeasureChart(
                points = progress.points,
                color = color,
                description = "Évolution de ${formatCm(progress.points.first().cm)} cm à " +
                    "${formatCm(latest.cm)} cm entre le ${dayLabel(firstDay)} et le ${dayLabel(latest.epochDay)}",
            )
        } else {
            Text(
                "Ajoute une deuxième mesure pour voir la courbe d’évolution.",
                color = MuscuColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun DeltaChip(delta: Double, label: String) {
    val (background, content) = when {
        abs(delta) < 0.05 -> MuscuColors.Border to MuscuColors.TextSecondary
        delta > 0 -> MuscuColors.PrimaryLight to MuscuColors.Primary
        else -> MuscuColors.StreakLight to MuscuColors.StreakText
    }
    Text(
        "${formatCmDelta(delta)} cm $label",
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        color = content,
        fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.bodyMedium,
    )
}

/** Courbe simple : l'axe horizontal est proportionnel au temps réel entre les mesures. */
@Composable
private fun MeasureChart(points: List<MeasurePoint>, color: Color, description: String) {
    val (low, high) = remember(points) { chartBounds(points) }
    val firstDay = points.first().epochDay
    val lastDay = points.last().epochDay
    val gridColor = MuscuColors.Border

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(140.dp)) {
            Column(
                Modifier.width(CHART_AXIS_WIDTH).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("${formatCm(high)} cm", color = MuscuColors.TextSecondary, fontSize = 12.sp)
                Text("${formatCm(low)} cm", color = MuscuColors.TextSecondary, fontSize = 12.sp)
            }
            Canvas(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clearAndSetSemantics { contentDescription = description },
            ) {
                val w = size.width
                val h = size.height
                val padX = 8.dp.toPx()
                val span = (lastDay - firstDay).coerceAtLeast(1L).toFloat()
                val xOf = { day: Long -> padX + (w - 2 * padX) * (day - firstDay).toFloat() / span }
                val yOf = { cm: Double -> h * (1f - ((cm - low) / (high - low)).toFloat()) }

                listOf(0f, h / 2, h).forEach { y ->
                    drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
                }

                val line = Path()
                val area = Path()
                points.forEachIndexed { i, p ->
                    val x = xOf(p.epochDay)
                    val y = yOf(p.cm)
                    if (i == 0) {
                        line.moveTo(x, y)
                        area.moveTo(x, h)
                        area.lineTo(x, y)
                    } else {
                        line.lineTo(x, y)
                        area.lineTo(x, y)
                    }
                }
                area.lineTo(xOf(lastDay), h)
                area.close()

                drawPath(area, color.copy(alpha = 0.12f))
                drawPath(
                    line,
                    color,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
                points.forEachIndexed { i, p ->
                    val isLast = i == points.lastIndex
                    if (!isLast && points.size > 40) return@forEachIndexed
                    val center = Offset(xOf(p.epochDay), yOf(p.cm))
                    if (isLast) drawCircle(Color.White, 7.dp.toPx(), center)
                    drawCircle(color, if (isLast) 5.dp.toPx() else 3.dp.toPx(), center)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(start = CHART_AXIS_WIDTH, top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(dayLabel(firstDay), color = MuscuColors.TextSecondary, fontSize = 12.sp)
            Text(dayLabel(lastDay), color = MuscuColors.TextSecondary, fontSize = 12.sp)
        }
    }
}

// --- Historique ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryCard(measurement: Measurement, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MuscuColors.Surface)
            .border(1.dp, MuscuColors.Border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            LocalDate.ofEpochDay(measurement.epochDay).format(longDate),
            style = MaterialTheme.typography.titleMedium,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            measurement.bicepsLeftCm?.let { PerfChip("Biceps gauche ${formatCm(it)} cm") }
            measurement.bicepsRightCm?.let { PerfChip("Biceps droit ${formatCm(it)} cm") }
            measurement.shouldersCm?.let { PerfChip("Épaules ${formatCm(it)} cm") }
        }
    }
}

// --- Saisie ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeasurementSheet(
    editing: Measurement?,
    latestBicepsLeft: Double?,
    latestBicepsRight: Double?,
    latestShoulders: Double?,
    onDismiss: () -> Unit,
    onSave: (date: LocalDate, bicepsLeft: Double?, bicepsRight: Double?, shoulders: Double?) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun closeThen(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            action()
            onDismiss()
        }
    }

    var date by remember { mutableStateOf(editing?.let { LocalDate.ofEpochDay(it.epochDay) } ?: LocalDate.now()) }
    var bicepsLeftText by remember { mutableStateOf(editing?.bicepsLeftCm?.let { formatCm(it) } ?: "") }
    var bicepsRightText by remember { mutableStateOf(editing?.bicepsRightCm?.let { formatCm(it) } ?: "") }
    var shouldersText by remember { mutableStateOf(editing?.shouldersCm?.let { formatCm(it) } ?: "") }
    var showPicker by remember { mutableStateOf(false) }

    val bicepsLeft = parseMeasure(bicepsLeftText, MeasureType.BICEPS_LEFT)
    val bicepsRight = parseMeasure(bicepsRightText, MeasureType.BICEPS_RIGHT)
    val shoulders = parseMeasure(shouldersText, MeasureType.SHOULDERS)
    val inputs = listOf(bicepsLeft, bicepsRight, shoulders)
    val canSave = inputs.none { it is MeasureInput.Invalid } && inputs.any { it is MeasureInput.Valid }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MuscuColors.Background,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MuscuColors.Border) },
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (editing == null) "Nouvelle mesure" else "Modifier la mesure",
                style = MaterialTheme.typography.titleLarge,
            )

            DateRow(date = date, onChange = { date = it }, onPick = { showPicker = true })

            MeasureField(MeasureType.BICEPS_LEFT, bicepsLeftText, { bicepsLeftText = it }, latestBicepsLeft)
            MeasureField(MeasureType.BICEPS_RIGHT, bicepsRightText, { bicepsRightText = it }, latestBicepsRight)
            MeasureField(MeasureType.SHOULDERS, shouldersText, { shouldersText = it }, latestShoulders)

            PrimaryButton(
                text = "Enregistrer",
                enabled = canSave,
                onClick = {
                    closeThen {
                        onSave(
                            date,
                            (bicepsLeft as? MeasureInput.Valid)?.cm,
                            (bicepsRight as? MeasureInput.Valid)?.cm,
                            (shoulders as? MeasureInput.Valid)?.cm,
                        )
                    }
                },
            )

            if (onDelete != null) {
                TextButton(
                    onClick = { closeThen(onDelete) },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text("Supprimer cette mesure", color = MuscuColors.Danger) }
            }
        }
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        // Une mensuration ne peut pas être datée dans le futur.
                        date = minOf(picked, LocalDate.now())
                    }
                    showPicker = false
                }) { Text("OK", color = MuscuColors.Primary) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Annuler", color = MuscuColors.Primary) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun DateRow(date: LocalDate, onChange: (LocalDate) -> Unit, onPick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MuscuColors.Surface)
            .border(1.dp, MuscuColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Date", color = MuscuColors.TextSecondary, modifier = Modifier.weight(1f))
        CircleIconButton(Icons.Default.ChevronLeft, "Jour précédent", { onChange(date.minusDays(1)) }, size = 42.dp)
        Text(
            date.format(shortDate),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .width(130.dp)
                .clickable(onClick = onPick)
                .padding(vertical = 8.dp),
        )
        CircleIconButton(
            Icons.Default.ChevronRight, "Jour suivant", { onChange(date.plusDays(1)) },
            size = 42.dp,
            enabled = date < LocalDate.now(),
        )
    }
}

@Composable
private fun MeasureField(type: MeasureType, text: String, onTextChange: (String) -> Unit, last: Double?) {
    val input = parseMeasure(text, type)

    // Champ vide : le premier appui reprend la dernière mesure connue, les suivants varient de 0,1 cm.
    fun step(delta: Double) {
        val current = (input as? MeasureInput.Valid)?.cm
        val next = if (current == null) last ?: type.startValue else current + delta
        onTextChange(formatCm(round1(next.coerceIn(type.min, type.max))))
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MuscuColors.Surface)
            .border(1.dp, MuscuColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(type.label, color = MuscuColors.TextSecondary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(Icons.Default.Remove, "Moins", { step(-STEP_CM) }, size = 42.dp)
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(88.dp), contentAlignment = Alignment.Center) {
                    if (text.isEmpty()) {
                        Text("—", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = MuscuColors.TextSecondary)
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = { new ->
                            if (new.length <= 5 && new.all { it.isDigit() || it == ',' || it == '.' }) onTextChange(new)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            color = if (input is MeasureInput.Invalid) MuscuColors.Danger else MuscuColors.Text,
                        ),
                        cursorBrush = SolidColor(MuscuColors.Primary),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text(
                    "cm",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MuscuColors.TextSecondary,
                )
            }
            CircleIconButton(Icons.Default.Add, "Plus", { step(STEP_CM) }, size = 42.dp, repeatOnHold = true)
        }
        Text(
            when {
                input is MeasureInput.Invalid ->
                    "Valeur attendue entre ${type.min.toInt()} et ${type.max.toInt()} cm"
                last != null -> "Dernière mesure : ${formatCm(last)} cm"
                else -> "Facultatif"
            },
            color = if (input is MeasureInput.Invalid) MuscuColors.Danger else MuscuColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
