package fr.muscu.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.muscu.app.MuscuApp
import fr.muscu.app.data.MuscuRepository

@Composable
inline fun <reified VM : ViewModel> muscuViewModel(
    key: String? = null,
    crossinline create: (MuscuRepository) -> VM,
): VM {
    val repository = (LocalContext.current.applicationContext as MuscuApp).repository
    return viewModel(key = key, factory = viewModelFactory { initializer { create(repository) } })
}
