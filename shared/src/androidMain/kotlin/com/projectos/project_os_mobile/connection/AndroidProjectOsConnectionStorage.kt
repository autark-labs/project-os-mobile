package com.projectos.project_os_mobile.connection

import android.content.Context

class AndroidProjectOsConnectionStorage(context: Context) : ProjectOsConnectionStorage {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun loadBaseUrl(): String? {
        return preferences.getString(KEY_BASE_URL, null)
    }

    override fun saveBaseUrl(baseUrl: String) {
        preferences.edit().putString(KEY_BASE_URL, baseUrl).apply()
    }

    override fun clearBaseUrl() {
        preferences.edit().remove(KEY_BASE_URL).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "project_os_connection"
        const val KEY_BASE_URL = "base_url"
    }
}
