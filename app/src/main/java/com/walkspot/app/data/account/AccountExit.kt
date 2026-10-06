package com.walkspot.app.data.account

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.walkspot.app.data.local.WalkSpotDatabase
import com.walkspot.app.data.settings.AppSettings
import com.walkspot.app.data.settings.Reminders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "AccountExit"

/** Leaving the account: for a while, or for good. */
class AccountExit @Inject constructor(
    private val auth: AuthRepository,
    private val account: AccountStore,
    private val database: WalkSpotDatabase,
    private val preferences: DataStore<Preferences>,
    private val reminders: Reminders,
) {
    // The data stays on the phone: the same person signing in again finds it at once.
    suspend fun signOut() {
        try {
            auth.signOut()
        } catch (refused: AuthRefused) {
            // Offline is no reason to keep somebody signed in; the session ends on this phone.
            Log.w(TAG, "signed out on the phone only", refused)
        }
        account.signedOut()
    }

    /** Throws [AuthRefused] when the server did not delete the account; the phone is left as it was. */
    suspend fun deleteAccount() {
        auth.deleteAccount()
        withContext(Dispatchers.IO) { database.clearAllTables() }
        preferences.edit { it.clear() }
        reminders.schedule(AppSettings())
    }
}
