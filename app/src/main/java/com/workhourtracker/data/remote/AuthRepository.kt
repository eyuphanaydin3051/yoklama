package com.workhourtracker.data.remote

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.workhourtracker.R
import com.workhourtracker.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class AuthRepository private constructor(
    private val context: Context,
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val _currentUserFlow = MutableStateFlow<UserProfile?>(getCurrentUserInternal())
    val currentUserFlow: StateFlow<UserProfile?> = _currentUserFlow.asStateFlow()

    init {
        firebaseAuth.addAuthStateListener { auth ->
            val user = auth.currentUser
            val profile = user?.let {
                UserProfile(
                    uid = it.uid,
                    email = it.email ?: "",
                    displayName = it.displayName,
                    photoUrl = it.photoUrl?.toString()
                )
            }
            _currentUserFlow.value = profile
        }
    }

    private fun getCurrentUserInternal(): UserProfile? = firebaseAuth.currentUser?.let { user ->
        UserProfile(
            uid = user.uid,
            email = user.email ?: "",
            displayName = user.displayName,
            photoUrl = user.photoUrl?.toString()
        )
    }

    companion object {
        const val RC_SIGN_IN = 9001

        @Volatile private var INSTANCE: AuthRepository? = null

        fun getInstance(context: Context): AuthRepository = INSTANCE ?: synchronized(this) {
            AuthRepository(context.applicationContext).also { INSTANCE = it }
        }
    }

    fun getGoogleSignInClient(): GoogleSignInClient {
        val webClientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            ""
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId.ifEmpty { "dummy_client_id" })
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    suspend fun signInWithGoogleToken(idToken: String): Result<UserProfile> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw IllegalStateException("Firebase kullanıcısı bulunamadı")

            val profile = UserProfile(
                uid = user.uid,
                email = user.email ?: "",
                displayName = user.displayName,
                photoUrl = user.photoUrl?.toString()
            )
            _currentUserFlow.value = profile
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        try {
            firebaseAuth.signOut()
        } catch (_: Exception) { }
        try {
            getGoogleSignInClient().signOut().await()
        } catch (_: Exception) { }
        _currentUserFlow.value = null
    }

    fun getCurrentUser(): UserProfile? = _currentUserFlow.value ?: getCurrentUserInternal()

    fun isSignedIn(): Boolean = getCurrentUser() != null
}
