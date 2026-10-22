package fr.muscu.app.ui.week

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.muscu.app.data.FR
import fr.muscu.app.data.WorkoutEntry
import fr.muscu.app.data.perf
import fr.muscu.app.ui.components.CircleIconButton
import fr.muscu.app.ui.components.ExerciseAvatar
import fr.muscu.app.ui.components.ExtendedPrimaryFab
import fr.muscu.app.ui.components.PerfChip
import fr.muscu.app.ui.components.SectionLabel
import fr.muscu.app.ui.muscuViewModel
import fr.muscu.app.ui.theme.MuscuColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields

private data class SheetRequest(val date: LocalDate, val editing: WorkoutEntry?)

@Composable
fun WeekScreen(onCreateExercise: () -> Unit) {
    val vm = muscuViewModel { WeekViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    var sheet by remember { mutableStateOf<SheetRequest?>(null) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                WeekHeader(
                    weekStart = state.weekStart,
                    isCurrentWeek = state.weekStart == LocalDate.now()
                        .with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                    onPrevious = vm::previousWeek,
                    onNext = vm::nextWeek,
                    onToday = vm::goToToday,
                )
            }
            item {
                DayStrip(
                    weekStart = state.weekStart,
                    selected = state.selectedDate,
                    daysWithEntries = state.daysWithEntries,
                    onSelect = vm::selectDay,
                )
            }
            item {
                SectionLabel(
                    state.selectedDate.format(DateTimeFormatter.ofPattern("EEEE d MMMM", FR)),
                    Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
            }
            if (state.dayEntries.isEmpty()) {
                item {
                    Text(
                        "Aucun exercice ce jour.\nAppuie sur « Ajouter » pour enregistrer ta séance.",
                        color = MuscuColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                    )
                }
            }
            items(state.dayEntries, key = { it.entry.id }) { item ->
                EntryCard(item) { sheet = SheetRequest(state.selectedDate, item.entry) }
            }
        }

        ExtendedPrimaryFab(
            text = "Ajouter",
            icon = Icons.Default.Add,
            onClick = { sheet = SheetRequest(state.selectedDate, null) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        )
    }

    sheet?.let { request ->
        val doneToday = state.dayEntries.map { it.exercise.id }.toSet()
        EntrySheet(
            date = request.date,
            exercises = state.exercises,
            editing = request.editing,
            initialExerciseId = state.exercises.firstOrNull { it.id !in doneToday }?.id,
            loadLatest = vm::latestFor,
            onDismiss = { sheet = null },
            onSave = { exerciseId, sets, reps, weight ->
                vm.saveEntry(request.editing?.id ?: 0, exerciseId, request.date, sets, reps, weight)
            },
            onDelete = request.editing?.let { e -> { vm.deleteEntry(e.id) } },
            onCreateExercise = {
                sheet = null
                onCreateExercise()
            },
        )
    }
}

@Composable
private fun WeekHeader(
    weekStart: LocalDate,
    isCurrentWeek: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Semaine ${weekStart.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}")
                if (!isCurrentWeek) {
                    TextButton(onClick = onToday, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text("Aujourd’hui", color = MuscuColors.Primary)
                    }
                }
            }
            Text(weekTitle(weekStart), style = MaterialTheme.typography.headlineLarge)
        }
        CircleIconButton(Icons.Default.ChevronLeft, "Semaine précédente", onPrevious)
        Spacer(Modifier.width(10.dp))
        CircleIconButton(Icons.Default.ChevronRight, "Semaine suivante", onNext)
    }
}

private fun weekTitle(start: LocalDate): String {
    val end = start.plusDays(6)
    val month = DateTimeFormatter.ofPattern("MMMM", FR)
    return if (start.month == end.month) {
        "${start.dayOfMonth} – ${end.dayOfMonth} ${end.format(month)}"
    } else {
        "${start.dayOfMonth} ${start.format(DateTimeFormatter.ofPattern("MMM", FR))} – " +
            "${end.dayOfMonth} ${end.format(DateTimeFormatter.ofPattern("MMM", FR))}"
    }
}

private val DAY_LETTERS = listOf("L", "M", "M", "J", "V", "S", "D")

@Composable
private fun DayStrip(
    weekStart: LocalDate,
    selected: LocalDate,
    daysWithEntries: Set<LocalDate>,
    onSelect: (LocalDate) -> Unit,
) {
    val today = LocalDate.now()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        (0..6).forEach { i ->
            val day = weekStart.plusDays(i.toLong())
            val isSelected = day == selected
            val content = if (isSelected) Color.White else MuscuColors.Text
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) MuscuColors.Primary else Color.Transparent)
                    .clickable { onSelect(day) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    DAY_LETTERS[i], fontSize = 12.sp,
                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else MuscuColors.TextSecondary,
                )
                Text(
                    day.dayOfMonth.toString(),
                    fontSize = 20.sp,
                    fontWeight = if (day == today) FontWeight.Bold else FontWeight.Medium,
                    color = if (!isSelected && day == today) MuscuColors.Primary else content,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
                Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                    if (day in daysWithEntries) {
                        Box(
                            Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White else MuscuColors.Success),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.Check, "Séance faite",
                                tint = if (isSelected) MuscuColors.Primary else Color.White,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryCard(item: DayEntryUi, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MuscuColors.Surface)
            .border(1.dp, MuscuColors.Border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExerciseAvatar(item.exercise.colorIndex, item.exercise.endImagePath ?: item.exercise.imagePath, 72.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(item.exercise.name, style = MaterialTheme.typography.titleMedium)
            Text(
                item.previous?.let { "Dernière fois : ${it.perf()}" } ?: "Première fois",
                color = MuscuColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            PerfChip(item.entry.perf())
        }
    }
}
