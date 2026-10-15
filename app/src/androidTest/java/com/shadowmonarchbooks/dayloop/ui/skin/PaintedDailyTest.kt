package com.shadowmonarchbooks.dayloop.ui.skin

import android.content.ContentValues
import android.provider.MediaStore
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.shadowmonarchbooks.dayloop.data.nextDeadline
import com.shadowmonarchbooks.dayloop.data.LoadedPack
import com.shadowmonarchbooks.dayloop.pack.PackLoader
import com.shadowmonarchbooks.dayloop.pack.schema.Step
import com.shadowmonarchbooks.dayloop.progress.StepMark
import com.shadowmonarchbooks.dayloop.ui.components.DeadlineBanner
import com.shadowmonarchbooks.dayloop.ui.components.StepRow
import com.shadowmonarchbooks.dayloop.ui.components.TasksList
import com.shadowmonarchbooks.dayloop.ui.theme.DayloopTheme
import com.shadowmonarchbooks.dayloop.ui.month.PaintedMonthScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Real Android component fixtures. These are not captures of the full Today/navigation flow. */
class PaintedDailyTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Volatile private var fontReady = false

    private fun asset(path: String) = context.assets.open(path).bufferedReader().use { it.readText() }
    private fun pack(slug: String) = LoadedPack(slug, requireNotNull(PackLoader.decodePack(asset("$slug/pack.json"))))

    private fun show(slug: String = "metaphor", scale: Float = 1f, content: @Composable () -> Unit) {
        val loaded = pack(slug)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                DayloopTheme(loaded) {
                    val skin = LocalSkin.current
                    SideEffect { fontReady = skin.type.display?.family != null }
                    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                        .skinBackdrop(skin).verticalScroll(rememberScrollState()).padding(16.dp)) {
                        content()
                    }
                }
            }
        }
        compose.waitUntil(15_000) { fontReady }
        compose.waitForIdle()
    }

    private fun capture(name: String) {
        // UTP uninstalls the tested APK after the suite, deleting app-private storage.
        // Shared emulator media survives long enough for the workflow's adb export.
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/dayloop-ui")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = requireNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
        requireNotNull(resolver.openOutputStream(uri)).use {
            check(compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }

    @Test fun compactTabsAreNamedSizedAndStaticAtDoubleText() {
        var selectedRoute = "today"
        val items = listOf(SkinNavItem("today", "Today", Icons.Filled.Home),
            SkinNavItem("calendar", "Calendar", Icons.Filled.DateRange),
            SkinNavItem("achievements", "Achievements", Icons.Filled.Star),
            SkinNavItem("bonds", "Followers", Icons.Filled.Person))
        show(scale = 2f) {
            var route by remember { mutableStateOf("today") }
            PaintedTopBar("Achievements", false, {}, {}, true, {})
            Box(Modifier.width(180.dp).testTag("painted-narrow-tabs")) {
                PaintedBottomBar(items, route) { route = it; selectedRoute = it }
            }
        }
        compose.onNodeWithTag("painted-toolbar-title").assertHeightIsEqualTo(64.dp)
        compose.onNodeWithText("ACHIEVEMENTS").assertIsDisplayed()
        compose.onNodeWithText("Followers").assertDoesNotExist()
        compose.onNodeWithTag("painted-tab-icon-today", useUnmergedTree = true)
            .assertWidthIsEqualTo(23.dp).assertHeightIsEqualTo(23.dp)
        compose.onNodeWithContentDescription("Today").assertWidthIsAtLeast(48.dp).assertHeightIsEqualTo(64.dp).assertIsSelected()
        compose.onNodeWithContentDescription("Followers").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Followers").assertIsDisplayed().assertIsSelected()
        assertEquals("bonds", selectedRoute)
        compose.onNodeWithTag("painted-tab-icon-bonds", useUnmergedTree = true).assertWidthIsEqualTo(23.dp)
        compose.waitForIdle()
        val before = compose.onNodeWithTag("painted-narrow-tabs").captureToImage().asAndroidBitmap()
        compose.mainClock.advanceTimeBy(3_000)
        val after = compose.onNodeWithTag("painted-narrow-tabs").captureToImage().asAndroidBitmap()
        assertTrue("stationary chrome must not animate at idle", before.sameAs(after))
        before.recycle(); after.recycle()
        capture("metaphor-components-tabs-double-text")
    }

    @Test fun legacyLaterRemainsEditableWithCompactVisualsAndSeparateTouchTargets() {
        var saved: StepMark? = StepMark.LATER
        show(scale = 2f) {
            var mark by remember { mutableStateOf<StepMark?>(StepMark.LATER) }
            StepRow(0, Step("Read the full instruction before marking it.", tip = "Separate source guidance."), mark,
                { value -> mark = value.takeUnless { it == mark }; saved = mark }, emptyMap(), null)
        }
        compose.onNodeWithText("Later").assertDoesNotExist()
        compose.onNodeWithContentDescription("Done").assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp).assertIsNotSelected()
        compose.onNodeWithTag("painted-task-0-done", useUnmergedTree = true).assertWidthIsEqualTo(40.dp).assertHeightIsEqualTo(40.dp)
        compose.onNodeWithContentDescription("Done").performClick().assertIsSelected()
        assertEquals(StepMark.DONE, saved)
        compose.onNodeWithContentDescription("Skip").performClick().assertIsSelected()
        assertEquals(StepMark.SKIP, saved)
        compose.onNodeWithTag("task-label-0").performScrollTo().performClick()
        compose.onNodeWithText("Separate source guidance.").assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
        assertEquals(StepMark.SKIP, saved)
        compose.onNodeWithContentDescription("Skip").performClick().assertIsNotSelected()
        assertEquals(null, saved)
        capture("metaphor-components-task-double-text")
    }

    @Test fun settingsReplacesTheDisabledGearWithOneBackCommand() {
        var back = 0
        show(scale = 2f) {
            PaintedTopBar("Settings", true, { back++ }, {}, false, {})
            PaintedDateHeader("2100-06-12", "Watersday, Jun 12", "Watersday")
            SkinSectionHeader("Afternoon")
            SkinActionButton("End day", {}, largeLabel = true)
        }
        compose.onAllNodesWithContentDescription("Back").assertCountEquals(1)
        compose.onNodeWithContentDescription("Settings").assertDoesNotExist()
        compose.onNodeWithContentDescription("Back").performClick()
        assertEquals(1, back)
        compose.onNodeWithContentDescription("Watersday, Jun 12").assertIsDisplayed()
        compose.onNodeWithText("2100", substring = true).assertDoesNotExist()
        compose.onNodeWithText("End day").performScrollTo().assertHeightIsAtLeast(48.dp)
        capture("metaphor-components-header-date-double-text")
    }

    @Test fun fiveColumnCalendarKeepsItsDateTargetsAndPaperInspectorAtNarrowDoubleText() {
        val loaded = pack("metaphor")
        val days = requireNotNull(PackLoader.decodeWalkthrough(asset("metaphor/walkthrough/2100-06.json")))
            .days.associateBy { it.date }
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                DayloopTheme(loaded) {
                    val skin = LocalSkin.current
                    SideEffect { fontReady = skin.type.display?.family != null }
                    Box(Modifier.fillMaxHeight().width(280.dp).background(MaterialTheme.colorScheme.background)) {
                        PaintedMonthScreen(loaded, days, listOf("2100-06"), 0, "2100-06-12",
                            "100% Completion Route", "narrow-fixture", {}, {}, {})
                    }
                }
            }
        }
        compose.waitUntil(15_000) { fontReady }
        compose.onNodeWithTag("calendar-date-2100-06-02").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        compose.onNodeWithContentDescription("Flamesday").assertIsDisplayed()
        compose.onNodeWithContentDescription("Idlesday").assertIsDisplayed()
        capture("metaphor-m2-narrow-grid-double-text")
        compose.onNodeWithTag("calendar-date-2100-06-12").performScrollTo().performClick()
        compose.onNodeWithTag("painted-open-day").performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        compose.onNodeWithText("Open walkthrough", useUnmergedTree = true)
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        assertTrue("The command must wrap between words at 200% text", (0 until layout.lineCount).any {
            layout.layoutInput.text.text.substring(layout.getLineStart(it), layout.getLineEnd(it, visibleEnd = true))
                .contains("walkthrough")
        })
        capture("metaphor-m2-narrow-inspector-double-text")
    }
}
