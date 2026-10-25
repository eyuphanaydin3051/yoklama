package com.workhourtracker.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workhourtracker.data.remote.AuthRepository
import com.workhourtracker.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val user: StateFlow<UserProfile?> = authRepository.currentUserFlow

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun getGoogleSignInClient() = authRepository.getGoogleSignInClient()

    fun handleGoogleSignInToken(idToken: String) {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            val result = authRepository.signInWithGoogleToken(idToken)
            _isLoading.value = false
            result.onFailure { exception ->
                _error.value = exception.localizedMessage ?: "Giriş başarısız"
            }
        }
    }

    fun setError(message: String) {
        _error.value = message
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun checkAuthState() {
        // Automatically handled reactively by currentUserFlow
    }
}
