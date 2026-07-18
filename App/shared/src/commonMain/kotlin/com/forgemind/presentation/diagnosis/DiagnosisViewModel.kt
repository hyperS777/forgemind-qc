package com.forgemind.presentation.diagnosis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forgemind.domain.model.Diagnosis
import com.forgemind.domain.model.Findings
import com.forgemind.domain.usecase.GenerateDiagnosisUseCase
import com.forgemind.domain.usecase.ObserveFindingsUseCase
import com.forgemind.domain.usecase.PublishResolutionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DiagnosisUiState {
    data object Idle : DiagnosisUiState
    data object WaitingForFindings : DiagnosisUiState
    data object Diagnosing : DiagnosisUiState
    data class Ready(val findings: Findings, val diagnosis: Diagnosis) : DiagnosisUiState
    data class Resolved(val faultTitle: String) : DiagnosisUiState
    data class Error(val message: String) : DiagnosisUiState
}

class DiagnosisViewModel(
    private val machineId: String,
    private val observeFindingsUseCase: ObserveFindingsUseCase,
    private val generateDiagnosisUseCase: GenerateDiagnosisUseCase,
    private val publishResolutionUseCase: PublishResolutionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<DiagnosisUiState>(DiagnosisUiState.WaitingForFindings)
    val uiState: StateFlow<DiagnosisUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeFindingsUseCase(machineId).collect { findings ->
                _uiState.value = DiagnosisUiState.Diagnosing
                runCatching { generateDiagnosisUseCase.invoke(findings) }
                    .onSuccess { diagnosis ->
                        _uiState.value = DiagnosisUiState.Ready(findings, diagnosis)
                    }
                    .onFailure { e ->
                        _uiState.value = DiagnosisUiState.Error(e.message ?: "Diagnosis failed")
                    }
            }
        }
    }

    fun markResolved() {
        val state = _uiState.value
        if (state is DiagnosisUiState.Ready) {
            publishResolutionUseCase.invoke(machineId, state.diagnosis.faultId)
            _uiState.value = DiagnosisUiState.Resolved(state.diagnosis.faultTitle)
        }
    }
}
