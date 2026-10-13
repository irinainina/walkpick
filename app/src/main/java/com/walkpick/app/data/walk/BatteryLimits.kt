package com.walkpick.app.data.walk

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

private const val MIUI_POWER = "com.miui.powerkeeper"

/**
 * MIUI takes the sensors away from an app three minutes after the screen goes dark, and the steps of a
 * walk stop. Only its own "No restrictions" for the app prevents that; an app can open that page, not set it.
 */
class BatteryLimits @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: DataStore<Preferences>,
) {
    private val page = Intent()
        .setComponent(ComponentName(MIUI_POWER, "$MIUI_POWER.ui.HiddenAppsConfigActivity"))
        .putExtra("package_name", context.packageName)
        .putExtra("package_label", context.applicationInfo.loadLabel(context.packageManager).toString())

    /** The page to offer: null on a phone without it and once it has been offered. */
    suspend fun pageToOffer(): Intent? = page.takeIf {
        store.data.first()[OFFERED] != true && context.packageManager.resolveActivity(it, 0) != null
    }

    suspend fun offered() {
        store.edit { it[OFFERED] = true }
    }

    private companion object {
        val OFFERED = booleanPreferencesKey("battery_limits_offered")
    }
}
