package com.projectos.project_os_mobile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
expect fun CachedServiceIcon(
    iconUrl: String,
    cacheKey: String,
    fallbackText: String,
    tint: Color,
    modifier: Modifier = Modifier,
)
