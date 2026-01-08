package com.codetech.binaryoptionstradingsignalapp.di

import com.codetech.binaryoptionstradingsignalapp.abstraction.domain.IndicatorsCalculator
import com.codetech.binaryoptionstradingsignalapp.abstraction.domain.SignalGenerator
import com.codetech.binaryoptionstradingsignalapp.abstraction.service.AlphaVantageService
import com.codetech.binaryoptionstradingsignalapp.abstraction.data.repo.TradingRepository
import com.codetech.binaryoptionstradingsignalapp.abstraction.simulator.MarketDataSimulator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideIndicatorsCalculator(): IndicatorsCalculator {
        return IndicatorsCalculator()
    }

    @Provides
    @Singleton
    fun provideSignalGenerator(
        calculator: IndicatorsCalculator
    ): SignalGenerator {
        return SignalGenerator(calculator)
    }

    @Provides
    @Singleton
    fun provideTwelveDataService(): AlphaVantageService {
        return AlphaVantageService()
    }

    @Provides
    @Singleton
    fun provideTradingRepository(
        apiService: AlphaVantageService,
        simulator: MarketDataSimulator,
        signalGenerator: SignalGenerator
    ): TradingRepository {
        return TradingRepository(apiService, simulator,signalGenerator)
    }

    @Provides
    @Singleton
    fun provideMarketDataSimulator(): MarketDataSimulator {
        return MarketDataSimulator()
    }
}