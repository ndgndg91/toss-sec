package com.giri.trader.infrastructure.toss

import java.math.BigDecimal

data class TossHoldingStock(
    val symbol: String,
    val quantity: BigDecimal,
    val averagePurchasePrice: BigDecimal
)
