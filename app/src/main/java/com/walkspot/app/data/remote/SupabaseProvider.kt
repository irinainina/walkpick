package com.walkspot.app.data.remote

import com.walkspot.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Inject
import javax.inject.Singleton

// One client for the whole app: a second one would hold its own session.
@Singleton
class SupabaseProvider @Inject constructor() {

    // False when local.properties has no Supabase lines, as in a fresh clone.
    val isConfigured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotEmpty() &&
            BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotEmpty()

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
        ) {
            install(Auth)
            install(Postgrest)
        }
    }
}
