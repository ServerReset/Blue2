package com.blue2.app.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blue2.app.data.local.database.AutoLockConfigDao
import com.blue2.app.data.local.database.AutoLockConfigEntity
import com.blue2.app.data.local.database.VehicleDao
import com.blue2.app.data.local.database.toDomain
import com.blue2.app.data.local.preferences.AppPreferences
import com.blue2.app.domain.models.Vehicle
import com.blue2.app.domain.repository.IBluelinkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: String = "system",
    val themeStyle: String = "material_expressive",
    val dynamicColor: Boolean = true,
    val amoledMode: Boolean = false,
    val useAtkinsonFont: Boolean = false,
    val distanceUnit: String = "km",
    val tempUnit: String = "C",
    val autoRefreshInterval: Int = 15,
    val appLog: String = "",
    val vehicles: List<Vehicle> = emptyList(),
    val autoLockConfigs: Map<String, AutoLockConfigEntity> = emptyMap(),
    val isLoading: Boolean = false,
    val userEmail: String? = null,
    val region: String = "US",
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPreferences,
    private val repository: IBluelinkRepository,
    private val vehicleDao: VehicleDao,
    private val autoLockConfigDao: AutoLockConfigDao,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(prefs.themeMode, prefs.themeStyle) { tm, ts -> tm to ts }
                .collect { (tm, ts) -> _state.update { st -> st.copy(themeMode = tm, themeStyle = ts) } }
        }
        viewModelScope.launch {
            combine(prefs.dynamicColor, prefs.amoledMode, prefs.useAtkinsonFont) { dc, am, af ->
                Triple(dc, am, af)
            }.collect { (dc, am, af) ->
                _state.update { st -> st.copy(dynamicColor = dc, amoledMode = am, useAtkinsonFont = af) }
            }
        }
        viewModelScope.launch {
            combine(prefs.distanceUnit, prefs.tempUnit, prefs.autoRefreshIntervalMin) { d, t, r -> Triple(d, t, r) }
                .collect { (d, t, r) -> _state.update { it.copy(distanceUnit = d, tempUnit = t, autoRefreshInterval = r) } }
        }
        viewModelScope.launch {
            prefs.appLog.collect { log -> _state.update { it.copy(appLog = log) } }
        }
        viewModelScope.launch {
            vehicleDao.observeAll().collect { list ->
                val vehicles = list.map { it.toDomain() }
                _state.update { it.copy(vehicles = vehicles) }
                vehicles.forEach { v ->
                    autoLockConfigDao.observe(v.vin).collect { config ->
                        if (config != null) {
                            _state.update { s -> s.copy(autoLockConfigs = s.autoLockConfigs + (v.vin to config)) }
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            combine(prefs.userEmail, prefs.region) { e, r -> e to r }
                .collect { (e, r) -> _state.update { it.copy(userEmail = e, region = r.name) } }
        }
    }

    fun setThemeMode(mode: String) { viewModelScope.launch { prefs.setThemeMode(mode) } }
    fun setThemeStyle(style: String) { viewModelScope.launch { prefs.setThemeStyle(style) } }
    fun setDynamicColor(v: Boolean) { viewModelScope.launch { prefs.setDynamicColor(v) } }
    fun setAmoledMode(v: Boolean) { viewModelScope.launch { prefs.setAmoledMode(v) } }
    fun setAtkinsonFont(v: Boolean) { viewModelScope.launch { prefs.setUseAtkinsonFont(v) } }
    fun setDistanceUnit(v: String) { viewModelScope.launch { prefs.setDistanceUnit(v) } }
    fun setTempUnit(v: String) { viewModelScope.launch { prefs.setTempUnit(v) } }
    fun setAutoRefreshInterval(v: Int) { viewModelScope.launch { prefs.setAutoRefreshInterval(v) } }

    fun clearLog() { viewModelScope.launch { prefs.clearLog() } }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }

    fun setCustomImage(vin: String, uri: Uri?) {
        viewModelScope.launch {
            // Copy image to internal storage for persistence
            val savedUri = if (uri != null) saveImageLocally(context, uri, vin) else null
            repository.setCustomImage(vin, savedUri?.toString())
        }
    }

    fun setAutoLock(vin: String, enabled: Boolean, deviceName: String?, deviceAddress: String?, delaySeconds: Int) {
        viewModelScope.launch {
            autoLockConfigDao.insert(
                AutoLockConfigEntity(
                    vin = vin,
                    enabled = enabled,
                    bluetoothDeviceName = deviceName,
                    bluetoothDeviceAddress = deviceAddress,
                    delaySeconds = delaySeconds,
                )
            )
        }
    }

    private fun saveImageLocally(context: Context, uri: Uri, vin: String): Uri? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val file = java.io.File(context.filesDir, "car_images/$vin.jpg")
            file.parentFile?.mkdirs()
            file.outputStream().use { output -> inputStream.copyTo(output) }
            Uri.fromFile(file)
        } catch (e: Exception) {
            null
        }
    }
}
