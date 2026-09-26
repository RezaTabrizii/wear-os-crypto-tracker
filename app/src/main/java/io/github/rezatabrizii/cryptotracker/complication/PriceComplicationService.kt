package io.github.rezatabrizii.cryptotracker.complication

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import io.github.rezatabrizii.cryptotracker.data.PriceFormat
import io.github.rezatabrizii.cryptotracker.data.PriceStore
import io.github.rezatabrizii.cryptotracker.presentation.MainActivity
import io.github.rezatabrizii.cryptotracker.work.RefreshScheduler

/**
 * Watch-face complication showing the cached price.
 * UPDATE_PERIOD_SECONDS is 0 in the manifest: the system never polls; PriceRepository pushes updates.
 */
class PriceComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? = build(type, 112_350L)

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        RefreshScheduler.refreshIfStale(this)
        return build(request.complicationType, PriceStore(this).snapshot()?.toman)
    }

    private fun build(type: ComplicationType, toman: Long?): ComplicationData? {
        val description = text(toman?.let { "USDT ${PriceFormat.full(it)} Toman" } ?: "USDT price unavailable")
        return when (type) {
            ComplicationType.SHORT_TEXT ->
                ShortTextComplicationData.Builder(text(toman?.let(PriceFormat::compact) ?: "--"), description)
                    .setTitle(text("USDT"))
                    .setTapAction(openAppIntent())
                    .build()

            ComplicationType.LONG_TEXT ->
                LongTextComplicationData.Builder(text(toman?.let { "${PriceFormat.full(it)} T" } ?: "--"), description)
                    .setTitle(text("USDT"))
                    .setTapAction(openAppIntent())
                    .build()

            else -> null
        }
    }

    private fun text(value: String) = PlainComplicationText.Builder(value).build()

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
