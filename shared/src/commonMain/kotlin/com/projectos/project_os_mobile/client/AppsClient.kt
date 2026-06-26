package com.projectos.project_os_mobile.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.Serial
import kotlin.time.Instant

@Serializable
data class AppTelemetry(
    val cpuPercent: String,
    val memoryUsage: String,
    val networkIo: String,
    val blockId: String,
    val checkedAt: Instant
)

@Serializable
data class AppHealthSnapshot (
    val appId: String,
    val status: String,
    val message: String,
    val detail: String,
    val dockerStatus: String,
    val localAccessStatus: String,
    val privateAccessStatus: String,
    val startupGrace: Boolean,
    val checkedAt: Instant
)

@Serializable
data class App(
    val appId: String,
    val appName: String,
    val category: String,
    val description: String,
    val version: String,
    val image: String,
    val friendlyStatus: String,
    val technicalStatus: String,
    val healthCheck: String,
    val runtimePath: String,
    val composeProject: String,
    val accessUrl: String,
    val installedAt: Instant,
    val lastBackup: String,
    val appTelemetry: AppTelemetry,
    val healthSnapshot: AppHealthSnapshot
)

val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        })
    }
}

suspend fun fetchApps(): List<App> {
    val response: List<App> = client.get("http://localhost:8082/api/apps").body()
    return response
}