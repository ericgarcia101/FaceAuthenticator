package com.example.facialnatura.viewModel

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.facialnatura.data.model.FaceLoginResult
import com.example.facialnatura.data.repository.UserRepository
import kotlinx.coroutines.launch

class LoginViewModel(
    private val repository: UserRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<LoginUiState>(LoginUiState.Idle)
    val uiState: LiveData<LoginUiState> = _uiState

    private val _isFaceReady = MutableLiveData(false)
    val isFaceReady: LiveData<Boolean> = _isFaceReady

    fun onFaceDetectionUpdate(detected: Boolean, count: Int) {
        _isFaceReady.value = detected && count == 1
    }

    fun attemptFaceLogin(bitmap: Bitmap) {
        _uiState.value = LoginUiState.Scanning

        viewModelScope.launch {
            val result = repository.loginWithFace(bitmap)

            result.onSuccess { (user, confidence) ->
                _uiState.value = LoginUiState.Success(
                    FaceLoginResult(user = user, confidence = confidence, success = true)
                )
            }.onFailure { e ->
                _uiState.value = LoginUiState.Error(
                    message = e.message ?: "Error en la verificación facial"
                )
            }
        }
    }

    fun loginWithPassword(email: String, password: String) {
        _uiState.value = LoginUiState.Scanning

        viewModelScope.launch {
            repository.loginWithPassword(email, password)
                .onSuccess { user ->
                    _uiState.value = LoginUiState.Success(
                        FaceLoginResult(user = user, confidence = 1f, success = true)
                    )
                }
                .onFailure { e ->
                    _uiState.value = LoginUiState.Error(
                        message = e.message ?: "Correo o contraseña incorrectos"
                    )
                }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }

    sealed class LoginUiState {
        object Idle : LoginUiState()
        object FaceDetected : LoginUiState()
        object Scanning : LoginUiState()
        data class Success(val result: FaceLoginResult) : LoginUiState()
        data class Error(val message: String) : LoginUiState()
        object MultipleFaces : LoginUiState()
    }
}

