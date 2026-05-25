package com.hflocal.android.ui.screens.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hflocal.shared.domain.model.DownloadedModel
import com.hflocal.shared.domain.repository.IModelRepository
import com.hflocal.shared.domain.usecase.DeleteModelUseCase
import com.hflocal.shared.domain.usecase.GetDownloadedModelsUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MyModelsUiState(
    val models: List<DownloadedModel> = emptyList(),
    val totalSizeBytes: Long = 0L,
    val freeSpaceBytes: Long = 64L * 1024 * 1024 * 1024, // Default 64GB
    val isLoading: Boolean = true,
    val error: String? = null
)

class MyModelsViewModel(
    private val getDownloadedModels: GetDownloadedModelsUseCase,
    private val deleteModelUseCase: DeleteModelUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(MyModelsUiState())
    val state: StateFlow<MyModelsUiState> = _state.asStateFlow()

    init {
        loadModels()
    }

    private fun loadModels() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            getDownloadedModels().collect { models ->
                val downloadedModels = models.filter { it.isDownloaded }
                val totalSize = downloadedModels.sumOf { it.fileSizeBytes }
                _state.update {
                    it.copy(
                        models = downloadedModels,
                        totalSizeBytes = totalSize,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun deleteModel(modelId: String) {
        viewModelScope.launch {
            try {
                deleteModelUseCase(modelId)
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to delete model: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
