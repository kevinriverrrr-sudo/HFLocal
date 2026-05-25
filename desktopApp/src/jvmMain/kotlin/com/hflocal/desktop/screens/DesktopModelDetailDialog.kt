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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hflocal.shared.domain.model.HFModel
import com.hflocal.shared.domain.model.ModelFile
import com.hflocal.shared.ui.theme.HFColors

@Composable
fun DesktopModelDetailDialog(
    model: HFModel,
    onDismiss: () -> Unit,
    onDownloadClick: (String, String) -> Unit = { _, _ -> }
) {
    val displayName = if (model.modelId.isNotEmpty()) "${model.author}/${model.modelId}" else model.id

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .widthIn(max = 600.dp)
            .heightIn(max = 700.dp),
        containerColor = HFColors.Surface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Model Details",
                        style = MaterialTheme.typography.headlineSmall,
                        color = HFColors.OnBackground
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = HFColors.OnSurfaceMuted
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Model name
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleLarge,
                    color = HFColors.OnBackground,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(12.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Downloads
                    StatChip(
                        icon = Icons.Default.Download,
                        label = "Downloads",
                        value = formatCount(model.downloads)
                    )
                    // Likes
                    StatChip(
                        icon = Icons.Default.Star,
                        label = "Likes",
                        value = formatCount(model.likes)
                    )
                    // Pipeline tag
                    if (model.pipelineTag.isNotEmpty()) {
                        StatChip(
                            icon = Icons.Default.Category,
                            label = "Pipeline",
                            value = model.pipelineTag
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Tags
                if (model.tags.isNotEmpty()) {
                    Text(
                        text = "Tags",
                        color = HFColors.OnSurfaceMuted,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        model.tags.take(5).forEach { tag ->
                            SuggestionChip(
                                onClick = { },
                                label = { Text(tag, maxLines = 1) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = HFColors.SurfaceVariant,
                                    labelColor = HFColors.OnSurface
                                )
                            )
                        }
                        if (model.tags.size > 5) {
                            Text(
                                "+${model.tags.size - 5} more",
                                color = HFColors.OnSurfaceMuted,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Gated warning
                if (model.gated) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = HFColors.Warning.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = HFColors.Warning,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "This model requires access approval on HuggingFace Hub",
                                color = HFColors.Warning,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Files header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Files",
                        color = HFColors.OnBackground,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${model.siblings.size} files",
                        color = HFColors.OnSurfaceMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Files list
                if (model.siblings.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No file information available",
                            color = HFColors.OnSurfaceMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 250.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(model.siblings) { file ->
                            FileRow(
                                file = file,
                                modelId = model.id,
                                onDownloadClick = onDownloadClick
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Open on HuggingFace Hub
                    OutlinedButton(
                        onClick = { /* Open browser */ },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = null,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = HFColors.OnSurface
                        )
                    ) {
                        Icon(
                            Icons.Default.OpenInBrowser,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Open on HF Hub")
                    }

                    // Download GGUF button
                    val ggufFile = model.siblings.firstOrNull {
                        it.rfilename.endsWith(".gguf", ignoreCase = true)
                    }
                    if (ggufFile != null) {
                        FilledButton(
                            onClick = {
                                onDownloadClick(model.id, ggufFile.rfilename)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledButtonColors(
                                containerColor = HFColors.Primary
                            )
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Download GGUF")
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
private fun StatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = HFColors.SurfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = HFColors.Primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Column {
                Text(
                    value,
                    color = HFColors.OnBackground,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    label,
                    color = HFColors.OnSurfaceMuted,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun FileRow(
    file: ModelFile,
    modelId: String,
    onDownloadClick: (String, String) -> Unit
) {
    val isGGUF = file.rfilename.endsWith(".gguf", ignoreCase = true)
    val fileName = file.rfilename.ifEmpty { "Unknown file" }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isGGUF) HFColors.Primary.copy(alpha = 0.06f) else HFColors.SurfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isGGUF) Icons.Default.Description else Icons.Default.InsertDriveFile,
                contentDescription = null,
                tint = if (isGGUF) HFColors.Primary else HFColors.OnSurfaceMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = fileName,
                color = HFColors.OnBackground,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            if (file.size != null) {
                Text(
                    text = formatFileSize(file.size),
                    color = HFColors.OnSurfaceMuted,
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(Modifier.width(8.dp))
            }
            if (isGGUF) {
                IconButton(
                    onClick = { onDownloadClick(modelId, fileName) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Download",
                        tint = HFColors.Primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun formatCount(count: Int): String = when {
    count >= 1_000_000 -> "%.1fM".format(count / 1e6)
    count >= 1000 -> "%.1fk".format(count / 1e3)
    else -> count.toString()
}

private fun formatFileSize(bytes: Long?): String {
    if (bytes == null) return ""
    val gb = bytes / (1024.0 * 1024 * 1024)
    val mb = bytes / (1024.0 * 1024)
    return when {
        gb >= 1.0 -> "%.1f GB".format(gb)
        mb >= 1.0 -> "%.0f MB".format(mb)
        else -> "${bytes} B"
    }
}
