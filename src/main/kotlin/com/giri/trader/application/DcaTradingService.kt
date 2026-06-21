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
    @Value("\${trader.dca.ticker}") private val ticker: String,
    @Value("\${trader.dca.base-amount}") private val baseAmountRaw: Double,
    @Value("\${trader.dca.max-daily-budget}") private val maxDailyBudgetRaw: Double
) {
    private val log = LoggerFactory.getLogger(DcaTradingService::class.java)
    private val dcaStrategy = DcaStrategy()

    fun executeDcaOrder() {
        log.info("Starting DCA Order Execution Flow for Ticker: {}", ticker)
        
        val baseAmount = Money.of(baseAmountRaw)
        val maxDailyBudget = Money.of(maxDailyBudgetRaw)

        // 1. 시세 조회
        val priceResponse = tossApiClient.getRealtimePrice(ticker)
        val currentPrice = Money(priceResponse.currentPrice)
        val previousClosePrice = Money(priceResponse.previousClosePrice)

        // 2. 전략적 주문 금액 계산
        var orderAmount = dcaStrategy.calculateOrderAmount(baseAmount, currentPrice, previousClosePrice)
        log.info("Calculated dynamic order amount: {} (Base: {})", orderAmount, baseAmount)

        // 3. 안전장치 (Safety Budget Limit Check)
        if (orderAmount.isGreaterThan(maxDailyBudget)) {
            log.warn("Calculated amount {} exceeds maximum daily budget {}. Capping to limit.", orderAmount, maxDailyBudget)
            orderAmount = maxDailyBudget
        }

        if (orderAmount.amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("Order amount is zero or negative. Skipping order placement.")
            return
        }

        // 4. DB에 PENDING 상태로 최초 기록 (주문 정합성 추적용)
        val history = orderHistoryRepository.save(
            OrderHistory(
                ticker = ticker,
                orderAmount = orderAmount.amount,
                currentPrice = currentPrice.amount,
                previousClosePrice = previousClosePrice.amount,
                status = "PENDING"
            )
        )

        try {
            // 5. 실제 토스증권 API 주문 실행
            val orderResponse = tossApiClient.placeOrder(ticker, orderAmount.amount)
            
            // 6. 주문 성공 시 DB 상태 업데이트
            history.orderId = orderResponse.orderId
            history.status = "SUCCESS"
            orderHistoryRepository.save(history)
            
            log.info("DCA Order process finished successfully. Status: {}, OrderID: {}", 
                orderResponse.status, orderResponse.orderId)

        } catch (e: Exception) {
            // 7. 주문 실패 시 DB 상태 업데이트
            history.status = "FAIL"
            orderHistoryRepository.save(history)
            
            log.error("Failed to execute DCA trading flow: {}", e.message, e)
            throw e
        }
    }
}
