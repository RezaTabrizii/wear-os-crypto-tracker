package io.github.rezatabrizii.cryptotracker.data

import android.content.Context
import android.content.SharedPreferences

/** Last known price. Everything (app, tile, complication) reads from here; only the repository writes. */
data class PriceSnapshot(
    val toman: Long,
    val source: String,
    val fetchedAtMillis: Long,
)

class PriceStore(context: Context) {
    val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun snapshot(): PriceSnapshot? {
        val toman = prefs.getLong(KEY_TOMAN, 0L)
        if (toman <= 0L) return null
        return PriceSnapshot(
            toman = toman,
            source = prefs.getString(KEY_SOURCE, null).orEmpty(),
            fetchedAtMillis = prefs.getLong(KEY_FETCHED_AT, 0L),
        )
    }

    /** Error from the latest attempt, or null when the latest attempt succeeded. */
    fun lastError(): String? = prefs.getString(KEY_LAST_ERROR, null)

    fun saveSuccess(price: FetchedPrice, nowMillis: Long) {
        prefs.edit()
            .putLong(KEY_TOMAN, price.toman)
            .putString(KEY_SOURCE, price.source)
            .putLong(KEY_FETCHED_AT, nowMillis)
            .remove(KEY_LAST_ERROR)
            .apply()
    }

    fun saveError(message: String) {
        prefs.edit().putString(KEY_LAST_ERROR, message).apply()
    }

    companion object {
        private const val PREFS_NAME = "price_store"
        private const val KEY_TOMAN = "toman"
        private const val KEY_SOURCE = "source"
        private const val KEY_FETCHED_AT = "fetched_at"
        private const val KEY_LAST_ERROR = "last_error"
    }
}
