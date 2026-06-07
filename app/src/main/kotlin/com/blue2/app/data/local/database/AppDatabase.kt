package com.blue2.app.data.local.database

import androidx.room.*
import com.blue2.app.domain.models.*
import kotlinx.coroutines.flow.Flow

// ─── Entities ───────────────────────────────────────────────────────────────

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey val vin: String,
    val vehicleId: String,
    val nickname: String,
    val modelName: String,
    val modelYear: String,
    val modelCode: String,
    val licensePlate: String,
    val fuelType: String,
    val region: String,
    val generation: Int,
    val customImageUri: String?,
)

@Entity(tableName = "vehicle_status")
data class VehicleStatusEntity(
    @PrimaryKey val vin: String,
    val timestamp: Long,
    val isLocked: Boolean,
    val doorFrontLeft: String,
    val doorFrontRight: String,
    val doorRearLeft: String,
    val doorRearRight: String,
    val trunkOpen: Boolean,
    val hoodOpen: Boolean,
    val engineRunning: Boolean,
    val evBatteryPercent: Int?,
    val evRangeKm: Double?,
    val evCharging: Boolean,
    val evChargeTargetPercent: Int?,
    val evPluggedIn: Boolean,
    val estimatedChargingMinutes: Int?,
    val fuelLevelPercent: Int?,
    val fuelRangeKm: Double?,
    val climateOn: Boolean,
    val interiorTempC: Double?,
    val exteriorTempC: Double?,
    val targetTempC: Double?,
    val defrostFront: Boolean,
    val defrostRear: Boolean,
    val latitude: Double?,
    val longitude: Double?,
    val heading: Int?,
    val odometer: Double?,
    val tirePressureFrontLeft: Int?,
    val tirePressureFrontRight: Int?,
    val tirePressureRearLeft: Int?,
    val tirePressureRearRight: Int?,
    val tirePressureWarning: Boolean,
    val batteryVoltage: Double?,
    val rawJson: String?,
)

@Entity(tableName = "vehicle_custom_images")
data class VehicleCustomImageEntity(
    @PrimaryKey val vin: String,
    val imageUri: String,
)

@Entity(tableName = "auto_lock_config")
data class AutoLockConfigEntity(
    @PrimaryKey val vin: String,
    val enabled: Boolean,
    val bluetoothDeviceName: String?,
    val bluetoothDeviceAddress: String?,
    val delaySeconds: Int = 30,
)

// ─── DAOs ─────────────────────────────────────────────────────────────────────

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY nickname ASC")
    fun observeAll(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles ORDER BY nickname ASC")
    suspend fun getAll(): List<VehicleEntity>

    @Query("SELECT * FROM vehicles WHERE vin = :vin")
    suspend fun getByVin(vin: String): VehicleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vehicles: List<VehicleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vehicle: VehicleEntity)

    @Query("UPDATE vehicles SET customImageUri = :uri WHERE vin = :vin")
    suspend fun updateCustomImage(vin: String, uri: String?)

    @Query("DELETE FROM vehicles WHERE vin NOT IN (:vins)")
    suspend fun deleteNotIn(vins: List<String>)

    @Query("DELETE FROM vehicles")
    suspend fun deleteAll()
}

@Dao
interface VehicleStatusDao {
    @Query("SELECT * FROM vehicle_status WHERE vin = :vin")
    fun observe(vin: String): Flow<VehicleStatusEntity?>

    @Query("SELECT * FROM vehicle_status WHERE vin = :vin")
    suspend fun get(vin: String): VehicleStatusEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(status: VehicleStatusEntity)

    @Query("DELETE FROM vehicle_status WHERE vin = :vin")
    suspend fun delete(vin: String)
}

@Dao
interface AutoLockConfigDao {
    @Query("SELECT * FROM auto_lock_config WHERE vin = :vin")
    fun observe(vin: String): Flow<AutoLockConfigEntity?>

    @Query("SELECT * FROM auto_lock_config WHERE enabled = 1")
    suspend fun getEnabledConfigs(): List<AutoLockConfigEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: AutoLockConfigEntity)

    @Query("DELETE FROM auto_lock_config WHERE vin = :vin")
    suspend fun delete(vin: String)
}

// ─── Database ─────────────────────────────────────────────────────────────────

@Database(
    entities = [
        VehicleEntity::class,
        VehicleStatusEntity::class,
        VehicleCustomImageEntity::class,
        AutoLockConfigEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun vehicleStatusDao(): VehicleStatusDao
    abstract fun autoLockConfigDao(): AutoLockConfigDao
}

// ─── Mappers ─────────────────────────────────────────────────────────────────

fun VehicleEntity.toDomain() = Vehicle(
    vin = vin,
    vehicleId = vehicleId,
    nickname = nickname,
    modelName = modelName,
    modelYear = modelYear,
    modelCode = modelCode,
    licensePlate = licensePlate,
    fuelType = FuelType.valueOf(fuelType),
    region = BluelinkRegion.valueOf(region),
    generation = generation,
    customImageUri = customImageUri,
)

fun Vehicle.toEntity() = VehicleEntity(
    vin = vin,
    vehicleId = vehicleId,
    nickname = nickname,
    modelName = modelName,
    modelYear = modelYear,
    modelCode = modelCode,
    licensePlate = licensePlate,
    fuelType = fuelType.name,
    region = region.name,
    generation = generation,
    customImageUri = customImageUri,
)

fun VehicleStatusEntity.toDomain() = VehicleStatus(
    vin = vin,
    timestamp = timestamp,
    isLocked = isLocked,
    doorFrontLeft = DoorState.valueOf(doorFrontLeft),
    doorFrontRight = DoorState.valueOf(doorFrontRight),
    doorRearLeft = DoorState.valueOf(doorRearLeft),
    doorRearRight = DoorState.valueOf(doorRearRight),
    trunkOpen = trunkOpen,
    hoodOpen = hoodOpen,
    engineRunning = engineRunning,
    evBatteryPercent = evBatteryPercent,
    evRangeKm = evRangeKm,
    evCharging = evCharging,
    evChargeTargetPercent = evChargeTargetPercent,
    evPluggedIn = evPluggedIn,
    estimatedChargingMinutes = estimatedChargingMinutes,
    fuelLevelPercent = fuelLevelPercent,
    fuelRangeKm = fuelRangeKm,
    climateOn = climateOn,
    interiorTempC = interiorTempC,
    exteriorTempC = exteriorTempC,
    targetTempC = targetTempC,
    defrostFront = defrostFront,
    defrostRear = defrostRear,
    latitude = latitude,
    longitude = longitude,
    heading = heading,
    odometer = odometer,
    tirePressureFrontLeft = tirePressureFrontLeft,
    tirePressureFrontRight = tirePressureFrontRight,
    tirePressureRearLeft = tirePressureRearLeft,
    tirePressureRearRight = tirePressureRearRight,
    tirePressureWarning = tirePressureWarning,
    batteryVoltage = batteryVoltage,
    rawJson = rawJson,
)

fun VehicleStatus.toEntity() = VehicleStatusEntity(
    vin = vin,
    timestamp = timestamp,
    isLocked = isLocked,
    doorFrontLeft = doorFrontLeft.name,
    doorFrontRight = doorFrontRight.name,
    doorRearLeft = doorRearLeft.name,
    doorRearRight = doorRearRight.name,
    trunkOpen = trunkOpen,
    hoodOpen = hoodOpen,
    engineRunning = engineRunning,
    evBatteryPercent = evBatteryPercent,
    evRangeKm = evRangeKm,
    evCharging = evCharging,
    evChargeTargetPercent = evChargeTargetPercent,
    evPluggedIn = evPluggedIn,
    estimatedChargingMinutes = estimatedChargingMinutes,
    fuelLevelPercent = fuelLevelPercent,
    fuelRangeKm = fuelRangeKm,
    climateOn = climateOn,
    interiorTempC = interiorTempC,
    exteriorTempC = exteriorTempC,
    targetTempC = targetTempC,
    defrostFront = defrostFront,
    defrostRear = defrostRear,
    latitude = latitude,
    longitude = longitude,
    heading = heading,
    odometer = odometer,
    tirePressureFrontLeft = tirePressureFrontLeft,
    tirePressureFrontRight = tirePressureFrontRight,
    tirePressureRearLeft = tirePressureRearLeft,
    tirePressureRearRight = tirePressureRearRight,
    tirePressureWarning = tirePressureWarning,
    batteryVoltage = batteryVoltage,
    rawJson = rawJson,
)
