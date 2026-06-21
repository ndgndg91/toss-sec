package com.giri.trader.infrastructure.toss

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.Instant
import java.util.UUID
import com.giri.trader.infrastructure.toss.dto.response.*

@Component
class TossTokenManager(
    private val tossRestClient: RestClient,
    @param:Value("\${toss.api.client-id}") private val clientId: String,
    @param:Value("\${toss.api.client-secret}") private val clientSecret: String,
    @param:Value("\${toss.api.base-url}") private val baseUrl: String
) {
    private val log = LoggerFactory.getLogger(TossTokenManager::class.java)
    
    private var cachedToken: String? = null
    private var tokenExpiredAt: Instant? = null

    @Synchronized
    fun getAccessToken(): String {
        val now = Instant.now()
        // 토큰이 존재하고 만료 시점보다 60초 이상 여유가 있는 경우 캐시된 토큰 반환
        if (cachedToken != null && tokenExpiredAt != null && tokenExpiredAt!!.minusSeconds(60).isAfter(now)) {
            return cachedToken!!
        }

        log.info("Requesting new Toss API Access Token...")
        val traceId = UUID.randomUUID().toString()

        try {
            val bodyMap = org.springframework.util.LinkedMultiValueMap<String, String>().apply {
                add("grant_type", "client_credentials")
                add("client_id", clientId)
                add("client_secret", clientSecret)
            }

            val response = tossRestClient.post()
                .uri("${baseUrl}/oauth2/token")
                .header("traceId", traceId)
                .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                .body(bodyMap)
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
