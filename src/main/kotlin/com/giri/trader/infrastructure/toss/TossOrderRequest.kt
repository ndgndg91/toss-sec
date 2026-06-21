package com.giri.trader.infrastructure.toss

import java.math.BigDecimal

data class TossOrderRequest(
    val ticker: String,
    val side: String, // "BUY" or "SELL"
    val amount: BigDecimal,
    val priceType: String // "MARKET" or "LIMIT"
)
