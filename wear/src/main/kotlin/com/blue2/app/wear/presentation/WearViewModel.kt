package com.blue2.app.wear.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blue2.app.domain.models.*
import com.blue2.app.domain.repository.IBluelinkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WearUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val statuses: Map<String, VehicleStatus> = emptyMap(),
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val commandLoading: Map<String, Boolean> = emptyMap(),
    val error: String? = null,
)

@HiltViewModel
class WearViewModel @Inject constructor(
    private val repository: IBluelinkRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(WearUiState())
    val state: StateFlow<WearUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.isLoggedIn().collect { loggedIn ->
                _state.update { it.copy(isLoggedIn = loggedIn) }
                if (loggedIn) loadVehicles()
            }
        }
    }

    private fun loadVehicles() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.getVehicles()
                .onSuccess { vehicles ->
                    _state.update { it.copy(isLoading = false) }
                    vehicles.forEach { v ->
                        launch {
                            repository.getVehicleStatus(v.vin).onSuccess { s ->
                                _state.update { st -> st.copy(statuses = st.statuses + (v.vin to s)) }
                            }
                        }
                    }
                }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
        viewModelScope.launch {
            repository.observeVehicles().collect { vehicles ->
                _state.update { it.copy(vehicles = vehicles) }
            }
        }
    }

    fun lock(vin: String) = command(vin, "lock") { repository.lock(vin) }
    fun unlock(vin: String) = command(vin, "unlock") { repository.unlock(vin) }
    fun startEngine(vin: String) = command(vin, "engine") { repository.startEngine(vin) }
    fun stopEngine(vin: String) = command(vin, "engine") { repository.stopEngine(vin) }
    fun startClimate(vin: String) = command(vin, "climate") { repository.startClimate(vin, ClimateSettings()) }
    fun stopClimate(vin: String) = command(vin, "climate") { repository.stopClimate(vin) }
    fun startCharge(vin: String) = command(vin, "charge") { repository.startCharge(vin) }
    fun stopCharge(vin: String) = command(vin, "charge") { repository.stopCharge(vin) }

    fun isCommandLoading(vin: String, cmd: String) = _state.value.commandLoading["${vin}_$cmd"] == true

    fun getStatus(vin: String) = _state.value.statuses[vin]

    private fun command(vin: String, key: String, action: suspend () -> Result<CommandResult>) {
        val k = "${vin}_$key"
        viewModelScope.launch {
            _state.update { it.copy(commandLoading = it.commandLoading + (k to true)) }
            action().onFailure { e -> _state.update { it.copy(error = e.message) } }
            _state.update { it.copy(commandLoading = it.commandLoading - k) }
        }
    }
}
