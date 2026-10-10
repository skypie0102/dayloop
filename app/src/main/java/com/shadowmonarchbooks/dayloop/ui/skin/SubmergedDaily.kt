package com.shadowmonarchbooks.dayloop.ui.skin

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.em
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shadowmonarchbooks.dayloop.pack.schema.Step
import com.shadowmonarchbooks.dayloop.progress.StepMark

/** Source guidance reads at full width while mark commands stay beside the instruction. */
@Composable
internal fun SubmergedTaskCard(
    index: Int,
    step: Step,
    mark: StepMark?,
    onToggle: (StepMark) -> Unit,
    statLabels: Map<String, String>,
    activityLabel: String?,
    onOpenActivity: (() -> Unit)?,
    displayNumber: Int = index + 1,
    inlineTips: Boolean = false,
    onTip: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val feedback = rememberMarkFeedback()
    Surface(color = colors.surface.copy(alpha = 0.82f), shape = RectangleShape,
        modifier = Modifier.fillMaxWidth().semantics {
            stateDescription = when (mark) { StepMark.DONE -> "Done"; StepMark.SKIP -> "Skipped"; else -> "Not marked" }
        }.drawWithCache {
            onDrawBehind { drawLine(colors.outlineVariant, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }
        }) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Text(displayNumber.toString().padStart(2, '0'), style = MaterialTheme.typography.labelLarge,
                    fontStyle = FontStyle.Italic, color = colors.secondary, modifier = Modifier.padding(top = 3.dp))
                Column(Modifier.weight(1f)) {
                    val taskText = buildAnnotatedString {
                        append(step.label)
                        if (step.tip != null && !inlineTips) { append(" "); appendInlineContent("tips", "Tips") }
                    }
                    Text(taskText,
                        inlineContent = if (step.tip == null || inlineTips) emptyMap() else mapOf(
                            "tips" to InlineTextContent(Placeholder(2.8.em, 1.1.em, PlaceholderVerticalAlign.TextCenter)) {
                                Text("Tips", color = colors.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }),
                        modifier = Modifier.testTag("task-label-$index").then(
                            if (step.tip != null && !inlineTips) Modifier.heightIn(min = 48.dp)
                                .clickable(role = Role.Button, onClickLabel = "Open task tips", onClick = onTip) else Modifier),
                        style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
                    if (step.statGains.isNotEmpty()) Text(
                        step.statGains.entries.joinToString(" · ") { (id, value) -> "${statLabels[id] ?: id} +$value" },
                        style = MaterialTheme.typography.labelMedium, color = colors.secondary,
                        modifier = Modifier.padding(top = 6.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf(StepMark.DONE to "Done", StepMark.SKIP to "Skip").forEach { (value, label) ->
                        val active = mark == value
                        Box(contentAlignment = Alignment.Center,
                            modifier = Modifier.selectable(selected = active, role = Role.Button,
                                onClick = { feedback(); onToggle(value) })
                                .semantics { text = AnnotatedString(label); contentDescription = label }
                                .size(PersonaTaskMarkSize).submergedCommandSelection(active)) {
                            Icon(if (value == StepMark.DONE) Icons.Filled.Check else Icons.Filled.Close,
                                contentDescription = null, modifier = Modifier.size(18.dp),
                                tint = if (active) colors.onPrimary else colors.secondary)
                        }
                    }
                }
            }
            if (inlineTips) step.tip?.let { guidance ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth().testTag("task-guidance-$index")) {
                    Text("Guide", style = MaterialTheme.typography.labelMedium, fontStyle = FontStyle.Italic,
                        color = colors.secondary)
                    Text(guidance, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                }
            }
            if (activityLabel != null && onOpenActivity != null) Text(activityLabel, color = colors.secondary,
                modifier = Modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onOpenActivity).padding(vertical = 10.dp))
        }
    }
}

@Composable
internal fun SubmergedSectionHeading(text: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier) {
        Box(Modifier.size(width = 3.dp, height = 22.dp).background(colors.primary))
        Text(text, style = MaterialTheme.typography.titleLarge.withSkinFont(LocalSkin.current.type.display),
            fontStyle = FontStyle.Italic, color = colors.onBackground)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SubmergedDeadline(label: String, daysLeft: Long, kindLabel: String?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val urgent = daysLeft <= 3
    Surface(
        shape = CutCornerShape(bottomEnd = 16.dp),
        color = colors.primaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(kindLabel ?: "Next deadline", style = MaterialTheme.typography.labelLarge,
                    color = colors.onPrimaryContainer, modifier = Modifier.align(Alignment.CenterVertically))
                Text(
                    when {
                        daysLeft < 0 -> "Overdue"
                        daysLeft == 0L -> "Due today"
                        daysLeft == 1L -> "1 day left"
                        else -> "$daysLeft days left"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (urgent) colors.error else colors.onPrimaryContainer,
                )
            }
            Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.onPrimaryContainer)
        }
    }
}

/** Shared P3R selection treatment for commands, including task marks and report stages. */
@Composable
internal fun Modifier.submergedCommandSelection(active: Boolean): Modifier {
    val colors = MaterialTheme.colorScheme
    val disabled = rememberAnimationsDisabled()
    val reveal = animateFloatAsState(if (active) 1f else 0f,
        animationSpec = tween(if (disabled) 0 else 160), label = "Reload command cursor")
    return drawWithCache {
        val inset = 5.dp.toPx()
        val slant = minOf(10.dp.toPx(), size.width * 0.12f)
        val strip = Path().apply {
            moveTo(slant, inset); lineTo(size.width, inset)
            lineTo(size.width - slant, size.height - inset); lineTo(0f, size.height - inset); close()
        }
        val cursor = Path().apply {
            moveTo(3.dp.toPx(), size.height / 2 - 4.dp.toPx())
            lineTo(8.dp.toPx(), size.height / 2)
            lineTo(3.dp.toPx(), size.height / 2 + 4.dp.toPx()); close()
        }
        onDrawBehind {
            if (active) {
                drawPath(strip, colors.primary)
                drawLine(colors.tertiary, Offset(slant, inset), Offset(size.width, inset), 2.dp.toPx())
                drawPath(cursor, colors.onPrimary.copy(alpha = reveal.value))
            }
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
    val style = MaterialTheme.typography.titleLarge.withSkinFont(LocalSkin.current.type.display)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .submergedCommandSelection(primary)
            .heightIn(min = 48.dp).widthIn(min = 48.dp)
            .padding(horizontal = 22.dp, vertical = 10.dp),
    ) {
        Text(text, style = if (largeLabel) style.copy(fontSize = 26.sp, lineHeight = 30.sp) else style,
            fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic,
            color = (if (primary) colors.onPrimary else colors.secondary).copy(alpha = if (enabled) 1f else 0.38f))
    }
}
