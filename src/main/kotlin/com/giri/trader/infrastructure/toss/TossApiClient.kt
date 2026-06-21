package com.giri.trader.infrastructure.toss

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.math.BigDecimal
import java.util.UUID

@Component
class TossApiClient(
    private val tossRestClient: RestClient,
    private val tokenManager: TossTokenManager,
    @Value("\${toss.api.base-url}") private val baseUrl: String,
    @Value("\${toss.api.sandbox-url}") private val sandboxUrl: String,
    @Value("\${toss.api.mode}") private val mode: String
) {
    private val log = LoggerFactory.getLogger(TossApiClient::class.java)

    private fun getApiUrl(): String {
        return if (mode.lowercase() == "real") baseUrl else sandboxUrl
    }

    fun getRealtimePrice(ticker: String): TossPriceResponse {
        val traceId = UUID.randomUUID().toString()
        val token = tokenManager.getAccessToken()

        log.info("Fetching price for ticker: {} [traceId: {}]", ticker, traceId)

        val response = tossRestClient.get()
            .uri("${getApiUrl()}/v1/market/realtime-price?ticker=$ticker")
            .header("Authorization", "Bearer $token")
            .header("traceId", traceId)
            .retrieve()
            .body(TossPriceResponse::class.java)

        if (response != null) {
            log.info("Price fetched for {}: Current={}, PrevClose={} [traceId: {}]", 
                ticker, response.currentPrice, response.previousClosePrice, traceId)
            return response
        } else {
            log.error("Failed to retrieve price data (empty response) [traceId: {}]", traceId)
            throw IllegalStateException("Empty response from Toss Market API for ticker: $ticker")
        }
    }

    fun placeOrder(ticker: String, amount: BigDecimal): TossOrderResponse {
        val traceId = UUID.randomUUID().toString()
        val token = tokenManager.getAccessToken()

        log.info("Placing order for ticker: {}, Amount: {} [traceId: {}]", ticker, amount, traceId)

        val requestBody = TossOrderRequest(
            ticker = ticker,
            side = "BUY",
            amount = amount,
            priceType = "MARKET"
        )

        val response = tossRestClient.post()
            .uri("${getApiUrl()}/v1/trading/order")
            .header("Authorization", "Bearer $token")
            .header("traceId", traceId)
            .body(requestBody)
            .retrieve()
            .body(TossOrderResponse::class.java)

        if (response != null) {
            log.info("Order placed successfully. OrderID: {}, Status: {} [traceId: {}]", 
                response.orderId, response.status, traceId)
            return response
        } else {
            log.error("Failed to place order (empty response) [traceId: {}]", traceId)
            throw IllegalStateException("Empty response from Toss Trading API for order: $ticker")
        }
    }
}
