package com.blue2.app.tiles

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.blue2.app.data.local.database.VehicleDao
import com.blue2.app.data.local.preferences.AppPreferences
import com.blue2.app.domain.repository.IBluelinkRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class LockTileService : TileService() {

    @Inject lateinit var repository: IBluelinkRepository
    @Inject lateinit var prefs: AppPreferences
    @Inject lateinit var vehicleDao: VehicleDao

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartListening() {
        super.onStartListening()
        scope.launch { updateTile() }
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            val vin = prefs.lastSelectedVin.first()
                ?: vehicleDao.getAll().firstOrNull()?.vin
                ?: return@launch

            qsTile.state = Tile.STATE_UNAVAILABLE
            qsTile.updateTile()

            val status = repository.getVehicleStatus(vin).getOrNull()
            val result = if (status?.isLocked == false) repository.lock(vin) else repository.unlock(vin)

            result.onSuccess { updateTile() }
                .onFailure {
                    qsTile.state = Tile.STATE_INACTIVE
                    qsTile.updateTile()
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private suspend fun updateTile() {
        val vin = prefs.lastSelectedVin.first()
            ?: vehicleDao.getAll().firstOrNull()?.vin
            ?: run {
                qsTile.state = Tile.STATE_UNAVAILABLE
                qsTile.label = "Lock"
                qsTile.updateTile()
                return
            }

        val status = repository.getVehicleStatus(vin).getOrNull()
        qsTile.state = if (status?.isLocked == true) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        qsTile.label = if (status?.isLocked == true) "Locked" else "Unlocked"
        qsTile.updateTile()
    }
}

@AndroidEntryPoint
class ClimateStartTileService : TileService() {

    @Inject lateinit var repository: IBluelinkRepository
    @Inject lateinit var prefs: AppPreferences
    @Inject lateinit var vehicleDao: VehicleDao

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onClick() {
        super.onClick()
        scope.launch {
            val vin = prefs.lastSelectedVin.first()
                ?: vehicleDao.getAll().firstOrNull()?.vin
                ?: return@launch
            val status = repository.getVehicleStatus(vin).getOrNull()
            if (status?.climateOn == true) {
                repository.stopClimate(vin)
            } else {
                repository.startClimate(vin, com.blue2.app.domain.models.ClimateSettings())
            }
            updateTile(vin)
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        scope.launch {
            val vin = prefs.lastSelectedVin.first() ?: vehicleDao.getAll().firstOrNull()?.vin ?: return@launch
            updateTile(vin)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private suspend fun updateTile(vin: String) {
        val status = repository.getVehicleStatus(vin).getOrNull()
        qsTile.state = if (status?.climateOn == true) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile.label = if (status?.climateOn == true) "A/C On" else "Start A/C"
        qsTile.updateTile()
    }
}

@AndroidEntryPoint
class ChargeStartTileService : TileService() {

    @Inject lateinit var repository: IBluelinkRepository
    @Inject lateinit var prefs: AppPreferences
    @Inject lateinit var vehicleDao: VehicleDao

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onClick() {
        super.onClick()
        scope.launch {
            val vin = prefs.lastSelectedVin.first()
                ?: vehicleDao.getAll().firstOrNull()?.vin
                ?: return@launch
            val status = repository.getVehicleStatus(vin).getOrNull()
            if (status?.evCharging == true) repository.stopCharge(vin) else repository.startCharge(vin)
            updateTile(vin)
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        scope.launch {
            val vin = prefs.lastSelectedVin.first() ?: vehicleDao.getAll().firstOrNull()?.vin ?: return@launch
            updateTile(vin)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private suspend fun updateTile(vin: String) {
        val status = repository.getVehicleStatus(vin).getOrNull()
        qsTile.state = if (status?.evCharging == true) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile.label = if (status?.evCharging == true) "Stop Charge" else "Start Charge"
        qsTile.updateTile()
    }
}
