package com.blue2.app.ui.screens.diagnostics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blue2.app.domain.models.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    vin: String,
    onBack: () -> Unit,
    viewModel: DiagnosticsViewModel = hiltViewModel(),
) {
    LaunchedEffect(vin) { viewModel.load(vin) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Diagnostics", fontWeight = FontWeight.Bold)
                        state.vehicle?.let {
                            Text(it.nickname, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh(vin) }) {
                        Icon(Icons.Rounded.Refresh, "Refresh")
                    }
                },
            )
        },
    ) { padding ->
        val status = state.status
        if (status == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (state.isLoading) CircularProgressIndicator()
                else Text("No status data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { DiagSection("Last Updated") { DiagRow("Timestamp", formatTime(status.timestamp)) } }

            item {
                DiagSection("Doors & Locks") {
                    DiagRow("Locked", status.isLocked.toString())
                    DiagRow("Front Left Door", status.doorFrontLeft.name)
                    DiagRow("Front Right Door", status.doorFrontRight.name)
                    DiagRow("Rear Left Door", status.doorRearLeft.name)
                    DiagRow("Rear Right Door", status.doorRearRight.name)
                    DiagRow("Trunk", if (status.trunkOpen) "OPEN" else "CLOSED")
                    DiagRow("Hood", if (status.hoodOpen) "OPEN" else "CLOSED")
                }
            }

            item {
                DiagSection("Engine / EV") {
                    DiagRow("Engine Running", status.engineRunning.toString())
                    status.evBatteryPercent?.let { DiagRow("EV Battery", "$it%") }
                    status.evRangeKm?.let { DiagRow("EV Range", "%.1f km".format(it)) }
                    DiagRow("Charging", status.evCharging.toString())
                    DiagRow("Plugged In", status.evPluggedIn.toString())
                    status.evChargeTargetPercent?.let { DiagRow("Charge Target", "$it%") }
                    status.estimatedChargingMinutes?.let { DiagRow("Est. Charge Time", "${it} min") }
                    status.fuelLevelPercent?.let { DiagRow("Fuel Level", "$it%") }
                    status.fuelRangeKm?.let { DiagRow("Fuel Range", "%.1f km".format(it)) }
                }
            }

            item {
                DiagSection("Climate") {
                    DiagRow("Climate On", status.climateOn.toString())
                    status.interiorTempC?.let { DiagRow("Interior Temp", "%.1f°C".format(it)) }
                    status.exteriorTempC?.let { DiagRow("Exterior Temp", "%.1f°C".format(it)) }
                    status.targetTempC?.let { DiagRow("Target Temp", "%.1f°C".format(it)) }
                    DiagRow("Front Defrost", status.defrostFront.toString())
                    DiagRow("Rear Defrost", status.defrostRear.toString())
                    DiagRow("Seat Heat FL", status.seatHeatFrontLeft.name)
                    DiagRow("Seat Heat FR", status.seatHeatFrontRight.name)
                    DiagRow("Seat Heat RL", status.seatHeatRearLeft.name)
                    DiagRow("Seat Heat RR", status.seatHeatRearRight.name)
                    DiagRow("Steering Wheel Heat", status.steeringWheelHeat.toString())
                }
            }

            item {
                DiagSection("Location") {
                    status.latitude?.let { DiagRow("Latitude", "%.6f".format(it)) }
                    status.longitude?.let { DiagRow("Longitude", "%.6f".format(it)) }
                    status.heading?.let { DiagRow("Heading", "$it°") }
                    status.speed?.let { DiagRow("Speed", "%.1f km/h".format(it)) }
                }
            }

            item {
                DiagSection("Maintenance") {
                    status.odometer?.let { DiagRow("Odometer", "%.0f km".format(it)) }
                    status.tirePressureFrontLeft?.let { DiagRow("Tire FL", "$it PSI") }
                    status.tirePressureFrontRight?.let { DiagRow("Tire FR", "$it PSI") }
                    status.tirePressureRearLeft?.let { DiagRow("Tire RL", "$it PSI") }
                    status.tirePressureRearRight?.let { DiagRow("Tire RR", "$it PSI") }
                    DiagRow("Tire Pressure Warning", status.tirePressureWarning.toString())
                    DiagRow("Low Washer Fluid", status.lowWasherFluid.toString())
                    DiagRow("Low Coolant", status.lowCoolant.toString())
                    status.engineOilLife?.let { DiagRow("Oil Life", "$it%") }
                    DiagRow("Brake Fluid Low", status.brakeFluidLow.toString())
                    status.maintenanceDueKm?.let { DiagRow("Next Service", "%.0f km".format(it)) }
                    if (status.dtcCodes.isNotEmpty()) {
                        DiagRow("DTC Codes", status.dtcCodes.joinToString(", "))
                    }
                }
            }

            item {
                DiagSection("Electronics") {
                    DiagRow("Lights On", status.lightsOn.toString())
                    DiagRow("High Beam", status.highBeam.toString())
                    status.batteryVoltage?.let { DiagRow("12V Battery", "%.1f V".format(it)) }
                }
            }

            // Raw JSON
            if (status.rawJson != null) {
                item {
                    var expanded by remember { mutableStateOf(false) }
                    ElevatedCard(shape = MaterialTheme.shapes.large) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Raw API Response", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                TextButton(onClick = { expanded = !expanded }) {
                                    Text(if (expanded) "Collapse" else "Expand")
                                }
                            }
                            if (expanded) {
                                Spacer(Modifier.height(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.small,
                                ) {
                                    Text(
                                        status.rawJson,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(12.dp),
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
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
private fun DiagSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun DiagRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.US)
    return sdf.format(java.util.Date(timestamp))
}
