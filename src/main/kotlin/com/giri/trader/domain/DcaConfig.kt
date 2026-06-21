package com.giri.trader.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "dca_config")
class DcaConfig(
    @Id
    @Column(nullable = false, length = 10)
    val symbol: String,

    @Column(nullable = false, precision = 15, scale = 2)
    var baseAmount: BigDecimal,

    @Column(nullable = false, precision = 15, scale = 2)
    var maxDailyBudget: BigDecimal,

    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
)
