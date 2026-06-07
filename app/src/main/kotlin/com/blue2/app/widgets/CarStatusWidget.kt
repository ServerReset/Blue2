package com.blue2.app.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.material3.ColorProviders
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.blue2.app.R
import com.blue2.app.data.local.database.AppDatabase
import com.blue2.app.data.local.database.toDomain
import com.blue2.app.data.local.preferences.AppPreferences
import com.blue2.app.domain.models.FuelType
import com.blue2.app.domain.models.VehicleStatus
import com.blue2.app.domain.repository.IBluelinkRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun repository(): IBluelinkRepository
    fun database(): AppDatabase
    fun prefs(): AppPreferences
}

class CarStatusWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(180.dp, 110.dp),
            DpSize(300.dp, 140.dp),
            DpSize(300.dp, 220.dp),
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val ep = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val repo = ep.repository()
        val db = ep.database()
        val prefs = ep.prefs()

        val vin = prefs.lastSelectedVin.first()
            ?: db.vehicleDao().getAll().firstOrNull()?.vin

        val vehicle = vin?.let { db.vehicleDao().getByVin(it)?.toDomain() }
        val status = vin?.let { repo.getVehicleStatus(it, forceRefresh = false).getOrNull() }

        provideContent {
            GlanceTheme {
                if (vehicle == null || status == null) {
                    Box(
                        modifier = GlanceModifier.fillMaxSize().background(GlanceTheme.colors.surface),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("No vehicle", style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp))
                    }
                } else {
                    val size = LocalSize.current
                    when {
                        size.width >= 300.dp && size.height >= 200.dp -> LargeWidget(vehicle, status, vin)
                        size.width >= 300.dp -> MediumWidget(vehicle, status, vin)
                        else -> SmallWidget(vehicle, status)
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallWidget(vehicle: com.blue2.app.domain.models.Vehicle, status: VehicleStatus) {
    Column(
        modifier = GlanceModifier.fillMaxSize().background(GlanceTheme.colors.surface).padding(12.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(vehicle.nickname.take(14), style = TextStyle(
            color = GlanceTheme.colors.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        ))
        Spacer(GlanceModifier.height(4.dp))
        Text(
            if (status.isLocked) "🔒 Locked" else "🔓 Unlocked",
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
        )
        status.evBatteryPercent?.let {
            Text("⚡ $it%", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp))
        }
    }
}

@Composable
private fun MediumWidget(vehicle: com.blue2.app.domain.models.Vehicle, status: VehicleStatus, vin: String) {
    Row(
        modifier = GlanceModifier.fillMaxSize().background(GlanceTheme.colors.surface).padding(12.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Column(modifier = GlanceModifier.defaultWeight(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            Text(vehicle.nickname, style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.height(2.dp))
            Text(
                if (status.isLocked) "Locked" else "Unlocked",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
            )
            status.evBatteryPercent?.let {
                Text("Battery: $it%", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp))
            }
            status.fuelLevelPercent?.let {
                Text("Fuel: $it%", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp))
            }
        }
        Column(verticalAlignment = Alignment.Vertical.CenterVertically, horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
            Button(
                text = if (status.isLocked) "Unlock" else "Lock",
                onClick = actionRunCallback<ToggleLockCallback>(
                    parameters = actionParametersOf(VIN_KEY to vin, LOCKED_KEY to status.isLocked.toString())
                ),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = GlanceTheme.colors.primaryContainer,
                    contentColor = GlanceTheme.colors.onPrimaryContainer,
                ),
            )
        }
    }
}

@Composable
private fun LargeWidget(vehicle: com.blue2.app.domain.models.Vehicle, status: VehicleStatus, vin: String) {
    Column(
        modifier = GlanceModifier.fillMaxSize().background(GlanceTheme.colors.surface).padding(16.dp),
    ) {
        Text(vehicle.nickname, style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold))
        Text("${vehicle.modelYear} ${vehicle.modelName}", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp))

        Spacer(GlanceModifier.height(12.dp))

        // Range info
        status.evBatteryPercent?.let {
            Text("⚡ Battery: $it% · ${status.evRangeKm?.let { r -> "%.0f km".format(r) } ?: ""}",
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp))
        }
        status.fuelLevelPercent?.let {
            Text("⛽ Fuel: $it% · ${status.fuelRangeKm?.let { r -> "%.0f km".format(r) } ?: ""}",
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp))
        }

        Spacer(GlanceModifier.height(12.dp))

        // Control buttons
        Row {
            Button(
                text = if (status.isLocked) "Unlock" else "Lock",
                onClick = actionRunCallback<ToggleLockCallback>(
                    parameters = actionParametersOf(VIN_KEY to vin, LOCKED_KEY to status.isLocked.toString())
                ),
            )
            Spacer(GlanceModifier.width(8.dp))
            if (vehicle.fuelType == FuelType.ELECTRIC || vehicle.fuelType == FuelType.PHEV) {
                Button(
                    text = if (status.evCharging) "Stop Charge" else "Charge",
                    onClick = actionRunCallback<ToggleChargeCallback>(
                        parameters = actionParametersOf(VIN_KEY to vin, CHARGING_KEY to status.evCharging.toString())
                    ),
                )
            } else {
                Button(
                    text = if (status.engineRunning) "Stop" else "Start",
                    onClick = actionRunCallback<ToggleEngineCallback>(
                        parameters = actionParametersOf(VIN_KEY to vin, RUNNING_KEY to status.engineRunning.toString())
                    ),
                )
            }
        }
    }
}

// ─── Widget action callbacks ─────────────────────────────────────────────────

val VIN_KEY = ActionParameters.Key<String>("vin")
val LOCKED_KEY = ActionParameters.Key<String>("locked")
val CHARGING_KEY = ActionParameters.Key<String>("charging")
val RUNNING_KEY = ActionParameters.Key<String>("running")

class ToggleLockCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val vin = parameters[VIN_KEY] ?: return
        val isLocked = parameters[LOCKED_KEY]?.toBoolean() ?: true
        val ep = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        if (isLocked) ep.repository().unlock(vin) else ep.repository().lock(vin)
        CarStatusWidget().update(context, glanceId)
    }
}

class ToggleChargeCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val vin = parameters[VIN_KEY] ?: return
        val isCharging = parameters[CHARGING_KEY]?.toBoolean() ?: false
        val ep = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        if (isCharging) ep.repository().stopCharge(vin) else ep.repository().startCharge(vin)
        CarStatusWidget().update(context, glanceId)
    }
}

class ToggleEngineCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val vin = parameters[VIN_KEY] ?: return
        val isRunning = parameters[RUNNING_KEY]?.toBoolean() ?: false
        val ep = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        if (isRunning) ep.repository().stopEngine(vin) else ep.repository().startEngine(vin)
        CarStatusWidget().update(context, glanceId)
    }
}

class CarStatusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CarStatusWidget()
}
