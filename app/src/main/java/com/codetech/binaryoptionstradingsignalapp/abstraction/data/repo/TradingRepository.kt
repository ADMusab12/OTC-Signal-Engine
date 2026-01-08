package com.codetech.binaryoptionstradingsignalapp.abstraction.data.repo

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.codetech.binaryoptionstradingsignalapp.abstraction.data.model.Candle
import com.codetech.binaryoptionstradingsignalapp.abstraction.data.model.TechnicalIndicators
import com.codetech.binaryoptionstradingsignalapp.abstraction.data.model.TradingSignal
import com.codetech.binaryoptionstradingsignalapp.abstraction.domain.SignalGenerator
import com.codetech.binaryoptionstradingsignalapp.abstraction.enums.CurrencyPair
import com.codetech.binaryoptionstradingsignalapp.abstraction.enums.SignalType
import com.codetech.binaryoptionstradingsignalapp.abstraction.service.AlphaVantageService
import com.codetech.binaryoptionstradingsignalapp.abstraction.simulator.MarketDataSimulator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class TradingRepository @Inject constructor(
    private val apiService: AlphaVantageService,
    private val simulator: MarketDataSimulator,
    private val signalGenerator: SignalGenerator
) {

    private val candleHistory = mutableMapOf<CurrencyPair, MutableList<Candle>>()
    private val maxHistorySize = 150
    private val minCandlesForSignal = 30
    private val updateIntervalMs = 60000L
    private var useSimulator: Boolean = true
    private val TAG = "TradingRepo"

    /**
     * Initialize candle history
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun initializePair(pair: CurrencyPair) {
        if (candleHistory.containsKey(pair)) return

        if (useSimulator) {
            val initialCandles = simulator.generateHistoricalCandles(pair, 100)
            candleHistory[pair] = initialCandles.toMutableList()
            Log.i(TAG, "Simulator → Initialized ${pair.name} with ${initialCandles.size} candles")
        } else {
            try {
                val candles = apiService.fetchTimeSeries(
                    pair = pair,
                    interval = "1min"
                )
                if (candles.isNotEmpty()) {
                    val sorted = candles.sortedBy { it.timestamp }.toMutableList()
                    candleHistory[pair] = sorted
                    Log.i(TAG, "API → Initialized ${pair.name} with ${sorted.size} candles")
                } else {
                    candleHistory[pair] = mutableListOf()
                    Log.w(TAG, "No candles from API for ${pair.name}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "API init failed for ${pair.name}", e)
                candleHistory[pair] = mutableListOf()
            }
        }
    }

    fun getCandles(pair: CurrencyPair): List<Candle> =
        candleHistory[pair]?.toList() ?: emptyList()

    /**
     * Main monitoring flow — emits real signals when conditions met
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun monitorPair(pair: CurrencyPair): Flow<TradingSignal> = flow {
        initializePair(pair)

        while (true) {
            try {
                val newCandles: List<Candle> = if (useSimulator) {
                    val history = candleHistory[pair] ?: emptyList()
                    if (history.isEmpty()) {
                        // safety fallback (should not happen after initialize)
                        simulator.generateHistoricalCandles(pair, 100).also {
                            candleHistory[pair] = it.toMutableList()
                        }
                        emptyList()
                    } else {
                        listOf(simulator.generateNextCandle(pair, history.last()))
                    }
                } else {
                    apiService.fetchTimeSeries(pair, "1min")
                }

                if (newCandles.isNotEmpty()) {
                    val history = candleHistory[pair] ?: mutableListOf()
                    val lastTs = history.lastOrNull()?.timestamp ?: 0L

                    var added = 0
                    newCandles.forEach { nc ->
                        if (nc.timestamp > lastTs && history.none { it.timestamp == nc.timestamp }) {
                            history.add(nc)
                            added++
                        }
                    }

                    // Trim old candles
                    while (history.size > maxHistorySize) {
                        history.removeAt(0)
                    }

                    if (added > 0) {
                        candleHistory[pair] = history
                        Log.d(TAG, "${pair.name} → +$added candles (total now ${history.size})")

                        if (history.size >= minCandlesForSignal) {
                            val signal = signalGenerator.generateSignal(pair, history)
                            if (signal != null && signal.type != SignalType.NONE) {
                                Log.i(TAG, "SIGNAL DETECTED → ${pair.name} ${signal.type} (${signal.confidence}%)")
                                emit(signal)
                            } else {
                                Log.v(TAG, "${pair.name} - signal was NONE")
                            }
                        } else {
                            Log.d(TAG, "${pair.name} waiting: ${history.size}/$minCandlesForSignal")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Monitoring error for ${pair.name}", e)
            }

            val jitter = Random.nextLong(-10000L, 15000L)
            delay(updateIntervalMs + jitter.coerceAtLeast(0L))
        }
    }

    fun getLatestIndicators(pair: CurrencyPair): TechnicalIndicators? {
        val candles = getCandles(pair)
        return if (candles.size >= minCandlesForSignal)
            signalGenerator.calculator.calculateIndicators(candles)
        else null
    }

    fun clearHistory() {
        candleHistory.clear()
        Log.i(TAG, "History cleared")
    }
}