package com.giri.trader.infrastructure.toss

import java.math.BigDecimal

data class TossOrderRequest(
    val symbol: String,
    val side: String, // "BUY" or "SELL"
    val orderType: String, // "MARKET" or "LIMIT"
    val orderAmount: BigDecimal? = null, // 금액 지정 매수 (US 마켓 소수점 전용)
    val quantity: BigDecimal? = null // 수량 지정 매수
)
