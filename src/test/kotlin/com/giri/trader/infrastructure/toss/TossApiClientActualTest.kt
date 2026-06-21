package com.giri.trader.infrastructure.toss

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@Testcontainers
class TossApiClientActualTest {

    companion object {
        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer("postgres:17-alpine")
    }

    @Autowired
    private lateinit var tossApiClient: TossApiClient

    @Value("\${toss.api.client-id}")
    private lateinit var clientId: String

    @Value("\${toss.api.client-secret}")
    private lateinit var clientSecret: String

    @Test
    @DisplayName("환경변수로 주입된 API Key를 사용하여 실제 Toss API 호출이 가능해야 함")
    fun should_call_actual_toss_api() {
        if (clientId == "mock-api-key" || clientSecret == "mock-secret-key") {
            throw IllegalStateException("TOSS_SEC_API_KEY 및 TOSS_SEC_SECRET_KEY 환경변수가 설정되지 않았습니다. Gradle 실행 시 주입하세요.")
        }

        // 1. 보유 주식 목록 조회 검증
        val holdings = tossApiClient.getHoldings()
        println("Holdings result: $holdings")
        
        // 2. 특정 주식 실시간 시세 조회 검증 (예: AAPL)
        val price = tossApiClient.getRealtimePrice("AAPL")
        println("Price result: $price")
    }
}
