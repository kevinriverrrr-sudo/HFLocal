package com.hflocal.desktop.screens

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.hflocal.shared.domain.model.AppSettings
import com.hflocal.shared.domain.model.ProxyConfig
import com.hflocal.shared.domain.model.ProxyType
import com.hflocal.shared.domain.repository.ISettingsRepository
import com.hflocal.shared.domain.usecase.GetSettingsUseCase
import com.hflocal.shared.domain.usecase.UpdateSettingsUseCase
import com.hflocal.shared.ui.theme.HFColors
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

@Composable
fun DesktopSettingsScreen() {
    var settings by remember { mutableStateOf(AppSettings()) }
    var hfToken by remember { mutableStateOf("") }
    var isLoaded by remember { mutableStateOf(false) }

    val getSettingsUseCase: GetSettingsUseCase by inject(GetSettingsUseCase::class.java)
    val updateSettingsUseCase: UpdateSettingsUseCase by inject(UpdateSettingsUseCase::class.java)
    val settingsRepository: ISettingsRepository by inject(ISettingsRepository::class.java)
    val coroutineScope = rememberCoroutineScope()

    // Load settings
    LaunchedEffect(Unit) {
        getSettingsUseCase().collect { appSettings ->
            settings = appSettings
            isLoaded = true
        }
        // Load the stored HF token
        hfToken = settingsRepository.getHfToken() ?: ""
    }

    if (!isLoaded) {
        Box(
            modifier = Modifier.fillMaxSize().background(HFColors.Background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = HFColors.Primary)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HFColors.Background)
    ) {
        // Header
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge,
            color = HFColors.OnBackground,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Account Section
            SettingsSection(title = "Account") {
                SettingsRow(
                    icon = Icons.Default.Person,
                    title = "HuggingFace Token",
                    subtitle = if (hfToken.isNotEmpty()) "****${hfToken.takeLast(4)}" else "Not set"
                )
                OutlinedTextField(
                    value = hfToken,
                    onValueChange = { hfToken = it },
                    label = { Text("HF Token") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HFColors.OnBackground,
                        unfocusedTextColor = HFColors.OnBackground,
                        focusedBorderColor = HFColors.Primary,
                        unfocusedBorderColor = HFColors.Divider,
                        focusedContainerColor = HFColors.SurfaceVariant,
                        unfocusedContainerColor = HFColors.SurfaceVariant
                    )
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            settingsRepository.setHfToken(hfToken)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text("Save Token")
                }
                Spacer(Modifier.height(8.dp))
            }

            // Catalog Section
            SettingsSection(title = "Catalog") {
                SettingsRow(
                    icon = Icons.Default.Sort,
                    title = "Default Sort",
                    subtitle = settings.defaultSort
                )
                SettingsSwitch(
                    icon = Icons.Default.Lock,
                    title = "Show Gated Models",
                    checked = settings.showGatedModels,
                    onCheckedChange = { newValue ->
                        coroutineScope.launch {
                            updateSettingsUseCase(settings.copy(showGatedModels = newValue))
                        }
                    }
                )
            }

            // Performance Section
            SettingsSection(title = "Performance") {
                SettingsRow(
                    icon = Icons.Default.Memory,
                    title = "CPU Threads",
                    subtitle = "${Runtime.getRuntime().availableProcessors()} cores detected"
                )
                SettingsRow(
                    icon = Icons.Default.RamenDining,
                    title = "Available Memory",
                    subtitle = formatMemory(Runtime.getRuntime().maxMemory())
                )
                SettingsSwitch(
                    icon = Icons.Default.Animation,
                    title = "Animations",
                    checked = settings.animationsEnabled,
                    onCheckedChange = { newValue ->
                        coroutineScope.launch {
                            updateSettingsUseCase(settings.copy(animationsEnabled = newValue))
                        }
                    }
                )
            }

            // Chat Section
            SettingsSection(title = "Chat") {
                SettingsSwitch(
                    icon = Icons.Default.History,
                    title = "Save Chat History",
                    checked = settings.saveChatHistory,
                    onCheckedChange = { newValue ->
                        coroutineScope.launch {
                            updateSettingsUseCase(settings.copy(saveChatHistory = newValue))
                        }
                    }
                )
                SettingsRow(
                    icon = Icons.Default.EditNote,
                    title = "System Prompt",
                    subtitle = if (settings.defaultSystemPrompt.isNotEmpty()) "Custom" else "Default"
                )
            }

            // Interface Section
            SettingsSection(title = "Interface") {
                SettingsRow(
                    icon = Icons.Default.DarkMode,
                    title = "Theme",
                    subtitle = settings.theme
                )
            }

            // Proxy Section
            SettingsSection(title = "Network") {
                SettingsSwitch(
                    icon = Icons.Default.VpnLock,
                    title = "Enable Proxy",
                    checked = settings.proxyConfig.enabled,
                    onCheckedChange = { newValue ->
                        val updatedProxy = settings.proxyConfig.copy(enabled = newValue)
                        coroutineScope.launch {
                            updateSettingsUseCase(settings.copy(proxyConfig = updatedProxy))
                        }
                    }
                )
                if (settings.proxyConfig.enabled) {
                    ProxySettingsFields(
                        proxyConfig = settings.proxyConfig,
                        onUpdate = { updatedProxy ->
                            coroutineScope.launch {
                                updateSettingsUseCase(settings.copy(proxyConfig = updatedProxy))
                            }
                        }
                    )
                }
            }

            // About Section
            SettingsSection(title = "About") {
                SettingsRow(
                    icon = Icons.Default.Info,
                    title = "Version",
                    subtitle = "1.0.0"
                )
                SettingsRow(
                    icon = Icons.Default.Code,
                    title = "Build",
                    subtitle = "Compose Multiplatform Desktop"
                )
                SettingsRow(
                    icon = Icons.Default.Computer,
                    title = "Platform",
                    subtitle = "${System.getProperty("os.name")} ${System.getProperty("os.version")}"
                )
                SettingsRow(
                    icon = Icons.Default.Architecture,
                    title = "Architecture",
                    subtitle = System.getProperty("os.arch")
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProxySettingsFields(
    proxyConfig: ProxyConfig,
    onUpdate: (ProxyConfig) -> Unit
) {
    var host by remember(proxyConfig) { mutableStateOf(proxyConfig.host) }
    var port by remember(proxyConfig) { mutableStateOf(proxyConfig.port.toString()) }

    Spacer(Modifier.height(4.dp))

    // Proxy type dropdown
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Router,
            contentDescription = null,
            tint = HFColors.OnSurface,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text("Type", color = HFColors.OnBackground, modifier = Modifier.width(100.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProxyType.entries.forEach { type ->
                FilterChip(
                    selected = proxyConfig.type == type,
                    onClick = { onUpdate(proxyConfig.copy(type = type)) },
                    label = { Text(type.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = HFColors.Primary.copy(alpha = 0.15f),
                        selectedLabelColor = HFColors.Primary,
                        containerColor = HFColors.SurfaceVariant,
                        labelColor = HFColors.OnSurfaceMuted
                    )
                )
            }
        }
    }

    // Host field
    OutlinedTextField(
        value = host,
        onValueChange = { host = it; onUpdate(proxyConfig.copy(host = it)) },
        label = { Text("Host") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = HFColors.OnBackground,
            unfocusedTextColor = HFColors.OnBackground,
            focusedBorderColor = HFColors.Primary,
            unfocusedBorderColor = HFColors.Divider,
            focusedContainerColor = HFColors.SurfaceVariant,
            unfocusedContainerColor = HFColors.SurfaceVariant
        )
    )

    // Port field
    OutlinedTextField(
        value = port,
        onValueChange = {
            port = it
            it.toIntOrNull()?.let { p -> onUpdate(proxyConfig.copy(port = p)) }
        },
        label = { Text("Port") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = HFColors.OnBackground,
            unfocusedTextColor = HFColors.OnBackground,
            focusedBorderColor = HFColors.Primary,
            unfocusedBorderColor = HFColors.Divider,
            focusedContainerColor = HFColors.SurfaceVariant,
            unfocusedContainerColor = HFColors.SurfaceVariant
        )
    )
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
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
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
            Text(title, color = HFColors.OnBackground)
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    color = HFColors.OnSurfaceMuted,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = HFColors.OnSurfaceMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsSwitch(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit = {}
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
        Text(
            title,
            color = HFColors.OnBackground,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = HFColors.Primary)
        )
    }
}

private fun formatMemory(bytes: Long): String {
    val gb = bytes / (1024.0 * 1024 * 1024)
    return if (gb >= 1.0) "%.1f GB".format(gb)
    else "%.0f MB".format(bytes / (1024.0 * 1024))
}
