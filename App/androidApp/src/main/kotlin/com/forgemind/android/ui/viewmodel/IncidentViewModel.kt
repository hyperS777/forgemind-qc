package com.forgemind.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forgemind.android.model.Diagnosis
import com.forgemind.android.repository.DiagnosisRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
class IncidentViewModel : ViewModel() {

    private val repository = DiagnosisRepository()

    private val _diagnosis = MutableStateFlow<Diagnosis?>(null)
    val diagnosis: StateFlow<Diagnosis?> = _diagnosis

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun analyze(
        imageFile: File?,
        audioFile: File?
    ) {
        viewModelScope.launch {

            try {

                _isLoading.value = true

                _diagnosis.value = repository.analyze(
                    imageFile,
                    audioFile
                )

            } catch (e: Exception) {

                android.util.Log.e(
                    "ForgeMind",
                    "Analyze failed",
                    e
                )

            } finally {

                _isLoading.value = false

            }

        }

    }

}