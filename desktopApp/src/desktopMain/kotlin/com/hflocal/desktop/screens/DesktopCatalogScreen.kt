package com.hflocal.desktop.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.hflocal.shared.domain.model.SearchQuery
import com.hflocal.shared.domain.usecase.SearchModelsUseCase
import com.hflocal.shared.ui.theme.HFColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopCatalogScreen(
    onModelClick: (HFModel) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var models by remember { mutableStateOf<List<HFModel>>(emptyList()) }

    val searchUseCase: SearchModelsUseCase by inject(SearchModelsUseCase::class.java)
    val coroutineScope = rememberCoroutineScope()

    val filters = listOf(
        "All", "Chat", "Code", "Multimodal", "Sum", "Translate"
    )
    val filterTags = listOf(
        "", "text-generation", "text2text-generation", "image-text-to-text",
        "summarization", "translation"
    )

    // Initial load
    LaunchedEffect(Unit) {
        performSearch(
            scope = coroutineScope,
            searchUseCase = searchUseCase,
            query = "",
            pipelineTag = "",
            onLoading = { isLoading = true },
            onError = { isError = true; errorMessage = it },
            onModels = { models = it; isLoading = false; isError = false }
        )
    }

    // Search on query or filter change (debounced)
    LaunchedEffect(searchQuery, selectedFilter) {
        kotlinx.coroutines.delay(300)
        performSearch(
            scope = coroutineScope,
            searchUseCase = searchUseCase,
            query = searchQuery,
            pipelineTag = filterTags.getOrElse(selectedFilter) { "" },
            onLoading = { isLoading = true },
            onError = { isError = true; errorMessage = it },
            onModels = { models = it; isLoading = false; isError = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HFColors.Background)
    ) {
        // Header
        Text(
            text = "Model Catalog",
            style = MaterialTheme.typography.headlineLarge,
            color = HFColors.OnBackground,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            placeholder = {
                Text("Search models...", color = HFColors.OnSurfaceMuted)
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = HFColors.OnSurfaceMuted
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = HFColors.OnSurfaceMuted
                        )
                    }
                }
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

        Spacer(Modifier.height(12.dp))

        // Filter chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filters.size) { index ->
                FilterChip(
                    selected = selectedFilter == index,
                    onClick = { selectedFilter = index },
                    label = { Text(filters[index]) },
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

        Spacer(Modifier.height(12.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = HFColors.Primary)
                        Spacer(Modifier.height(16.dp))
                        Text("Searching models...", color = HFColors.OnSurfaceMuted)
                    }
                }
            }
            isError -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = HFColors.Error
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Error: $errorMessage",
                            color = HFColors.Error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = {
                            performSearch(
                                scope = coroutineScope,
                                searchUseCase = searchUseCase,
                                query = searchQuery,
                                pipelineTag = filterTags.getOrElse(selectedFilter) { "" },
                                onLoading = { isLoading = true },
                                onError = { isError = true; errorMessage = it },
                                onModels = { models = it; isLoading = false; isError = false }
                            )
                        }) {
                            Text("Retry")
                        }
                    }
                }
            }
            models.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = HFColors.OnSurfaceMuted
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No models found",
                            color = HFColors.OnSurfaceMuted,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "Try a different search or filter",
                            color = HFColors.OnSurfaceMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            else -> {
                Text(
                    text = "${models.size} models found",
                    color = HFColors.OnSurfaceMuted,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(Modifier.height(4.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(models.size) { index ->
                        val model = models[index]
                        ModelCard(
                            model = model,
                            onClick = { onModelClick(model) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelCard(
    model: HFModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = HFColors.Surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (model.modelId.isNotEmpty()) "${model.author}/${model.modelId}" else model.id,
                        color = HFColors.OnBackground,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Likes",
                            modifier = Modifier.size(14.dp),
                            tint = HFColors.Warning
                        )
                        Text(
                            " ${formatCount(model.likes)}",
                            color = HFColors.OnSurface,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(Modifier.width(12.dp))
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Downloads",
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
                if (model.gated != "false" && model.gated.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = HFColors.Warning.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "GATED",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = HFColors.Warning,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (model.pipelineTag.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = HFColors.Primary.copy(alpha = 0.1f)
                ) {
                    Text(
                        model.pipelineTag,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = HFColors.Primary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FilledTonalButton(onClick = onClick) {
                    Text("Details", color = HFColors.OnSurface)
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onClick) {
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

private fun performSearch(
    scope: CoroutineScope,
    searchUseCase: SearchModelsUseCase,
    query: String,
    pipelineTag: String,
    onLoading: () -> Unit,
    onError: (String) -> Unit,
    onModels: (List<HFModel>) -> Unit
) {
    scope.launch {
        try {
            onLoading()
            val result = searchUseCase(
                SearchQuery(
                    query = query,
                    pipelineTag = pipelineTag,
                    sort = "downloads",
                    direction = "desc",
                    limit = 20
                )
            )
            onModels(result)
        } catch (e: Exception) {
            onError(e.message ?: "Unknown error")
        }
    }
}

private fun formatCount(count: Int): String = when {
    count >= 1_000_000 -> "%.1fM".format(count / 1e6)
    count >= 1000 -> "%.1fk".format(count / 1e3)
    else -> count.toString()
}
