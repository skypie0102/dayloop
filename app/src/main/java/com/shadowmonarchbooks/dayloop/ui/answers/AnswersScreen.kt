package com.shadowmonarchbooks.dayloop.ui.answers

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.shadowmonarchbooks.dayloop.data.byId
import com.shadowmonarchbooks.dayloop.data.formatDate
import com.shadowmonarchbooks.dayloop.ui.DayloopViewModel
import com.shadowmonarchbooks.dayloop.ui.components.AnswerKindChip
import com.shadowmonarchbooks.dayloop.ui.components.answerKindLabel
import com.shadowmonarchbooks.dayloop.ui.components.EmptyState
import com.shadowmonarchbooks.dayloop.ui.components.SkinTag
import com.shadowmonarchbooks.dayloop.ui.skin.LocalSkin
import com.shadowmonarchbooks.dayloop.ui.skin.hasSubmergedChrome

/**
 * Exam/test answer sheets (docs/PLAN.md Phase 5): every structured answer the
 * pack carries, chronological. Tapping a sheet opens its in-game day.
 */
@Composable
fun AnswersScreen(
    vm: DayloopViewModel = hiltViewModel(),
    onOpenDay: (String) -> Unit,
    initialDate: String? = null,
) {
    val state by vm.state.collectAsState()
    val pack = state.selected ?: run {
        EmptyState("No pack selected.")
        return
    }
    val sheets = remember(pack.answersByDate) { pack.answersByDate.values.sortedBy { it.date } }
    if (sheets.isEmpty()) {
        EmptyState("No answer sheets in this pack yet — exams and class questions appear here.")
        return
    }

    if (LocalSkin.current.hasSubmergedChrome()) {
        SubmergedAnswersCollection(pack, sheets, initialDate, onOpenDay)
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize().padding(16.dp),
    ) {
        items(sheets, key = { it.id }) { sheet ->
            val skin = LocalSkin.current
            val slash = skin.hasSkin && skin.motion == "slash"
            val kindLabel = answerKindLabel(sheet.kind)
            val cardShape = if (slash) skin.shapes.card else RoundedCornerShape(12.dp)
            Surface(
                shape = cardShape,
                color = if (slash) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (slash) Modifier.border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.onBackground,
                            shape = cardShape,
                        ) else Modifier,
                    )
                    .clickable { onOpenDay(sheet.date) },
            ) {
                Column(
                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (slash) {
                            SkinTag(
                                text = skin.cased(kindLabel, "display"),
                                container = MaterialTheme.colorScheme.primary,
                                content = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            AnswerKindChip(sheet.kind)
                        }
                        if (!sheet.label.equals(kindLabel, ignoreCase = true)) {
                            Text(
                                text = if (slash) skin.cased(sheet.label, "display") else sheet.label,
                                style = if (slash) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (slash) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                        Text(
                            text = formatDate(sheet.date, pack.calendar),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (slash) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    pack.deadlines.byId(sheet.deadlineRef)?.let { dl ->
                        Text(
                            text = "Deadline: ${dl.label}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (slash) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        )
                    }
                    sheet.answers.forEachIndexed { i, answer ->
                        Text(
                            text = "${i + 1}. $answer",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (slash) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}
