package com.shadowmonarchbooks.dayloop.ui.skin

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.em
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.shadowmonarchbooks.dayloop.pack.schema.Step
import com.shadowmonarchbooks.dayloop.progress.StepMark

/** A wide reading surface; controls never steal width from the instruction. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SubmergedTaskCard(
    index: Int,
    step: Step,
    mark: StepMark?,
    onToggle: (StepMark) -> Unit,
    statLabels: Map<String, String>,
    activityLabel: String?,
    onOpenActivity: (() -> Unit)?,
    onTip: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val feedback = rememberMarkFeedback()
    Surface(
        color = colors.surface,
        shape = CutCornerShape(topEnd = 12.dp),
        border = BorderStroke(1.dp, if (mark == StepMark.DONE) colors.primary else colors.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    (index + 1).toString().padStart(2, '0'),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Column(Modifier.weight(1f)) {
                    val taskText = buildAnnotatedString {
                        append(step.label)
                        if (step.tip != null) { append(" "); appendInlineContent("tips", "Tips") }
                    }
                    Text(
                        taskText,
                        inlineContent = if (step.tip == null) emptyMap() else mapOf(
                            "tips" to InlineTextContent(Placeholder(2.8.em, 1.1.em, PlaceholderVerticalAlign.TextCenter)) {
                                Text("Tips", color = colors.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            },
                        ),
                        modifier = if (step.tip == null) Modifier else Modifier.heightIn(min = 48.dp)
                            .clickable(role = Role.Button, onClickLabel = "Open task tips", onClick = onTip),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurface,
                        textDecoration = if (mark == StepMark.DONE) TextDecoration.LineThrough else null,
                    )
                    if (step.statGains.isNotEmpty()) {
                        Text(
                            step.statGains.entries.joinToString(" · ") { (id, value) -> "${statLabels[id] ?: id} +$value" },
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.secondary,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    if (activityLabel != null && onOpenActivity != null) {
                        Text(
                            activityLabel,
                            color = colors.primary,
                            modifier = Modifier.heightIn(min = 48.dp)
                                .clickable(role = Role.Button, onClick = onOpenActivity)
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(StepMark.DONE to "Done", StepMark.SKIP to "Skip", StepMark.LATER to "Later").forEach { (value, label) ->
                    val active = mark == value
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .heightIn(min = 48.dp).widthIn(min = 64.dp)
                            .background(if (active) colors.primary else colors.surfaceVariant, CutCornerShape(4.dp))
                            .selectable(selected = active, role = Role.Button, onClick = { feedback(); onToggle(value) })
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Text(label, style = MaterialTheme.typography.labelLarge,
                            color = if (active) colors.onPrimary else colors.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
internal fun SubmergedSectionHeading(text: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier) {
        Box(Modifier.size(width = 3.dp, height = 22.dp).background(colors.primary))
        Text(text, style = MaterialTheme.typography.titleLarge, fontStyle = FontStyle.Italic, color = colors.onBackground)
    }
}

@Composable
internal fun SubmergedDeadline(label: String, daysLeft: Long, kindLabel: String?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val urgent = daysLeft <= 3
    Surface(
        shape = CutCornerShape(bottomEnd = 16.dp),
        color = colors.primaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(kindLabel ?: "Next deadline", style = MaterialTheme.typography.labelLarge, color = colors.onPrimaryContainer)
            Text(
                when {
                    daysLeft < 0 -> "Overdue"
                    daysLeft == 0L -> "Due today"
                    daysLeft == 1L -> "1 day left"
                    else -> "$daysLeft days left"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (urgent) colors.error else colors.onPrimaryContainer,
            )
            Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.onPrimaryContainer)
        }
    }
}

@Composable
internal fun SubmergedActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean,
    fillWidth: Boolean = false,
    largeLabel: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
        color = if (primary) colors.primary else colors.surface,
        contentColor = if (primary) colors.onPrimary else colors.onSurface,
        border = if (primary) null else BorderStroke(1.dp, colors.outlineVariant),
        modifier = modifier.then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = 48.dp).widthIn(min = 48.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(text, style = if (largeLabel) MaterialTheme.typography.titleLarge else MaterialTheme.typography.labelLarge,
                fontStyle = if (primary) FontStyle.Italic else FontStyle.Normal,
                color = (if (primary) colors.onPrimary else colors.onSurface).copy(alpha = if (enabled) 1f else 0.38f))
        }
    }
}
