package com.giri.trader.application

import com.giri.trader.config.DcaProperties
import com.giri.trader.domain.DcaStrategy
import com.giri.trader.domain.Money
import com.giri.trader.domain.OrderHistory
import com.giri.trader.infrastructure.persistence.DcaConfigRepository
import com.giri.trader.infrastructure.persistence.OrderHistoryRepository
import com.giri.trader.infrastructure.toss.TossApiClient
import com.giri.trader.infrastructure.toss.dto.response.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class DcaTradingService(
    private val tossApiClient: TossApiClient,
    private val orderHistoryRepository: OrderHistoryRepository,
    private val dcaConfigRepository: DcaConfigRepository,
    private val mailService: MailService,
    private val dcaProperties: DcaProperties
) {
    private val log = LoggerFactory.getLogger(DcaTradingService::class.java)
    private val dcaStrategy = DcaStrategy()

    fun executeDcaOrder() {
        val dryRun = dcaProperties.dryRun
        val emailRecipient = dcaProperties.notificationEmail
        log.info("Starting Multi-Stock DCA Order Execution Flow (Dry-Run: {})", dryRun)
        
        val executedHistories = mutableListOf<OrderHistory>()

        try {
            // 1. 토스증권에서 현재 투자/보유 중인 주식 목록 조회
            val holdingsResponse = tossApiClient.getHoldings()
            val holdingStocks = holdingsResponse.result.items

            if (holdingStocks.isEmpty()) {
                log.info("No holding stocks found in Toss account. Skipping DCA flow.")
                return
            }

            log.info("Found {} stocks in Toss portfolio.", holdingStocks.size)

            // 2. DB에서 보유 종목들의 dca_config 설정 로드
            val symbols = holdingStocks.map { it.symbol }
            val dbConfigs = dcaConfigRepository.findAllById(symbols).associateBy { it.symbol }

            // 3. 각 종목별로 루프를 돌며 동적 가중치 분할 매수 실행
            for (stock in holdingStocks) {
                val ticker = stock.symbol
                try {
                    // 3-0. DB 설정 매칭 확인
                    val config = dbConfigs[ticker]
                    if (config == null) {
                        log.info("[{}] No DCA configuration found in DB. Skipping auto-trade for this stock.", ticker)
                        continue
                    }

                    log.info("Processing DCA for Ticker: {}", ticker)
                    val baseAmount = Money(config.baseAmount)
                    val maxDailyBudget = Money(config.maxDailyBudget)

                    log.info("[{}] RDBMS Config resolved: BaseAmount={}, MaxDailyBudget={}", ticker, baseAmount, maxDailyBudget)

                    // 3-1. 해당 종목 시세 조회
                    val priceResponse = tossApiClient.getRealtimePrice(ticker)
                    val currentPrice = Money(priceResponse.currentPrice)
                    val previousClosePrice = Money(priceResponse.previousClosePrice)

                    // 3-2. 하락률 비례 매수액 계산
                    var orderAmount = dcaStrategy.calculateOrderAmount(baseAmount, currentPrice, previousClosePrice)
                    log.info("[{}] Calculated dynamic order amount: {} (Base: {})", ticker, orderAmount, baseAmount)

                    // 3-3. 안전장치 (종목별 최대 예매 한도 캡핑)
                    if (orderAmount.isGreaterThan(maxDailyBudget)) {
                        log.warn("[{}] Calculated amount {} exceeds max budget {}. Capping to limit.", ticker, orderAmount, maxDailyBudget)
                        orderAmount = maxDailyBudget
                    }

                    if (orderAmount.amount.compareTo(BigDecimal.ZERO) <= 0) {
                        log.info("[{}] Order amount is zero or negative. Skipping.", ticker)
                        continue
                    }

                    // 3-4. DB에 PENDING 기록
                    val history = orderHistoryRepository.save(
                        OrderHistory(
                            ticker = ticker,
                            orderAmount = orderAmount.amount,
                            currentPrice = currentPrice.amount,
                            previousClosePrice = previousClosePrice.amount,
                            status = "PENDING"
                        )
                    )

                    // 3-5. 주문 전송 (dry-run 여부에 따른 조건부 분기) 및 DB 상태 업데이트
                    try {
                        if (dryRun) {
                            val mockOrderId = "DRY-RUN-${java.util.UUID.randomUUID()}"
                            history.orderId = mockOrderId
                            history.status = "DRY_RUN"
                            val savedHistory = orderHistoryRepository.save(history)
                            executedHistories.add(savedHistory)
                            log.info("[DRY-RUN] [{}] Order simulated successfully. Virtual OrderID: {}", ticker, mockOrderId)
                        } else {
                            val orderResponse = tossApiClient.placeOrder(ticker, orderAmount.amount)
                            history.orderId = orderResponse.result.orderId
                            history.status = "SUCCESS"
                            val savedHistory = orderHistoryRepository.save(history)
                            executedHistories.add(savedHistory)
                            log.info("[{}] DCA Order placed successfully. OrderID: {}", ticker, orderResponse.result.orderId)
                        }
                    } catch (e: Exception) {
                        history.status = "FAIL"
                        val savedHistory = orderHistoryRepository.save(history)
                        executedHistories.add(savedHistory)
                        log.error("[{}] Failed to place order", ticker, e)
                    }

                } catch (e: Exception) {
                    log.error("Error occurred while processing ticker: {}", ticker, e)
                }
            }

            // 4. 스케줄 수행 후 이메일 일일 리포트 발송
            if (executedHistories.isNotEmpty() && emailRecipient.isNotBlank()) {
                mailService.sendDailyReport(emailRecipient, executedHistories)
            }

        } catch (e: Exception) {
            log.error("Failed to execute holdings-based DCA trading flow", e)
            if (emailRecipient.isNotBlank()) {
                mailService.sendErrorReport(emailRecipient, e.stackTraceToString())
            }
            throw e
        }
    }
}
