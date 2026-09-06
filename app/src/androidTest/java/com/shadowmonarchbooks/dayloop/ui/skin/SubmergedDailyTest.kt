package com.shadowmonarchbooks.dayloop.ui.skin

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Real Android component fixtures. These are not captures of the full Today/navigation flow. */
class SubmergedDailyTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Volatile private var fontReady = false

    private fun asset(path: String) = context.assets.open(path).bufferedReader().use { it.readText() }
    private fun pack(slug: String) = LoadedPack(slug, requireNotNull(PackLoader.decodePack(asset("$slug/pack.json"))))

    private fun show(slug: String = "p3r", scale: Float = 1f, content: @Composable () -> Unit) {
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
        val folder = File(context.getExternalFilesDir(null), "ui-captures").apply { mkdirs() }
        File(folder, "$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun dayFixture(date: String, name: String, scale: Float = 1f, slug: String = "p3r") {
        val p = pack(slug)
        val days = requireNotNull(PackLoader.decodeWalkthrough(asset("$slug/walkthrough/${date.take(7)}.json"))).days
        val day = days.single { it.date == date }
        show(slug, scale) {
            SkinSectionHeader("Daily plan")
            Spacer(Modifier.height(12.dp))
            if (slug == "p3r") SubmergedDateHeader(date, date)
            val deadlines = requireNotNull(PackLoader.decodeDeadlines(asset("$slug/deadlines.json"))).deadlines
            nextDeadline(deadlines, date, p.calendar)?.let { (deadline, remaining) -> DeadlineBanner(deadline, remaining) }
            Spacer(Modifier.height(14.dp))
            TasksList(day.steps, { null }, { _, _ -> }, p.pack.stats.associate { it.id to it.label }, emptyMap(),
                slotLabels = p.pack.slots.associate { it.id to it.label })
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SkinTextActionButton("Back", {})
                SkinActionButton("End Day", {})
            }
        }
        capture(name)
        compose.onNodeWithText("End Day").performScrollTo().assertIsDisplayed()
        capture("$name-controls")
    }

    @Test fun schoolDay() = dayFixture("2009-04-21", "p3r-school")
    @Test fun freeDay() = dayFixture("2009-04-26", "p3r-free")
    @Test fun operationDay() = dayFixture("2009-05-09", "p3r-operation")
    @Test fun largeText() = dayFixture("2009-04-26", "p3r-large-text", 1.5f)

    @Test fun markToggleAndTipsRemainIndependent() {
        var selected: StepMark? = null
        val instruction = "Read the task guidance before choosing how to mark this action."
        show {
            var mark by remember { mutableStateOf<StepMark?>(null) }
            StepRow(0, Step(instruction, tip = "This is fixture guidance, not game data."), mark,
                { value -> mark = value.takeUnless { it == mark }; selected = mark }, emptyMap(), null)
        }
        compose.onNodeWithText("Done").performClick().assertIsSelected()
        assertEquals(StepMark.DONE, selected)
        compose.onNodeWithText("Done").performClick().assertIsNotSelected()
        assertEquals(null, selected)
        compose.onNodeWithText("Later").performClick().assertIsSelected()
        compose.onNodeWithText(instruction, substring = true).performClick()
        compose.onNodeWithText("This is fixture guidance, not game data.").assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
        assertEquals(StepMark.LATER, selected)
        compose.onNodeWithText("Skip").performClick().assertIsSelected()
        capture("p3r-task-marked")
    }
}
