package com.blue2.app.data.api.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ─────────── US Vehicle Models ───────────

@JsonClass(generateAdapter = true)
data class UsEnrollmentResponse(
    @Json(name = "enrolledVehicleDetails") val enrolledVehicleDetails: List<UsEnrolledVehicle>?,
    @Json(name = "resultCode") val resultCode: String?,
    @Json(name = "errorCode") val errorCode: String?,
    @Json(name = "errorMessage") val errorMessage: String?,
)

@JsonClass(generateAdapter = true)
data class UsEnrolledVehicle(
    @Json(name = "vehicleDetails") val vehicleDetails: UsVehicleDetails?,
)

@JsonClass(generateAdapter = true)
data class UsVehicleDetails(
    @Json(name = "vin") val vin: String?,
    @Json(name = "regid") val regId: String?,            // registration ID used in API headers
    @Json(name = "vehicleIdentifier") val vehicleIdentifier: String?,
    @Json(name = "nickName") val nickName: String?,
    @Json(name = "modelName") val modelName: String?,
    @Json(name = "modelYear") val modelYear: String?,
    @Json(name = "modelCode") val modelCode: String?,
    @Json(name = "licensePlate") val licensePlate: String?,
    @Json(name = "fuelType") val fuelType: String?,
    @Json(name = "trim") val trim: String?,
    @Json(name = "vehicleGeneration") val vehicleGeneration: String?,
    @Json(name = "generation") val generation: String?,
    @Json(name = "enrollmentStatus") val enrollmentStatus: String?,
)

@JsonClass(generateAdapter = true)
data class UsVehicleStatusResponse(
    @Json(name = "vehicleStatus") val vehicleStatus: UsVehicleStatus?,
    @Json(name = "resultCode") val resultCode: String?,
    @Json(name = "errorCode") val errorCode: String?,
    @Json(name = "errorMessage") val errorMessage: String?,
)

@JsonClass(generateAdapter = true)
data class UsVehicleStatus(
    @Json(name = "lastStatusDate") val lastStatusDate: String?,
    @Json(name = "dateTime") val dateTime: String?,
    @Json(name = "doorLock") val doorLock: Boolean?,
    @Json(name = "doorOpen") val doorOpen: UsDoorOpen?,
    @Json(name = "trunkOpen") val trunkOpen: Boolean?,
    @Json(name = "hoodOpen") val hoodOpen: Boolean?,
    @Json(name = "engine") val engine: Boolean?,
    @Json(name = "acc") val acc: Boolean?,
    @Json(name = "evStatus") val evStatus: UsEvStatus?,
    @Json(name = "battery") val battery: UsBattery?,
    @Json(name = "climate") val climate: UsClimate?,
    @Json(name = "odometer") val odometer: UsOdometer?,
    @Json(name = "tirePressure") val tirePressure: UsTirePressure?,
    @Json(name = "lowFuelLight") val lowFuelLight: Boolean?,
    @Json(name = "dte") val dte: UsDte?,
    @Json(name = "heading") val heading: Int?,
    @Json(name = "latitude") val latitude: Double?,
    @Json(name = "longitude") val longitude: Double?,
    @Json(name = "speed") val speed: UsSpeed?,
    @Json(name = "seatBelt") val seatBelt: Map<String, Boolean>?,
)

@JsonClass(generateAdapter = true)
data class UsDoorOpen(
    @Json(name = "frontLeft") val frontLeft: Int?,
    @Json(name = "frontRight") val frontRight: Int?,
    @Json(name = "backLeft") val backLeft: Int?,
    @Json(name = "backRight") val backRight: Int?,
)

@JsonClass(generateAdapter = true)
data class UsEvStatus(
    @Json(name = "batteryStatus") val batteryStatus: Int?,
    @Json(name = "batteryStatusAct") val batteryStatusAct: Int?,
    @Json(name = "batteryPlugin") val batteryPlugin: Int?,
    @Json(name = "batteryCharge") val batteryCharge: Boolean?,
    @Json(name = "remainChargeTime") val remainChargeTime: List<UsRemainChargeTime>?,
    @Json(name = "drvDistance") val drvDistance: List<UsDrvDistance>?,
    @Json(name = "targetSOClist") val targetSOClist: List<UsTargetSOC>?,
    @Json(name = "reservChargeInfos") val reservChargeInfos: UsReservChargeInfos?,
    @Json(name = "chargingPower") val chargingPower: Double?,
)

@JsonClass(generateAdapter = true)
data class UsRemainChargeTime(
    @Json(name = "timeInterval") val timeInterval: UsTimeInterval?,
    @Json(name = "chargingType") val chargingType: Int?,
    @Json(name = "totalTargetSoc") val totalTargetSoc: Int?,
)

@JsonClass(generateAdapter = true)
data class UsTimeInterval(
    @Json(name = "value") val value: Int?,
    @Json(name = "unit") val unit: Int?,
)

@JsonClass(generateAdapter = true)
data class UsDrvDistance(
    @Json(name = "rangeByFuel") val rangeByFuel: UsRangeByFuel?,
    @Json(name = "type") val type: Int?,
)

@JsonClass(generateAdapter = true)
data class UsRangeByFuel(
    @Json(name = "evModeRange") val evModeRange: UsRange?,
    @Json(name = "totalAvailableRange") val totalAvailableRange: UsRange?,
    @Json(name = "gasModeRange") val gasModeRange: UsRange?,
)

@JsonClass(generateAdapter = true)
data class UsRange(
    @Json(name = "value") val value: Double?,
    @Json(name = "unit") val unit: Int?,
)

@JsonClass(generateAdapter = true)
data class UsReservChargeInfos(
    @Json(name = "targetSOClist") val targetSOClist: List<UsTargetSOC>?,
)

@JsonClass(generateAdapter = true)
data class UsTargetSOC(
    @Json(name = "targetSOClevel") val targetSOClevel: Int?,
    @Json(name = "dte") val dte: UsRange?,
    @Json(name = "plugType") val plugType: Int?,
)

@JsonClass(generateAdapter = true)
data class UsBattery(
    @Json(name = "batSoc") val batSoc: Int?,
    @Json(name = "batState") val batState: Int?,
    @Json(name = "sjbDeliveryMode") val sjbDeliveryMode: Int?,
)

@JsonClass(generateAdapter = true)
data class UsClimate(
    @Json(name = "airCtrl") val airCtrl: Boolean?,
    @Json(name = "airTemp") val airTemp: UsAirTemp?,
    @Json(name = "defrost") val defrost: Boolean?,
    @Json(name = "heating1") val heating1: Int?,
    @Json(name = "seatHeaterVentInfo") val seatHeaterVentInfo: UsSeatInfo?,
    @Json(name = "steeringwheel") val steeringwheel: Int?,
    @Json(name = "rearDefrost") val rearDefrost: Boolean?,
)

@JsonClass(generateAdapter = true)
data class UsAirTemp(
    @Json(name = "value") val value: String?,
    @Json(name = "unit") val unit: Int?,
    @Json(name = "hvacTempType") val hvacTempType: Int?,
)

@JsonClass(generateAdapter = true)
data class UsSeatInfo(
    @Json(name = "driverSeatHeatState") val driverSeatHeatState: Int?,
    @Json(name = "passengerSeatHeatState") val passengerSeatHeatState: Int?,
    @Json(name = "rearLeftSeatHeatState") val rearLeftSeatHeatState: Int?,
    @Json(name = "rearRightSeatHeatState") val rearRightSeatHeatState: Int?,
)

@JsonClass(generateAdapter = true)
data class UsOdometer(
    @Json(name = "value") val value: Double?,
    @Json(name = "unit") val unit: Int?,
)

@JsonClass(generateAdapter = true)
data class UsTirePressure(
    @Json(name = "all") val all: Int?,
    @Json(name = "frontLeft") val frontLeft: Int?,
    @Json(name = "frontRight") val frontRight: Int?,
    @Json(name = "backLeft") val backLeft: Int?,
    @Json(name = "backRight") val backRight: Int?,
)

@JsonClass(generateAdapter = true)
data class UsDte(
    @Json(name = "value") val value: Double?,
    @Json(name = "unit") val unit: Int?,
)

@JsonClass(generateAdapter = true)
data class UsSpeed(
    @Json(name = "value") val value: Double?,
    @Json(name = "unit") val unit: Int?,
)

// ─────────── Canada Vehicle Models ───────────

@JsonClass(generateAdapter = true)
data class CaVehicleListResponse(
    @Json(name = "retCode") val retCode: String?,
    @Json(name = "msgCode") val msgCode: String?,
    @Json(name = "msgTxt") val msgTxt: String?,
    @Json(name = "result") val result: CaVehicleListResult?,
)

@JsonClass(generateAdapter = true)
data class CaVehicleListResult(
    @Json(name = "vehicles") val vehicles: List<CaVehicle>?,
)

@JsonClass(generateAdapter = true)
data class CaVehicle(
    @Json(name = "vin") val vin: String?,
    @Json(name = "vehicleId") val vehicleId: String?,
    @Json(name = "nickName") val nickName: String?,
    @Json(name = "modelName") val modelName: String?,
    @Json(name = "modelYear") val modelYear: String?,
    @Json(name = "modelCode") val modelCode: String?,
    @Json(name = "licensePlate") val licensePlate: String?,
    @Json(name = "fuelType") val fuelType: String?,
    @Json(name = "generation") val generation: String?,
)

@JsonClass(generateAdapter = true)
data class CaVehicleStatusResponse(
    @Json(name = "retCode") val retCode: String?,
    @Json(name = "msgCode") val msgCode: String?,
    @Json(name = "msgTxt") val msgTxt: String?,
    @Json(name = "result") val result: CaVehicleStatusResult?,
)

@JsonClass(generateAdapter = true)
data class CaVehicleStatusResult(
    @Json(name = "status") val status: CaVehicleStatus?,
)

@JsonClass(generateAdapter = true)
data class CaVehicleStatus(
    @Json(name = "lastStatusDate") val lastStatusDate: String?,
    @Json(name = "doorLock") val doorLock: Boolean?,
    @Json(name = "doorOpen") val doorOpen: CaDoorOpen?,
    @Json(name = "trunkOpen") val trunkOpen: Boolean?,
    @Json(name = "hoodOpen") val hoodOpen: Boolean?,
    @Json(name = "engine") val engine: Boolean?,
    @Json(name = "acc") val acc: Boolean?,
    @Json(name = "evStatus") val evStatus: CaEvStatus?,
    @Json(name = "battery") val battery: CaBattery?,
    @Json(name = "climate") val climate: CaClimate?,
    @Json(name = "odometer") val odometer: Double?,
    @Json(name = "odometerUnit") val odometerUnit: Int?,
    @Json(name = "tirePressureLamp") val tirePressureLamp: CaTirePressureLamp?,
    @Json(name = "washerFluidStatus") val washerFluidStatus: Boolean?,
    @Json(name = "fuelLevel") val fuelLevel: Int?,
    @Json(name = "lowFuelLight") val lowFuelLight: Boolean?,
    @Json(name = "dte") val dte: Double?,
    @Json(name = "dteUnit") val dteUnit: Int?,
)

@JsonClass(generateAdapter = true)
data class CaDoorOpen(
    @Json(name = "frontLeft") val frontLeft: Int?,
    @Json(name = "frontRight") val frontRight: Int?,
    @Json(name = "backLeft") val backLeft: Int?,
    @Json(name = "backRight") val backRight: Int?,
)

@JsonClass(generateAdapter = true)
data class CaEvStatus(
    @Json(name = "batteryStatus") val batteryStatus: Int?,
    @Json(name = "batteryPlugin") val batteryPlugin: Boolean?,
    @Json(name = "batteryCharge") val batteryCharge: Boolean?,
    @Json(name = "drvDistance") val drvDistance: Double?,
    @Json(name = "drvDistanceUnit") val drvDistanceUnit: Int?,
    @Json(name = "chargingPower") val chargingPower: Double?,
)

@JsonClass(generateAdapter = true)
data class CaBattery(
    @Json(name = "batSoc") val batSoc: Int?,
    @Json(name = "voltage") val voltage: Double?,
)

@JsonClass(generateAdapter = true)
data class CaClimate(
    @Json(name = "airCtrl") val airCtrl: Boolean?,
    @Json(name = "airTemp") val airTemp: CaAirTemp?,
    @Json(name = "defrost") val defrost: Boolean?,
    @Json(name = "rearDefrost") val rearDefrost: Boolean?,
)

@JsonClass(generateAdapter = true)
data class CaAirTemp(
    @Json(name = "value") val value: String?,
    @Json(name = "unit") val unit: Int?,
)

@JsonClass(generateAdapter = true)
data class CaTirePressureLamp(
    @Json(name = "tirePressureLampAll") val tirePressureLampAll: Int?,
    @Json(name = "tirePressureLampFL") val tirePressureLampFL: Int?,
    @Json(name = "tirePressureLampFR") val tirePressureLampFR: Int?,
    @Json(name = "tirePressureLampRL") val tirePressureLampRL: Int?,
    @Json(name = "tirePressureLampRR") val tirePressureLampRR: Int?,
)

// ─────────── EU / Gen5W Vehicle Models ───────────

@JsonClass(generateAdapter = true)
data class EuVehicleListResponse(
    @Json(name = "retCode") val retCode: String?,
    @Json(name = "message") val message: EuMessage?,
    @Json(name = "payload") val payload: EuVehicleListPayload?,
)

@JsonClass(generateAdapter = true)
data class EuMessage(
    @Json(name = "id") val id: String?,
    @Json(name = "code") val code: Int?,
    @Json(name = "category") val category: String?,
    @Json(name = "subcategory") val subcategory: String?,
    @Json(name = "description") val description: String?,
)

@JsonClass(generateAdapter = true)
data class EuVehicleListPayload(
    @Json(name = "vehicles") val vehicles: List<EuVehicle>?,
)

@JsonClass(generateAdapter = true)
data class EuVehicle(
    @Json(name = "vin") val vin: String?,
    @Json(name = "vehicleId") val vehicleId: String?,
    @Json(name = "nickName") val nickName: String?,
    @Json(name = "modelName") val modelName: String?,
    @Json(name = "modelYear") val modelYear: String?,
    @Json(name = "modelCode") val modelCode: String?,
    @Json(name = "licensePlate") val licensePlate: String?,
    @Json(name = "fuelType") val fuelType: String?,
    @Json(name = "generation") val generation: Int?,
    @Json(name = "ccuCCS2ProtocolSupport") val ccs2Support: Int?,
)

@JsonClass(generateAdapter = true)
data class EuVehicleStatusResponse(
    @Json(name = "retCode") val retCode: String?,
    @Json(name = "message") val message: EuMessage?,
    @Json(name = "payload") val payload: EuVehicleStatusPayload?,
)

@JsonClass(generateAdapter = true)
data class EuVehicleStatusPayload(
    @Json(name = "updatedAt") val updatedAt: String?,
    @Json(name = "vehicleStatus") val vehicleStatus: EuStatusDetails?,
    @Json(name = "resMsg") val resMsg: EuCcs2ResMsg?,
)

// Legacy EU status
@JsonClass(generateAdapter = true)
data class EuStatusDetails(
    @Json(name = "doorLock") val doorLock: Boolean?,
    @Json(name = "doorOpen") val doorOpen: EuDoorOpen?,
    @Json(name = "trunkOpen") val trunkOpen: Boolean?,
    @Json(name = "hoodOpen") val hoodOpen: Boolean?,
    @Json(name = "acc") val acc: Boolean?,
    @Json(name = "engine") val engine: Boolean?,
    @Json(name = "evStatus") val evStatus: EuEvStatus?,
    @Json(name = "battery") val battery: EuBattery?,
    @Json(name = "climate") val climate: EuClimate?,
    @Json(name = "odometer") val odometer: EuOdometer?,
    @Json(name = "tirePressure") val tirePressure: EuTirePressure?,
    @Json(name = "fuelLevel") val fuelLevel: Int?,
    @Json(name = "lowFuelLight") val lowFuelLight: Boolean?,
    @Json(name = "dteMax") val dteMax: EuDte?,
    @Json(name = "latitude") val latitude: Double?,
    @Json(name = "longitude") val longitude: Double?,
    @Json(name = "heading") val heading: Int?,
    @Json(name = "speed") val speed: EuSpeed?,
)

// CCS2 protocol response (newer EVs)
@JsonClass(generateAdapter = true)
data class EuCcs2ResMsg(
    @Json(name = "state") val state: EuCcs2State?,
)

@JsonClass(generateAdapter = true)
data class EuCcs2State(
    @Json(name = "Vehicle") val vehicle: EuCcs2Vehicle?,
)

@JsonClass(generateAdapter = true)
data class EuCcs2Vehicle(
    @Json(name = "Drivetrain") val drivetrain: EuCcs2Drivetrain?,
    @Json(name = "Green") val green: EuCcs2Green?,
    @Json(name = "Cabin") val cabin: EuCcs2Cabin?,
    @Json(name = "Body") val body: EuCcs2Body?,
    @Json(name = "Chassis") val chassis: EuCcs2Chassis?,
    @Json(name = "Electronics") val electronics: EuCcs2Electronics?,
    @Json(name = "Location") val location: EuCcs2Location?,
)

@JsonClass(generateAdapter = true)
data class EuCcs2Drivetrain(
    @Json(name = "Odometer") val odometer: Double?,
    @Json(name = "FuelSystem") val fuelSystem: EuCcs2FuelSystem?,
)

@JsonClass(generateAdapter = true)
data class EuCcs2FuelSystem(
    @Json(name = "DTE") val dte: EuCcs2Dte?,
    @Json(name = "FuelLevel") val fuelLevel: Double?,
)

@JsonClass(generateAdapter = true)
data class EuCcs2Dte(
    @Json(name = "Total") val total: Double?,
)

@JsonClass(generateAdapter = true)
data class EuCcs2Green(
    @Json(name = "BatteryManagement") val batteryManagement: EuCcs2BatteryManagement?,
    @Json(name = "ChargingInformation") val chargingInformation: EuCcs2ChargingInfo?,
    @Json(name = "Electric") val electric: EuCcs2Electric?,
)

@JsonClass(generateAdapter = true)
data class EuCcs2BatteryManagement(
    @Json(name = "BatteryRemain") val batteryRemain: EuCcs2BatteryRemain?,
)

@JsonClass(generateAdapter = true)
data class EuCcs2BatteryRemain(
    @Json(name = "Ratio") val ratio: Int?,
)

@JsonClass(generateAdapter = true)
data class EuCcs2ChargingInfo(
    @Json(name = "ConnectorFastening") val connectorFastening: EuCcs2ConnectorFastening?,
    @Json(name = "Charging") val charging: EuCcs2Charging?,
    @Json(name = "TargetSoC") val targetSoC: EuCcs2TargetSoC?,
)

@JsonClass(generateAdapter = true)
data class EuCcs2ConnectorFastening(
    @Json(name = "State") val state: Int?,
)

@JsonClass(generateAdapter = true)
data class EuCcs2Charging(
    @Json(name = "RemainTime") val remainTime: Int?,
    @Json(name = "ChargingPower") val chargingPower: Double?,
    @Json(name = "IsCharging") val isCharging: Boolean?,
)

@JsonClass(generateAdapter = true)
data class EuCcs2TargetSoC(
    @Json(name = "Standard") val standard: Int?,
    @Json(name = "Quick") val quick: Int?,
)

@JsonClass(generateAdapter = true)
data class EuCcs2Electric(
    @Json(name = "SmartGrid") val smartGrid: EuCcs2SmartGrid?,
    @Json(name = "Range") val range: Double?,
)

@JsonClass(generateAdapter = true)
data class EuCcs2SmartGrid(
    @Json(name = "RealTimePower") val realTimePower: Double?,
)

@JsonClass(generateAdapter = true)
data class EuCcs2Cabin(
    @Json(name = "Door") val door: EuCcs2Door?,
    @Json(name = "HVAC") val hvac: Map<String, Any>?,
)

@JsonClass(generateAdapter = true)
data class EuCcs2Door(
    @Json(name = "Row1") val row1: EuCcs2DoorRow?,
    @Json(name = "Row2") val row2: EuCcs2DoorRow?,
    @Json(name = "Trunk") val trunk: EuCcs2DoorState?,
    @Json(name = "Hood") val hood: EuCcs2DoorState?,
    @Json(name = "AllDoorLock") val allDoorLock: Int?,
)

@JsonClass(generateAdapter = true)
data class EuCcs2DoorRow(
    @Json(name = "Driver") val driver: EuCcs2DoorState?,
    @Json(name = "Passenger") val passenger: EuCcs2DoorState?,
)

@JsonClass(generateAdapter = true)
data class EuCcs2DoorState(
    @Json(name = "Open") val open: Int?,
    @Json(name = "Lock") val lock: Int?,
)

@JsonClass(generateAdapter = true)
data class EuCcs2Body(
    @Json(name = "Hood") val hood: EuCcs2DoorState?,
    @Json(name = "Trunk") val trunk: EuCcs2DoorState?,
)

@JsonClass(generateAdapter = true)
data class EuCcs2Chassis(
    @Json(name = "tirePressureWarnings") val tirePressureWarnings: Map<String, Int>?,
)

@JsonClass(generateAdapter = true)
data class EuCcs2Electronics(
    @Json(name = "Battery") val battery: EuCcs2ElecBattery?,
)

@JsonClass(generateAdapter = true)
data class EuCcs2ElecBattery(
    @Json(name = "Level") val level: Int?,
    @Json(name = "Voltage") val voltage: Double?,
)

@JsonClass(generateAdapter = true)
data class EuCcs2Location(
    @Json(name = "GeoCoord") val geoCoord: EuCcs2GeoCoord?,
    @Json(name = "Heading") val heading: Int?,
    @Json(name = "Speed") val speed: Double?,
)

@JsonClass(generateAdapter = true)
data class EuCcs2GeoCoord(
    @Json(name = "Latitude") val latitude: Double?,
    @Json(name = "Longitude") val longitude: Double?,
)

@JsonClass(generateAdapter = true)
data class EuDoorOpen(
    @Json(name = "frontLeft") val frontLeft: Int?,
    @Json(name = "frontRight") val frontRight: Int?,
    @Json(name = "backLeft") val backLeft: Int?,
    @Json(name = "backRight") val backRight: Int?,
)

@JsonClass(generateAdapter = true)
data class EuEvStatus(
    @Json(name = "batteryStatus") val batteryStatus: Int?,
    @Json(name = "batteryPlugin") val batteryPlugin: Int?,
    @Json(name = "batteryCharge") val batteryCharge: Boolean?,
    @Json(name = "remainChargeTime") val remainChargeTime: Int?,
    @Json(name = "drvDistance") val drvDistance: Double?,
    @Json(name = "drvDistanceUnit") val drvDistanceUnit: Int?,
    @Json(name = "targetSOC") val targetSOC: List<EuTargetSOCStatus>?,
    @Json(name = "chargingPower") val chargingPower: Double?,
)

@JsonClass(generateAdapter = true)
data class EuTargetSOCStatus(
    @Json(name = "targetSOClevel") val targetSOClevel: Int?,
    @Json(name = "dte") val dte: Double?,
    @Json(name = "plugType") val plugType: Int?,
)

@JsonClass(generateAdapter = true)
data class EuBattery(
    @Json(name = "batSoc") val batSoc: Int?,
    @Json(name = "voltage") val voltage: Double?,
)

@JsonClass(generateAdapter = true)
data class EuClimate(
    @Json(name = "airCtrl") val airCtrl: Boolean?,
    @Json(name = "airTemp") val airTemp: EuAirTemp?,
    @Json(name = "defrost") val defrost: Boolean?,
    @Json(name = "rearDefrost") val rearDefrost: Boolean?,
    @Json(name = "seatHeaterVentInfo") val seatHeaterVentInfo: EuSeatInfo?,
    @Json(name = "steeringwheel") val steeringwheel: Int?,
)

@JsonClass(generateAdapter = true)
data class EuAirTemp(
    @Json(name = "value") val value: String?,
    @Json(name = "unit") val unit: Int?,
)

@JsonClass(generateAdapter = true)
data class EuSeatInfo(
    @Json(name = "driverSeatHeatState") val driverSeatHeatState: Int?,
    @Json(name = "passengerSeatHeatState") val passengerSeatHeatState: Int?,
    @Json(name = "rearLeftSeatHeatState") val rearLeftSeatHeatState: Int?,
    @Json(name = "rearRightSeatHeatState") val rearRightSeatHeatState: Int?,
)

@JsonClass(generateAdapter = true)
data class EuOdometer(
    @Json(name = "value") val value: Double?,
    @Json(name = "unit") val unit: Int?,
)

@JsonClass(generateAdapter = true)
data class EuTirePressure(
    @Json(name = "all") val all: Int?,
    @Json(name = "frontLeft") val frontLeft: Int?,
    @Json(name = "frontRight") val frontRight: Int?,
    @Json(name = "backLeft") val backLeft: Int?,
    @Json(name = "backRight") val backRight: Int?,
)

@JsonClass(generateAdapter = true)
data class EuDte(
    @Json(name = "value") val value: Double?,
    @Json(name = "unit") val unit: Int?,
)

@JsonClass(generateAdapter = true)
data class EuSpeed(
    @Json(name = "value") val value: Double?,
    @Json(name = "unit") val unit: Int?,
)
