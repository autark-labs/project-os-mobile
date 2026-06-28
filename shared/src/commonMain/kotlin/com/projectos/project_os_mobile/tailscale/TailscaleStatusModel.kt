package com.projectos.project_os_mobile.tailscale

import com.projectos.project_os_mobile.client.ProjectOsIssue
import com.projectos.project_os_mobile.client.client
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable

@Serializable
data class ProjectOsAccessStatus(
    val mode: String = "",
    val serverLanUrl: String = "",
    val tailscale: ProjectOsTailscaleStatus = ProjectOsTailscaleStatus(),
    val issues: List<ProjectOsIssue> = emptyList(),
    val updatedAt: String = "",
)

@Serializable
data class ProjectOsTailscaleStatus(
    val installed: Boolean = false,
    val signedIn: Boolean = false,
    val hostname: String = "",
    val magicDnsReady: Boolean = false,
    val httpsReady: Boolean = false,
    val serveReady: Boolean = false,
    val mode: String = "",
)

data class TailscaleAccessSummary(
    val tone: TailscaleAccessTone,
    val title: String,
    val detail: String,
    val actionLabel: String,
)

enum class TailscaleAccessTone {
    Ready,
    Info,
    Warning,
}

suspend fun fetchProjectOsAccessStatus(baseUrl: String): ProjectOsAccessStatus {
    return client.get("${baseUrl.trimEnd('/')}/api/access/status").body()
}

fun tailscaleAccessSummary(
    status: ProjectOsAccessStatus?,
    mobileAppInstalled: Boolean,
): TailscaleAccessSummary {
    if (status == null) {
        return TailscaleAccessSummary(
            tone = TailscaleAccessTone.Info,
            title = "Checking Tailscale",
            detail = "Project-os private access status has not loaded yet.",
            actionLabel = if (mobileAppInstalled) "Open Tailscale" else "Install Tailscale",
        )
    }

    val tailscale = status.tailscale
    if (tailscale.mode == "mock") {
        return TailscaleAccessSummary(
            tone = TailscaleAccessTone.Info,
            title = "Tailscale mock mode",
            detail = "Project-os is simulating Tailscale for local development. This is not production private access.",
            actionLabel = if (mobileAppInstalled) "Open Tailscale" else "Install Tailscale",
        )
    }

    if (!mobileAppInstalled) {
        return TailscaleAccessSummary(
            tone = TailscaleAccessTone.Warning,
            title = "Install Tailscale on this phone",
            detail = "Private service links need this phone connected to the same Tailscale network.",
            actionLabel = "Install Tailscale",
        )
    }

    if (!tailscale.installed) {
        return TailscaleAccessSummary(
            tone = TailscaleAccessTone.Warning,
            title = "Tailscale missing on Project-os",
            detail = firstIssueSummary(status) ?: "Private links need Tailscale installed on the Project-os host.",
            actionLabel = "Open Tailscale",
        )
    }

    if (!tailscale.signedIn) {
        return TailscaleAccessSummary(
            tone = TailscaleAccessTone.Warning,
            title = "Sign in to Tailscale",
            detail = firstIssueSummary(status) ?: "Project-os is not signed in to Tailscale, so private links may not work.",
            actionLabel = "Open Tailscale",
        )
    }

    if (!tailscale.magicDnsReady || !tailscale.httpsReady || !tailscale.serveReady) {
        return TailscaleAccessSummary(
            tone = TailscaleAccessTone.Warning,
            title = "Private links need attention",
            detail = firstIssueSummary(status) ?: "Tailscale is connected, but MagicDNS, HTTPS, or Serve is not fully ready.",
            actionLabel = "Open Tailscale",
        )
    }

    return TailscaleAccessSummary(
        tone = TailscaleAccessTone.Ready,
        title = "Private access ready",
        detail = tailscale.hostname.ifBlank { "Tailscale private service links are ready." },
        actionLabel = "Open Tailscale",
    )
}

private fun firstIssueSummary(status: ProjectOsAccessStatus): String? {
    return status.issues.firstOrNull()?.summary?.takeIf { it.isNotBlank() }
}
