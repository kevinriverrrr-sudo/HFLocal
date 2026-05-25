package com.hflocal.android.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hflocal.shared.domain.model.*
import com.hflocal.shared.domain.repository.IChatRepository
import com.hflocal.shared.domain.usecase.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isGenerating: Boolean = false,
    val modelId: String = "",
    val sessionTitle: String = "New Chat",
    val error: String? = null
)

class ChatViewModel(
    private val modelId: String,
    private val createSession: CreateChatSessionUseCase,
    private val addMessage: AddMessageUseCase,
    private val updateMessage: UpdateMessageUseCase,
    private val updateSession: UpdateSessionUseCase,
    private val chatRepo: IChatRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState(modelId = modelId))
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var currentSessionId: Long = 0
    // BUG-05 fix: guard against sending messages before session creation completes
    private val sessionIdReady = MutableStateFlow(false)
    private var generationJob: Job? = null
    // BUG-11 fix: remember original createdAt for session title updates
    private var originalCreatedAt: Long = 0L

    init {
        initSession()
    }

    private fun initSession() {
        viewModelScope.launch {
            try {
                val sessionId = createSession(
                    ChatSession(
                        modelId = modelId,
                        title = modelId.split("/").lastOrNull() ?: "New Chat",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
                currentSessionId = sessionId
                originalCreatedAt = System.currentTimeMillis()
                sessionIdReady.value = true

                // BUG-26 fix: separate Flow collection into its own try block so that
                // a database error during collection doesn't leave the chat permanently frozen
                try {
                    chatRepo.getMessages(sessionId).collect { messages ->
                        _state.update { it.copy(messages = messages) }
                    }
                } catch (e: Exception) {
                    _state.update { it.copy(error = "Message collection failed: ${e.message}") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to create session: ${e.message}") }
            }
        }
    }

    fun onInputChanged(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _state.value.inputText.trim()
        if (text.isBlank() || _state.value.isGenerating) return

        // BUG-05 fix: don't send messages until the session is ready
        if (!sessionIdReady.value) return

        viewModelScope.launch {
            try {
                // Add user message
                val userMessage = ChatMessage(
                    sessionId = currentSessionId,
                    role = MessageRole.USER,
                    content = text,
                    timestamp = System.currentTimeMillis()
                )
                addMessage(userMessage)
                _state.update { it.copy(inputText = "") }

                // Update session title from first user message
                val currentMessages = _state.value.messages
                if (currentMessages.count { it.role == MessageRole.USER } == 1) {
                    val title = text.take(40) + if (text.length > 40) "..." else ""
                    // BUG-11 fix: preserve original createdAt instead of overwriting
                    updateSession(
                        ChatSession(
                            id = currentSessionId,
                            modelId = modelId,
                            title = title,
                            createdAt = originalCreatedAt,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    _state.update { it.copy(sessionTitle = title) }
                }

                // Start AI response
                generateResponse(text)
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to send message: ${e.message}") }
            }
        }
    }

    private fun generateResponse(userText: String) {
        generationJob = viewModelScope.launch {
            try {
                _state.update { it.copy(isGenerating = true) }

                // Add streaming placeholder for assistant message
                val assistantMessage = ChatMessage(
                    sessionId = currentSessionId,
                    role = MessageRole.ASSISTANT,
                    content = "",
                    timestamp = System.currentTimeMillis(),
                    isStreaming = true
                )
                val msgId = addMessage(assistantMessage)

                // Simulate delay before starting to stream
                kotlinx.coroutines.delay(1000L)

                // Generate mock response tokens
                val fullResponse = generateMockResponse(userText)
                val words = fullResponse.split(" ")
                var currentContent = ""

                words.forEachIndexed { index, word ->
                    if (!coroutineContext.isActive) return@forEach

                    currentContent = if (currentContent.isEmpty()) word
                    else "$currentContent $word"

                    updateMessage(
                        ChatMessage(
                            id = msgId,
                            sessionId = currentSessionId,
                            role = MessageRole.ASSISTANT,
                            content = currentContent,
                            timestamp = System.currentTimeMillis(),
                            isStreaming = index < words.lastIndex
                        )
                    )

                    // Variable delay to simulate realistic streaming
                    val delay = Random.nextLong(30, 120)
                    kotlinx.coroutines.delay(delay)
                }

                // Mark streaming as complete
                updateMessage(
                    ChatMessage(
                        id = msgId,
                        sessionId = currentSessionId,
                        role = MessageRole.ASSISTANT,
                        content = currentContent,
                        timestamp = System.currentTimeMillis(),
                        isStreaming = false
                    )
                )

                _state.update { it.copy(isGenerating = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isGenerating = false,
                        error = "Generation failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun cancelGeneration() {
        generationJob?.cancel()
        generationJob = null
        _state.update { it.copy(isGenerating = false) }
    }

    fun regenerate() {
        if (_state.value.isGenerating) return

        val messages = _state.value.messages
        val lastAssistantIndex = messages.indexOfLast { it.role == MessageRole.ASSISTANT }

        if (lastAssistantIndex >= 0) {
            val lastAssistant = messages[lastAssistantIndex]

            viewModelScope.launch {
                try {
                    // BUG-10 fix: delete the old assistant message before regenerating
                    // to prevent duplicate assistant messages appearing in the chat.
                    // Instead of overwriting content (which leaves the old row), we
                    // clear the content and mark as non-streaming, then generate a
                    // fresh response that replaces it.
                    chatRepo.updateMessage(
                        lastAssistant.copy(content = "", isStreaming = false)
                    )

                    // Find the last user message for context
                    val lastUserMessage = messages
                        .filter { it.role == MessageRole.USER }
                        .lastOrNull()

                    val userText = lastUserMessage?.content ?: "Hello"
                    // Reuse the existing message ID instead of creating a new one
                    generateResponseWithId(userText, lastAssistant.id)
                } catch (e: Exception) {
                    _state.update {
                        it.copy(error = "Regeneration failed: ${e.message}")
                    }
                }
            }
        }
    }

    /**
     * Like [generateResponse] but streams into an existing message ID
     * instead of creating a new assistant placeholder.
     */
    private fun generateResponseWithId(userText: String, existingMsgId: Long) {
        generationJob = viewModelScope.launch {
            try {
                _state.update { it.copy(isGenerating = true) }

                // Simulate delay before starting to stream
                kotlinx.coroutines.delay(1000L)

                val fullResponse = generateMockResponse(userText)
                val words = fullResponse.split(" ")
                var currentContent = ""

                words.forEachIndexed { index, word ->
                    if (!coroutineContext.isActive) return@forEach

                    currentContent = if (currentContent.isEmpty()) word
                    else "$currentContent $word"

                    updateMessage(
                        ChatMessage(
                            id = existingMsgId,
                            sessionId = currentSessionId,
                            role = MessageRole.ASSISTANT,
                            content = currentContent,
                            timestamp = System.currentTimeMillis(),
                            isStreaming = index < words.lastIndex
                        )
                    )

                    val delay = Random.nextLong(30, 120)
                    kotlinx.coroutines.delay(delay)
                }

                // Mark streaming as complete
                updateMessage(
                    ChatMessage(
                        id = existingMsgId,
                        sessionId = currentSessionId,
                        role = MessageRole.ASSISTANT,
                        content = currentContent,
                        timestamp = System.currentTimeMillis(),
                        isStreaming = false
                    )
                )

                _state.update { it.copy(isGenerating = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isGenerating = false,
                        error = "Generation failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun generateMockResponse(userText: String): String {
        val responses = listOf(
            "That's a great question! Let me think about this carefully.\n\n" +
                "Based on my understanding, I can provide a thoughtful analysis of what you're asking. " +
                "This is a simulated response since the actual model inference engine is not yet connected.\n\n" +
                "In a real scenario, this would be the output from a quantized GGUF model running " +
                "locally on your device using llama.cpp.",

            "I understand what you're looking for. Here's my perspective:\n\n" +
                "The topic you've raised touches on several important areas. While I'm currently " +
                "running in simulation mode, the actual local inference would process this through " +
                "multiple attention layers and transformer blocks.\n\n" +
                "This mock response demonstrates the streaming UI capability that will be used " +
                "with real model inference.",

            "Interesting question! Let me break this down:\n\n" +
                "1. First, consider the context and background of what you're asking\n" +
                "2. Then, look at it from multiple angles\n" +
                "3. Finally, synthesize the key insights\n\n" +
                "This is a placeholder response that simulates how a real GGUF model would " +
                "stream tokens to the UI. The actual model response would be generated locally " +
                "on your device without any API calls.",

            "Here's what I can tell you about that:\n\n" +
                "This is a demonstration of the chat interface with mock responses. " +
                "When a real model is loaded and running, you would see actual AI-generated " +
                "content streaming token by token here.\n\n" +
                "The model would run entirely on-device using quantized weights (Q4_K_M, Q5_K_M, etc.) " +
                "for efficient inference on mobile hardware."
        )
        return responses[Random.nextInt(responses.size)]
    }
}
