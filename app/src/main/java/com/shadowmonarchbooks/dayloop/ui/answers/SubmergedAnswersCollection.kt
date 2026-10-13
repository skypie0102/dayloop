package com.shadowmonarchbooks.dayloop.ui.answers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.shadowmonarchbooks.dayloop.data.LoadedPack
import com.shadowmonarchbooks.dayloop.data.byId
import com.shadowmonarchbooks.dayloop.data.formatDate
import com.shadowmonarchbooks.dayloop.pack.schema.AnswerSheet
import com.shadowmonarchbooks.dayloop.ui.components.SubmergedAnswerSheet

/** Opens at the originating sheet, then retains scroll position while its source day is read. */
@Composable
internal fun SubmergedAnswersCollection(
    pack: LoadedPack,
    sheets: List<AnswerSheet>,
    initialDate: String?,
    onOpenDay: (String) -> Unit,
) {
    val initialIndex = remember(sheets, initialDate) {
        if (initialDate == null) 0 else sheets.indexOfFirst { it.date >= initialDate }
            .let { if (it < 0) sheets.lastIndex else it } + 1 // Intro occupies index 0.
    }
    val scroll = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    LazyColumn(state = scroll, contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize().testTag("p3r-answer-list")) {
        item(key = "answer-intro") {
            Text("${sheets.size} sheets · Answers in question order. Open a sheet to read its walkthrough day.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
        }
        items(sheets, key = { it.id }) { sheet ->
            val date = formatDate(sheet.date, pack.calendar)
            SubmergedAnswerSheet(sheet, onOpenAnswers = { onOpenDay(sheet.date) },
                deadlineLabel = pack.deadlines.byId(sheet.deadlineRef)?.label,
                dateLabel = date, onClickLabel = "Open walkthrough for $date")
        }
    }
}
