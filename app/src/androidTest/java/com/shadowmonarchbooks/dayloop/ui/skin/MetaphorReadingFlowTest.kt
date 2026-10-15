package com.shadowmonarchbooks.dayloop.ui.skin

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import com.shadowmonarchbooks.dayloop.data.formatDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** M2 reading, date preview and Room-backed clock/progress contracts. */
class MetaphorReadingFlowTest : MetaphorReviewFlow() {
    @Test fun missionUsesTheCutoffAfterItsOpeningAndDoesNotDependOnTaskMarks() {
        launch("2100-06-13")
        val mission = compose.onNodeWithTag("painted-notice-metaphor.deadline.mission.grand-cathedral")
        mission.assert(hasAnyDescendant(hasText("8 days left")))
        mission.assert(hasAnyDescendant(hasText("Ends ", substring = true)))
        capture("metaphor-m2-open-mission-cutoff")
        unchangedClockAndMarks("2100-06-13")
    }

    @Test fun selectedStoryDateSurvivesTabsAndRecreationWithoutChangingTheClockAtDoubleText() {
        launch("2100-07-20", scale = 2f)
        tab("Calendar").performClick()
        compose.onNodeWithTag("calendar-date-2100-07-20").assertIsSelected()
        compose.onNodeWithTag("calendar-date-2100-07-21").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithTag("calendar-date-2100-07-20").assertIsNotSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Current game date, Walkthrough available"))
        compose.onNodeWithTag("painted-selected-date").performScrollTo().assertTextContains("Jul 21")
        compose.onNodeWithText("Story only. No route tasks are scheduled for this date.").assertIsDisplayed()
        compose.onNodeWithTag("painted-open-day").assertDoesNotExist()
        capture("metaphor-m2-story-preview-double-text")
        scenario!!.recreate()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("painted-selected-date").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("painted-selected-date").assertTextContains("Jul 21").assertIsDisplayed()
        tab("Today").performClick()
        tab("Calendar").performClick()
        compose.onNodeWithTag("painted-selected-date").assertTextContains("Jul 21").assertIsDisplayed()
        unchangedClockAndMarks("2100-07-20")
        capture("metaphor-m2-story-preview-restored-double-text")
    }

    @Test fun denseCityGuidanceAndCommandsRemainAboveTheFooterAtDoubleText() {
        launch("2100-07-23", scale = 2f)
        val title = compose.onNodeWithTag("task-guidance-0-section-0-title", useUnmergedTree = true)
        title.performScrollTo().assertIsDisplayed().assertTextEquals("Inn and Nightprowler")
        capture("metaphor-m2-city-guidance-double-text")
        val body = compose.onNodeWithTag("task-guidance-4-section-1-body", useUnmergedTree = true)
        body.performScrollTo().assertIsDisplayed()
        val end = compose.onNodeWithText("End day", ignoreCase = true)
        assertTrue("Guidance must remain above the fixed End day rail",
            body.fetchSemanticsNode().boundsInRoot.bottom <= end.fetchSemanticsNode().boundsInRoot.top)
        capture("metaphor-m2-guidance-footer-double-text")
        val card = hasAnyDescendant(hasTestTag("task-label-4")) and
            SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription)
        val done = compose.onNode(hasContentDescription("Done") and hasAnyAncestor(card))
        done.performScrollTo().performClick().assertIsSelected()
        compose.waitUntil(10_000) { runBlocking {
            dependencies.repo().marksFor(profileId).first().any { it.stepIndex == 4 && it.mark == "DONE" }
        } }
        done.performClick().assertIsNotSelected()
        compose.waitUntil(10_000) { runBlocking { dependencies.repo().marksFor(profileId).first().isEmpty() } }
        body.performScrollTo()
        tab("Calendar").performClick()
        tab("Today").performClick()
        body.assertIsDisplayed()
        unchangedClockAndMarks("2100-07-23")
    }

    @Test fun calendarSwipesClampAndThirtyDaySourceNavigationKeepsTheClock() {
        launch("2100-06-30")
        val calendar = dependencies.store().state.value.selected!!.calendar
        tab("Calendar").performClick()
        compose.onNodeWithContentDescription("Previous month").assertIsNotEnabled()
        compose.onNodeWithTag("painted-month-grid").performTouchInput { swipeRight() }
        compose.onNodeWithTag("painted-month-heading").assertTextEquals("June")
        capture("metaphor-m2-june-open-grid")
        compose.onNodeWithTag("painted-month-grid").performTouchInput { swipeLeft() }
        compose.onNodeWithTag("painted-month-heading").assertTextEquals("July")
        compose.onNodeWithTag("calendar-date-2100-07-30").performScrollTo().performClick()
        compose.onNodeWithTag("calendar-date-2100-07-31").assertDoesNotExist()
        compose.onNodeWithTag("painted-open-day").performScrollTo().performClick()
        compose.onNodeWithContentDescription(formatDate("2100-07-30", calendar)).assertIsDisplayed()
        capture("metaphor-m2-july-thirty-source")
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithTag("painted-selected-date").assertTextContains("Jul 30")
        repeat(3) { compose.onNodeWithContentDescription("Next month").performScrollTo().performClick() }
        compose.onNodeWithTag("painted-month-heading").assertTextEquals("October")
        compose.onNodeWithContentDescription("Next month").assertIsNotEnabled()
        compose.onNodeWithTag("painted-month-grid").performTouchInput { swipeLeft() }
        compose.onNodeWithTag("painted-month-heading").assertTextEquals("October")
        compose.onNodeWithTag("calendar-date-2100-10-30").performScrollTo().performClick()
        compose.onNodeWithText("Outside this guide's date range.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("painted-open-day").assertDoesNotExist()
        capture("metaphor-m2-outside-guide-preview")
        unchangedClockAndMarks("2100-06-30")
    }

    @Test fun endingJulyTwentySkipsTheStoryDateAndBackPreservesSettledMarks() {
        launch("2100-07-20")
        val count = dependencies.store().state.value.selected!!.day("standard", "2100-07-20")!!.steps.size
        compose.onNodeWithText("End day", ignoreCase = true).performClick()
        compose.waitUntil(15_000) { runBlocking { dependencies.db().profileDao().byId(profileId)?.clockDate == "2100-07-22" } }
        val settled = runBlocking { dependencies.repo().marksFor(profileId).first() }
            .map { Triple(it.date, it.stepIndex, it.mark) }.toSet()
        assertEquals((0 until count).map { Triple("2100-07-20", it, "SKIP") }.toSet(), settled)
        capture("metaphor-m2-july-next-authored-day")
        compose.onNodeWithText("Back").performClick()
        compose.waitUntil(15_000) { runBlocking { dependencies.db().profileDao().byId(profileId)?.clockDate == "2100-07-20" } }
        assertEquals(settled, runBlocking { dependencies.repo().marksFor(profileId).first() }
            .map { Triple(it.date, it.stepIndex, it.mark) }.toSet())
    }

    @Test fun existingProfileOnJulyTwentyOneLoadsWithoutRewritingItsDateOrMarks() {
        launch("2100-07-21")
        compose.onNodeWithText("Story-only date.", substring = true).assertIsDisplayed()
        unchangedClockAndMarks("2100-07-21")
        capture("metaphor-m2-existing-story-date-profile")
        compose.onNodeWithText("End day", ignoreCase = true).performClick()
        compose.waitUntil(15_000) { runBlocking { dependencies.db().profileDao().byId(profileId)?.clockDate == "2100-07-22" } }
        unchangedClockAndMarks("2100-07-22")
    }
}
