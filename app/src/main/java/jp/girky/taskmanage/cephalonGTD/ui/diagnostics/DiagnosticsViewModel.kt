package jp.girky.taskmanage.cephalonGTD.ui.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.girky.taskmanage.cephalonGTD.ai.AiEngine
import jp.girky.taskmanage.cephalonGTD.ai.DiagnosticStepItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val aiEngine: AiEngine
) : ViewModel() {

    private val _steps = MutableStateFlow<List<DiagnosticStepItem>>(emptyList())
    val steps: StateFlow<List<DiagnosticStepItem>> = _steps.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    init {
        runDiagnostics()
    }

    fun runDiagnostics(testPrompt: String = "こんにちは。10文字以内で挨拶を返してください。") {
        viewModelScope.launch {
            _isRunning.value = true
            try {
                aiEngine.runStepByStepDiagnostics(testPrompt).collect { updatedSteps ->
                    _steps.value = updatedSteps
                }
            } finally {
                _isRunning.value = false
            }
        }
    }
}
