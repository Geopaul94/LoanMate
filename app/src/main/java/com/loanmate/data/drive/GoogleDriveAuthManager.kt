package com.loanmate.data.drive

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.loanmate.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles Google Sign-In and provides an authorized [Drive] service instance.
 */
@Singleton
class GoogleDriveAuthManager @Inject constructor(
    @ApplicationContext val context: Context
) {

    val isConfigured: Boolean
        get() = BuildConfig.GOOGLE_OAUTH_WEB_CLIENT_ID.isNotBlank()

    private val gso: GoogleSignInOptions by lazy {
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
        if (isConfigured) {
            builder.requestIdToken(BuildConfig.GOOGLE_OAUTH_WEB_CLIENT_ID)
        }
        builder.build()
    }

    private val signInClient: GoogleSignInClient by lazy {
        GoogleSignIn.getClient(context, gso)
    }

    val lastSignedInAccount: GoogleSignInAccount?
        get() = GoogleSignIn.getLastSignedInAccount(context)

    fun getSignInIntent(): Intent = signInClient.signInIntent

    fun signOut(onComplete: () -> Unit) {
        signInClient.signOut().addOnCompleteListener { onComplete() }
    }

    fun getDriveService(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_FILE)
        ).apply {
            selectedAccount = account.account
        }

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("LoanMate").build()
    }
}
