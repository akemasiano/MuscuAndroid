package fr.muscu.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import fr.muscu.app.data.EXERCISE_ASSET_PREFIX
import fr.muscu.app.ui.theme.MuscuColors
import java.io.File

private fun exerciseImageModel(path: String): Any =
    if (path.startsWith(EXERCISE_ASSET_PREFIX)) path else File(path)

/** Haltère dessinée, fidèle au style des maquettes. */
@Composable
fun DumbbellIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier.aspectRatio(1f)) {
        val w = size.width
        val h = size.height
        val barH = h * 0.11f
        drawRoundRect(
            color, Offset(w * 0.04f, (h - barH) / 2), Size(w * 0.92f, barH), CornerRadius(barH / 2)
        )
        val bigW = w * 0.13f
        val bigH = h * 0.66f
        listOf(w * 0.17f, w * 0.70f).forEach { x ->
            drawRoundRect(color, Offset(x, (h - bigH) / 2), Size(bigW, bigH), CornerRadius(bigW / 2.5f))
        }
        val smallW = w * 0.08f
        val smallH = h * 0.42f
        listOf(w * 0.33f, w * 0.59f).forEach { x ->
            drawRoundRect(
                color.copy(alpha = 0.75f), Offset(x, (h - smallH) / 2), Size(smallW, smallH),
                CornerRadius(smallW / 2.5f)
            )
        }
    }
}

@Composable
fun ExerciseAvatar(
    colorIndex: Int,
    imagePath: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    corner: Dp = size * 0.2f,
) {
    val (start, end) = MuscuColors.gradient(colorIndex)
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(Brush.linearGradient(listOf(start, end))),
        contentAlignment = Alignment.Center,
    ) {
        if (imagePath != null) {
            AsyncImage(
                model = exerciseImageModel(imagePath),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().background(MuscuColors.Surface),
            )
        } else {
            DumbbellIcon(Modifier.size(size * 0.58f))
        }
    }
}

@Composable
fun ExercisePhases(
    startImagePath: String?,
    endImagePath: String?,
    modifier: Modifier = Modifier,
    onEdit: ((Boolean) -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(false to startImagePath, true to endImagePath).forEach { (isEnd, path) ->
            val label = if (isEnd) "Fin" else "Départ"
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MuscuColors.Surface)
                    .border(1.dp, MuscuColors.Border, RoundedCornerShape(16.dp))
                    .then(if (onEdit != null) Modifier.clickable { onEdit(isEnd) } else Modifier)
                    .padding(12.dp),
            ) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().aspectRatio(0.85f), contentAlignment = Alignment.Center) {
                    if (path != null) {
                        AsyncImage(
                            model = exerciseImageModel(path),
                            contentDescription = "Position de ${if (isEnd) "fin" else "départ"}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            DumbbellIcon(Modifier.size(48.dp), MuscuColors.Primary)
                            Text("Sans image", color = MuscuColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                if (onEdit != null) {
                    Text(
                        if (path == null) "Ajouter" else "Modifier",
                        color = MuscuColors.Primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MuscuColors.TextSecondary,
        modifier = modifier,
    )
}

@Composable
fun PerfChip(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MuscuColors.PrimaryLight)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        color = MuscuColors.Primary,
        fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    enabled: Boolean = true,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(MuscuColors.Surface)
            .border(1.dp, MuscuColors.Border, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon, contentDescription,
            tint = if (enabled) MuscuColors.Text else MuscuColors.Border,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MuscuColors.Primary),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp))
    }
}

@Composable
fun ExtendedPrimaryFab(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(58.dp),
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 24.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MuscuColors.Primary),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(20.dp))
            Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp))
        }
    }
}
