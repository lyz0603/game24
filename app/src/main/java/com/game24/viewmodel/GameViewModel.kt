package com.game24.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.game24.R
import com.game24.engine.Solver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameUiState(
    val inputNumbers: List<String> = List(4) { "" },
    val results: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isNoSolution: Boolean = false,
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    fun updateNumber(index: Int, value: String) {
        val newNumbers = _uiState.value.inputNumbers.toMutableList()
        newNumbers[index] = value
        _uiState.update { it.copy(inputNumbers = newNumbers, error = null) }
    }

    fun calculate() {
        val raw = _uiState.value.inputNumbers
        val ctx = getApplication<Application>()

        // 校验输入
        val numbers = raw.map { it.trim().toIntOrNull() }
        if (numbers.any { it == null }) {
            _uiState.update { it.copy(error = ctx.getString(R.string.error_invalid_number)) }
            return
        }

        val nums = numbers.filterNotNull()
        if (nums.size != 4) {
            _uiState.update { it.copy(error = ctx.getString(R.string.error_need_four)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, results = emptyList(), error = null) }

            val results = Solver.solve(nums)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    results = results,
                    error = if (results.isEmpty()) ctx.getString(R.string.error_no_solution) else null,
                    isNoSolution = results.isEmpty(),
                )
            }
        }
    }
}
