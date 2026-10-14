package com.walkpick.app.data.account

import android.content.Context
import android.net.Uri
import com.walkpick.app.BuildConfig
import com.walkpick.app.data.remote.SupabaseProvider
import com.walkpick.app.data.remote.signedInSession
import com.walkpick.app.data.remote.webClient
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

/** Export my data: everything the account holds, as the service puts it into one JSON file. */
class DataExport @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabase: SupabaseProvider,
) {
    /** Writes the file into [target], a document the walker has chosen; false when it could not. */
    suspend fun into(target: Uri): Boolean = try {
        val token = supabase.client.signedInSession()?.accessToken
        val answer = token?.let { webClient.post("${BuildConfig.API_URL}/v1/me/export") { bearerAuth(it) } }
        val file = answer?.takeIf { it.status.isSuccess() }?.bodyAsBytes()
        file != null && withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(target)?.use { it.write(file) } != null
        }
    } catch (_: IOException) {
        false
    }
}
