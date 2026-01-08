package com.codetech.binaryoptionstradingsignalapp.abstraction.data.model

import com.codetech.binaryoptionstradingsignalapp.abstraction.enums.CurrencyPair

// Pair monitoring status
data class PairStatus(
    val pair: CurrencyPair,
    val isMonitoring: Boolean = false,
    val lastCandle: Candle? = null,
    val lastIndicators: TechnicalIndicators? = null,
    val lastSignal: TradingSignal? = null
)