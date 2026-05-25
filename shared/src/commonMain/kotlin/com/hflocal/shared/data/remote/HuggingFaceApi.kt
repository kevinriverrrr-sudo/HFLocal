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
     * Returns an empty list on any network / parsing error.
     */
    suspend fun searchModels(query: SearchQuery): List<HFModel> = try {
        http.get("$API/models") {
            parameter("search", query.query.ifEmpty { null })
            parameter("author", query.author.ifEmpty { null })
            parameter("pipeline_tag", query.pipelineTag.ifEmpty { null })
            parameter("library", "gguf")
            parameter("sort", query.sort)
            parameter("direction", query.direction)
            parameter("limit", query.limit)
            parameter("offset", query.offset)
        }.body()
    } catch (e: Exception) {
        emptyList()
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
     * This is a pure URL-construction method – no network call.
     */
    fun getDownloadUrl(modelId: String, filename: String): String =
        "$BASE/$modelId/resolve/main/$filename"
}
