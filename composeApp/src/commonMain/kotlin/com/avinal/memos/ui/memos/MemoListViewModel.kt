package com.avinal.memos.ui.memos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avinal.memos.api.ApiResult
import com.avinal.memos.domain.Memo
import com.avinal.memos.domain.MemoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MemoListUiState(
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val error: String? = null,
)

class MemoListViewModel(private val memoRepository: MemoRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MemoListUiState())
    val uiState: StateFlow<MemoListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private var searchJob: Job? = null

    val memos: StateFlow<List<Memo>> = combine(
        memoRepository.observeMemos(),
        _searchQuery,
    ) { allMemos, query ->
        if (query.isBlank()) {
            allMemos
        } else {
            val q = query.lowercase()
            allMemos.filter { memo ->
                memo.content.lowercase().contains(q) ||
                    memo.tags.any { it.lowercase().contains(q) } ||
                    memo.snippet.lowercase().contains(q)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            _searchQuery.value = query
        }
    }

    fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "") }
        _searchQuery.value = ""
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            when (val result = memoRepository.refreshMemos()) {
                is ApiResult.Success -> _uiState.update { it.copy(isRefreshing = false) }
                is ApiResult.Error -> _uiState.update { it.copy(isRefreshing = false, error = result.message) }
                is ApiResult.NetworkError -> _uiState.update {
                    it.copy(isRefreshing = false, error = result.exception.message)
                }
            }
        }
    }

    fun loadMore() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            memoRepository.loadNextPage()
            _uiState.update { it.copy(isLoadingMore = false) }
        }
    }

    fun createMemo(content: String, visibility: com.avinal.memos.domain.MemoVisibility) {
        viewModelScope.launch { memoRepository.createMemo(content, visibility) }
    }

    fun deleteMemo(id: String) {
        viewModelScope.launch { memoRepository.deleteMemo(id) }
    }

    fun archiveMemo(id: String) {
        viewModelScope.launch { memoRepository.archiveMemo(id) }
    }

    fun togglePin(memo: Memo) {
        viewModelScope.launch { memoRepository.updateMemo(memo.id, pinned = !memo.pinned) }
    }

    fun updateMemo(id: String, content: String, visibility: com.avinal.memos.domain.MemoVisibility) {
        viewModelScope.launch { memoRepository.updateMemo(id, content = content, visibility = visibility) }
    }

    fun reactToMemo(memoId: String, emoji: String) {
        viewModelScope.launch { memoRepository.reactToMemo(memoId, emoji) }
    }
}
