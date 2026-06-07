package com.blue2.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blue2.app.domain.models.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showClimateDialog by remember { mutableStateOf<String?>(null) }
    var showChargeTargetDialog by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }
    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearSuccess()
        }
    }

    val currentVehicle = state.vehicles.getOrNull(state.currentPage)
    val currentStatus = currentVehicle?.let { state.statuses[it.vin] }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blue2", fontWeight = FontWeight.Bold) },
                actions = {
                    if (state.isRefreshing) {
                        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    } else {
                        IconButton(onClick = {
                            currentVehicle?.let { viewModel.refreshStatus(it.vin, forceRefresh = true) }
                        }) {
                            Icon(Icons.Rounded.Refresh, "Refresh")
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Rounded.Settings, "Settings")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.isLoadingVehicles && state.vehicles.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.vehicles.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.DirectionsCar, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("No vehicles found", style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(onClick = { viewModel.refreshAll() }) { Text("Retry") }
                    }
                }
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { currentVehicle?.let { viewModel.refreshStatus(it.vin, forceRefresh = true) } },
                    modifier = Modifier.fillMaxSize().padding(padding),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (state.vehicles.size > 1) {
                            item {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(state.vehicles) { vehicle ->
                                        FilterChip(
                                            selected = vehicle.vin == currentVehicle?.vin,
                                            onClick = { viewModel.setPage(state.vehicles.indexOf(vehicle)) },
                                            label = { Text(vehicle.nickname) },
                                            leadingIcon = if (vehicle.vin == currentVehicle?.vin) {
                                                { Icon(Icons.Rounded.Check, null, Modifier.size(16.dp)) }
                                            } else null,
                                        )
                                    }
                                }
                            }
                        }

                        currentVehicle?.let { vehicle ->
                            item {
                                StatusCard(
                                    vehicle = vehicle,
                                    status = currentStatus,
                                    distanceUnit = state.distanceUnit,
                                )
                            }
                            item {
                                LockUnlockRow(
                                    isLocked = currentStatus?.isLocked,
                                    lockLoading = viewModel.isCommandLoading(vehicle.vin, "lock"),
                                    unlockLoading = viewModel.isCommandLoading(vehicle.vin, "unlock"),
                                    onLock = { viewModel.lock(vehicle.vin) },
                                    onUnlock = { viewModel.unlock(vehicle.vin) },
                                )
                            }
                            item {
                                CommandGrid(
                                    vehicle = vehicle,
                                    status = currentStatus,
                                    viewModel = viewModel,
                                    onStartClimate = { showClimateDialog = vehicle.vin },
                                    onSetChargeTarget = { showChargeTargetDialog = vehicle.vin },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    showClimateDialog?.let { vin ->
        val vehicle = state.vehicles.find { it.vin == vin }
        ClimateDialog(
            tempUnit = state.tempUnit,
            onDismiss = { showClimateDialog = null },
            onConfirm = { settings ->
                showClimateDialog = null
                if (vehicle?.fuelType == FuelType.GASOLINE) {
                    viewModel.startEngine(vin, settings)
                } else {
                    viewModel.startClimate(vin, settings)
                }
            },
        )
    }

    showChargeTargetDialog?.let { vin ->
        ChargeTargetDialog(
            onDismiss = { showChargeTargetDialog = null },
            onConfirm = { ac, dc ->
                showChargeTargetDialog = null
                viewModel.setChargeTarget(vin, ac, dc)
            },
        )
    }
}

// ─── Status Card ─────────────────────────────────────────────────────────────

@Composable
private fun StatusCard(
    vehicle: Vehicle,
    status: VehicleStatus?,
    distanceUnit: String,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(vehicle.nickname, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("${vehicle.modelYear} ${vehicle.modelName}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (status != null) {
                    val locked = status.isLocked
                    Surface(
                        color = if (locked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                if (locked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                                null,
                                Modifier.size(16.dp),
                                tint = if (locked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Text(
                                if (locked) "Locked" else "Unlocked",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (locked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            }

            if (status == null) {
                Text("No status available — pull to refresh", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }

            when (vehicle.fuelType) {
                FuelType.ELECTRIC, FuelType.PHEV -> {
                    status.evBatteryPercent?.let { pct ->
                        LevelBar(
                            label = "Battery",
                            value = pct,
                            detail = run {
                                val km = status.evRangeKm ?: 0.0
                                val dist = if (distanceUnit == "mi") km * 0.621371 else km
                                val unit = if (distanceUnit == "mi") "mi" else "km"
                                "~${dist.toInt()} $unit range"
                            },
                            color = when {
                                pct >= 50 -> MaterialTheme.colorScheme.primary
                                pct >= 20 -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.error
                            },
                        )
                    }
                    if (vehicle.fuelType == FuelType.PHEV) {
                        status.fuelLevelPercent?.let { pct ->
                            LevelBar(
                                label = "Fuel",
                                value = pct,
                                detail = run {
                                    val km = status.fuelRangeKm ?: 0.0
                                    val dist = if (distanceUnit == "mi") km * 0.621371 else km
                                    val unit = if (distanceUnit == "mi") "mi" else "km"
                                    "~${dist.toInt()} $unit range"
                                },
                            )
                        }
                    }
                }
                FuelType.GASOLINE, FuelType.HYBRID -> {
                    status.fuelLevelPercent?.let { pct ->
                        LevelBar(
                            label = "Fuel",
                            value = pct,
                            detail = run {
                                val km = status.fuelRangeKm ?: 0.0
                                val dist = if (distanceUnit == "mi") km * 0.621371 else km
                                val unit = if (distanceUnit == "mi") "mi" else "km"
                                "~${dist.toInt()} $unit range"
                            },
                        )
                    }
                }
            }

            val indicators = buildList {
                if (status.evCharging) add("Charging")
                if (status.evPluggedIn && !status.evCharging) add("Plugged in")
                if (status.climateOn) add("Climate on")
                if (status.engineRunning) add("Engine running")
                if (status.trunkOpen) add("Trunk open")
                if (status.hoodOpen) add("Hood open")
                val openDoors = listOf(status.doorFrontLeft, status.doorFrontRight, status.doorRearLeft, status.doorRearRight)
                    .count { it == DoorState.OPEN }
                if (openDoors > 0) add("$openDoors door${if (openDoors > 1) "s" else ""} open")
            }
            if (indicators.isNotEmpty()) {
                Text(
                    indicators.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val fmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
            Text(
                "Updated ${fmt.format(Date(status.timestamp))}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun LevelBar(
    label: String,
    value: Int,
    detail: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text("$value%  ·  $detail", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(
            progress = { value / 100f },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

// ─── Lock / Unlock Row ────────────────────────────────────────────────────────

@Composable
private fun LockUnlockRow(
    isLocked: Boolean?,
    lockLoading: Boolean,
    unlockLoading: Boolean,
    onLock: () -> Unit,
    onUnlock: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onLock,
            enabled = !lockLoading,
            modifier = Modifier.weight(1f).height(52.dp),
        ) {
            if (lockLoading) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Icon(Icons.Rounded.Lock, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Lock")
            }
        }
        OutlinedButton(
            onClick = onUnlock,
            enabled = !unlockLoading,
            modifier = Modifier.weight(1f).height(52.dp),
        ) {
            if (unlockLoading) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Rounded.LockOpen, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Unlock")
            }
        }
    }
}

// ─── Command Grid ─────────────────────────────────────────────────────────────

private data class CommandItem(
    val label: String,
    val icon: ImageVector,
    val key: String,
    val action: () -> Unit,
)

@Composable
private fun CommandGrid(
    vehicle: Vehicle,
    status: VehicleStatus?,
    viewModel: HomeViewModel,
    onStartClimate: () -> Unit,
    onSetChargeTarget: () -> Unit,
) {
    val vin = vehicle.vin
    val isEv = vehicle.fuelType == FuelType.ELECTRIC || vehicle.fuelType == FuelType.PHEV
    val isIce = vehicle.fuelType == FuelType.GASOLINE || vehicle.fuelType == FuelType.HYBRID

    val commands = buildList {
        if (isIce) {
            if (status?.engineRunning == true) {
                add(CommandItem("Stop Engine", Icons.Rounded.Stop, "engine") { viewModel.stopEngine(vin) })
            } else {
                add(CommandItem("Start Engine", Icons.Rounded.PlayArrow, "engine") { onStartClimate() })
            }
        }
        if (isEv) {
            if (status?.climateOn == true) {
                add(CommandItem("Stop Climate", Icons.Rounded.Stop, "climate") { viewModel.stopClimate(vin) })
            } else {
                add(CommandItem("Start Climate", Icons.Rounded.AcUnit, "climate") { onStartClimate() })
            }
            if (status?.evCharging == true) {
                add(CommandItem("Stop Charge", Icons.Rounded.BatteryAlert, "charge") { viewModel.stopCharge(vin) })
            } else {
                add(CommandItem("Start Charge", Icons.Rounded.BatteryChargingFull, "charge") { viewModel.startCharge(vin) })
            }
            add(CommandItem("Charge Target", Icons.Rounded.Tune, "chargeTarget") { onSetChargeTarget() })
        }
        add(CommandItem("Flash Lights", Icons.Rounded.FlashlightOn, "lights") { viewModel.flashLights(vin) })
        add(CommandItem("Honk Horn", Icons.AutoMirrored.Rounded.VolumeUp, "horn") { viewModel.honkHorn(vin) })
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        commands.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { cmd ->
                    val loading = viewModel.isCommandLoading(vin, cmd.key)
                    OutlinedCard(
                        onClick = { if (!loading) cmd.action() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (loading) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(cmd.icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                            Text(cmd.label, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

// ─── Climate Dialog ───────────────────────────────────────────────────────────

@Composable
private fun ClimateDialog(
    tempUnit: String,
    onDismiss: () -> Unit,
    onConfirm: (ClimateSettings) -> Unit,
) {
    var tempC by remember { mutableFloatStateOf(22f) }
    var defrostFront by remember { mutableStateOf(false) }
    var defrostRear by remember { mutableStateOf(false) }
    var steeringWheelHeat by remember { mutableStateOf(false) }

    val displayTemp = if (tempUnit == "F") (tempC * 9 / 5 + 32).toInt() else tempC.toInt()
    val unit = if (tempUnit == "F") "°F" else "°C"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Climate Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Temperature: $displayTemp$unit", style = MaterialTheme.typography.bodyMedium)
                Slider(value = tempC, onValueChange = { tempC = it }, valueRange = 16f..30f, steps = 27)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Front Defrost")
                    Switch(checked = defrostFront, onCheckedChange = { defrostFront = it })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Rear Defrost")
                    Switch(checked = defrostRear, onCheckedChange = { defrostRear = it })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Steering Wheel Heat")
                    Switch(checked = steeringWheelHeat, onCheckedChange = { steeringWheelHeat = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(ClimateSettings(
                    temperatureC = tempC.toDouble(),
                    defrostFront = defrostFront,
                    defrostRear = defrostRear,
                    steeringWheelHeat = steeringWheelHeat,
                ))
            }) { Text("Start") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ─── Charge Target Dialog ─────────────────────────────────────────────────────

@Composable
private fun ChargeTargetDialog(
    onDismiss: () -> Unit,
    onConfirm: (acPct: Int, dcPct: Int) -> Unit,
) {
    var acTarget by remember { mutableFloatStateOf(80f) }
    var dcTarget by remember { mutableFloatStateOf(80f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Charge Target") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("AC Charging: ${acTarget.toInt()}%")
                Slider(value = acTarget, onValueChange = { acTarget = it }, valueRange = 50f..100f, steps = 9)
                Text("DC Charging: ${dcTarget.toInt()}%")
                Slider(value = dcTarget, onValueChange = { dcTarget = it }, valueRange = 50f..100f, steps = 9)
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(acTarget.toInt(), dcTarget.toInt()) }) { Text("Set") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
