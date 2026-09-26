package io.github.rezatabrizii.cryptotracker.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class FetchedPrice(val toman: Long, val source: String)

/**
 * Tries each source in order and returns the first valid price.
 * [httpGet] is injectable so the fallback logic is unit-testable without network.
 */
class PriceFetcher(
    private val sources: List<PriceSource> = DEFAULT_SOURCES,
    private val httpGet: (String) -> String = ::defaultHttpGet,
) {
    /** @throws IOException with a combined message when every source fails. */
    fun fetch(): FetchedPrice {
        val errors = mutableListOf<String>()
        for (source in sources) {
            try {
                val toman = source.parseToman(httpGet(source.url))
                return FetchedPrice(toman, source.name)
            } catch (e: Exception) {
                errors += "${source.name}: ${e.message ?: e.javaClass.simpleName}"
            }
        }
        throw IOException(errors.joinToString("; "))
    }
}

private const val CONNECT_TIMEOUT_MS = 10_000
private const val READ_TIMEOUT_MS = 15_000

/** Plain HttpURLConnection: no extra dependencies. On a watch it transparently uses the phone's internet. */
fun defaultHttpGet(url: String): String {
    val connection = URL(url).openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android; Wear OS) CryptoTracker/1.0")
        val code = connection.responseCode
        if (code !in 200..299) throw IOException("HTTP $code")
        return connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}
