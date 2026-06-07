package com.blue2.app.data.api

import com.blue2.app.data.api.models.*
import retrofit2.Response
import retrofit2.http.*

// US Hyundai Bluelink — CCNC API (api.telematics.hyundaiusa.com)
// Headers sourced from hyundai_kia_connect_api open-source project
interface UsBluelinkApiService {

    // ─── Auth ─────────────────────────────────────────────────────────────

    @Headers("from: SPA", "to: ISS", "language: 0", "encryptFlag: false", "brandIndicator: H")
    @POST("v2/ac/oauth/token")
    suspend fun login(
        @Header("client_id") clientId: String,
        @Header("clientSecret") clientSecret: String,
        @Body request: UsLoginRequest,
    ): Response<UsLoginResponse>

    // ─── Vehicles ──────────────────────────────────────────────────────────

    @Headers("from: SPA", "to: ISS", "language: 0", "encryptFlag: false", "brandIndicator: H")
    @GET("ac/v2/enrollment/details/{email}")
    suspend fun getEnrollmentDetails(
        @Path("email") email: String,
        @Header("accessToken") accessToken: String,
        @Header("client_id") clientId: String,
        @Header("clientSecret") clientSecret: String,
        @Header("blueLinkServicePin") pin: String,
        @Header("username") username: String,
        @Header("offset") offset: String,
    ): Response<UsEnrollmentResponse>

    // ─── Vehicle Status ────────────────────────────────────────────────────

    @Headers("from: SPA", "to: ISS", "language: 0", "encryptFlag: false", "brandIndicator: H")
    @GET("ac/v2/rcs/rvs/vehicleStatus")
    suspend fun getVehicleStatus(
        @Header("accessToken") accessToken: String,
        @Header("client_id") clientId: String,
        @Header("clientSecret") clientSecret: String,
        @Header("blueLinkServicePin") pin: String,
        @Header("username") username: String,
        @Header("registrationId") registrationId: String,
        @Header("gen") gen: String,
        @Header("vin") vin: String,
        @Header("offset") offset: String,
        @Header("REFRESH") refresh: String = "false",
    ): Response<UsVehicleStatusResponse>

    // ─── Lock / Unlock ────────────────────────────────────────────────────

    @Headers("from: SPA", "to: ISS", "language: 0", "encryptFlag: false", "brandIndicator: H")
    @POST("ac/v2/rcs/rdo/off")
    suspend fun lockDoors(
        @Header("accessToken") accessToken: String,
        @Header("client_id") clientId: String,
        @Header("clientSecret") clientSecret: String,
        @Header("blueLinkServicePin") pin: String,
        @Header("username") username: String,
        @Header("registrationId") registrationId: String,
        @Header("gen") gen: String,
        @Header("vin") vin: String,
        @Header("APPCLOUD-VIN") appcloudVin: String,
        @Header("offset") offset: String,
        @Body body: UsCommandBody,
    ): Response<UsCommandResponse>

    @Headers("from: SPA", "to: ISS", "language: 0", "encryptFlag: false", "brandIndicator: H")
    @POST("ac/v2/rcs/rdo/on")
    suspend fun unlockDoors(
        @Header("accessToken") accessToken: String,
        @Header("client_id") clientId: String,
        @Header("clientSecret") clientSecret: String,
        @Header("blueLinkServicePin") pin: String,
        @Header("username") username: String,
        @Header("registrationId") registrationId: String,
        @Header("gen") gen: String,
        @Header("vin") vin: String,
        @Header("APPCLOUD-VIN") appcloudVin: String,
        @Header("offset") offset: String,
        @Body body: UsCommandBody,
    ): Response<UsCommandResponse>

    // ─── Engine (ICE) ─────────────────────────────────────────────────────

    @Headers("from: SPA", "to: ISS", "language: 0", "encryptFlag: false", "brandIndicator: H")
    @POST("ac/v2/rcs/rsc/start")
    suspend fun startEngine(
        @Header("accessToken") accessToken: String,
        @Header("client_id") clientId: String,
        @Header("clientSecret") clientSecret: String,
        @Header("blueLinkServicePin") pin: String,
        @Header("username") username: String,
        @Header("registrationId") registrationId: String,
        @Header("gen") gen: String,
        @Header("vin") vin: String,
        @Header("offset") offset: String,
        @Body request: UsClimateRequest,
    ): Response<UsCommandResponse>

    @Headers("from: SPA", "to: ISS", "language: 0", "encryptFlag: false", "brandIndicator: H")
    @POST("ac/v2/rcs/rsc/stop")
    suspend fun stopEngine(
        @Header("accessToken") accessToken: String,
        @Header("client_id") clientId: String,
        @Header("clientSecret") clientSecret: String,
        @Header("blueLinkServicePin") pin: String,
        @Header("username") username: String,
        @Header("registrationId") registrationId: String,
        @Header("gen") gen: String,
        @Header("vin") vin: String,
        @Header("offset") offset: String,
        @Body body: UsCommandBody,
    ): Response<UsCommandResponse>

    // ─── EV Climate ───────────────────────────────────────────────────────

    @Headers("from: SPA", "to: ISS", "language: 0", "encryptFlag: false", "brandIndicator: H")
    @POST("ac/v2/evc/fatc/start")
    suspend fun startClimate(
        @Header("accessToken") accessToken: String,
        @Header("client_id") clientId: String,
        @Header("clientSecret") clientSecret: String,
        @Header("blueLinkServicePin") pin: String,
        @Header("username") username: String,
        @Header("registrationId") registrationId: String,
        @Header("gen") gen: String,
        @Header("vin") vin: String,
        @Header("offset") offset: String,
        @Body request: UsClimateRequest,
    ): Response<UsCommandResponse>

    @Headers("from: SPA", "to: ISS", "language: 0", "encryptFlag: false", "brandIndicator: H")
    @POST("ac/v2/evc/fatc/stop")
    suspend fun stopClimate(
        @Header("accessToken") accessToken: String,
        @Header("client_id") clientId: String,
        @Header("clientSecret") clientSecret: String,
        @Header("blueLinkServicePin") pin: String,
        @Header("username") username: String,
        @Header("registrationId") registrationId: String,
        @Header("gen") gen: String,
        @Header("vin") vin: String,
        @Header("offset") offset: String,
        @Body body: UsCommandBody,
    ): Response<UsCommandResponse>

    // ─── EV Charge ────────────────────────────────────────────────────────

    @Headers("from: SPA", "to: ISS", "language: 0", "encryptFlag: false", "brandIndicator: H")
    @POST("ac/v2/evc/charge/start")
    suspend fun startCharge(
        @Header("accessToken") accessToken: String,
        @Header("client_id") clientId: String,
        @Header("clientSecret") clientSecret: String,
        @Header("blueLinkServicePin") pin: String,
        @Header("username") username: String,
        @Header("registrationId") registrationId: String,
        @Header("gen") gen: String,
        @Header("vin") vin: String,
        @Header("offset") offset: String,
        @Body request: UsChargeBody,
    ): Response<UsCommandResponse>

    @Headers("from: SPA", "to: ISS", "language: 0", "encryptFlag: false", "brandIndicator: H")
    @POST("ac/v2/evc/charge/stop")
    suspend fun stopCharge(
        @Header("accessToken") accessToken: String,
        @Header("client_id") clientId: String,
        @Header("clientSecret") clientSecret: String,
        @Header("blueLinkServicePin") pin: String,
        @Header("username") username: String,
        @Header("registrationId") registrationId: String,
        @Header("gen") gen: String,
        @Header("vin") vin: String,
        @Header("offset") offset: String,
        @Body request: UsChargeBody,
    ): Response<UsCommandResponse>

    @Headers("from: SPA", "to: ISS", "language: 0", "encryptFlag: false", "brandIndicator: H")
    @PUT("ac/v2/evc/soc")
    suspend fun setChargeTarget(
        @Header("accessToken") accessToken: String,
        @Header("client_id") clientId: String,
        @Header("clientSecret") clientSecret: String,
        @Header("blueLinkServicePin") pin: String,
        @Header("username") username: String,
        @Header("registrationId") registrationId: String,
        @Header("gen") gen: String,
        @Header("vin") vin: String,
        @Header("offset") offset: String,
        @Body request: UsChargeTargetRequest,
    ): Response<UsCommandResponse>

    // ─── Horn + Lights ────────────────────────────────────────────────────

    @Headers("from: SPA", "to: ISS", "language: 0", "encryptFlag: false", "brandIndicator: H")
    @POST("ac/v2/rcs/rfc/horn")
    suspend fun honkHorn(
        @Header("accessToken") accessToken: String,
        @Header("client_id") clientId: String,
        @Header("clientSecret") clientSecret: String,
        @Header("blueLinkServicePin") pin: String,
        @Header("username") username: String,
        @Header("registrationId") registrationId: String,
        @Header("gen") gen: String,
        @Header("vin") vin: String,
        @Header("offset") offset: String,
        @Body body: UsCommandBody,
    ): Response<UsCommandResponse>

    @Headers("from: SPA", "to: ISS", "language: 0", "encryptFlag: false", "brandIndicator: H")
    @POST("ac/v2/rcs/rfc/light")
    suspend fun flashLights(
        @Header("accessToken") accessToken: String,
        @Header("client_id") clientId: String,
        @Header("clientSecret") clientSecret: String,
        @Header("blueLinkServicePin") pin: String,
        @Header("username") username: String,
        @Header("registrationId") registrationId: String,
        @Header("gen") gen: String,
        @Header("vin") vin: String,
        @Header("offset") offset: String,
        @Body body: UsCommandBody,
    ): Response<UsCommandResponse>
}
