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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import fr.muscu.app.ui.components.ExerciseAvatar
import fr.muscu.app.ui.components.PrimaryButton
import fr.muscu.app.ui.components.SectionLabel
import fr.muscu.app.ui.muscuViewModel
import fr.muscu.app.ui.theme.MuscuColors

@Composable
fun ExerciseEditScreen(exerciseId: Long, onBack: () -> Unit) {
    val vm = muscuViewModel(key = "exercise-$exerciseId") { ExerciseEditViewModel(it, exerciseId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var colorMenu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

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
            if (!state.isNew) {
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
            Box(Modifier.align(Alignment.CenterHorizontally)) {
                ExerciseAvatar(state.colorIndex, state.imagePath, 200.dp, corner = 40.dp)
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 12.dp, y = 12.dp)
                ) {
                    Box(
                        Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MuscuColors.Primary)
                            .border(3.dp, MuscuColors.Background, CircleShape)
                            .clickable { colorMenu = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Edit, "Personnaliser", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    DropdownMenu(
                        expanded = colorMenu,
                        onDismissRequest = { colorMenu = false },
                        containerColor = MuscuColors.Surface,
                    ) {
                        Text(
                            "Couleur", color = MuscuColors.TextSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            MuscuColors.Palette.forEachIndexed { index, (start, end) ->
                                Box(
                                    Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(start, end)))
                                        .clickable {
                                            vm.setColor(index)
                                            colorMenu = false
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (index == state.colorIndex) {
                                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                        if (state.imagePath != null) {
                            DropdownMenuItem(
                                text = { Text("Retirer la photo", color = MuscuColors.Danger) },
                                leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null, tint = MuscuColors.Danger) },
                                onClick = {
                                    vm.removeImage()
                                    colorMenu = false
                                },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SourceButton("Galerie", Icons.Outlined.PhotoLibrary, Modifier.weight(1f)) {
                    gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                SourceButton("Appareil photo", Icons.Outlined.PhotoCamera, Modifier.weight(1f)) {
                    try {
                        camera.launch(vm.prepareCameraUri())
                    } catch (e: ActivityNotFoundException) {
                        vm.onCameraResult(false)
                        Toast.makeText(context, "Aucune application appareil photo", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            SectionLabel("Nom de l’exercice", Modifier.padding(top = 36.dp, bottom = 10.dp))
            OutlinedTextField(
                value = state.name,
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
        }

        PrimaryButton(
            text = if (state.isNew) "Créer l’exercice" else "Enregistrer",
            enabled = state.loaded && !state.busy,
            onClick = { vm.save(onBack) },
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
