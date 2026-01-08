package com.codetech.binaryoptionstradingsignalapp.abstraction.data.model

import android.os.Build
import androidx.annotation.RequiresApi
import com.codetech.binaryoptionstradingsignalapp.abstraction.enums.CurrencyPair
import com.codetech.binaryoptionstradingsignalapp.abstraction.enums.MarketCondition
import com.codetech.binaryoptionstradingsignalapp.abstraction.enums.SignalType
import java.time.LocalDateTime

// Trading Signal
data class TradingSignal(
    val pair: CurrencyPair,
    val type: SignalType,
    val confidence: Int, // 0-100
    val price: Double,
    val timestamp: LocalDateTime,
    val indicators: TechnicalIndicators,
    val marketCondition: MarketCondition,
    val reason: String,
    val expiryMinutes: Int = 1 // For M1 timeframe
) {
    @RequiresApi(Build.VERSION_CODES.O)
    fun isValid(): Boolean {
        val now = LocalDateTime.now()
        return timestamp.plusMinutes(5) > now && type != SignalType.NONE
    }
}