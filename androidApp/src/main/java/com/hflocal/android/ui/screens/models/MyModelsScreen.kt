package com.hflocal.android.ui.screens.models

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.hflocal.shared.ui.navigation.Screen
import com.hflocal.shared.ui.theme.HFColors
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyModelsScreen(nav: NavController) {
    val viewModel: MyModelsViewModel = koinViewModel()
    val uiState by viewModel.state.collectAsState()

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
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    "My Models",
                    color = HFColors.OnBackground
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        // Storage usage bar
        StorageUsageCard(uiState)

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = HFColors.Primary)
            }
        } else if (uiState.models.isEmpty()) {
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
                        Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = HFColors.OnSurfaceMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No downloaded models",
                        color = HFColors.OnSurfaceMuted,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Browse the catalog to find GGUF models",
                        color = HFColors.OnSurfaceMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { nav.navigate(Screen.Catalog.route) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HFColors.Primary
                        )
                    ) {
                        Icon(
                            Icons.Default.Explore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Browse Catalog")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(uiState.models) { model ->
                    ModelCard(
                        model = model,
                        onPlay = {
                            nav.navigate(Screen.Chat.createRoute(model.modelId))
                        },
                        onDelete = {
                            viewModel.deleteModel(model.modelId)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageUsageCard(uiState: MyModelsUiState) {
    val totalSizeGb = uiState.totalSizeBytes / (1024.0 * 1024 * 1024)
    val freeSpaceGb = uiState.freeSpaceBytes / (1024.0 * 1024 * 1024)
    val usageFraction = if (freeSpaceGb > 0) {
        (totalSizeGb / freeSpaceGb).coerceIn(0f, 1f)
    } else {
        0f
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = HFColors.Surface,
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
                    "Models: ${"%.1f".format(totalSizeGb)} GB",
                    color = HFColors.OnBackground,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { usageFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = HFColors.Primary,
                    trackColor = HFColors.Divider
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "of ${"%.0f".format(freeSpaceGb)} GB",
                color = HFColors.OnSurfaceMuted,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun ModelCard(
    model: DownloadedModel,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = HFColors.Surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Psychology,
                contentDescription = null,
                tint = HFColors.Primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${model.author}/${model.modelId}",
                    color = HFColors.OnBackground,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = model.quantization,
                        color = HFColors.Primary,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = formatFileSize(model.fileSizeBytes),
                        color = HFColors.OnSurfaceMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            IconButton(onClick = onPlay) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Chat with model",
                    tint = HFColors.Success
                )
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete model",
                    tint = HFColors.Error
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    "Delete Model",
                    color = HFColors.OnBackground
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete ${model.modelId}? " +
                        "This will free ${formatFileSize(model.fileSizeBytes)} of storage.",
                    color = HFColors.OnSurface
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = HFColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = HFColors.OnSurfaceMuted)
                }
            },
            containerColor = HFColors.SurfaceVariant
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
