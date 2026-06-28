package com.projectos.project_os_mobile.connection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ProjectOsConnection {
    const val DEFAULT_BASE_URL = "http://10.0.2.2:8082"

    var baseUrl by mutableStateOf(DEFAULT_BASE_URL)
        private set

    var lastConnectionResult by mutableStateOf<ConnectionTestResult>(ConnectionTestResult.Idle)
        private set

    val isConfigured: Boolean
        get() = baseUrl.isNotBlank()

    private var storage: ProjectOsConnectionStorage? = null

    fun initialize(storage: ProjectOsConnectionStorage) {
        this.storage = storage
        storage.loadBaseUrl()?.let { stored ->
            normalizeProjectOsBaseUrl(stored)?.let { baseUrl = it }
        }
    }

    fun updateBaseUrl(value: String): Boolean {
        val normalized = normalizeProjectOsBaseUrl(value) ?: return false
        baseUrl = normalized
        storage?.saveBaseUrl(normalized)
        lastConnectionResult = ConnectionTestResult.Idle
        return true
    }

    fun updateFromDeepLink(uri: String?): Boolean {
        val normalized = projectOsBaseUrlFromDeepLink(uri) ?: return false
        return updateBaseUrl(normalized)
    }

    fun reset() {
        baseUrl = DEFAULT_BASE_URL
        storage?.clearBaseUrl()
        lastConnectionResult = ConnectionTestResult.Idle
    }

    fun setConnectionResult(result: ConnectionTestResult) {
        lastConnectionResult = result
    }
}

interface ProjectOsConnectionStorage {
    fun loadBaseUrl(): String?
    fun saveBaseUrl(baseUrl: String)
    fun clearBaseUrl()
}

sealed interface ConnectionTestResult {
    data object Idle : ConnectionTestResult
    data object Checking : ConnectionTestResult
    data class Connected(val message: String) : ConnectionTestResult
    data class Failed(val message: String) : ConnectionTestResult
}

fun normalizeProjectOsBaseUrl(value: String): String? {
    val normalized = value.trim().trimEnd('/')
    if (normalized.isBlank()) return null
    if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) return null
    return normalized
}

fun projectOsBaseUrlFromDeepLink(uri: String?): String? {
    if (uri.isNullOrBlank()) return null
    if (!uri.startsWith("projectosmobile://connect")) return null

    val query = uri.substringAfter('?', missingDelimiterValue = "")
    if (query.isBlank()) return null

    val encodedBaseUrl = query.split('&')
        .firstNotNullOfOrNull { part ->
            val key = part.substringBefore('=')
            val value = part.substringAfter('=', missingDelimiterValue = "")
            if (key == "baseUrl" && value.isNotBlank()) value else null
        } ?: return null

    return normalizeProjectOsBaseUrl(percentDecode(encodedBaseUrl))
}

private fun percentDecode(value: String): String {
    val output = StringBuilder()
    var index = 0
    while (index < value.length) {
        val char = value[index]
        if (char == '%' && index + 2 < value.length) {
            val hex = value.substring(index + 1, index + 3)
            val decoded = hex.toIntOrNull(16)
            if (decoded != null) {
                output.append(decoded.toChar())
                index += 3
                continue
            }
        }
        output.append(if (char == '+') ' ' else char)
        index++
    }
    return output.toString()
}
