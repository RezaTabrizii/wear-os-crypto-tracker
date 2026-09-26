package io.github.rezatabrizii.cryptotracker.data

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PriceFormat {
    private val symbols = DecimalFormatSymbols(Locale.US)

    /** 112350 -> "112,350" */
    fun full(toman: Long): String = DecimalFormat("#,###", symbols).format(toman)

    /** Short form for small complications (max ~7 chars): 112350 -> "112.4K", 1234567 -> "1.23M". */
    fun compact(toman: Long): String = when {
        toman < 10_000 -> full(toman)
        toman < 1_000_000 -> scaled(toman, 1_000, decimals = 1) + "K"
        else -> scaled(toman, 1_000_000, decimals = 2) + "M"
    }

    // BigDecimal avoids binary rounding surprises (112.35 as a double is 112.3499...).
    private fun scaled(value: Long, divisor: Long, decimals: Int): String =
        BigDecimal.valueOf(value)
            .divide(BigDecimal.valueOf(divisor))
            .setScale(decimals, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()

    /** Absolute time ("14:05") so screens never need a redraw just to update a relative age. */
    fun time(epochMillis: Long): String = SimpleDateFormat("HH:mm", Locale.US).format(Date(epochMillis))
}
