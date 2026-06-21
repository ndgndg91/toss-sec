package com.giri.trader.infrastructure.toss.dto.response

data class TossCandlesResult(
    val candles: List<TossCandle>,
    val nextBefore: String?
)
