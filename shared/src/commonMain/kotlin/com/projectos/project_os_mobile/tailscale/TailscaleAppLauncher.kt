package com.projectos.project_os_mobile.tailscale

import androidx.compose.runtime.Composable

interface TailscaleAppController {
    val isInstalled: Boolean
    fun openTailscale()
    fun openInstallPage()
}

@Composable
expect fun rememberTailscaleAppController(): TailscaleAppController
