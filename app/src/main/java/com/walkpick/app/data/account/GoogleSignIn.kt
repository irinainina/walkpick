package com.walkpick.app.data.account

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.walkpick.app.BuildConfig
import dagger.hilt.android.qualifiers.ActivityContext
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject

sealed interface GoogleResult {
    // [nonce] is the raw one; the token carries its hash, and Supabase compares them.
    data class Token(val idToken: String, val nonce: String) : GoogleResult
    data object Dismissed : GoogleResult
    data object Offline : GoogleResult
    data class Failed(val cause: Exception) : GoogleResult
}

// Built per activity: Credential Manager opens a window of Android's own on top of it.
class GoogleSignIn @Inject constructor(
    @ActivityContext private val context: Context,
) {
    val isConfigured: Boolean get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotEmpty()

    // A chooser that fails is an answer, not a crash.
    @Suppress("TooGenericExceptionCaught")
    suspend fun requestToken(): GoogleResult {
        // Google gets the hash, Supabase the original: a stolen token cannot be replayed.
        val nonce = randomNonce()
        // Not GetGoogleIdOption: that one only lists accounts already on the phone.
        val option = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setNonce(sha256(nonce))
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        return try {
            val response = CredentialManager.create(context).getCredential(context, request)
            val credential = GoogleIdTokenCredential.createFrom(response.credential.data)
            GoogleResult.Token(credential.idToken, nonce)
        } catch (_: GetCredentialCancellationException) {
            // With no network Google closes its chooser and calls it cancelled.
            if (hasNetwork()) GoogleResult.Dismissed else GoogleResult.Offline
        } catch (e: NoCredentialException) {
            // No Google account on the phone: nothing refused, nothing failed.
            GoogleResult.Failed(e)
        } catch (e: Exception) {
            GoogleResult.Failed(e)
        }
    }

    private fun hasNetwork(): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return true
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun randomNonce(): String =
        ByteArray(16).also(SecureRandom()::nextBytes).joinToString("") { "%02x".format(it) }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
