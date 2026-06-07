package com.blue2.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blue2.app.data.local.preferences.AppPreferences
import com.blue2.app.domain.models.*
import com.blue2.app.domain.repository.IBluelinkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

// All possible command keys in default order
val DEFAULT_COMMAND_ORDER = listOf(
    "engine", "climate", "charge", "chargeTarget",
    "trunk", "lights", "horn", "findCar",
)

data class HomeUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val statuses: Map<String, VehicleStatus> = emptyMap(),
    val currentPage: Int = 0,
    val isLoadingVehicles: Boolean = false,
    val isRefreshing: Boolean = false,
    val commandLoading: Map<String, Boolean> = emptyMap(),
    val error: String? = null,
    val successMessage: String? = null,
    val distanceUnit: String = "km",
    val tempUnit: String = "C",
    val commandOrder: List<String> = DEFAULT_COMMAND_ORDER,
    val carLocation: Map<String, Pair<Double, Double>> = emptyMap(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: IBluelinkRepository,
    private val prefs: AppPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val refreshJobs = mutableMapOf<String, Job>()

    init {
        observeVehicles()
        observePrefs()
        loadVehicles()
    }

    private fun observeVehicles() {
        viewModelScope.launch {
            repository.observeVehicles().collect { vehicles ->
                _state.update { it.copy(vehicles = vehicles) }
                vehicles.forEach { v -> observeStatus(v.vin) }
            }
        }
    }

    private fun observeStatus(vin: String) {
        viewModelScope.launch {
            repository.observeVehicleStatus(vin).collect { status ->
                if (status != null) {
                    _state.update { s -> s.copy(statuses = s.statuses + (vin to status)) }
                }
            }
        }
    }

    private fun observePrefs() {
        viewModelScope.launch {
            combine(prefs.distanceUnit, prefs.tempUnit) { dist, temp -> dist to temp }
                .collect { (dist, temp) -> _state.update { it.copy(distanceUnit = dist, tempUnit = temp) } }
        }
        viewModelScope.launch {
            prefs.commandOrder.collect { saved ->
                val order = saved?.split(",")?.filter { it.isNotBlank() }
                    ?.let { saved -> DEFAULT_COMMAND_ORDER.sortedBy { key ->
                        val i = saved.indexOf(key)
                        if (i < 0) saved.size + DEFAULT_COMMAND_ORDER.indexOf(key) else i
                    }}
                    ?: DEFAULT_COMMAND_ORDER
                _state.update { it.copy(commandOrder = order) }
            }
        }
    }

    fun loadVehicles() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingVehicles = true) }
            repository.getVehicles()
                .onSuccess { vehicles ->
                    _state.update { it.copy(isLoadingVehicles = false) }
                    vehicles.forEach { v -> refreshStatus(v.vin, forceRefresh = false) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoadingVehicles = false, error = e.message) }
                }
        }
    }

    fun refreshStatus(vin: String, forceRefresh: Boolean = true) {
        refreshJobs[vin]?.cancel()
        refreshJobs[vin] = viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            repository.getVehicleStatus(vin, forceRefresh)
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    fun refreshAll() {
        val vehicles = _state.value.vehicles
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            vehicles.forEach { v -> launch { repository.getVehicleStatus(v.vin, forceRefresh = true) } }
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    fun setPage(page: Int) { _state.update { it.copy(currentPage = page) } }

    fun lock(vin: String) = sendCommand(vin, "lock") { repository.lock(vin) }
    fun unlock(vin: String) = sendCommand(vin, "unlock") { repository.unlock(vin) }
    fun startEngine(vin: String, climate: ClimateSettings? = null) = sendCommand(vin, "engine") { repository.startEngine(vin, climate) }
    fun stopEngine(vin: String) = sendCommand(vin, "engine") { repository.stopEngine(vin) }
    fun startClimate(vin: String, settings: ClimateSettings) = sendCommand(vin, "climate") { repository.startClimate(vin, settings) }
    fun stopClimate(vin: String) = sendCommand(vin, "climate") { repository.stopClimate(vin) }
    fun startCharge(vin: String) = sendCommand(vin, "charge") { repository.startCharge(vin) }
    fun stopCharge(vin: String) = sendCommand(vin, "charge") { repository.stopCharge(vin) }
    fun setChargeTarget(vin: String, acPct: Int, dcPct: Int) = sendCommand(vin, "chargeTarget") { repository.setChargeTarget(vin, acPct, dcPct) }
    fun flashLights(vin: String) = sendCommand(vin, "lights") { repository.flashLights(vin) }
    fun honkHorn(vin: String) = sendCommand(vin, "horn") { repository.honkHorn(vin) }
    fun openTrunk(vin: String) = sendCommand(vin, "trunk") { repository.openTrunk(vin) }
    fun closeTrunk(vin: String) = sendCommand(vin, "trunk") { repository.closeTrunk(vin) }

    fun findCar(vin: String) {
        viewModelScope.launch {
            repository.getLocation(vin)
                .onSuccess { loc -> _state.update { it.copy(carLocation = it.carLocation + (vin to loc)) } }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }

    fun saveCommandOrder(order: List<String>) {
        _state.update { it.copy(commandOrder = order) }
        viewModelScope.launch { prefs.setCommandOrder(order.joinToString(",")) }
    }

    private fun commandKey(vin: String, cmd: String) = "${vin}_${cmd}"

    private fun sendCommand(vin: String, cmd: String, action: suspend () -> Result<CommandResult>) {
        val key = commandKey(vin, cmd)
        viewModelScope.launch {
            _state.update { it.copy(commandLoading = it.commandLoading + (key to true)) }
            action()
                .onSuccess { result ->
                    val msg = if (result.success) "Command sent" else result.error ?: "Failed"
                    _state.update { it.copy(
                        successMessage = if (result.success) msg else null,
                        error = if (!result.success) msg else null,
                    )}
                    delay(3000)
                    refreshStatus(vin, forceRefresh = true)
                }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message) }
                }
            _state.update { it.copy(commandLoading = it.commandLoading - key) }
        }
    }

    fun isCommandLoading(vin: String, cmd: String) = _state.value.commandLoading[commandKey(vin, cmd)] == true

    fun clearError() { _state.update { it.copy(error = null) } }
    fun clearSuccess() { _state.update { it.copy(successMessage = null) } }
}
