package com.blue2.app.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.blue2.app.domain.models.*
import com.blue2.app.ui.components.*
import com.blue2.app.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onDiagnostics: (String) -> Unit,
    onSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Show errors / success toasts
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHost.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }
    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHost.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearSuccess()
        }
    }

    // Adaptive columns based on window width
    val windowInfo = calculateWindowSizeClass()
    val vehiclesPerPage = when {
        windowInfo.windowWidthSizeClass == WindowWidthSizeClass.Expanded -> 3
        windowInfo.windowWidthSizeClass == WindowWidthSizeClass.Medium -> 2
        else -> 1
    }

    val pages = if (state.vehicles.isEmpty()) 0
    else (state.vehicles.size + vehiclesPerPage - 1) / vehiclesPerPage

    val pagerState = rememberPagerState(pageCount = { pages })

    LaunchedEffect(pagerState.currentPage) { viewModel.setPage(pagerState.currentPage) }

    val pullRefreshState = rememberPullToRefreshState()
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(Unit) {
            viewModel.refreshAll()
            pullRefreshState.endRefresh()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text("Blue2", fontWeight = FontWeight.Bold) },
                actions = {
                    if (state.isLoadingVehicles || state.isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 8.dp), strokeWidth = 2.dp)
                    }
                    IconButton(onClick = { viewModel.refreshAll() }) {
                        Icon(Icons.Rounded.Refresh, "Refresh")
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Rounded.Settings, "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            PullToRefreshContainer(
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
            )

            when {
                state.isLoadingVehicles && state.vehicles.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            CircularProgressIndicator()
                            Text("Loading your vehicles…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                state.vehicles.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.DirectionsCar, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                            Text("No vehicles found", style = MaterialTheme.typography.titleMedium)
                            Text("Make sure your Bluelink account has enrolled vehicles.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedButton(onClick = { viewModel.refreshAll() }) { Text("Try Again") }
                        }
                    }
                }
                else -> {
                    Column(Modifier.fillMaxSize()) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.weight(1f),
                            pageSpacing = 0.dp,
                        ) { pageIndex ->
                            val startIdx = pageIndex * vehiclesPerPage
                            val pageVehicles = state.vehicles.drop(startIdx).take(vehiclesPerPage)

                            if (vehiclesPerPage == 1) {
                                val vehicle = pageVehicles.firstOrNull() ?: return@HorizontalPager
                                val pageOffset = (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
                                VehicleCard(
                                    vehicle = vehicle,
                                    status = state.statuses[vehicle.vin],
                                    viewModel = viewModel,
                                    onDiagnostics = onDiagnostics,
                                    distanceUnit = state.distanceUnit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            val pct = abs(pageOffset)
                                            scaleX = 1f - pct * 0.04f
                                            scaleY = 1f - pct * 0.04f
                                            alpha = 1f - pct * 0.3f
                                        },
                                )
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    pageVehicles.forEach { vehicle ->
                                        VehicleCard(
                                            vehicle = vehicle,
                                            status = state.statuses[vehicle.vin],
                                            viewModel = viewModel,
                                            onDiagnostics = onDiagnostics,
                                            distanceUnit = state.distanceUnit,
                                            modifier = Modifier.weight(1f).fillMaxHeight(),
                                        )
                                    }
                                    // Fill remaining space if last page has fewer vehicles
                                    repeat(vehiclesPerPage - pageVehicles.size) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        // Page indicator
                        if (pages > 1) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                repeat(pages) { i ->
                                    val isSelected = pagerState.currentPage == i
                                    val width by animateDpAsState(if (isSelected) 24.dp else 8.dp, label = "dot")
                                    Box(
                                        Modifier
                                            .padding(horizontal = 4.dp)
                                            .height(8.dp)
                                            .width(width)
                                            .clip(RoundedCornerShape(50))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outlineVariant
                                            )
                                            .clickable { scope.launch { pagerState.animateScrollToPage(i) } }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun calculateWindowSizeClass(): WindowSizeInfo {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val widthDp = configuration.screenWidthDp.dp
    return WindowSizeInfo(
        windowWidthSizeClass = when {
            widthDp >= 900.dp -> WindowWidthSizeClass.Expanded
            widthDp >= 600.dp -> WindowWidthSizeClass.Medium
            else -> WindowWidthSizeClass.Compact
        }
    )
}

private data class WindowSizeInfo(val windowWidthSizeClass: WindowWidthSizeClass)
private enum class WindowWidthSizeClass { Compact, Medium, Expanded }

@Composable
private fun VehicleCard(
    vehicle: Vehicle,
    status: VehicleStatus?,
    viewModel: HomeViewModel,
    onDiagnostics: (String) -> Unit,
    distanceUnit: String,
    modifier: Modifier = Modifier,
) {
    var showClimateSheet by remember { mutableStateOf(false) }
    var climateSettings by remember { mutableStateOf(ClimateSettings()) }

    ElevatedCard(
        modifier = modifier.padding(12.dp),
        shape = CarCardShape,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // Car header image
            CarHeader(vehicle = vehicle, status = status)

            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Car info
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(vehicle.nickname, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "${vehicle.modelYear} ${vehicle.modelName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (vehicle.licensePlate.isNotEmpty()) {
                        Text(vehicle.licensePlate, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                    }
                }

                // Status chips
                if (status != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusChip(
                            text = if (status.isLocked) "Locked" else "Unlocked",
                            isOk = status.isLocked,
                        )
                        if (status.engineRunning) {
                            StatusChip(text = "Engine Running", isOk = true)
                        }
                        if (status.climateOn) {
                            StatusChip(text = "Climate On", isOk = true)
                        }
                    }
                }

                // Range bars
                if (status != null) {
                    DualRangeBar(
                        evPercent = status.evBatteryPercent?.div(100f),
                        evRangeKm = status.evRangeKm,
                        fuelPercent = status.fuelLevelPercent?.div(100f),
                        fuelRangeKm = status.fuelRangeKm,
                        isCharging = status.evCharging,
                        isPluggedIn = status.evPluggedIn,
                        rangeUnit = distanceUnit,
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primaryContainer)
                }

                // Primary actions row: Lock/Unlock + Engine
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    LockButton(
                        isLocked = status?.isLocked ?: true,
                        isLoading = viewModel.isCommandLoading(vehicle.vin, "lock") || viewModel.isCommandLoading(vehicle.vin, "unlock"),
                        onToggle = {
                            if (status?.isLocked == true) viewModel.unlock(vehicle.vin)
                            else viewModel.lock(vehicle.vin)
                        },
                        modifier = Modifier.weight(1f),
                    )

                    if (vehicle.fuelType != FuelType.ELECTRIC) {
                        FilledTonalButton(
                            onClick = {
                                if (status?.engineRunning == true) viewModel.stopEngine(vehicle.vin)
                                else viewModel.startEngine(vehicle.vin)
                            },
                            enabled = !viewModel.isCommandLoading(vehicle.vin, "engine"),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            if (viewModel.isCommandLoading(vehicle.vin, "engine")) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                            } else {
                                Icon(
                                    if (status?.engineRunning == true) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                    null,
                                    Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(if (status?.engineRunning == true) "Stop" else "Start")
                        }
                    }
                }

                // Secondary actions grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ControlButton(
                        icon = Icons.Rounded.AcUnit,
                        label = if (status?.climateOn == true) "Stop A/C" else "Start A/C",
                        isActive = status?.climateOn == true,
                        isLoading = viewModel.isCommandLoading(vehicle.vin, "climate"),
                        onClick = {
                            if (status?.climateOn == true) viewModel.stopClimate(vehicle.vin)
                            else showClimateSheet = true
                        },
                        modifier = Modifier.weight(1f),
                    )
                    if (vehicle.fuelType == FuelType.ELECTRIC || vehicle.fuelType == FuelType.PHEV) {
                        ControlButton(
                            icon = if (status?.evCharging == true) Icons.Rounded.BatteryChargingFull else Icons.Rounded.Bolt,
                            label = if (status?.evCharging == true) "Stop Charge" else "Charge",
                            isActive = status?.evCharging == true,
                            isLoading = viewModel.isCommandLoading(vehicle.vin, "charge"),
                            onClick = {
                                if (status?.evCharging == true) viewModel.stopCharge(vehicle.vin)
                                else viewModel.startCharge(vehicle.vin)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    ControlButton(
                        icon = Icons.Rounded.FlashlightOn,
                        label = "Flash",
                        isActive = false,
                        isLoading = viewModel.isCommandLoading(vehicle.vin, "lights"),
                        onClick = { viewModel.flashLights(vehicle.vin) },
                        modifier = Modifier.weight(1f),
                    )
                    ControlButton(
                        icon = Icons.Rounded.VolumeUp,
                        label = "Horn",
                        isActive = false,
                        isLoading = viewModel.isCommandLoading(vehicle.vin, "horn"),
                        onClick = { viewModel.honkHorn(vehicle.vin) },
                        modifier = Modifier.weight(1f),
                    )
                }

                // Door status
                if (status != null) {
                    DoorStatusGrid(status = status)
                }

                // Diagnostics link
                OutlinedButton(
                    onClick = { onDiagnostics(vehicle.vin) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Rounded.Analytics, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Full Diagnostics")
                }
            }
        }
    }

    if (showClimateSheet) {
        ClimateBottomSheet(
            settings = climateSettings,
            onSettingsChange = { climateSettings = it },
            onDismiss = { showClimateSheet = false },
            onStart = {
                showClimateSheet = false
                viewModel.startClimate(vehicle.vin, climateSettings)
            },
        )
    }
}

@Composable
private fun CarHeader(vehicle: Vehicle, status: VehicleStatus?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        if (vehicle.customImageUri != null) {
            AsyncImage(
                model = vehicle.customImageUri,
                contentDescription = vehicle.modelName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(16.dp),
            )
        } else {
            // Stock image based on model code
            val stockImageUrl = stockImageUrl(vehicle.modelCode, vehicle.modelYear)
            if (stockImageUrl != null) {
                AsyncImage(
                    model = stockImageUrl,
                    contentDescription = vehicle.modelName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                )
            } else {
                Icon(
                    Icons.Rounded.DirectionsCar,
                    null,
                    modifier = Modifier.size(100.dp).align(Alignment.Center),
                    tint = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }

        // Status indicator overlay
        if (status?.engineRunning == true) {
            Surface(
                color = StatusGreen.copy(alpha = 0.9f),
                shape = RoundedCornerShape(bottomEnd = 16.dp),
                modifier = Modifier.align(Alignment.TopStart),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(androidx.compose.ui.graphics.Color.White))
                    Spacer(Modifier.width(6.dp))
                    Text("Running", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun stockImageUrl(modelCode: String?, modelYear: String?): String? {
    // Map common model codes to their Hyundai CDN image URLs
    val code = modelCode?.uppercase() ?: return null
    return when {
        code.contains("IONIQ6") || code.contains("GE") -> "https://www.hyundaiusa.com/content/dam/hyundai/us/home/2023/ioniq6/MY23_Ioniq6_Hero.png"
        code.contains("IONIQ5") || code.contains("NE") -> "https://www.hyundaiusa.com/content/dam/hyundai/us/home/2023/ioniq5/ioniq5-exterior-hero.png"
        code.contains("IONIQ9") -> null
        code.contains("KONA") || code.contains("SX2") -> null
        code.contains("TUCSON") || code.contains("NX4") -> null
        code.contains("SANTA FE") || code.contains("MX5") -> null
        code.contains("PALISADE") || code.contains("LX2") -> null
        code.contains("ELANTRA") || code.contains("CN7") -> null
        code.contains("SONATA") || code.contains("DN8") -> null
        else -> null
    }
}

@Composable
private fun DoorStatusGrid(status: VehicleStatus) {
    val openDoors = buildList {
        if (status.doorFrontLeft == DoorState.OPEN) add("FL")
        if (status.doorFrontRight == DoorState.OPEN) add("FR")
        if (status.doorRearLeft == DoorState.OPEN) add("RL")
        if (status.doorRearRight == DoorState.OPEN) add("RR")
        if (status.trunkOpen) add("Trunk")
        if (status.hoodOpen) add("Hood")
    }

    if (openDoors.isNotEmpty()) {
        Surface(
            color = StatusRed.copy(alpha = 0.1f),
            shape = RoundedCornerShape(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.Warning, null, tint = StatusRed, modifier = Modifier.size(20.dp))
                Text(
                    "Open: ${openDoors.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusRed,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClimateBottomSheet(
    settings: ClimateSettings,
    onSettingsChange: (ClimateSettings) -> Unit,
    onDismiss: () -> Unit,
    onStart: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = BottomSheetShape,
    ) {
        Column(
            modifier = Modifier.padding(24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Climate Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            // Temperature slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Temperature", style = MaterialTheme.typography.bodyMedium)
                Text("%.0f°C".format(settings.temperatureC), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = settings.temperatureC.toFloat(),
                onValueChange = { onSettingsChange(settings.copy(temperatureC = it.toDouble())) },
                valueRange = 16f..28f,
                steps = 23,
            )

            // Toggles
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = settings.defrostFront,
                    onClick = { onSettingsChange(settings.copy(defrostFront = !settings.defrostFront)) },
                    label = { Text("Front Defrost") },
                    leadingIcon = if (settings.defrostFront) {{ Icon(Icons.Rounded.Check, null, Modifier.size(16.dp)) }} else null,
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = settings.defrostRear,
                    onClick = { onSettingsChange(settings.copy(defrostRear = !settings.defrostRear)) },
                    label = { Text("Rear Defrost") },
                    leadingIcon = if (settings.defrostRear) {{ Icon(Icons.Rounded.Check, null, Modifier.size(16.dp)) }} else null,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = settings.steeringWheelHeat,
                    onClick = { onSettingsChange(settings.copy(steeringWheelHeat = !settings.steeringWheelHeat)) },
                    label = { Text("Steering Wheel") },
                    leadingIcon = if (settings.steeringWheelHeat) {{ Icon(Icons.Rounded.Check, null, Modifier.size(16.dp)) }} else null,
                    modifier = Modifier.weight(1f),
                )
                // Duration
                Column(modifier = Modifier.weight(1f)) {
                    Text("Duration: ${settings.durationMinutes} min", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = settings.durationMinutes.toFloat(),
                        onValueChange = { onSettingsChange(settings.copy(durationMinutes = it.toInt())) },
                        valueRange = 5f..30f,
                        steps = 4,
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                    Text("Cancel")
                }
                Button(onClick = onStart, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Rounded.AcUnit, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Start Climate")
                }
            }
        }
    }
}
