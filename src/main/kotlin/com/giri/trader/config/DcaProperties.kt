package com.giri.trader.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.math.BigDecimal

@ConfigurationProperties(prefix = "trader.dca")
data class DcaProperties(
    val defaultBaseAmount: BigDecimal = BigDecimal("10000.00"),
    val defaultMaxDailyBudget: BigDecimal = BigDecimal("50000.00"),
    val cron: String,
    val dryRun: Boolean,
    val notificationEmail: String
)
