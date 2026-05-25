package com.hflocal.android.ui.screens.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hflocal.shared.domain.model.HFModel
import com.hflocal.shared.domain.model.SearchQuery
import com.hflocal.shared.domain.repository.IDeviceRepository
import com.hflocal.shared.domain.usecase.SearchModelsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CatalogUiState(
    val query: String = "",
    val models: List<HFModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedFilter: Int = 0,
    val tier: String = ""
)

class CatalogViewModel(
    private val searchModels: SearchModelsUseCase,
    private val deviceRepo: IDeviceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CatalogUiState())
    val state: StateFlow<CatalogUiState> = _state.asStateFlow()

    companion object {
        val FILTER_LABELS = listOf(
            "All",
            "Text Gen",
            "Seq2Seq",
            "Vision",
            "Summarization",
            "Translation"
        )

        val FILTER_TAGS = listOf(
            "",
            "text-generation",
            "text2text-generation",
            "image-text-to-text",
            "summarization",
            "translation"
        )
    }

    init {
        viewModelScope.launch {
            try {
                val tier = deviceRepo.getCurrentTier()
                _state.update { it.copy(tier = tier.name) }
            } catch (_: Exception) { }
            loadModels()
        }
    }

    fun loadModels() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val current = _state.value
                val result = searchModels(
                    SearchQuery(
                        query = current.query,
                        pipelineTag = FILTER_TAGS[current.selectedFilter],
                        sort = "downloads",
                        direction = "desc",
                        limit = 30
                    )
                )
                _state.value = current.copy(models = result, isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Search failed"
                )
            }
        }
    }

    fun updateQuery(query: String) {
        _state.value = _state.value.copy(query = query)
    }

    fun setFilter(index: Int) {
        _state.value = _state.value.copy(selectedFilter = index)
        loadModels()
    }

    fun search() {
        loadModels()
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
