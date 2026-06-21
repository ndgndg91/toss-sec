package com.giri.trader.application

import com.giri.trader.domain.DcaStrategy
import com.giri.trader.domain.Money
import com.giri.trader.domain.OrderHistory
import com.giri.trader.infrastructure.persistence.OrderHistoryRepository
import com.giri.trader.infrastructure.toss.TossApiClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class DcaTradingService(
    private val tossApiClient: TossApiClient,
    private val orderHistoryRepository: OrderHistoryRepository,
    @Value("\${trader.dca.base-amount}") private val baseAmountRaw: Double,
    @Value("\${trader.dca.max-daily-budget}") private val maxDailyBudgetRaw: Double,
    @Value("\${trader.dca.dry-run}") private val dryRun: Boolean
) {
    private val log = LoggerFactory.getLogger(DcaTradingService::class.java)
    private val dcaStrategy = DcaStrategy()

    fun executeDcaOrder() {
        log.info("Starting Multi-Stock DCA Order Execution Flow (Dry-Run: {})", dryRun)
        
        val baseAmount = Money.of(baseAmountRaw)
        val maxDailyBudget = Money.of(maxDailyBudgetRaw)

        try {
            // 1. 토스증권에서 현재 투자/보유 중인 주식 목록 조회
            val holdingsResponse = tossApiClient.getHoldings()
            val holdingStocks = holdingsResponse.result.items

            if (holdingStocks.isEmpty()) {
                log.info("No holding stocks found in Toss account. Skipping DCA flow.")
                return
            }

            log.info("Found {} stocks to execute DCA.", holdingStocks.size)

            // 2. 각 종목별로 루프를 돌며 동적 가중치 분할 매수 실행
            for (stock in holdingStocks) {
                val ticker = stock.symbol
                try {
                    log.info("Processing DCA for Ticker: {}", ticker)

                    // 2-1. 해당 종목 시세 조회
                    val priceResponse = tossApiClient.getRealtimePrice(ticker)
                    val currentPrice = Money(priceResponse.currentPrice)
                    val previousClosePrice = Money(priceResponse.previousClosePrice)

                    // 2-2. 하락률 비례 매수액 계산
                    var orderAmount = dcaStrategy.calculateOrderAmount(baseAmount, currentPrice, previousClosePrice)
                    log.info("[{}] Calculated dynamic order amount: {} (Base: {})", ticker, orderAmount, baseAmount)

                    // 2-3. 안전장치 (종목별 최대 예매 한도 캡핑)
                    if (orderAmount.isGreaterThan(maxDailyBudget)) {
                        log.warn("[{}] Calculated amount {} exceeds max budget {}. Capping to limit.", ticker, orderAmount, maxDailyBudget)
                        orderAmount = maxDailyBudget
                    }

                    if (orderAmount.amount.compareTo(BigDecimal.ZERO) <= 0) {
                        log.info("[{}] Order amount is zero or negative. Skipping.", ticker)
                        continue
                    }

                    // 2-4. DB에 PENDING 기록
                    val history = orderHistoryRepository.save(
                        OrderHistory(
                            ticker = ticker,
                            orderAmount = orderAmount.amount,
                            currentPrice = currentPrice.amount,
                            previousClosePrice = previousClosePrice.amount,
                            status = "PENDING"
                        )
                    )

                    // 2-5. 주문 전송 (dry-run 여부에 따른 조건부 분기) 및 DB 상태 업데이트
                    try {
                        if (dryRun) {
                            val mockOrderId = "DRY-RUN-${java.util.UUID.randomUUID()}"
                            history.orderId = mockOrderId
                            history.status = "DRY_RUN"
                            orderHistoryRepository.save(history)
                            log.info("[DRY-RUN] [{}] Order simulated successfully. Virtual OrderID: {}", ticker, mockOrderId)
                        } else {
                            val orderResponse = tossApiClient.placeOrder(ticker, orderAmount.amount)
                            history.orderId = orderResponse.result.orderId
                            history.status = "SUCCESS"
                            orderHistoryRepository.save(history)
                            log.info("[{}] DCA Order placed successfully. OrderID: {}", ticker, orderResponse.result.orderId)
                        }
                    } catch (e: Exception) {
                        history.status = "FAIL"
                        orderHistoryRepository.save(history)
                        log.error("[{}] Failed to place order", ticker, e)
                    }

                } catch (e: Exception) {
                    log.error("Error occurred while processing ticker: {}", ticker, e)
                }
            }

        } catch (e: Exception) {
            log.error("Failed to execute holdings-based DCA trading flow", e)
            throw e
        }
    }
}
