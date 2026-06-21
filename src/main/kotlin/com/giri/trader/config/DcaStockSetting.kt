package com.giri.trader.config

import java.math.BigDecimal

data class DcaStockSetting(
    val baseAmount: BigDecimal,
    val maxDailyBudget: BigDecimal
)
