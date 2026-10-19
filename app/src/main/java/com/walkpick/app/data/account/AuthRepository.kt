package com.walkpick.app.data.account

import android.util.Log
import com.walkpick.app.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.IOException
import java.nio.channels.UnresolvedAddressException
import javax.inject.Inject

// [name] is the first name Google gave; a sign-in by email code has none.
data class AccountUser(val id: String, val email: String, val name: String? = null)

/** Signing in. Everything here can fail with [AuthRefused]. */
interface AuthRepository {
    suspend fun currentUser(): AccountUser?

    // There is no sign-up screen: the first code makes the account.
    suspend fun sendCode(email: String)

    suspend fun verifyCode(email: String, code: String)

    suspend fun signInWithGoogle(idToken: String, nonce: String)

    // The row every other table of the account hangs beside; kept if it is already there.
    suspend fun rememberProfile()

    suspend fun signOut()

    /** The name the walker chose, kept in the account beside the one Google gave. */
    suspend fun rename(name: String)

    // The server removes the photos, the user and, by cascade, every row of the account.
    suspend fun deleteAccount()
}

private const val TAG = "AuthRepository"
private const val DELETE_ACCOUNT = "delete-account"
private const val PREFERRED_NAME = "preferred_name"

class SupabaseAuthRepository @Inject constructor(
    private val supabase: SupabaseProvider,
) : AuthRepository {

    private val auth get() = supabase.client.auth

    override suspend fun currentUser(): AccountUser? {
        if (!supabase.isConfigured) return null
        return onServer {
            auth.currentUserOrNull()?.let { user ->
                val meta = user.userMetadata
                val chosen = meta?.get(PREFERRED_NAME)?.jsonPrimitive?.contentOrNull
                val google = meta?.get("full_name")?.jsonPrimitive?.contentOrNull?.substringBefore(' ')
                AccountUser(user.id, user.email.orEmpty(), (chosen ?: google)?.ifBlank { null })
            }
        }
    }

    // Not "full_name": a sign-in with Google writes that one again every time.
    override suspend fun rename(name: String) = onServer {
        auth.updateUser { data = buildJsonObject { put(PREFERRED_NAME, name) } }
        Unit
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

    override suspend fun signOut() = onServer { auth.signOut() }

    override suspend fun deleteAccount() {
        onServer {
            checkNotNull(auth.currentUserOrNull()) { "nobody is signed in" }
            supabase.client.functions.invoke(DELETE_ACCOUNT)
        }
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
