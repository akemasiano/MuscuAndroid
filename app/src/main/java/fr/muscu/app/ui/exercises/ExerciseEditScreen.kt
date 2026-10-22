package fr.muscu.app.ui.exercises

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.muscu.app.ui.components.CircleIconButton
import fr.muscu.app.ui.components.ExercisePhases
import fr.muscu.app.ui.components.PrimaryButton
import fr.muscu.app.ui.components.SectionLabel
import fr.muscu.app.ui.muscuViewModel
import fr.muscu.app.ui.theme.MuscuColors

@Composable
fun ExerciseEditScreen(exerciseId: Long, onBack: () -> Unit) {
    val vm = muscuViewModel(key = "exercise-$exerciseId") { ExerciseEditViewModel(it, exerciseId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var imageMenu by remember { mutableStateOf<Boolean?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val editable = state.loaded && !state.busy && !state.importing

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.onGalleryPicked(uri)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        vm.onCameraResult(ok)
    }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(Icons.Default.ChevronLeft, "Retour", onBack)
            Spacer(Modifier.width(16.dp))
            Text(
                if (state.isNew) "Nouvel exercice" else "Modifier l’exercice",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            if (!state.isNew && editable) {
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Outlined.DeleteOutline, "Supprimer", tint = MuscuColors.Danger)
                }
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(24.dp))
            SectionLabel("Nom de l’exercice", Modifier.padding(bottom = 10.dp))
            OutlinedTextField(
                value = state.name,
                enabled = editable,
                onValueChange = vm::setName,
                singleLine = true,
                placeholder = { Text("Ex. Soulevé de terre roumain") },
                isError = state.error != null,
                supportingText = state.error?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MuscuColors.Surface,
                    unfocusedContainerColor = MuscuColors.Surface,
                    errorContainerColor = MuscuColors.Surface,
                    unfocusedBorderColor = MuscuColors.Border,
                    focusedBorderColor = MuscuColors.Primary,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            SectionLabel("Phases du mouvement", Modifier.padding(top = 24.dp, bottom = 10.dp))
            Text(
                "Une image pour chaque position, sans recadrage.",
                color = MuscuColors.TextSecondary,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            ExercisePhases(
                startImagePath = state.imagePath,
                endImagePath = state.endImagePath,
                onEdit = if (editable) { isEnd -> imageMenu = isEnd } else null,
            )
            TextButton(
                enabled = editable && (state.imagePath != null || state.endImagePath != null),
                onClick = vm::swapImages,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) { Text("Inverser les deux images") }
            if (state.importing) {
                Text("Import de l’image…", color = MuscuColors.Primary)
            }
            state.imageError?.let {
                Text(it, color = MuscuColors.Danger, modifier = Modifier.padding(vertical = 8.dp))
            }
            SectionLabel("Couleur de la vignette", Modifier.padding(top = 20.dp, bottom = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MuscuColors.Palette.forEachIndexed { index, (start, end) ->
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(start, end)))
                            .clickable(enabled = editable) { vm.setColor(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (index == state.colorIndex) {
                            Icon(Icons.Default.Check, "Couleur sélectionnée", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        PrimaryButton(
            text = if (state.isNew) "Créer l’exercice" else "Enregistrer",
            enabled = editable,
            onClick = { vm.save(onBack) },
        )
    }

    imageMenu?.let { isEnd ->
        AlertDialog(
            onDismissRequest = { imageMenu = null },
            title = { Text("Image de ${if (isEnd) "fin" else "départ"}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SourceButton("Galerie", Icons.Outlined.PhotoLibrary, Modifier.fillMaxWidth()) {
                        vm.selectImagePhase(isEnd)
                        imageMenu = null
                        try {
                            gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(context, "Aucune application galerie", Toast.LENGTH_SHORT).show()
                        } catch (e: SecurityException) {
                            Toast.makeText(context, "Accès à la galerie refusé", Toast.LENGTH_SHORT).show()
                        }
                    }
                    SourceButton("Appareil photo", Icons.Outlined.PhotoCamera, Modifier.fillMaxWidth()) {
                        vm.selectImagePhase(isEnd)
                        imageMenu = null
                        val uri = vm.prepareCameraUri()
                        if (uri != null) {
                            try {
                                camera.launch(uri)
                            } catch (e: ActivityNotFoundException) {
                                vm.onCameraResult(false)
                                Toast.makeText(context, "Aucune application appareil photo", Toast.LENGTH_SHORT).show()
                            } catch (e: SecurityException) {
                                vm.onCameraResult(false)
                                Toast.makeText(context, "Accès à l’appareil photo refusé", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    if ((if (isEnd) state.endImagePath else state.imagePath) != null) {
                        TextButton(onClick = {
                            vm.removeImage(isEnd)
                            imageMenu = null
                        }) { Text("Retirer l’image", color = MuscuColors.Danger) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { imageMenu = null }) { Text("Annuler") }
            },
            containerColor = MuscuColors.Surface,
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Supprimer cet exercice ?") },
            text = { Text("Tout l’historique de « ${state.name} » sera également supprimé.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.delete(onBack)
                }) { Text("Supprimer", color = MuscuColors.Danger) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Annuler") }
            },
            containerColor = MuscuColors.Surface,
        )
    }
}

@Composable
private fun SourceButton(text: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MuscuColors.Surface)
            .border(1.dp, MuscuColors.Border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}
