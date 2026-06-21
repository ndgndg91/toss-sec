package com.giri.trader.infrastructure.persistence

import com.giri.trader.domain.DcaConfig
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DcaConfigRepository : JpaRepository<DcaConfig, String>
