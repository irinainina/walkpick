package com.walkspot.app.data.account

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import javax.inject.Inject

// Who this phone belongs to, kept beside the session so the first screen need not wait on the server.
class AccountStore @Inject constructor(
    private val store: DataStore<Preferences>,
) {
    suspend fun isSignedIn(): Boolean = store.data.first()[SIGNED_IN] ?: false

    // Kept after signing out: the next sign-in compares before wiping.
    suspend fun lastUserId(): String? = store.data.first()[USER_ID]

    suspend fun signedIn(userId: String) {
        store.edit {
            it[USER_ID] = userId
            it[SIGNED_IN] = true
        }
    }

    private companion object {
        val USER_ID = stringPreferencesKey("account_user_id")
        val SIGNED_IN = booleanPreferencesKey("account_signed_in")
    }
}
