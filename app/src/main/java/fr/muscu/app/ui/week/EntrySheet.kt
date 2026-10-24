package fr.muscu.app.ui.week

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.muscu.app.data.Exercise
import fr.muscu.app.data.FR
import fr.muscu.app.data.WorkoutEntry
import fr.muscu.app.data.capitalizeFr
import fr.muscu.app.data.formatPerf
import fr.muscu.app.data.formatWeight
import fr.muscu.app.data.parseWeight
import fr.muscu.app.data.perf
import fr.muscu.app.ui.components.CircleIconButton
import fr.muscu.app.ui.components.ExerciseAvatar
import fr.muscu.app.ui.components.ExercisePhases
import fr.muscu.app.ui.components.PrimaryButton
import fr.muscu.app.ui.theme.MuscuColors
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.roundToLong

private const val WEIGHT_STEP = 0.25

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrySheet(
    date: LocalDate,
    exercises: List<Exercise>,
    entryHistory: List<WorkoutEntry>,
    editing: WorkoutEntry?,
    initialExerciseId: Long?,
    loadLatest: suspend (exerciseId: Long, excludeEntryId: Long) -> WorkoutEntry?,
    onDismiss: () -> Unit,
    onSave: (exerciseId: Long, sets: Int, reps: Int, weight: Double) -> Unit,
    onDelete: (() -> Unit)?,
    onCreateExercise: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun closeThen(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            action()
            onDismiss()
        }
    }

    var exerciseId by remember {
        mutableStateOf(editing?.exerciseId ?: initialExerciseId ?: exercises.firstOrNull()?.id)
    }
    var sets by remember { mutableIntStateOf(editing?.sets ?: 3) }
    var reps by remember { mutableIntStateOf(editing?.reps ?: 10) }
    var weightText by remember { mutableStateOf(editing?.let { formatWeight(it.weightKg) } ?: "0") }
    var last by remember { mutableStateOf<WorkoutEntry?>(null) }
    var firstLoad by remember { mutableStateOf(true) }

    LaunchedEffect(exercises) {
        if (exerciseId == null || exercises.none { it.id == exerciseId }) {
            exerciseId = exercises.firstOrNull()?.id
        }
    }

    // Pré-remplissage avec la dernière valeur connue pour l'exercice choisi.
    LaunchedEffect(exerciseId) {
        val id = exerciseId ?: return@LaunchedEffect
        val latest = loadLatest(id, editing?.id ?: -1L)
        last = latest
        val keepEditedValues = firstLoad && editing != null && editing.exerciseId == id
        firstLoad = false
        if (!keepEditedValues) {
            sets = latest?.sets ?: 3
            reps = latest?.reps ?: 10
            weightText = latest?.let { formatWeight(it.weightKg) } ?: "0"
        }
    }

    val exercise = exercises.firstOrNull { it.id == exerciseId }
    val weight = parseWeight(weightText)
    val recencyByExercise = remember(entryHistory, date, editing?.id) {
        weeksSinceLastUse(entryHistory, date, editing?.id)
    }

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
            val dayLabel = date.format(DateTimeFormatter.ofPattern("EEEE d", FR)).capitalizeFr()
            Text(
                "$dayLabel · ${if (editing == null) "Nouvelle entrée" else "Modifier l’entrée"}",
                style = MaterialTheme.typography.titleLarge,
            )

            if (exercise == null) {
                EmptyLibraryCard(onCreateExercise)
                return@Column
            }

            ExercisePickerCard(
                exercise = exercise,
                exercises = exercises,
                recencyByExercise = recencyByExercise,
                onSelect = { exerciseId = it.id },
                onCreateExercise = onCreateExercise,
            )

            if (exercise.imagePath != null || exercise.endImagePath != null) {
                ExercisePhases(
                    startImagePath = exercise.imagePath,
                    endImagePath = exercise.endImagePath,
                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MuscuColors.PrimaryLight)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.History, null, tint = MuscuColors.Primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    last?.let { "Dernière fois : ${it.perf()} (préremplie)" }
                        ?: "Première fois pour cet exercice",
                    color = MuscuColors.Primary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            StepperRow(
                label = "Séries",
                onMinus = { sets = max(1, sets - 1) },
                onPlus = { sets = minOf(99, sets + 1) },
            ) { ValueText(sets.toString()) }

            StepperRow(
                label = "Répétitions",
                onMinus = { reps = max(1, reps - 1) },
                onPlus = { reps = minOf(999, reps + 1) },
            ) { ValueText(reps.toString()) }

            StepperRow(
                label = "Poids (kg)",
                onMinus = { weightText = formatWeight(roundKg(max(0.0, (weight ?: 0.0) - WEIGHT_STEP))) },
                onPlus = { weightText = formatWeight(roundKg((weight ?: 0.0) + WEIGHT_STEP)) },
                repeatPlusOnHold = true,
            ) {
                BasicTextField(
                    value = weightText,
                    onValueChange = { new ->
                        if (new.length <= 6 && new.all { it.isDigit() || it == ',' || it == '.' }) weightText = new
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = TextStyle(
                        fontSize = 24.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                        color = if (weight == null) MuscuColors.Danger else MuscuColors.Text,
                    ),
                    cursorBrush = SolidColor(MuscuColors.Primary),
                    modifier = Modifier.width(90.dp),
                )
            }

            Text(
                buildAnnotatedString {
                    append("Aperçu : ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MuscuColors.Text)) {
                        append(if (weight != null) formatPerf(sets, reps, weight) else "poids invalide")
                    }
                },
                color = MuscuColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            PrimaryButton(
                text = "Enregistrer",
                enabled = weight != null,
                onClick = {
                    val id = exerciseId ?: return@PrimaryButton
                    closeThen { onSave(id, sets, reps, weight ?: 0.0) }
                },
            )

            if (onDelete != null) {
                TextButton(
                    onClick = { closeThen(onDelete) },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text("Supprimer cette entrée", color = MuscuColors.Danger) }
            }
        }
    }
}

private fun roundKg(value: Double) = (value * 100).roundToLong() / 100.0

@Composable
private fun ValueText(text: String) {
    Text(
        text, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
        modifier = Modifier.width(90.dp),
    )
}

@Composable
private fun StepperRow(
    label: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    repeatPlusOnHold: Boolean = false,
    value: @Composable () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MuscuColors.Surface)
            .border(1.dp, MuscuColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MuscuColors.TextSecondary, modifier = Modifier.weight(1f))
        CircleIconButton(Icons.Default.Remove, "Moins", onMinus, size = 42.dp)
        value()
        CircleIconButton(Icons.Default.Add, "Plus", onPlus, size = 42.dp, repeatOnHold = repeatPlusOnHold)
    }
}

@Composable
private fun ExercisePickerCard(
    exercise: Exercise,
    exercises: List<Exercise>,
    recencyByExercise: Map<Long, Long>,
    onSelect: (Exercise) -> Unit,
    onCreateExercise: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MuscuColors.Surface)
            .border(1.dp, MuscuColors.Border, RoundedCornerShape(20.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExerciseAvatar(exercise.colorIndex, exercise.endImagePath ?: exercise.imagePath, 58.dp, corner = 14.dp)
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(exercise.name, style = MaterialTheme.typography.titleLarge)
            ExerciseRecency(recencyByExercise[exercise.id])
            Spacer(Modifier.height(8.dp))
            Box {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MuscuColors.PrimaryLight)
                        .clickable { expanded = true }
                        .padding(start = 12.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Changer", color = MuscuColors.Primary, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Default.ArrowDropDown, null, tint = MuscuColors.Primary)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    containerColor = MuscuColors.Surface,
                ) {
                    exercises.forEach { ex ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        ex.name,
                                        fontWeight = if (ex.id == exercise.id) FontWeight.Bold else FontWeight.Normal,
                                    )
                                    ExerciseRecency(recencyByExercise[ex.id])
                                }
                            },
                            leadingIcon = { ExerciseAvatar(ex.colorIndex, ex.imagePath ?: ex.endImagePath, 32.dp) },
                            onClick = {
                                expanded = false
                                onSelect(ex)
                            },
                        )
                    }
                    HorizontalDivider(color = MuscuColors.Border)
                    DropdownMenuItem(
                        text = { Text("Nouvel exercice", color = MuscuColors.Primary) },
                        leadingIcon = { Icon(Icons.Default.Add, null, tint = MuscuColors.Primary) },
                        onClick = {
                            expanded = false
                            onCreateExercise()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseRecency(weeks: Long?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            Icons.Default.History,
            contentDescription = null,
            tint = MuscuColors.TextSecondary,
            modifier = Modifier.size(16.dp),
        )
        Text(
            exerciseRecencyLabel(weeks),
            color = MuscuColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun EmptyLibraryCard(onCreateExercise: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MuscuColors.Surface)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Ta bibliothèque d’exercices est vide.", color = MuscuColors.TextSecondary)
        PrimaryButton("Créer un exercice", onCreateExercise)
    }
}
