package com.shadowmonarchbooks.dayloop.ui.skin

import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import com.shadowmonarchbooks.dayloop.data.formatDate
import com.shadowmonarchbooks.dayloop.progress.StepMark
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Actual activity/Room navigation; no replacement content or screenshot-only assertions. */
class MetaphorAppFlowTest : MetaphorReviewFlow() {
    @Test fun actualGameDateCalendarBrowseAndSourceBackAtDoubleText() {
        launch("2100-06-12", scale = 2f)
        val calendar = dependencies.store().state.value.selected!!.calendar
        compose.onNodeWithContentDescription(formatDate("2100-06-12", calendar)).assertIsDisplayed()
        compose.onNodeWithText("Month 1", substring = true).assertDoesNotExist()
        capture("metaphor-today-double-text")
        tab("Calendar").performClick()
        compose.onNodeWithTag("painted-month-heading").assertTextEquals("June")
        compose.onNodeWithText("2100", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Wat").assertIsDisplayed()
        compose.onNodeWithText("Arb").assertIsDisplayed()
        capture("metaphor-calendar-double-text")
        compose.onNodeWithContentDescription("Next month").performClick()
        compose.onNodeWithTag("painted-month-heading").assertTextEquals("July")
        compose.onNodeWithTag("calendar-date-2100-07-02").performScrollTo().performClick()
        compose.onNodeWithContentDescription(formatDate("2100-07-02", calendar)).assertIsDisplayed()
        compose.onNodeWithText("Browsing walkthrough").assertIsDisplayed()
        compose.onNodeWithText("100% Completion Route").assertIsDisplayed()
        capture("metaphor-source-day-double-text")
        compose.onNodeWithContentDescription("Back").performClick()
        tab("Calendar").assertIsSelected()
        compose.onNodeWithTag("painted-month-heading").assertTextEquals("July")
        tab("Today").performClick()
        compose.onNodeWithContentDescription(formatDate("2100-06-12", calendar)).assertIsDisplayed()
        unchangedClockAndMarks("2100-06-12")
        capture("metaphor-source-back-double-text")
    }

    @Test fun namedCommandsSaveAndReverseWhileReadingPositionSurvivesTabsAndRecreation() {
        launch("2100-06-12")
        val day = dependencies.store().state.value.selected!!.day("standard", "2100-06-12")!!
        val last = day.steps.lastIndex
        val instruction = compose.onNodeWithTag("task-label-$last", useUnmergedTree = true)
        instruction.performScrollTo().assertIsDisplayed()
        capture("metaphor-today-reading-position")
        tab("Calendar").performClick()
        tab("Today").performClick()
        instruction.assertIsDisplayed()
        // Use the exact task's ancestor, since the page also has Done commands for other instructions.
        val card = hasAnyDescendant(hasTestTag("task-label-$last")) and
            androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.StateDescription)
        val doneInCard = compose.onNode(hasContentDescription("Done") and hasAnyAncestor(card))
        doneInCard.assertIsDisplayed().performClick()
        compose.waitUntil(10_000) { runBlocking {
            dependencies.repo().marksFor(profileId).first().any { it.stepIndex == last && it.mark == "DONE" }
        } }
        doneInCard.assertIsSelected()
        scenario!!.recreate()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("task-label-$last", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        instruction.assertIsDisplayed()
        doneInCard.assertIsSelected().performClick()
        compose.waitUntil(10_000) { runBlocking { dependencies.repo().marksFor(profileId).first().isEmpty() } }
        unchangedClockAndMarks("2100-06-12")
        capture("metaphor-reading-restored-and-reversed")
    }

    @Test fun nonPlayableSpanAdvancesOnceAndBackRestoresTheRealDate() {
        launch("2100-06-02")
        capture("metaphor-today-start")
        compose.onNodeWithText("End day", ignoreCase = true).performClick()
        compose.waitUntil(15_000) { runBlocking { dependencies.db().profileDao().byId(profileId)?.clockDate == "2100-06-05" } }
        val calendar = dependencies.store().state.value.selected!!.calendar
        compose.onNodeWithContentDescription(formatDate("2100-06-05", calendar)).assertIsDisplayed()
        // End Day intentionally settles unchecked tasks as Skip. Back moves the
        // clock without clearing those saved marks or granting Done progress.
        val taskCount = dependencies.store().state.value.selected!!
            .day("standard", "2100-06-02")!!.steps.size
        val skipped = runBlocking { dependencies.repo().marksFor(profileId).first() }
            .map { Triple(it.date, it.stepIndex, it.mark) }.toSet()
        assertEquals((0 until taskCount).map { Triple("2100-06-02", it, "SKIP") }.toSet(), skipped)
        capture("metaphor-next-playable-day")
        compose.onNodeWithText("Back").performClick()
        compose.waitUntil(15_000) { runBlocking { dependencies.db().profileDao().byId(profileId)?.clockDate == "2100-06-02" } }
        compose.waitUntil(15_000) {
            compose.onAllNodesWithContentDescription(formatDate("2100-06-02", calendar)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription(formatDate("2100-06-02", calendar)).assertIsDisplayed()
        assertEquals(skipped, runBlocking { dependencies.repo().marksFor(profileId).first() }
            .map { Triple(it.date, it.stepIndex, it.mark) }.toSet())
        capture("metaphor-playable-day-back-preserves-skips")
    }
}
