package com.giri.trader.domain

import java.math.BigDecimal
import java.math.RoundingMode

data class Money(
    val amount: BigDecimal
) {
    companion object {
        val ZERO: Money = Money(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
        
        fun of(value: Double): Money {
            return Money(BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP))
        }

        fun of(value: String): Money {
            return Money(BigDecimal(value).setScale(2, RoundingMode.HALF_UP))
        }
    }

    fun multiply(factor: Double): Money {
        val result = this.amount.multiply(BigDecimal.valueOf(factor))
        return Money(result.setScale(2, RoundingMode.HALF_UP))
    }

    fun isGreaterThan(other: Money): Boolean {
        return this.amount > other.amount
    }

    override fun toString(): String {
        return amount.toPlainString()
    }
}
