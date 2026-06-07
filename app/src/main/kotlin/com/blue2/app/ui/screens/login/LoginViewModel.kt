package com.blue2.app.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blue2.app.domain.models.BluelinkRegion
import com.blue2.app.domain.repository.IBluelinkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val pin: String = "",
    val region: BluelinkRegion = BluelinkRegion.US,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: IBluelinkRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun setEmail(v: String) { _state.update { it.copy(email = v) } }
    fun setPassword(v: String) { _state.update { it.copy(password = v) } }
    fun setPin(v: String) { _state.update { it.copy(pin = v) } }
    fun setRegion(r: BluelinkRegion) { _state.update { it.copy(region = r) } }
    fun clearError() { _state.update { it.copy(error = null) } }

    fun login() {
        val s = _state.value
        if (s.email.isBlank() || s.password.isBlank()) {
            _state.update { it.copy(error = "Email and password are required") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            repository.login(s.email.trim(), s.password, s.region, s.pin)
                .onSuccess {
                    // Load vehicles to confirm login worked
                    repository.getVehicles()
                        .onSuccess { _state.update { st -> st.copy(isLoading = false, isLoggedIn = true) } }
                        .onFailure { e -> _state.update { st -> st.copy(isLoading = false, error = e.message) } }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message ?: "Login failed") }
                }
        }
    }
}
