package com.giri.trader.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "order_history")
class OrderHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val ticker: String,

    @Column(nullable = false, precision = 15, scale = 2)
    val orderAmount: BigDecimal,

    @Column(nullable = false, precision = 15, scale = 2)
    val currentPrice: BigDecimal,

    @Column(nullable = false, precision = 15, scale = 2)
    val previousClosePrice: BigDecimal,

    @Column(name = "order_id")
    var orderId: String? = null,

    @Column(nullable = false)
    var status: String,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)
