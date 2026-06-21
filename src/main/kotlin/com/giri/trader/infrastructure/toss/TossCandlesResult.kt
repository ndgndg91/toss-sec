package com.giri.trader.infrastructure.toss

data class TossCandlesResult(
    val candles: List<TossCandle>,
    val nextBefore: String?
)
