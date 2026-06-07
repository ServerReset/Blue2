package com.blue2.app.data.api.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ─────────── US Auth ───────────

@JsonClass(generateAdapter = true)
data class UsLoginRequest(
    @Json(name = "username") val username: String,
    @Json(name = "password") val password: String,
)

@JsonClass(generateAdapter = true)
data class UsLoginResponse(
    @Json(name = "access_token") val accessToken: String?,
    @Json(name = "refresh_token") val refreshToken: String?,
    @Json(name = "token_type") val tokenType: String?,
    @Json(name = "expires_in") val expiresIn: String?,
    @Json(name = "username") val username: String?,
    @Json(name = "userId") val userId: String?,
)

@JsonClass(generateAdapter = true)
data class UsRefreshRequest(
    @Json(name = "refresh_token") val refreshToken: String,
)

// ─────────── Canada Auth ───────────

@JsonClass(generateAdapter = true)
data class CaLoginRequest(
    @Json(name = "loginId") val loginId: String,
    @Json(name = "password") val password: String,
    @Json(name = "pushRegToken") val pushRegToken: String = "",
    @Json(name = "pushType") val pushType: String = "GCM",
    @Json(name = "uuid") val uuid: String = "",
)

@JsonClass(generateAdapter = true)
data class CaLoginResponse(
    @Json(name = "retCode") val retCode: String?,
    @Json(name = "msgCode") val msgCode: String?,
    @Json(name = "msgTxt") val msgTxt: String?,
    @Json(name = "result") val result: CaLoginResult?,
)

@JsonClass(generateAdapter = true)
data class CaLoginResult(
    @Json(name = "accessToken") val accessToken: String?,
    @Json(name = "refreshToken") val refreshToken: String?,
    @Json(name = "expireIn") val expireIn: String?,
    @Json(name = "userId") val userId: String?,
    @Json(name = "EV") val ev: Boolean?,
    @Json(name = "otpFailCount") val otpFailCount: Int?,
)

@JsonClass(generateAdapter = true)
data class CaRefreshRequest(
    @Json(name = "refreshToken") val refreshToken: String,
)

@JsonClass(generateAdapter = true)
data class CaPinVerifyRequest(
    @Json(name = "pin") val pin: String,
)

@JsonClass(generateAdapter = true)
data class CaPinVerifyResponse(
    @Json(name = "retCode") val retCode: String?,
    @Json(name = "msgCode") val msgCode: String?,
    @Json(name = "msgTxt") val msgTxt: String?,
    @Json(name = "result") val result: CaPinVerifyResult?,
)

@JsonClass(generateAdapter = true)
data class CaPinVerifyResult(
    @Json(name = "pAuth") val pAuth: String?,
)

// ─────────── Europe / Gen5W Auth ───────────

@JsonClass(generateAdapter = true)
data class EuSignInRequest(
    @Json(name = "username") val username: String,
    @Json(name = "password") val password: String,
    @Json(name = "encryptedPassword") val encryptedPassword: Boolean = false,
    @Json(name = "client_id") val clientId: String,
    @Json(name = "redirect_uri") val redirectUri: String,
    @Json(name = "state") val state: String = "ccsp",
    @Json(name = "remember_me") val rememberMe: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class EuTokenResponse(
    @Json(name = "access_token") val accessToken: String?,
    @Json(name = "refresh_token") val refreshToken: String?,
    @Json(name = "token_type") val tokenType: String?,
    @Json(name = "expires_in") val expiresIn: Long?,
    @Json(name = "scope") val scope: String?,
)

@JsonClass(generateAdapter = true)
data class EuDeviceRegistrationRequest(
    @Json(name = "pushRegId") val pushRegId: String,
    @Json(name = "pushType") val pushType: String = "GCM",
    @Json(name = "uuid") val uuid: String,
)

@JsonClass(generateAdapter = true)
data class EuDeviceRegistrationResponse(
    @Json(name = "retCode") val retCode: String?,
    @Json(name = "message") val message: EuMessage?,
    @Json(name = "payload") val payload: EuDevicePayload?,
)

@JsonClass(generateAdapter = true)
data class EuDevicePayload(
    @Json(name = "deviceId") val deviceId: String?,
)

@JsonClass(generateAdapter = true)
data class EuPinRequest(
    @Json(name = "deviceId") val deviceId: String,
    @Json(name = "pin") val pin: String,
)

@JsonClass(generateAdapter = true)
data class EuControlTokenResponse(
    @Json(name = "retCode") val retCode: String?,
    @Json(name = "message") val message: EuMessage?,
    @Json(name = "payload") val payload: EuControlTokenPayload?,
)

@JsonClass(generateAdapter = true)
data class EuControlTokenPayload(
    @Json(name = "controlToken") val controlToken: String?,
    @Json(name = "expiresTime") val expiresTime: Long?,
)
