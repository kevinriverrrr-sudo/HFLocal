package com.hflocal.android.ui.screens.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.hflocal.android.service.DownloadService
import com.hflocal.shared.ui.navigation.Screen
import com.hflocal.shared.ui.theme.HFColors
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelDetailScreen(
    nav: NavController,
    modelId: String
) {
    val viewModel: ModelDetailViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    LaunchedEffect(modelId) {
        viewModel.loadModel(modelId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.model?.modelId ?: "Model Details",
                        color = HFColors.OnBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = HFColors.OnBackground
                        )
                    }
                },
                actions = {
                    state.model?.let { model ->
                        IconButton(onClick = {
                            val url = "https://huggingface.co/${model.id}"
                            uriHandler.openUri(url)
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open on HF Hub",
                                tint = HFColors.Secondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HFColors.Surface
                )
            )
        },
        containerColor = HFColors.Background
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = HFColors.Primary)
                        Spacer(Modifier.height(16.dp))
                        Text("Loading model...", color = HFColors.OnSurface)
                    }
                }
            }
            state.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = HFColors.Error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            state.error,
                            color = HFColors.Error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(16.dp))
                        FilledTonalButton(onClick = { viewModel.loadModel(modelId) }) {
                            Text("Retry", color = HFColors.OnSurface)
                        }
                    }
                }
            }
            state.model != null -> {
                ModelDetailContent(
                    state = state,
                    nav = nav,
                    onRetry = { viewModel.loadModel(modelId) },
                    onDownloadFile = { fileName, author, downloadUrl ->
                        DownloadService.enqueue(
                            context = context,
                            modelId = modelId,
                            fileName = fileName,
                            author = author,
                            downloadUrl = downloadUrl
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModelDetailContent(
    state: ModelDetailUiState,
    nav: NavController,
    onRetry: () -> Unit,
    onDownloadFile: (String, String, String) -> Unit
) {
    val model = state.model!!
    val ggufFiles = model.siblings.ggufFiles()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Model header card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = HFColors.Surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Author
                    if (model.author.isNotBlank()) {
                        Text(
                            text = model.author,
                            color = HFColors.Primary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                    }

                    // Model name
                    Text(
                        text = model.modelId.ifBlank { model.id },
                        color = HFColors.OnBackground,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(12.dp))

                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatItem(
                            label = "Downloads",
                            value = formatCount(model.downloads)
                        )
                        StatItem(
                            label = "Likes",
                            value = formatCount(model.likes)
                        )
                        if (model.pipelineTag.isNotBlank()) {
                            StatItem(
                                label = "Pipeline",
                                value = model.pipelineTag
                            )
                        }
                    }

                    // Gated badge
                    if (model.gated) {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = HFColors.Warning.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = HFColors.Warning,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Gated model — requires access approval",
                                    color = HFColors.Warning,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }

                    // Tags
                    if (model.tags.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            model.tags
                                .take(8)
                                .forEach { tag ->
                                    Surface(
                                        shape = RoundedCornerShape(50.dp),
                                        color = HFColors.SurfaceVariant
                                    ) {
                                        Text(
                                            tag,
                                            modifier = Modifier.padding(
                                                horizontal = 10.dp,
                                                vertical = 4.dp
                                            ),
                                            color = HFColors.OnSurface,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            if (model.tags.size > 8) {
                                Text(
                                    "+${model.tags.size - 8} more",
                                    color = HFColors.OnSurfaceMuted,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }

        // Compatibility info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = HFColors.SurfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = HFColors.Secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Device tier: ${state.maxModelSizeBytes.let { bytes ->
                            when {
                                bytes <= 0L -> "No limit"
                                bytes >= 1_073_741_824L -> "%.0f GB".format(bytes / 1_073_741_824.0)
                                bytes >= 1_048_576L -> "%.0f MB".format(bytes / 1_048_576.0)
                                else -> "$bytes B"
                            }
                        }} max",
                        color = HFColors.OnSurface,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // GGUF files header
        item {
            Text(
                "GGUF Files",
                color = HFColors.OnBackground,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // GGUF file list
        if (ggufFiles.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = HFColors.Surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint = HFColors.OnSurfaceMuted,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No GGUF files found",
                            color = HFColors.OnSurfaceMuted
                        )
                    }
                }
            }
        } else {
            items(
                items = ggufFiles,
                key = { it.rfilename }
            ) { file ->
                GgufFileCard(
                    file = file,
                    maxModelSizeBytes = state.maxModelSizeBytes,
                    onDownload = {
                        onDownloadFile(
                            file.rfilename,
                            model.author,
                            "https://huggingface.co/${model.id}/resolve/main/${file.rfilename}"
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String
) {
    Column {
        Text(
            label,
            color = HFColors.OnSurfaceMuted,
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            value,
            color = HFColors.OnBackground,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun GgufFileCard(
    file: com.hflocal.shared.domain.model.ModelFile,
    maxModelSizeBytes: Long,
    onDownload: () -> Unit
) {
    val compatible = isFileCompatible(file.size, maxModelSizeBytes)
    val fileSizeStr = formatFileSize(file.size)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = HFColors.Surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // File name + compatibility badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    file.rfilename,
                    color = HFColors.OnBackground,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = if (compatible) HFColors.Success.copy(alpha = 0.15f)
                           else HFColors.Error.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (compatible) Icons.Default.CheckCircle
                            else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (compatible) HFColors.Success else HFColors.Error,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (compatible) "Compatible" else "Too large",
                            color = if (compatible) HFColors.Success else HFColors.Error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // File size
            Text(
                "Size: $fileSizeStr",
                color = HFColors.OnSurfaceMuted,
                style = MaterialTheme.typography.labelSmall
            )

            Spacer(Modifier.height(12.dp))

            // Download button
            FilledButton(
                onClick = onDownload,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.filledButtonColors(
                    containerColor = if (compatible) HFColors.Primary
                                   else HFColors.SurfaceVariant
                ),
                enabled = compatible
            ) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (compatible) "Download" else "Incompatible",
                    color = if (compatible) HFColors.OnBackground
                           else HFColors.OnSurfaceMuted
                )
            }
        }
    }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> "%.1fM".format(count / 1e6)
        count >= 1_000 -> "%.1fk".format(count / 1e3)
        else -> count.toString()
    }
}
