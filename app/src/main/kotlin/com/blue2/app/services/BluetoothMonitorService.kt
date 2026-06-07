package com.blue2.app.services

import android.app.*
import android.bluetooth.*
import android.content.*
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.blue2.app.Blue2Application
import com.blue2.app.R
import com.blue2.app.data.local.database.AutoLockConfigDao
import com.blue2.app.domain.repository.IBluelinkRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Foreground service that monitors Bluetooth connections.
 * When a configured vehicle's Bluetooth disconnects, triggers auto-lock after a delay.
 */
@AndroidEntryPoint
class BluetoothMonitorService : Service() {

    @Inject lateinit var repository: IBluelinkRepository
    @Inject lateinit var autoLockConfigDao: AutoLockConfigDao

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pendingLocks = mutableMapOf<String, Job>()

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device?.let { onBluetoothDisconnected(it) }
                }
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val device = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device?.let { cancelPendingLock(it.address) }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerBluetoothReceiver()
        startForeground(NOTIF_ID, buildForegroundNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothReceiver)
        scope.cancel()
    }

    private fun registerBluetoothReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        }
        registerReceiver(bluetoothReceiver, filter)
    }

    private fun onBluetoothDisconnected(device: BluetoothDevice) {
        scope.launch {
            val configs = autoLockConfigDao.getEnabledConfigs()
            val matchingConfig = configs.firstOrNull { it.bluetoothDeviceAddress == device.address }
                ?: return@launch

            notify("Auto-Lock Pending", "${matchingConfig.vin.take(8)}: Locking in ${matchingConfig.delaySeconds}s")

            val job = scope.launch {
                delay(matchingConfig.delaySeconds * 1000L)
                repository.lock(matchingConfig.vin)
                    .onSuccess { notify("Auto-Locked", "Vehicle ${matchingConfig.vin.take(8)} locked automatically") }
                    .onFailure { e -> notify("Auto-Lock Failed", "Could not lock vehicle: ${e.message}") }
            }
            pendingLocks[device.address] = job
        }
    }

    private fun cancelPendingLock(address: String) {
        pendingLocks.remove(address)?.cancel()
    }

    private fun buildForegroundNotification(): Notification =
        NotificationCompat.Builder(this, Blue2Application.CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_car)
            .setContentTitle("Blue2 Active")
            .setContentText("Monitoring Bluetooth for auto-lock")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun notify(title: String, text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        val notif = NotificationCompat.Builder(this, Blue2Application.CHANNEL_AUTO_LOCK)
            .setSmallIcon(R.drawable.ic_car)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIF_AUTO_LOCK_ID, notif)
    }

    companion object {
        private const val NOTIF_ID = 1001
        private const val NOTIF_AUTO_LOCK_ID = 1002

        fun start(context: Context) {
            val intent = Intent(context, BluetoothMonitorService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BluetoothMonitorService::class.java))
        }
    }
}
