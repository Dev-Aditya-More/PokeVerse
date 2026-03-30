package com.aditya1875.pokeverse.presentation.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
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

class AuthManager(private val context: Context) {

    private val auth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(context.applicationContext)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    init {
        _currentUser.value = auth.currentUser
        _authState.value = if (auth.currentUser != null)
            AuthState.Authenticated(auth.currentUser!!)
        else
            AuthState.Unauthenticated
    }

    suspend fun signInWithGoogle(activity: Activity): AuthResult {
        return try {
            _authState.value = AuthState.Loading

            val rawNonce = UUID.randomUUID().toString()
            val hashedNonce = MessageDigest.getInstance("SHA-256")
                .digest(rawNonce.toByteArray())
                .fold("") { str, it -> str + "%02x".format(it) }


            val idToken = tryGetCredential(activity, hashedNonce, filterByAuthorizedAccounts = true)
                ?: tryGetCredential(activity, hashedNonce, filterByAuthorizedAccounts = false)
                ?: run {
                    _authState.value = AuthState.Unauthenticated
                    return AuthResult.Error("cancelled")
                }

            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user

            if (user != null) {
                _currentUser.value = user
                _authState.value = AuthState.Authenticated(user)
                Log.d("AuthManager", "Sign-in successful: ${user.displayName}")
                AuthResult.Success(user)
            } else {
                _authState.value = AuthState.Error("sign_in_failed")
                AuthResult.Error("sign_in_failed")
            }

        } catch (e: GetCredentialCancellationException) {
            _authState.value = if (auth.currentUser != null)
                AuthState.Authenticated(auth.currentUser!!) else AuthState.Unauthenticated
            AuthResult.Error("cancelled")
        } catch (e: NoCredentialException) {
            Log.e("AuthManager", "No Google accounts found", e)
            _authState.value = AuthState.Error("no_credentials")
            AuthResult.Error("no_credentials")
        } catch (e: GetCredentialException) {
            Log.e("AuthManager", "Credential error: ${e.type}", e)
            _authState.value = AuthState.Error(e.message ?: "credential_error")
            AuthResult.Error(e.message ?: "credential_error")
        } catch (e: Exception) {
            Log.e("AuthManager", "Sign-in error", e)
            _authState.value = AuthState.Error(e.message ?: "unknown_error")
            AuthResult.Error(e.message ?: "unknown_error")
        }
    }

    // Returns idToken on success, null when no credentials in this pass (try next pass),
    // rethrows cancellation and real errors so the outer handler deals with them.
    private suspend fun tryGetCredential(
        activity: Activity,
        hashedNonce: String,
        filterByAuthorizedAccounts: Boolean
    ): String? {
        return try {
            val option = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
                .setServerClientId("362465307544-2aahhujet94930f2tei585jbh9rmrtub.apps.googleusercontent.com")
                .setNonce(hashedNonce)
                .setAutoSelectEnabled(false)   // always show picker, never silently auto-select
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build()

            val result = credentialManager.getCredential(request = request, context = activity)
            GoogleIdTokenCredential.createFrom(result.credential.data).idToken

        } catch (e: GetCredentialCancellationException) {
            throw e   // user cancelled — stop both passes immediately
        } catch (e: NoCredentialException) {
            Log.d("AuthManager", "Pass filterAuthorized=$filterByAuthorizedAccounts: no credentials, trying next")
            null      // no accounts in this pass — let the outer block try pass 2
        } catch (e: GetCredentialException) {
            throw e   // real error — propagate up
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