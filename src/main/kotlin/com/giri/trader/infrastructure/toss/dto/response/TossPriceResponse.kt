package com.giri.trader.infrastructure.toss.dto.response

import java.math.BigDecimal

data class TossPriceResponse(
    val ticker: String,
    val currentPrice: BigDecimal,
    val previousClosePrice: BigDecimal
)
