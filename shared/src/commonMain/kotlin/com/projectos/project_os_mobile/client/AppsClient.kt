package com.projectos.project_os_mobile.client

import kotlinx.serialization.Serializable
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