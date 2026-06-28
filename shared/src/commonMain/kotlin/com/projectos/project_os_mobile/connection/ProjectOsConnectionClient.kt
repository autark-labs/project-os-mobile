package com.projectos.project_os_mobile.connection

import com.projectos.project_os_mobile.client.client
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.get
import kotlinx.serialization.Serializable

@Serializable
private data class ProjectOsHealthResponse(
    val status: String = "",
    val timestamp: String = "",
)

suspend fun testProjectOsConnection(baseUrl: String): ConnectionTestResult {
    val normalized = normalizeProjectOsBaseUrl(baseUrl)
        ?: return ConnectionTestResult.Failed("Enter a URL starting with http:// or https://.")

    return try {
        val response: ProjectOsHealthResponse = client.get("${normalized}/api/health").body()
        if (response.status.equals("ok", ignoreCase = true)) {
            ConnectionTestResult.Connected("Connected to Project-os at $normalized.")
        } else {
            ConnectionTestResult.Failed("The server responded, but it did not look like Project-os.")
        }
    } catch (error: RedirectResponseException) {
        ConnectionTestResult.Failed("Project-os redirected the health check unexpectedly.")
    } catch (error: ClientRequestException) {
        ConnectionTestResult.Failed("Project-os rejected the health check: HTTP ${error.response.status.value}.")
    } catch (error: ServerResponseException) {
        ConnectionTestResult.Failed("Project-os returned an error: HTTP ${error.response.status.value}.")
    } catch (error: Exception) {
        ConnectionTestResult.Failed(error.message ?: "Project-os could not be reached.")
    }
}
