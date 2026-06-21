package com.giri.trader.application

import com.giri.trader.domain.MarketPriceHistory
import com.giri.trader.infrastructure.persistence.MarketPriceHistoryRepository
import com.giri.trader.infrastructure.toss.TossApiClient
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
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class MarketDataCollectorServiceTest {

    @Mock
    private lateinit var tossApiClient: TossApiClient

    @Mock
    private lateinit var marketPriceHistoryRepository: MarketPriceHistoryRepository

    private lateinit var marketDataCollectorService: MarketDataCollectorService

    @BeforeEach
    fun setUp() {
        marketDataCollectorService = MarketDataCollectorService(
            tossApiClient = tossApiClient,
            marketPriceHistoryRepository = marketPriceHistoryRepository
        )
    }

    private fun <T> anyNonNull(default: T): T {
        any<T>()
        return default
    }

    @Test
    @DisplayName("보유 주식이 없는 경우 시세 조회를 중단하고 리포지토리에 저장하지 않음")
    fun should_not_collect_prices_when_holdings_are_empty() {
        // given
        val mockHoldings = TossHoldingsResponse(
            result = TossHoldingsResult(items = emptyList())
        )
        `when`(tossApiClient.getHoldings()).thenReturn(mockHoldings)

        // when
        marketDataCollectorService.collectMarketPrices()

        // then
        verify(tossApiClient).getHoldings()
        verifyNoMoreInteractions(tossApiClient)
        verifyNoMoreInteractions(marketPriceHistoryRepository)
    }

    @Test
    @DisplayName("보유 주식이 있는 경우 실시간 가격을 조회하여 DB에 정상 저장해야 함")
    fun should_collect_and_save_prices_for_holding_stocks() {
        // given
        val mockHoldings = TossHoldingsResponse(
            result = TossHoldingsResult(
                items = listOf(
                    TossHoldingStock("AAPL", BigDecimal.TEN, BigDecimal("100.00")),
                    TossHoldingStock("TSLA", BigDecimal.ONE, BigDecimal("200.00"))
                )
            )
        )
        val mockAaplPrice = TossPriceResponse(
            ticker = "AAPL",
            currentPrice = BigDecimal("105.50"),
            previousClosePrice = BigDecimal("100.00")
        )
        val mockTslaPrice = TossPriceResponse(
            ticker = "TSLA",
            currentPrice = BigDecimal("195.00"),
            previousClosePrice = BigDecimal("200.00")
        )

        `when`(tossApiClient.getHoldings()).thenReturn(mockHoldings)
        `when`(tossApiClient.getRealtimePrice("AAPL")).thenReturn(mockAaplPrice)
        `when`(tossApiClient.getRealtimePrice("TSLA")).thenReturn(mockTslaPrice)
        
        val defaultHistory = MarketPriceHistory(symbol = "", price = BigDecimal.ZERO)
        doAnswer { it.arguments[0] as MarketPriceHistory }
            .`when`(marketPriceHistoryRepository)
            .save(anyNonNull(defaultHistory))

        // when
        marketDataCollectorService.collectMarketPrices()

        // then
        verify(tossApiClient).getHoldings()
        verify(tossApiClient).getRealtimePrice("AAPL")
        verify(tossApiClient).getRealtimePrice("TSLA")
        verify(marketPriceHistoryRepository, times(2)).save(anyNonNull(defaultHistory))
    }

    @Test
    @DisplayName("클린업 수행 시 지정한 기준일 이전의 시세 데이터를 레포지토리에서 삭제해야 함")
    fun should_delete_old_prices_on_cleanup() {
        // given
        val retentionDays = 30L
        doReturn(15)
            .`when`(marketPriceHistoryRepository)
            .deleteByCreatedAtBefore(anyNonNull(Instant.EPOCH))

        // when
        marketDataCollectorService.cleanOldPrices(retentionDays)

        // then
        verify(marketPriceHistoryRepository).deleteByCreatedAtBefore(anyNonNull(Instant.EPOCH))
    }
}
