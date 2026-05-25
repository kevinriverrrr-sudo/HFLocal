package com.hflocal.android.ui.screens.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hflocal.shared.domain.model.HFModel
import com.hflocal.shared.domain.model.ModelFile
import com.hflocal.shared.domain.repository.IDeviceRepository
import com.hflocal.shared.domain.usecase.GetModelDetailsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ModelDetailUiState(
    val model: HFModel? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val maxModelSizeBytes: Long = 0L
)

class ModelDetailViewModel(
    private val getModelDetails: GetModelDetailsUseCase,
    private val deviceRepo: IDeviceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ModelDetailUiState())
    val state: StateFlow<ModelDetailUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    fun loadModel(modelId: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val tier = deviceRepo.getCurrentTier()
                val model = getModelDetails(modelId)
                _state.value = _state.value.copy(
                    model = model,
                    isLoading = false,
                    maxModelSizeBytes = tier.maxModelSizeBytes
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load model"
                )
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

/**
 * Returns GGUF files from a model, sorted by size descending (largest first).
 */
fun List<ModelFile>.ggufFiles(): List<ModelFile> {
    return this
        .filter { it.rfilename.endsWith(".gguf", ignoreCase = true) }
        .sortedByDescending { it.size ?: 0L }
}

/**
 * Formats a file size in bytes to a human-readable string.
 */
fun formatFileSize(bytes: Long?): String {
    if (bytes == null) return "Unknown"
    return when {
        bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024 -> "%.1f KB".format(bytes / 1_024.0)
        else -> "$bytes B"
    }
}

/**
 * Checks if a GGUF file size is compatible with the device tier.
 */
fun isFileCompatible(fileSize: Long?, maxSizeBytes: Long): Boolean {
    if (maxSizeBytes <= 0L) return true
    if (fileSize == null) return false
    return fileSize <= maxSizeBytes
}
