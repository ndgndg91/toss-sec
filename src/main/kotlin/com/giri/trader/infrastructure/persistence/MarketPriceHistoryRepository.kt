package com.giri.trader.infrastructure.persistence

import com.giri.trader.domain.MarketPriceHistory
import java.time.Instant

interface MarketPriceHistoryRepository {
    fun save(history: MarketPriceHistory): MarketPriceHistory
    fun deleteByCreatedAtBefore(time: Instant): Int
    fun findAll(): List<MarketPriceHistory>
}
