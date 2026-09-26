package io.github.rezatabrizii.cryptotracker.tile

import android.content.Context
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import io.github.rezatabrizii.cryptotracker.data.PriceFormat
import io.github.rezatabrizii.cryptotracker.data.PriceSnapshot
import io.github.rezatabrizii.cryptotracker.data.PriceStore
import io.github.rezatabrizii.cryptotracker.presentation.MainActivity
import io.github.rezatabrizii.cryptotracker.work.RefreshScheduler

private const val RESOURCES_VERSION = "1"
private const val COLOR_USDT = 0xFF26A17B.toInt()
private const val COLOR_TEXT = 0xFFFFFFFF.toInt()
private const val COLOR_SECONDARY = 0xFFAAAAAA.toInt()

/**
 * Tile that only renders the cached price (no network here).
 * The worker pushes updates via TileService.getUpdater(), so no periodic tile refresh is needed.
 */
class PriceTileService : TileService() {

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        RefreshScheduler.refreshIfStale(this)
        val layout = tileLayout(this, requestParams.deviceConfiguration, PriceStore(this).snapshot())
        val tile = TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setFreshnessIntervalMillis(0) // 0 = never auto-refresh; updates are pushed.
            .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout))
            .build()
        return immediateFuture(tile)
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<ResourceBuilders.Resources> =
        immediateFuture(ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build())
}

private fun tileLayout(
    context: Context,
    deviceParameters: DeviceParameters,
    snapshot: PriceSnapshot?,
): LayoutElementBuilders.LayoutElement {
    val price = snapshot?.let { PriceFormat.full(it.toman) } ?: "--"
    val footer = snapshot?.let { "Toman · ${PriceFormat.time(it.fetchedAtMillis)}" } ?: "Toman"

    val content = PrimaryLayout.Builder(deviceParameters)
        .setPrimaryLabelTextContent(
            Text.Builder(context, "USDT")
                .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                .setColor(argb(COLOR_USDT))
                .build(),
        )
        .setContent(
            Text.Builder(context, price)
                .setTypography(Typography.TYPOGRAPHY_DISPLAY3)
                .setColor(argb(COLOR_TEXT))
                .build(),
        )
        .setSecondaryLabelTextContent(
            Text.Builder(context, footer)
                .setTypography(Typography.TYPOGRAPHY_CAPTION2)
                .setColor(argb(COLOR_SECONDARY))
                .build(),
        )
        .build()

    // Tapping anywhere on the tile opens the app.
    val openApp = ModifiersBuilders.Clickable.Builder()
        .setId("open_app")
        .setOnClick(
            ActionBuilders.LaunchAction.Builder()
                .setAndroidActivity(
                    ActionBuilders.AndroidActivity.Builder()
                        .setPackageName(context.packageName)
                        .setClassName(MainActivity::class.java.name)
                        .build(),
                )
                .build(),
        )
        .build()

    return LayoutElementBuilders.Box.Builder()
        .setWidth(expand())
        .setHeight(expand())
        .setModifiers(ModifiersBuilders.Modifiers.Builder().setClickable(openApp).build())
        .addContent(content)
        .build()
}

private fun <T> immediateFuture(value: T): ListenableFuture<T> =
    CallbackToFutureAdapter.getFuture { completer ->
        completer.set(value)
        "immediateFuture"
    }
