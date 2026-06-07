package com.blue2.app.wear.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.*

@Composable
fun WearCarScreen(
    vin: String,
    viewModel: WearViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val vehicle = state.vehicles.firstOrNull { it.vin == vin }
    val status = state.statuses[vin]

    Scaffold(
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                ListHeader {
                    Text(vehicle?.nickname ?: vin.take(8), fontWeight = FontWeight.Bold)
                }
            }

            // Status chips
            if (status != null) {
                item {
                    Text(
                        buildString {
                            append(if (status.isLocked) "🔒 Locked" else "🔓 Unlocked")
                            if (status.engineRunning) append("  🟢 Running")
                        },
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    )
                }

                status.evBatteryPercent?.let { bat ->
                    item {
                        Text(
                            "⚡ $bat%${status.evRangeKm?.let { " · %.0f km".format(it) } ?: ""}",
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                        )
                    }
                }

                status.fuelLevelPercent?.let { fuel ->
                    item {
                        Text(
                            "⛽ $fuel%${status.fuelRangeKm?.let { " · %.0f km".format(it) } ?: ""}",
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                        )
                    }
                }
            }

            // Lock/Unlock
            item {
                Chip(
                    label = {
                        Text(
                            if (viewModel.isCommandLoading(vin, "lock") || viewModel.isCommandLoading(vin, "unlock")) "Working…"
                            else if (status?.isLocked == true) "Unlock" else "Lock"
                        )
                    },
                    onClick = {
                        if (status?.isLocked == true) viewModel.unlock(vin)
                        else viewModel.lock(vin)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ChipDefaults.primaryChipColors(),
                )
            }

            // Climate
            item {
                Chip(
                    label = {
                        Text(
                            if (viewModel.isCommandLoading(vin, "climate")) "Working…"
                            else if (status?.climateOn == true) "Stop A/C" else "Start A/C"
                        )
                    },
                    onClick = {
                        if (status?.climateOn == true) viewModel.stopClimate(vin)
                        else viewModel.startClimate(vin)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Engine (non-EV)
            vehicle?.let { v ->
                if (v.fuelType != com.blue2.app.domain.models.FuelType.ELECTRIC) {
                    item {
                        Chip(
                            label = {
                                Text(
                                    if (viewModel.isCommandLoading(vin, "engine")) "Working…"
                                    else if (status?.engineRunning == true) "Stop Engine" else "Start Engine"
                                )
                            },
                            onClick = {
                                if (status?.engineRunning == true) viewModel.stopEngine(vin)
                                else viewModel.startEngine(vin)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // Charge (EV)
            vehicle?.let { v ->
                if (v.fuelType == com.blue2.app.domain.models.FuelType.ELECTRIC ||
                    v.fuelType == com.blue2.app.domain.models.FuelType.PHEV) {
                    item {
                        Chip(
                            label = {
                                Text(
                                    if (viewModel.isCommandLoading(vin, "charge")) "Working…"
                                    else if (status?.evCharging == true) "Stop Charge" else "Start Charge"
                                )
                            },
                            onClick = {
                                if (status?.evCharging == true) viewModel.stopCharge(vin)
                                else viewModel.startCharge(vin)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
