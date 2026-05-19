package com.avinal.memos.ui.memos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avinal.memos.api.ApiResult
import com.avinal.memos.domain.MemoRepository
import com.avinal.memos.domain.MemoVisibility
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MemoEditorUiState(
    val content: String = "",
    val originalContent: String = "",
    val visibility: MemoVisibility = MemoVisibility.PRIVATE,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false,
    val isEditMode: Boolean = false,
) {
    val isDirty: Boolean get() = content != originalContent
}

class MemoEditorViewModel(
    private val memoId: String?,
    private val memoRepository: MemoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemoEditorUiState(isEditMode = memoId != null))
    val uiState: StateFlow<MemoEditorUiState> = _uiState.asStateFlow()

    init {
        if (memoId != null) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                val memo = memoRepository.getMemo(memoId)
                if (memo != null) {
                    _uiState.update {
                        it.copy(
                            content = memo.content,
                            originalContent = memo.content,
                            visibility = memo.visibility,
                            isLoading = false,
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Memo not found") }
                }
            }
        }
    }

    fun updateContent(content: String) {
        _uiState.update { it.copy(content = content, error = null) }
    }

    fun setVisibility(visibility: MemoVisibility) {
        _uiState.update { it.copy(visibility = visibility) }
    }

    fun save() {
        val state = _uiState.value
        if (state.content.isBlank()) {
            _uiState.update { it.copy(error = "Content cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val result = if (memoId != null) {
                memoRepository.updateMemo(
                    id = memoId,
                    content = state.content,
                    visibility = state.visibility,
                )
            } else {
                memoRepository.createMemo(state.content, state.visibility)
            }

            when (result) {
                is ApiResult.Success -> _uiState.update { it.copy(isSaving = false, isSaved = true) }
                is ApiResult.Error -> _uiState.update { it.copy(isSaving = false, error = result.message) }
                is ApiResult.NetworkError -> _uiState.update {
                    it.copy(isSaving = false, error = result.exception.message ?: "Network error")
                }
            }
        }
    }
}
