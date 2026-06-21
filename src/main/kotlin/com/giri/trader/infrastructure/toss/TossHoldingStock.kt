package com.giri.trader.infrastructure.toss

import java.math.BigDecimal

data class TossHoldingStock(
    val ticker: String,
    val quantity: BigDecimal,
    val averagePurchasePrice: BigDecimal
)
