package com.giri.trader.interfaces.scheduler

import com.giri.trader.application.MarketDataCollectorService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class MarketDataCollectorScheduler(
    private val collectorService: MarketDataCollectorService
) {
    private val log = LoggerFactory.getLogger(MarketDataCollectorScheduler::class.java)

    @Scheduled(cron = "\${trader.dca.collector-cron}")
    fun triggerPriceCollection() {
        log.info("MarketDataCollectorScheduler triggered.")
        try {
            collectorService.collectMarketPrices()
        } catch (e: Exception) {
            log.error("Execution failed during scheduled market price collection: {}", e.message, e)
        }
    }
}
