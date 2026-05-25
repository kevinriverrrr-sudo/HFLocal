package com.hflocal.shared.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.hflocal.shared.data.local.db.HFLocalDatabase
import com.hflocal.shared.data.remote.HuggingFaceApi
import com.hflocal.shared.domain.model.*
import com.hflocal.shared.domain.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class HuggingFaceRepositoryImpl(
    private val api: HuggingFaceApi
) : IHuggingFaceRepository {

    override suspend fun searchModels(query: SearchQuery): List<HFModel> {
        return api.searchModels(query)
    }

    override suspend fun getModelDetails(modelId: String): HFModel {
        return api.getModelDetails(modelId)
    }

    override suspend fun getWhoami(token: String): UserInfo {
        return api.getWhoami(token)
    }

    override suspend fun getDownloadUrl(modelId: String, filename: String): String {
        return api.getDownloadUrl(modelId, filename)
    }
}

class ModelRepositoryImpl(
    private val database: HFLocalDatabase
) : IModelRepository {

    override fun getDownloadedModels(): Flow<List<DownloadedModel>> {
        return database.hFLocalDatabaseQueries
            .selectAllDownloadedModels()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list ->
                list.map { row ->
                    DownloadedModel(
                        id = row.id,
                        modelId = row.model_id,
                        author = row.author,
                        fileName = row.file_name,
                        filePath = row.file_path,
                        fileSizeBytes = row.file_size_bytes,
                        quantization = row.quantization,
                        downloadDate = row.download_date,
                        isDownloaded = row.is_downloaded == 1L,
                        downloadProgress = row.download_progress.toFloat()
                    )
                }
            }
    }

    override suspend fun getDownloadedModel(modelId: String): DownloadedModel? {
        return database.hFLocalDatabaseQueries
            .selectDownloadedModelById(modelId)
            .executeAsOneOrNull()
            ?.let { row ->
                DownloadedModel(
                    id = row.id,
                    modelId = row.model_id,
                    author = row.author,
                    fileName = row.file_name,
                    filePath = row.file_path,
                    fileSizeBytes = row.file_size_bytes,
                    quantization = row.quantization,
                    downloadDate = row.download_date,
                    isDownloaded = row.is_downloaded == 1L,
                    downloadProgress = row.download_progress.toFloat()
                )
            }
    }

    override suspend fun saveModel(model: DownloadedModel) {
        database.hFLocalDatabaseQueries.insertOrReplaceDownloadedModel(
            model_id = model.modelId,
            author = model.author,
            file_name = model.fileName,
            file_path = model.filePath,
            file_size_bytes = model.fileSizeBytes,
            quantization = model.quantization,
            download_date = model.downloadDate,
            is_downloaded = if (model.isDownloaded) 1L else 0L,
            download_progress = model.downloadProgress.toDouble()
        )
    }

    override suspend fun deleteModel(modelId: String) {
        database.hFLocalDatabaseQueries.deleteDownloadedModel(modelId)
    }

    override suspend fun updateDownloadProgress(modelId: String, progress: Float) {
        database.hFLocalDatabaseQueries.updateDownloadProgress(
            download_progress = progress.toDouble(),
            model_id = modelId
        )
    }

    override suspend fun isModelDownloaded(modelId: String): Boolean {
        return database.hFLocalDatabaseQueries
            .selectDownloadedModelById(modelId)
            .executeAsOneOrNull()
            ?.let { it.is_downloaded == 1L } == true
    }
}

class ChatRepositoryImpl(
    private val database: HFLocalDatabase
) : IChatRepository {

    override fun getSessions(): Flow<List<ChatSession>> {
        return database.hFLocalDatabaseQueries
            .selectAllChatSessions()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list ->
                list.map { row ->
                    ChatSession(
                        id = row.id,
                        modelId = row.model_id,
                        title = row.title,
                        createdAt = row.created_at,
                        updatedAt = row.updated_at,
                        systemPrompt = row.system_prompt,
                        messageCount = row.message_count.toInt()
                    )
                }
            }
    }

    override suspend fun createSession(session: ChatSession): Long {
        val now = System.currentTimeMillis()
        return database.transactionWithResult {
            database.hFLocalDatabaseQueries.insertChatSession(
                model_id = session.modelId,
                title = session.title.ifEmpty { "New Chat" },
                created_at = if (session.createdAt > 0) session.createdAt else now,
                updated_at = now,
                system_prompt = session.systemPrompt,
                message_count = 0
            )
            database.hFLocalDatabaseQueries.lastInsertedRowId().executeAsOne()
        }
    }

    override suspend fun updateSession(session: ChatSession) {
        database.hFLocalDatabaseQueries.updateChatSession(
            model_id = session.modelId,
            title = session.title,
            updated_at = System.currentTimeMillis(),
            system_prompt = session.systemPrompt,
            message_count = session.messageCount.toLong(),
            id = session.id
        )
    }

    override suspend fun deleteSession(sessionId: Long) {
        database.hFLocalDatabaseQueries.deleteChatSession(sessionId)
    }

    override fun getMessages(sessionId: Long): Flow<List<ChatMessage>> {
        return database.hFLocalDatabaseQueries
            .selectMessagesBySessionId(sessionId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list ->
                list.map { row ->
                    ChatMessage(
                        id = row.id,
                        sessionId = row.session_id,
                        role = try {
                            MessageRole.valueOf(row.role)
                        } catch (e: IllegalArgumentException) {
                            MessageRole.USER
                        },
                        content = row.content,
                        timestamp = row.timestamp,
                        imagePath = row.image_path,
                        isStreaming = row.is_streaming == 1L
                    )
                }
            }
    }

    override suspend fun addMessage(message: ChatMessage): Long {
        return database.transactionWithResult {
            database.hFLocalDatabaseQueries.insertMessage(
                session_id = message.sessionId,
                role = message.role.name,
                content = message.content,
                timestamp = if (message.timestamp > 0) message.timestamp else System.currentTimeMillis(),
                image_path = message.imagePath,
                is_streaming = if (message.isStreaming) 1L else 0L
            )
            database.hFLocalDatabaseQueries.lastInsertedRowId().executeAsOne()
        }
    }

    override suspend fun updateMessage(message: ChatMessage) {
        database.hFLocalDatabaseQueries.updateMessage(
            content = message.content,
            is_streaming = if (message.isStreaming) 1L else 0L,
            id = message.id
        )
    }
}

class SettingsRepositoryImpl(
    private val database: HFLocalDatabase
) : ISettingsRepository {

    private val queries = database.hFLocalDatabaseQueries

    override fun getSettings(): Flow<AppSettings> {
        return queries.getSettingByKey("app_settings")
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { json ->
                if (json != null) {
                    try {
                        kotlinx.serialization.json.Json.decodeFromString<AppSettings>(json)
                    } catch (_: Exception) { AppSettings() }
                } else {
                    AppSettings()
                }
            }
    }

    override suspend fun updateSettings(newSettings: AppSettings) {
        val json = kotlinx.serialization.json.Json.encodeToString(
            com.hflocal.shared.domain.model.AppSettings.serializer(),
            newSettings
        )
        queries.setSetting(key = "app_settings", value_ = json)
    }

    override suspend fun getHfToken(): String? = withContext(Dispatchers.IO) {
        val row = try {
            queries.getSettingByKey("hf_token").executeAsOneOrNull()
        } catch (_: Exception) { null }
        row
    }

    override suspend fun setHfToken(token: String) {
        queries.setSetting(key = "hf_token", value_ = token)
    }

    override suspend fun clearHfToken() {
        queries.deleteSettingByKey("hf_token")
    }

}
