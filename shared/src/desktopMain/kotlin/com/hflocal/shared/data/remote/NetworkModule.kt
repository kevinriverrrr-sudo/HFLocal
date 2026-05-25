package com.hflocal.shared.data.remote
import com.hflocal.shared.domain.model.ProxyConfig
import com.hflocal.shared.domain.model.ProxyType
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
actual fun createHttpClient(proxyConfig: ProxyConfig?): HttpClient = HttpClient(OkHttp) {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }) }
    engine {
        proxyConfig?.let { c ->
            if (c.enabled) {
                proxy = Proxy(
                    when (c.type) {
                        ProxyType.HTTP -> Proxy.Type.HTTP
                        ProxyType.HTTPS -> Proxy.Type.HTTP
                        ProxyType.SOCKS5 -> Proxy.Type.SOCKS
                    },
                    InetSocketAddress(c.host, c.port)
                )
            }
        }
        config {
            followRedirects(true)
            connectTimeout(30, TimeUnit.SECONDS)
            readTimeout(60, TimeUnit.SECONDS)
        }
    }
}
