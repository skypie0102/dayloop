package com.shadowmonarchbooks.dayloop.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.shadowmonarchbooks.dayloop.data.LoadedPack
import com.shadowmonarchbooks.dayloop.data.SearchHits
import com.shadowmonarchbooks.dayloop.data.formatDate
import com.shadowmonarchbooks.dayloop.ui.skin.SubmergedSectionHeading
import com.shadowmonarchbooks.dayloop.ui.skin.SubmergedUtilityMessage

/** The input stays above the bounded result list; source browsing never changes the run clock. */
@Composable
internal fun SubmergedSearch(
    pack: LoadedPack,
    routeId: String,
    query: String,
    onQuery: (String) -> Unit,
    hits: SearchHits,
    onOpenDay: (String) -> Unit,
    onOpenBond: (String) -> Unit,
    onOpenActivity: (String) -> Unit,
    onOpenDeadlines: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val list = rememberLazyListState()
    // A restored query starts with its restored list state, rather than resetting on Back/recreation.
    var previousQuery by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(query) }
    LaunchedEffect(query) {
        if (previousQuery != query) list.scrollToItem(0)
        previousQuery = query
    }
    fun dismissKeyboard() { focus.clearFocus(); keyboard?.hide() }
    fun open(action: () -> Unit) { dismissKeyboard(); action() }

    Column(Modifier.fillMaxSize().imePadding().padding(horizontal = 16.dp).testTag("p3r-search")) {
        OutlinedTextField(query, onValueChange = onQuery, singleLine = true,
            label = { Text("Search this guide") }, shape = RectangleShape,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { dismissKeyboard() }),
            trailingIcon = if (query.isNotEmpty()) {
                { IconButton(onClick = { onQuery("") }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                } }
            } else null,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("p3r-search-input"))
        LazyColumn(state = list, contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f).clipToBounds().testTag("p3r-search-results")) {
            item(key = "context") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(pack.pack.title, style = MaterialTheme.typography.bodyMedium, color = colors.secondary)
                    Text(pack.routeLabel(routeId), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    if (query.isNotBlank() && !hits.isEmpty) {
                        Text("${hits.total} ${if (hits.total == 1) "result" else "results"}",
                            style = MaterialTheme.typography.labelLarge, color = colors.secondary)
                    }
                }
            }
            if (query.isBlank()) {
                item(key = "empty") { SubmergedUtilityMessage("Find a source day",
                    "Search walkthrough steps, Social Links, activities, deadlines or class and exam answers. Try a name, an answer or a date such as 2009-05-19.") }
            } else if (hits.isEmpty) {
                item(key = "empty") { SubmergedUtilityMessage("No matches",
                    "Nothing matches \"$query\" in ${pack.pack.title}. Try fewer words or a date such as 2009-05-19.") }
            }
            if (hits.days.isNotEmpty()) {
                item(key = "days") { SearchHeading("Source days · ${hits.days.size}") }
                items(hits.days, key = { "day-${it.date}" }) { hit ->
                    SearchResult(formatDate(hit.date, pack.calendar), hit.snippet, "Open source day",
                        "search-day-${hit.date}") { open { onOpenDay(hit.date) } }
                }
            }
            if (hits.bonds.isNotEmpty()) {
                item(key = "bonds") { SearchHeading("${pack.pack.labels.bond}s · ${hits.bonds.size}") }
                items(hits.bonds, key = { "bond-${it.bondId}" }) { hit ->
                    SearchResult(hit.label, hit.snippet, "Open ${pack.pack.labels.bond}", "search-bond-${hit.bondId}") {
                        open { onOpenBond(hit.bondId) }
                    }
                }
            }
            if (hits.activities.isNotEmpty()) {
                item(key = "activities") { SearchHeading("Activities · ${hits.activities.size}") }
                items(hits.activities, key = { "activity-${it.activityId}" }) { hit ->
                    SearchResult(hit.label, null, "Open activity", "search-activity-${hit.activityId}") {
                        open { onOpenActivity(hit.activityId) }
                    }
                }
            }
            if (hits.deadlines.isNotEmpty()) {
                item(key = "deadlines") { SearchHeading("Deadlines · ${hits.deadlines.size}") }
                items(hits.deadlines, key = { "deadline-${it.deadlineId}" }) { hit ->
                    SearchResult(hit.label, null, "Open deadlines", "search-deadline-${hit.deadlineId}") { open(onOpenDeadlines) }
                }
            }
            if (hits.answers.isNotEmpty()) {
                item(key = "answers") { SearchHeading("Answers · ${hits.answers.size}") }
                items(hits.answers, key = { "answer-${it.date}" }) { hit ->
                    SearchResult(formatDate(hit.date, pack.calendar), "${hit.label}\n${hit.snippet}",
                        "Open answer source day", "search-answer-${hit.date}") { open { onOpenDay(hit.date) } }
                }
            }
        }
    }
}

@Composable
private fun SearchHeading(title: String) =
    SubmergedSectionHeading(title, Modifier.semantics(mergeDescendants = true) { heading() })

@Composable
private fun SearchResult(title: String, snippet: String?, action: String, tag: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.surface, modifier = Modifier.fillMaxWidth().testTag(tag)
        .clickable(role = Role.Button, onClickLabel = action, onClick = onClick)) {
        Column(Modifier.drawWithCache {
            onDrawBehind { drawLine(colors.secondary.copy(alpha = 0.4f), Offset(0f, size.height),
                Offset(size.width, size.height), 1.dp.toPx()) }
        }.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold,
                    color = colors.secondary, modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = colors.secondary, modifier = Modifier.size(24.dp))
            }
            snippet?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}
