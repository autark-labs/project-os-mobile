package com.projectos.project_os_mobile.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AppTelemetry(
    val cpuPercent: String = "Unavailable",
    val memoryUsage: String = "Unavailable",
    val memoryPercent: String = "Unavailable",
    val networkIo: String = "Unavailable",
    val blockIo: String = "Unavailable",
    val checkedAt: String = ""
)

@Serializable
data class AppHealthSnapshot (
    val appId: String = "",
    val status: String = "Unknown",
    val message: String = "",
    val detail: String = "",
    val dockerStatus: String = "",
    val localAccessStatus: String = "",
    val privateAccessStatus: String = "",
    val startupGrace: Boolean = false,
    val checkedAt: String = ""
)

@Serializable
data class AppAccessRoute(
    val primaryOpenUrl: String? = null,
    val localUrl: String? = null,
    val privateUrl: String? = null,
    val backendTargetUrl: String? = null,
    val backendProtocol: String? = null,
    val localPort: Int? = null,
    val privatePort: Int? = null,
    val privateLinkStatus: String? = null
)

@Serializable
data class AccessObservedState(
    val localUrl: String? = null,
    val privateUrl: String? = null,
    val localPort: Int? = null,
    val protocol: String? = null,
    val privateLinkStatus: String? = null,
    val lastAccessCheckAt: String? = null,
    val lastSuccessfulAccessAt: String? = null,
    val lastRepairAttemptAt: String? = null,
    val lastRepairStatus: String? = null
)

@Serializable
data class App(
    val appId: String = "",
    val appName: String = "Unknown service",
    val category: String = "",
    val description: String = "",
    val version: String = "",
    val image: String? = null,
    val friendlyStatus: String = "Unknown",
    val technicalStatus: String = "",
    val healthCheck: String = "",
    val runtimePath: String = "",
    val composeProject: String = "",
    val accessUrl: String? = null,
    val accessRoute: AppAccessRoute? = null,
    val observedAccess: AccessObservedState? = null,
    val installedAt: String = "",
    val lastBackup: String = "",
    val telemetry: AppTelemetry? = null,
    val healthSnapshot: AppHealthSnapshot? = null
)

sealed interface AppsFetchResult {
    data class Success(val apps: List<App>) : AppsFetchResult
    data class Failure(val message: String) : AppsFetchResult
}

val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
        })
    }
}

suspend fun fetchApps(baseUrl: String = "http://10.0.2.2:8082"): AppsFetchResult {
    return try {
        val apps: List<App> = client.get("${baseUrl.trimEnd('/')}/api/apps").body()
        AppsFetchResult.Success(apps)
    } catch (error: Exception) {
        AppsFetchResult.Failure(error.message ?: "Project-os could not be reached.")
    }
}
