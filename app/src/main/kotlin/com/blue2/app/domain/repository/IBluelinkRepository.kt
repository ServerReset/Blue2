package com.blue2.app.domain.repository

import com.blue2.app.domain.models.*
import kotlinx.coroutines.flow.Flow

interface IBluelinkRepository {
    // Auth
    suspend fun login(email: String, password: String, region: BluelinkRegion, pin: String = ""): Result<Unit>
    suspend fun logout()
    suspend fun refreshTokens(): Result<Unit>
    fun isLoggedIn(): Flow<Boolean>

    // Vehicles
    suspend fun getVehicles(): Result<List<Vehicle>>
    fun observeVehicles(): Flow<List<Vehicle>>

    // Status
    suspend fun getVehicleStatus(vin: String, forceRefresh: Boolean = false): Result<VehicleStatus>
    fun observeVehicleStatus(vin: String): Flow<VehicleStatus?>

    // Commands
    suspend fun lock(vin: String): Result<CommandResult>
    suspend fun unlock(vin: String): Result<CommandResult>
    suspend fun startEngine(vin: String, climate: ClimateSettings? = null): Result<CommandResult>
    suspend fun stopEngine(vin: String): Result<CommandResult>
    suspend fun startClimate(vin: String, settings: ClimateSettings): Result<CommandResult>
    suspend fun stopClimate(vin: String): Result<CommandResult>
    suspend fun startCharge(vin: String): Result<CommandResult>
    suspend fun stopCharge(vin: String): Result<CommandResult>
    suspend fun setChargeTarget(vin: String, percentAc: Int, percentDc: Int): Result<CommandResult>
    suspend fun flashLights(vin: String): Result<CommandResult>
    suspend fun honkHorn(vin: String): Result<CommandResult>
    suspend fun openTrunk(vin: String): Result<CommandResult>
    suspend fun closeTrunk(vin: String): Result<CommandResult>

    // Location
    suspend fun getLocation(vin: String): Result<Pair<Double, Double>>

    // Custom images
    suspend fun setCustomImage(vin: String, uri: String?)
}
