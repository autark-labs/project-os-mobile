package com.projectos.project_os_mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.projectos.project_os_mobile.client.App
import com.projectos.project_os_mobile.client.AppGuideValue
import com.projectos.project_os_mobile.client.AppSetupField
import com.projectos.project_os_mobile.client.ProjectOsIssue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ServiceDetailSheet(
    service: ServiceCardModel,
    onDismiss: () -> Unit,
    onOpenError: suspend (String) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        ServiceDetailContent(
            service = service,
            onOpenError = onOpenError,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 720.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun ServiceDetailContent(
    service: ServiceCardModel,
    onOpenError: suspend (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val app = service.source

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(service.status.accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                CachedServiceIcon(
                    iconUrl = service.iconUrl,
                    cacheKey = service.id,
                    fallbackText = service.name.firstOrNull()?.uppercase() ?: "?",
                    tint = service.status.accent,
                    modifier = Modifier.size(38.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = service.name,
                    color = DetailGraphite,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = service.category.ifBlank { "Project-os service" },
                    color = DetailMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            DetailStatusChip(service.status)
        }

        DetailSection(title = "Open service") {
            LinkRow(
                label = "Preferred link",
                value = service.url,
                preferred = app.accessRoute?.privateUrl == service.url || app.observedAccess?.privateUrl == service.url,
                onCopy = { clipboard.setText(AnnotatedString(service.url)) },
                onOpen = {
                    val result = runCatching { uriHandler.openUri(service.url) }
                    if (result.isFailure) {
                        scope.launch { onOpenError("Could not open ${service.name}. Check that the service URL is valid.") }
                    }
                },
            )
            app.accessRoute?.privateUrl?.takeIf { it.isNotBlank() && it != service.url }?.let {
                LinkRow("Private Tailscale link", it, preferred = true, onCopy = { clipboard.setText(AnnotatedString(it)) })
            }
            app.accessRoute?.localUrl?.takeIf { it.isNotBlank() && it != service.url }?.let {
                LinkRow("Local link", it, preferred = false, onCopy = { clipboard.setText(AnnotatedString(it)) })
            }
        }

        if (app.issues.isNotEmpty()) {
            DetailSection(title = "Project-os notes") {
                app.issues.forEach { IssueRow(it) }
            }
        }

        app.usageGuide?.let { guide ->
            if (guide.headline.isNotBlank() || guide.summary.isNotBlank() || guide.setupSteps.isNotEmpty() || guide.values.isNotEmpty() || guide.notes.isNotEmpty()) {
                DetailSection(title = "How to use") {
                    if (guide.headline.isNotBlank()) Text(guide.headline, color = DetailGraphite, fontWeight = FontWeight.Bold)
                    if (guide.summary.isNotBlank()) Text(guide.summary, color = DetailMuted, style = MaterialTheme.typography.bodySmall)
                    guide.setupSteps.forEachIndexed { index, step -> StepRow(index + 1, step) }
                    guide.values.forEach { value -> GuideValueRow(value, onCopy = { clipboard.setText(AnnotatedString(value.value)) }) }
                    guide.notes.forEach { note -> BulletRow(note) }
                }
            }
        }

        app.setupGuide?.let { guide ->
            if (guide.userSteps.isNotEmpty() || guide.copyableFields.isNotEmpty() || guide.qrFields.isNotEmpty() || guide.automationCapabilities.isNotEmpty()) {
                DetailSection(title = "Setup") {
                    guide.userSteps.forEachIndexed { index, step -> StepRow(index + 1, step) }
                    guide.copyableFields.forEach { field -> SetupFieldRow(field, onCopy = { clipboard.setText(AnnotatedString(field.value)) }) }
                    guide.qrFields.forEach { field -> SetupFieldRow(field, onCopy = { clipboard.setText(AnnotatedString(field.value)) }) }
                    guide.automationCapabilities.forEach { capability -> BulletRow(capability) }
                }
            }
        }

        if (app.recentEvents.isNotEmpty()) {
            DetailSection(title = "Recent activity") {
                app.recentEvents.take(5).forEach { event ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(event.message.ifBlank { event.type }, color = DetailGraphite, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Text(event.createdAt, color = DetailMuted, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Text(
            text = "Mobile is read-only for now. Backend actions such as restart or repair are intentionally not available here.",
            color = DetailMuted,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(bottom = 16.dp),
        )
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable ColumnScopeCompat.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(title, color = DetailGraphite, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFF8FAFE),
        ) {
            ColumnScopeCompat.ColumnContent(content)
        }
    }
}

private object ColumnScopeCompat {
    @Composable
    fun ColumnContent(content: @Composable ColumnScopeCompat.() -> Unit) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            content()
        }
    }
}

@Composable
private fun LinkRow(label: String, value: String, preferred: Boolean, onCopy: () -> Unit, onOpen: (() -> Unit)? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, color = DetailMuted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            if (preferred) {
                Text("Preferred", color = DetailGreen, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
        Text(value.ifBlank { "No link configured" }, color = DetailCobalt, style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCopy, modifier = Modifier.heightIn(min = 32.dp), contentPadding = PaddingValues(horizontal = 10.dp)) {
                Text("Copy")
            }
            if (onOpen != null) {
                Button(onClick = onOpen, modifier = Modifier.heightIn(min = 32.dp), contentPadding = PaddingValues(horizontal = 10.dp), colors = ButtonDefaults.buttonColors(containerColor = DetailCobalt)) {
                    Text("Open")
                }
            }
        }
    }
}

@Composable
private fun IssueRow(issue: ProjectOsIssue) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(issue.title.ifBlank { issue.reasonCode.ifBlank { "Project-os note" } }, color = DetailGraphite, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        Text(issue.summary, color = DetailMuted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun StepRow(index: Int, step: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Text("$index.", color = DetailCobalt, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        Text(step, color = DetailGraphite, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun GuideValueRow(value: AppGuideValue, onCopy: () -> Unit) {
    SetupLikeRow(label = value.label, value = value.value, onCopy = onCopy)
}

@Composable
private fun SetupFieldRow(field: AppSetupField, onCopy: () -> Unit) {
    SetupLikeRow(label = field.label, value = field.value, onCopy = onCopy)
}

@Composable
private fun SetupLikeRow(label: String, value: String, onCopy: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label.ifBlank { "Value" }, color = DetailMuted, style = MaterialTheme.typography.labelSmall)
            Text(value, color = DetailGraphite, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        OutlinedButton(onClick = onCopy, contentPadding = PaddingValues(horizontal = 10.dp)) {
            Text("Copy")
        }
    }
}

@Composable
private fun BulletRow(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.padding(top = 7.dp).size(6.dp).clip(CircleShape).background(DetailCobalt))
        Text(text, color = DetailGraphite, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DetailStatusChip(status: ServiceStatus) {
    Surface(shape = RoundedCornerShape(50), color = status.accent.copy(alpha = 0.10f), contentColor = status.accent) {
        Row(modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(status.accent))
            Text(status.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

private val DetailGraphite = Color(0xFF12182B)
private val DetailMuted = Color(0xFF748096)
private val DetailCobalt = Color(0xFF2F5CC8)
private val DetailGreen = Color(0xFF2FBF71)
