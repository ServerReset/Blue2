package com.blue2.app.data.api.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ─────────── US Command Models ───────────

@JsonClass(generateAdapter = true)
data class UsCommandResponse(
    @Json(name = "responseHeader") val responseHeader: UsResponseHeader?,
    @Json(name = "resultCode") val resultCode: String?,
    @Json(name = "errorSubCode") val errorSubCode: String?,
    @Json(name = "errorMessage") val errorMessage: String?,
    @Json(name = "payloadBody") val payloadBody: UsPayloadBody?,
)

@JsonClass(generateAdapter = true)
data class UsResponseHeader(
    @Json(name = "responseCode") val responseCode: Int?,
    @Json(name = "responseDesc") val responseDesc: String?,
)

@JsonClass(generateAdapter = true)
data class UsPayloadBody(
    @Json(name = "transactionId") val transactionId: String?,
    @Json(name = "status") val status: String?,
    @Json(name = "value") val value: String?,
)

@JsonClass(generateAdapter = true)
data class UsChargeBody(
    @Json(name = "userName") val userName: String,
    @Json(name = "vin") val vin: String,
)

@JsonClass(generateAdapter = true)
data class UsCommandBody(
    @Json(name = "userName") val userName: String,
    @Json(name = "vin") val vin: String,
    @Json(name = "pin") val pin: String = "",
)

@JsonClass(generateAdapter = true)
data class UsClimateRequest(
    @Json(name = "Ims") val ims: Int = 0,
    @Json(name = "airCtrl") val airCtrl: Int = 1,
    @Json(name = "airTemp") val airTemp: UsClimateTemp,
    @Json(name = "defrost") val defrost: Boolean = false,
    @Json(name = "heating1") val heating1: Int = 0,
    @Json(name = "igniOnDuration") val igniOnDuration: Int = 10,
    @Json(name = "seatHeaterVentInfo") val seatHeaterVentInfo: UsSeatHeatRequest? = null,
    @Json(name = "username") val username: String,
    @Json(name = "vin") val vin: String,
)

@JsonClass(generateAdapter = true)
data class UsClimateTemp(
    @Json(name = "value") val value: String,
    @Json(name = "unit") val unit: Int = 1,
    @Json(name = "hvacTempType") val hvacTempType: Int = 0,
)

@JsonClass(generateAdapter = true)
data class UsSeatHeatRequest(
    @Json(name = "driverSeatHeatState") val driverSeatHeatState: Int = 0,
    @Json(name = "passengerSeatHeatState") val passengerSeatHeatState: Int = 0,
    @Json(name = "rearLeftSeatHeatState") val rearLeftSeatHeatState: Int = 0,
    @Json(name = "rearRightSeatHeatState") val rearRightSeatHeatState: Int = 0,
)

@JsonClass(generateAdapter = true)
data class UsChargeTargetRequest(
    @Json(name = "chargeTargetList") val chargeTargetList: List<UsChargeTarget>,
)

@JsonClass(generateAdapter = true)
data class UsChargeTarget(
    @Json(name = "targetSOClevel") val targetSOClevel: Int,
    @Json(name = "dte") val dte: UsChargeTargetDte? = null,
    @Json(name = "plugType") val plugType: Int,
)

@JsonClass(generateAdapter = true)
data class UsChargeTargetDte(
    @Json(name = "rangeByFuel") val rangeByFuel: Map<String, Any>? = null,
)

// ─────────── Canada Command Models ───────────

@JsonClass(generateAdapter = true)
data class CaCommandResponse(
    @Json(name = "retCode") val retCode: String?,
    @Json(name = "msgCode") val msgCode: String?,
    @Json(name = "msgTxt") val msgTxt: String?,
    @Json(name = "result") val result: CaCommandResult?,
)

@JsonClass(generateAdapter = true)
data class CaCommandResult(
    @Json(name = "transactionId") val transactionId: String?,
    @Json(name = "reqId") val reqId: String?,
    @Json(name = "pAuth") val pAuth: String?,
)

@JsonClass(generateAdapter = true)
data class CaCommandStatusResponse(
    @Json(name = "retCode") val retCode: String?,
    @Json(name = "msgCode") val msgCode: String?,
    @Json(name = "msgTxt") val msgTxt: String?,
    @Json(name = "result") val result: CaCommandStatusResult?,
)

@JsonClass(generateAdapter = true)
data class CaCommandStatusResult(
    @Json(name = "statusCode") val statusCode: Int?,
    @Json(name = "transactionId") val transactionId: String?,
)

@JsonClass(generateAdapter = true)
data class CaClimateRequest(
    @Json(name = "pin") val pin: String,
    @Json(name = "pAuth") val pAuth: String,
    @Json(name = "hvacType") val hvacType: Int = 0,
    @Json(name = "options") val options: CaClimateOptions,
    @Json(name = "tempCode") val tempCode: String,
    @Json(name = "unit") val unit: String = "C",
)

@JsonClass(generateAdapter = true)
data class CaClimateOptions(
    @Json(name = "defrost") val defrost: Boolean = false,
    @Json(name = "heating1") val heating1: Int = 0,
)

@JsonClass(generateAdapter = true)
data class CaLockRequest(
    @Json(name = "pin") val pin: String,
    @Json(name = "pAuth") val pAuth: String = "",
)

@JsonClass(generateAdapter = true)
data class CaChargeRequest(
    @Json(name = "pin") val pin: String,
    @Json(name = "pAuth") val pAuth: String,
)

@JsonClass(generateAdapter = true)
data class CaSetSocRequest(
    @Json(name = "pin") val pin: String,
    @Json(name = "pAuth") val pAuth: String,
    @Json(name = "chargeConfig") val chargeConfig: CaChargeConfig,
)

@JsonClass(generateAdapter = true)
data class CaChargeConfig(
    @Json(name = "targetSOClist") val targetSOClist: List<CaTargetSOC>,
)

@JsonClass(generateAdapter = true)
data class CaTargetSOC(
    @Json(name = "targetSOClevel") val targetSOClevel: Int,
    @Json(name = "plugType") val plugType: Int,
)

// ─────────── EU / Gen5W Command Models ───────────

@JsonClass(generateAdapter = true)
data class EuCommandResponse(
    @Json(name = "retCode") val retCode: String?,
    @Json(name = "message") val message: EuMessage?,
    @Json(name = "payload") val payload: EuCommandPayload?,
)

@JsonClass(generateAdapter = true)
data class EuCommandPayload(
    @Json(name = "transactionId") val transactionId: String?,
    @Json(name = "reqId") val reqId: String?,
    @Json(name = "stampId") val stampId: String?,
)

@JsonClass(generateAdapter = true)
data class EuDoorControlRequest(
    @Json(name = "action") val action: String,
    @Json(name = "deviceId") val deviceId: String,
)

@JsonClass(generateAdapter = true)
data class EuClimateRequest(
    @Json(name = "action") val action: String,
    @Json(name = "hvacType") val hvacType: Int = 0,
    @Json(name = "options") val options: EuClimateOptions,
    @Json(name = "tempCode") val tempCode: String,
    @Json(name = "unit") val unit: String = "C",
)

@JsonClass(generateAdapter = true)
data class EuClimateOptions(
    @Json(name = "defrost") val defrost: Boolean = false,
    @Json(name = "heating1") val heating1: Int = 0,
    @Json(name = "rearWindowHeat") val rearWindowHeat: Int = 0,
    @Json(name = "steeringWheel") val steeringWheel: Int = 0,
)

@JsonClass(generateAdapter = true)
data class EuChargeActionRequest(
    @Json(name = "action") val action: String,
    @Json(name = "deviceId") val deviceId: String,
)

@JsonClass(generateAdapter = true)
data class EuChargeTargetRequest(
    @Json(name = "targetSOClist") val targetSOClist: List<EuTargetSOC>,
)

@JsonClass(generateAdapter = true)
data class EuTargetSOC(
    @Json(name = "targetSOClevel") val targetSOClevel: Int,
    @Json(name = "plugType") val plugType: Int,
)
