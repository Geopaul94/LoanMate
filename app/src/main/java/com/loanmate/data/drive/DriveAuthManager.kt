package com.loanmate.data.drive

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.loanmate.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns Google Sign-In state for Drive sync. Exposes the current account
 * as a StateFlow so UI can react to sign-in / sign-out.
 *
 * Uses the `drive.appdata` scope — the most restricted Drive scope.
 * The app can ONLY see files it created in the user's appDataFolder;
 * it cannot enumerate or read anything else in their Drive.
 */
@Singleton
class DriveAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val _account = MutableStateFlow(GoogleSignIn.getLastSignedInAccount(context))
    val account: StateFlow<GoogleSignInAccount?> = _account.asStateFlow()

    val isConfigured: Boolean
        get() = BuildConfig.GOOGLE_OAUTH_WEB_CLIENT_ID.isNotBlank()

    private val gso: GoogleSignInOptions by lazy {
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
        if (isConfigured) {
            builder.requestIdToken(BuildConfig.GOOGLE_OAUTH_WEB_CLIENT_ID)
        }
        builder.build()
    }

    private val client: GoogleSignInClient by lazy {
        GoogleSignIn.getClient(context, gso)
    }

    fun signInIntent(): Intent = client.signInIntent

    fun onSignInResult(data: Intent?): Result<GoogleSignInAccount> {
        return runCatching {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                ?: error("No account returned")
            _account.value = account
            account
        }
    }

    suspend fun signOut() {
        client.signOut()
        _account.value = null
    }

    fun refresh() {
        _account.value = GoogleSignIn.getLastSignedInAccount(context)
    }
}
