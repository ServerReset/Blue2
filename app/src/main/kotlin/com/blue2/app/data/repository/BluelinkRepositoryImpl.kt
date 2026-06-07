package com.blue2.app.data.repository

import com.blue2.app.data.api.*
import com.blue2.app.data.api.models.*
import com.blue2.app.data.local.database.*
import com.blue2.app.data.local.preferences.AppPreferences
import com.blue2.app.domain.models.*
import com.blue2.app.domain.repository.IBluelinkRepository
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluelinkRepositoryImpl @Inject constructor(
    private val prefs: AppPreferences,
    private val vehicleDao: VehicleDao,
    private val vehicleStatusDao: VehicleStatusDao,
    private val moshi: Moshi,
    private val okHttpClient: OkHttpClient,
) : IBluelinkRepository {

    // ─── In-memory state ──────────────────────────────────────────────────
    private var currentRegion: BluelinkRegion = BluelinkRegion.US
    private var currentEmail: String = ""
    private var currentUserId: String = ""
    private var currentDeviceId: String = ""
    private var currentAccessToken: String = ""
    private var currentRefreshToken: String = ""
    private var tokenExpiry: Long = 0L
    private var currentVehiclePin: String = ""
    private var currentCaPAuth: String = ""
    private var caPAuthExpiry: Long = 0L
    private var currentEuControlToken: String = ""
    private var euControlTokenExpiry: Long = 0L

    private val retrofitCache = mutableMapOf<String, Retrofit>()

    private fun retrofitFor(baseUrl: String) = retrofitCache.getOrPut(baseUrl) {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    private fun usApi(url: String) = retrofitFor(url).create(UsBluelinkApiService::class.java)
    private fun caApi(url: String) = retrofitFor(url).create(CaBluelinkApiService::class.java)
    private fun euApi(url: String) = retrofitFor(url).create(EuBluelinkApiService::class.java)
    private fun euIdpApi(url: String) = retrofitFor(url).create(EuBluelinkIdpService::class.java)

    // ─── Auth ──────────────────────────────────────────────────────────────

    override suspend fun login(
        email: String,
        password: String,
        region: BluelinkRegion,
        pin: String,
    ): Result<Unit> = runCatching {
        val config = RegionConstants.forRegion(region)
        currentRegion = region
        currentEmail = email
        currentDeviceId = RegionConstants.generateDeviceId()
        if (pin.isNotBlank()) currentVehiclePin = pin

        prefs.saveRegion(region)
        prefs.saveDeviceId(currentDeviceId)
        prefs.appendLog("Login: region=$region")

        when (region) {
            BluelinkRegion.US -> {
                loginUs(email, password, config)
                if (pin.isNotBlank()) prefs.saveUsPin(pin)
            }
            BluelinkRegion.CA -> {
                loginCa(email, password, config)
                if (pin.isNotBlank()) prefs.saveCaPin(pin)
            }
            else -> {
                loginEu(email, password, config)
                if (pin.isNotBlank()) prefs.saveEuPin(pin)
            }
        }
        prefs.appendLog("Login OK: region=$region")
    }.onFailure { prefs.appendLog("Login failed: ${it.message}") }

    private suspend fun loginUs(email: String, password: String, config: RegionConstants.RegionConfig) {
        val resp = usApi(config.baseUrl).login(
            clientId = config.clientId,
            clientSecret = config.clientSecret,
            request = UsLoginRequest(username = email, password = password),
        )
        val body = resp.body()
        if (!resp.isSuccessful || body?.accessToken == null)
            throw BluelinkError.AuthError("Login failed (HTTP ${resp.code()})")
        saveUsSession(body, email)
    }

    private suspend fun loginCa(email: String, password: String, config: RegionConstants.RegionConfig) {
        val resp = caApi(config.baseUrl).login(
            request = CaLoginRequest(loginId = email, password = password)
        )
        val body = resp.body()
        val result = body?.result
        if (!resp.isSuccessful || result?.accessToken == null)
            throw BluelinkError.AuthError(body?.msgTxt ?: "Login failed (HTTP ${resp.code()})")
        saveCaSession(result, email)
    }

    private suspend fun loginEu(email: String, password: String, config: RegionConstants.RegionConfig) {
        val idp = euIdpApi(config.authBaseUrl)
        val state = java.util.UUID.randomUUID().toString()

        val pageResp = idp.getAuthorizePage(
            state = state,
            clientId = config.clientId,
            redirectUri = config.redirectUri,
        )
        val html = pageResp.body()?.string() ?: ""
        val sessionKey = extractConnectorSessionKey(html)
            ?: throw BluelinkError.AuthError("Could not extract IDP session key")

        val signInResp = idp.signIn(
            clientId = config.clientId,
            username = email,
            password = password,
            connectorSessionKey = sessionKey,
            redirectUri = config.redirectUri,
            state = state,
        )
        val location = signInResp.raw().header("Location")
            ?: signInResp.raw().header("location")
            ?: throw BluelinkError.AuthError("No redirect after sign-in")
        val code = extractQueryParam(location, "code")
            ?: throw BluelinkError.AuthError("No auth code in redirect URL")

        val tokenResp = idp.getToken(
            code = code,
            redirectUri = config.redirectUri,
            basicAuth = okhttp3.Credentials.basic(config.clientId, config.clientSecret),
        )
        val tokenBody = tokenResp.body()
        if (!tokenResp.isSuccessful || tokenBody?.accessToken == null)
            throw BluelinkError.AuthError("Token exchange failed (HTTP ${tokenResp.code()})")
        saveEuSession(tokenBody, email)
    }

    private suspend fun saveUsSession(body: UsLoginResponse, email: String) {
        currentAccessToken = body.accessToken ?: return
        currentRefreshToken = body.refreshToken ?: ""
        currentUserId = body.userId ?: email
        val expiry = body.expiresIn?.toLongOrNull() ?: 3600L
        tokenExpiry = System.currentTimeMillis() + expiry * 1000L
        prefs.saveTokens(currentAccessToken, currentRefreshToken, expiry, currentUserId, email)
    }

    private suspend fun saveCaSession(result: CaLoginResult, email: String) {
        currentAccessToken = result.accessToken ?: return
        currentRefreshToken = result.refreshToken ?: ""
        currentUserId = result.userId ?: email
        val expiry = result.expireIn?.toLongOrNull() ?: 3600L
        tokenExpiry = System.currentTimeMillis() + expiry * 1000L
        prefs.saveTokens(currentAccessToken, currentRefreshToken, expiry, currentUserId, email)
    }

    private suspend fun saveEuSession(body: EuTokenResponse, email: String) {
        currentAccessToken = body.accessToken ?: return
        currentRefreshToken = body.refreshToken ?: ""
        currentUserId = email
        val expiry = body.expiresIn ?: 3600L
        tokenExpiry = System.currentTimeMillis() + expiry * 1000L
        prefs.saveTokens(currentAccessToken, currentRefreshToken, expiry, email, email)
    }

    override suspend fun logout() {
        prefs.clearSession()
        vehicleDao.deleteAll()
        currentAccessToken = ""
        currentRefreshToken = ""
        currentUserId = ""
        currentVehiclePin = ""
        currentEuControlToken = ""
        currentCaPAuth = ""
        prefs.appendLog("Logged out")
    }

    override suspend fun refreshTokens(): Result<Unit> = runCatching {
        ensurePrefsLoaded()
        val config = RegionConstants.forRegion(currentRegion)
        when (currentRegion) {
            BluelinkRegion.US -> {
                val resp = usApi(config.baseUrl).refreshToken(
                    clientId = config.clientId,
                    clientSecret = config.clientSecret,
                    request = UsRefreshRequest(refreshToken = currentRefreshToken),
                )
                val body = resp.body()
                if (!resp.isSuccessful || body?.accessToken == null) throw BluelinkError.TokenExpired()
                saveUsSession(body, currentEmail)
            }
            BluelinkRegion.CA -> {
                val resp = caApi(config.baseUrl).refreshToken(
                    request = CaRefreshRequest(refreshToken = currentRefreshToken)
                )
                val result = resp.body()?.result
                if (!resp.isSuccessful || result?.accessToken == null) throw BluelinkError.TokenExpired()
                saveCaSession(result, currentEmail)
            }
            else -> {
                val resp = euIdpApi(config.authBaseUrl).refreshToken(
                    refreshToken = currentRefreshToken,
                    redirectUri = config.redirectUri,
                    basicAuth = okhttp3.Credentials.basic(config.clientId, config.clientSecret),
                )
                val body = resp.body()
                if (!resp.isSuccessful || body?.accessToken == null) throw BluelinkError.TokenExpired()
                saveEuSession(body, currentEmail)
            }
        }
    }.onFailure { prefs.appendLog("Token refresh failed: ${it.message}") }

    override fun isLoggedIn(): Flow<Boolean> = prefs.isLoggedIn

    // ─── Vehicles ──────────────────────────────────────────────────────────

    override suspend fun getVehicles(): Result<List<Vehicle>> = withAuth {
        val config = RegionConstants.forRegion(currentRegion)
        val vehicles = when (currentRegion) {
            BluelinkRegion.US -> fetchVehiclesUs(config)
            BluelinkRegion.CA -> fetchVehiclesCa(config)
            else -> fetchVehiclesEu(config)
        }
        vehicleDao.insertAll(vehicles.map { it.toEntity() })
        vehicleDao.deleteNotIn(vehicles.map { it.vin })
        prefs.appendLog("Vehicles: ${vehicles.size}")
        vehicles
    }

    private suspend fun fetchVehiclesUs(config: RegionConstants.RegionConfig): List<Vehicle> {
        val resp = usApi(config.baseUrl).getEnrollmentDetails(
            email = currentEmail,
            accessToken = currentAccessToken,
            clientId = config.clientId,
            deviceId = currentDeviceId,
        )
        val body = resp.body()
        if (!resp.isSuccessful || body?.enrolledVehicleDetails == null)
            throw BluelinkError.ApiError(resp.code(), "Failed to fetch vehicles")
        return body.enrolledVehicleDetails.mapNotNull { it.vehicleDetails?.toVehicle(BluelinkRegion.US) }
    }

    private suspend fun fetchVehiclesCa(config: RegionConstants.RegionConfig): List<Vehicle> {
        val resp = caApi(config.baseUrl).getVehicles(
            accessToken = currentAccessToken,
            userId = currentUserId,
            deviceId = currentDeviceId,
        )
        val body = resp.body()
        if (!resp.isSuccessful || body?.result?.vehicles == null)
            throw BluelinkError.ApiError(resp.code(), body?.msgTxt ?: "Failed to fetch vehicles")
        return body.result.vehicles.mapNotNull { it.toVehicle(BluelinkRegion.CA) }
    }

    private suspend fun fetchVehiclesEu(config: RegionConstants.RegionConfig): List<Vehicle> {
        val resp = euApi(config.baseUrl).getVehicles(
            bearerToken = "Bearer $currentAccessToken",
            serviceId = config.serviceId,
            appId = config.appId,
            stamp = euStamp(config),
            deviceId = currentDeviceId,
        )
        val body = resp.body()
        if (!resp.isSuccessful || body?.payload?.vehicles == null)
            throw BluelinkError.ApiError(resp.code(), "Failed to fetch vehicles")
        return body.payload.vehicles.mapNotNull { it.toVehicle(currentRegion) }
    }

    override fun observeVehicles(): Flow<List<Vehicle>> =
        vehicleDao.observeAll().map { it.map { e -> e.toDomain() } }

    // ─── Status ────────────────────────────────────────────────────────────

    override suspend fun getVehicleStatus(
        vin: String,
        forceRefresh: Boolean,
    ): Result<VehicleStatus> = withAuth {
        prefs.appendLog("Status fetch: vin=$vin region=$currentRegion force=$forceRefresh")
        val config = RegionConstants.forRegion(currentRegion)
        val vehicle = vehicleDao.getByVin(vin) ?: throw BluelinkError.VehicleNotFound(vin)
        val status = when (currentRegion) {
            BluelinkRegion.US -> fetchStatusUs(config, vin, vehicle.vehicleId, forceRefresh)
            BluelinkRegion.CA -> fetchStatusCa(config, vin, vehicle.vehicleId, forceRefresh)
            else -> fetchStatusEu(config, vin, vehicle.vehicleId, forceRefresh)
        }
        vehicleStatusDao.insert(status.toEntity())
        prefs.appendLog("Status OK: vin=$vin")
        status
    }.also { result ->
        result.onFailure { prefs.appendLog("Status FAIL: $vin ${it.message}") }
    }

    private suspend fun fetchStatusUs(
        config: RegionConstants.RegionConfig,
        vin: String,
        vehicleId: String,
        forceRefresh: Boolean,
    ): VehicleStatus {
        val api = usApi(config.baseUrl)
        val resp = if (forceRefresh) {
            api.getVehicleStatusRefresh(
                accessToken = currentAccessToken,
                clientId = config.clientId,
                vehicleId = vehicleId,
                vin = vin,
            )
        } else {
            api.getVehicleStatusCached(
                accessToken = currentAccessToken,
                clientId = config.clientId,
                vehicleId = vehicleId,
                vin = vin,
            )
        }
        val body = resp.body()
        if (!resp.isSuccessful || body?.vehicleStatus == null)
            throw BluelinkError.ApiError(resp.code(), "Status fetch failed")
        return body.vehicleStatus.toDomain(vin, moshi)
    }

    private suspend fun fetchStatusCa(
        config: RegionConstants.RegionConfig,
        vin: String,
        vehicleId: String,
        forceRefresh: Boolean,
    ): VehicleStatus {
        val api = caApi(config.baseUrl)
        val resp = if (forceRefresh) {
            api.getVehicleStatusRefresh(
                accessToken = currentAccessToken,
                userId = currentUserId,
                vehicleId = vehicleId,
                deviceId = currentDeviceId,
            )
        } else {
            api.getVehicleStatusCached(
                accessToken = currentAccessToken,
                userId = currentUserId,
                vehicleId = vehicleId,
                deviceId = currentDeviceId,
            )
        }
        val body = resp.body()
        if (!resp.isSuccessful || body?.result?.status == null)
            throw BluelinkError.ApiError(resp.code(), body?.msgTxt ?: "Status fetch failed")
        return body.result.status.toDomain(vin, moshi)
    }

    private suspend fun fetchStatusEu(
        config: RegionConstants.RegionConfig,
        vin: String,
        vehicleId: String,
        forceRefresh: Boolean,
    ): VehicleStatus {
        val api = euApi(config.baseUrl)
        val stamp = euStamp(config)
        val resp = if (forceRefresh) {
            api.getVehicleStatusRefresh(
                vehicleId = vehicleId,
                bearerToken = "Bearer $currentAccessToken",
                serviceId = config.serviceId,
                appId = config.appId,
                stamp = stamp,
                deviceId = currentDeviceId,
            )
        } else {
            api.getVehicleStatusCached(
                vehicleId = vehicleId,
                bearerToken = "Bearer $currentAccessToken",
                serviceId = config.serviceId,
                appId = config.appId,
                stamp = stamp,
                deviceId = currentDeviceId,
            )
        }
        val body = resp.body()
        if (!resp.isSuccessful || body?.payload == null)
            throw BluelinkError.ApiError(resp.code(), "Status fetch failed")
        return body.payload.vehicleStatus?.toDomain(vin, moshi)
            ?: throw BluelinkError.ApiError(resp.code(), "Empty vehicle status")
    }

    override fun observeVehicleStatus(vin: String): Flow<VehicleStatus?> =
        vehicleStatusDao.observe(vin).map { it?.toDomain() }

    // ─── Commands ──────────────────────────────────────────────────────────

    override suspend fun lock(vin: String): Result<CommandResult> = sendCommand(vin, "lock") {
        val (config, vehicle) = configAndVehicle(vin)
        when (currentRegion) {
            BluelinkRegion.US -> usApi(config.baseUrl).lockDoors(
                accessToken = currentAccessToken, clientId = config.clientId,
                vehicleId = vehicle.vehicleId, vin = vin,
                body = UsCommandBody(userName = currentEmail, vin = vin, pin = currentVehiclePin),
            ).toUsResult()
            BluelinkRegion.CA -> {
                val pAuth = ensureCaPAuth(config, vehicle.vehicleId)
                caApi(config.baseUrl).lockDoors(
                    accessToken = currentAccessToken, userId = currentUserId,
                    vehicleId = vehicle.vehicleId, deviceId = currentDeviceId,
                    request = CaLockRequest(pin = currentVehiclePin, pAuth = pAuth),
                ).toCaResult()
            }
            else -> {
                val ct = ensureEuControlToken(config)
                euApi(config.baseUrl).controlDoor(
                    vehicleId = vehicle.vehicleId,
                    controlToken = "Bearer $ct",
                    serviceId = config.serviceId, appId = config.appId,
                    stamp = euStamp(config), deviceId = currentDeviceId,
                    request = EuDoorControlRequest(action = "close", deviceId = currentDeviceId),
                ).toEuResult()
            }
        }
    }

    override suspend fun unlock(vin: String): Result<CommandResult> = sendCommand(vin, "unlock") {
        val (config, vehicle) = configAndVehicle(vin)
        when (currentRegion) {
            BluelinkRegion.US -> usApi(config.baseUrl).unlockDoors(
                accessToken = currentAccessToken, clientId = config.clientId,
                vehicleId = vehicle.vehicleId, vin = vin,
                body = UsCommandBody(userName = currentEmail, vin = vin, pin = currentVehiclePin),
            ).toUsResult()
            BluelinkRegion.CA -> {
                val pAuth = ensureCaPAuth(config, vehicle.vehicleId)
                caApi(config.baseUrl).unlockDoors(
                    accessToken = currentAccessToken, userId = currentUserId,
                    vehicleId = vehicle.vehicleId, deviceId = currentDeviceId,
                    request = CaLockRequest(pin = currentVehiclePin, pAuth = pAuth),
                ).toCaResult()
            }
            else -> {
                val ct = ensureEuControlToken(config)
                euApi(config.baseUrl).controlDoor(
                    vehicleId = vehicle.vehicleId,
                    controlToken = "Bearer $ct",
                    serviceId = config.serviceId, appId = config.appId,
                    stamp = euStamp(config), deviceId = currentDeviceId,
                    request = EuDoorControlRequest(action = "open", deviceId = currentDeviceId),
                ).toEuResult()
            }
        }
    }

    override suspend fun startEngine(vin: String, climate: ClimateSettings?): Result<CommandResult> =
        sendCommand(vin, "startEngine") {
            val (config, vehicle) = configAndVehicle(vin)
            val cs = climate ?: ClimateSettings()
            when (currentRegion) {
                BluelinkRegion.US -> usApi(config.baseUrl).startEngine(
                    accessToken = currentAccessToken, clientId = config.clientId,
                    vehicleId = vehicle.vehicleId, vin = vin,
                    request = cs.toUsClimate(currentEmail, vin),
                ).toUsResult()
                BluelinkRegion.CA -> {
                    val pAuth = ensureCaPAuth(config, vehicle.vehicleId)
                    caApi(config.baseUrl).startEngine(
                        accessToken = currentAccessToken, userId = currentUserId,
                        vehicleId = vehicle.vehicleId, deviceId = currentDeviceId,
                        request = cs.toCaClimate(currentVehiclePin, pAuth),
                    ).toCaResult()
                }
                else -> {
                    val ct = ensureEuControlToken(config)
                    euApi(config.baseUrl).controlClimate(
                        vehicleId = vehicle.vehicleId,
                        controlToken = "Bearer $ct",
                        serviceId = config.serviceId, appId = config.appId,
                        stamp = euStamp(config), deviceId = currentDeviceId,
                        request = cs.toEuClimate("start"),
                    ).toEuResult()
                }
            }
        }

    override suspend fun stopEngine(vin: String): Result<CommandResult> =
        sendCommand(vin, "stopEngine") {
            val (config, vehicle) = configAndVehicle(vin)
            when (currentRegion) {
                BluelinkRegion.US -> usApi(config.baseUrl).stopEngine(
                    accessToken = currentAccessToken, clientId = config.clientId,
                    vehicleId = vehicle.vehicleId, vin = vin,
                    body = UsCommandBody(userName = currentEmail, vin = vin, pin = currentVehiclePin),
                ).toUsResult()
                BluelinkRegion.CA -> {
                    val pAuth = ensureCaPAuth(config, vehicle.vehicleId)
                    caApi(config.baseUrl).stopEngine(
                        accessToken = currentAccessToken, userId = currentUserId,
                        vehicleId = vehicle.vehicleId, deviceId = currentDeviceId,
                        request = CaLockRequest(pin = currentVehiclePin, pAuth = pAuth),
                    ).toCaResult()
                }
                else -> {
                    val ct = ensureEuControlToken(config)
                    euApi(config.baseUrl).controlClimate(
                        vehicleId = vehicle.vehicleId,
                        controlToken = "Bearer $ct",
                        serviceId = config.serviceId, appId = config.appId,
                        stamp = euStamp(config), deviceId = currentDeviceId,
                        request = ClimateSettings().toEuClimate("stop"),
                    ).toEuResult()
                }
            }
        }

    override suspend fun startClimate(vin: String, settings: ClimateSettings): Result<CommandResult> =
        sendCommand(vin, "startClimate") {
            val (config, vehicle) = configAndVehicle(vin)
            when (currentRegion) {
                BluelinkRegion.US -> usApi(config.baseUrl).startClimate(
                    accessToken = currentAccessToken, clientId = config.clientId,
                    vehicleId = vehicle.vehicleId, vin = vin,
                    request = settings.toUsClimate(currentEmail, vin),
                ).toUsResult()
                BluelinkRegion.CA -> {
                    val pAuth = ensureCaPAuth(config, vehicle.vehicleId)
                    caApi(config.baseUrl).startClimate(
                        accessToken = currentAccessToken, userId = currentUserId,
                        vehicleId = vehicle.vehicleId, deviceId = currentDeviceId,
                        request = settings.toCaClimate(currentVehiclePin, pAuth),
                    ).toCaResult()
                }
                else -> {
                    val ct = ensureEuControlToken(config)
                    euApi(config.baseUrl).controlClimate(
                        vehicleId = vehicle.vehicleId,
                        controlToken = "Bearer $ct",
                        serviceId = config.serviceId, appId = config.appId,
                        stamp = euStamp(config), deviceId = currentDeviceId,
                        request = settings.toEuClimate("start"),
                    ).toEuResult()
                }
            }
        }

    override suspend fun stopClimate(vin: String): Result<CommandResult> =
        sendCommand(vin, "stopClimate") {
            val (config, vehicle) = configAndVehicle(vin)
            when (currentRegion) {
                BluelinkRegion.US -> usApi(config.baseUrl).stopClimate(
                    accessToken = currentAccessToken, clientId = config.clientId,
                    vehicleId = vehicle.vehicleId, vin = vin,
                    body = UsCommandBody(userName = currentEmail, vin = vin, pin = currentVehiclePin),
                ).toUsResult()
                BluelinkRegion.CA -> {
                    val pAuth = ensureCaPAuth(config, vehicle.vehicleId)
                    caApi(config.baseUrl).stopClimate(
                        accessToken = currentAccessToken, userId = currentUserId,
                        vehicleId = vehicle.vehicleId, deviceId = currentDeviceId,
                        request = CaLockRequest(pin = currentVehiclePin, pAuth = pAuth),
                    ).toCaResult()
                }
                else -> {
                    val ct = ensureEuControlToken(config)
                    euApi(config.baseUrl).controlClimate(
                        vehicleId = vehicle.vehicleId,
                        controlToken = "Bearer $ct",
                        serviceId = config.serviceId, appId = config.appId,
                        stamp = euStamp(config), deviceId = currentDeviceId,
                        request = ClimateSettings().toEuClimate("stop"),
                    ).toEuResult()
                }
            }
        }

    override suspend fun startCharge(vin: String): Result<CommandResult> =
        sendCommand(vin, "startCharge") {
            val (config, vehicle) = configAndVehicle(vin)
            when (currentRegion) {
                BluelinkRegion.US -> usApi(config.baseUrl).startCharge(
                    accessToken = currentAccessToken, clientId = config.clientId,
                    vehicleId = vehicle.vehicleId, vin = vin,
                    request = UsChargeBody(userName = currentEmail, vin = vin),
                ).toUsResult()
                BluelinkRegion.CA -> {
                    val pAuth = ensureCaPAuth(config, vehicle.vehicleId)
                    caApi(config.baseUrl).startCharge(
                        accessToken = currentAccessToken, userId = currentUserId,
                        vehicleId = vehicle.vehicleId, deviceId = currentDeviceId,
                        request = CaChargeRequest(pin = currentVehiclePin, pAuth = pAuth),
                    ).toCaResult()
                }
                else -> {
                    val ct = ensureEuControlToken(config)
                    euApi(config.baseUrl).controlCharge(
                        vehicleId = vehicle.vehicleId,
                        controlToken = "Bearer $ct",
                        serviceId = config.serviceId, appId = config.appId,
                        stamp = euStamp(config), deviceId = currentDeviceId,
                        request = EuChargeActionRequest(action = "start", deviceId = currentDeviceId),
                    ).toEuResult()
                }
            }
        }

    override suspend fun stopCharge(vin: String): Result<CommandResult> =
        sendCommand(vin, "stopCharge") {
            val (config, vehicle) = configAndVehicle(vin)
            when (currentRegion) {
                BluelinkRegion.US -> usApi(config.baseUrl).stopCharge(
                    accessToken = currentAccessToken, clientId = config.clientId,
                    vehicleId = vehicle.vehicleId, vin = vin,
                    request = UsChargeBody(userName = currentEmail, vin = vin),
                ).toUsResult()
                BluelinkRegion.CA -> {
                    val pAuth = ensureCaPAuth(config, vehicle.vehicleId)
                    caApi(config.baseUrl).stopCharge(
                        accessToken = currentAccessToken, userId = currentUserId,
                        vehicleId = vehicle.vehicleId, deviceId = currentDeviceId,
                        request = CaChargeRequest(pin = currentVehiclePin, pAuth = pAuth),
                    ).toCaResult()
                }
                else -> {
                    val ct = ensureEuControlToken(config)
                    euApi(config.baseUrl).controlCharge(
                        vehicleId = vehicle.vehicleId,
                        controlToken = "Bearer $ct",
                        serviceId = config.serviceId, appId = config.appId,
                        stamp = euStamp(config), deviceId = currentDeviceId,
                        request = EuChargeActionRequest(action = "stop", deviceId = currentDeviceId),
                    ).toEuResult()
                }
            }
        }

    override suspend fun setChargeTarget(
        vin: String,
        percentAc: Int,
        percentDc: Int,
    ): Result<CommandResult> = sendCommand(vin, "setChargeTarget") {
        val (config, vehicle) = configAndVehicle(vin)
        when (currentRegion) {
            BluelinkRegion.US -> usApi(config.baseUrl).setChargeTarget(
                accessToken = currentAccessToken, clientId = config.clientId,
                vehicleId = vehicle.vehicleId, vin = vin,
                request = UsChargeTargetRequest(
                    chargeTargetList = listOf(
                        UsChargeTarget(targetSOClevel = percentAc, plugType = 1),
                        UsChargeTarget(targetSOClevel = percentDc, plugType = 0),
                    )
                ),
            ).toUsResult()
            BluelinkRegion.CA -> {
                val pAuth = ensureCaPAuth(config, vehicle.vehicleId)
                caApi(config.baseUrl).setChargeTarget(
                    accessToken = currentAccessToken, userId = currentUserId,
                    vehicleId = vehicle.vehicleId, deviceId = currentDeviceId,
                    request = CaSetSocRequest(
                        pin = currentVehiclePin, pAuth = pAuth,
                        chargeConfig = CaChargeConfig(
                            targetSOClist = listOf(
                                CaTargetSOC(targetSOClevel = percentAc, plugType = 1),
                                CaTargetSOC(targetSOClevel = percentDc, plugType = 0),
                            )
                        ),
                    ),
                ).toCaResult()
            }
            else -> euApi(config.baseUrl).setChargeTarget(
                vehicleId = vehicle.vehicleId,
                bearerToken = "Bearer $currentAccessToken",
                serviceId = config.serviceId, appId = config.appId,
                stamp = euStamp(config), deviceId = currentDeviceId,
                request = EuChargeTargetRequest(
                    targetSOClist = listOf(
                        EuTargetSOC(targetSOClevel = percentAc, plugType = 1),
                        EuTargetSOC(targetSOClevel = percentDc, plugType = 0),
                    )
                ),
            ).toEuResult()
        }
    }

    override suspend fun flashLights(vin: String): Result<CommandResult> =
        sendCommand(vin, "flashLights") {
            val (config, vehicle) = configAndVehicle(vin)
            when (currentRegion) {
                BluelinkRegion.US -> usApi(config.baseUrl).flashLights(
                    accessToken = currentAccessToken, clientId = config.clientId,
                    vehicleId = vehicle.vehicleId, vin = vin,
                    body = UsCommandBody(userName = currentEmail, vin = vin, pin = currentVehiclePin),
                ).toUsResult()
                BluelinkRegion.CA -> caApi(config.baseUrl).hornAndLights(
                    accessToken = currentAccessToken, userId = currentUserId,
                    vehicleId = vehicle.vehicleId, deviceId = currentDeviceId,
                    request = CaLockRequest(pin = currentVehiclePin),
                ).toCaResult()
                else -> CommandResult(success = false, error = "Not available for this region")
            }
        }

    override suspend fun honkHorn(vin: String): Result<CommandResult> =
        sendCommand(vin, "honkHorn") {
            val (config, vehicle) = configAndVehicle(vin)
            when (currentRegion) {
                BluelinkRegion.US -> usApi(config.baseUrl).honkHorn(
                    accessToken = currentAccessToken, clientId = config.clientId,
                    vehicleId = vehicle.vehicleId, vin = vin,
                    body = UsCommandBody(userName = currentEmail, vin = vin, pin = currentVehiclePin),
                ).toUsResult()
                BluelinkRegion.CA -> caApi(config.baseUrl).hornAndLights(
                    accessToken = currentAccessToken, userId = currentUserId,
                    vehicleId = vehicle.vehicleId, deviceId = currentDeviceId,
                    request = CaLockRequest(pin = currentVehiclePin),
                ).toCaResult()
                else -> CommandResult(success = false, error = "Not available for this region")
            }
        }

    override suspend fun openTrunk(vin: String): Result<CommandResult> =
        Result.success(CommandResult(success = false, error = "Not supported via API"))

    override suspend fun closeTrunk(vin: String): Result<CommandResult> =
        Result.success(CommandResult(success = false, error = "Not supported via API"))

    override suspend fun getLocation(vin: String): Result<Pair<Double, Double>> = withAuth {
        val status = vehicleStatusDao.get(vin)?.toDomain()
            ?: getVehicleStatus(vin).getOrThrow()
        val lat = status.latitude ?: throw BluelinkError.ApiError(0, "Location unavailable")
        val lon = status.longitude ?: throw BluelinkError.ApiError(0, "Location unavailable")
        lat to lon
    }

    override suspend fun setCustomImage(vin: String, uri: String?) {
        vehicleDao.updateCustomImage(vin, uri)
    }

    // ─── Internal helpers ─────────────────────────────────────────────────

    private suspend fun ensurePrefsLoaded() {
        if (currentAccessToken.isEmpty()) {
            prefs.accessToken.first()?.let { currentAccessToken = it }
            prefs.refreshToken.first()?.let { currentRefreshToken = it }
            prefs.userId.first()?.let { currentUserId = it }
            prefs.userEmail.first()?.let { currentEmail = it }
            prefs.deviceId.first()?.let { currentDeviceId = it }
            prefs.tokenExpiry.first().let { tokenExpiry = it }
            currentRegion = prefs.region.first()
        }
        if (currentVehiclePin.isEmpty()) {
            when (currentRegion) {
                BluelinkRegion.US -> prefs.getUsPinOnce()?.let { currentVehiclePin = it }
                BluelinkRegion.CA -> prefs.getCaPinOnce()?.let { currentVehiclePin = it }
                BluelinkRegion.EU, BluelinkRegion.AU, BluelinkRegion.ME ->
                    prefs.getEuPinOnce()?.let { currentVehiclePin = it }
            }
        }
        if (currentEuControlToken.isEmpty()) {
            val (token, expiry) = prefs.getEuControlTokenOnce()
            if (token != null) { currentEuControlToken = token; euControlTokenExpiry = expiry }
        }
    }

    private suspend fun ensureValidToken() {
        ensurePrefsLoaded()
        if (System.currentTimeMillis() >= tokenExpiry - 60_000) {
            refreshTokens().getOrElse { throw BluelinkError.TokenExpired() }
        }
    }

    private suspend fun <T> withAuth(block: suspend () -> T): Result<T> = runCatching {
        ensureValidToken()
        block()
    }.recoverCatching { ex ->
        if (ex is BluelinkError.TokenExpired || (ex is BluelinkError.ApiError && ex.code == 401)) {
            refreshTokens().getOrElse { throw ex }
            block()
        } else throw ex
    }

    private suspend fun <T : CommandResult> sendCommand(
        vin: String,
        name: String,
        block: suspend () -> T,
    ): Result<T> = withAuth {
        prefs.appendLog("CMD $name [$vin]")
        val result = block()
        prefs.appendLog("CMD $name done: success=${result.success} err=${result.error}")
        result
    }

    private suspend fun configAndVehicle(vin: String): Pair<RegionConstants.RegionConfig, VehicleEntity> {
        val config = RegionConstants.forRegion(currentRegion)
        val vehicle = vehicleDao.getByVin(vin) ?: throw BluelinkError.VehicleNotFound(vin)
        return config to vehicle
    }

    private suspend fun ensureCaPAuth(config: RegionConstants.RegionConfig, vehicleId: String): String {
        if (currentCaPAuth.isNotEmpty() && System.currentTimeMillis() < caPAuthExpiry - 30_000) {
            return currentCaPAuth
        }
        val pin = currentVehiclePin.ifEmpty {
            prefs.getCaPinOnce() ?: throw BluelinkError.AuthError("CA PIN not configured — log in again")
        }
        currentVehiclePin = pin
        val resp = caApi(config.baseUrl).verifyPin(
            accessToken = currentAccessToken,
            userId = currentUserId,
            vehicleId = vehicleId,
            deviceId = currentDeviceId,
            request = CaPinVerifyRequest(pin = pin),
        )
        val pAuth = resp.body()?.result?.pAuth
        if (!resp.isSuccessful || pAuth == null)
            throw BluelinkError.AuthError(resp.body()?.msgTxt ?: "PIN verification failed")
        currentCaPAuth = pAuth
        caPAuthExpiry = System.currentTimeMillis() + 5 * 60_000L
        return pAuth
    }

    private suspend fun ensureEuControlToken(config: RegionConstants.RegionConfig): String {
        if (currentEuControlToken.isNotEmpty() && System.currentTimeMillis() < euControlTokenExpiry - 30_000) {
            return currentEuControlToken
        }
        val pin = currentVehiclePin.ifEmpty {
            prefs.getEuPinOnce() ?: throw BluelinkError.AuthError("Vehicle PIN not configured — log in again")
        }
        currentVehiclePin = pin
        val resp = euApi(config.baseUrl).verifyPin(
            bearerToken = "Bearer $currentAccessToken",
            serviceId = config.serviceId,
            appId = config.appId,
            stamp = euStamp(config),
            deviceId = currentDeviceId,
            request = EuPinRequest(deviceId = currentDeviceId, pin = pin),
        )
        val ct = resp.body()?.payload?.controlToken
        if (!resp.isSuccessful || ct == null)
            throw BluelinkError.AuthError("EU PIN verification failed (HTTP ${resp.code()})")
        currentEuControlToken = ct
        euControlTokenExpiry = System.currentTimeMillis() + ((resp.body()?.payload?.expiresTime ?: 600L) * 1000L)
        prefs.saveEuControlToken(ct, resp.body()?.payload?.expiresTime ?: 600L)
        return ct
    }

    private fun euStamp(config: RegionConstants.RegionConfig): String =
        RegionConstants.generateStamp(config.appId, config.cfbBlob)

    private fun extractConnectorSessionKey(html: String): String? =
        Regex("""name=["']connector_session_key["'][^>]*value=["']([^"']+)["']""")
            .find(html)?.groupValues?.getOrNull(1)
            ?: Regex("""connector_session_key["']\s*:\s*["']([^"']+)""")
            .find(html)?.groupValues?.getOrNull(1)

    private fun extractQueryParam(url: String, param: String): String? = try {
        android.net.Uri.parse(url).getQueryParameter(param)
    } catch (_: Exception) { null }
}

// ─── Response → CommandResult ─────────────────────────────────────────────────

private fun retrofit2.Response<UsCommandResponse>.toUsResult(): CommandResult {
    val body = body()
    val ok = isSuccessful && (body?.responseHeader?.responseCode == 0 || body?.resultCode == "S")
    return CommandResult(
        success = ok,
        transactionId = body?.payloadBody?.transactionId,
        message = body?.responseHeader?.responseDesc,
        error = if (!ok) body?.errorMessage ?: "HTTP ${code()}" else null,
    )
}

private fun retrofit2.Response<CaCommandResponse>.toCaResult(): CommandResult {
    val body = body()
    val ok = isSuccessful && body?.retCode == "S"
    return CommandResult(
        success = ok,
        transactionId = body?.result?.transactionId,
        message = body?.msgTxt,
        error = if (!ok) body?.msgTxt ?: "HTTP ${code()}" else null,
    )
}

private fun retrofit2.Response<EuCommandResponse>.toEuResult(): CommandResult {
    val body = body()
    return CommandResult(
        success = isSuccessful,
        transactionId = body?.payload?.transactionId,
        message = body?.message?.description,
        error = if (!isSuccessful) "HTTP ${code()}" else null,
    )
}

// ─── Domain Mappers ───────────────────────────────────────────────────────────

private fun UsVehicleDetails.toVehicle(region: BluelinkRegion) = vin?.let {
    Vehicle(
        vin = it, vehicleId = vehicleIdentifier ?: it,
        nickname = nickName ?: modelName ?: it,
        modelName = modelName ?: "", modelYear = modelYear ?: "",
        modelCode = modelCode ?: "", licensePlate = licensePlate ?: "",
        fuelType = parseFuelType(fuelType), region = region,
        generation = generation?.toIntOrNull() ?: 5,
    )
}

private fun CaVehicle.toVehicle(region: BluelinkRegion) = vin?.let {
    Vehicle(
        vin = it, vehicleId = vehicleId ?: it,
        nickname = nickName ?: modelName ?: it,
        modelName = modelName ?: "", modelYear = modelYear ?: "",
        modelCode = modelCode ?: "", licensePlate = licensePlate ?: "",
        fuelType = parseFuelType(fuelType), region = region,
    )
}

private fun EuVehicle.toVehicle(region: BluelinkRegion) = vin?.let {
    Vehicle(
        vin = it, vehicleId = vehicleId ?: it,
        nickname = nickName ?: modelName ?: it,
        modelName = modelName ?: "", modelYear = modelYear ?: "",
        modelCode = modelCode ?: "", licensePlate = licensePlate ?: "",
        fuelType = parseFuelType(fuelType), region = region,
        generation = generation ?: 5,
    )
}

private fun parseFuelType(s: String?): FuelType = when (s?.uppercase()) {
    "E", "EV", "ELECTRIC", "BEV" -> FuelType.ELECTRIC
    "H", "HEV", "HYBRID" -> FuelType.HYBRID
    "P", "PHEV" -> FuelType.PHEV
    else -> FuelType.GASOLINE
}

// ─── Climate builders ─────────────────────────────────────────────────────────

private fun ClimateSettings.toUsClimate(username: String, vin: String) = UsClimateRequest(
    airTemp = UsClimateTemp(value = temperatureC.toString()),
    defrost = defrostFront,
    igniOnDuration = durationMinutes,
    username = username,
    vin = vin,
    seatHeaterVentInfo = UsSeatHeatRequest(
        driverSeatHeatState = seatHeatFrontLeft.ordinal,
        passengerSeatHeatState = seatHeatFrontRight.ordinal,
        rearLeftSeatHeatState = seatHeatRearLeft.ordinal,
        rearRightSeatHeatState = seatHeatRearRight.ordinal,
    ),
)

private fun ClimateSettings.toCaClimate(pin: String, pAuth: String) = CaClimateRequest(
    pin = pin,
    pAuth = pAuth,
    tempCode = RegionConstants.celsiusToTempCode(temperatureC),
    options = CaClimateOptions(defrost = defrostFront),
)

private fun ClimateSettings.toEuClimate(action: String) = EuClimateRequest(
    action = action,
    tempCode = RegionConstants.celsiusToTempCode(temperatureC),
    options = EuClimateOptions(
        defrost = defrostFront,
        rearWindowHeat = if (defrostRear) 1 else 0,
        steeringWheel = if (steeringWheelHeat) 1 else 0,
        heating1 = seatHeatFrontLeft.ordinal,
    ),
)

// ─── Status domain mappers ────────────────────────────────────────────────────

private fun UsVehicleStatus.toDomain(vin: String, moshi: Moshi): VehicleStatus {
    val drvDist = evStatus?.drvDistance?.firstOrNull { it.type == 2 } ?: evStatus?.drvDistance?.firstOrNull()
    val evRange = drvDist?.rangeByFuel?.evModeRange?.value
        ?.let { if (drvDist.rangeByFuel?.evModeRange?.unit == 1) it * 1.60934 else it }
    val fuelRange = dte?.value?.let { if (dte.unit == 1) it * 1.60934 else it }
    return VehicleStatus(
        vin = vin,
        isLocked = doorLock ?: true,
        doorFrontLeft = if (doorOpen?.frontLeft == 1) DoorState.OPEN else DoorState.CLOSED,
        doorFrontRight = if (doorOpen?.frontRight == 1) DoorState.OPEN else DoorState.CLOSED,
        doorRearLeft = if (doorOpen?.backLeft == 1) DoorState.OPEN else DoorState.CLOSED,
        doorRearRight = if (doorOpen?.backRight == 1) DoorState.OPEN else DoorState.CLOSED,
        trunkOpen = trunkOpen ?: false,
        hoodOpen = hoodOpen ?: false,
        engineRunning = engine ?: false,
        evBatteryPercent = evStatus?.batteryStatus,
        evRangeKm = evRange,
        evCharging = evStatus?.batteryPlugin?.let { it > 1 } ?: false,
        evPluggedIn = evStatus?.batteryPlugin?.let { it > 0 } ?: false,
        evChargeTargetPercent = evStatus?.targetSOClist?.firstOrNull()?.targetSOClevel,
        estimatedChargingMinutes = evStatus?.remainChargeTime?.firstOrNull()?.timeInterval?.value,
        fuelRangeKm = fuelRange,
        climateOn = climate?.airCtrl ?: false,
        targetTempC = climate?.airTemp?.value?.toDoubleOrNull(),
        defrostFront = climate?.defrost ?: false,
        seatHeatFrontLeft = (climate?.seatHeaterVentInfo?.driverSeatHeatState ?: 0).toSeatLevel(),
        seatHeatFrontRight = (climate?.seatHeaterVentInfo?.passengerSeatHeatState ?: 0).toSeatLevel(),
        seatHeatRearLeft = (climate?.seatHeaterVentInfo?.rearLeftSeatHeatState ?: 0).toSeatLevel(),
        seatHeatRearRight = (climate?.seatHeaterVentInfo?.rearRightSeatHeatState ?: 0).toSeatLevel(),
        steeringWheelHeat = (climate?.steeringwheel ?: 0) > 0,
        latitude = latitude,
        longitude = longitude,
        heading = heading,
        speed = speed?.value?.let { if (speed.unit == 1) it * 1.60934 else it },
        odometer = odometer?.value?.let { if (odometer.unit == 1) it * 1.60934 else it },
        tirePressureFrontLeft = tirePressure?.frontLeft,
        tirePressureFrontRight = tirePressure?.frontRight,
        tirePressureRearLeft = tirePressure?.backLeft,
        tirePressureRearRight = tirePressure?.backRight,
        tirePressureWarning = tirePressure?.all?.let { it > 0 } ?: false,
        rawJson = moshi.adapter(UsVehicleStatus::class.java).toJson(this),
    )
}

private fun CaVehicleStatus.toDomain(vin: String, moshi: Moshi): VehicleStatus {
    return VehicleStatus(
        vin = vin,
        isLocked = doorLock ?: true,
        doorFrontLeft = if (doorOpen?.frontLeft == 1) DoorState.OPEN else DoorState.CLOSED,
        doorFrontRight = if (doorOpen?.frontRight == 1) DoorState.OPEN else DoorState.CLOSED,
        doorRearLeft = if (doorOpen?.backLeft == 1) DoorState.OPEN else DoorState.CLOSED,
        doorRearRight = if (doorOpen?.backRight == 1) DoorState.OPEN else DoorState.CLOSED,
        trunkOpen = trunkOpen ?: false,
        hoodOpen = hoodOpen ?: false,
        engineRunning = engine ?: false,
        evBatteryPercent = evStatus?.batteryStatus,
        evRangeKm = evStatus?.drvDistance?.let { if (evStatus.drvDistanceUnit == 1) it * 1.60934 else it },
        evCharging = evStatus?.batteryPlugin == true,
        evPluggedIn = evStatus?.batteryPlugin == true,
        fuelLevelPercent = fuelLevel,
        fuelRangeKm = dte,
        climateOn = climate?.airCtrl ?: false,
        targetTempC = climate?.airTemp?.value?.toDoubleOrNull(),
        defrostFront = climate?.defrost ?: false,
        odometer = odometer,
        tirePressureWarning = tirePressureLamp?.tirePressureLampAll?.let { it > 0 } ?: false,
        tirePressureFrontLeft = tirePressureLamp?.tirePressureLampFL,
        tirePressureFrontRight = tirePressureLamp?.tirePressureLampFR,
        tirePressureRearLeft = tirePressureLamp?.tirePressureLampRL,
        tirePressureRearRight = tirePressureLamp?.tirePressureLampRR,
        lowWasherFluid = washerFluidStatus ?: false,
        rawJson = moshi.adapter(CaVehicleStatus::class.java).toJson(this),
    )
}

private fun EuStatusDetails.toDomain(vin: String, moshi: Moshi): VehicleStatus {
    val evRange = evStatus?.drvDistance?.let { if (evStatus.drvDistanceUnit == 1) it * 1.60934 else it }
    val fuelRange = dteMax?.value?.let { if (dteMax.unit == 1) it * 1.60934 else it }
    return VehicleStatus(
        vin = vin,
        isLocked = doorLock ?: true,
        doorFrontLeft = if (doorOpen?.frontLeft == 1) DoorState.OPEN else DoorState.CLOSED,
        doorFrontRight = if (doorOpen?.frontRight == 1) DoorState.OPEN else DoorState.CLOSED,
        doorRearLeft = if (doorOpen?.backLeft == 1) DoorState.OPEN else DoorState.CLOSED,
        doorRearRight = if (doorOpen?.backRight == 1) DoorState.OPEN else DoorState.CLOSED,
        trunkOpen = trunkOpen ?: false,
        hoodOpen = hoodOpen ?: false,
        engineRunning = engine ?: false,
        evBatteryPercent = evStatus?.batteryStatus,
        evRangeKm = evRange,
        evCharging = evStatus?.batteryPlugin?.let { it > 1 } ?: false,
        evPluggedIn = evStatus?.batteryPlugin?.let { it > 0 } ?: false,
        evChargeTargetPercent = evStatus?.targetSOC?.firstOrNull()?.targetSOClevel,
        estimatedChargingMinutes = evStatus?.remainChargeTime,
        fuelLevelPercent = fuelLevel,
        fuelRangeKm = fuelRange,
        climateOn = climate?.airCtrl ?: false,
        targetTempC = climate?.airTemp?.value?.toDoubleOrNull(),
        defrostFront = climate?.defrost ?: false,
        defrostRear = climate?.rearDefrost ?: false,
        seatHeatFrontLeft = (climate?.seatHeaterVentInfo?.driverSeatHeatState ?: 0).toSeatLevel(),
        seatHeatFrontRight = (climate?.seatHeaterVentInfo?.passengerSeatHeatState ?: 0).toSeatLevel(),
        seatHeatRearLeft = (climate?.seatHeaterVentInfo?.rearLeftSeatHeatState ?: 0).toSeatLevel(),
        seatHeatRearRight = (climate?.seatHeaterVentInfo?.rearRightSeatHeatState ?: 0).toSeatLevel(),
        steeringWheelHeat = (climate?.steeringwheel ?: 0) > 0,
        latitude = latitude,
        longitude = longitude,
        heading = heading,
        speed = speed?.value?.let { if (speed.unit == 1) it * 1.60934 else it },
        odometer = odometer?.value?.let { if (odometer.unit == 1) it * 1.60934 else it },
        tirePressureFrontLeft = tirePressure?.frontLeft,
        tirePressureFrontRight = tirePressure?.frontRight,
        tirePressureRearLeft = tirePressure?.backLeft,
        tirePressureRearRight = tirePressure?.backRight,
        tirePressureWarning = tirePressure?.all?.let { it > 0 } ?: false,
        batteryVoltage = battery?.voltage,
        rawJson = moshi.adapter(EuStatusDetails::class.java).toJson(this),
    )
}

private fun Int.toSeatLevel(): SeatHeatingLevel = when (this) {
    1 -> SeatHeatingLevel.LOW
    2 -> SeatHeatingLevel.MEDIUM
    3 -> SeatHeatingLevel.HIGH
    else -> SeatHeatingLevel.OFF
}
