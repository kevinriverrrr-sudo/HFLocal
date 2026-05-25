package com.hflocal.shared.data.remote

import com.hflocal.shared.domain.model.HFModel
import com.hflocal.shared.domain.model.SearchQuery
import com.hflocal.shared.domain.model.UserInfo
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class HuggingFaceApi(private val http: HttpClient) {

    companion object {
        const val BASE = "https://huggingface.co"
        const val API = "$BASE/api"
    }

    /**
     * Search models on HuggingFace Hub.
     * Re-throws on error so callers can show proper error UI.
     */
    suspend fun searchModels(query: SearchQuery): List<HFModel> {
        return http.get("$API/models") {
            if (query.query.isNotBlank()) parameter("search", query.query)
            if (query.author.isNotBlank()) parameter("author", query.author)
            if (query.pipelineTag.isNotBlank()) parameter("pipeline_tag", query.pipelineTag)
            parameter("library", "gguf")
            parameter("sort", query.sort)
            parameter("direction", query.direction)
            parameter("limit", query.limit)
            parameter("offset", query.offset)
        }.body()
    }

    /**
     * Get full details for a single model by its ID (e.g. "author/model-name").
     * Re-throws on error so callers can decide how to handle it.
     */
    suspend fun getModelDetails(modelId: String): HFModel =
        http.get("$API/models/$modelId").body()

    /**
     * Verify an HF token and retrieve user info.
     * Re-throws on authentication / network error.
     */
    suspend fun getWhoami(token: String): UserInfo =
        http.get("$API/whoami") {
            header("Authorization", "Bearer $token")
        }.body()

    /**
     * Build the direct-download URL for a specific file inside a model repo.
     * This is a pure URL-construction method - no network call.
     */
    fun getDownloadUrl(modelId: String, filename: String): String =
        "$BASE/$modelId/resolve/main/$filename"
}
