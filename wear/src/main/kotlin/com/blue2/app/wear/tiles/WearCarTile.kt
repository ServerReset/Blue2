package com.blue2.app.wear.tiles

import android.content.Context
import androidx.wear.protolayout.*
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.expression.ProtoLayoutExpressions
import androidx.wear.tiles.*
import com.blue2.app.domain.repository.IBluelinkRepository
import com.google.android.horologist.tiles.SuspendingTileService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WearCarTile : SuspendingTileService() {

    @Inject lateinit var repository: IBluelinkRepository

    override suspend fun resourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ResourceBuilders.Resources =
        ResourceBuilders.Resources.Builder()
            .setVersion("1")
            .build()

    override suspend fun tileRequest(requestParams: RequestBuilders.TileRequest): TileBuilders.Tile {
        val vehicles = repository.getVehicles().getOrNull()
        val vehicle = vehicles?.firstOrNull()
        val status = vehicle?.let { repository.getVehicleStatus(it.vin).getOrNull() }

        val layout = buildTileLayout(vehicle?.nickname ?: "Blue2", status)

        return TileBuilders.Tile.Builder()
            .setResourcesVersion("1")
            .setFreshnessIntervalMillis(300_000) // 5 min
            .setTileTimeline(
                TimelineBuilders.Timeline.Builder()
                    .addTimelineEntry(
                        TimelineBuilders.TimelineEntry.Builder()
                            .setLayout(LayoutElementBuilders.Layout.Builder().setRoot(layout).build())
                            .build()
                    )
                    .build()
            )
            .build()
    }

    private fun buildTileLayout(
        vehicleName: String,
        status: com.blue2.app.domain.models.VehicleStatus?,
    ): LayoutElementBuilders.LayoutElement {
        val lockText = if (status?.isLocked == true) "Locked 🔒" else "Unlocked 🔓"
        val battText = status?.evBatteryPercent?.let { "⚡ $it%" } ?: ""
        val fuelText = status?.fuelLevelPercent?.let { "⛽ $it%" } ?: ""

        return LayoutElementBuilders.Column.Builder()
            .setWidth(DimensionBuilders.expand())
            .setHeight(DimensionBuilders.expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .addContent(
                LayoutElementBuilders.Text.Builder()
                    .setText(vehicleName)
                    .setFontStyle(
                        LayoutElementBuilders.FontStyle.Builder()
                            .setSize(DimensionBuilders.SpProp.Builder().setValue(16f).build())
                            .setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                            .build()
                    )
                    .build()
            )
            .addContent(
                LayoutElementBuilders.Text.Builder()
                    .setText(lockText)
                    .setFontStyle(
                        LayoutElementBuilders.FontStyle.Builder()
                            .setSize(DimensionBuilders.SpProp.Builder().setValue(13f).build())
                            .build()
                    )
                    .build()
            )
            .apply {
                if (battText.isNotEmpty()) {
                    addContent(
                        LayoutElementBuilders.Text.Builder()
                            .setText(battText)
                            .build()
                    )
                }
                if (fuelText.isNotEmpty()) {
                    addContent(
                        LayoutElementBuilders.Text.Builder()
                            .setText(fuelText)
                            .build()
                    )
                }
            }
            .build()
    }
}
