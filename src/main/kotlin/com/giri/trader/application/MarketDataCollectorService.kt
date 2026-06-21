package com.giri.trader.application

import com.giri.trader.domain.MarketPriceHistory
import com.giri.trader.infrastructure.persistence.MarketPriceHistoryRepository
import com.giri.trader.infrastructure.toss.TossApiClient
import com.giri.trader.infrastructure.toss.dto.response.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class MarketDataCollectorService(
    private val tossApiClient: TossApiClient,
    private val marketPriceHistoryRepository: MarketPriceHistoryRepository
) {
    private val log = LoggerFactory.getLogger(MarketDataCollectorService::class.java)

    fun collectMarketPrices() {
        log.info("Starting real-time market price collection...")

        try {
            // 1. 보유 주식 목록 획득
            val holdingsResponse = tossApiClient.getHoldings()
            val items = holdingsResponse.result.items

            if (items.isEmpty()) {
                log.info("No holding stocks to collect prices for.")
                return
            }

            // 2. 종목별 현재가 조회 및 DB 적재
            for (stock in items) {
                val symbol = stock.symbol
                try {
                    val priceResponse = tossApiClient.getRealtimePrice(symbol)
                    
                    marketPriceHistoryRepository.save(
                        MarketPriceHistory(
                            symbol = symbol,
                            price = priceResponse.currentPrice
                        )
                    )
                    
                    log.info("Successfully recorded market price for {}: {}", symbol, priceResponse.currentPrice)
                } catch (e: Exception) {
                    log.error("Failed to collect market price for symbol: {}", symbol, e)
                }
            }

            log.info("Market price collection phase completed.")
        } catch (e: Exception) {
            log.error("Failed to run market price collection flow", e)
        }
    }

    fun cleanOldPrices(retentionDays: Long) {
        log.info("Starting market price history cleanup (retention: {} days)...", retentionDays)
        try {
            val cutoff = java.time.Instant.now().minus(retentionDays, java.time.temporal.ChronoUnit.DAYS)
            val deletedCount = marketPriceHistoryRepository.deleteByCreatedAtBefore(cutoff)
            log.info("Cleanup completed. Deleted {} records older than {}.", deletedCount, cutoff)
        } catch (e: Exception) {
            log.error("Failed to clean up old market prices", e)
        }
    }
}
