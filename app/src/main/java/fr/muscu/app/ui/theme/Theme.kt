package fr.muscu.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object MuscuColors {
    val Primary = Color(0xFF1F5F5B)
    val PrimaryLight = Color(0xFFDCEDE9)
    val Success = Color(0xFF2E9E5B)
    val Background = Color(0xFFF4F2ED)
    val Surface = Color(0xFFFFFFFF)
    val Border = Color(0xFFE6E2DA)
    val Text = Color(0xFF1C1C1A)
    val TextSecondary = Color(0xFF7A7770)
    val Danger = Color(0xFFB3261E)

    /** Dégradés des vignettes d'exercice. */
    val Palette: List<Pair<Color, Color>> = listOf(
        Color(0xFF4B5D73) to Color(0xFF8395AB), // ardoise
        Color(0xFF5C7537) to Color(0xFF9AAE6B), // olive
        Color(0xFF9C5A3A) to Color(0xFFD69E78), // cuivre
        Color(0xFF5A3A5F) to Color(0xFF9273A0), // prune
        Color(0xFF1E6B62) to Color(0xFF4FA394), // sarcelle
        Color(0xFF2F4A7A) to Color(0xFF6C8CC4), // bleu
    )

    fun gradient(index: Int) = Palette[Math.floorMod(index, Palette.size)]
}

private val colorScheme = lightColorScheme(
    primary = MuscuColors.Primary,
    onPrimary = Color.White,
    primaryContainer = MuscuColors.PrimaryLight,
    onPrimaryContainer = MuscuColors.Primary,
    secondaryContainer = MuscuColors.PrimaryLight,
    onSecondaryContainer = MuscuColors.Primary,
    background = MuscuColors.Background,
    onBackground = MuscuColors.Text,
    surface = MuscuColors.Background,
    onSurface = MuscuColors.Text,
    surfaceContainerLow = MuscuColors.Surface,
    surfaceContainer = MuscuColors.Surface,
    surfaceContainerHigh = MuscuColors.Surface,
    surfaceVariant = MuscuColors.Surface,
    onSurfaceVariant = MuscuColors.TextSecondary,
    outline = MuscuColors.Border,
    outlineVariant = MuscuColors.Border,
    error = MuscuColors.Danger,
)

private val typography = Typography(
    headlineLarge = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp),
)

@Composable
fun MuscuTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorScheme, typography = typography, content = content)
}
