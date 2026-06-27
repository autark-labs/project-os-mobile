package com.projectos.project_os_mobile.connection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ProjectOsConnection {
    const val DEFAULT_BASE_URL = "http://10.0.2.2:8082"

    var baseUrl by mutableStateOf(DEFAULT_BASE_URL)
        private set

    fun updateBaseUrl(value: String) {
        val normalized = normalizeBaseUrl(value)
        if (normalized.isNotBlank()) {
            baseUrl = normalized
        }
    }

    fun reset() {
        baseUrl = DEFAULT_BASE_URL
    }

    private fun normalizeBaseUrl(value: String): String {
        return value.trim().trimEnd('/')
    }
}
