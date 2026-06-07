package com.blue2.app.ui.screens.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blue2.app.data.local.database.VehicleDao
import com.blue2.app.data.local.database.toDomain
import com.blue2.app.domain.models.Vehicle
import com.blue2.app.domain.models.VehicleStatus
import com.blue2.app.domain.repository.IBluelinkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiagnosticsUiState(
    val vehicle: Vehicle? = null,
    val status: VehicleStatus? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val repository: IBluelinkRepository,
    private val vehicleDao: VehicleDao,
) : ViewModel() {
    private val _state = MutableStateFlow(DiagnosticsUiState())
    val state: StateFlow<DiagnosticsUiState> = _state.asStateFlow()

    fun load(vin: String) {
        viewModelScope.launch {
            val vehicle = vehicleDao.getByVin(vin)?.toDomain()
            _state.update { it.copy(vehicle = vehicle, isLoading = true) }

            repository.observeVehicleStatus(vin)
                .onEach { status -> _state.update { it.copy(status = status, isLoading = false) } }
                .launchIn(this)

            repository.getVehicleStatus(vin, forceRefresh = false)
                .onFailure { e -> _state.update { it.copy(error = e.message, isLoading = false) } }
        }
    }

    fun refresh(vin: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.getVehicleStatus(vin, forceRefresh = true)
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
            _state.update { it.copy(isLoading = false) }
        }
    }
}
