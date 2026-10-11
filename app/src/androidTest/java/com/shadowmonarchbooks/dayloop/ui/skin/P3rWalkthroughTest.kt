package com.shadowmonarchbooks.dayloop.ui.skin

import android.content.ContentValues
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.shadowmonarchbooks.dayloop.data.LoadedPack
import com.shadowmonarchbooks.dayloop.pack.PackLoader
import com.shadowmonarchbooks.dayloop.progress.StepMark
import com.shadowmonarchbooks.dayloop.ui.components.TasksList
import com.shadowmonarchbooks.dayloop.ui.theme.DayloopTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class P3rWalkthroughTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Volatile private var fontReady = false
    private var markedIndex: Int? = null
    private var markedValue: StepMark? = null

    private fun asset(path: String) = context.assets.open(path).bufferedReader().use { it.readText() }

    private fun show(date: String, scale: Float = 1f, inlineTips: Boolean? = null) {
        val pack = LoadedPack("p3r", requireNotNull(PackLoader.decodePack(asset("p3r/pack.json"))))
        val day = requireNotNull(PackLoader.decodeWalkthrough(asset("p3r/walkthrough/${date.take(7)}.json"))).days.single { it.date == date }
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                DayloopTheme(pack) {
                    val skin = LocalSkin.current
                    SideEffect { fontReady = skin.type.display?.family != null }
                    val marks = remember { mutableStateMapOf<Int, StepMark>() }
                    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                        .skinBackdrop(skin).verticalScroll(rememberScrollState()).padding(16.dp)) {
                        TasksList(
                            steps = day.steps,
                            markAt = { marks[it] },
                            onToggleMark = { index, mark ->
                                markedIndex = index
                                if (marks[index] == mark) marks.remove(index) else marks[index] = mark
                                markedValue = marks[index]
                            },
                            statLabels = pack.pack.stats.associate { it.id to it.label },
                            activityLabels = emptyMap(),
                            slotLabels = pack.pack.slots.associate { it.id to it.label },
                            inlineTips = inlineTips ?: pack.pack.walkthroughPresentation.inlineTips,
                            displayOrder = day.displayOrder,
                            artworkBackdrop = true,
                        )
                    }
                }
            }
        }
        compose.waitUntil(20_000) { fontReady }
        compose.waitForIdle()
    }

    private fun capture(name: String, dialog: Boolean = false) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/dayloop-ui")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = requireNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
        requireNotNull(resolver.openOutputStream(uri)).use {
            val target = if (dialog) compose.onNode(isDialog()) else compose.onRoot()
            check(target.captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }

    @Test fun classAnswerIsVisibleWithoutOpeningTips() {
        show("2009-04-18")
        compose.onNodeWithText("Answer: Middens.").assertIsDisplayed()
        compose.onNodeWithText("Tips").assertDoesNotExist()
        capture("p3r-inline-class-answer")
    }

    @Test fun twoHundredPercentTextKeepsFullWidthGuidanceAndFortyDpCommands() {
        show("2009-07-01", 2f)
        val label = compose.onNodeWithTag("task-label-0").performScrollTo().fetchSemanticsNode().boundsInRoot
        val guidance = compose.onNodeWithTag("task-guidance-0").performScrollTo().fetchSemanticsNode().boundsInRoot
        assertTrue("guidance uses the width below the controls", guidance.width > label.width)
        compose.onAllNodesWithContentDescription("Done")[0].performScrollTo()
            .assertWidthIsEqualTo(40.dp).assertHeightIsEqualTo(40.dp).performClick()
        assertEquals(0, markedIndex)
        assertEquals(StepMark.DONE, markedValue)
        compose.onAllNodesWithContentDescription("Done")[0].performClick()
        assertEquals(null, markedValue)
        compose.onNodeWithTag("task-guidance-0").performScrollTo()
        capture("p3r-inline-social-link-large-text")
    }

    @Test fun finalRescuePrecedesTheAfternoonWithoutChangingItsSavedKey() {
        show("2010-01-30")
        compose.onAllNodesWithContentDescription("Done")[0].performScrollTo().performClick()
        assertEquals(1, markedIndex)
        assertEquals(StepMark.DONE, markedValue)
        compose.onNodeWithTag("task-guidance-2").performScrollTo().assertIsDisplayed()
        capture("p3r-inline-final-tartarus")
    }

    @Test fun operationSectionsReadAtTwoHundredPercentWithoutMarkingTheTask() {
        show("2009-05-09", 2f)
        compose.onNodeWithText("Prepare · Priestess").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Summoned Tiaras").performScrollTo().assertIsDisplayed()
        assertEquals(null, markedIndex)
        val label = compose.onNodeWithTag("task-label-0").performScrollTo().fetchSemanticsNode().boundsInRoot
        val body = compose.onNodeWithTag("task-guidance-0-section-1-body").performScrollTo().fetchSemanticsNode().boundsInRoot
        assertTrue("battle advice uses the full reading width", body.width > label.width)
        capture("p3r-combat-priestess-large-text")
        compose.onAllNodesWithContentDescription("Done")[0].performScrollTo()
            .assertWidthIsEqualTo(40.dp).assertHeightIsEqualTo(40.dp).performClick()
        assertEquals(0, markedIndex)
        assertEquals(StepMark.DONE, markedValue)
    }

    @Test fun correctedGatekeeperAndItsPhysicalAdviceAreVisibleInline() {
        show("2009-10-01")
        compose.onNodeWithText("143F · Isolated Castle").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("task-guidance-3-section-4-body").assertTextContains("Use physical skills", substring = true)
        capture("p3r-combat-isolated-castle")
    }

    @Test fun sectionsWithoutATipRemainAccessibleThroughTheLegacyDialog() {
        show("2009-05-09", inlineTips = false)
        compose.onNodeWithTag("task-label-0").performScrollTo().performClick()
        compose.onNodeWithText("Prepare · Priestess").assertIsDisplayed()
        compose.onNodeWithText("Summoned Tiaras").performScrollTo().assertIsDisplayed()
        capture("p3r-combat-tips-dialog", dialog = true)
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithText("Prepare · Priestess").assertDoesNotExist()
        assertEquals(null, markedIndex)
    }
}
