package com.hflocal.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hflocal.desktop.di.desktopModule
import com.hflocal.desktop.screens.*
import com.hflocal.shared.di.sharedModule
import com.hflocal.shared.domain.model.HFModel
import com.hflocal.shared.ui.theme.HFColors
import com.hflocal.shared.ui.theme.HFLocalTheme
import org.koin.core.context.startKoin
import java.awt.Dimension
import java.awt.Toolkit

fun main() {
    // Initialize Koin with shared and desktop modules
    startKoin {
        modules(sharedModule, desktopModule)
    }

    // Calculate centered window size: 1200x800 or 85% of screen, whichever is smaller
    val screenSize = Toolkit.getDefaultToolkit().screenSize
    val windowWidth = minOf(1200, (screenSize.width * 0.85).toInt()).coerceAtLeast(1024)
    val windowHeight = minOf(800, (screenSize.height * 0.85).toInt()).coerceAtLeast(768)

    androidx.compose.desktop.Window(
        title = "HF Local",
        size = Dimension(windowWidth, windowHeight),
        centered = true,
        icon = null // Will be set via nativeDistributions
    ) {
        HFLocalTheme {
            DesktopApp()
        }
    }
}

/**
 * Navigation tabs for the desktop app bottom navigation bar.
 */
private enum class DesktopTab(val label: String) {
    CATALOG("Catalog"),
    MODELS("Models"),
    CHAT("Chat"),
    SETTINGS("Settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopApp() {
    // Navigation state: which tab is selected
    var selectedTab by remember { mutableIntStateOf(DesktopTab.CATALOG.ordinal) }

    // Secondary navigation state: model detail dialog or chat screen
    var selectedModelForDetail by remember { mutableStateOf<HFModel?>(null) }
    var chatModelId by remember { mutableStateOf<String?>(null) }

    // Show chat as an overlay or replace content
    if (chatModelId != null) {
        // Full-screen chat mode
        DesktopChatScreen(
            modelId = chatModelId!!,
            onBackClick = { chatModelId = null }
        )

        // Show model detail dialog on top if triggered from chat
        if (selectedModelForDetail != null) {
            DesktopModelDetailDialog(
                model = selectedModelForDetail!!,
                onDismiss = { selectedModelForDetail = null }
            )
        }
    } else {
        // Main navigation with bottom bar
        Scaffold(
            containerColor = HFColors.Background,
            bottomBar = {
                NavigationBar(
                    containerColor = HFColors.Surface,
                    tonalElevation = 8.dp
                ) {
                    DesktopTab.entries.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = when (tab) {
                                        DesktopTab.CATALOG -> Icons.Default.Explore
                                        DesktopTab.MODELS -> Icons.Default.ModelTraining
                                        DesktopTab.CHAT -> Icons.Default.Chat
                                        DesktopTab.SETTINGS -> Icons.Default.Settings
                                    },
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label) },
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = HFColors.Primary,
                                selectedTextColor = HFColors.Primary,
                                unselectedIconColor = HFColors.OnSurfaceMuted,
                                unselectedTextColor = HFColors.OnSurfaceMuted,
                                indicatorColor = HFColors.Primary.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (selectedTab) {
                    DesktopTab.CATALOG.ordinal -> {
                        DesktopCatalogScreen(
                            onModelClick = { model ->
                                selectedModelForDetail = model
                            }
                        )
                    }
                    DesktopTab.MODELS.ordinal -> {
                        DesktopModelsScreen(
                            onChatClick = { modelId ->
                                chatModelId = modelId
                            }
                        )
                    }
                    DesktopTab.CHAT.ordinal -> {
                        // Chat tab: show placeholder or launch with a default model
                        ChatTabPlaceholder(
                            onModelSelect = { modelId ->
                                chatModelId = modelId
                            }
                        )
                    }
                    DesktopTab.SETTINGS.ordinal -> {
                        DesktopSettingsScreen()
                    }
                }
            }
        }

        // Model detail dialog overlay
        if (selectedModelForDetail != null) {
            DesktopModelDetailDialog(
                model = selectedModelForDetail!!,
                onDismiss = { selectedModelForDetail = null }
            )
        }
    }
}

/**
 * Placeholder shown when user clicks the Chat tab without a specific model selected.
 */
@Composable
private fun ChatTabPlaceholder(
    onModelSelect: (String) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = HFColors.Primary.copy(alpha = 0.1f)
            ) {
                Icon(
                    Icons.Default.ChatBubbleOutline,
                    contentDescription = null,
                    tint = HFColors.Primary,
                    modifier = Modifier
                        .size(96.dp)
                        .padding(20.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Chat with AI Models",
                style = MaterialTheme.typography.headlineSmall,
                color = HFColors.OnBackground
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Select a downloaded model from the Models tab to start chatting",
                style = MaterialTheme.typography.bodyLarge,
                color = HFColors.OnSurfaceMuted
            )

            Spacer(Modifier.height(24.dp))

            FilledButton(
                onClick = {
                    // Navigate to models tab by returning a special signal
                    // For simplicity, we'll just show a message
                },
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.filledButtonColors(
                    containerColor = HFColors.Primary
                )
            ) {
                Icon(
                    Icons.Default.ModelTraining,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Go to My Models")
            }
        }
    }
}
