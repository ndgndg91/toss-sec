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

    @Volatile
    private var cachedAccountSeq: Long? = null

    @Synchronized
    private fun getAccountSeq(): Long {
        if (cachedAccountSeq != null) {
            return cachedAccountSeq!!
        }

        val traceId = UUID.randomUUID().toString()
        val token = tokenManager.getAccessToken()

        log.info("Fetching accounts to resolve accountSeq [traceId: {}]", traceId)

        val response = tossRestClient.get()
            .uri("${baseUrl}/api/v1/accounts")
            .header("Authorization", "Bearer $token")
            .header("traceId", traceId)
            .retrieve()
            .body(TossAccountsResponse::class.java)

        val account = response?.result?.firstOrNull()
            ?: throw IllegalStateException("No brokerage account found in Toss account list [traceId: $traceId]")

        cachedAccountSeq = account.accountSeq
        log.info("Resolved and cached accountSeq: {} for AccountNo: {} [traceId: {}]", 
            cachedAccountSeq, account.accountNo, traceId)

        return cachedAccountSeq!!
    }

    fun getHoldings(): TossHoldingsResponse {
        val traceId = UUID.randomUUID().toString()
        val token = tokenManager.getAccessToken()
        val accountSeq = getAccountSeq()

        log.info("Fetching holding stocks for accountSeq: {} [traceId: {}]", accountSeq, traceId)

        val response = tossRestClient.get()
            .uri("${baseUrl}/api/v1/holdings")
            .header("Authorization", "Bearer $token")
            .header("X-Tossinvest-Account", accountSeq.toString())
            .header("traceId", traceId)
            .retrieve()
            .body(TossHoldingsResponse::class.java)

        if (response != null) {
            log.info("Successfully fetched holdings. Count: {} [traceId: {}]", response.result.items.size, traceId)
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
        val accountSeq = getAccountSeq()

        log.info("Placing order for ticker: {}, Amount: {}, AccountSeq: {} [traceId: {}]", 
            ticker, amount, accountSeq, traceId)

        val requestBody = TossOrderRequest(
            symbol = ticker,
            side = "BUY",
            orderType = "MARKET",
            orderAmount = amount
        )

        val response = tossRestClient.post()
            .uri("${baseUrl}/api/v1/orders")
            .header("Authorization", "Bearer $token")
            .header("X-Tossinvest-Account", accountSeq.toString())
            .header("traceId", traceId)
            .body(requestBody)
            .retrieve()
            .body(TossOrderResponse::class.java)

        if (response != null) {
            log.info("Order placed successfully. OrderID: {} [traceId: {}]", 
                response.result.orderId, traceId)
            return response
        } else {
            log.error("Failed to place order (empty response) [traceId: {}]", traceId)
            throw IllegalStateException("Empty response from Toss Trading API for order: $ticker")
        }
    }
}
