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
data class AppHealthSnapshot(
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
data class ProjectOsAction(
    val id: String = "",
    val label: String = "",
    val method: String? = null,
    val href: String? = null,
    val route: String? = null,
    val confirmationRequired: Boolean = false,
    val danger: Boolean = false,
)

@Serializable
data class ProjectOsIssue(
    val id: String = "",
    val scope: String = "",
    val subjectId: String = "",
    val severity: String = "",
    val reasonCode: String = "",
    val title: String = "",
    val summary: String = "",
)

@Serializable
data class AppGuideValue(
    val label: String = "",
    val value: String = "",
    val sensitive: Boolean = false,
    val qr: Boolean = false,
)

@Serializable
data class AppUsageGuide(
    val kind: String = "",
    val primaryAction: String = "",
    val openUrlLabel: String = "",
    val headline: String = "",
    val summary: String = "",
    val setupSteps: List<String> = emptyList(),
    val values: List<AppGuideValue> = emptyList(),
    val notes: List<String> = emptyList(),
)

@Serializable
data class AppSetupField(
    val label: String = "",
    val value: String = "",
)

@Serializable
data class AppSetupIntegration(
    val label: String = "",
    val status: String = "",
    val detail: String = "",
)

@Serializable
data class AppSetupGuide(
    val kind: String = "",
    val automation: String = "",
    val generatedValues: List<AppSetupField> = emptyList(),
    val copyableFields: List<AppSetupField> = emptyList(),
    val qrFields: List<AppSetupField> = emptyList(),
    val integrations: List<AppSetupIntegration> = emptyList(),
    val userSteps: List<String> = emptyList(),
    val automationCapabilities: List<String> = emptyList(),
)

@Serializable
data class AppEvent(
    val id: Long = 0,
    val appId: String = "",
    val type: String = "",
    val message: String = "",
    val createdAt: String = "",
)

@Serializable
data class ProjectOsManagedApp(
    val appInstanceId: String = "",
    val catalogAppId: String = "",
    val name: String = "Unknown service",
    val category: String = "",
    val icon: String = "",
    val userStatus: String = "Unknown",
    val installState: String = "",
    val runtimeState: String = "",
    val ownershipState: String = "",
    val accessState: String = "",
    val backupState: String = "",
    val localUrl: String? = null,
    val privateUrl: String? = null,
    val issues: List<ProjectOsIssue> = emptyList(),
    val actions: List<ProjectOsAction> = emptyList(),
    val updatedAt: String = "",
)

@Serializable
data class ProjectOsApplicationState(
    val managedApps: List<ProjectOsManagedApp> = emptyList(),
    val runtimeApps: List<App> = emptyList(),
    val updatedAt: String = "",
    val refreshStatus: String = "",
    val stale: Boolean = false,
    val lastError: String = "",
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
    val healthSnapshot: AppHealthSnapshot? = null,
    val usageGuide: AppUsageGuide? = null,
    val setupGuide: AppSetupGuide? = null,
    val recentEvents: List<AppEvent> = emptyList(),
    val issues: List<ProjectOsIssue> = emptyList(),
    val canonicalUserStatus: String? = null,
    val canonicalRuntimeState: String? = null,
    val canonicalOwnershipState: String? = null,
    val canonicalAccessState: String? = null,
    val canonicalBackupState: String? = null
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
    val normalizedBaseUrl = baseUrl.trimEnd('/')
    val applicationStateResult = runCatching {
        val state: ProjectOsApplicationState = client.get("$normalizedBaseUrl/api/application-state").body()
        applicationStateToApps(state)
    }

    applicationStateResult.getOrNull()
        ?.takeIf { it.isNotEmpty() }
        ?.let { return AppsFetchResult.Success(it) }

    return try {
        val apps: List<App> = client.get("$normalizedBaseUrl/api/apps").body()
        AppsFetchResult.Success(apps)
    } catch (error: Exception) {
        val message = applicationStateResult.exceptionOrNull()?.message
            ?: error.message
            ?: "Project-os could not be reached."
        AppsFetchResult.Failure(message)
    }
}

fun applicationStateToApps(state: ProjectOsApplicationState): List<App> {
    if (state.managedApps.isEmpty()) return state.runtimeApps

    val runtimeById = state.runtimeApps.associateBy { it.appId }
    return state.managedApps.map { managed ->
        val runtime = runtimeById[managed.catalogAppId]
        val selectedUrl = selectedOpenUrl(managed, runtime)
        val runtimeAccessRoute = runtime?.accessRoute

        App(
            appId = managed.catalogAppId.ifBlank { runtime?.appId.orEmpty() },
            appName = managed.name.ifBlank { runtime?.appName ?: "Unknown service" },
            category = managed.category.ifBlank { runtime?.category.orEmpty() },
            description = runtime?.description.orEmpty(),
            version = runtime?.version.orEmpty(),
            image = managed.icon.ifBlank { runtime?.image.orEmpty() },
            friendlyStatus = managed.userStatus.ifBlank { runtime?.friendlyStatus ?: "Unknown" },
            technicalStatus = runtime?.technicalStatus ?: managed.runtimeState,
            healthCheck = runtime?.healthCheck.orEmpty(),
            runtimePath = runtime?.runtimePath.orEmpty(),
            composeProject = runtime?.composeProject.orEmpty(),
            accessUrl = selectedUrl,
            accessRoute = AppAccessRoute(
                primaryOpenUrl = selectedUrl,
                localUrl = managed.localUrl ?: runtimeAccessRoute?.localUrl,
                privateUrl = managed.privateUrl ?: runtimeAccessRoute?.privateUrl,
                backendTargetUrl = runtimeAccessRoute?.backendTargetUrl,
                backendProtocol = runtimeAccessRoute?.backendProtocol,
                localPort = runtimeAccessRoute?.localPort,
                privatePort = runtimeAccessRoute?.privatePort,
                privateLinkStatus = runtimeAccessRoute?.privateLinkStatus,
            ),
            observedAccess = runtime?.observedAccess,
            installedAt = runtime?.installedAt.orEmpty(),
            lastBackup = runtime?.lastBackup.orEmpty(),
            telemetry = runtime?.telemetry,
            healthSnapshot = runtime?.healthSnapshot,
            usageGuide = runtime?.usageGuide,
            setupGuide = runtime?.setupGuide,
            recentEvents = runtime?.recentEvents.orEmpty(),
            issues = managed.issues,
            canonicalUserStatus = managed.userStatus,
            canonicalRuntimeState = managed.runtimeState,
            canonicalOwnershipState = managed.ownershipState,
            canonicalAccessState = managed.accessState,
            canonicalBackupState = managed.backupState,
        )
    }
}

private fun selectedOpenUrl(managed: ProjectOsManagedApp, runtime: App?): String? {
    return managed.actions.firstNotNullOfOrNull { action ->
        val isOpenAction = action.id.startsWith("open-", ignoreCase = true) ||
            action.label.equals("Open", ignoreCase = true)
        action.href?.takeIf { isOpenAction && action.method.equals("GET", ignoreCase = true) && it.isAbsoluteHttpUrl() }
    }
        ?: managed.privateUrl?.takeIf { it.isAbsoluteHttpUrl() }
        ?: runtime?.accessRoute?.privateUrl?.takeIf { it.isAbsoluteHttpUrl() }
        ?: runtime?.observedAccess?.privateUrl?.takeIf { it.isAbsoluteHttpUrl() }
        ?: managed.localUrl?.takeIf { it.isAbsoluteHttpUrl() }
        ?: runtime?.accessRoute?.localUrl?.takeIf { it.isAbsoluteHttpUrl() }
        ?: runtime?.observedAccess?.localUrl?.takeIf { it.isAbsoluteHttpUrl() }
        ?: runtime?.accessUrl?.takeIf { it.isAbsoluteHttpUrl() }
}

private fun String.isAbsoluteHttpUrl(): Boolean {
    return startsWith("http://") || startsWith("https://")
}
