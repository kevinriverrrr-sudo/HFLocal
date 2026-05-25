package com.hflocal.android.ui.screens.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hflocal.shared.domain.model.DownloadedModel
import com.hflocal.shared.domain.repository.IModelRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DownloadsUiState(
    val activeDownloads: List<DownloadedModel> = emptyList(),
    val completedDownloads: List<DownloadedModel> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class DownloadsViewModel(
    private val modelRepo: IModelRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DownloadsUiState())
    val state: StateFlow<DownloadsUiState> = _state.asStateFlow()

    init {
        loadDownloads()
    }

    private fun loadDownloads() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            modelRepo.getDownloadedModels().collect { models ->
                val active = models.filter { !it.isDownloaded }
                val completed = models.filter { it.isDownloaded }
                _state.update {
                    it.copy(
                        activeDownloads = active,
                        completedDownloads = completed,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun cancelDownload(modelId: String) {
        viewModelScope.launch {
            try {
                modelRepo.deleteModel(modelId)
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = "Failed to cancel download: ${e.message}")
                }
            }
        }
    }

    fun deleteCompleted(modelId: String) {
        viewModelScope.launch {
            try {
                modelRepo.deleteModel(modelId)
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = "Failed to delete: ${e.message}")
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
