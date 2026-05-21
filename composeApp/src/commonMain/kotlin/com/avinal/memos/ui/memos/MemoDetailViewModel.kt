package com.avinal.memos.ui.memos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avinal.memos.api.ApiResult
import com.avinal.memos.domain.Memo
import com.avinal.memos.domain.MemoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MemoDetailViewModel(
    private val memoId: String,
    private val memoRepository: MemoRepository,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val memo: StateFlow<Memo?> = memoRepository.observeMemo(memoId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init { loadMemo() }

    private fun loadMemo() {
        viewModelScope.launch {
            _isLoading.value = true
            memoRepository.getMemo(memoId)
            _isLoading.value = false
        }
    }

    fun retry() = loadMemo()

    fun toggleTask(lineIndex: Int, checked: Boolean) {
        val current = memo.value ?: return
        val tasks = com.avinal.memos.parser.TaskParser.extractTasks(memoId, current.content)
        val task = tasks.find { it.lineIndex == lineIndex } ?: return
        viewModelScope.launch {
            val newContent = com.avinal.memos.parser.TaskParser.toggleTaskInContent(current.content, task)
            if (newContent != current.content) memoRepository.updateMemo(memoId, content = newContent)
        }
    }

    suspend fun deleteMemoAndWait(): Boolean {
        return when (memoRepository.deleteMemo(memoId)) {
            is ApiResult.Success -> true
            else -> false
        }
    }
}
