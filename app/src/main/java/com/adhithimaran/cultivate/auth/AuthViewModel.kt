package com.adhithimaran.cultivate.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// represents all states UI may need to show
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState() // user actively signing in
    data class Success(val user: FirebaseUser) : AuthState() // signed in
    data class Error(val message: String) : AuthState() // error statement

}

// Facilitates communication between Firebase and UI
class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle) // ui can't see, only this can change
    val authState: StateFlow<AuthState> = _authState // can see not change
    val currentUser: FirebaseUser? get() = auth.currentUser // is user already signed-in upon openign app

    fun signInWithGoogleToken(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()
                result.user?.let {
                    _authState.value = AuthState.Success(it)
                } ?: run {
                    _authState.value = AuthState.Error("Sign-in failed: no user returned")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Google sign-in failed")
            }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                result.user?.let {
                    _authState.value = AuthState.Success(it)
                } ?: run {
                    _authState.value = AuthState.Error("Sign-in failed")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Email sign-in failed")
            }
        }
    }

    fun signUpWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                result.user?.let {
                    _authState.value = AuthState.Success(it)
                } ?: run {
                    _authState.value = AuthState.Error("Sign-up failed")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Sign-up failed")
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _authState.value = AuthState.Idle
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }



}