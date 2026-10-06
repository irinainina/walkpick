package com.walkspot.app.data.account

import android.util.Log
import com.walkspot.app.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.IOException
import java.nio.channels.UnresolvedAddressException
import javax.inject.Inject

data class AccountUser(val id: String, val email: String)

/** Signing in. Everything here can fail with [AuthRefused]. */
interface AuthRepository {
    suspend fun currentUser(): AccountUser?

    // There is no sign-up screen: the first code makes the account.
    suspend fun sendCode(email: String)

    suspend fun verifyCode(email: String, code: String)

    suspend fun signInWithGoogle(idToken: String, nonce: String)

    // The row every other table of the account hangs beside; kept if it is already there.
    suspend fun rememberProfile()
}

private const val TAG = "AuthRepository"

class SupabaseAuthRepository @Inject constructor(
    private val supabase: SupabaseProvider,
) : AuthRepository {

    private val auth get() = supabase.client.auth

    override suspend fun currentUser(): AccountUser? {
        if (!supabase.isConfigured) return null
        return onServer { auth.currentUserOrNull()?.let { AccountUser(it.id, it.email.orEmpty()) } }
    }

    override suspend fun sendCode(email: String) = onServer {
        auth.signInWith(OTP) { this.email = email }
    }

    override suspend fun verifyCode(email: String, code: String) {
        onServer { auth.verifyEmailOtp(type = OtpType.Email.EMAIL, email = email, token = code) }
    }

    override suspend fun signInWithGoogle(idToken: String, nonce: String) = onServer {
        auth.signInWith(IDToken) {
            this.idToken = idToken
            this.provider = Google
            this.nonce = nonce
        }
    }

    override suspend fun rememberProfile() {
        onServer { rememberOwnProfile() }
    }

    private suspend fun rememberOwnProfile() {
        val row = JsonObject(mapOf("email" to JsonPrimitive(auth.currentUserOrNull()?.email)))
        supabase.client.from("profiles").upsert(row) {
            onConflict = "id"
            ignoreDuplicates = true
        }
    }

    // The one place where the server library's exceptions become an AuthProblem.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun <T> onServer(call: suspend () -> T): T = withContext(Dispatchers.IO) {
        try {
            // Without this the call goes out carrying the app key and the stored session is dropped.
            auth.awaitInitialization()
            call()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (refusal: RestException) {
            // Only the code: the server's wording can name the address it refused.
            val code = (refusal as? AuthRestException)?.errorCode
            Log.w(TAG, "refused: $code")
            throw AuthRefused(code?.asProblem())
        } catch (error: Exception) {
            Log.w(TAG, "request failed", error)
            throw AuthRefused(if (error.isOffline()) AuthProblem.Offline else AuthProblem.NoAnswer, error)
        }
    }
}

// Asked of the failure rather than of the system; the cause is walked because the client wraps it.
internal fun Throwable.isOffline(): Boolean {
    var cause: Throwable? = this
    val seen = mutableSetOf<Throwable>()
    while (cause != null && seen.add(cause)) {
        if (cause is IOException || cause is UnresolvedAddressException) return true
        cause = cause.cause
    }
    return false
}

private fun AuthErrorCode.asProblem(): AuthProblem? = when (this) {
    AuthErrorCode.OverEmailSendRateLimit, AuthErrorCode.OverRequestRateLimit -> AuthProblem.TooOften
    else -> null
}
