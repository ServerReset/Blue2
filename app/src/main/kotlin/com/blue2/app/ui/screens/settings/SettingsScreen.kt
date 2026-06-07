package com.blue2.app.ui.screens.settings

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blue2.app.domain.models.Vehicle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showLogoutConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Account section
            item {
                SettingsSection(title = "Account") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(state.userEmail ?: "Unknown", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("Region: ${state.region}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(
                            onClick = { showLogoutConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Icon(Icons.Rounded.Logout, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Sign Out")
                        }
                    }
                }
            }

            // Theme section
            item {
                SettingsSection(title = "Appearance") {
                    // Theme mode
                    Text("Theme", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("system" to "Auto", "light" to "Light", "dark" to "Dark").forEach { (key, label) ->
                            FilterChip(
                                selected = state.themeMode == key,
                                onClick = { viewModel.setThemeMode(key) },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("Style", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "material_expressive" to "Material",
                            "liquid_glass" to "Liquid Glass",
                            "one_ui" to "One UI",
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = state.themeStyle == key,
                                onClick = { viewModel.setThemeStyle(key) },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    SettingsSwitch(
                        label = "Dynamic Color (Material You)",
                        description = "Use system wallpaper colors (Material style only)",
                        checked = state.dynamicColor,
                        onCheckedChange = viewModel::setDynamicColor,
                        enabled = state.themeStyle == "material_expressive",
                    )
                    SettingsSwitch(
                        label = "AMOLED Mode",
                        description = "Pure black backgrounds to save battery on OLED screens",
                        checked = state.amoledMode,
                        onCheckedChange = viewModel::setAmoledMode,
                        enabled = state.themeMode != "light",
                    )
                    SettingsSwitch(
                        label = "Atkinson Hyperlegible Next",
                        description = "High legibility font designed for visual accessibility",
                        checked = state.useAtkinsonFont,
                        onCheckedChange = viewModel::setAtkinsonFont,
                    )
                }
            }

            // Units section
            item {
                SettingsSection(title = "Units") {
                    Text("Distance", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.distanceUnit == "km",
                            onClick = { viewModel.setDistanceUnit("km") },
                            label = { Text("Kilometers") },
                            modifier = Modifier.weight(1f),
                        )
                        FilterChip(
                            selected = state.distanceUnit == "mi",
                            onClick = { viewModel.setDistanceUnit("mi") },
                            label = { Text("Miles") },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("Temperature", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.tempUnit == "C",
                            onClick = { viewModel.setTempUnit("C") },
                            label = { Text("Celsius") },
                            modifier = Modifier.weight(1f),
                        )
                        FilterChip(
                            selected = state.tempUnit == "F",
                            onClick = { viewModel.setTempUnit("F") },
                            label = { Text("Fahrenheit") },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    // Auto refresh interval
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Auto-refresh interval", style = MaterialTheme.typography.bodyMedium)
                        Text("${state.autoRefreshInterval} min", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = state.autoRefreshInterval.toFloat(),
                        onValueChange = { viewModel.setAutoRefreshInterval(it.toInt()) },
                        valueRange = 5f..60f,
                        steps = 10,
                    )
                }
            }

            // Vehicle images section
            if (state.vehicles.isNotEmpty()) {
                item {
                    SettingsSection(title = "Vehicle Images") {
                        state.vehicles.forEach { vehicle ->
                            VehicleImageRow(vehicle = vehicle, onImagePicked = { uri ->
                                viewModel.setCustomImage(vehicle.vin, uri)
                            })
                            if (vehicle != state.vehicles.last()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }
            }

            // Auto-lock section
            if (state.vehicles.isNotEmpty()) {
                item {
                    SettingsSection(title = "Walk-Away Auto Lock") {
                        Text(
                            "When your phone disconnects from the car's Bluetooth, automatically lock the car after the specified delay.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        state.vehicles.forEach { vehicle ->
                            AutoLockRow(
                                vehicle = vehicle,
                                config = state.autoLockConfigs[vehicle.vin],
                                onSave = { enabled, deviceName, deviceAddress, delay ->
                                    viewModel.setAutoLock(vehicle.vin, enabled, deviceName, deviceAddress, delay)
                                },
                            )
                            if (vehicle != state.vehicles.last()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }
            }

            // Debug Log section
            item {
                SettingsSection(title = "Debug Log") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Blue2 Log", state.appLog))
                        }) {
                            Icon(Icons.Rounded.ContentCopy, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Copy")
                        }
                        TextButton(onClick = viewModel::clearLog) {
                            Icon(Icons.Rounded.Delete, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Clear")
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                    ) {
                        Text(
                            text = state.appLog.ifEmpty { "(no log entries yet)" },
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState()),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Sign Out?") },
            text = { Text("You will need to sign in again to control your vehicles.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        viewModel.logout()
                        onLoggedOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Sign Out") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(shape = MaterialTheme.shapes.extraLarge) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingsSwitch(
    label: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun VehicleImageRow(vehicle: Vehicle, onImagePicked: (android.net.Uri?) -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        onImagePicked(uri)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(vehicle.nickname, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(vehicle.vin, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (vehicle.customImageUri != null) {
                TextButton(onClick = { onImagePicked(null) }) { Text("Remove") }
            }
            OutlinedButton(onClick = { launcher.launch("image/*") }) {
                Icon(Icons.Rounded.Image, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (vehicle.customImageUri != null) "Change" else "Set Image")
            }
        }
    }
}

@Composable
private fun AutoLockRow(
    vehicle: Vehicle,
    config: com.blue2.app.data.local.database.AutoLockConfigEntity?,
    onSave: (Boolean, String?, String?, Int) -> Unit,
) {
    val context = LocalContext.current
    var enabled by remember(config) { mutableStateOf(config?.enabled ?: false) }
    var delay by remember(config) { mutableStateOf(config?.delaySeconds ?: 30) }
    var selectedDeviceName by remember(config) { mutableStateOf(config?.bluetoothDeviceName) }
    var selectedDeviceAddress by remember(config) { mutableStateOf(config?.bluetoothDeviceAddress) }
    var showPicker by remember { mutableStateOf(false) }

    // Permission launcher for BLUETOOTH_CONNECT (API 31+)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) showPicker = true }

    fun launchPicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permission = Manifest.permission.BLUETOOTH_CONNECT
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                showPicker = true
            } else {
                permissionLauncher.launch(permission)
            }
        } else {
            showPicker = true
        }
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(vehicle.nickname, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                if (selectedDeviceName != null) {
                    Text(
                        "Trigger: $selectedDeviceName",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else if (enabled) {
                    Text(
                        "No device selected — tap to choose",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Switch(checked = enabled, onCheckedChange = { v ->
                enabled = v
                onSave(v, selectedDeviceName, selectedDeviceAddress, delay)
            })
        }

        if (enabled) {
            Spacer(Modifier.height(8.dp))

            // Bluetooth device picker button
            OutlinedButton(
                onClick = { launchPicker() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.Bluetooth, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(selectedDeviceName ?: "Select Bluetooth Device")
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Lock delay: ${delay}s", style = MaterialTheme.typography.bodySmall)
            }
            Slider(
                value = delay.toFloat(),
                onValueChange = { delay = it.toInt() },
                valueRange = 10f..120f,
                onValueChangeFinished = {
                    onSave(enabled, selectedDeviceName, selectedDeviceAddress, delay)
                },
            )
        }
    }

    // Bluetooth device picker dialog
    if (showPicker) {
        val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
        val pairedDevices = remember {
            try {
                bluetoothManager?.adapter?.bondedDevices?.toList() ?: emptyList()
            } catch (e: SecurityException) {
                emptyList()
            }
        }

        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Select Bluetooth Device") },
            text = {
                if (pairedDevices.isEmpty()) {
                    Text(
                        "No paired Bluetooth devices found. Pair your car's Bluetooth in Android Settings first.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items(pairedDevices) { device ->
                            val name = try { device.name ?: device.address } catch (e: SecurityException) { device.address }
                            val address = device.address
                            ListItem(
                                headlineContent = { Text(name) },
                                supportingContent = { Text(address, style = MaterialTheme.typography.labelSmall) },
                                leadingContent = { Icon(Icons.Rounded.Bluetooth, null) },
                                modifier = Modifier.clickable {
                                    selectedDeviceName = name
                                    selectedDeviceAddress = address
                                    onSave(enabled, name, address, delay)
                                    showPicker = false
                                },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            },
        )
    }
}
