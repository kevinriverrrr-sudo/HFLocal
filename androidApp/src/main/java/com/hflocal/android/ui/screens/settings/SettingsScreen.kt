package com.hflocal.android.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hflocal.shared.domain.model.PerformanceTier
import com.hflocal.shared.ui.navigation.Screen
import com.hflocal.shared.ui.theme.HFColors
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(nav: NavController) {
    val viewModel: SettingsViewModel = koinViewModel()
    val uiState by viewModel.state.collectAsState()

    // System prompt dialog state
    var showPromptDialog by remember { mutableStateOf(false) }
    var promptText by remember { mutableStateOf("") }

    // Sort dialog state
    var showSortDialog by remember { mutableStateOf(false) }

    // Logout confirmation
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Error handling
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            viewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HFColors.Background)
    ) {
        TopAppBar(
            title = {
                Text("Settings", color = HFColors.OnBackground)
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical =8.dp)
        ) {
            // Account Section
            SettingsSection(title = "Account") {
                if (uiState.isLoggedIn) {
                    SettingsRow(
                        icon = Icons.Default.Person,
                        title = "HuggingFace Account",
                        subtitle = "Logged in"
                    )
                    SettingsRow(
                        icon = Icons.Default.Logout,
                        title = "Logout",
                        subtitle = "",
                        onClick = { showLogoutDialog = true }
                    )
                } else {
                    SettingsRow(
                        icon = Icons.Default.Login,
                        title = "Login to HuggingFace",
                        subtitle = "Not logged in",
                        onClick = { nav.navigate(Screen.Auth.route) }
                    )
                }
            }

            // About Device Section
            SettingsSection(title = "Device Info") {
                val profile = uiState.deviceProfile
                val tierName = when (profile.tier) {
                    PerformanceTier.TIER_1_HIGH_END -> "High End"
                    PerformanceTier.TIER_2_MID_RANGE -> "Mid Range"
                    PerformanceTier.TIER_3_BUDGET -> "Budget"
                    PerformanceTier.TIER_4_UNSUPPORTED -> "Unsupported"
                }
                SettingsRow(
                    icon = Icons.Default.Memory,
                    title = "Performance Tier",
                    subtitle = tierName
                )
                SettingsRow(
                    icon = Icons.Default.Star,
                    title = "Max Model Size",
                    subtitle = formatFileSize(profile.tier.maxModelSizeBytes)
                )
                SettingsRow(
                    icon = Icons.Default.DeveloperBoard,
                    title = "CPU",
                    subtitle = "${profile.cpuArch} (${profile.cpuCores} cores)"
                )
                SettingsRow(
                    icon = Icons.Default.PhoneAndroid,
                    title = "Android",
                    subtitle = "${profile.androidVersion} (SDK ${profile.androidSdkVersion})"
                )
                SettingsRow(
                    icon = Icons.Default.Speed,
                    title = "Vulkan",
                    subtitle = if (profile.supportsVulkan) "Supported" else "Not supported"
                )
            }

            // Catalog Section
            SettingsSection(title = "Catalog") {
                SettingsRow(
                    icon = Icons.Default.Sort,
                    title = "Default Sort",
                    subtitle = uiState.settings.defaultSort,
                    onClick = { showSortDialog = true }
                )
                SettingsSwitchRow(
                    icon = Icons.Default.Lock,
                    title = "Show Gated Models",
                    checked = uiState.settings.showGatedModels,
                    onCheckedChange = { viewModel.toggleGatedModels(it) }
                )
            }

            // Network Section
            SettingsSection(title = "Network") {
                SettingsSwitchRow(
                    icon = Icons.Default.Wifi,
                    title = "WiFi Only Downloads",
                    subtitle = "Only download on WiFi",
                    checked = uiState.settings.downloadOnlyOnWifi,
                    onCheckedChange = { viewModel.toggleWifiOnly(it) }
                )
            }

            // Chat Section
            SettingsSection(title = "Chat") {
                SettingsRow(
                    icon = Icons.Default.EditNote,
                    title = "System Prompt",
                    subtitle = if (uiState.settings.defaultSystemPrompt.isEmpty())
                        "Not set"
                    else
                        uiState.settings.defaultSystemPrompt.take(30) + "...",
                    onClick = {
                        promptText = uiState.settings.defaultSystemPrompt
                        showPromptDialog = true
                    }
                )
                SettingsSwitchRow(
                    icon = Icons.Default.History,
                    title = "Save Chat History",
                    checked = uiState.settings.saveChatHistory,
                    onCheckedChange = { viewModel.toggleChatHistory(it) }
                )
            }

            // Interface Section
            SettingsSection(title = "Interface") {
                SettingsRow(
                    icon = Icons.Default.DarkMode,
                    title = "Theme",
                    subtitle = when (uiState.settings.theme) {
                        "dark" -> "Dark"
                        "light" -> "Light"
                        "system" -> "System"
                        else -> "Dark"
                    },
                    onClick = {
                        val next = when (uiState.settings.theme) {
                            "dark" -> "light"
                            "light" -> "system"
                            else -> "dark"
                        }
                        viewModel.updateTheme(next)
                    }
                )
                SettingsSwitchRow(
                    icon = Icons.Default.Animation,
                    title = "Animations",
                    checked = uiState.settings.animationsEnabled,
                    onCheckedChange = { viewModel.toggleAnimations(it) }
                )
            }

            // Navigation links
            Spacer(Modifier.height(8.dp))
            SettingsRow(
                icon = Icons.Default.PhoneAndroid,
                title = "Device Info",
                subtitle = "Full device details",
                onClick = { nav.navigate(Screen.DeviceInfo.route) }
            )
            SettingsRow(
                icon = Icons.Default.Info,
                title = "About",
                subtitle = "HFLocal v1.0.0"
            )

            Spacer(Modifier.height(24.dp))
        }
    }

    // System Prompt Dialog
    if (showPromptDialog) {
        AlertDialog(
            onDismissRequest = { showPromptDialog = false },
            title = {
                Text("System Prompt", color = HFColors.OnBackground)
            },
            text = {
                OutlinedTextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    placeholder = {
                        Text(
                            "Enter a custom system prompt...",
                            color = HFColors.OnSurfaceMuted
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HFColors.OnBackground,
                        unfocusedTextColor = HFColors.OnBackground,
                        focusedBorderColor = HFColors.Primary,
                        unfocusedBorderColor = HFColors.Divider,
                        focusedContainerColor = HFColors.SurfaceVariant,
                        unfocusedContainerColor = HFColors.SurfaceVariant
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateSystemPrompt(promptText)
                    showPromptDialog = false
                }) {
                    Text("Save", color = HFColors.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPromptDialog = false }) {
                    Text("Cancel", color = HFColors.OnSurfaceMuted)
                }
            },
            containerColor = HFColors.SurfaceVariant
        )
    }

    // Sort Dialog
    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = {
                Text("Default Sort", color = HFColors.OnBackground)
            },
            text = {
                Column {
                    listOf(
                        "downloads" to "Most Downloads",
                        "likes" to "Most Likes",
                        "lastModified" to "Recently Updated"
                    ).forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateDefaultSort(value)
                                    showSortDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.settings.defaultSort == value,
                                onClick = {
                                    viewModel.updateDefaultSort(value)
                                    showSortDialog = false
                                }
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(label, color = HFColors.OnBackground)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSortDialog = false }) {
                    Text("Cancel", color = HFColors.OnSurfaceMuted)
                }
            },
            containerColor = HFColors.SurfaceVariant
        )
    }

    // Logout Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text("Logout", color = HFColors.OnBackground)
            },
            text = {
                Text(
                    "Are you sure you want to logout from HuggingFace?",
                    color = HFColors.OnSurface
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.performLogout {
                        showLogoutDialog = false
                        nav.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }) {
                    Text("Logout", color = HFColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = HFColors.OnSurfaceMuted)
                }
            },
            containerColor = HFColors.SurfaceVariant
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Text(
        text = title,
        color = HFColors.Secondary,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = HFColors.Surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(content = content)
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = HFColors.OnSurface,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = HFColors.OnBackground
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    color = HFColors.OnSurfaceMuted,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
        if (onClick != {}) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = HFColors.OnSurfaceMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String = "",
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) HFColors.OnSurface else HFColors.OnSurfaceMuted,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = HFColors.OnBackground
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    color = HFColors.OnSurfaceMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = HFColors.Primary
            )
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    val gb = bytes / (1024.0 * 1024 * 1024)
    return if (gb >= 1) {
        "%.1f GB".format(gb)
    } else {
        val mb = bytes / (1024.0 * 1024)
        "%.0f MB".format(mb)
    }
}
