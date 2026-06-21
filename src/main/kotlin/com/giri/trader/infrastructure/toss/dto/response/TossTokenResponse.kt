package com.giri.trader.infrastructure.toss.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

data class TossTokenResponse(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("token_type") val tokenType: String,
    @JsonProperty("expires_in") val expiresIn: Long
)
