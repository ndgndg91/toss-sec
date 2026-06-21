package com.giri.trader.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class DcaStrategyTest {

    private val strategy = DcaStrategy()
    private val baseAmount = Money.of(10000.0)

    @Test
    @DisplayName("보합세 또는 상승세일 때 기본 금액을 반환해야 함")
    fun should_return_base_amount_when_price_is_equal_or_higher() {
        // given
        val currentPrice = Money.of(100.0)
        val previousClose = Money.of(100.0)

        // when
        val result = strategy.calculateOrderAmount(baseAmount, currentPrice, previousClose)

        // then
        assertEquals(BigDecimal("10000.00"), result.amount)
    }

    @Test
    @DisplayName("하락률이 3퍼센트 미만일 때 기본 금액을 반환해야 함")
    fun should_return_base_amount_when_drop_is_less_than_three_percent() {
        // given
        val currentPrice = Money.of(98.0) // -2% 하락
        val previousClose = Money.of(100.0)

        // when
        val result = strategy.calculateOrderAmount(baseAmount, currentPrice, previousClose)

        // then
        assertEquals(BigDecimal("10000.00"), result.amount)
    }

    @Test
    @DisplayName("하락률이 3퍼센트 이상 5퍼센트 미만일 때 1.5배 가중치를 적용해야 함")
    fun should_return_one_and_half_times_amount_when_drop_is_between_three_and_five_percent() {
        // given
        val currentPrice = Money.of(96.0) // -4% 하락
        val previousClose = Money.of(100.0)

        // when
        val result = strategy.calculateOrderAmount(baseAmount, currentPrice, previousClose)

        // then
        assertEquals(BigDecimal("15000.00"), result.amount)
    }

    @Test
    @DisplayName("하락률이 5퍼센트 이상일 때 2배 가중치를 적용해야 함")
    fun should_return_double_amount_when_drop_is_five_percent_or_more() {
        // given
        val currentPrice = Money.of(94.0) // -6% 하락
        val previousClose = Money.of(100.0)

        // when
        val result = strategy.calculateOrderAmount(baseAmount, currentPrice, previousClose)

        // then
        assertEquals(BigDecimal("20000.00"), result.amount)
    }
}
