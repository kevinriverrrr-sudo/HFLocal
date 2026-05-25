package com.hflocal.android.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.hflocal.shared.domain.model.MessageRole
import com.hflocal.shared.ui.theme.HFColors
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(nav: NavController, modelId: String) {
    val viewModel: ChatViewModel = koinViewModel {
        parametersOf(modelId)
    }
    val uiState by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Also scroll when streaming content updates (last message)
    LaunchedEffect(uiState.messages.lastOrNull()?.content) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Handle errors
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Could show a snackbar here
            viewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HFColors.Background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = modelId.split("/").lastOrNull() ?: "Chat",
                        color = HFColors.OnBackground
                    )
                    uiState.sessionTitle.let { title ->
                        if (title != "New Chat") {
                            Text(
                                text = title,
                                color = HFColors.OnSurfaceMuted,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = HFColors.OnBackground
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            ),
            actions = {
                IconButton(onClick = { viewModel.regenerate() }) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Regenerate",
                        tint = HFColors.OnSurface
                    )
                }
            }
        )

        HorizontalDivider(color = HFColors.Divider)

        // Messages area
        if (uiState.messages.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        tint = HFColors.OnSurfaceMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Start a conversation",
                        color = HFColors.OnSurfaceMuted,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Messages are processed locally on your device",
                        color = HFColors.OnSurfaceMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(uiState.messages) { message ->
                    val isUser = message.role == MessageRole.USER
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 18.dp,
                                topEnd = 18.dp,
                                bottomStart = if (isUser) 18.dp else 4.dp,
                                bottomEnd = if (isUser) 4.dp else 18.dp
                            ),
                            color = if (isUser) HFColors.UserMessageBg
                            else HFColors.AssistantMessageBg,
                            modifier = Modifier.widthIn(max = 320.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // Role label
                                Text(
                                    text = if (isUser) "You" else "Assistant",
                                    color = if (isUser) HFColors.Primary
                                    else HFColors.Secondary,
                                    fontSize = 11.sp,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Spacer(Modifier.height(4.dp))
                                // Message content
                                Text(
                                    text = message.content.ifEmpty {
                                        if (message.isStreaming) "..." else ""
                                    },
                                    color = HFColors.OnBackground,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    fontFamily = FontFamily.SansSerif
                                )
                                // Streaming cursor
                                if (message.isStreaming) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "▊",
                                        color = HFColors.Primary,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // AI typing indicator (when generating and last message is not streaming)
                if (uiState.isGenerating) {
                    val lastMsg = uiState.messages.lastOrNull()
                    if (lastMsg == null || !lastMsg.isStreaming) {
                        item {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = HFColors.Primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "AI thinking...",
                                    color = HFColors.OnSurfaceMuted,
                                    fontSize = 13.sp
                                )
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(onClick = { /* Attach file - not implemented */ }) {
                Icon(
                    Icons.Default.AttachFile,
                    contentDescription = "Attach",
                    tint = HFColors.OnSurfaceMuted
                )
            }

            OutlinedTextField(
                value = uiState.inputText,
                onValueChange = { viewModel.onInputChanged(it) },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Type message...",
                        color = HFColors.OnSurfaceMuted
                    )
                },
                shape = RoundedCornerShape(18.dp),
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

            if (uiState.isGenerating) {
                IconButton(onClick = { viewModel.cancelGeneration() }) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop generating",
                        tint = HFColors.Error
                    )
                }
            } else {
                IconButton(
                    onClick = { viewModel.sendMessage() },
                    enabled = uiState.inputText.isNotBlank()
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (uiState.inputText.isNotBlank())
                            HFColors.Primary
                        else HFColors.OnSurfaceMuted
                    )
                }
            }
        }
    }
}
