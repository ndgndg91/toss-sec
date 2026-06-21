package com.giri.trader.application

import com.giri.trader.config.DcaProperties
import com.giri.trader.domain.DcaConfig
import com.giri.trader.domain.OrderHistory
import com.giri.trader.infrastructure.persistence.DcaConfigRepository
import com.giri.trader.infrastructure.persistence.OrderHistoryRepository
import com.giri.trader.infrastructure.toss.*
import com.giri.trader.infrastructure.toss.dto.response.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.Mockito.times
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal

@ExtendWith(MockitoExtension::class)
class DcaTradingServiceTest {

    @Mock
    private lateinit var tossApiClient: TossApiClient

    @Mock
    private lateinit var orderHistoryRepository: OrderHistoryRepository

    @Mock
    private lateinit var dcaConfigRepository: DcaConfigRepository

    @Mock
    private lateinit var mailService: MailService

    private lateinit var dcaTradingService: DcaTradingService

    @BeforeEach
    fun setUp() {
        val properties = DcaProperties(
            defaultBaseAmount = BigDecimal("10000.00"),
            defaultMaxDailyBudget = BigDecimal("15000.00"),
            cron = "0 0 0 * * TUE-SAT",
            collectorCron = "0 */10 22-23,0-4 * * MON-FRI",
            dryRun = false,
            notificationEmail = "giri@example.com"
        )

        dcaTradingService = DcaTradingService(
            tossApiClient = tossApiClient,
            orderHistoryRepository = orderHistoryRepository,
            dcaConfigRepository = dcaConfigRepository,
            mailService = mailService,
            dcaProperties = properties
        )
    }

    private fun <T> anyNonNull(default: T): T {
        any<T>()
        return default
    }

    @Test
    @DisplayName("일반적인 하락 시나리오에서 가중치가 반영된 주문을 전송하고 DB에 성공 이력을 남겨야 함")
    fun should_place_weighted_order_when_price_drops() {
        // given
        val mockHoldings = TossHoldingsResponse(
            result = TossHoldingsResult(
                items = listOf(
                    TossHoldingStock("AAPL", BigDecimal.TEN, BigDecimal("100.00"))
                )
            )
        )
        val mockConfigs = listOf(
            DcaConfig("AAPL", BigDecimal("10000.00"), BigDecimal("15000.00"))
        )
        val mockPriceResponse = TossPriceResponse(
            ticker = "AAPL",
            currentPrice = BigDecimal("96.00"), // -4% 하락
            previousClosePrice = BigDecimal("100.00")
        )
        val mockOrderResponse = TossOrderResponse(
            result = TossOrderResult(
                orderId = "ORD-001",
                clientOrderId = null
            )
        )

        val defaultHistory = OrderHistory(
            ticker = "AAPL",
            orderAmount = BigDecimal.ZERO,
            currentPrice = BigDecimal.ZERO,
            previousClosePrice = BigDecimal.ZERO,
            status = "PENDING"
        )

        `when`(tossApiClient.getHoldings()).thenReturn(mockHoldings)
        `when`(dcaConfigRepository.findAllById(listOf("AAPL"))).thenReturn(mockConfigs)
        `when`(tossApiClient.getRealtimePrice("AAPL")).thenReturn(mockPriceResponse)
        `when`(tossApiClient.placeOrder("AAPL", BigDecimal("15000.00"))).thenReturn(mockOrderResponse)
        `when`(orderHistoryRepository.save(anyNonNull(defaultHistory))).thenAnswer { it.arguments[0] as OrderHistory }

        // when
        dcaTradingService.executeDcaOrder()

        // then
        verify(tossApiClient).getHoldings()
        verify(dcaConfigRepository).findAllById(listOf("AAPL"))
        verify(tossApiClient).getRealtimePrice("AAPL")
        verify(tossApiClient).placeOrder("AAPL", BigDecimal("15000.00"))
        verify(orderHistoryRepository, times(2)).save(anyNonNull(defaultHistory)) // PENDING 및 SUCCESS 저장 검증
    }

    @Test
    @DisplayName("계산된 주문 금액이 최대 예산 한도를 초과하면 최대 예산까지만 주문을 넣어야 함")
    fun should_cap_order_amount_to_max_budget_when_exceeding() {
        // given
        val mockHoldings = TossHoldingsResponse(
            result = TossHoldingsResult(
                items = listOf(
                    TossHoldingStock("AAPL", BigDecimal.TEN, BigDecimal("100.00"))
                )
            )
        )
        val mockConfigs = listOf(
            DcaConfig("AAPL", BigDecimal("10000.00"), BigDecimal("15000.00"))
        )
        val mockPriceResponse = TossPriceResponse(
            ticker = "AAPL",
            currentPrice = BigDecimal("94.00"), // -6% 하락
            previousClosePrice = BigDecimal("100.00")
        )
        val mockOrderResponse = TossOrderResponse(
            result = TossOrderResult(
                orderId = "ORD-002",
                clientOrderId = null
            )
        )

        val defaultHistory = OrderHistory(
            ticker = "AAPL",
            orderAmount = BigDecimal.ZERO,
            currentPrice = BigDecimal.ZERO,
            previousClosePrice = BigDecimal.ZERO,
            status = "PENDING"
        )

        `when`(tossApiClient.getHoldings()).thenReturn(mockHoldings)
        `when`(dcaConfigRepository.findAllById(listOf("AAPL"))).thenReturn(mockConfigs)
        `when`(tossApiClient.getRealtimePrice("AAPL")).thenReturn(mockPriceResponse)
        `when`(tossApiClient.placeOrder("AAPL", BigDecimal("15000.00"))).thenReturn(mockOrderResponse)
        `when`(orderHistoryRepository.save(anyNonNull(defaultHistory))).thenAnswer { it.arguments[0] as OrderHistory }

        // when
        dcaTradingService.executeDcaOrder()

        // then
        verify(tossApiClient).getHoldings()
        verify(dcaConfigRepository).findAllById(listOf("AAPL"))
        verify(tossApiClient).getRealtimePrice("AAPL")
        verify(tossApiClient).placeOrder("AAPL", BigDecimal("15000.00")) // 20000원이 아닌 15000원으로 매수 제한됨 검증
        verify(orderHistoryRepository, times(2)).save(anyNonNull(defaultHistory))
    }
}
