package com.giri.trader.infrastructure.toss.dto.response

import java.math.BigDecimal

data class TossPriceItem(
    val symbol: String,
    val lastPrice: BigDecimal,
    val currency: String
)
