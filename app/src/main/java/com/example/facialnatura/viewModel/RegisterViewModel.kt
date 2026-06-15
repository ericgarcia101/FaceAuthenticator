package com.example.facialnatura.viewModel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.facialnatura.data.model.User
import com.example.facialnatura.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val step: Step = Step.FORM,
    val capturedBitmap: Bitmap? = null,
    val isUploading: Boolean = false,
    val success: User? = null,
    val error: String? = null,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val faceDetected: Boolean = false
) {
    enum class Step { FORM, CAPTURED, UPLOADING, SUCCESS }
}

class RegisterViewModel(
    private val repository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state

    fun onFaceDetected(detected: Boolean, count: Int) {
        _state.update { it.copy(faceDetected = detected && count == 1) }
    }

    fun captureFace(bitmap: Bitmap?) {
        if (bitmap == null) {
            _state.update { it.copy(error = "No se pudo capturar la imagen") }
            return
        }
        _state.update {
            it.copy(
                capturedBitmap = bitmap,
                step = RegisterUiState.Step.CAPTURED,
                error = null
            )
        }
    }

    fun retakePhoto() {
        _state.update {
            it.copy(
                capturedBitmap = null,
                step = RegisterUiState.Step.FORM,
                error = null
            )
        }
    }

    fun register(name: String, email: String, password: String) {
        var valid = true
        if (name.isBlank()) {
            _state.update { it.copy(nameError = "Introduce tu nombre") }
            valid = false
        } else _state.update { it.copy(nameError = null) }

        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _state.update { it.copy(emailError = "Correo inválido") }
            valid = false
        } else _state.update { it.copy(emailError = null) }

        if (password.length < 6) {
            _state.update { it.copy(passwordError = "Mínimo 6 caracteres") }
            valid = false
        } else _state.update { it.copy(passwordError = null) }

        val bitmap = _state.value.capturedBitmap
        if (bitmap == null) {
            _state.update { it.copy(error = "Primero captura tu rostro") }
            valid = false
        }

        if (!valid) return

        viewModelScope.launch {
            _state.update { it.copy(isUploading = true, step = RegisterUiState.Step.UPLOADING, error = null) }
            repository.registerUser(name.trim(), email.trim(), password, bitmap!!)
                .onSuccess { user ->
                    _state.update {
                        it.copy(
                            isUploading = false,
                            success = user,
                            step = RegisterUiState.Step.SUCCESS
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isUploading = false,
                            step = RegisterUiState.Step.FORM,
                            error = e.message ?: "Error al registrar"
                        )
                    }
                }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

class RegisterViewModelFactory(
    private val repository: UserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RegisterViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}