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
    @Value("\${toss.api.base-url}") private val baseUrl: String
) {
    private val log = LoggerFactory.getLogger(TossApiClient::class.java)

    fun getHoldings(): TossHoldingsResponse {
        val traceId = UUID.randomUUID().toString()
        val token = tokenManager.getAccessToken()

        log.info("Fetching holding stocks [traceId: {}]", traceId)

        val response = tossRestClient.get()
            .uri("${baseUrl}/api/v1/holdings")
            .header("Authorization", "Bearer $token")
            .header("traceId", traceId)
            .retrieve()
            .body(TossHoldingsResponse::class.java)

        if (response != null) {
            log.info("Successfully fetched holdings. Count: {} [traceId: {}]", response.holdings.size, traceId)
            return response
        } else {
            log.error("Failed to retrieve holdings data (empty response) [traceId: {}]", traceId)
            throw IllegalStateException("Empty response from Toss Trading API for holdings")
        }
    }

    fun getRealtimePrice(ticker: String): TossPriceResponse {
        val traceId = UUID.randomUUID().toString()
        val token = tokenManager.getAccessToken()

        log.info("Fetching real-time price and candles for ticker: {} [traceId: {}]", ticker, traceId)

        // 1. 현재가 조회 API 호출
        val priceApiResponse = tossRestClient.get()
            .uri("${baseUrl}/api/v1/prices?symbols=$ticker")
            .header("Authorization", "Bearer $token")
            .header("traceId", traceId)
            .retrieve()
            .body(TossPricesApiDto::class.java)

        val currentPrice = priceApiResponse?.result?.firstOrNull()?.lastPrice
            ?: throw IllegalStateException("Failed to fetch last price for ticker: $ticker")

        // 2. 전일 종가 획득을 위해 일봉 캔들 조회 API 호출
        val candlesApiResponse = tossRestClient.get()
            .uri("${baseUrl}/api/v1/candles?symbol=$ticker&interval=1d&count=2")
            .header("Authorization", "Bearer $token")
            .header("traceId", traceId)
            .retrieve()
            .body(TossCandlesResponse::class.java)

        val candles = candlesApiResponse?.result?.candles
        val previousClosePrice = if (candles != null && candles.size >= 2) {
            // index 1이 전일 일봉
            candles[1].closePrice
        } else {
            log.warn("Insufficient daily candles for ticker: {}. Falling back to last price as prev close.", ticker)
            currentPrice
        }

        log.info("Price query success. Symbol: {}, Current: {}, Previous Close: {} [traceId: {}]", 
            ticker, currentPrice, previousClosePrice, traceId)

        return TossPriceResponse(
            ticker = ticker,
            currentPrice = currentPrice,
            previousClosePrice = previousClosePrice
        )
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

        // 주문 엔드포인트도 실제 스펙에 맞춰 /api/v1/orders로 매핑
        val response = tossRestClient.post()
            .uri("${baseUrl}/api/v1/orders")
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
