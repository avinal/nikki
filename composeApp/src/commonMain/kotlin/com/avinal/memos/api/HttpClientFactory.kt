package com.avinal.memos.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.plugin
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object HttpClientFactory {

    fun create(tokenProvider: () -> String?): HttpClient {
        val client = HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                })
            }
            install(io.ktor.client.plugins.HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 30_000
            }
        }

        client.plugin(HttpSend).intercept { request ->
            tokenProvider()?.let { token ->
                request.headers.append(HttpHeaders.Authorization, "Bearer $token")
            }
            execute(request)
        }

        return client
    }
}
