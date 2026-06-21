package com.giri.trader.domain

import java.math.BigDecimal
import java.math.RoundingMode

class DcaStrategy {
    fun calculateOrderAmount(
        baseAmount: Money,
        currentPrice: Money,
        previousClosePrice: Money
    ): Money {
        if (previousClosePrice.amount.compareTo(BigDecimal.ZERO) == 0) {
            return baseAmount
        }

        // 하락률 계산: (현재가 - 전일종가) / 전일종가
        val diff = currentPrice.amount.subtract(previousClosePrice.amount)
        val changeRate = diff.divide(previousClosePrice.amount, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))

        val multiplier = when {
            changeRate.compareTo(BigDecimal("-5.00")) <= 0 -> 2.0 // -5% 이하 하락 시 2.0배
            changeRate.compareTo(BigDecimal("-3.00")) <= 0 -> 1.5 // -3% 이하 하락 시 1.5배
            else -> 1.0 // 일반 상태 1.0배
        }

        return baseAmount.multiply(multiplier)
    }
}
