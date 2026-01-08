package com.codetech.binaryoptionstradingsignalapp.abstraction.data.model

import kotlin.math.abs

// Candle data for M1 timeframe
data class Candle(
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val timestamp: Long,
    val volume: Double = 0.0
) {
    val body: Double
        get() = abs(close - open)
    
    val upperWick: Double
        get() = high - maxOf(open, close)
    
    val lowerWick: Double
        get() = minOf(open, close) - low
    
    val totalRange: Double
        get() = high - low
    
    val bodyPercentage: Double
        get() = if (totalRange > 0) (body / totalRange) * 100 else 0.0
    
    val isBullish: Boolean
        get() = close > open
    
    fun hasGoodQuality(): Boolean {
        if (totalRange == 0.0) return false
        val upperWickPct = (upperWick / totalRange) * 100
        val lowerWickPct = (lowerWick / totalRange) * 100
        return bodyPercentage in 25.0..85.0 && upperWickPct < 40.0 && lowerWickPct < 40.0
    }
}