package com.blue2.app.data.repository

import com.blue2.app.data.local.database.VehicleDao
import com.blue2.app.data.local.database.VehicleStatusDao
import com.blue2.app.data.local.database.toDomain
import com.blue2.app.data.local.database.toEntity
import com.blue2.app.data.local.preferences.AppPreferences
import com.blue2.app.domain.models.*
import com.blue2.app.domain.repository.IBluelinkRepository
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearRepositoryImpl @Inject constructor(
    private val prefs: AppPreferences,
    private val vehicleDao: VehicleDao,
    private val vehicleStatusDao: VehicleStatusDao,
    private val moshi: Moshi,
    private val okHttpClient: OkHttpClient,
) : IBluelinkRepository {

    private val json = "application/json; charset=utf-8".toMediaType()
    private val retrofitCache = mutableMapOf<String, Retrofit>()

    private fun retrofitFor(baseUrl: String) = retrofitCache.getOrPut(baseUrl) {
        Retrofit.Builder().baseUrl(baseUrl).client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi)).build()
    }

    // ─── Auth ──────────────────────────────────────────────────────────────

    override suspend fun login(
        email: String, password: String, region: BluelinkRegion, pin: String,
    ): Result<Unit> = runCatching {
        // On the watch, login is handled by the companion phone app.
        // The watch reads cached credentials from DataStore.
        throw UnsupportedOperationException("Login via the Blue2 phone app.")
    }

    override suspend fun logout() {
        prefs.clearSession()
        vehicleDao.deleteAll()
    }

    override suspend fun refreshTokens(): Result<Unit> = Result.failure(
        UnsupportedOperationException("Token refresh handled by phone app")
    )

    override fun isLoggedIn(): Flow<Boolean> = prefs.isLoggedIn

    // ─── Vehicles ──────────────────────────────────────────────────────────

    override suspend fun getVehicles(): Result<List<Vehicle>> = runCatching {
        vehicleDao.getAll().map { it.toDomain() }
    }

    override fun observeVehicles(): Flow<List<Vehicle>> =
        vehicleDao.observeAll().map { list -> list.map { it.toDomain() } }

    // ─── Status ────────────────────────────────────────────────────────────

    override suspend fun getVehicleStatus(vin: String, forceRefresh: Boolean): Result<VehicleStatus> = runCatching {
        vehicleStatusDao.get(vin)?.toDomain()
            ?: throw Exception("No cached status for $vin")
    }

    override fun observeVehicleStatus(vin: String): Flow<VehicleStatus?> =
        vehicleStatusDao.observe(vin).map { it?.toDomain() }

    // ─── Commands (stub — require phone for actual execution) ──────────────

    private fun notSupported(name: String): Result<CommandResult> =
        Result.success(CommandResult(success = false, error = "Open Blue2 on your phone to $name"))

    override suspend fun lock(vin: String) = notSupported("lock")
    override suspend fun unlock(vin: String) = notSupported("unlock")
    override suspend fun startEngine(vin: String, climate: ClimateSettings?) = notSupported("start engine")
    override suspend fun stopEngine(vin: String) = notSupported("stop engine")
    override suspend fun startClimate(vin: String, settings: ClimateSettings) = notSupported("start climate")
    override suspend fun stopClimate(vin: String) = notSupported("stop climate")
    override suspend fun startCharge(vin: String) = notSupported("start charge")
    override suspend fun stopCharge(vin: String) = notSupported("stop charge")
    override suspend fun setChargeTarget(vin: String, percentAc: Int, percentDc: Int) = notSupported("set charge target")
    override suspend fun flashLights(vin: String) = notSupported("flash lights")
    override suspend fun honkHorn(vin: String) = notSupported("honk horn")
    override suspend fun openTrunk(vin: String) = notSupported("open trunk")
    override suspend fun closeTrunk(vin: String) = notSupported("close trunk")

    // ─── Location ──────────────────────────────────────────────────────────

    override suspend fun getLocation(vin: String): Result<Pair<Double, Double>> = runCatching {
        val status = vehicleStatusDao.get(vin)
        val lat = status?.latitude ?: throw Exception("Location unavailable")
        val lon = status.longitude ?: throw Exception("Location unavailable")
        lat to lon
    }

    // ─── Custom images ─────────────────────────────────────────────────────

    override suspend fun setCustomImage(vin: String, uri: String?) {
        vehicleDao.updateCustomImage(vin, uri)
    }
}
