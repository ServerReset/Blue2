package com.blue2.app.ui.screens.home

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
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
    val context = LocalContext.current

    // Biometric capability check
    val biometricManager = BiometricManager.from(context)
    val canUseBiometrics = remember(context) {
        biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    // Helper to wrap sensitive commands with biometric prompt
    fun withBiometrics(action: () -> Unit) {
        if (!canUseBiometrics) {
            action()
            return
        }
        val activity = context as? FragmentActivity ?: run { action(); return }
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    action()
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Confirm Vehicle Command")
            .setSubtitle("Authenticate to send command")
            .setNegativeButtonText("Cancel")
            .build()
        prompt.authenticate(info)
    }

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
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val widthDp = configuration.screenWidthDp.dp
    val vehiclesPerPage = when {
        widthDp >= 900.dp -> 3
        widthDp >= 600.dp -> 2
        else -> 1
    }

    val pages = if (state.vehicles.isEmpty()) 0
    else (state.vehicles.size + vehiclesPerPage - 1) / vehiclesPerPage

    val pagerState = rememberPagerState(pageCount = { pages })
    LaunchedEffect(pagerState.currentPage) { viewModel.setPage(pagerState.currentPage) }

    val isRefreshing = state.isRefreshing || state.isLoadingVehicles

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoadingVehicles && state.vehicles.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            CircularProgressIndicator()
                            Text("Loading your vehicles…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                state.vehicles.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(32.dp),
                        ) {
                            Icon(
                                Icons.Rounded.DirectionsCar,
                                null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.outlineVariant,
                            )
                            Text("No vehicles found", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                "Make sure your Bluelink account has enrolled vehicles.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            FilledTonalButton(onClick = { viewModel.refreshAll() }) { Text("Try Again") }
                        }
                    }
                }

                else -> {
                    Column(Modifier.fillMaxSize()) {
                        // Thin animated refresh bar at the top
                        AnimatedVisibility(
                            visible = isRefreshing,
                            enter = expandVertically(tween(200)),
                            exit = shrinkVertically(tween(200)),
                        ) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }

                        // Pager takes all space except bottom strip
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
                                    tempUnit = state.tempUnit,
                                    onBiometricAction = { action -> withBiometrics(action) },
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
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    pageVehicles.forEach { vehicle ->
                                        VehicleCard(
                                            vehicle = vehicle,
                                            status = state.statuses[vehicle.vin],
                                            viewModel = viewModel,
                                            onDiagnostics = onDiagnostics,
                                            distanceUnit = state.distanceUnit,
                                            tempUnit = state.tempUnit,
                                            onBiometricAction = { action -> withBiometrics(action) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(),
                                        )
                                    }
                                    repeat(vehiclesPerPage - pageVehicles.size) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        // Bottom status strip: car name + page dots + refresh
                        BottomStatusStrip(
                            vehicles = state.vehicles,
                            pagerState = pagerState,
                            pages = pages,
                            isRefreshing = isRefreshing,
                            onRefresh = { viewModel.refreshAll() },
                            onSettings = onSettings,
                            scope = scope,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BottomStatusStrip(
    vehicles: List<Vehicle>,
    pagerState: PagerState,
    pages: Int,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    val currentVehicle = vehicles.getOrNull(pagerState.currentPage)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Car name
            Text(
                text = currentVehicle?.nickname ?: "Blue2",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )

            // Page dots
            if (pages > 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(pages) { i ->
                        val isSelected = pagerState.currentPage == i
                        val dotWidth by animateDpAsState(
                            targetValue = if (isSelected) 20.dp else 6.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "dotWidth",
                        )
                        Box(
                            Modifier
                                .height(6.dp)
                                .width(dotWidth)
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

            // Actions
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Rounded.Refresh, "Refresh", modifier = Modifier.size(20.dp))
                    }
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Rounded.Settings, "Settings", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun VehicleCard(
    vehicle: Vehicle,
    status: VehicleStatus?,
    viewModel: HomeViewModel,
    onDiagnostics: (String) -> Unit,
    distanceUnit: String,
    tempUnit: String,
    onBiometricAction: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    var climateExpanded by remember { mutableStateOf(false) }
    var chargeExpanded by remember { mutableStateOf(false) }
    var diagnosticsExpanded by remember { mutableStateOf(true) }
    var climateSettings by remember { mutableStateOf(ClimateSettings()) }

    // Charge target state
    var acTarget by remember(status?.evChargeTargetPercent) {
        mutableStateOf(status?.evChargeTargetPercent ?: 80)
    }
    var dcTarget by remember(status?.evChargeTargetPercent) {
        mutableStateOf(status?.evChargeTargetPercent ?: 80)
    }

    val isEv = vehicle.fuelType == FuelType.ELECTRIC || vehicle.fuelType == FuelType.PHEV

    ElevatedCard(
        modifier = modifier.padding(10.dp),
        shape = CarCardShape,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {

            // ── 1. HERO ─────────────────────────────────────────────────────────
            item(key = "hero") {
                HeroSection(vehicle = vehicle, status = status)
            }

            // ── 2. ENERGY ROW ───────────────────────────────────────────────────
            item(key = "energy") {
                if (status != null) {
                    EnergySection(
                        status = status,
                        vehicle = vehicle,
                        distanceUnit = distanceUnit,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    ) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        )
                    }
                }
            }

            // ── 3. QUICK ACTIONS (2×2 grid) ──────────────────────────────────────
            item(key = "quickActions") {
                QuickActionsSection(
                    vehicle = vehicle,
                    status = status,
                    viewModel = viewModel,
                    isEv = isEv,
                    onBiometricAction = onBiometricAction,
                    haptic = haptic,
                    climateSettings = climateSettings,
                )
            }

            // ── 4. SECONDARY ACTIONS ROW ─────────────────────────────────────────
            item(key = "secondaryActions") {
                SecondaryActionsRow(
                    vehicle = vehicle,
                    viewModel = viewModel,
                    haptic = haptic,
                )
            }

            // ── 5. CLIMATE PANEL (collapsible) ───────────────────────────────────
            item(key = "climateHeader") {
                CollapsibleSectionHeader(
                    title = "Climate",
                    icon = Icons.Rounded.AcUnit,
                    expanded = climateExpanded,
                    badge = if (status?.climateOn == true) "On" else null,
                    badgeColor = if (status?.defrostFront == true || status?.defrostRear == true) HeatOrange else CoolBlue,
                    onToggle = { climateExpanded = !climateExpanded },
                )
            }
            item(key = "climatePanel") {
                AnimatedVisibility(
                    visible = climateExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    ClimatePanel(
                        status = status,
                        settings = climateSettings,
                        onSettingsChange = { climateSettings = it },
                        tempUnit = tempUnit,
                        onStartClimate = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.startClimate(vehicle.vin, climateSettings)
                        },
                        onStopClimate = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.stopClimate(vehicle.vin)
                        },
                        isLoading = viewModel.isCommandLoading(vehicle.vin, "climate"),
                    )
                }
            }

            // ── 6. CHARGE PANEL (collapsible, EV/PHEV only) ──────────────────────
            if (isEv) {
                item(key = "chargeHeader") {
                    CollapsibleSectionHeader(
                        title = "Charge Limits",
                        icon = Icons.Rounded.BatteryChargingFull,
                        expanded = chargeExpanded,
                        badge = status?.evChargeTargetPercent?.let { "${it}%" },
                        badgeColor = ChargeGreen,
                        onToggle = { chargeExpanded = !chargeExpanded },
                    )
                }
                item(key = "chargePanel") {
                    AnimatedVisibility(
                        visible = chargeExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        ChargePanel(
                            acTarget = acTarget,
                            dcTarget = dcTarget,
                            onAcChange = { acTarget = it },
                            onDcChange = { dcTarget = it },
                            onSet = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.setChargeTarget(vehicle.vin, acTarget, dcTarget)
                            },
                        )
                    }
                }
            }

            // ── 7. DOOR STATUS ───────────────────────────────────────────────────
            if (status != null) {
                item(key = "doors") {
                    DoorStatusSection(status = status)
                }
            }

            // ── 8. DIAGNOSTICS PANEL (collapsible, expanded by default) ──────────
            item(key = "diagHeader") {
                CollapsibleSectionHeader(
                    title = "Diagnostics",
                    icon = Icons.Rounded.Analytics,
                    expanded = diagnosticsExpanded,
                    badge = if (status?.tirePressureWarning == true || status?.lowWasherFluid == true) "!" else null,
                    badgeColor = WarnRed,
                    onToggle = { diagnosticsExpanded = !diagnosticsExpanded },
                )
            }
            item(key = "diagPanel") {
                AnimatedVisibility(
                    visible = diagnosticsExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    DiagnosticsPanel(
                        status = status,
                        distanceUnit = distanceUnit,
                        onFullDiagnostics = { onDiagnostics(vehicle.vin) },
                    )
                }
            }

            item(key = "bottomSpacer") {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HERO SECTION
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HeroSection(vehicle: Vehicle, status: VehicleStatus?) {
    val infiniteTransition = rememberInfiniteTransition(label = "enginePulse")
    val enginePulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "enginePulseAlpha",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )
            ),
    ) {
        // Car image
        if (vehicle.customImageUri != null) {
            AsyncImage(
                model = vehicle.customImageUri,
                contentDescription = vehicle.modelName,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            )
        } else {
            val stockUrl = stockImageUrl(vehicle.modelCode, vehicle.modelYear)
            if (stockUrl != null) {
                AsyncImage(
                    model = stockUrl,
                    contentDescription = vehicle.modelName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                )
            } else {
                Icon(
                    Icons.Rounded.DirectionsCar,
                    null,
                    modifier = Modifier
                        .size(110.dp)
                        .align(Alignment.Center),
                    tint = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }

        // Vehicle name overlay (bottom of hero)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 14.dp, end = 20.dp),
        ) {
            Text(
                text = vehicle.nickname,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 34.sp,
            )
            Text(
                text = "${vehicle.modelYear} ${vehicle.modelName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (vehicle.licensePlate.isNotEmpty()) {
                Text(
                    text = vehicle.licensePlate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }

        // Status chips top-right
        if (status != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 14.dp, end = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val lockColor = if (status.isLocked) MaterialTheme.colorScheme.onSurface else LockGold
                HeroChip(
                    text = if (status.isLocked) "Locked" else "Unlocked",
                    color = lockColor,
                )
                if (status.engineRunning) {
                    HeroChip(text = "Running", color = StatusGreen)
                }
                if (status.evCharging) {
                    HeroChip(text = "Charging", color = ChargeChargingColor)
                }
                if (status.climateOn) {
                    HeroChip(text = "Climate", color = CoolBlue)
                }
            }
        }

        // Engine running pulsing dot overlay
        if (status?.engineRunning == true) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(StatusGreen.copy(alpha = enginePulse)),
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(StatusGreen)
                        .align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun HeroChip(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ENERGY SECTION
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EnergySection(
    status: VehicleStatus,
    vehicle: Vehicle,
    distanceUnit: String,
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val evPercent = status.evBatteryPercent?.div(100f)
        val fuelPercent = status.fuelLevelPercent?.div(100f)

        if (evPercent != null) {
            AnimatedEnergyBar(
                label = "Battery",
                icon = if (status.evCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryFull,
                percent = evPercent,
                rangeKm = status.evRangeKm,
                isCharging = status.evCharging,
                isPluggedIn = status.evPluggedIn,
                distanceUnit = distanceUnit,
                barColor = when {
                    status.evCharging -> ChargeChargingColor
                    evPercent >= 0.5f -> ChargeGreen
                    evPercent >= 0.2f -> StatusOrange
                    else -> WarnRed
                },
                estimatedChargingMinutes = status.estimatedChargingMinutes,
            )
        }

        if (fuelPercent != null && vehicle.fuelType != FuelType.ELECTRIC) {
            AnimatedEnergyBar(
                label = "Fuel",
                icon = Icons.Rounded.LocalGasStation,
                percent = fuelPercent,
                rangeKm = status.fuelRangeKm,
                isCharging = false,
                isPluggedIn = false,
                distanceUnit = distanceUnit,
                barColor = when {
                    fuelPercent >= 0.5f -> ChargeGreen
                    fuelPercent >= 0.2f -> StatusOrange
                    else -> WarnRed
                },
                estimatedChargingMinutes = null,
            )
        }

        if (evPercent == null && fuelPercent == null) {
            Text(
                "Range data unavailable",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AnimatedEnergyBar(
    label: String,
    icon: ImageVector,
    percent: Float,
    rangeKm: Double?,
    isCharging: Boolean,
    isPluggedIn: Boolean,
    distanceUnit: String,
    barColor: Color,
    estimatedChargingMinutes: Int?,
) {
    val animatedPercent by animateFloatAsState(
        targetValue = percent.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "energyBar",
    )

    val shimmerTranslate by rememberInfiniteTransition(label = "chargeShimmer").animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerPos",
    )

    val displayRange = rangeKm?.let {
        if (distanceUnit == "mi") "%.0f mi".format(it * 0.621371) else "%.0f km".format(it)
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(icon, null, modifier = Modifier.size(18.dp), tint = barColor)
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (isCharging) {
                    Surface(color = ChargeChargingColor.copy(alpha = 0.15f), shape = RoundedCornerShape(50)) {
                        Text(
                            "⚡ Charging",
                            style = MaterialTheme.typography.labelSmall,
                            color = ChargeChargingColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                } else if (isPluggedIn) {
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(50)) {
                        Text(
                            "Plugged in",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "%.0f%%".format(percent * 100),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = barColor,
                )
                if (displayRange != null) {
                    Text(
                        displayRange,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
            }
        }

        // Bar track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedPercent)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isCharging) {
                            Brush.horizontalGradient(
                                colors = listOf(barColor, barColor.copy(alpha = 0.6f), barColor),
                                startX = shimmerTranslate * 400f,
                                endX = shimmerTranslate * 400f + 400f,
                            )
                        } else {
                            Brush.horizontalGradient(listOf(barColor, barColor.copy(alpha = 0.8f)))
                        }
                    ),
            )
            // iOS-style notch tip
            if (animatedPercent > 0.02f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedPercent)
                        .fillMaxHeight()
                        .padding(end = 2.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.6f)),
                    )
                }
            }
        }

        if (isCharging && estimatedChargingMinutes != null && estimatedChargingMinutes > 0) {
            val hours = estimatedChargingMinutes / 60
            val mins = estimatedChargingMinutes % 60
            val timeStr = if (hours > 0) "${hours}h ${mins}m remaining" else "${mins}m remaining"
            Text(
                timeStr,
                style = MaterialTheme.typography.labelSmall,
                color = ChargeChargingColor,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// QUICK ACTIONS (2×2 grid)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuickActionsSection(
    vehicle: Vehicle,
    status: VehicleStatus?,
    viewModel: HomeViewModel,
    isEv: Boolean,
    onBiometricAction: (() -> Unit) -> Unit,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    climateSettings: ClimateSettings,
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Row 1: Lock/Unlock + Engine Start/Stop
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Lock/Unlock
            val lockLoading = viewModel.isCommandLoading(vehicle.vin, "lock") || viewModel.isCommandLoading(vehicle.vin, "unlock")
            QuickActionButton(
                icon = if (status?.isLocked == true) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                label = if (status?.isLocked == true) "Unlock" else "Lock",
                isActive = status?.isLocked == false,
                isLoading = lockLoading,
                activeColor = LockGold,
                modifier = Modifier.weight(1f),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onBiometricAction {
                        if (status?.isLocked == true) viewModel.unlock(vehicle.vin)
                        else viewModel.lock(vehicle.vin)
                    }
                },
            )

            // Engine Start/Stop (ICE/PHEV/HYBRID)
            if (vehicle.fuelType != FuelType.ELECTRIC) {
                val engineLoading = viewModel.isCommandLoading(vehicle.vin, "engine")
                QuickActionButton(
                    icon = if (status?.engineRunning == true) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    label = if (status?.engineRunning == true) "Stop Engine" else "Start Engine",
                    isActive = status?.engineRunning == true,
                    isLoading = engineLoading,
                    activeColor = StatusGreen,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onBiometricAction {
                            if (status?.engineRunning == true) viewModel.stopEngine(vehicle.vin)
                            else viewModel.startEngine(vehicle.vin)
                        }
                    },
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
        }

        // Row 2: Climate Start/Stop + Charge Start/Stop (EV/PHEV)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val climateLoading = viewModel.isCommandLoading(vehicle.vin, "climate")
            QuickActionButton(
                icon = Icons.Rounded.AcUnit,
                label = if (status?.climateOn == true) "Stop Climate" else "Start Climate",
                isActive = status?.climateOn == true,
                isLoading = climateLoading,
                activeColor = if (status?.defrostFront == true || status?.defrostRear == true) HeatOrange else CoolBlue,
                modifier = Modifier.weight(1f),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (status?.climateOn == true) viewModel.stopClimate(vehicle.vin)
                    else viewModel.startClimate(vehicle.vin, climateSettings)
                },
            )

            if (isEv) {
                val chargeLoading = viewModel.isCommandLoading(vehicle.vin, "charge")
                QuickActionButton(
                    icon = if (status?.evCharging == true) Icons.Rounded.BatteryChargingFull else Icons.Rounded.Bolt,
                    label = if (status?.evCharging == true) "Stop Charge" else "Start Charge",
                    isActive = status?.evCharging == true,
                    isLoading = chargeLoading,
                    activeColor = ChargeGreen,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (status?.evCharging == true) viewModel.stopCharge(vehicle.vin)
                        else viewModel.startCharge(vehicle.vin)
                    },
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    isLoading: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isLoading) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "quickActionScale",
    )
    val containerColor by animateColorAsState(
        targetValue = if (isActive) activeColor.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "quickActionBg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isActive) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "quickActionContent",
    )

    FilledTonalButton(
        onClick = onClick,
        enabled = !isLoading,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        modifier = modifier
            .scale(scale)
            .height(56.dp),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = contentColor,
            )
        } else {
            Icon(icon, null, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECONDARY ACTIONS ROW
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SecondaryActionsRow(
    vehicle: Vehicle,
    viewModel: HomeViewModel,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SecondaryIconButton(
            icon = Icons.Rounded.FlashlightOn,
            label = "Flash",
            isLoading = viewModel.isCommandLoading(vehicle.vin, "lights"),
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.flashLights(vehicle.vin)
            },
            modifier = Modifier.weight(1f),
        )
        SecondaryIconButton(
            icon = Icons.AutoMirrored.Rounded.VolumeUp,
            label = "Horn",
            isLoading = viewModel.isCommandLoading(vehicle.vin, "horn"),
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.honkHorn(vehicle.vin)
            },
            modifier = Modifier.weight(1f),
        )
        SecondaryIconButton(
            icon = Icons.Rounded.LocationOn,
            label = "Locate",
            isLoading = false,
            onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
            modifier = Modifier.weight(1f),
        )
        SecondaryIconButton(
            icon = Icons.Rounded.Tune,
            label = "Limits",
            isLoading = false,
            onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SecondaryIconButton(
    icon: ImageVector,
    label: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isLoading) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "secBtnScale",
    )

    Surface(
        onClick = onClick,
        enabled = !isLoading,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .scale(scale)
            .height(60.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    icon,
                    null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COLLAPSIBLE SECTION HEADER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CollapsibleSectionHeader(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    badge: String?,
    badgeColor: Color,
    onToggle: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chevronRotation",
    )

    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        modifier = Modifier.padding(horizontal = 16.dp),
    )

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onToggle()
        },
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    icon,
                    null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (badge != null) {
                    Surface(
                        color = badgeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            badge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
            Icon(
                Icons.Rounded.ExpandMore,
                null,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = rotation },
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CLIMATE PANEL
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ClimatePanel(
    status: VehicleStatus?,
    settings: ClimateSettings,
    onSettingsChange: (ClimateSettings) -> Unit,
    tempUnit: String,
    onStartClimate: () -> Unit,
    onStopClimate: () -> Unit,
    isLoading: Boolean,
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Current interior/exterior temp if available
        if (status?.interiorTempC != null || status?.exteriorTempC != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                status?.interiorTempC?.let { temp ->
                    TempDisplay(label = "Interior", tempC = temp, tempUnit = tempUnit, modifier = Modifier.weight(1f))
                }
                status?.exteriorTempC?.let { temp ->
                    TempDisplay(label = "Exterior", tempC = temp, tempUnit = tempUnit, modifier = Modifier.weight(1f))
                }
                status?.targetTempC?.let { temp ->
                    TempDisplay(label = "Target", tempC = temp, tempUnit = tempUnit, modifier = Modifier.weight(1f))
                }
            }
        }

        // Temperature selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Set Temperature", style = MaterialTheme.typography.bodyMedium)
            val displayTemp = if (tempUnit == "F") {
                "%.0f°F".format(settings.temperatureC * 9.0 / 5.0 + 32.0)
            } else {
                "%.0f°C".format(settings.temperatureC)
            }
            Text(
                displayTemp,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = settings.temperatureC.toFloat(),
            onValueChange = { onSettingsChange(settings.copy(temperatureC = it.toDouble())) },
            valueRange = 16f..28f,
            steps = 23,
        )

        // Defrost toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val defrostFrontColor = if (settings.defrostFront || status?.defrostFront == true) HeatOrange else MaterialTheme.colorScheme.outlineVariant
            val defrostRearColor = if (settings.defrostRear || status?.defrostRear == true) HeatOrange else MaterialTheme.colorScheme.outlineVariant

            FilterChip(
                selected = settings.defrostFront,
                onClick = { onSettingsChange(settings.copy(defrostFront = !settings.defrostFront)) },
                label = { Text("Front Defrost") },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.AcUnit,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = defrostFrontColor,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = HeatOrange.copy(alpha = 0.15f),
                    selectedLabelColor = HeatOrange,
                ),
                modifier = Modifier.weight(1f),
            )
            FilterChip(
                selected = settings.defrostRear,
                onClick = { onSettingsChange(settings.copy(defrostRear = !settings.defrostRear)) },
                label = { Text("Rear Defrost") },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.AcUnit,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = defrostRearColor,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = HeatOrange.copy(alpha = 0.15f),
                    selectedLabelColor = HeatOrange,
                ),
                modifier = Modifier.weight(1f),
            )
        }

        // Steering wheel heat
        FilterChip(
            selected = settings.steeringWheelHeat,
            onClick = { onSettingsChange(settings.copy(steeringWheelHeat = !settings.steeringWheelHeat)) },
            label = { Text("Steering Wheel Heat") },
            leadingIcon = {
                Icon(
                    Icons.Rounded.Autorenew,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = if (settings.steeringWheelHeat) HeatOrange else MaterialTheme.colorScheme.outlineVariant,
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = HeatOrange.copy(alpha = 0.15f),
                selectedLabelColor = HeatOrange,
            ),
        )

        // Seat heating
        Text(
            "Seat Heating",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SeatHeatSelector(
                label = "FL",
                level = settings.seatHeatFrontLeft,
                onLevel = { onSettingsChange(settings.copy(seatHeatFrontLeft = it)) },
                modifier = Modifier.weight(1f),
            )
            SeatHeatSelector(
                label = "FR",
                level = settings.seatHeatFrontRight,
                onLevel = { onSettingsChange(settings.copy(seatHeatFrontRight = it)) },
                modifier = Modifier.weight(1f),
            )
            SeatHeatSelector(
                label = "RL",
                level = settings.seatHeatRearLeft,
                onLevel = { onSettingsChange(settings.copy(seatHeatRearLeft = it)) },
                modifier = Modifier.weight(1f),
            )
            SeatHeatSelector(
                label = "RR",
                level = settings.seatHeatRearRight,
                onLevel = { onSettingsChange(settings.copy(seatHeatRearRight = it)) },
                modifier = Modifier.weight(1f),
            )
        }

        // Duration
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Duration", style = MaterialTheme.typography.bodyMedium)
            Text(
                "${settings.durationMinutes} min",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = settings.durationMinutes.toFloat(),
            onValueChange = { onSettingsChange(settings.copy(durationMinutes = it.toInt())) },
            valueRange = 5f..30f,
            steps = 4,
        )

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (status?.climateOn == true) {
                OutlinedButton(
                    onClick = onStopClimate,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    if (isLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Stop Climate")
                }
            }
            Button(
                onClick = onStartClimate,
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f).height(48.dp),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Rounded.AcUnit, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Start Climate")
                }
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun TempDisplay(label: String, tempC: Double, tempUnit: String, modifier: Modifier = Modifier) {
    val display = if (tempUnit == "F") "%.0f°F".format(tempC * 9.0 / 5.0 + 32.0) else "%.1f°C".format(tempC)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
        ) {
            Text(display, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SeatHeatSelector(
    label: String,
    level: SeatHeatingLevel,
    onLevel: (SeatHeatingLevel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val levels = listOf(SeatHeatingLevel.OFF, SeatHeatingLevel.LOW, SeatHeatingLevel.MEDIUM, SeatHeatingLevel.HIGH)
    val currentIdx = levels.indexOf(level)
    val levelColor = when (level) {
        SeatHeatingLevel.OFF -> MaterialTheme.colorScheme.surfaceContainerHigh
        SeatHeatingLevel.LOW -> HeatOrange.copy(alpha = 0.3f)
        SeatHeatingLevel.MEDIUM -> HeatOrange.copy(alpha = 0.6f)
        SeatHeatingLevel.HIGH -> HeatOrange
    }
    val textColor = when (level) {
        SeatHeatingLevel.OFF -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> HeatOrangeLight
    }

    Surface(
        onClick = {
            val next = levels[(currentIdx + 1) % levels.size]
            onLevel(next)
        },
        shape = RoundedCornerShape(12.dp),
        color = levelColor,
        modifier = modifier.height(48.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = textColor, fontWeight = FontWeight.Bold)
            Text(
                when (level) {
                    SeatHeatingLevel.OFF -> "Off"
                    SeatHeatingLevel.LOW -> "Lo"
                    SeatHeatingLevel.MEDIUM -> "Med"
                    SeatHeatingLevel.HIGH -> "Hi"
                },
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CHARGE PANEL
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChargePanel(
    acTarget: Int,
    dcTarget: Int,
    onAcChange: (Int) -> Unit,
    onDcChange: (Int) -> Unit,
    onSet: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // AC target
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("AC Charge Limit", style = MaterialTheme.typography.bodyMedium)
            Text("$acTarget%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ChargeGreen)
        }
        Slider(
            value = acTarget.toFloat(),
            onValueChange = { onAcChange(it.toInt()) },
            valueRange = 50f..100f,
            steps = 9,
            colors = SliderDefaults.colors(thumbColor = ChargeGreen, activeTrackColor = ChargeGreen),
        )

        // DC target
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("DC Charge Limit", style = MaterialTheme.typography.bodyMedium)
            Text("$dcTarget%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ChargeGreen)
        }
        Slider(
            value = dcTarget.toFloat(),
            onValueChange = { onDcChange(it.toInt()) },
            valueRange = 50f..100f,
            steps = 9,
            colors = SliderDefaults.colors(thumbColor = ChargeGreen, activeTrackColor = ChargeGreen),
        )

        Button(
            onClick = onSet,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ChargeGreen),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Icon(Icons.Rounded.Check, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Set Charge Targets", fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(4.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DOOR STATUS SECTION
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DoorStatusSection(status: VehicleStatus) {
    data class DoorItem(val label: String, val isOpen: Boolean, val icon: ImageVector)

    val doors = listOf(
        DoorItem("FL", status.doorFrontLeft == DoorState.OPEN, Icons.Rounded.ChevronRight),
        DoorItem("FR", status.doorFrontRight == DoorState.OPEN, Icons.Rounded.ChevronRight),
        DoorItem("RL", status.doorRearLeft == DoorState.OPEN, Icons.Rounded.ChevronRight),
        DoorItem("RR", status.doorRearRight == DoorState.OPEN, Icons.Rounded.ChevronRight),
        DoorItem("Hood", status.hoodOpen, Icons.Rounded.KeyboardArrowUp),
        DoorItem("Trunk", status.trunkOpen, Icons.Rounded.KeyboardArrowDown),
    )

    val anyOpen = doors.any { it.isOpen }

    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Rounded.DirectionsCar,
                null,
                modifier = Modifier.size(18.dp),
                tint = if (anyOpen) WarnRed else MaterialTheme.colorScheme.primary,
            )
            Text(
                "Doors & Openings",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            if (anyOpen) {
                Surface(color = WarnRed.copy(alpha = 0.12f), shape = RoundedCornerShape(50)) {
                    Text(
                        "Open",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarnRed,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            doors.forEach { door ->
                val color by animateColorAsState(
                    targetValue = if (door.isOpen) WarnRed else MaterialTheme.colorScheme.surfaceContainerLow,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "door_${door.label}",
                )
                val borderColor by animateColorAsState(
                    targetValue = if (door.isOpen) WarnRed else Color.Transparent,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "doorBorder_${door.label}",
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = color,
                    border = BorderStroke(if (door.isOpen) 1.5.dp else 0.dp, borderColor),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Icon(
                            if (door.isOpen) Icons.Rounded.Warning else Icons.Rounded.Check,
                            null,
                            modifier = Modifier.size(14.dp),
                            tint = if (door.isOpen) WarnRed else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            door.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (door.isOpen) WarnRed else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DIAGNOSTICS PANEL
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DiagnosticsPanel(
    status: VehicleStatus?,
    distanceUnit: String,
    onFullDiagnostics: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (status == null) {
            Text("No diagnostic data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            // Tire pressures in 2×2 grid
            val tirePressures = listOf(
                Triple("FL", status.tirePressureFrontLeft, status.tirePressureWarning),
                Triple("FR", status.tirePressureFrontRight, status.tirePressureWarning),
                Triple("RL", status.tirePressureRearLeft, status.tirePressureWarning),
                Triple("RR", status.tirePressureRearRight, status.tirePressureWarning),
            )
            val hasTireData = tirePressures.any { it.second != null }

            if (hasTireData) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            "Tire Pressure",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (status.tirePressureWarning) {
                            Icon(Icons.Rounded.Warning, null, modifier = Modifier.size(14.dp), tint = WarnRed)
                        }
                    }
                    // 2-column grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TirePressureCell(label = "Front Left", psi = tirePressures[0].second, warn = status.tirePressureWarning)
                            TirePressureCell(label = "Rear Left", psi = tirePressures[2].second, warn = status.tirePressureWarning)
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TirePressureCell(label = "Front Right", psi = tirePressures[1].second, warn = status.tirePressureWarning)
                            TirePressureCell(label = "Rear Right", psi = tirePressures[3].second, warn = status.tirePressureWarning)
                        }
                    }
                }
            }

            // Odometer, oil life, washer fluid row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                status.odometer?.let { odo ->
                    val odometerStr = if (distanceUnit == "mi") "%.0f mi".format(odo * 0.621371) else "%.0f km".format(odo)
                    DiagCell(label = "Odometer", value = odometerStr, modifier = Modifier.weight(1f))
                }
                status.engineOilLife?.let { oil ->
                    DiagCell(
                        label = "Oil Life",
                        value = "$oil%",
                        warn = oil < 20,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (status.lowWasherFluid) {
                    DiagCell(label = "Washer Fluid", value = "Low", warn = true, modifier = Modifier.weight(1f))
                }
            }

            // Alerts
            val alerts = buildList {
                if (status.tirePressureWarning) add("Tire pressure warning")
                if (status.lowWasherFluid) add("Low washer fluid")
                if (status.lowCoolant) add("Low coolant")
                if (status.brakeFluidLow) add("Low brake fluid")
                status.dtcCodes.forEach { add("DTC: $it") }
            }
            if (alerts.isNotEmpty()) {
                Surface(
                    color = WarnRed.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(Icons.Rounded.Warning, null, modifier = Modifier.size(16.dp), tint = WarnRed)
                            Text("Alerts", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = WarnRed)
                        }
                        alerts.forEach { alert ->
                            Text("• $alert", style = MaterialTheme.typography.bodySmall, color = WarnRed)
                        }
                    }
                }
            }
        }

        // Full diagnostics button
        OutlinedButton(
            onClick = onFullDiagnostics,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Icon(Icons.Rounded.Analytics, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Full Diagnostics")
        }
    }
}

@Composable
private fun TirePressureCell(label: String, psi: Int?, warn: Boolean) {
    Surface(
        color = if (warn && psi != null) WarnRed.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                if (psi != null) "$psi psi" else "—",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (warn && psi != null) WarnRed else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun DiagCell(label: String, value: String, warn: Boolean = false, modifier: Modifier = Modifier) {
    Surface(
        color = if (warn) WarnRed.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(10.dp),
        ) {
            Text(
                value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (warn) WarnRed else MaterialTheme.colorScheme.onSurface,
            )
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────────────────────────────────────

private fun stockImageUrl(modelCode: String?, modelYear: String?): String? {
    val code = modelCode?.uppercase() ?: return null
    return when {
        code.contains("IONIQ6") || code.contains("GE") ->
            "https://www.hyundaiusa.com/content/dam/hyundai/us/home/2023/ioniq6/MY23_Ioniq6_Hero.png"
        code.contains("IONIQ5") || code.contains("NE") ->
            "https://www.hyundaiusa.com/content/dam/hyundai/us/home/2023/ioniq5/ioniq5-exterior-hero.png"
        else -> null
    }
}
