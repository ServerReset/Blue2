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
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.*

@Composable
fun WearHomeScreen(
    onSelectVehicle: (String) -> Unit,
    viewModel: WearViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
    ) {
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            !state.isLoggedIn -> {
                Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Sign in on your phone first",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.body2,
                    )
                }
            }
            state.vehicles.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                    Text("No vehicles found", textAlign = TextAlign.Center)
                }
            }
            else -> {
                ScalingLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    item {
                        ListHeader { Text("Blue2", fontWeight = FontWeight.Bold) }
                    }
                    items(state.vehicles) { vehicle ->
                        Chip(
                            label = { Text(vehicle.nickname) },
                            secondaryLabel = {
                                val status = state.statuses[vehicle.vin]
                                if (status != null) {
                                    Text(
                                        buildString {
                                            append(if (status.isLocked) "🔒" else "🔓")
                                            status.evBatteryPercent?.let { append(" ⚡$it%") }
                                            status.fuelLevelPercent?.let { append(" ⛽$it%") }
                                        }
                                    )
                                }
                            },
                            onClick = { onSelectVehicle(vehicle.vin) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
