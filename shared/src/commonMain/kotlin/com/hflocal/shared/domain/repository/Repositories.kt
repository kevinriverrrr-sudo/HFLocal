package com.hflocal.shared.domain.repository

import com.hflocal.shared.domain.model.*
import kotlinx.coroutines.flow.Flow

interface IHuggingFaceRepository {
    suspend fun searchModels(query: SearchQuery): List<HFModel>
    suspend fun getModelDetails(modelId: String): HFModel
    suspend fun getWhoami(token: String): UserInfo
    suspend fun getDownloadUrl(modelId: String, filename: String): String
}

interface IModelRepository {
    fun getDownloadedModels(): Flow<List<DownloadedModel>>
    suspend fun getDownloadedModel(modelId: String): DownloadedModel?
    suspend fun saveModel(model: DownloadedModel)
    suspend fun deleteModel(modelId: String)
    suspend fun updateDownloadProgress(modelId: String, progress: Float)
    suspend fun isModelDownloaded(modelId: String): Boolean
}

interface IChatRepository {
    fun getSessions(): Flow<List<ChatSession>>
    suspend fun createSession(session: ChatSession): Long
    suspend fun updateSession(session: ChatSession)
    suspend fun deleteSession(sessionId: Long)
    fun getMessages(sessionId: Long): Flow<List<ChatMessage>>
    suspend fun addMessage(message: ChatMessage): Long
    suspend fun updateMessage(message: ChatMessage)
}

interface IDeviceRepository {
    fun getDeviceProfile(): Flow<DeviceProfile>
    suspend fun refreshDeviceProfile(): DeviceProfile
    suspend fun getCurrentTier(): PerformanceTier
}

interface ISettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun updateSettings(settings: AppSettings)
    suspend fun getHfToken(): String?
    suspend fun setHfToken(token: String)
    suspend fun clearHfToken()
}
