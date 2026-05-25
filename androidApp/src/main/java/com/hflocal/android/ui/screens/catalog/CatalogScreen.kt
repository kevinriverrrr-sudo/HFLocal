package com.hflocal.android.ui.screens.catalog

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hflocal.shared.domain.model.HFModel
import com.hflocal.shared.ui.navigation.Screen
import com.hflocal.shared.ui.theme.HFColors
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(nav: NavController) {
    val viewModel: CatalogViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HFColors.Background)
    ) {
        // Search bar
        OutlinedTextField(
            value = state.query,
            onValueChange = { viewModel.updateQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = {
                Text(
                    "Search models...",
                    color = HFColors.OnSurfaceMuted
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = HFColors.OnSurfaceMuted
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = HFColors.OnBackground,
                unfocusedTextColor = HFColors.OnBackground,
                focusedBorderColor = HFColors.Primary,
                unfocusedBorderColor = HFColors.Divider,
                focusedContainerColor = HFColors.Surface,
                unfocusedContainerColor = HFColors.Surface
            )
        )

        // Filter chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(CatalogViewModel.FILTER_LABELS.size) { index ->
                FilterChip(
                    selected = state.selectedFilter == index,
                    onClick = { viewModel.setFilter(index) },
                    label = { Text(CatalogViewModel.FILTER_LABELS[index]) },
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = HFColors.Primary.copy(alpha = 0.15f),
                        selectedLabelColor = HFColors.Primary,
                        containerColor = HFColors.Surface,
                        labelColor = HFColors.OnSurfaceMuted
                    ),
                    border = null
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Content
        when {
            state.isLoading -> {
                LoadingState()
            }
            state.error != null -> {
                ErrorState(
                    message = state.error!!,
                    onRetry = { viewModel.loadModels() },
                    onDismiss = { viewModel.clearError() }
                )
            }
            state.models.isEmpty() -> {
                EmptyState(query = state.query)
            }
            else -> {
                ModelList(
                    models = state.models,
                    onModelClick = { model ->
                        nav.navigate(Screen.ModelDetail.createRoute(model.id))
                    }
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = HFColors.Primary)
            Spacer(Modifier.height(16.dp))
            Text(
                "Loading catalog...",
                color = HFColors.OnSurface
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
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
                message,
                color = HFColors.Error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss", color = HFColors.OnSurfaceMuted)
                }
                FilledTonalButton(onClick = onRetry) {
                    Text("Retry", color = HFColors.OnSurface)
                }
            }
        }
    }
}

@Composable
private fun EmptyState(query: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = HFColors.OnSurfaceMuted,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                if (query.isNotBlank()) "No models found for \"$query\""
                else "No models available",
                color = HFColors.OnSurfaceMuted,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun ModelList(
    models: List<HFModel>,
    onModelClick: (HFModel) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(
            count = models.size,
            key = { models[it].id }
        ) { index ->
            ModelCard(model = models[index], onClick = { onModelClick(models[index]) })
        }
    }
}

@Composable
private fun ModelCard(
    model: HFModel,
    onClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = HFColors.Surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize()
        ) {
            // Header row: model name + gated badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (model.author.isNotBlank()) "${model.author}/${model.modelId}"
                               else model.modelId,
                        color = HFColors.OnBackground,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(4.dp))
                    Row {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = HFColors.Warning
                        )
                        Text(
                            " ${formatCount(model.likes)}",
                            color = HFColors.OnSurface,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = HFColors.Secondary
                        )
                        Text(
                            " ${formatCount(model.downloads)}",
                            color = HFColors.OnSurface,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                if (model.gated) {
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = HFColors.Warning.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "GATED",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = HFColors.Warning,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Pipeline tag
            if (model.pipelineTag.isNotBlank()) {
                Text(
                    model.pipelineTag,
                    color = HFColors.OnSurfaceMuted,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Spacer(Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FilledTonalButton(onClick = onClick) {
                    Text("Details", color = HFColors.OnSurface)
                }
                Spacer(Modifier.width(8.dp))
                FilledButton(
                    onClick = onClick,
                    colors = ButtonDefaults.filledButtonColors(
                        containerColor = HFColors.Primary
                    )
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Download")
                }
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
