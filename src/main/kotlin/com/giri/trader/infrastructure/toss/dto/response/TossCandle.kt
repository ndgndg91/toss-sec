package com.giri.trader.infrastructure.toss.dto.response

import java.math.BigDecimal

data class TossCandle(
    val timestamp: String,
    val openPrice: BigDecimal,
    val highPrice: BigDecimal,
    val lowPrice: BigDecimal,
    val closePrice: BigDecimal,
    val volume: BigDecimal,
    val currency: String
)
