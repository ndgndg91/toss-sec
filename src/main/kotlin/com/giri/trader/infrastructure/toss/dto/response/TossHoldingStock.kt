package com.giri.trader.infrastructure.toss.dto.response

import java.math.BigDecimal

data class TossHoldingStock(
    val symbol: String,
    val quantity: BigDecimal,
    val averagePurchasePrice: BigDecimal
)
