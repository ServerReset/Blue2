package com.blue2.app.domain.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class BluelinkRegion { US, CA, EU, AU, ME }

@Parcelize
data class Vehicle(
    val vin: String,
    val vehicleId: String,
    val nickname: String,
    val modelName: String,
    val modelYear: String,
    val modelCode: String,
    val licensePlate: String,
    val fuelType: FuelType,
    val region: BluelinkRegion,
    val generation: Int = 5,
    val customImageUri: String? = null,
) : Parcelable

enum class FuelType { GASOLINE, ELECTRIC, HYBRID, PHEV }

data class VehicleStatus(
    val vin: String,
    val timestamp: Long = System.currentTimeMillis(),

    // Door/Lock
    val isLocked: Boolean = true,
    val doorFrontLeft: DoorState = DoorState.CLOSED,
    val doorFrontRight: DoorState = DoorState.CLOSED,
    val doorRearLeft: DoorState = DoorState.CLOSED,
    val doorRearRight: DoorState = DoorState.CLOSED,
    val trunkOpen: Boolean = false,
    val hoodOpen: Boolean = false,

    // Engine / EV
    val engineRunning: Boolean = false,
    val evBatteryPercent: Int? = null,
    val evRangeKm: Double? = null,
    val evCharging: Boolean = false,
    val evChargeTargetPercent: Int? = null,
    val evPluggedIn: Boolean = false,
    val estimatedChargingMinutes: Int? = null,

    // Fuel
    val fuelLevelPercent: Int? = null,
    val fuelRangeKm: Double? = null,

    // Climate
    val climateOn: Boolean = false,
    val interiorTempC: Double? = null,
    val exteriorTempC: Double? = null,
    val targetTempC: Double? = null,
    val defrostFront: Boolean = false,
    val defrostRear: Boolean = false,
    val seatHeatFrontLeft: SeatHeatingLevel = SeatHeatingLevel.OFF,
    val seatHeatFrontRight: SeatHeatingLevel = SeatHeatingLevel.OFF,
    val seatHeatRearLeft: SeatHeatingLevel = SeatHeatingLevel.OFF,
    val seatHeatRearRight: SeatHeatingLevel = SeatHeatingLevel.OFF,
    val steeringWheelHeat: Boolean = false,

    // Location
    val latitude: Double? = null,
    val longitude: Double? = null,
    val heading: Int? = null,
    val speed: Double? = null,

    // Diagnostics / Maintenance
    val odometer: Double? = null,
    val tirePressureFrontLeft: Int? = null,
    val tirePressureFrontRight: Int? = null,
    val tirePressureRearLeft: Int? = null,
    val tirePressureRearRight: Int? = null,
    val tirePressureWarning: Boolean = false,
    val lowWasherFluid: Boolean = false,
    val lowCoolant: Boolean = false,
    val engineOilLife: Int? = null,
    val brakeFluidLow: Boolean = false,
    val maintenanceDueKm: Double? = null,
    val dtcCodes: List<String> = emptyList(),

    // Lights
    val lightsOn: Boolean = false,
    val highBeam: Boolean = false,

    // Windows
    val windowFrontLeft: WindowState = WindowState.CLOSED,
    val windowFrontRight: WindowState = WindowState.CLOSED,
    val windowRearLeft: WindowState = WindowState.CLOSED,
    val windowRearRight: WindowState = WindowState.CLOSED,

    // 12V Battery
    val batteryVoltage: Double? = null,

    val rawJson: String? = null,
)

enum class DoorState { OPEN, CLOSED, UNKNOWN }
enum class WindowState { OPEN, CLOSED, UNKNOWN }
enum class SeatHeatingLevel { OFF, LOW, MEDIUM, HIGH }

data class ClimateSettings(
    val temperatureC: Double = 22.0,
    val defrostFront: Boolean = false,
    val defrostRear: Boolean = false,
    val seatHeatFrontLeft: SeatHeatingLevel = SeatHeatingLevel.OFF,
    val seatHeatFrontRight: SeatHeatingLevel = SeatHeatingLevel.OFF,
    val seatHeatRearLeft: SeatHeatingLevel = SeatHeatingLevel.OFF,
    val seatHeatRearRight: SeatHeatingLevel = SeatHeatingLevel.OFF,
    val steeringWheelHeat: Boolean = false,
    val durationMinutes: Int = 10,
)

data class CommandResult(
    val success: Boolean,
    val transactionId: String? = null,
    val message: String? = null,
    val error: String? = null,
)

sealed class BluelinkError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class AuthError(message: String, cause: Throwable? = null) : BluelinkError(message, cause)
    class NetworkError(message: String, cause: Throwable? = null) : BluelinkError(message, cause)
    class ApiError(val code: Int, message: String) : BluelinkError(message)
    class CommandTimeout(message: String = "Command timed out") : BluelinkError(message)
    class RegionNotSupported(region: BluelinkRegion) : BluelinkError("Region $region not supported")
    class VehicleNotFound(vin: String) : BluelinkError("Vehicle $vin not found")
    class TokenExpired : BluelinkError("Session expired, please log in again")
    class RateLimited(message: String = "Too many requests, please wait") : BluelinkError(message)
    class ServerError(val statusCode: Int, message: String) : BluelinkError(message)
}
