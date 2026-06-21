package com.giri.trader.infrastructure.persistence

import com.giri.trader.domain.OrderHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OrderHistoryRepository : JpaRepository<OrderHistory, Long>
