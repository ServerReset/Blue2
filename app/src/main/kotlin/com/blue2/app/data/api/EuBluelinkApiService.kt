package com.blue2.app.data.api

import com.blue2.app.data.api.models.*
import retrofit2.Response
import retrofit2.http.*

// EU / AU / ME Gen5W SPA API
// Main base URL: https://prd.eu-ccapi.hyundai.com:8080/
// Auth IDP base URL: https://idpconnect-eu.hyundai.com/
interface EuBluelinkIdpService {
    @GET("auth/api/v2/user/oauth2/authorize")
    suspend fun getAuthorizePage(
        @Query("response_type") responseType: String = "code",
        @Query("state") state: String,
        @Query("client_id") clientId: String,
        @Query("redirect_uri") redirectUri: String,
        @Query("lang") lang: String = "en",
    ): Response<okhttp3.ResponseBody>

    @FormUrlEncoded
    @POST("auth/account/signin")
    suspend fun signIn(
        @Field("client_id") clientId: String,
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("encryptedPassword") encryptedPassword: Boolean = false,
        @Field("connector_session_key") connectorSessionKey: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("state") state: String = "ccsp",
        @Field("remember_me") rememberMe: Boolean = false,
    ): Response<okhttp3.ResponseBody>

    @FormUrlEncoded
    @POST("auth/api/v2/user/oauth2/token")
    suspend fun getToken(
        @Field("grant_type") grantType: String = "authorization_code",
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("client_id") clientId: String? = null,
        @Header("Authorization") basicAuth: String,
    ): Response<EuTokenResponse>

    @FormUrlEncoded
    @POST("auth/api/v2/user/oauth2/token")
    suspend fun refreshToken(
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("refresh_token") refreshToken: String,
        @Field("redirect_uri") redirectUri: String,
        @Header("Authorization") basicAuth: String,
    ): Response<EuTokenResponse>
}

interface EuBluelinkApiService {

    // Device Registration (required after first login)
    @POST("api/v1/spa/notifications/register")
    suspend fun registerDevice(
        @Header("Authorization") bearerToken: String,
        @Header("ccsp-service-id") serviceId: String,
        @Header("ccsp-application-id") appId: String,
        @Header("Stamp") stamp: String,
        @Header("ccsp-device-id") deviceId: String,
        @Body request: EuDeviceRegistrationRequest,
    ): Response<EuDeviceRegistrationResponse>

    // PIN → control token (required before commands)
    @PUT("api/v1/user/pin")
    suspend fun verifyPin(
        @Header("Authorization") bearerToken: String,
        @Header("ccsp-service-id") serviceId: String,
        @Header("ccsp-application-id") appId: String,
        @Header("Stamp") stamp: String,
        @Header("ccsp-device-id") deviceId: String,
        @Body request: EuPinRequest,
    ): Response<EuControlTokenResponse>

    // Vehicles
    @GET("api/v1/spa/vehicles")
    suspend fun getVehicles(
        @Header("Authorization") bearerToken: String,
        @Header("ccsp-service-id") serviceId: String,
        @Header("ccsp-application-id") appId: String,
        @Header("Stamp") stamp: String,
        @Header("ccsp-device-id") deviceId: String,
        @Header("offset") offset: Int = 0,
    ): Response<EuVehicleListResponse>

    // Status — CCS2 protocol (newer EVs)
    @GET("api/v1/spa/vehicles/{vehicleId}/ccs2/carstatus/latest")
    suspend fun getVehicleStatusCached(
        @Path("vehicleId") vehicleId: String,
        @Header("Authorization") bearerToken: String,
        @Header("ccsp-service-id") serviceId: String,
        @Header("ccsp-application-id") appId: String,
        @Header("Stamp") stamp: String,
        @Header("ccsp-device-id") deviceId: String,
    ): Response<EuVehicleStatusResponse>

    @GET("api/v1/spa/vehicles/{vehicleId}/ccs2/carstatus")
    suspend fun getVehicleStatusRefresh(
        @Path("vehicleId") vehicleId: String,
        @Header("Authorization") bearerToken: String,
        @Header("ccsp-service-id") serviceId: String,
        @Header("ccsp-application-id") appId: String,
        @Header("Stamp") stamp: String,
        @Header("ccsp-device-id") deviceId: String,
    ): Response<EuVehicleStatusResponse>

    // Door control (lock/unlock) — requires controlToken
    @POST("api/v2/spa/vehicles/{vehicleId}/control/door")
    suspend fun controlDoor(
        @Path("vehicleId") vehicleId: String,
        @Header("Authorization") controlToken: String,
        @Header("ccsp-service-id") serviceId: String,
        @Header("ccsp-application-id") appId: String,
        @Header("Stamp") stamp: String,
        @Header("ccsp-device-id") deviceId: String,
        @Body request: EuDoorControlRequest,
    ): Response<EuCommandResponse>

    // Climate control — requires controlToken
    @POST("api/v2/spa/vehicles/{vehicleId}/control/temperature")
    suspend fun controlClimate(
        @Path("vehicleId") vehicleId: String,
        @Header("Authorization") controlToken: String,
        @Header("ccsp-service-id") serviceId: String,
        @Header("ccsp-application-id") appId: String,
        @Header("Stamp") stamp: String,
        @Header("ccsp-device-id") deviceId: String,
        @Body request: EuClimateRequest,
    ): Response<EuCommandResponse>

    // Charge control — requires controlToken
    @POST("api/v2/spa/vehicles/{vehicleId}/control/charge")
    suspend fun controlCharge(
        @Path("vehicleId") vehicleId: String,
        @Header("Authorization") controlToken: String,
        @Header("ccsp-service-id") serviceId: String,
        @Header("ccsp-application-id") appId: String,
        @Header("Stamp") stamp: String,
        @Header("ccsp-device-id") deviceId: String,
        @Body request: EuChargeActionRequest,
    ): Response<EuCommandResponse>

    // Charge target — requires accessToken (not controlToken)
    @POST("api/v1/spa/vehicles/{vehicleId}/charge/target")
    suspend fun setChargeTarget(
        @Path("vehicleId") vehicleId: String,
        @Header("Authorization") bearerToken: String,
        @Header("ccsp-service-id") serviceId: String,
        @Header("ccsp-application-id") appId: String,
        @Header("Stamp") stamp: String,
        @Header("ccsp-device-id") deviceId: String,
        @Body request: EuChargeTargetRequest,
    ): Response<EuCommandResponse>

    // Notifications / command result polling
    @GET("api/v1/spa/notifications/{vehicleId}/records")
    suspend fun getNotifications(
        @Path("vehicleId") vehicleId: String,
        @Header("Authorization") bearerToken: String,
        @Header("ccsp-service-id") serviceId: String,
        @Header("ccsp-application-id") appId: String,
        @Header("Stamp") stamp: String,
        @Header("ccsp-device-id") deviceId: String,
    ): Response<EuCommandResponse>
}
