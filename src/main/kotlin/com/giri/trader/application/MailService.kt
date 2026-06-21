package com.giri.trader.application

import com.giri.trader.domain.OrderHistory
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class MailService(
    private val mailSender: JavaMailSender,
    @param:Value("\${spring.mail.username:}") private val fromEmail: String
) {
    private val log = LoggerFactory.getLogger(MailService::class.java)

    fun sendEmail(to: String, subject: String, content: String) {
        if (to.isBlank()) {
            log.warn("Recipient email is empty. Skipping email dispatch.")
            return
        }

        try {
            val message = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")
            
            if (fromEmail.isNotBlank()) {
                helper.setFrom(fromEmail)
            }
            helper.setTo(to)
            helper.setSubject(subject)
            helper.setText(content, true) // HTML format

            mailSender.send(message)
            log.info("Email sent successfully to: {}", to)
        } catch (e: Exception) {
            log.error("Failed to send email to {}: {}", to, e.message, e)
        }
    }

    fun sendDailyReport(to: String, histories: List<OrderHistory>) {
        val today = LocalDate.now()
        val subject = "[Toss DCA] Daily Trading Report - $today"
        
        val htmlContent = StringBuilder().apply {
            append("<h2>Toss DCA Daily Trading Report ($today)</h2>")
            append("<table border='1' cellpadding='8' style='border-collapse: collapse;'>")
            append("<tr style='background-color: #f2f2f2;'>")
            append("<th>Ticker</th><th>Order Amount</th><th>Current Price</th><th>Prev Close</th><th>Status</th><th>OrderId</th>")
            append("</tr>")
            
            for (h in histories) {
                val statusColor = when(h.status) {
                    "SUCCESS" -> "green"
                    "FAIL" -> "red"
                    else -> "orange"
                }
                append("<tr>")
                append("<td><b>${h.ticker}</b></td>")
                append("<td>${h.orderAmount}</td>")
                append("<td>${h.currentPrice}</td>")
                append("<td>${h.previousClosePrice}</td>")
                append("<td style='color: $statusColor;'><b>${h.status}</b></td>")
                append("<td>${h.orderId ?: "-"}</td>")
                append("</tr>")
            }
            append("</table>")
            append("<br><p>This is an automated report from Toss DCA Trading Bot.</p>")
        }.toString()

        sendEmail(to, subject, htmlContent)
    }

    fun sendErrorReport(to: String, errorMessage: String) {
        val subject = "[ALERT] Toss DCA Trading Bot Failure"
        val htmlContent = """
            <h2>DCA Automatic Trading Process Failed</h2>
            <p style='color: red;'><b>Error Detail:</b></p>
            <pre style='background: #f4f4f4; padding: 10px;'>$errorMessage</pre>
            <p>Please check the server logs immediately.</p>
        """.trimIndent()

        sendEmail(to, subject, htmlContent)
    }
}
