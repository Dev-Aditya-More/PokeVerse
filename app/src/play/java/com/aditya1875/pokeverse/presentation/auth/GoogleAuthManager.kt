package com.aditya1875.pokeverse.presentation.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

class AuthManager(
    private val context: Context
) {
    private val auth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(context)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    init {
        // Check if user is already signed in
        _currentUser.value = auth.currentUser
        _authState.value = if (auth.currentUser != null) {
            AuthState.Authenticated(auth.currentUser!!)
        } else {
            AuthState.Unauthenticated
        }
    }

    suspend fun signInWithGoogle(): AuthResult {
        return try {
            _authState.value = AuthState.Loading

            // Generate nonce for security
            val rawNonce = UUID.randomUUID().toString()
            val bytes = rawNonce.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            // Configure Google ID option
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId("362465307544-2aahhujet94930f2tei585jbh9rmrtub.apps.googleusercontent.com")
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            // Get credential
            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken

            // Sign in to Firebase
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(firebaseCredential).await()

            val user = authResult.user
            if (user != null) {
                _currentUser.value = user
                _authState.value = AuthState.Authenticated(user)
                Log.d("AuthManager", "Sign-in successful: ${user.displayName}")
                AuthResult.Success(user)
            } else {
                _authState.value = AuthState.Error("Sign-in failed")
                AuthResult.Error("Sign-in failed")
            }

        } catch (e: GetCredentialException) {
            Log.e("AuthManager", "Credential error", e)
            _authState.value = AuthState.Error(e.message ?: "Authentication failed")
            AuthResult.Error(e.message ?: "Authentication failed")
        } catch (e: Exception) {
            Log.e("AuthManager", "Sign-in error", e)
            _authState.value = AuthState.Error(e.message ?: "Unknown error")
            AuthResult.Error(e.message ?: "Unknown error")
        }
    }

    fun signOut() {
        auth.signOut()
        _currentUser.value = null
        _authState.value = AuthState.Unauthenticated
    }

    fun isSignedIn(): Boolean = auth.currentUser != null
}

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
}

sealed class AuthResult {
    data class Success(val user: FirebaseUser) : AuthResult()
    data class Error(val message: String) : AuthResult()
}