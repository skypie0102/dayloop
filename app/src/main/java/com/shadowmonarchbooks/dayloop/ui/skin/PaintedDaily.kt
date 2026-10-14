package com.shadowmonarchbooks.dayloop.ui.skin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.shadowmonarchbooks.dayloop.pack.schema.Step
import com.shadowmonarchbooks.dayloop.progress.StepMark
import com.shadowmonarchbooks.dayloop.ui.components.TaskGuidance

/** Cached swash and a fine turquoise rule. Selection has no perpetual animation. */
@Composable
internal fun Modifier.paintedCommandSelection(active: Boolean): Modifier {
    val colors = MaterialTheme.colorScheme
    return drawWithCache {
        val inset = 5.dp.toPx()
        val slash = minOf(8.dp.toPx(), size.width * .1f)
        val paint = Path().apply {
            moveTo(0f, inset + 2.dp.toPx()); lineTo(size.width, inset)
            lineTo(size.width - slash, size.height - inset); lineTo(slash, size.height - inset - 2.dp.toPx()); close()
        }
        onDrawBehind {
            if (active) drawPath(paint, colors.primary)
            drawLine(if (active) colors.onPrimary else colors.secondary,
                Offset(slash, size.height - inset), Offset(size.width - slash, size.height - inset), 1.dp.toPx())
        }
    }
}

@Composable
internal fun PaintedActionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, primary: Boolean, fillWidth: Boolean = false, largeLabel: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Box(modifier.then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
        .heightIn(min = 48.dp).widthIn(min = 48.dp).clickable(enabled, role = Role.Button, onClick = onClick)
        .paintedCommandSelection(primary && enabled).padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center) {
        Text(text, style = (if (largeLabel) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge)
            .withSkinFont(LocalSkin.current.type.display), fontWeight = FontWeight.Black,
            color = (if (primary && enabled) colors.onPrimary else colors.onBackground).copy(alpha = if (enabled) 1f else .5f))
    }
}

@Composable
internal fun PaintedSectionHeading(text: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Text(text, style = MaterialTheme.typography.headlineSmall.withSkinFont(LocalSkin.current.type.display),
        fontWeight = FontWeight.Black, color = colors.onBackground,
        modifier = modifier.semantics { heading() }.drawWithCache {
            onDrawBehind { drawLine(colors.secondary, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }
        }.padding(bottom = 4.dp))
}

/** Source text stays in a legible body face, with two compact, separately named commands. */
@Composable
internal fun PaintedTaskCard(index: Int, step: Step, mark: StepMark?, onToggle: (StepMark) -> Unit,
    statLabels: Map<String, String>, activityLabel: String?, onOpenActivity: (() -> Unit)?,
    displayNumber: Int, inlineTips: Boolean, onTip: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val feedback = rememberMarkFeedback()
    Surface(color = colors.surface, shape = RectangleShape, modifier = Modifier.fillMaxWidth().semantics {
        stateDescription = when (mark) {
            StepMark.DONE -> "Done"; StepMark.SKIP -> "Skipped"; StepMark.LATER -> "Later"; null -> "Not marked"
        }
    }) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(displayNumber.toString().padStart(2, '0'), style = MaterialTheme.typography.labelMedium,
                        color = colors.secondary)
                    val instruction = buildAnnotatedString {
                        append(step.label)
                        if (step.hasGuidance && !inlineTips) { append(" "); appendInlineContent("tips", "Tips") }
                    }
                    Text(instruction, style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (mark == StepMark.DONE) TextDecoration.LineThrough else null,
                        color = if (mark == StepMark.LATER) colors.secondary else colors.onSurface,
                        modifier = Modifier.testTag("task-label-$index").then(
                            if (step.hasGuidance && !inlineTips) Modifier.heightIn(min = 48.dp)
                                .clickable(role = Role.Button, onClickLabel = "Open task tips", onClick = onTip) else Modifier),
                        inlineContent = if (!step.hasGuidance || inlineTips) emptyMap() else mapOf(
                            "tips" to InlineTextContent(Placeholder(2.8.em, 1.1.em, PlaceholderVerticalAlign.TextCenter)) {
                                Text("Tips", style = MaterialTheme.typography.labelMedium, color = colors.secondary)
                            }))
                    if (step.statGains.isNotEmpty()) Text(step.statGains.entries.joinToString(" · ") { (id, n) ->
                        "${statLabels[id] ?: id} +$n" }, style = MaterialTheme.typography.labelMedium, color = colors.secondary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf(StepMark.DONE to "Done", StepMark.SKIP to "Skip").forEach { (value, label) ->
                        val selected = mark == value
                        Box(Modifier.size(48.dp).selectable(selected, role = Role.Button,
                            onClick = { feedback(); onToggle(value) }).semantics {
                                text = AnnotatedString(label); contentDescription = label
                            }, contentAlignment = Alignment.Center) {
                            Box(Modifier.size(PersonaTaskMarkSize).paintedCommandSelection(selected)
                                .testTag("painted-task-$index-${label.lowercase()}"), contentAlignment = Alignment.Center) {
                                Icon(if (value == StepMark.DONE) Icons.Filled.Check else Icons.Filled.Close, null,
                                    tint = if (selected) colors.onPrimary else colors.onSurface, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
            if (inlineTips) TaskGuidance(step, index)
            if (activityLabel != null && onOpenActivity != null) Text(activityLabel,
                style = MaterialTheme.typography.bodyMedium, color = colors.secondary,
                modifier = Modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onOpenActivity).padding(vertical = 10.dp))
        }
    }
}

/** Existing authored deadline arithmetic; no live time-slot or screenshot-inferred countdown. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PaintedDeadline(label: String, daysLeft: Long, kindLabel: String?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().background(colors.surface), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(kindLabel ?: "Next deadline", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
            color = colors.onPrimary, modifier = Modifier.background(colors.primary).padding(horizontal = 10.dp, vertical = 6.dp))
        FlowRow(Modifier.padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(when {
                daysLeft < 0 -> "Overdue"; daysLeft == 0L -> "Due today"; daysLeft == 1L -> "1 day left"; else -> "$daysLeft days left"
            }, style = MaterialTheme.typography.headlineSmall.withSkinFont(LocalSkin.current.type.display),
                fontWeight = FontWeight.Black, color = if (daysLeft <= 3) colors.error else colors.onSurface)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
        }
        Spacer(Modifier.height(2.dp))
    }
}
