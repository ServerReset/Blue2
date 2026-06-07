package com.blue2.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blue2.app.ui.theme.*

@Composable
fun RangeBar(
    percent: Float,
    rangeKm: Double?,
    isCharging: Boolean = false,
    isPluggedIn: Boolean = false,
    label: String,
    rangeUnit: String = "km",
    modifier: Modifier = Modifier,
    height: Dp = 12.dp,
) {
    val animatedPercent by animateFloatAsState(
        targetValue = percent.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "rangePercent",
    )

    // Charging shimmer animation
    val shimmerTranslate by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label = "shimmerTranslate",
    )

    val barColor = when {
        isCharging -> ChargeChargingColor
        percent >= 0.5f -> ChargeHighColor
        percent >= 0.2f -> ChargeMidColor
        else -> ChargeLowColor
    }

    val displayRange = rangeKm?.let {
        if (rangeUnit == "mi") "%.0f mi".format(it * 0.621371) else "%.0f km".format(it)
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (isCharging) {
                    Surface(
                        color = ChargeChargingColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            text = "⚡ Charging",
                            style = MaterialTheme.typography.labelSmall,
                            color = ChargeChargingColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                } else if (isPluggedIn) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            text = "🔌 Plugged in",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "%.0f%%".format(percent * 100),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = barColor,
                )
                if (displayRange != null) {
                    Text(
                        text = displayRange,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
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
                                colors = listOf(
                                    barColor,
                                    barColor.copy(alpha = 0.7f),
                                    barColor,
                                ),
                                startX = shimmerTranslate * 300f,
                                endX = shimmerTranslate * 300f + 300f,
                            )
                        } else {
                            Brush.horizontalGradient(colors = listOf(barColor, barColor.copy(alpha = 0.85f)))
                        }
                    ),
            )
        }
    }
}

@Composable
fun DualRangeBar(
    evPercent: Float?,
    evRangeKm: Double?,
    fuelPercent: Float?,
    fuelRangeKm: Double?,
    isCharging: Boolean = false,
    isPluggedIn: Boolean = false,
    rangeUnit: String = "km",
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        evPercent?.let {
            RangeBar(
                percent = it,
                rangeKm = evRangeKm,
                isCharging = isCharging,
                isPluggedIn = isPluggedIn,
                label = "Electric Range",
                rangeUnit = rangeUnit,
            )
        }
        fuelPercent?.let {
            RangeBar(
                percent = it,
                rangeKm = fuelRangeKm,
                label = "Fuel Range",
                rangeUnit = rangeUnit,
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
