package com.walkpick.app.data.account

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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

    /** The name and address as the server last gave them: there before the server answers again. */
    val user: Flow<AccountUser?> = store.data.map { saved ->
        saved[USER_ID]?.let { AccountUser(it, saved[EMAIL].orEmpty(), saved[NAME]) }
    }

    suspend fun remember(user: AccountUser) {
        store.edit {
            it[EMAIL] = user.email
            if (user.name == null) it.remove(NAME) else it[NAME] = user.name
        }
    }

    // The user id stays: the next sign-in compares it before wiping the phone.
    suspend fun signedOut() {
        store.edit { it[SIGNED_IN] = false }
    }

    // Everything but who was here: the profile, the settings and the step counter of the previous owner.
    suspend fun forgetOwner() {
        store.edit { saved ->
            val owner = saved[USER_ID]
            saved.clear()
            owner?.let { saved[USER_ID] = it }
        }
    }

    private companion object {
        val USER_ID = stringPreferencesKey("account_user_id")
        val EMAIL = stringPreferencesKey("account_email")
        val NAME = stringPreferencesKey("account_name")
        val SIGNED_IN = booleanPreferencesKey("account_signed_in")
    }
}
