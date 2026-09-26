package io.github.rezatabrizii.cryptotracker.data

import android.content.ComponentName
import android.content.Context
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import io.github.rezatabrizii.cryptotracker.complication.PriceComplicationService
import io.github.rezatabrizii.cryptotracker.tile.PriceTileService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Single entry point for refreshing the price. Used by the worker and the app's refresh button. */
object PriceRepository {
    private val mutex = Mutex()
    private val fetcher = PriceFetcher()

    /** Fetches, stores and pushes the new price to the tile/complication. Returns true on success. */
    suspend fun refresh(context: Context): Boolean = mutex.withLock {
        val appContext = context.applicationContext
        val store = PriceStore(appContext)
        val previous = store.snapshot()
        try {
            val price = withContext(Dispatchers.IO) { fetcher.fetch() }
            store.saveSuccess(price, System.currentTimeMillis())
            // Tile shows the update time, so it is always refreshed; complications only when the price changed.
            TileService.getUpdater(appContext).requestUpdate(PriceTileService::class.java)
            if (previous?.toman != price.toman) {
                ComplicationDataSourceUpdateRequester
                    .create(appContext, ComponentName(appContext, PriceComplicationService::class.java))
                    .requestUpdateAll()
            }
            true
        } catch (e: Exception) {
            store.saveError(e.message ?: e.javaClass.simpleName)
            false
        }
    }
}
