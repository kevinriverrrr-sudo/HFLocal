package com.hflocal.shared.domain.usecase

import com.hflocal.shared.domain.model.*
import com.hflocal.shared.domain.repository.*
import kotlinx.coroutines.flow.Flow

// ── HuggingFace / Model search ───────────────────────────────────────

class SearchModelsUseCase(
    private val hfRepository: IHuggingFaceRepository,
) {
    /**
     * Search models on HuggingFace Hub.
     * Note: we do NOT filter by device tier here because the search API
     * does not return file sizes for siblings.  Tier filtering happens
     * only on the model detail page where individual file sizes are available.
     */
    suspend operator fun invoke(query: SearchQuery): List<HFModel> {
        return hfRepository.searchModels(query)
    }
}

class GetModelDetailsUseCase(
    private val repository: IHuggingFaceRepository,
) {
    suspend operator fun invoke(modelId: String): HFModel =
        repository.getModelDetails(modelId)
}

// ── Downloaded models ────────────────────────────────────────────────

class GetDownloadedModelsUseCase(
    private val repository: IModelRepository,
) {
    operator fun invoke(): Flow<List<DownloadedModel>> =
        repository.getDownloadedModels()
}

class DeleteModelUseCase(
    private val repository: IModelRepository,
) {
    suspend operator fun invoke(modelId: String) =
        repository.deleteModel(modelId)
}

class DownloadModelUseCase(
    private val hfRepository: IHuggingFaceRepository,
    private val modelRepository: IModelRepository,
) {
    /**
     * Prepares a model for download by recording it in the repository
     * with initial progress.  The actual file download is handled
     * by the download manager / service layer which calls
     * [IModelRepository.updateDownloadProgress].
     */
    suspend operator fun invoke(
        modelId: String,
        fileName: String,
    ) {
        val url = hfRepository.getDownloadUrl(modelId, fileName)
        val model = DownloadedModel(
            modelId = modelId,
            fileName = fileName,
            filePath = "",                          // BUG-18 fix: empty until download completes
            downloadProgress = 0f,
            isDownloaded = false,
        )
        modelRepository.saveModel(model)
    }
}

// ── Chat sessions ────────────────────────────────────────────────────

class GetChatSessionsUseCase(
    private val repository: IChatRepository,
) {
    operator fun invoke(): Flow<List<ChatSession>> =
        repository.getSessions()
}

class CreateChatSessionUseCase(
    private val repository: IChatRepository,
) {
    suspend operator fun invoke(session: ChatSession): Long =
        repository.createSession(session)
}

class DeleteChatSessionUseCase(
    private val repository: IChatRepository,
) {
    suspend operator fun invoke(sessionId: Long) =
        repository.deleteSession(sessionId)
}

class UpdateChatSessionUseCase(
    private val repository: IChatRepository,
) {
    suspend operator fun invoke(session: ChatSession) =
        repository.updateSession(session)
}

/** Alias used by ChatViewModel */
class UpdateSessionUseCase(
    private val repository: IChatRepository,
) {
    suspend operator fun invoke(session: ChatSession) =
        repository.updateSession(session)
}

// ── Chat messages ─────────────────────────────────────────────────────

class GetMessagesUseCase(
    private val repository: IChatRepository,
) {
    operator fun invoke(sessionId: Long): Flow<List<ChatMessage>> =
        repository.getMessages(sessionId)
}

class AddMessageUseCase(
    private val repository: IChatRepository,
) {
    suspend operator fun invoke(message: ChatMessage): Long =
        repository.addMessage(message)
}

class UpdateMessageUseCase(
    private val repository: IChatRepository,
) {
    suspend operator fun invoke(message: ChatMessage) =
        repository.updateMessage(message)
}

class SendMessageUseCase(
    private val repository: IChatRepository,
) {
    suspend operator fun invoke(message: ChatMessage): Long =
        repository.addMessage(message)
}

// ── Device ────────────────────────────────────────────────────────────

class GetDeviceProfileUseCase(
    private val repository: IDeviceRepository,
) {
    operator fun invoke(): Flow<DeviceProfile> =
        repository.getDeviceProfile()
}

// ── Auth / Settings ──────────────────────────────────────────────────

class LoginWithTokenUseCase(
    private val settingsRepository: ISettingsRepository,
    private val hfRepository: IHuggingFaceRepository,
) {
    suspend operator fun invoke(token: String): UserInfo {
        val user = hfRepository.getWhoami(token)
        try {
            settingsRepository.setHfToken(token)
        } catch (e: Exception) {
            // Log but don't fail — token verified, persist on next opportunity
        }
        return user
    }
}

class LogoutUseCase(
    private val repository: ISettingsRepository,
) {
    suspend operator fun invoke() =
        repository.clearHfToken()
}

class GetSettingsUseCase(
    private val repository: ISettingsRepository,
) {
    operator fun invoke(): Flow<AppSettings> =
        repository.getSettings()
}

class UpdateSettingsUseCase(
    private val repository: ISettingsRepository,
) {
    suspend operator fun invoke(settings: AppSettings) =
        repository.updateSettings(settings)
}
