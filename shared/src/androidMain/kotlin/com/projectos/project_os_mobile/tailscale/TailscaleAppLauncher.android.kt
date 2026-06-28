package com.projectos.project_os_mobile.tailscale

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberTailscaleAppController(): TailscaleAppController {
    val context = LocalContext.current
    return remember(context) {
        object : TailscaleAppController {
            override val isInstalled: Boolean
                get() {
                    return runCatching {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            context.packageManager.getPackageInfo(TAILSCALE_PACKAGE, 0)
                        } else {
                            @Suppress("DEPRECATION")
                            context.packageManager.getPackageInfo(TAILSCALE_PACKAGE, 0)
                        }
                    }.isSuccess
                }

            override fun openTailscale() {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(TAILSCALE_PACKAGE)
                if (launchIntent != null) {
                    context.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } else {
                    openInstallPage()
                }
            }

            override fun openInstallPage() {
                val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$TAILSCALE_PACKAGE"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://tailscale.com/download/android"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(marketIntent)
                } catch (_: ActivityNotFoundException) {
                    context.startActivity(webIntent)
                }
            }
        }
    }
}

private const val TAILSCALE_PACKAGE = "com.tailscale.ipn"
