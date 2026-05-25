package com.hflocal.shared.data.remote
import com.hflocal.shared.domain.model.ProxyConfig
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit
actual fun createHttpClient(proxyConfig: ProxyConfig?): HttpClient = HttpClient(OkHttp) {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }) }
    engine {
        config {
            followRedirects(true)
            connectTimeout(30, TimeUnit.SECONDS)
            readTimeout(60, TimeUnit.SECONDS)
        }
    }
}
