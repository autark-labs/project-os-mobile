package com.projectos.project_os_mobile.tailscale

import kotlin.test.Test
import kotlin.test.assertEquals

class TailscaleStatusModelTest {
    @Test
    fun readyHostWithMobileAppReportsPrivateAccessReady() {
        val summary = tailscaleAccessSummary(
            status = ProjectOsAccessStatus(
                mode = "private_ready",
                tailscale = ProjectOsTailscaleStatus(
                    installed = true,
                    signedIn = true,
                    magicDnsReady = true,
                    httpsReady = true,
                    serveReady = true,
                    mode = "real",
                )
            ),
            mobileAppInstalled = true,
        )

        assertEquals(TailscaleAccessTone.Ready, summary.tone)
        assertEquals("Private access ready", summary.title)
    }

    @Test
    fun mockModeReportsDevelopmentOnlyState() {
        val summary = tailscaleAccessSummary(
            status = ProjectOsAccessStatus(
                mode = "mocked_dev",
                tailscale = ProjectOsTailscaleStatus(
                    installed = true,
                    signedIn = true,
                    magicDnsReady = true,
                    httpsReady = true,
                    serveReady = true,
                    mode = "mock",
                )
            ),
            mobileAppInstalled = true,
        )

        assertEquals(TailscaleAccessTone.Info, summary.tone)
        assertEquals("Tailscale mock mode", summary.title)
    }

    @Test
    fun disconnectedHostReportsRemediationNeeded() {
        val summary = tailscaleAccessSummary(
            status = ProjectOsAccessStatus(
                tailscale = ProjectOsTailscaleStatus(
                    installed = true,
                    signedIn = false,
                    magicDnsReady = false,
                    httpsReady = false,
                    serveReady = false,
                    mode = "real",
                )
            ),
            mobileAppInstalled = true,
        )

        assertEquals(TailscaleAccessTone.Warning, summary.tone)
        assertEquals("Sign in to Tailscale", summary.title)
    }

    @Test
    fun missingMobileAppReportsInstallPrompt() {
        val summary = tailscaleAccessSummary(
            status = ProjectOsAccessStatus(
                tailscale = ProjectOsTailscaleStatus(
                    installed = true,
                    signedIn = true,
                    magicDnsReady = true,
                    httpsReady = true,
                    serveReady = true,
                    mode = "real",
                )
            ),
            mobileAppInstalled = false,
        )

        assertEquals(TailscaleAccessTone.Warning, summary.tone)
        assertEquals("Install Tailscale on this phone", summary.title)
    }
}
