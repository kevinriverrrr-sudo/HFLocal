package com.hflocal.desktop.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hflocal.shared.domain.model.ChatMessage
import com.hflocal.shared.domain.model.MessageRole
import com.hflocal.shared.ui.theme.HFColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopChatScreen(
    modelId: String,
    onBackClick: () -> Unit
) {
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Mock AI responses for desktop (no llama.cpp integration yet)
    val mockResponses = listOf(
        "Hello! I'm a simulated AI assistant running on your desktop. " +
            "In a full implementation, this would be powered by a local GGUF model loaded via llama.cpp.",
        "That's an interesting question! Since we're running locally, " +
            "your conversations stay private on your machine. No data is sent to external servers.",
        "I'm currently running in demo mode. To get real AI responses, " +
            "you would need to download a GGUF model and set up the inference engine.",
        "Great point! Desktop inference allows you to run models with " +
            "full privacy and no API costs. You can run models up to your system's RAM capacity.",
        "Local AI models are perfect for tasks like text generation, " +
            "summarization, and code completion. Performance depends on your CPU and available RAM."
    )
    var responseIndex by remember { mutableIntStateOf(0) }

    // Auto-scroll when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    // Simulate AI typing
    LaunchedEffect(isGenerating) {
        if (isGenerating) {
            delay(1500)
            val response = mockResponses[responseIndex % mockResponses.size]
            responseIndex++
            messages = messages + ChatMessage(
                role = MessageRole.ASSISTANT,
                content = response,
                timestamp = System.currentTimeMillis()
            )
            isGenerating = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HFColors.Background)
    ) {
        // Top bar with model name and back button
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = modelId.split("/").lastOrNull() ?: "Chat",
                        color = HFColors.OnBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Desktop Mode (Simulated)",
                        color = HFColors.OnSurfaceMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = HFColors.OnBackground
                    )
                }
            },
            actions = {
                IconButton(onClick = {
                    // New chat
                    messages = emptyList()
                    inputText = ""
                }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "New Chat",
                        tint = HFColors.OnSurface
                    )
                }
                IconButton(onClick = { /* Settings */ }) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = "Settings",
                        tint = HFColors.OnSurface
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = HFColors.Surface.copy(alpha = 0.5f)
            )
        )

        HorizontalDivider(color = HFColors.Divider)

        // Message list or empty state
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = HFColors.Primary.copy(alpha = 0.1f)
                    ) {
                        Icon(
                            Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            tint = HFColors.Primary,
                            modifier = Modifier
                                .size(80.dp)
                                .padding(16.dp)
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Start a conversation",
                        color = HFColors.OnBackground,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Messages are processed locally on your device",
                        color = HFColors.OnSurfaceMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(16.dp))
                    // Quick action chips
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SuggestionChip(
                            onClick = {
                                inputText = "What can you help me with?"
                            },
                            label = { Text("What can you help me with?") },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = HFColors.SurfaceVariant,
                                labelColor = HFColors.OnSurface
                            )
                        )
                        SuggestionChip(
                            onClick = {
                                inputText = "Tell me about local AI models"
                            },
                            label = { Text("Tell me about local AI") },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = HFColors.SurfaceVariant,
                                labelColor = HFColors.OnSurface
                            )
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages) { message ->
                    val isUser = message.role == MessageRole.USER
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            if (!isUser) {
                                // Assistant avatar
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = HFColors.Primary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Psychology,
                                        contentDescription = null,
                                        tint = HFColors.Primary,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .padding(6.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                            }
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 18.dp,
                                    topEnd = 18.dp,
                                    bottomStart = if (isUser) 18.dp else 4.dp,
                                    bottomEnd = if (isUser) 4.dp else 18.dp
                                ),
                                color = if (isUser) HFColors.UserMessageBg else HFColors.AssistantMessageBg,
                                modifier = Modifier.widthIn(max = 480.dp)
                            ) {
                                Text(
                                    text = message.content,
                                    modifier = Modifier.padding(12.dp),
                                    color = HFColors.OnBackground,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            }
                            if (isUser) {
                                Spacer(Modifier.width(8.dp))
                                // User avatar
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = HFColors.Secondary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = HFColors.Secondary,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .padding(6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Typing indicator
                if (isGenerating) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = HFColors.Primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = HFColors.Primary,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(6.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 18.dp,
                                    topEnd = 18.dp,
                                    bottomStart = 4.dp,
                                    bottomEnd = 18.dp
                                ),
                                color = HFColors.AssistantMessageBg
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = HFColors.Primary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "AI is thinking...",
                                        color = HFColors.OnSurfaceMuted,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = HFColors.Divider)

        // Input area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HFColors.Surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Attach file button (placeholder)
            IconButton(onClick = { /* TODO: file attachment */ }) {
                Icon(
                    Icons.Default.AttachFile,
                    contentDescription = "Attach file",
                    tint = HFColors.OnSurfaceMuted
                )
            }

            // Message input
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Type a message...",
                        color = HFColors.OnSurfaceMuted
                    )
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = HFColors.OnBackground,
                    unfocusedTextColor = HFColors.OnBackground,
                    focusedBorderColor = HFColors.Primary,
                    unfocusedBorderColor = HFColors.Divider,
                    focusedContainerColor = HFColors.SurfaceVariant,
                    unfocusedContainerColor = HFColors.SurfaceVariant
                ),
                maxLines = 4
            )

            Spacer(Modifier.width(8.dp))

            // Send or Stop button
            if (isGenerating) {
                IconButton(onClick = { isGenerating = false }) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop generation",
                        tint = HFColors.Error,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            messages = messages + ChatMessage(
                                role = MessageRole.USER,
                                content = inputText.trim(),
                                timestamp = System.currentTimeMillis()
                            )
                            inputText = ""
                            isGenerating = true
                        }
                    },
                    enabled = inputText.isNotBlank()
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send message",
                        tint = if (inputText.isNotBlank()) HFColors.Primary else HFColors.OnSurfaceMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
