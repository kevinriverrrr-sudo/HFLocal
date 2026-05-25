package com.hflocal.shared.domain.model
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
@Serializable data class HFModel(
    val id: String = "",
    val modelId: String = "",
    val author: String = "",
    val sha: String = "",
    val lastModified: String = "",
    @SerialName("private") val isPrivate: Boolean = false,
    val gated: String = "false",
    val downloads: Int = 0,
    val likes: Int = 0,
    val tags: List<String> = emptyList(),
    @SerialName("pipeline_tag") val pipelineTag: String = "",
    val siblings: List<ModelFile> = emptyList()
)
@Serializable data class ModelFile(val rfilename: String = "", val size: Long? = null, val blobId: String? = null)
@Serializable data class UserInfo(val name: String = "", val avatarUrl: String? = null, val fullname: String? = null)
enum class PerformanceTier { TIER_1_HIGH_END, TIER_2_MID_RANGE, TIER_3_BUDGET, TIER_4_UNSUPPORTED; val maxModelSizeBytes: Long get() = when(this) { TIER_1_HIGH_END -> 6L*1024*1024*1024; TIER_2_MID_RANGE -> 3L*1024*1024*1024; TIER_3_BUDGET -> 1536L*1024*1024; TIER_4_UNSUPPORTED -> 0 } }
data class DeviceProfile(val socModel: String = "", val cpuCores: Int = 0, val cpuArch: String = "", val totalRamBytes: Long = 0, val availableRamBytes: Long = 0, val freeStorageBytes: Long = 0, val gpuRenderer: String = "", val supportsVulkan: Boolean = false, val androidSdkVersion: Int = 0, val androidVersion: String = "", val tier: PerformanceTier = PerformanceTier.TIER_4_UNSUPPORTED)
data class DownloadedModel(val id: Long = 0, val modelId: String = "", val author: String = "", val fileName: String = "", val filePath: String = "", val fileSizeBytes: Long = 0, val quantization: String = "", val downloadDate: Long = 0, val isDownloaded: Boolean = false, val downloadProgress: Float = 0f)
@Serializable data class ChatSession(val id: Long = 0, val modelId: String = "", val title: String = "", val createdAt: Long = 0, val updatedAt: Long = 0, val systemPrompt: String = "", val messageCount: Int = 0)
@Serializable data class ChatMessage(val id: Long = 0, val sessionId: Long = 0, val role: MessageRole = MessageRole.USER, val content: String = "", val timestamp: Long = 0, val imagePath: String? = null, val isStreaming: Boolean = false)
enum class MessageRole { SYSTEM, USER, ASSISTANT }
data class InferenceConfig(val temperature: Float = 0.7f, val topP: Float = 0.9f, val topK: Int = 40, val maxNewTokens: Int = 512, val contextLength: Int = 2048, val repeatPenalty: Float = 1.1f, val seed: Long = -1L)
@Serializable data class ProxyConfig(val enabled: Boolean = false, val type: ProxyType = ProxyType.HTTP, val host: String = "", val port: Int = 8080, val username: String = "", val password: String = "")
enum class ProxyType { HTTP, HTTPS, SOCKS5 }
@Serializable data class AppSettings(val defaultSort: String = "downloads", val showGatedModels: Boolean = true, val downloadOnlyOnWifi: Boolean = true, val theme: String = "dark", val animationsEnabled: Boolean = true, val saveChatHistory: Boolean = true, val defaultSystemPrompt: String = "", val proxyConfig: ProxyConfig = ProxyConfig())
data class SearchQuery(val query: String = "", val pipelineTag: String = "", val author: String = "", val sort: String = "downloads", val direction: String = "desc", val limit: Int = 20, val offset: Int = 0)
