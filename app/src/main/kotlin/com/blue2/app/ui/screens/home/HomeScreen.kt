package com.blue2.app.ui.screens.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blue2.app.domain.models.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

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
    var showReorderSheet by remember { mutableStateOf(false) }
    var showLocationSheet by remember { mutableStateOf<Pair<Double, Double>?>(null) }

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
    LaunchedEffect(pagerState.currentPage) { viewModel.setPage(pagerState.currentPage) }
    LaunchedEffect(state.currentPage) {
        if (pagerState.currentPage != state.currentPage) pagerState.scrollToPage(state.currentPage)
    }

    val currentVehicle = state.vehicles.getOrNull(pagerState.currentPage)

    // Show location when findCar result arrives
    LaunchedEffect(state.carLocation) {
        currentVehicle?.let { v -> state.carLocation[v.vin]?.let { showLocationSheet = it } }
    }

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
                        IconButton(onClick = { currentVehicle?.let { viewModel.refreshStatus(it.vin, forceRefresh = true) } }) {
                            Icon(Icons.Rounded.Refresh, "Refresh")
                        }
                    }
                    IconButton(onClick = { showReorderSheet = true }) {
                        Icon(Icons.Rounded.Tune, "Customize controls")
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
                        pageIndex = page,
                        distanceUnit = state.distanceUnit,
                        tempUnit = state.tempUnit,
                        commandOrder = state.commandOrder,
                        viewModel = viewModel,
                        onStartClimate = { showClimateDialog = vehicle.vin },
                        onSetChargeTarget = { showChargeTargetDialog = vehicle.vin },
                    )
                }
            }
        }
    }

    // Dialogs
    showClimateDialog?.let { vin ->
        val vehicle = state.vehicles.find { it.vin == vin }
        ClimateDialog(
            tempUnit = state.tempUnit,
            onDismiss = { showClimateDialog = null },
            onConfirm = { settings ->
                showClimateDialog = null
                if (vehicle?.fuelType == FuelType.GASOLINE) viewModel.startEngine(vin, settings)
                else viewModel.startClimate(vin, settings)
            },
        )
    }
    showChargeTargetDialog?.let { vin ->
        ChargeTargetDialog(
            onDismiss = { showChargeTargetDialog = null },
            onConfirm = { ac, dc -> showChargeTargetDialog = null; viewModel.setChargeTarget(vin, ac, dc) },
        )
    }
    if (showReorderSheet) {
        CommandReorderSheet(
            currentOrder = state.commandOrder,
            onDismiss = { showReorderSheet = false },
            onSave = { newOrder -> viewModel.saveCommandOrder(newOrder); showReorderSheet = false },
        )
    }
    showLocationSheet?.let { (lat, lon) ->
        LocationSheet(lat, lon, onDismiss = { showLocationSheet = null })
    }
}

// ─── Per-vehicle page ─────────────────────────────────────────────────────────

@Composable
private fun VehiclePage(
    vehicle: Vehicle,
    status: VehicleStatus?,
    vehicleCount: Int,
    pageIndex: Int,
    distanceUnit: String,
    tempUnit: String,
    commandOrder: List<String>,
    viewModel: HomeViewModel,
    onStartClimate: () -> Unit,
    onSetChargeTarget: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (vehicleCount > 1) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    repeat(vehicleCount) { i ->
                        Surface(
                            modifier = Modifier.padding(horizontal = 3.dp).size(if (i == pageIndex) 10.dp else 7.dp),
                            shape = MaterialTheme.shapes.extraSmall,
                            color = if (i == pageIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        ) {}
                    }
                }
            }
        }

        item { StatusCard(vehicle, status, distanceUnit) }
        item { LockUnlockRow(vehicle.vin, status?.isLocked, viewModel) }
        item {
            CommandGrid(
                vehicle = vehicle,
                status = status,
                commandOrder = commandOrder,
                viewModel = viewModel,
                onStartClimate = onStartClimate,
                onSetChargeTarget = onSetChargeTarget,
            )
        }
        if (status?.climateOn == true || status?.engineRunning == true) {
            item { ClimateStatusCard(status, tempUnit) }
        }
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
                    Text("${vehicle.modelYear} ${vehicle.modelName}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (status != null) LockBadge(status.isLocked)
            }

            if (status == null) {
                Text("No status — tap refresh to load", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }

            when (vehicle.fuelType) {
                FuelType.ELECTRIC, FuelType.PHEV -> {
                    status.evBatteryPercent?.let { pct ->
                        LevelBar("Battery", pct, "~${formatDist(status.evRangeKm ?: 0.0, distanceUnit)} range", batteryColor(pct))
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
                if (status.lightsOn) add("Lights on")
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
                null, Modifier.size(16.dp),
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
    color: Color = MaterialTheme.colorScheme.primary,
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
    val errorColor = MaterialTheme.colorScheme.error
    val outlineBorderColor = MaterialTheme.colorScheme.outline

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
            border = if (isLocked == false)
                ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(errorColor))
            else
                ButtonDefaults.outlinedButtonBorder(enabled = !unlockLoading).copy(brush = SolidColor(outlineBorderColor)),
        ) {
            if (unlockLoading) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.LockOpen, null, Modifier.size(28.dp),
                        tint = if (isLocked == false) errorColor else LocalContentColor.current,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Unlock",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isLocked == false) errorColor else LocalContentColor.current,
                    )
                }
            }
        }
    }
}

// ─── Command definitions ──────────────────────────────────────────────────────

private data class CommandDef(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val applicableTo: Set<FuelType> = FuelType.entries.toSet(),
)

private val ALL_COMMANDS = listOf(
    CommandDef("engine",      "Start Engine",     Icons.Rounded.PlayArrow,         setOf(FuelType.GASOLINE, FuelType.HYBRID)),
    CommandDef("climate",     "Start Climate",    Icons.Rounded.AcUnit,            setOf(FuelType.ELECTRIC, FuelType.PHEV, FuelType.HYBRID)),
    CommandDef("charge",      "Start Charge",     Icons.Rounded.BatteryChargingFull, setOf(FuelType.ELECTRIC, FuelType.PHEV)),
    CommandDef("chargeTarget","Charge Target",    Icons.Rounded.Tune,              setOf(FuelType.ELECTRIC, FuelType.PHEV)),
    CommandDef("trunk",       "Trunk",            Icons.Rounded.DirectionsCar),
    CommandDef("lights",      "Flash Lights",     Icons.Rounded.FlashlightOn),
    CommandDef("horn",        "Honk Horn",        Icons.AutoMirrored.Rounded.VolumeUp),
    CommandDef("findCar",     "Find My Car",      Icons.Rounded.LocationOn),
)

// ─── Command Grid ─────────────────────────────────────────────────────────────

@Composable
private fun CommandGrid(
    vehicle: Vehicle,
    status: VehicleStatus?,
    commandOrder: List<String>,
    viewModel: HomeViewModel,
    onStartClimate: () -> Unit,
    onSetChargeTarget: () -> Unit,
) {
    val vin = vehicle.vin

    // Build the ordered, applicable commands with dynamic label/action based on status
    val commands = commandOrder.mapNotNull { key ->
        val def = ALL_COMMANDS.find { it.key == key } ?: return@mapNotNull null
        if (vehicle.fuelType !in def.applicableTo) return@mapNotNull null
        when (key) {
            "engine" -> if (status?.engineRunning == true)
                Triple("Stop Engine", Icons.Rounded.Stop, Runnable { viewModel.stopEngine(vin) })
            else Triple("Start Engine", Icons.Rounded.PlayArrow, Runnable { onStartClimate() })
            "climate" -> if (status?.climateOn == true)
                Triple("Stop Climate", Icons.Rounded.Stop, Runnable { viewModel.stopClimate(vin) })
            else Triple("Start Climate", Icons.Rounded.AcUnit, Runnable { onStartClimate() })
            "charge" -> if (status?.evCharging == true)
                Triple("Stop Charge", Icons.Rounded.BatteryAlert, Runnable { viewModel.stopCharge(vin) })
            else Triple("Start Charge", Icons.Rounded.BatteryChargingFull, Runnable { viewModel.startCharge(vin) })
            "chargeTarget" -> Triple(def.label, def.icon, Runnable { onSetChargeTarget() })
            "trunk" -> if (status?.trunkOpen == true)
                Triple("Close Trunk", Icons.Rounded.DirectionsCar, Runnable { viewModel.closeTrunk(vin) })
            else Triple("Open Trunk", Icons.Rounded.DirectionsCar, Runnable { viewModel.openTrunk(vin) })
            "lights" -> Triple(def.label, def.icon, Runnable { viewModel.flashLights(vin) })
            "horn" -> Triple(def.label, def.icon, Runnable { viewModel.honkHorn(vin) })
            "findCar" -> Triple(def.label, def.icon, Runnable { viewModel.findCar(vin) })
            else -> null
        }?.let { (label, icon, action) -> CommandItem(label, icon, key, action::run) }
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
                            if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            else Icon(cmd.icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(cmd.label, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private data class CommandItem(val label: String, val icon: ImageVector, val key: String, val action: () -> Unit)

// ─── Reorder Bottom Sheet ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommandReorderSheet(
    currentOrder: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit,
) {
    var orderedKeys by remember { mutableStateOf(currentOrder) }
    var dragIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val itemHeightPx = with(density) { 60.dp.toPx() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Customize Controls", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { onSave(orderedKeys) }) { Text("Save") }
            }
            Text("Hold and drag ☰ to reorder", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))

            Box {
                Column {
                    orderedKeys.forEachIndexed { index, key ->
                        val def = ALL_COMMANDS.find { it.key == key } ?: return@forEachIndexed
                        val isDragging = index == dragIndex

                        val targetIndex = if (dragIndex >= 0)
                            (dragIndex + dragOffsetY / itemHeightPx).roundToInt().coerceIn(0, orderedKeys.size - 1)
                        else -1

                        val visualOffsetY = when {
                            isDragging -> dragOffsetY
                            dragIndex >= 0 && dragIndex < targetIndex && index in (dragIndex + 1)..targetIndex -> -itemHeightPx
                            dragIndex >= 0 && dragIndex > targetIndex && index in targetIndex until dragIndex -> itemHeightPx
                            else -> 0f
                        }
                        val animatedOffset by animateFloatAsState(
                            if (isDragging) visualOffsetY else visualOffsetY,
                            label = "drag_$key",
                        )

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .offset { IntOffset(0, animatedOffset.roundToInt()) }
                                .zIndex(if (isDragging) 1f else 0f),
                            color = if (isDragging) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(def.icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(def.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                Icon(
                                    Icons.Rounded.DragHandle,
                                    "Drag to reorder",
                                    modifier = Modifier
                                        .size(24.dp)
                                        .pointerInput(key) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { dragIndex = index; dragOffsetY = 0f },
                                                onDrag = { _, amt -> dragOffsetY += amt.y },
                                                onDragEnd = {
                                                    val newIdx = (dragIndex + dragOffsetY / itemHeightPx).roundToInt().coerceIn(0, orderedKeys.size - 1)
                                                    orderedKeys = orderedKeys.toMutableList().apply {
                                                        val item = removeAt(dragIndex)
                                                        add(newIdx, item)
                                                    }
                                                    dragIndex = -1; dragOffsetY = 0f
                                                },
                                                onDragCancel = { dragIndex = -1; dragOffsetY = 0f },
                                            )
                                        },
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (index < orderedKeys.size - 1) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── Location Bottom Sheet ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationSheet(lat: Double, lon: Double, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
                Text("Car Location", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Latitude:  ${String.format("%.6f", lat)}", style = MaterialTheme.typography.bodyMedium)
                    Text("Longitude: ${String.format("%.6f", lon)}", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text(
                "Open in Maps to see your car's exact position",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
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
                val toDisplay: (Double) -> Int = { if (tempUnit == "F") (it * 9 / 5 + 32).toInt() else it.toInt() }
                val unit = if (tempUnit == "F") "°F" else "°C"
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TempChip("Interior", toDisplay(t), unit)
                    status.exteriorTempC?.let { TempChip("Exterior", toDisplay(it), unit) }
                }
            }
            val chips = buildList {
                if (status.defrostFront) add("Front Defrost")
                if (status.defrostRear) add("Rear Defrost")
                if (status.steeringWheelHeat) add("Wheel Heat")
            }
            if (chips.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    chips.forEach { ActiveChip(it) }
                }
            }
            val anySeatHeat = listOf(status.seatHeatFrontLeft, status.seatHeatFrontRight, status.seatHeatRearLeft, status.seatHeatRearRight)
                .any { it != SeatHeatingLevel.OFF }
            if (anySeatHeat) SeatHeatGrid(status)
        }
    }
}

@Composable
private fun TempChip(label: String, value: Int, unit: String) {
    Surface(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text("$value$unit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
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
            Text(
                when (level) { SeatHeatingLevel.OFF -> "–"; SeatHeatingLevel.LOW -> "Lo"; SeatHeatingLevel.MEDIUM -> "Med"; SeatHeatingLevel.HIGH -> "Hi" },
                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
            )
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

            val alerts = buildList {
                if (status.tirePressureWarning) add("Tire Pressure" to true)
                if (status.lowWasherFluid) add("Washer Fluid Low" to false)
                if (status.lowCoolant) add("Low Coolant" to true)
                if (status.brakeFluidLow) add("Brake Fluid Low" to true)
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
                                Icon(Icons.Rounded.Warning, null, Modifier.size(16.dp),
                                    tint = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer)
                                Text(msg, style = MaterialTheme.typography.bodySmall,
                                    color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                        }
                    }
                }
            }

            val metrics = buildList {
                status.odometer?.let { add("Odometer" to formatDist(it, distanceUnit)) }
                status.batteryVoltage?.let { add("12V Battery" to "${String.format("%.1f", it)}V") }
                status.engineOilLife?.let { add("Oil Life" to "$it%") }
                status.maintenanceDueKm?.let { add("Next Service" to formatDist(it, distanceUnit)) }
            }
            if (metrics.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    metrics.forEach { (label, value) ->
                        Surface(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }

            if (status.tirePressureFrontLeft != null || status.tirePressureFrontRight != null ||
                status.tirePressureRearLeft != null || status.tirePressureRearRight != null) {
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
private fun RowScope.TirePressureCell(pos: String, psi: Int?, warn: Boolean) {
    Surface(
        modifier = Modifier.weight(1f),
        color = if (warn && psi != null && psi < 30) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
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
                        Text("Temperature"); Text("$displayTemp$unit", fontWeight = FontWeight.Bold)
                    }
                    Slider(value = tempC, onValueChange = { tempC = it }, valueRange = 16f..30f, steps = 27)
                }
                ClimateToggleRow("Front Defrost", defrostFront) { defrostFront = it }
                ClimateToggleRow("Rear Defrost", defrostRear) { defrostRear = it }
                ClimateToggleRow("Steering Wheel Heat", steeringWheelHeat) { steeringWheelHeat = it }
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
private fun ClimateToggleRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

@Composable
private fun SeatHeatSelector(label: String, current: SeatHeatingLevel, onSelect: (SeatHeatingLevel) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SeatHeatingLevel.entries.forEach { level ->
                FilterChip(
                    selected = current == level,
                    onClick = { onSelect(level) },
                    label = { Text(when (level) { SeatHeatingLevel.OFF -> "Off"; SeatHeatingLevel.LOW -> "Lo"; SeatHeatingLevel.MEDIUM -> "Med"; SeatHeatingLevel.HIGH -> "Hi" }) },
                )
            }
        }
    }
}

// ─── Charge Target Dialog ─────────────────────────────────────────────────────

@Composable
private fun ChargeTargetDialog(onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    var acTarget by remember { mutableFloatStateOf(80f) }
    var dcTarget by remember { mutableFloatStateOf(80f) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Charge Target") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("AC Charging"); Text("${acTarget.toInt()}%", fontWeight = FontWeight.Bold)
                }
                Slider(value = acTarget, onValueChange = { acTarget = it }, valueRange = 50f..100f, steps = 9)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("DC Charging"); Text("${dcTarget.toInt()}%", fontWeight = FontWeight.Bold)
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
    return "${v.toInt()} ${if (unit == "mi") "mi" else "km"}"
}
