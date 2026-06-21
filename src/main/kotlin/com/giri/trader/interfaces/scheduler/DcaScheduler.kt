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

    // application.yml 에 설정된 cron 주기에 따라 자동 실행
    @Scheduled(cron = "\${trader.dca.cron}")
    fun triggerDcaTrading() {
        log.info("DcaScheduler triggered. Running automated trade...")
        try {
            dcaTradingService.executeDcaOrder()
        } catch (e: Exception) {
            log.error("Execution failed during scheduled DCA trading: {}", e.message, e)
        }
    }
}
