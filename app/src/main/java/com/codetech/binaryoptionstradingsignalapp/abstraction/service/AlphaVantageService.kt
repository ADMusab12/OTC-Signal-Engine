package com.codetech.binaryoptionstradingsignalapp.abstraction.service

import android.os.Build
import androidx.annotation.RequiresApi
import com.codetech.binaryoptionstradingsignalapp.abstraction.data.model.Candle
import com.codetech.binaryoptionstradingsignalapp.abstraction.enums.CurrencyPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AlphaVantageService @Inject constructor() {
    private val apiKey = "P7Q8RQN3EGFSA2EN"
    private val baseUrl = "https://www.alphavantage.co/query"
    
    private val pairMapping = mapOf(
        CurrencyPair.EURUSD_OTC to ("EUR" to "USD"),
        CurrencyPair.GBPUSD_OTC to ("GBP" to "USD"),
        CurrencyPair.USDJPY_OTC to ("USD" to "JPY"),
        CurrencyPair.AUDUSD_OTC to ("AUD" to "USD"),
        CurrencyPair.USDCAD_OTC to ("USD" to "CAD"),
        CurrencyPair.USDCHF_OTC to ("USD" to "CHF"),
        CurrencyPair.NZDUSD_OTC to ("NZD" to "USD"),
        CurrencyPair.EURJPY_OTC to ("EUR" to "JPY")
    )

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun fetchTimeSeries(
        pair: CurrencyPair,
        interval: String
    ): List<Candle> = fetchIntraday(pair, interval)

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun fetchIntraday(pair: CurrencyPair, interval: String = "1min"): List<Candle> {
        return withContext(Dispatchers.IO) {
            try {
                val (fromCurrency, toCurrency) = pairMapping[pair] 
                    ?: return@withContext emptyList()
                
                val url = "$baseUrl?" +
                        "function=FX_INTRADAY" +
                        "&from_symbol=$fromCurrency" +
                        "&to_symbol=$toCurrency" +
                        "&interval=$interval" +
                        "&apikey=$apiKey" +
                        "&outputsize=compact"
                
                val response = URL(url).readText()
                parseAlphaVantageResponse(response)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    private fun parseAlphaVantageResponse(response: String): List<Candle> {
        val candles = mutableListOf<Candle>()
        
        try {
            val json = JSONObject(response)
            val timeSeries = json.optJSONObject("Time Series FX (1min)") ?: return emptyList()
            
            timeSeries.keys().forEach { timestamp ->
                val data = timeSeries.getJSONObject(timestamp)
                
                val candle = Candle(
                    open = data.getString("1. open").toDouble(),
                    high = data.getString("2. high").toDouble(),
                    low = data.getString("3. low").toDouble(),
                    close = data.getString("4. close").toDouble(),
                    timestamp = parseTimestamp(timestamp),
                    volume = 0.0 // FX doesn't have volume
                )
                
                candles.add(candle)
            }
            
            // Sort by timestamp ascending
            candles.sortBy { it.timestamp }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return candles
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    private fun parseTimestamp(timestamp: String): Long {
        return try {
            java.time.LocalDateTime.parse(
                timestamp,
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            ).atZone(java.time.ZoneId.of("America/New_York"))
                .toInstant()
                .toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}