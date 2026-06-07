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

val DEFAULT_COMMAND_ORDER = listOf(
    "engine", "climate", "charge", "chargeTarget",
    "trunk", "lights", "horn", "findCar",
)

data class CommandRecord(
    val commandName: String,
    val vehicleNickname: String,
    val timestampMs: Long,
    val success: Boolean,
    val detail: String,
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
    val recentCommands: List<CommandRecord> = emptyList(),
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
                if (status != null) _state.update { s -> s.copy(statuses = s.statuses + (vin to status)) }
            }
        }
    }

    private fun observePrefs() {
        viewModelScope.launch {
            combine(prefs.distanceUnit, prefs.tempUnit) { d, t -> d to t }
                .collect { (d, t) -> _state.update { it.copy(distanceUnit = d, tempUnit = t) } }
        }
        viewModelScope.launch {
            prefs.commandOrder.collect { saved ->
                val order = saved?.split(",")?.filter { it.isNotBlank() }
                    ?.let { keys -> DEFAULT_COMMAND_ORDER.sortedBy { k -> keys.indexOf(k).let { if (it < 0) keys.size + DEFAULT_COMMAND_ORDER.indexOf(k) else it } } }
                    ?: DEFAULT_COMMAND_ORDER
                _state.update { it.copy(commandOrder = order) }
            }
        }
        viewModelScope.launch {
            prefs.commandHistory.collect { json ->
                val records = json?.let { parseCommandHistory(it) } ?: emptyList()
                _state.update { it.copy(recentCommands = records) }
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
                .onFailure { e -> _state.update { it.copy(isLoadingVehicles = false, error = e.message) } }
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
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            _state.value.vehicles.forEach { v -> launch { repository.getVehicleStatus(v.vin, true) } }
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    fun setPage(page: Int) { _state.update { it.copy(currentPage = page) } }

    fun lock(vin: String) = sendCommand(vin, "lock", "Lock doors") { repository.lock(vin) }
    fun unlock(vin: String) = sendCommand(vin, "unlock", "Unlock doors") { repository.unlock(vin) }
    fun startEngine(vin: String, climate: ClimateSettings? = null) = sendCommand(vin, "engine", "Start engine") { repository.startEngine(vin, climate) }
    fun stopEngine(vin: String) = sendCommand(vin, "engine", "Stop engine") { repository.stopEngine(vin) }
    fun startClimate(vin: String, settings: ClimateSettings) = sendCommand(vin, "climate", "Start climate") { repository.startClimate(vin, settings) }
    fun stopClimate(vin: String) = sendCommand(vin, "climate", "Stop climate") { repository.stopClimate(vin) }
    fun startCharge(vin: String) = sendCommand(vin, "charge", "Start charge") { repository.startCharge(vin) }
    fun stopCharge(vin: String) = sendCommand(vin, "charge", "Stop charge") { repository.stopCharge(vin) }
    fun setChargeTarget(vin: String, acPct: Int, dcPct: Int) = sendCommand(vin, "chargeTarget", "Set charge target") { repository.setChargeTarget(vin, acPct, dcPct) }
    fun flashLights(vin: String) = sendCommand(vin, "lights", "Flash lights") { repository.flashLights(vin) }
    fun honkHorn(vin: String) = sendCommand(vin, "horn", "Honk horn") { repository.honkHorn(vin) }
    fun openTrunk(vin: String) = sendCommand(vin, "trunk", "Open trunk") { repository.openTrunk(vin) }
    fun closeTrunk(vin: String) = sendCommand(vin, "trunk", "Close trunk") { repository.closeTrunk(vin) }

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

    fun clearCommandHistory() {
        _state.update { it.copy(recentCommands = emptyList()) }
        viewModelScope.launch { prefs.setCommandHistory("") }
    }

    private fun commandKey(vin: String, cmd: String) = "${vin}_${cmd}"

    private fun sendCommand(vin: String, cmd: String, displayName: String, action: suspend () -> Result<CommandResult>) {
        val key = commandKey(vin, cmd)
        viewModelScope.launch {
            _state.update { it.copy(commandLoading = it.commandLoading + (key to true)) }
            val vehicleName = _state.value.vehicles.find { it.vin == vin }?.nickname ?: vin
            action()
                .onSuccess { result ->
                    recordCommand(CommandRecord(
                        commandName = displayName,
                        vehicleNickname = vehicleName,
                        timestampMs = System.currentTimeMillis(),
                        success = result.success,
                        detail = if (result.success) result.message ?: "Accepted by Hyundai" else result.error ?: "Failed",
                    ))
                    val msg = if (result.success) "$displayName sent" else result.error ?: "Failed"
                    _state.update { it.copy(
                        successMessage = if (result.success) msg else null,
                        error = if (!result.success) msg else null,
                    )}
                    delay(3000)
                    refreshStatus(vin, forceRefresh = true)
                }
                .onFailure { e ->
                    recordCommand(CommandRecord(
                        commandName = displayName,
                        vehicleNickname = vehicleName,
                        timestampMs = System.currentTimeMillis(),
                        success = false,
                        detail = e.message ?: "Error",
                    ))
                    _state.update { it.copy(error = e.message) }
                }
            _state.update { it.copy(commandLoading = it.commandLoading - key) }
        }
    }

    private fun recordCommand(record: CommandRecord) {
        val updated = (listOf(record) + _state.value.recentCommands).take(20)
        _state.update { it.copy(recentCommands = updated) }
        viewModelScope.launch { prefs.setCommandHistory(serializeCommandHistory(updated)) }
    }

    fun isCommandLoading(vin: String, cmd: String) = _state.value.commandLoading[commandKey(vin, cmd)] == true
    fun clearError() { _state.update { it.copy(error = null) } }
    fun clearSuccess() { _state.update { it.copy(successMessage = null) } }

    private fun serializeCommandHistory(records: List<CommandRecord>): String =
        records.joinToString("|") { r ->
            "${r.commandName}~~${r.vehicleNickname}~~${r.timestampMs}~~${r.success}~~${r.detail}"
        }

    private fun parseCommandHistory(s: String): List<CommandRecord> =
        s.split("|").mapNotNull { entry ->
            val parts = entry.split("~~")
            if (parts.size == 5) CommandRecord(
                commandName = parts[0],
                vehicleNickname = parts[1],
                timestampMs = parts[2].toLongOrNull() ?: return@mapNotNull null,
                success = parts[3] == "true",
                detail = parts[4],
            ) else null
        }
}
