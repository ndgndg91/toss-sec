package com.giri.trader.infrastructure.toss

import java.math.BigDecimal

data class TossPriceResponse(
    val ticker: String,
    val currentPrice: BigDecimal,
    val previousClosePrice: BigDecimal
)
