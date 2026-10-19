package fr.muscu.app.ui.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.muscu.app.data.perf
import fr.muscu.app.data.relativeDay
import fr.muscu.app.ui.components.ExerciseAvatar
import fr.muscu.app.ui.components.ExtendedPrimaryFab
import fr.muscu.app.ui.components.SectionLabel
import fr.muscu.app.ui.muscuViewModel
import fr.muscu.app.ui.theme.MuscuColors

@Composable
fun ExercisesScreen(onOpenExercise: (Long) -> Unit, onNewExercise: () -> Unit) {
    val vm = muscuViewModel { ExercisesViewModel(it) }
    val items by vm.items.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)) {
                SectionLabel("Bibliothèque")
                Text("Mes exercices", style = MaterialTheme.typography.headlineLarge)
                SearchField(query, onChange = { vm.query.value = it }, Modifier.padding(vertical = 14.dp))
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 110.dp)) {
                if (items.isEmpty()) {
                    item {
                        Text(
                            if (query.isBlank()) "Aucun exercice. Crée ton premier exercice !"
                            else "Aucun exercice ne correspond à « $query »",
                            color = MuscuColors.TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                        )
                    }
                }
                items(items, key = { it.exercise.id }) { item ->
                    ExerciseRow(item) { onOpenExercise(item.exercise.id) }
                    HorizontalDivider(color = MuscuColors.Border)
                }
            }
        }
        ExtendedPrimaryFab(
            text = "Nouvel exercice",
            icon = Icons.Default.Add,
            onClick = onNewExercise,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        )
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MuscuColors.Surface)
            .border(1.dp, MuscuColors.Border, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, null, tint = MuscuColors.TextSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text("Rechercher un exercice", color = MuscuColors.TextSecondary)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MuscuColors.Text),
                cursorBrush = SolidColor(MuscuColors.Primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ExerciseRow(item: ExerciseItem, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExerciseAvatar(item.exercise.colorIndex, item.exercise.imagePath, 58.dp, corner = 14.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(item.exercise.name, style = MaterialTheme.typography.titleMedium)
            Text(
                buildAnnotatedString {
                    val last = item.last
                    if (last == null) {
                        append("Pas encore réalisé")
                    } else {
                        withStyle(SpanStyle(color = MuscuColors.Primary, fontWeight = FontWeight.SemiBold)) {
                            append(last.perf())
                        }
                        append(" · ${relativeDay(last.epochDay)}")
                    }
                },
                color = MuscuColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
