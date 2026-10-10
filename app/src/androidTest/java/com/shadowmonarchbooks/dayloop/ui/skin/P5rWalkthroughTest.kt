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

class P5rWalkthroughTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Volatile private var fontReady = false
    private var markedIndex: Int? = null
    private var markedValue: StepMark? = null

    private fun asset(path: String) = context.assets.open(path).bufferedReader().use { it.readText() }

    private fun show(date: String, scale: Float = 1f) {
        val pack = LoadedPack("p5r", requireNotNull(PackLoader.decodePack(asset("p5r/pack.json"))))
        val day = requireNotNull(PackLoader.decodeWalkthrough(asset("p5r/walkthrough/${date.take(7)}.json"))).days.single { it.date == date }
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                DayloopTheme(pack) {
                    val skin = LocalSkin.current
                    SideEffect { fontReady = skin.type.display?.family != null }
                    val marks = remember { mutableStateMapOf<Int, StepMark>() }
                    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                        .verticalScroll(rememberScrollState()).padding(16.dp)) {
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
                            inlineTips = pack.pack.walkthroughPresentation.inlineTips,
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

    private fun capture(name: String) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/dayloop-ui")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = requireNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
        requireNotNull(resolver.openOutputStream(uri)).use {
            check(compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }

    @Test fun dialogueAndCraftingAreReadableWithoutOpeningTips() {
        show("2016-04-23")
        compose.onNodeWithTag("task-guidance-2").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("TIPS").assertDoesNotExist()
        capture("p5r-inline-dialogue")
        compose.onNodeWithTag("task-guidance-3").performScrollTo().assertIsDisplayed()
        capture("p5r-inline-crafting")
    }

    @Test fun twoHundredPercentTextRetainsFullWidthGuidanceAndOriginalProgressKeys() {
        show("2016-04-23", 2f)
        val guidance = compose.onNodeWithTag("task-guidance-2").performScrollTo().fetchSemanticsNode().boundsInRoot
        val label = compose.onNodeWithTag("task-label-2").fetchSemanticsNode().boundsInRoot
        assertTrue("guidance must use the width beneath the controls", guidance.width > label.width)
        compose.onAllNodesWithText("Done")[2].performScrollTo().performClick()
        assertEquals(2, markedIndex)
        assertEquals(StepMark.DONE, markedValue)
        compose.onAllNodesWithText("Done")[2].performClick()
        assertEquals(null, markedValue)
        capture("p5r-large-text")
    }

    @Test fun appendedRequestKeepsItsOriginalKeyWhenPresentedBeforeNight() {
        show("2016-05-07")
        // Reading order is task indexes 0, 1, 3, 2. The third visible Done button
        // belongs to the appended request, not to the earlier stored night row.
        compose.onAllNodesWithText("Done")[2].performScrollTo().performClick()
        assertEquals(3, markedIndex)
        assertEquals(StepMark.DONE, markedValue)
        compose.onNodeWithTag("task-label-3").performScrollTo().assertIsDisplayed()
        capture("p5r-request-order")
    }
}
