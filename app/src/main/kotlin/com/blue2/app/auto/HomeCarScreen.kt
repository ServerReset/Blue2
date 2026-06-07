package com.blue2.app.auto

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.lifecycle.lifecycleScope
import com.blue2.app.data.local.preferences.AppPreferences
import com.blue2.app.domain.models.ClimateSettings
import com.blue2.app.domain.models.FuelType
import com.blue2.app.domain.models.Vehicle
import com.blue2.app.domain.models.VehicleStatus
import com.blue2.app.domain.repository.IBluelinkRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AutoEntryPoint {
    fun repository(): IBluelinkRepository
    fun prefs(): AppPreferences
}

class HomeCarScreen(carContext: CarContext) : Screen(carContext) {

    private val ep = EntryPointAccessors.fromApplication(carContext.applicationContext, AutoEntryPoint::class.java)
    private val repository = ep.repository()

    private var vehicles: List<Vehicle> = emptyList()
    private var statuses: Map<String, VehicleStatus> = emptyMap()
    private var isLoading = true

    init {
        lifecycleScope.launch {
            repository.getVehicles()
                .onSuccess { v ->
                    vehicles = v
                    v.forEach { vehicle ->
                        launch {
                            repository.getVehicleStatus(vehicle.vin).onSuccess { s ->
                                statuses = statuses + (vehicle.vin to s)
                                invalidate()
                            }
                        }
                    }
                    isLoading = false
                    invalidate()
                }
                .onFailure {
                    isLoading = false
                    invalidate()
                }
        }
    }

    override fun onGetTemplate(): Template {
        if (isLoading) {
            return MessageTemplate.Builder("Loading vehicles…")
                .setTitle("Blue2")
                .setLoading(true)
                .build()
        }

        if (vehicles.isEmpty()) {
            return MessageTemplate.Builder("No vehicles found. Check your Bluelink account in the app.")
                .setTitle("Blue2")
                .build()
        }

        val listBuilder = ItemList.Builder()
        vehicles.forEach { vehicle ->
            val status = statuses[vehicle.vin]
            val description = buildString {
                status?.let { s ->
                    append(if (s.isLocked) "Locked" else "Unlocked")
                    s.evBatteryPercent?.let { append(" · ⚡$it%") }
                    s.fuelLevelPercent?.let { append(" · ⛽$it%") }
                    if (s.engineRunning) append(" · Running")
                } ?: append("Tap to control")
            }

            listBuilder.addItem(
                Row.Builder()
                    .setTitle(vehicle.nickname)
                    .addText(description)
                    .setOnClickListener {
                        screenManager.push(CarControlScreen(carContext, vehicle, status))
                    }
                    .build()
            )
        }

        return ListTemplate.Builder()
            .setTitle("Blue2 — My Vehicles")
            .setSingleList(listBuilder.build())
            .build()
    }
}

class CarControlScreen(
    carContext: CarContext,
    private val vehicle: Vehicle,
    private var status: VehicleStatus?,
) : Screen(carContext) {

    private val ep = EntryPointAccessors.fromApplication(carContext.applicationContext, AutoEntryPoint::class.java)
    private val repository = ep.repository()
    private var isLoading = false

    override fun onGetTemplate(): Template {
        val paneBuilder = Pane.Builder()

        paneBuilder.addRow(
            Row.Builder()
                .setTitle(if (status?.isLocked == true) "🔒 Locked" else "🔓 Unlocked")
                .addText("Tap to ${if (status?.isLocked == true) "unlock" else "lock"}")
                .build()
        )

        val lockAction = Action.Builder()
            .setTitle(if (status?.isLocked == true) "Unlock" else "Lock")
            .setOnClickListener {
                lifecycleScope.launch {
                    isLoading = true
                    invalidate()
                    if (status?.isLocked == true) repository.unlock(vehicle.vin)
                    else repository.lock(vehicle.vin)
                    refreshStatus()
                }
            }
            .build()

        val climateAction = Action.Builder()
            .setTitle(if (status?.climateOn == true) "Stop A/C" else "Start A/C")
            .setOnClickListener {
                lifecycleScope.launch {
                    if (status?.climateOn == true) repository.stopClimate(vehicle.vin)
                    else repository.startClimate(vehicle.vin, ClimateSettings())
                    refreshStatus()
                }
            }
            .build()

        val actions = mutableListOf(lockAction, climateAction)

        if (vehicle.fuelType != FuelType.ELECTRIC) {
            actions.add(
                Action.Builder()
                    .setTitle(if (status?.engineRunning == true) "Stop Engine" else "Start Engine")
                    .setOnClickListener {
                        lifecycleScope.launch {
                            if (status?.engineRunning == true) repository.stopEngine(vehicle.vin)
                            else repository.startEngine(vehicle.vin)
                            refreshStatus()
                        }
                    }
                    .build()
            )
        }

        if (vehicle.fuelType == FuelType.ELECTRIC || vehicle.fuelType == FuelType.PHEV) {
            actions.add(
                Action.Builder()
                    .setTitle(if (status?.evCharging == true) "Stop Charge" else "Start Charge")
                    .setOnClickListener {
                        lifecycleScope.launch {
                            if (status?.evCharging == true) repository.stopCharge(vehicle.vin)
                            else repository.startCharge(vehicle.vin)
                            refreshStatus()
                        }
                    }
                    .build()
            )
        }

        status?.evBatteryPercent?.let { bat ->
            paneBuilder.addRow(
                Row.Builder()
                    .setTitle("⚡ Battery")
                    .addText("$bat%${status?.evRangeKm?.let { " · %.0f km range".format(it) } ?: ""}")
                    .build()
            )
        }

        status?.fuelLevelPercent?.let { fuel ->
            paneBuilder.addRow(
                Row.Builder()
                    .setTitle("⛽ Fuel")
                    .addText("$fuel%${status?.fuelRangeKm?.let { " · %.0f km range".format(it) } ?: ""}")
                    .build()
            )
        }

        if (isLoading) paneBuilder.setLoading(true)

        actions.take(4).forEach { paneBuilder.addAction(it) }

        return PaneTemplate.Builder(paneBuilder.build())
            .setTitle(vehicle.nickname)
            .setHeaderAction(Action.BACK)
            .build()
    }

    private suspend fun refreshStatus() {
        repository.getVehicleStatus(vehicle.vin, forceRefresh = true).onSuccess { s -> status = s }
        isLoading = false
        invalidate()
    }
}
