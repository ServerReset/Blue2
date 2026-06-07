package com.blue2.app.wear.tiles

import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import com.blue2.app.domain.models.VehicleStatus
import com.blue2.app.domain.repository.IBluelinkRepository
import com.google.android.horologist.tiles.SuspendingTileService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WearCarTile : SuspendingTileService() {

    @Inject lateinit var repository: IBluelinkRepository

    override suspend fun resourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ResourceBuilders.Resources = ResourceBuilders.Resources.Builder().setVersion("1").build()

    override suspend fun tileRequest(
        requestParams: RequestBuilders.TileRequest,
    ): TileBuilders.Tile {
        val vehicle = repository.getVehicles().getOrNull()?.firstOrNull()
        val status = vehicle?.let { repository.getVehicleStatus(it.vin).getOrNull() }
        val label = buildLabel(vehicle?.nickname ?: "Blue2", status)

        return TileBuilders.Tile.Builder()
            .setResourcesVersion("1")
            .setFreshnessIntervalMillis(300_000L)
            .setTileTimeline(
                TimelineBuilders.Timeline.Builder()
                    .addTimelineEntry(
                        TimelineBuilders.TimelineEntry.Builder()
                            .setLayout(
                                LayoutElementBuilders.Layout.Builder()
                                    .setRoot(
                                        LayoutElementBuilders.Text.Builder()
                                            .setText(label)
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()
    }

    private fun buildLabel(name: String, status: VehicleStatus?): String = buildString {
        append(name)
        if (status != null) {
            append("  ")
            append(if (status.isLocked) "🔒" else "🔓")
            status.evBatteryPercent?.let { append(" ⚡$it%") }
            status.fuelLevelPercent?.let { append(" ⛽$it%") }
            if (status.engineRunning) append(" 🟢")
            if (status.evCharging) append(" ↑")
        }
    }
}
