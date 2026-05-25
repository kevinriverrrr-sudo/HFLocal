package com.hflocal.desktop.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hflocal.shared.domain.model.DownloadedModel
import com.hflocal.shared.domain.usecase.DeleteModelUseCase
import com.hflocal.shared.domain.usecase.GetDownloadedModelsUseCase
import com.hflocal.shared.ui.theme.HFColors
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopModelsScreen(
    onChatClick: (String) -> Unit
) {
    var models by remember { mutableStateOf<List<DownloadedModel>>(emptyList()) }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }

    val getModelsUseCase: GetDownloadedModelsUseCase by inject(GetDownloadedModelsUseCase::class.java)
    val deleteModelUseCase: DeleteModelUseCase by inject(DeleteModelUseCase::class.java)
    val coroutineScope = rememberCoroutineScope()

    // Load models
    LaunchedEffect(Unit) {
        getModelsUseCase().collect { models = it }
    }

    // Calculate storage usage
    val totalSizeBytes = models.filter { it.isDownloaded }.sumOf { it.fileSizeBytes.toDouble() }
    val totalSizeFormatted = formatFileSize(totalSizeBytes.toLong())

    // Delete confirmation dialog
    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Model", color = HFColors.OnBackground) },
            text = {
                Text(
                    "Are you sure you want to delete this model? This will free up disk space.",
                    color = HFColors.OnSurface
                )
            },
            confirmButton = {
                FilledButton(
                    onClick = {
                        val modelId = showDeleteDialog
                        showDeleteDialog = null
                        if (modelId != null) {
                            coroutineScope.launch {
                                try {
                                    deleteModelUseCase(modelId)
                                } catch (_: Exception) {
                                    // Refresh after delete
                                }
                                // Reload models
                                getModelsUseCase().collect { models = it }
                            }
                        }
                    },
                    colors = ButtonDefaults.filledButtonColors(containerColor = HFColors.Error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel", color = HFColors.OnSurface)
                }
            },
            containerColor = HFColors.Surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HFColors.Background)
    ) {
        // Header
        Text(
            text = "My Models",
            style = MaterialTheme.typography.headlineLarge,
            color = HFColors.OnBackground,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )

        // Storage usage bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            colors = CardDefaults.cardColors(containerColor = HFColors.Surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Storage,
                    contentDescription = null,
                    tint = HFColors.Secondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Models: $totalSizeFormatted",
                        color = HFColors.OnBackground,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (totalSizeBytes / (64.0 * 1024 * 1024 * 1024)).toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = HFColors.Primary,
                        trackColor = HFColors.Divider
                    )
                    Text(
                        text = "${models.size} model${if (models.size != 1) "s" else ""} downloaded",
                        color = HFColors.OnSurfaceMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (models.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ModelTraining,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = HFColors.OnSurfaceMuted
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No models downloaded yet",
                        color = HFColors.OnSurfaceMuted,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "Browse the catalog to find models",
                        color = HFColors.OnSurfaceMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(models) { model ->
                    DownloadedModelCard(
                        model = model,
                        onPlayClick = { onChatClick(model.modelId) },
                        onDeleteClick = { showDeleteDialog = model.modelId }
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadedModelCard(
    model: DownloadedModel,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = HFColors.Surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Model icon
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = HFColors.Primary.copy(alpha = 0.1f)
            ) {
                Icon(
                    Icons.Default.Psychology,
                    contentDescription = null,
                    tint = HFColors.Primary,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(8.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            // Model info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (model.modelId.isNotEmpty()) "${model.author}/${model.modelId}" else "Unknown Model",
                    color = HFColors.OnBackground,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = buildString {
                        if (model.quantization.isNotEmpty()) append("${model.quantization} | ")
                        append(formatFileSize(model.fileSizeBytes))
                        if (model.fileName.isNotEmpty()) append(" | ${model.fileName}")
                    },
                    color = HFColors.OnSurfaceMuted,
                    style = MaterialTheme.typography.labelMedium
                )
                if (model.downloadProgress > 0f && model.downloadProgress < 1f) {
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { model.downloadProgress },
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = HFColors.Secondary,
                        trackColor = HFColors.Divider
                    )
                    Text(
                        text = "${(model.downloadProgress * 100).toInt()}%",
                        color = HFColors.Secondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Action buttons
            IconButton(onClick = onPlayClick) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Open Chat",
                    tint = HFColors.Success,
                    modifier = Modifier.size(28.dp)
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = HFColors.Error,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    val gb = bytes / (1024.0 * 1024 * 1024)
    val mb = bytes / (1024.0 * 1024)
    return when {
        gb >= 1.0 -> "%.1f GB".format(gb)
        mb >= 1.0 -> "%.0f MB".format(mb)
        else -> "$bytes B"
    }
}
