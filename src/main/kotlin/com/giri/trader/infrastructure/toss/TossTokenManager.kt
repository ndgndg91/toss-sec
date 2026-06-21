package com.giri.trader.infrastructure.toss

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.Instant
import java.util.UUID

@Component
class TossTokenManager(
    private val tossRestClient: RestClient,
    @Value("\${toss.api.client-id}") private val clientId: String,
    @Value("\${toss.api.client-secret}") private val clientSecret: String,
    @Value("\${toss.api.base-url}") private val baseUrl: String,
    @Value("\${toss.api.sandbox-url}") private val sandboxUrl: String,
    @Value("\${toss.api.mode}") private val mode: String
) {
    private val log = LoggerFactory.getLogger(TossTokenManager::class.java)
    
    private var cachedToken: String? = null
    private var tokenExpiredAt: Instant? = null

    private fun getApiUrl(): String {
        return if (mode.lowercase() == "real") baseUrl else sandboxUrl
    }

    @Synchronized
    fun getAccessToken(): String {
        val now = Instant.now()
        // 토큰이 존재하고 만료 시점보다 60초 이상 여유가 있는 경우 캐시된 토큰 반환
        if (cachedToken != null && tokenExpiredAt != null && tokenExpiredAt!!.minusSeconds(60).isAfter(now)) {
            return cachedToken!!
        }

        log.info("Requesting new Toss API Access Token... [mode: {}]", mode)
        val traceId = UUID.randomUUID().toString()

        try {
            val response = tossRestClient.post()
                .uri("${getApiUrl()}/v1/oauth2/token")
                .header("traceId", traceId)
                .body(mapOf(
                    "grant_type" to "client_credentials",
                    "client_id" to clientId,
                    "client_secret" to clientSecret
                ))
                .retrieve()
                .body(TossTokenResponse::class.java)

            if (response != null) {
                cachedToken = response.accessToken
                tokenExpiredAt = Instant.now().plusSeconds(response.expiresIn)
                log.info("Successfully fetched and cached new Toss API Access Token.")
                return cachedToken!!
            } else {
                throw IllegalStateException("Failed to obtain OAuth Token from Toss API: Response was empty.")
            }
        } catch (e: Exception) {
            log.error("Error occurred while fetching Toss OAuth token (traceId: {}): {}", traceId, e.message, e)
            throw e
        }
    }
}
