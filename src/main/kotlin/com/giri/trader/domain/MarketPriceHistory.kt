package com.giri.trader.domain

import java.math.BigDecimal
import java.time.Instant

data class MarketPriceHistory(
    val id: Long? = null,
    val symbol: String,
    val price: BigDecimal,
    val createdAt: Instant = Instant.now()
)
