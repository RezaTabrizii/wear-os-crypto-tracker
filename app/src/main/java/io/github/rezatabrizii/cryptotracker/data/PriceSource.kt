package io.github.rezatabrizii.cryptotracker.data

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.math.BigDecimal
import java.math.RoundingMode

/** One exchange that can report the USDT price in Toman. */
interface PriceSource {
    val name: String
    val url: String

    /** Parses the raw HTTP body into a Toman price. Throws on unexpected data. */
    fun parseToman(body: String): Long
}

/**
 * Nobitex: `stats["usdt-rls"].latest` is the last trade price in **Rial** (as a string).
 * Rial / 10 = Toman.
 */
object NobitexSource : PriceSource {
    override val name = "Nobitex"
    override val url = "https://api.nobitex.ir/market/stats?srcCurrency=usdt&dstCurrency=rls"

    override fun parseToman(body: String): Long {
        val root = JSONObject(body)
        val status = root.optString("status")
        require(status == "ok") { "status=$status" }
        val market = root.getJSONObject("stats").getJSONObject("usdt-rls")
        val rial = market.decimalOrNull("latest")
            ?: midPrice(market.decimalOrNull("bestBuy"), market.decimalOrNull("bestSell"))
            ?: error("no price fields")
        return rial.divide(BigDecimal.TEN).toTomanLong()
    }
}

/**
 * Wallex: `result.symbols.USDTTMN.stats.lastPrice` is already in **Toman** (as a string).
 */
object WallexSource : PriceSource {
    override val name = "Wallex"
    override val url = "https://api.wallex.ir/v1/markets"

    override fun parseToman(body: String): Long {
        val root = JSONObject(body)
        require(root.optBoolean("success", true)) { root.optString("message", "success=false") }
        val stats = root.getJSONObject("result")
            .getJSONObject("symbols")
            .getJSONObject("USDTTMN")
            .getJSONObject("stats")
        val toman = stats.decimalOrNull("lastPrice")
            ?: midPrice(stats.decimalOrNull("bidPrice"), stats.decimalOrNull("askPrice"))
            ?: error("no price fields")
        return toman.toTomanLong()
    }
}

/**
 * Bitpin: a JSON array of tickers; `USDT_IRT.price` is in **Toman** (IRT = Toman).
 */
object BitpinSource : PriceSource {
    override val name = "Bitpin"
    override val url = "https://api.bitpin.org/api/v1/mkt/tickers/"

    override fun parseToman(body: String): Long {
        val tickers = when (val json = JSONTokener(body).nextValue()) {
            is JSONArray -> json
            is JSONObject -> json.optJSONArray("results") ?: error("no tickers array")
            else -> error("unexpected JSON")
        }
        for (i in 0 until tickers.length()) {
            val ticker = tickers.optJSONObject(i) ?: continue
            if (ticker.optString("symbol") == "USDT_IRT") {
                val toman = ticker.decimalOrNull("price") ?: error("no price field")
                return toman.toTomanLong()
            }
        }
        error("USDT_IRT not found")
    }
}

/** Order matters: first source is preferred, the others are fallbacks. */
val DEFAULT_SOURCES: List<PriceSource> = listOf(NobitexSource, WallexSource, BitpinSource)

/** Rejects zero/garbage values so a broken response never replaces a good cached price. */
internal val SANE_TOMAN_RANGE = 1_000L..100_000_000L

private fun JSONObject.decimalOrNull(key: String): BigDecimal? {
    if (!has(key) || isNull(key)) return null
    return get(key).toString().trim().replace(",", "").toBigDecimalOrNull()
        ?.takeIf { it.signum() > 0 }
}

private fun midPrice(a: BigDecimal?, b: BigDecimal?): BigDecimal? =
    if (a != null && b != null) a.add(b).divide(BigDecimal(2)) else a ?: b

private fun BigDecimal.toTomanLong(): Long {
    val value = setScale(0, RoundingMode.HALF_UP).toLong()
    require(value in SANE_TOMAN_RANGE) { "price out of range: $value" }
    return value
}
