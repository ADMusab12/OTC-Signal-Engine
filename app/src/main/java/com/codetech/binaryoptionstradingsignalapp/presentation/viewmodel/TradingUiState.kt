package com.codetech.binaryoptionstradingsignalapp.presentation.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codetech.binaryoptionstradingsignalapp.abstraction.enums.CurrencyPair
import com.codetech.binaryoptionstradingsignalapp.abstraction.data.model.PairStatus
import com.codetech.binaryoptionstradingsignalapp.abstraction.data.model.TradingSignal
import com.codetech.binaryoptionstradingsignalapp.abstraction.data.repo.TradingRepository
import com.codetech.binaryoptionstradingsignalapp.abstraction.enums.SignalType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TradingUiState(
    val selectedPairs: Set<CurrencyPair> = emptySet(),
    val pairStatuses: Map<CurrencyPair, PairStatus> = emptyMap(),
    val currentSignal: TradingSignal? = null,
    val signalHistory: List<TradingSignal> = emptyList(),
    val isMonitoring: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TradingViewModel @Inject constructor(
    private val repository: TradingRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TradingUiState())
    val uiState: StateFlow<TradingUiState> = _uiState.asStateFlow()
    
    private val monitoringJobs = mutableMapOf<CurrencyPair, Job>()
    private val maxSignalHistory = 20
    
    // Toggle pair selection
    fun togglePairSelection(pair: CurrencyPair) {
        val currentPairs = _uiState.value.selectedPairs.toMutableSet()
        
        if (currentPairs.contains(pair)) {
            currentPairs.remove(pair)
            stopMonitoringPair(pair)
        } else {
            currentPairs.add(pair)
        }
        
        _uiState.update { it.copy(selectedPairs = currentPairs) }
    }
    
    // Start monitoring all selected pairs
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun startMonitoring() {
        val pairs = _uiState.value.selectedPairs
        
        if (pairs.isEmpty()) {
            _uiState.update { it.copy(error = "Please select at least one currency pair") }
            return
        }
        
        _uiState.update { it.copy(isMonitoring = true, error = null) }
        
        pairs.forEach { pair ->
            startMonitoringPair(pair)
        }
    }
    
    // Stop monitoring all pairs
    fun stopMonitoring() {
        monitoringJobs.values.forEach { it.cancel() }
        monitoringJobs.clear()
        _uiState.update { it.copy(isMonitoring = false) }
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun startMonitoringPair(pair: CurrencyPair) {
        if (monitoringJobs.containsKey(pair)) return
        
        repository.initializePair(pair)
        
        val job = viewModelScope.launch {
            repository.monitorPair(pair)
                .catch { e ->
                    _uiState.update { it.copy(error = "Error monitoring $pair: ${e.message}") }
                }
                .collect { signal ->
                    handleNewSignal(signal)
                }
        }
        
        monitoringJobs[pair] = job
        updatePairStatus(pair, true)
    }
    
    private fun stopMonitoringPair(pair: CurrencyPair) {
        monitoringJobs[pair]?.cancel()
        monitoringJobs.remove(pair)
        updatePairStatus(pair, false)
    }
    
    private fun updatePairStatus(pair: CurrencyPair, isMonitoring: Boolean) {
        val candles = repository.getCandles(pair)
        val indicators = repository.getLatestIndicators(pair)
        
        val status = PairStatus(
            pair = pair,
            isMonitoring = isMonitoring,
            lastCandle = candles.lastOrNull(),
            lastIndicators = indicators
        )
        
        _uiState.update {
            it.copy(
                pairStatuses = it.pairStatuses + (pair to status)
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleNewSignal(signal: TradingSignal) {
        _uiState.update {
            val newStatuses = it.pairStatuses.toMutableMap()
            val old = newStatuses[signal.pair] ?: PairStatus(signal.pair)
            newStatuses[signal.pair] = old.copy(
                lastCandle = repository.getCandles(signal.pair).lastOrNull(),
                lastIndicators = repository.getLatestIndicators(signal.pair),
                lastSignal = signal
            )
            it.copy(
                pairStatuses = newStatuses,
                currentSignal = signal.takeIf { it.type != SignalType.NONE && it.isValid() }
            )
        }
        addToHistory(signal)
    }
    
    private fun addToHistory(signal: TradingSignal) {
        _uiState.update {
            val history = (listOf(signal) + it.signalHistory).take(maxSignalHistory)
            it.copy(signalHistory = history)
        }
    }
    
    // Dismiss current signal
    fun dismissSignal() {
        _uiState.update { it.copy(currentSignal = null) }
    }
    
    // Clear error
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    // Clear history
    fun clearHistory() {
        _uiState.update { it.copy(signalHistory = emptyList()) }
        repository.clearHistory()
    }
    
    override fun onCleared() {
        super.onCleared()
        stopMonitoring()
    }
}