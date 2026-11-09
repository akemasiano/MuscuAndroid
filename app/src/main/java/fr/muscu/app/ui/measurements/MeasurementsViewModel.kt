package fr.muscu.app.ui.measurements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.muscu.app.data.Measurement
import fr.muscu.app.data.MuscuRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class MeasurementsUiState(
    /** Toutes les prises de mensurations, de la plus récente à la plus ancienne. */
    val history: List<Measurement> = emptyList(),
    val bicepsLeft: MeasureProgress = MeasureProgress(MeasureType.BICEPS_LEFT, emptyList()),
    val bicepsRight: MeasureProgress = MeasureProgress(MeasureType.BICEPS_RIGHT, emptyList()),
    val shoulders: MeasureProgress = MeasureProgress(MeasureType.SHOULDERS, emptyList()),
)

class MeasurementsViewModel(private val repo: MuscuRepository) : ViewModel() {

    val state = repo.measurements.map { all ->
        MeasurementsUiState(
            history = all.sortedWith(
                compareByDescending<Measurement> { it.epochDay }.thenByDescending { it.id }
            ),
            bicepsLeft = measureProgress(all, MeasureType.BICEPS_LEFT),
            bicepsRight = measureProgress(all, MeasureType.BICEPS_RIGHT),
            shoulders = measureProgress(all, MeasureType.SHOULDERS),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MeasurementsUiState())

    fun save(id: Long, date: LocalDate, bicepsLeftCm: Double?, bicepsRightCm: Double?, shouldersCm: Double?) {
        viewModelScope.launch { repo.saveMeasurement(id, date, bicepsLeftCm, bicepsRightCm, shouldersCm) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repo.deleteMeasurement(id) }
    }
}
