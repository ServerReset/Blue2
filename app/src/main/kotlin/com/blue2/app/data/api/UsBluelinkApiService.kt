package com.blue2.app.data.api

import com.blue2.app.data.api.models.*
import retrofit2.Response
import retrofit2.http.*

// US Hyundai Bluelink — CCNC API
// Base URL: https://api.telematics.hyundaiusa.com/
interface UsBluelinkApiService {

    // ─── Auth ─────────────────────────────────────────────────────────────

    @POST("v2/ac/oauth/token")
    suspend fun login(
        @Header("client_id") clientId: String,
        @Header("client_secret") clientSecret: String,
        @Body request: UsLoginRequest,
    ): Response<UsLoginResponse>

    @POST("v2/ac/oauth/token/refresh")
    suspend fun refreshToken(
        @Header("client_id") clientId: String,
        @Header("client_secret") clientSecret: String,
        @Body request: UsRefreshRequest,
    ): Response<UsLoginResponse>

    // ─── Vehicles ──────────────────────────────────────────────────────────

    @GET("ac/v2/enrollment/details/{email}")
    suspend fun getEnrollmentDetails(
        @Path("email") email: String,
        @Header("access_token") accessToken: String,
        @Header("client_id") clientId: String,
        @Header("deviceId") deviceId: String,
    ): Response<UsEnrollmentResponse>

    // ─── Vehicle Status ────────────────────────────────────────────────────

    // Cached status (fast)
    @GET("ac/v2/rcs/rvs/vehicleStatus")
    suspend fun getVehicleStatusCached(
        @Header("access_token") accessToken: String,
        @Header("client_id") clientId: String,
        @Header("vehicleId") vehicleId: String,
        @Header("VIN") vin: String,
    ): Response<UsVehicleStatusResponse>

    // Live/forced refresh status
    @GET("ac/v2/rcs/rvs/vehicleStatus")
    suspend fun getVehicleStatusRefresh(
        @Header("access_token") accessToken: String,
        @Header("client_id") clientId: String,
        @Header("vehicleId") vehicleId: String,
        @Header("VIN") vin: String,
        @Query("REFRESH") refresh: Boolean = true,
    ): Response<UsVehicleStatusResponse>

    // ─── Lock / Unlock ────────────────────────────────────────────────────

    @POST("ac/v2/rcs/rdo/off")
    suspend fun lockDoors(
        @Header("access_token") accessToken: String,
        @Header("client_id") clientId: String,
        @Header("vehicleId") vehicleId: String,
        @Header("VIN") vin: String,
        @Body body: UsCommandBody,
    ): Response<UsCommandResponse>

    @POST("ac/v2/rcs/rdo/on")
    suspend fun unlockDoors(
        @Header("access_token") accessToken: String,
        @Header("client_id") clientId: String,
        @Header("vehicleId") vehicleId: String,
        @Header("VIN") vin: String,
        @Body body: UsCommandBody,
    ): Response<UsCommandResponse>

    // ─── Engine ───────────────────────────────────────────────────────────

    @POST("ac/v2/rcs/rsc/start")
    suspend fun startEngine(
        @Header("access_token") accessToken: String,
        @Header("client_id") clientId: String,
        @Header("vehicleId") vehicleId: String,
        @Header("VIN") vin: String,
        @Body request: UsClimateRequest,
    ): Response<UsCommandResponse>

    @POST("ac/v2/rcs/rsc/stop")
    suspend fun stopEngine(
        @Header("access_token") accessToken: String,
        @Header("client_id") clientId: String,
        @Header("vehicleId") vehicleId: String,
        @Header("VIN") vin: String,
        @Body body: UsCommandBody,
    ): Response<UsCommandResponse>

    // ─── EV Climate ───────────────────────────────────────────────────────

    @POST("ac/v2/evc/fatc/start")
    suspend fun startClimate(
        @Header("access_token") accessToken: String,
        @Header("client_id") clientId: String,
        @Header("vehicleId") vehicleId: String,
        @Header("VIN") vin: String,
        @Body request: UsClimateRequest,
    ): Response<UsCommandResponse>

    @POST("ac/v2/evc/fatc/stop")
    suspend fun stopClimate(
        @Header("access_token") accessToken: String,
        @Header("client_id") clientId: String,
        @Header("vehicleId") vehicleId: String,
        @Header("VIN") vin: String,
        @Body body: UsCommandBody,
    ): Response<UsCommandResponse>

    // ─── EV Charge ────────────────────────────────────────────────────────

    @POST("ac/v2/evc/charge/start")
    suspend fun startCharge(
        @Header("access_token") accessToken: String,
        @Header("client_id") clientId: String,
        @Header("vehicleId") vehicleId: String,
        @Header("VIN") vin: String,
        @Body request: UsChargeBody,
    ): Response<UsCommandResponse>

    @POST("ac/v2/evc/charge/stop")
    suspend fun stopCharge(
        @Header("access_token") accessToken: String,
        @Header("client_id") clientId: String,
        @Header("vehicleId") vehicleId: String,
        @Header("VIN") vin: String,
        @Body request: UsChargeBody,
    ): Response<UsCommandResponse>

    @PUT("ac/v2/evc/soc")
    suspend fun setChargeTarget(
        @Header("access_token") accessToken: String,
        @Header("client_id") clientId: String,
        @Header("vehicleId") vehicleId: String,
        @Header("VIN") vin: String,
        @Body request: UsChargeTargetRequest,
    ): Response<UsCommandResponse>

    // ─── Horn + Lights ────────────────────────────────────────────────────

    @POST("ac/v2/rcs/rfc/horn")
    suspend fun honkHorn(
        @Header("access_token") accessToken: String,
        @Header("client_id") clientId: String,
        @Header("vehicleId") vehicleId: String,
        @Header("VIN") vin: String,
        @Body body: UsCommandBody,
    ): Response<UsCommandResponse>

    @POST("ac/v2/rcs/rfc/light")
    suspend fun flashLights(
        @Header("access_token") accessToken: String,
        @Header("client_id") clientId: String,
        @Header("vehicleId") vehicleId: String,
        @Header("VIN") vin: String,
        @Body body: UsCommandBody,
    ): Response<UsCommandResponse>
}
