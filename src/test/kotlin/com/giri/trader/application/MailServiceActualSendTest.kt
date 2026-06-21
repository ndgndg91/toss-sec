package com.giri.trader.application

import com.giri.trader.domain.OrderHistory
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal

@SpringBootTest
@Testcontainers
// @Disabled("실제 메일 발송 테스트이므로 로컬에서 SMTP 설정 및 환경변수 주입 후 수동 실행")
class MailServiceActualSendTest {

    companion object {
        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer("postgres:17-alpine")
    }

    @Autowired
    private lateinit var mailService: MailService

    @Value("\${trader.dca.notification-email:ndgndg91@gmail.com}")
    private lateinit var notificationEmail: String

    @Test
    @DisplayName("환경변수로 지정된 수신자에게 모의 거래 내역 리포트 메일이 실제로 성공적으로 발송되어야 함")
    fun should_send_actual_daily_report_mail() {
        // given
        val recipient = notificationEmail.ifBlank { 
            throw IllegalStateException("TRADER_NOTIFICATION_EMAIL 환경변수가 설정되지 않았습니다. Gradle 실행 시 -DTRADER_NOTIFICATION_EMAIL=xxx 를 사용하세요.")
        }
        
        val executedHistories = listOf(
            OrderHistory(
                ticker = "AAPL",
                orderAmount = BigDecimal("15000.00"),
                currentPrice = BigDecimal("180.50"),
                previousClosePrice = BigDecimal("185.00"),
                status = "SUCCESS"
            ),
            OrderHistory(
                ticker = "TSLA",
                orderAmount = BigDecimal("20000.00"),
                currentPrice = BigDecimal("170.00"),
                previousClosePrice = BigDecimal("180.00"),
                status = "DRY_RUN"
            )
        )

        // when
        mailService.sendDailyReport(recipient, executedHistories)

        // then
        // 메일 수신함에서 직접 확인 필요
    }
}
