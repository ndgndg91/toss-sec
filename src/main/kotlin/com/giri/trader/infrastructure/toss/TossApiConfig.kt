package com.giri.trader.infrastructure.toss

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

@Configuration
class TossApiConfig {

    @Bean
    fun tossRestClient(): RestClient {
        // HTTP/2 및 커넥션 풀을 자체 내장한 Jdk HttpClient 구성
        val httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()

        val requestFactory = JdkClientHttpRequestFactory(httpClient).apply {
            setReadTimeout(Duration.ofSeconds(5))
        }

        return RestClient.builder()
            .requestFactory(requestFactory)
            .build()
    }
}
