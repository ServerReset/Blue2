package com.blue2.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

    val pagerState = rememberPagerState { state.vehicles.size.coerceAtLeast(1) }

    // Keep pagerState and viewModel page in sync
    LaunchedEffect(pagerState.currentPage) {
        viewModel.setPage(pagerState.currentPage)
    }
    LaunchedEffect(state.currentPage) {
        if (pagerState.currentPage != state.currentPage) {
            pagerState.scrollToPage(state.currentPage)
        }
    }

    val currentVehicle = state.vehicles.getOrNull(pagerState.currentPage)
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
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize().padding(padding),
                ) { page ->
                    val vehicle = state.vehicles.getOrNull(page) ?: return@HorizontalPager
                    val status = state.statuses[vehicle.vin]
                    VehiclePage(
                        vehicle = vehicle,
                        status = status,
                        vehicleCount = state.vehicles.size,
                        currentPage = page,
                        distanceUnit = state.distanceUnit,
                        tempUnit = state.tempUnit,
                        viewModel = viewModel,
                        onStartClimate = { showClimateDialog = vehicle.vin },
                        onSetChargeTarget = { showChargeTargetDialog = vehicle.vin },
                    )
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

// ─── Per-vehicle page ─────────────────────────────────────────────────────────

@Composable
private fun VehiclePage(
    vehicle: Vehicle,
    status: VehicleStatus?,
    vehicleCount: Int,
    currentPage: Int,
    distanceUnit: String,
    tempUnit: String,
    viewModel: HomeViewModel,
    onStartClimate: () -> Unit,
    onSetChargeTarget: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Page indicator dots (only when >1 vehicle)
        if (vehicleCount > 1) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    repeat(vehicleCount) { i ->
                        Box(
                            Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (i == currentPage) 8.dp else 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = MaterialTheme.shapes.extraSmall,
                                color = if (i == currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            ) {}
                        }
                    }
                }
            }
        }

        // Status card
        item { StatusCard(vehicle, status, distanceUnit) }

        // Big Lock / Unlock buttons
        item { LockUnlockRow(vehicle.vin, status?.isLocked, viewModel) }

        // Quick commands
        item {
            CommandGrid(
                vehicle = vehicle,
                status = status,
                viewModel = viewModel,
                onStartClimate = onStartClimate,
                onSetChargeTarget = onSetChargeTarget,
            )
        }

        // Climate status (when active)
        if (status?.climateOn == true || status?.engineRunning == true) {
            item { ClimateStatusCard(status, tempUnit) }
        }

        // Advanced diagnostics
        if (status != null) {
            item { DiagnosticsCard(status, distanceUnit) }
        }
    }
}

// ─── Status Card ─────────────────────────────────────────────────────────────

@Composable
private fun StatusCard(vehicle: Vehicle, status: VehicleStatus?, distanceUnit: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(vehicle.nickname, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "${vehicle.modelYear} ${vehicle.modelName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (status != null) {
                    LockBadge(status.isLocked)
                }
            }

            if (status == null) {
                Text("No status — tap refresh to load", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }

            when (vehicle.fuelType) {
                FuelType.ELECTRIC, FuelType.PHEV -> {
                    status.evBatteryPercent?.let { pct ->
                        val km = status.evRangeKm ?: 0.0
                        LevelBar(
                            label = "Battery",
                            value = pct,
                            detail = "~${formatDist(km, distanceUnit)} range",
                            color = batteryColor(pct),
                        )
                    }
                    if (vehicle.fuelType == FuelType.PHEV) {
                        status.fuelLevelPercent?.let { pct ->
                            LevelBar("Fuel", pct, "~${formatDist(status.fuelRangeKm ?: 0.0, distanceUnit)} range")
                        }
                    }
                }
                FuelType.GASOLINE, FuelType.HYBRID -> {
                    status.fuelLevelPercent?.let { pct ->
                        LevelBar("Fuel", pct, "~${formatDist(status.fuelRangeKm ?: 0.0, distanceUnit)} range")
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
                Text(indicators.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            val fmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
            Text("Updated ${fmt.format(Date(status.timestamp))}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun LockBadge(locked: Boolean) {
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
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

// ─── Big Lock / Unlock ────────────────────────────────────────────────────────

@Composable
private fun LockUnlockRow(vin: String, isLocked: Boolean?, viewModel: HomeViewModel) {
    val lockLoading = viewModel.isCommandLoading(vin, "lock")
    val unlockLoading = viewModel.isCommandLoading(vin, "unlock")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = { viewModel.lock(vin) },
            enabled = !lockLoading,
            modifier = Modifier.weight(1f).height(80.dp),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isLocked == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                contentColor = if (isLocked == true) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        ) {
            if (lockLoading) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Lock, null, Modifier.size(28.dp))
                    Spacer(Modifier.height(4.dp))
                    Text("Lock", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        OutlinedButton(
            onClick = { viewModel.unlock(vin) },
            enabled = !unlockLoading,
            modifier = Modifier.weight(1f).height(80.dp),
            shape = MaterialTheme.shapes.large,
            border = ButtonDefaults.outlinedButtonBorder(enabled = !unlockLoading).let {
                if (isLocked == false) ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error)
                ) else it
            },
        ) {
            if (unlockLoading) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.LockOpen, null, Modifier.size(28.dp),
                        tint = if (isLocked == false) MaterialTheme.colorScheme.error else LocalContentColor.current,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Unlock",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isLocked == false) MaterialTheme.colorScheme.error else LocalContentColor.current,
                    )
                }
            }
        }
    }
}

// ─── Command Grid ─────────────────────────────────────────────────────────────

private data class CommandItem(val label: String, val icon: ImageVector, val key: String, val action: () -> Unit)

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
            if (status?.engineRunning == true)
                add(CommandItem("Stop Engine", Icons.Rounded.Stop, "engine") { viewModel.stopEngine(vin) })
            else
                add(CommandItem("Start Engine", Icons.Rounded.PlayArrow, "engine") { onStartClimate() })
        }
        if (isEv) {
            if (status?.climateOn == true)
                add(CommandItem("Stop Climate", Icons.Rounded.Stop, "climate") { viewModel.stopClimate(vin) })
            else
                add(CommandItem("Start Climate", Icons.Rounded.AcUnit, "climate") { onStartClimate() })
            if (status?.evCharging == true)
                add(CommandItem("Stop Charge", Icons.Rounded.BatteryAlert, "charge") { viewModel.stopCharge(vin) })
            else
                add(CommandItem("Start Charge", Icons.Rounded.BatteryChargingFull, "charge") { viewModel.startCharge(vin) })
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

// ─── Climate Status Card ──────────────────────────────────────────────────────

@Composable
private fun ClimateStatusCard(status: VehicleStatus, tempUnit: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.AcUnit, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Text("Climate Active", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }

            status.interiorTempC?.let { t ->
                val display = if (tempUnit == "F") (t * 9 / 5 + 32) else t
                val unit = if (tempUnit == "F") "°F" else "°C"
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TempChip("Interior", display, unit)
                    status.exteriorTempC?.let { ext ->
                        val extDisplay = if (tempUnit == "F") (ext * 9 / 5 + 32) else ext
                        TempChip("Exterior", extDisplay, unit)
                    }
                }
            }

            if (status.defrostFront || status.defrostRear || status.steeringWheelHeat) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (status.defrostFront) ActiveChip("Front Defrost")
                    if (status.defrostRear) ActiveChip("Rear Defrost")
                    if (status.steeringWheelHeat) ActiveChip("Wheel Heat")
                }
            }

            val anySeatHeat = listOf(
                status.seatHeatFrontLeft, status.seatHeatFrontRight,
                status.seatHeatRearLeft, status.seatHeatRearRight,
            ).any { it != SeatHeatingLevel.OFF }
            if (anySeatHeat) {
                SeatHeatGrid(status)
            }
        }
    }
}

@Composable
private fun TempChip(label: String, value: Double, unit: String) {
    Surface(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text("${value.toInt()}$unit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun ActiveChip(label: String) {
    Surface(color = MaterialTheme.colorScheme.secondary, shape = MaterialTheme.shapes.extraSmall) {
        Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondary)
    }
}

@Composable
private fun SeatHeatGrid(status: VehicleStatus) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Seat Heat", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SeatHeatCell("FL", status.seatHeatFrontLeft)
            SeatHeatCell("FR", status.seatHeatFrontRight)
            SeatHeatCell("RL", status.seatHeatRearLeft)
            SeatHeatCell("RR", status.seatHeatRearRight)
        }
    }
}

@Composable
private fun SeatHeatCell(pos: String, level: SeatHeatingLevel) {
    val label = when (level) {
        SeatHeatingLevel.OFF -> "–"
        SeatHeatingLevel.LOW -> "Lo"
        SeatHeatingLevel.MEDIUM -> "Med"
        SeatHeatingLevel.HIGH -> "Hi"
    }
    Surface(
        color = when (level) {
            SeatHeatingLevel.OFF -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            SeatHeatingLevel.LOW -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
            SeatHeatingLevel.MEDIUM -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
            SeatHeatingLevel.HIGH -> MaterialTheme.colorScheme.tertiary
        },
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(pos, style = MaterialTheme.typography.labelSmall)
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

// ─── Diagnostics Card ─────────────────────────────────────────────────────────

@Composable
private fun DiagnosticsCard(status: VehicleStatus, distanceUnit: String) {
    val hasData = status.odometer != null || status.tirePressureFrontLeft != null ||
            status.batteryVoltage != null || status.dtcCodes.isNotEmpty() ||
            status.lowWasherFluid || status.lowCoolant || status.brakeFluidLow ||
            status.tirePressureWarning || status.engineOilLife != null

    if (!hasData) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Build, null, tint = MaterialTheme.colorScheme.primary)
                Text("Diagnostics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            // Alerts row
            val alerts = buildList {
                if (status.tirePressureWarning) add("Tire Pressure" to true)
                if (status.lowWasherFluid) add("Washer Fluid" to false)
                if (status.lowCoolant) add("Low Coolant" to true)
                if (status.brakeFluidLow) add("Brake Fluid" to true)
                if (status.dtcCodes.isNotEmpty()) add("${status.dtcCodes.size} DTC Code${if (status.dtcCodes.size > 1) "s" else ""}" to true)
            }
            if (alerts.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    alerts.forEach { (msg, isError) ->
                        Surface(
                            color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Rounded.Warning, null, Modifier.size(16.dp),
                                    tint = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                                Text(
                                    msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                            }
                        }
                    }
                }
            }

            // Metrics grid
            val metrics = buildList {
                status.odometer?.let { add("Odometer" to "${formatDist(it, distanceUnit)}") }
                status.batteryVoltage?.let { add("12V Battery" to "${String.format("%.1f", it)}V") }
                status.engineOilLife?.let { add("Oil Life" to "$it%") }
                status.maintenanceDueKm?.let { add("Next Service" to "${formatDist(it, distanceUnit)}") }
            }
            if (metrics.isNotEmpty()) {
                DiagnosticsMetricsRow(metrics)
            }

            // Tire pressure grid
            if (status.tirePressureFrontLeft != null || status.tirePressureFrontRight != null ||
                status.tirePressureRearLeft != null || status.tirePressureRearRight != null
            ) {
                TirePressureGrid(status)
            }

            // DTC codes
            if (status.dtcCodes.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("DTC Codes", style = MaterialTheme.typography.labelMedium)
                    status.dtcCodes.forEach { code ->
                        Text(code, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsMetricsRow(metrics: List<Pair<String, String>>) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        metrics.forEach { (label, value) ->
            Surface(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
            ) {
                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun TirePressureGrid(status: VehicleStatus) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Tire Pressure (PSI)", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TirePressureCell("FL", status.tirePressureFrontLeft, status.tirePressureWarning)
            TirePressureCell("FR", status.tirePressureFrontRight, status.tirePressureWarning)
            TirePressureCell("RL", status.tirePressureRearLeft, status.tirePressureWarning)
            TirePressureCell("RR", status.tirePressureRearRight, status.tirePressureWarning)
        }
    }
}

@Composable
private fun RowScope.TirePressureCell(pos: String, psi: Int?, warning: Boolean) {
    Surface(
        modifier = Modifier.weight(1f),
        color = if (warning && psi != null && psi < 30) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(pos, style = MaterialTheme.typography.labelSmall)
            Text(psi?.toString() ?: "–", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

// ─── Climate Dialog ───────────────────────────────────────────────────────────

@Composable
private fun ClimateDialog(tempUnit: String, onDismiss: () -> Unit, onConfirm: (ClimateSettings) -> Unit) {
    var tempC by remember { mutableFloatStateOf(22f) }
    var defrostFront by remember { mutableStateOf(false) }
    var defrostRear by remember { mutableStateOf(false) }
    var steeringWheelHeat by remember { mutableStateOf(false) }
    var driverSeat by remember { mutableStateOf(SeatHeatingLevel.OFF) }
    var passengerSeat by remember { mutableStateOf(SeatHeatingLevel.OFF) }

    val displayTemp = if (tempUnit == "F") (tempC * 9 / 5 + 32).toInt() else tempC.toInt()
    val unit = if (tempUnit == "F") "°F" else "°C"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Climate Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Temperature", style = MaterialTheme.typography.bodyMedium)
                        Text("$displayTemp$unit", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    Slider(value = tempC, onValueChange = { tempC = it }, valueRange = 16f..30f, steps = 27)
                }
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
                SeatHeatSelector("Driver Seat", driverSeat) { driverSeat = it }
                SeatHeatSelector("Passenger Seat", passengerSeat) { passengerSeat = it }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(ClimateSettings(
                    temperatureC = tempC.toDouble(),
                    defrostFront = defrostFront,
                    defrostRear = defrostRear,
                    steeringWheelHeat = steeringWheelHeat,
                    seatHeatFrontLeft = driverSeat,
                    seatHeatFrontRight = passengerSeat,
                ))
            }) { Text("Start") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SeatHeatSelector(label: String, current: SeatHeatingLevel, onSelect: (SeatHeatingLevel) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SeatHeatingLevel.entries.forEach { level ->
                FilterChip(
                    selected = current == level,
                    onClick = { onSelect(level) },
                    label = {
                        Text(when (level) {
                            SeatHeatingLevel.OFF -> "Off"
                            SeatHeatingLevel.LOW -> "Lo"
                            SeatHeatingLevel.MEDIUM -> "Med"
                            SeatHeatingLevel.HIGH -> "Hi"
                        })
                    },
                )
            }
        }
    }
}

// ─── Charge Target Dialog ─────────────────────────────────────────────────────

@Composable
private fun ChargeTargetDialog(onDismiss: () -> Unit, onConfirm: (acPct: Int, dcPct: Int) -> Unit) {
    var acTarget by remember { mutableFloatStateOf(80f) }
    var dcTarget by remember { mutableFloatStateOf(80f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Charge Target") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("AC Charging")
                    Text("${acTarget.toInt()}%", fontWeight = FontWeight.Bold)
                }
                Slider(value = acTarget, onValueChange = { acTarget = it }, valueRange = 50f..100f, steps = 9)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("DC Charging")
                    Text("${dcTarget.toInt()}%", fontWeight = FontWeight.Bold)
                }
                Slider(value = dcTarget, onValueChange = { dcTarget = it }, valueRange = 50f..100f, steps = 9)
            }
        },
        confirmButton = { Button(onClick = { onConfirm(acTarget.toInt(), dcTarget.toInt()) }) { Text("Set") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun batteryColor(pct: Int) = when {
    pct >= 50 -> MaterialTheme.colorScheme.primary
    pct >= 20 -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.error
}

private fun formatDist(km: Double, unit: String): String {
    val v = if (unit == "mi") km * 0.621371 else km
    val label = if (unit == "mi") "mi" else "km"
    return "${v.toInt()} $label"
}
