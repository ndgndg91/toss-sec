package com.giri.trader.infrastructure.toss

import java.math.BigDecimal

data class TossPriceItem(
    val symbol: String,
    val lastPrice: BigDecimal,
    val currency: String
)
