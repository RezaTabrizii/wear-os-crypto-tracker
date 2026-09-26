package io.github.rezatabrizii.cryptotracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class PriceSourceTest {

    @Test
    fun nobitex_convertsRialToToman() {
        val body = """
            {"status":"ok","stats":{"usdt-rls":{"isClosed":false,"bestSell":"1125000","bestBuy":"1123000",
            "latest":"1124505","dayLow":"1110000","dayHigh":"1130000","dayChange":"0.45"}}}
        """.trimIndent()
        assertEquals(112_451L, NobitexSource.parseToman(body))
    }

    @Test
    fun nobitex_fallsBackToMidPrice_whenLatestMissing() {
        val body = """{"status":"ok","stats":{"usdt-rls":{"bestSell":"1125000","bestBuy":"1123000"}}}"""
        assertEquals(112_400L, NobitexSource.parseToman(body))
    }

    @Test
    fun nobitex_rejectsErrorStatus() {
        assertThrows { NobitexSource.parseToman("""{"status":"failed","message":"rate limited"}""") }
    }

    @Test
    fun wallex_readsTomanLastPrice() {
        val body = """
            {"result":{"symbols":{
              "BTCTMN":{"symbol":"BTCTMN","stats":{"lastPrice":"9000000000.0000000000000000"}},
              "USDTTMN":{"symbol":"USDTTMN","stats":{"bidPrice":"112300","askPrice":"112500",
                "lastPrice":"112400.0000000000000000","24h_ch":"-0.3"}}}},
             "message":"The operation was successful","success":true}
        """.trimIndent()
        assertEquals(112_400L, WallexSource.parseToman(body))
    }

    @Test
    fun wallex_usesMidPrice_whenLastPriceIsDash() {
        val body = """
            {"result":{"symbols":{"USDTTMN":{"stats":{"bidPrice":"112300","askPrice":"112500","lastPrice":"-"}}}},"success":true}
        """.trimIndent()
        assertEquals(112_400L, WallexSource.parseToman(body))
    }

    @Test
    fun bitpin_findsUsdtIrtInArray() {
        val body = """
            [{"symbol":"BTC_USDT","price":"95000.10","daily_change_price":-1.2,"low":"1","high":"2","timestamp":1},
             {"symbol":"USDT_IRT","price":"112350","daily_change_price":0.5,"low":"111000","high":"113000","timestamp":1}]
        """.trimIndent()
        assertEquals(112_350L, BitpinSource.parseToman(body))
    }

    @Test
    fun bitpin_acceptsPaginatedWrapper() {
        val body = """{"results":[{"symbol":"USDT_IRT","price":"112350.4"}]}"""
        assertEquals(112_350L, BitpinSource.parseToman(body))
    }

    @Test
    fun bitpin_failsWhenSymbolMissing() {
        assertThrows { BitpinSource.parseToman("""[{"symbol":"BTC_USDT","price":"1"}]""") }
    }

    @Test
    fun rejectsOutOfRangePrice() {
        assertThrows { BitpinSource.parseToman("""[{"symbol":"USDT_IRT","price":"0"}]""") }
        assertThrows { BitpinSource.parseToman("""[{"symbol":"USDT_IRT","price":"5"}]""") }
    }

    @Test
    fun fetcher_usesFirstWorkingSource() {
        val responses = mapOf(
            NobitexSource.url to { throw IOException("HTTP 503") },
            WallexSource.url to { "not json" },
            BitpinSource.url to { """[{"symbol":"USDT_IRT","price":"112350"}]""" },
        )
        val fetcher = PriceFetcher(DEFAULT_SOURCES) { url -> responses.getValue(url)() }
        assertEquals(FetchedPrice(112_350L, "Bitpin"), fetcher.fetch())
    }

    @Test
    fun fetcher_prefersNobitex() {
        val calls = mutableListOf<String>()
        val fetcher = PriceFetcher(DEFAULT_SOURCES) { url ->
            calls += url
            """{"status":"ok","stats":{"usdt-rls":{"latest":"1123500"}}}"""
        }
        assertEquals(FetchedPrice(112_350L, "Nobitex"), fetcher.fetch())
        assertEquals(listOf(NobitexSource.url), calls)
    }

    @Test
    fun fetcher_reportsAllErrors_whenEverySourceFails() {
        val fetcher = PriceFetcher(DEFAULT_SOURCES) { throw IOException("offline") }
        try {
            fetcher.fetch()
            fail("expected IOException")
        } catch (e: IOException) {
            val message = e.message.orEmpty()
            assertTrue(message, "Nobitex: offline" in message && "Wallex: offline" in message && "Bitpin: offline" in message)
        }
    }

    private fun assertThrows(block: () -> Unit) {
        try {
            block()
        } catch (_: Exception) {
            return
        }
        fail("expected exception")
    }
}
