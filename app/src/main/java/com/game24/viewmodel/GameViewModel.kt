package com.game24.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
)

class GameViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    fun updateNumber(index: Int, value: String) {
        val newNumbers = _uiState.value.inputNumbers.toMutableList()
        newNumbers[index] = value
        _uiState.update { it.copy(inputNumbers = newNumbers, error = null) }
    }

    fun calculate() {
        val raw = _uiState.value.inputNumbers

        // 校验输入
        val numbers = raw.map { it.trim().toIntOrNull() }
        if (numbers.any { it == null }) {
            _uiState.update { it.copy(error = "请输入有效的整数") }
            return
        }

        val nums = numbers.filterNotNull()
        if (nums.size != 4) {
            _uiState.update { it.copy(error = "请输入四个数字") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, results = emptyList(), error = null) }

            val results = Solver.solve(nums)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    results = results,
                    error = if (results.isEmpty()) "无解——这四个数字无法算出 24" else null,
                )
            }
        }
    }
}
