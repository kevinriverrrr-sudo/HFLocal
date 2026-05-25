package com.hflocal.shared.data.remote
import com.hflocal.shared.domain.model.ProxyConfig
import io.ktor.client.*
expect fun createHttpClient(proxyConfig: ProxyConfig? = null): HttpClient
