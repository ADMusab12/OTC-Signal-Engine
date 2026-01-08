package com.codetech.binaryoptionstradingsignalapp.abstraction.domain

import android.os.Build
import androidx.annotation.RequiresApi
import com.codetech.binaryoptionstradingsignalapp.abstraction.enums.CurrencyPair
import com.codetech.binaryoptionstradingsignalapp.abstraction.enums.MarketCondition
import com.codetech.binaryoptionstradingsignalapp.abstraction.enums.SignalType
import com.codetech.binaryoptionstradingsignalapp.abstraction.data.model.Candle
import com.codetech.binaryoptionstradingsignalapp.abstraction.data.model.TechnicalIndicators
import com.codetech.binaryoptionstradingsignalapp.abstraction.data.model.TradingSignal
import java.time.LocalDateTime
import kotlin.math.abs

class SignalGenerator(
    val calculator: IndicatorsCalculator = IndicatorsCalculator()
) {
    
    // Generate signal from candles
    @RequiresApi(Build.VERSION_CODES.O)
    fun generateSignal(pair: CurrencyPair, candles: List<Candle>): TradingSignal? {
        if (candles.size < 30) return null
        
        val lastCandle = candles.last()
        val indicators = calculator.calculateIndicators(candles) ?: return null
        
        // Check market condition
        val marketCondition = assessMarketCondition(candles, indicators)
        
        // Filter bad market conditions
        if (shouldFilterSignal(indicators, marketCondition, lastCandle)) {
            return TradingSignal(
                pair = pair,
                type = SignalType.NONE,
                confidence = 0,
                price = lastCandle.close,
                timestamp = LocalDateTime.now(),
                indicators = indicators,
                marketCondition = marketCondition,
                reason = "Market conditions not favorable: ${getFilterReason(indicators, marketCondition, lastCandle)}"
            )
        }
        
        // Generate signal based on indicators
        val signal = analyzeIndicators(indicators, lastCandle)
        
        if (signal == SignalType.NONE) {
            return TradingSignal(
                pair = pair,
                type = SignalType.NONE,
                confidence = 0,
                price = lastCandle.close,
                timestamp = LocalDateTime.now(),
                indicators = indicators,
                marketCondition = marketCondition,
                reason = "No clear signal"
            )
        }
        
        // Calculate confidence
        val confidence = calculateConfidence(indicators, lastCandle, signal)
        
        return TradingSignal(
            pair = pair,
            type = signal,
            confidence = confidence,
            price = lastCandle.close,
            timestamp = LocalDateTime.now(),
            indicators = indicators,
            marketCondition = marketCondition,
            reason = generateReason(signal, indicators, lastCandle),
            expiryMinutes = 1
        )
    }
    
    private fun assessMarketCondition(
        candles: List<Candle>,
        indicators: TechnicalIndicators
    ): MarketCondition {
        // Extreme RSI
        if (indicators.rsi > 80 || indicators.rsi < 20) {
            return MarketCondition.EXTREME
        }
        
        // Flat EMA (slope near zero)
        if (abs(indicators.emaSlope) < 0.0001) {
            return MarketCondition.RANGING
        }
        
        // Choppy candles (many small bodies)
        val recentCandles = candles.takeLast(10)
        val avgBody = recentCandles.map { it.body }.average()
        val avgRange = recentCandles.map { it.totalRange }.average()
        if (avgRange > 0 && (avgBody / avgRange) < 0.3) {
            return MarketCondition.CHOPPY
        }
        
        // Strong trend (high ADX)
        if (indicators.adx > 25) {
            return MarketCondition.TRENDING
        }
        
        return MarketCondition.RANGING
    }
    
    private fun shouldFilterSignal(
        indicators: TechnicalIndicators,
        condition: MarketCondition,
        candle: Candle
    ): Boolean {
        // Filter extreme RSI (>80 or <20)
        if (indicators.rsi > 80 || indicators.rsi < 20) return true

        // Filter flat EMA (very small slope)
        if (abs(indicators.emaSlope) < 0.0001) return true

        // Filter choppy market (low ADX < 15)
        if (indicators.adx < 15) return true

        // Filter poor candle quality
        if (!candle.hasGoodQuality()) return true

        return false
    }
    
    private fun getFilterReason(
        indicators: TechnicalIndicators,
        condition: MarketCondition,
        candle: Candle
    ): String {
        val reasons = mutableListOf<String>()
        
        if (indicators.rsi > 80) reasons.add("RSI overbought (${indicators.rsi.toInt()})")
        if (indicators.rsi < 20) reasons.add("RSI oversold (${indicators.rsi.toInt()})")
        if (abs(indicators.emaSlope) < 0.0001) reasons.add("Flat EMA")
        if (indicators.adx < 15) reasons.add("Low ADX (${indicators.adx.toInt()})")
        if (!candle.hasGoodQuality()) reasons.add("Poor candle quality")
        
        return reasons.joinToString(", ")
    }
    
    private fun analyzeIndicators(
        indicators: TechnicalIndicators,
        candle: Candle
    ): SignalType {
        val price = candle.close
        val ema9 = indicators.ema9
        val ema20 = indicators.ema20
        val rsi = indicators.rsi
        val buyConditions = listOf(
            price > ema9,
            ema9 > ema20,
            rsi in 40.0..70.0,
            candle.hasGoodQuality(),
            candle.isBullish
        )
        val sellConditions = listOf(
            price < ema9,
            ema9 < ema20,
            rsi in 30.0..60.0,
            candle.hasGoodQuality(),
            !candle.isBullish
        )
        
        val buyScore = buyConditions.count { it }
        val sellScore = sellConditions.count { it }
        
        return when {
            buyScore >= 3 -> SignalType.BUY
            sellScore >= 3 -> SignalType.SELL
            else -> SignalType.NONE
        }
    }
    
    private fun calculateConfidence(
        indicators: TechnicalIndicators,
        candle: Candle,
        signal: SignalType
    ): Int {
        var confidence = 50
        
        // ADX strength bonus (max +20)
        confidence += ((indicators.adx / 50.0) * 20).toInt().coerceAtMost(20)
        
        // EMA alignment bonus (max +15)
        val emaAlignment = abs(indicators.ema9 - indicators.ema20)
        confidence += (emaAlignment * 10).toInt().coerceAtMost(15)
        
        // RSI position bonus (max +10)
        val rsiOptimal = when (signal) {
            SignalType.BUY -> indicators.rsi in 50.0..60.0
            SignalType.SELL -> indicators.rsi in 40.0..50.0
            else -> false
        }
        if (rsiOptimal) confidence += 10
        
        // Candle quality bonus (max +5)
        if (candle.bodyPercentage > 50) confidence += 5
        
        return confidence.coerceIn(0, 100)
    }
    
    private fun generateReason(
        signal: SignalType,
        indicators: TechnicalIndicators,
        candle: Candle
    ): String {
        return when (signal) {
            SignalType.BUY -> buildString {
                append("BUY: Price above EMA9(${String.format("%.5f", indicators.ema9)}), ")
                append("EMA9 > EMA20(${String.format("%.5f", indicators.ema20)}), ")
                append("RSI: ${indicators.rsi.toInt()}, ")
                append("ADX: ${indicators.adx.toInt()}, ")
                append("Bullish candle with good quality")
            }
            SignalType.SELL -> buildString {
                append("SELL: Price below EMA9(${String.format("%.5f", indicators.ema9)}), ")
                append("EMA9 < EMA20(${String.format("%.5f", indicators.ema20)}), ")
                append("RSI: ${indicators.rsi.toInt()}, ")
                append("ADX: ${indicators.adx.toInt()}, ")
                append("Bearish candle with good quality")
            }
            SignalType.NONE -> "No clear signal"
        }
    }
}