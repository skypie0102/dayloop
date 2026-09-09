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
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shadowmonarchbooks.dayloop.pack.schema.Step
import com.shadowmonarchbooks.dayloop.progress.StepMark

/** A reading column with Done/Skip anchored to its right, matching the P5R task flow. */
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
    val commandStyle = MaterialTheme.typography.titleLarge.withSkinFont(LocalSkin.current.type.display)
        .copy(fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val commandWidth = with(density) {
        maxOf(measurer.measure(AnnotatedString("Done"), commandStyle).size.width,
            measurer.measure(AnnotatedString("Skip"), commandStyle).size.width).toDp()
    }.plus(32.dp).coerceAtLeast(76.dp)
    Surface(
        color = colors.surface,
        shape = RectangleShape,
        modifier = Modifier.fillMaxWidth().drawBehind {
            drawLine(colors.outlineVariant, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
        },
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        // The selected Done command already communicates completion. Avoid
                        // line-through glyph decoration here: on API 35's software renderer,
                        // applying it to a long wrapped task can crash RenderThread while the
                        // row is being re-laid out after a mark change.
                        textDecoration = null,
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
            Column(Modifier.width(commandWidth), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf(StepMark.DONE to "Done", StepMark.SKIP to "Skip").forEach { (value, label) ->
                    val active = mark == value
                    // P3R command lettering with a slanted selection strip, rather than tiled chips.
                    // Keep the whole 48dp target selectable, including the clear space at its edges.
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .selectable(selected = active, role = Role.Button, onClick = { feedback(); onToggle(value) })
                            .submergedCommandSelection(active)
                            .fillMaxWidth().heightIn(min = 48.dp)
                            .padding(start = 18.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                    ) {
                        Text(label, style = commandStyle, maxLines = 1, softWrap = false,
                            color = if (active) colors.onPrimary else colors.secondary)
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
private fun Modifier.submergedCommandSelection(active: Boolean): Modifier {
    val colors = MaterialTheme.colorScheme
    return drawBehind {
        if (active) {
            val inset = 6.dp.toPx()
            val slant = 10.dp.toPx()
            val strip = Path().apply {
                moveTo(slant, inset); lineTo(size.width, inset)
                lineTo(size.width - slant, size.height - inset)
                lineTo(0f, size.height - inset); close()
            }
            drawPath(strip, colors.primary)
            drawLine(colors.tertiary, Offset(slant, inset), Offset(size.width, inset), 2.dp.toPx())
            val cursor = Path().apply {
                moveTo(9.dp.toPx(), size.height / 2 - 4.dp.toPx())
                lineTo(14.dp.toPx(), size.height / 2)
                lineTo(9.dp.toPx(), size.height / 2 + 4.dp.toPx()); close()
            }
            drawPath(cursor, colors.onPrimary)
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
