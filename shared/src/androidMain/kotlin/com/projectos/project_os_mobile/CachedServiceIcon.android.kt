package com.projectos.project_os_mobile

import android.graphics.drawable.PictureDrawable
import android.view.View
import android.widget.ImageView
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import com.caverock.androidsvg.SVG
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
actual fun CachedServiceIcon(
    iconUrl: String,
    cacheKey: String,
    fallbackText: String,
    tint: Color,
    modifier: Modifier,
) {
    val context = LocalContext.current
    var iconDrawable by remember(iconUrl, cacheKey) { mutableStateOf<PictureDrawable?>(null) }

    LaunchedEffect(iconUrl, cacheKey) {
        iconDrawable = withContext(Dispatchers.IO) {
            cacheSvgIcon(context.cacheDir, iconUrl, cacheKey)?.toPictureDrawable()
        }
    }

    val drawable = iconDrawable
    if (drawable != null) {
        AndroidView(
            modifier = modifier,
            factory = { viewContext ->
                ImageView(viewContext).apply {
                    setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                    adjustViewBounds = true
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }
            },
            update = { imageView ->
                imageView.setImageDrawable(drawable)
            },
        )
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = fallbackText,
                color = tint,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

private fun cacheSvgIcon(cacheRoot: File, iconUrl: String, cacheKey: String): File? {
    if (iconUrl.isBlank()) return null

    val iconDir = File(cacheRoot, "project-os-icons")
    if (!iconDir.exists() && !iconDir.mkdirs()) return null

    val iconFile = File(iconDir, "${safeCacheKey(cacheKey)}.svg")
    if (iconFile.isFile && iconFile.length() > 0L) return iconFile

    return runCatching {
        val connection = (URL(iconUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 5_000
            requestMethod = "GET"
            instanceFollowRedirects = true
        }
        try {
            if (connection.responseCode !in 200..299) return null
            val contentType = connection.contentType.orEmpty()
            if (!contentType.contains("svg", ignoreCase = true)) return null
            connection.inputStream.use { input ->
                iconFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            iconFile.takeIf { it.length() > 0L }
        } finally {
            connection.disconnect()
        }
    }.getOrNull()
}

private fun safeCacheKey(value: String): String {
    return value.lowercase().replace(Regex("[^a-z0-9._-]+"), "-").trim('-').ifBlank { "service-icon" }
}

private fun File.toPictureDrawable(): PictureDrawable? {
    return runCatching {
        inputStream().use { input ->
            PictureDrawable(SVG.getFromInputStream(input).renderToPicture())
        }
    }.getOrNull()
}
