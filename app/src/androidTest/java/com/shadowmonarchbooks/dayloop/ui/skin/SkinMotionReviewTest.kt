package com.shadowmonarchbooks.dayloop.ui.skin

import android.os.ParcelFileDescriptor
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.shadowmonarchbooks.dayloop.ui.theme.DayloopTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SkinMotionReviewTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun scale(value: Int) {
        for (key in listOf("animator_duration_scale", "transition_animation_scale")) {
            ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(
                "settings put global $key $value")).use { it.readBytes() }
        }
    }
    @After fun restore() { scale(0) }

    @Test fun motionSettingsUpdateWhileThemeRemainsOpen() {
        scale(1)
        compose.setContent {
            DayloopTheme { Text(if (rememberAnimationsDisabled()) "Motion removed" else "Motion enabled") }
        }
        compose.onNodeWithText("Motion enabled").assertIsDisplayed()
        scale(0)
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Motion removed").fetchSemanticsNodes().isNotEmpty() }
        scale(1)
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Motion enabled").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun skippingAnimatedDayAdvanceCommitsExactlyOnceAndKeepsResultsLayoutStable() {
        scale(1)
        compose.mainClock.autoAdvance = false
        val fx = mutableStateOf<AdvanceFx?>(AdvanceFx("masks", listOf("First task" to true, "Second task" to false)))
        var commits = 0
        var finishes = 0
        compose.setContent {
            DayloopTheme {
                CompositionLocalProvider(LocalAnimationsDisabled provides false) {
                    DayAdvanceOverlay(fx.value, onCovered = { commits++ }, onFinished = { finishes++; fx.value = null })
                }
            }
        }
        // Results rows exist from the first frame, so the slash never adds layout work per frame.
        compose.onNodeWithText("First task").assertExists()
        compose.onNodeWithText("Second task").assertExists()
        compose.mainClock.advanceTimeBy(500)
        compose.runOnIdle { assertEquals(1, commits) }
        compose.onNodeWithTag("day-advance").performClick()
        compose.mainClock.advanceTimeBy(3_000)
        compose.runOnIdle { assertEquals(1, commits); assertEquals(1, finishes) }
        compose.onNodeWithTag("day-advance").assertDoesNotExist()
    }

    @Test fun disablingMotionDuringTheResultsHoldDoesNotCommitTheClockTwice() {
        scale(1)
        compose.mainClock.autoAdvance = false
        val removed = mutableStateOf(false)
        val fx = mutableStateOf<AdvanceFx?>(AdvanceFx("moon", emptyList()))
        var commits = 0
        var finishes = 0
        compose.setContent {
            DayloopTheme {
                CompositionLocalProvider(LocalAnimationsDisabled provides removed.value) {
                    DayAdvanceOverlay(fx.value, onCovered = { commits++ }, onFinished = { finishes++; fx.value = null })
                }
            }
        }
        compose.mainClock.advanceTimeBy(500)
        compose.runOnIdle { assertEquals(1, commits); removed.value = true }
        compose.mainClock.advanceTimeBy(32)
        compose.runOnIdle { assertEquals(1, commits); assertEquals(1, finishes) }
    }
}
