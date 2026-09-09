package com.shadowmonarchbooks.dayloop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.Surface
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.shadowmonarchbooks.dayloop.pack.schema.AnswerSheet
import com.shadowmonarchbooks.dayloop.ui.skin.LocalSkin
import com.shadowmonarchbooks.dayloop.ui.skin.withSkinFont

/** The answer remains visible; the whole strip opens the answer collection when available. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SubmergedAnswerSheet(
    sheet: AnswerSheet,
    onOpenAnswers: (() -> Unit)?,
    deadlineLabel: String?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = RectangleShape, color = colors.surface, modifier = modifier.fillMaxWidth()
        .then(if (onOpenAnswers != null) Modifier.clickable(role = Role.Button,
            onClickLabel = "Open answers", onClick = onOpenAnswers) else Modifier)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(12.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(answerKindLabel(sheet.kind), color = colors.surface,
                    style = MaterialTheme.typography.titleMedium.withSkinFont(LocalSkin.current.type.display),
                    modifier = Modifier.background(colors.secondary).padding(horizontal = 8.dp, vertical = 3.dp))
                if (!sheet.label.equals(answerKindLabel(sheet.kind), ignoreCase = true)) {
                    Text(sheet.label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface,
                        modifier = Modifier.padding(vertical = 4.dp))
                }
            }
            deadlineLabel?.let {
                Text("Deadline: $it", style = MaterialTheme.typography.labelMedium, color = colors.secondary)
            }
            sheet.answers.forEachIndexed { i, answer ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text((i + 1).toString().padStart(2, '0'), style = MaterialTheme.typography.labelLarge,
                        color = colors.secondary, modifier = Modifier.padding(top = 2.dp))
                    Text(answer, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface,
                        modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
