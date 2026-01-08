package com.codetech.binaryoptionstradingsignalapp.abstraction.simulator

import com.codetech.binaryoptionstradingsignalapp.abstraction.data.model.Candle
import com.codetech.binaryoptionstradingsignalapp.abstraction.enums.CurrencyPair
import kotlin.math.sin
import kotlin.random.Random

class MarketDataSimulator {

    private val basePrice = mapOf(
        CurrencyPair.EURUSD_OTC to 1.0850,
        CurrencyPair.GBPUSD_OTC to 1.2650,
        CurrencyPair.USDJPY_OTC to 148.50,
        CurrencyPair.AUDUSD_OTC to 0.6650,
        CurrencyPair.USDCAD_OTC to 1.3550,
        CurrencyPair.USDCHF_OTC to 0.8750,
        CurrencyPair.NZDUSD_OTC to 0.6150,
        CurrencyPair.EURJPY_OTC to 161.50
    )

    private val volatility = 0.0005 // 0.05% per candle
    private var candleCounter = 0

    // Generate realistic historical candles
    fun generateHistoricalCandles(pair: CurrencyPair, count: Int): List<Candle> {
        val candles = mutableListOf<Candle>()
        var currentPrice = basePrice[pair] ?: 1.0
        val currentTime = System.currentTimeMillis()

        for (i in 0 until count) {
            val timestamp = currentTime - ((count - i) * 60 * 1000L) // M1 = 1 minute

            // Add trend component (sine wave)
            val trend = sin(i * 0.1) * volatility * 2

            // Add random walk
            val change = (Random.Default.nextDouble(-1.0, 1.0) * volatility) + trend
            currentPrice *= (1 + change)

            val open = currentPrice
            val close = currentPrice * (1 + Random.Default.nextDouble(-volatility, volatility))

            // Generate high and low based on candle direction
            val high: Double
            val low: Double

            if (close > open) {
                // Bullish candle
                high = close + (close - open) * Random.Default.nextDouble(0.1, 0.3)
                low = open - (close - open) * Random.Default.nextDouble(0.05, 0.15)
            } else {
                // Bearish candle
                high = open + (open - close) * Random.Default.nextDouble(0.1, 0.3)
                low = close - (open - close) * Random.Default.nextDouble(0.05, 0.15)
            }

            candles.add(
                Candle(
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    timestamp = timestamp,
                    volume = Random.Default.nextDouble(100.0, 1000.0)
                )
            )

            currentPrice = close
        }

        return candles
    }

    // Generate next candle (for live updates)
    fun generateNextCandle(pair: CurrencyPair, previousCandle: Candle): Candle {
        candleCounter++

        val trend = sin(candleCounter * 0.1) * volatility * 2
        val change = (Random.Default.nextDouble(-1.0, 1.0) * volatility) + trend

        val open = previousCandle.close
        val close = open * (1 + change)

        val high: Double
        val low: Double

        if (close > open) {
            high = close + (close - open) * Random.Default.nextDouble(0.1, 0.3)
            low = open - (close - open) * Random.Default.nextDouble(0.05, 0.15)
        } else {
            high = open + (open - close) * Random.Default.nextDouble(0.1, 0.3)
            low = close - (open - close) * Random.Default.nextDouble(0.05, 0.15)
        }

        return Candle(
            open = open,
            high = high,
            low = low,
            close = close,
            timestamp = System.currentTimeMillis(),
            volume = Random.Default.nextDouble(100.0, 1000.0)
        )
    }
}