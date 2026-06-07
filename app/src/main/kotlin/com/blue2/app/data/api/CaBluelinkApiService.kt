package com.blue2.app.data.api

import com.blue2.app.data.api.models.*
import retrofit2.Response
import retrofit2.http.*

// Canada Hyundai Bluelink — CCNC API
// Base URL: https://mybluelink.ca/
interface CaBluelinkApiService {

    @POST("tods/api/lgn")
    suspend fun login(
        @Header("from") from: String = "SPA",
        @Header("language") language: Int = 0,
        @Header("offset") offset: Int = 0,
        @Body request: CaLoginRequest,
    ): Response<CaLoginResponse>

    @POST("tods/api/lgn")
    suspend fun refreshToken(
        @Header("from") from: String = "SPA",
        @Header("language") language: Int = 0,
        @Body request: CaRefreshRequest,
    ): Response<CaLoginResponse>

    @POST("tods/api/lgout")
    suspend fun logout(
        @Header("accessToken") accessToken: String,
        @Header("userId") userId: String,
        @Header("vehicleId") vehicleId: String,
    ): Response<CaCommandResponse>

    // PIN verification — returns pAuth token required for commands
    @POST("tods/api/vrfypin")
    suspend fun verifyPin(
        @Header("accessToken") accessToken: String,
        @Header("userId") userId: String,
        @Header("vehicleId") vehicleId: String,
        @Header("deviceId") deviceId: String,
        @Body request: CaPinVerifyRequest,
    ): Response<CaPinVerifyResponse>

    @POST("tods/api/vhcllst")
    suspend fun getVehicles(
        @Header("accessToken") accessToken: String,
        @Header("userId") userId: String,
        @Header("deviceId") deviceId: String,
    ): Response<CaVehicleListResponse>

    @POST("tods/api/lstvhclsts")
    suspend fun getVehicleStatusCached(
        @Header("accessToken") accessToken: String,
        @Header("userId") userId: String,
        @Header("vehicleId") vehicleId: String,
        @Header("deviceId") deviceId: String,
        @Body body: Map<String, String> = emptyMap(),
    ): Response<CaVehicleStatusResponse>

    @POST("tods/api/rltmvhclsts")
    suspend fun getVehicleStatusRefresh(
        @Header("accessToken") accessToken: String,
        @Header("userId") userId: String,
        @Header("vehicleId") vehicleId: String,
        @Header("deviceId") deviceId: String,
        @Body body: Map<String, String> = emptyMap(),
    ): Response<CaVehicleStatusResponse>

    @POST("tods/api/drlck")
    suspend fun lockDoors(
        @Header("accessToken") accessToken: String,
        @Header("userId") userId: String,
        @Header("vehicleId") vehicleId: String,
        @Header("deviceId") deviceId: String,
        @Body request: CaLockRequest,
    ): Response<CaCommandResponse>

    @POST("tods/api/drulck")
    suspend fun unlockDoors(
        @Header("accessToken") accessToken: String,
        @Header("userId") userId: String,
        @Header("vehicleId") vehicleId: String,
        @Header("deviceId") deviceId: String,
        @Body request: CaLockRequest,
    ): Response<CaCommandResponse>

    @POST("tods/api/evc/rfon")
    suspend fun startEngine(
        @Header("accessToken") accessToken: String,
        @Header("userId") userId: String,
        @Header("vehicleId") vehicleId: String,
        @Header("deviceId") deviceId: String,
        @Body request: CaClimateRequest,
    ): Response<CaCommandResponse>

    @POST("tods/api/evc/rfoff")
    suspend fun stopEngine(
        @Header("accessToken") accessToken: String,
        @Header("userId") userId: String,
        @Header("vehicleId") vehicleId: String,
        @Header("deviceId") deviceId: String,
        @Body request: CaLockRequest,
    ): Response<CaCommandResponse>

    @POST("tods/api/evc/rfon")
    suspend fun startClimate(
        @Header("accessToken") accessToken: String,
        @Header("userId") userId: String,
        @Header("vehicleId") vehicleId: String,
        @Header("deviceId") deviceId: String,
        @Body request: CaClimateRequest,
    ): Response<CaCommandResponse>

    @POST("tods/api/evc/rfoff")
    suspend fun stopClimate(
        @Header("accessToken") accessToken: String,
        @Header("userId") userId: String,
        @Header("vehicleId") vehicleId: String,
        @Header("deviceId") deviceId: String,
        @Body request: CaLockRequest,
    ): Response<CaCommandResponse>

    @POST("tods/api/evc/rcstrt")
    suspend fun startCharge(
        @Header("accessToken") accessToken: String,
        @Header("userId") userId: String,
        @Header("vehicleId") vehicleId: String,
        @Header("deviceId") deviceId: String,
        @Body request: CaChargeRequest,
    ): Response<CaCommandResponse>

    @POST("tods/api/evc/rcstp")
    suspend fun stopCharge(
        @Header("accessToken") accessToken: String,
        @Header("userId") userId: String,
        @Header("vehicleId") vehicleId: String,
        @Header("deviceId") deviceId: String,
        @Body request: CaChargeRequest,
    ): Response<CaCommandResponse>

    @POST("tods/api/evc/setsoc")
    suspend fun setChargeTarget(
        @Header("accessToken") accessToken: String,
        @Header("userId") userId: String,
        @Header("vehicleId") vehicleId: String,
        @Header("deviceId") deviceId: String,
        @Body request: CaSetSocRequest,
    ): Response<CaCommandResponse>

    @POST("tods/api/hornlight")
    suspend fun hornAndLights(
        @Header("accessToken") accessToken: String,
        @Header("userId") userId: String,
        @Header("vehicleId") vehicleId: String,
        @Header("deviceId") deviceId: String,
        @Body request: CaLockRequest,
    ): Response<CaCommandResponse>

    @POST("tods/api/fndmcr")
    suspend fun findMyCar(
        @Header("accessToken") accessToken: String,
        @Header("userId") userId: String,
        @Header("vehicleId") vehicleId: String,
        @Header("deviceId") deviceId: String,
        @Body request: CaLockRequest,
    ): Response<CaCommandResponse>

    @POST("tods/api/rmtsts")
    suspend fun getCommandStatus(
        @Header("accessToken") accessToken: String,
        @Header("userId") userId: String,
        @Header("vehicleId") vehicleId: String,
        @Header("deviceId") deviceId: String,
        @Body body: Map<String, String>,
    ): Response<CaCommandStatusResponse>
}
