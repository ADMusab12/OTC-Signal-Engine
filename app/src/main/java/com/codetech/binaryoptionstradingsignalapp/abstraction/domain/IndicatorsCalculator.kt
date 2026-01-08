package com.codetech.binaryoptionstradingsignalapp.abstraction.domain

import com.codetech.binaryoptionstradingsignalapp.abstraction.data.model.Candle
import com.codetech.binaryoptionstradingsignalapp.abstraction.data.model.TechnicalIndicators
import kotlin.collections.map
import kotlin.math.abs
import kotlin.math.max

class IndicatorsCalculator {
    // Calculate EMA (Exponential Moving Average)
    fun calculateEMA(prices: List<Double>, period: Int): Double {
        if (prices.size < period) return prices.lastOrNull() ?: 0.0
        
        val multiplier = 2.0 / (period + 1)
        var ema = prices.take(period).average() // Start with SMA
        
        for (i in period until prices.size) {
            ema = (prices[i] - ema) * multiplier + ema
        }
        
        return ema
    }
    
    // Calculate RSI (Relative Strength Index)
    fun calculateRSI(prices: List<Double>, period: Int = 14): Double {
        if (prices.size < period + 1) return 50.0
        
        val changes = mutableListOf<Double>()
        for (i in 1 until prices.size) {
            changes.add(prices[i] - prices[i - 1])
        }
        
        if (changes.size < period) return 50.0
        
        var avgGain = 0.0
        var avgLoss = 0.0
        
        // First average
        for (i in 0 until period) {
            if (changes[i] > 0) avgGain += changes[i]
            else avgLoss += abs(changes[i])
        }
        avgGain /= period
        avgLoss /= period
        
        // Smooth subsequent values
        for (i in period until changes.size) {
            if (changes[i] > 0) {
                avgGain = (avgGain * (period - 1) + changes[i]) / period
                avgLoss = (avgLoss * (period - 1)) / period
            } else {
                avgGain = (avgGain * (period - 1)) / period
                avgLoss = (avgLoss * (period - 1) + abs(changes[i])) / period
            }
        }
        
        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }
    
    // Calculate ADX (Average Directional Index)
    fun calculateADX(candles: List<Candle>, period: Int = 14): Double {
        if (candles.size < period + 1) return 0.0
        
        val trList = mutableListOf<Double>()
        val plusDMList = mutableListOf<Double>()
        val minusDMList = mutableListOf<Double>()
        
        // Calculate TR, +DM, -DM
        for (i in 1 until candles.size) {
            val current = candles[i]
            val previous = candles[i - 1]
            
            val tr = max(
                current.high - current.low,
                max(
                    abs(current.high - previous.close),
                    abs(current.low - previous.close)
                )
            )
            trList.add(tr)
            
            val highDiff = current.high - previous.high
            val lowDiff = previous.low - current.low
            
            val plusDM = if (highDiff > lowDiff && highDiff > 0) highDiff else 0.0
            val minusDM = if (lowDiff > highDiff && lowDiff > 0) lowDiff else 0.0
            
            plusDMList.add(plusDM)
            minusDMList.add(minusDM)
        }
        
        if (trList.size < period) return 0.0
        
        // Smooth TR, +DM, -DM
        var atr = trList.take(period).average()
        var smoothPlusDM = plusDMList.take(period).average()
        var smoothMinusDM = minusDMList.take(period).average()
        
        for (i in period until trList.size) {
            atr = (atr * (period - 1) + trList[i]) / period
            smoothPlusDM = (smoothPlusDM * (period - 1) + plusDMList[i]) / period
            smoothMinusDM = (smoothMinusDM * (period - 1) + minusDMList[i]) / period
        }
        
        if (atr == 0.0) return 0.0
        
        val plusDI = (smoothPlusDM / atr) * 100
        val minusDI = (smoothMinusDM / atr) * 100
        
        val diDiff = abs(plusDI - minusDI)
        val diSum = plusDI + minusDI
        
        return if (diSum > 0) (diDiff / diSum) * 100 else 0.0
    }
    
    // Calculate EMA slope (to detect flat EMA)
    fun calculateEMASlope(ema: List<Double>): Double {
        if (ema.size < 2) return 0.0
        val recent = ema.takeLast(5)
        if (recent.size < 2) return 0.0
        
        // Simple linear regression slope
        val n = recent.size
        val x = (0 until n).map { it.toDouble() }
        val y = recent
        
        val xMean = x.average()
        val yMean = y.average()
        
        var numerator = 0.0
        var denominator = 0.0
        
        for (i in 0 until n) {
            numerator += (x[i] - xMean) * (y[i] - yMean)
            denominator += (x[i] - xMean) * (x[i] - xMean)
        }
        
        return if (denominator != 0.0) numerator / denominator else 0.0
    }
    
    // Calculate all indicators at once
    fun calculateIndicators(candles: List<Candle>): TechnicalIndicators? {
        if (candles.size < 30) return null // Need enough data
        
        val closePrices = candles.map { it.close }
        
        val ema9 = calculateEMA(closePrices, 9)
        val ema20 = calculateEMA(closePrices, 20)
        val rsi = calculateRSI(closePrices, 14)
        val adx = calculateADX(candles, 14)
        
        // Calculate EMA slope for last 20 candles
        val recentEMA9 = closePrices.takeLast(20).mapIndexed { it, price ->
            calculateEMA(closePrices.take(closePrices.size - 20 + it + 1), 9)
        }
        val emaSlope = calculateEMASlope(recentEMA9)
        
        return TechnicalIndicators(
            ema9 = ema9,
            ema20 = ema20,
            rsi = rsi,
            adx = adx,
            emaSlope = emaSlope
        )
    }
}