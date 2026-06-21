package com.giri.trader.interfaces.scheduler

import com.giri.trader.application.MarketDataCollectorService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class MarketDataCleanupScheduler(
    private val collectorService: MarketDataCollectorService
) {
    private val log = LoggerFactory.getLogger(MarketDataCleanupScheduler::class.java)

    // 매일 오전 8시(KST)에 지난 30일 이전의 시세 데이터를 정리
    @Scheduled(cron = "0 0 8 * * *")
    fun triggerPriceCleanup() {
        log.info("MarketDataCleanupScheduler triggered.")
        try {
            collectorService.cleanOldPrices(30)
        } catch (e: Exception) {
            log.error("Execution failed during scheduled market price cleanup: {}", e.message, e)
        }
    }
}
