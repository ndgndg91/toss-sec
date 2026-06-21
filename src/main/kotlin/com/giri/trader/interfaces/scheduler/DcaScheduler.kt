package com.giri.trader.interfaces.scheduler

import com.giri.trader.application.DcaTradingService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class DcaScheduler(
    private val dcaTradingService: DcaTradingService
) {
    private val log = LoggerFactory.getLogger(DcaScheduler::class.java)

    // 매주 월~금 15:00:00 (KST 기준 국내 주식 장마감 전 또는 해외주식 프리마켓 전 등) 실행
    @Scheduled(cron = "0 0 15 * * MON-FRI")
    fun triggerDcaTrading() {
        log.info("DcaScheduler triggered. Running automated trade...")
        try {
            dcaTradingService.executeDcaOrder()
        } catch (e: Exception) {
            log.error("Execution failed during scheduled DCA trading: {}", e.message, e)
        }
    }
}
