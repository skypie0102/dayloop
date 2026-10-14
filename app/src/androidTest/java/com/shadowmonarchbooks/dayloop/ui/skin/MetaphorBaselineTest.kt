package com.shadowmonarchbooks.dayloop.ui.skin

import android.content.ContentValues
import android.os.ParcelFileDescriptor
import android.provider.Settings
import android.util.Log
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import android.provider.MediaStore
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.shadowmonarchbooks.dayloop.review.ReviewDependencies
import com.shadowmonarchbooks.dayloop.MainActivity
import com.shadowmonarchbooks.dayloop.data.formatDate
import com.shadowmonarchbooks.dayloop.data.progress.ProfileEntity
import com.shadowmonarchbooks.dayloop.progress.StepMark
import com.shadowmonarchbooks.dayloop.progress.StepKey
import com.shadowmonarchbooks.dayloop.widget.WidgetEntryPoint
import com.shadowmonarchbooks.dayloop.widget.widgetLaunchIntent
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Real activity, navigation, ViewModel and Room/DataStore; no replacement screen content. */
abstract class MetaphorReviewFlow {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    protected val dependencies by lazy {
        EntryPointAccessors.fromApplication(context.applicationContext, ReviewDependencies::class.java)
    }
    protected var scenario: ActivityScenario<MainActivity>? = null
    protected var profileId = 0L
    private var expectedFontScale = 1f

    private fun setFontScale(scale: Float) {
        fun shell(command: String) = ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand(command),
        ).use { it.readBytes().toString(Charsets.UTF_8).trim() }
        // Use the active user's provider value: a cached Settings.System read can
        // remain stale while the preceding activity is being recreated/closed.
        shell("settings --user current put system font_scale $scale")
        compose.waitUntil(10_000) {
            shell("settings --user current get system font_scale").toFloatOrNull() == scale
        }
        assertEquals(scale, shell("settings --user current get system font_scale").toFloatOrNull())
        // launch() independently verifies the actual receiving window's fontScale.
    }

    private fun setMotionScale(scale: Int) {
        listOf("animator_duration_scale", "transition_animation_scale").forEach { key ->
            ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(
                "settings put global $key $scale")).use { it.readBytes() }
        }
    }

    /** Seed a separate test profile before launching; subsequent actions use the real UI. */
    protected fun launch(date: String, slug: String = "metaphor", scale: Float = 1f) {
        expectedFontScale = scale
        setFontScale(scale)
        val store = dependencies.store()
        val pack = store.state.value.packs.single { it.slug == slug }
        runBlocking {
            store.state.first { it.selectionReady }
            profileId = dependencies.db().profileDao().insert(ProfileEntity(
                packId = slug, name = "Android review", routeId = pack.routes.first().id,
                clockDate = date, contentVersion = pack.pack.contentVersion,
                createdAt = System.currentTimeMillis(),
            ))
            dependencies.repo().selectProfile(slug, profileId)
            dependencies.repo().selectPack(slug)
            store.state.first { it.selectedSlug == slug }
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
        // Application resources may retain the prior configuration while no activity exists.
        // Verify the actual window receiving the system configuration before interacting.
        compose.waitUntil(10_000) {
            var applied = false
            scenario!!.onActivity { applied = it.resources.configuration.fontScale == scale }
            applied
        }
        compose.waitUntil(20_000) {
            compose.onAllNodesWithText("End day", ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("End day", ignoreCase = true).assertIsDisplayed()
        // Room/DataStore may publish the selected profile before the richer UI
        // projection reaches it. Wait for that exact date before querying controls.
        if (slug == "p3r") {
            compose.waitUntil(20_000) {
                compose.onAllNodesWithContentDescription(formatDate(date, pack.calendar)).fetchSemanticsNodes().isNotEmpty()
            }
        } else {
            val day = requireNotNull(pack.day(pack.routes.first().id, date))
            val index = day.displayOrder.firstOrNull() ?: 0
            compose.waitUntil(20_000) {
                compose.onAllNodes(hasTestTag("task-label-$index") and
                    hasText(day.steps[index].label, substring = true, ignoreCase = true), useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
        }
    }

    @After fun close() {
        try {
            setMotionScale(0)
            // Apply the reset while a receiving window still exists. Closing first
            // can leave process resources at the prior scale for the next test.
            setFontScale(1f)
            scenario?.let { active ->
                compose.waitUntil(10_000) {
                    var restored = false
                    active.onActivity { restored = it.resources.configuration.fontScale == 1f }
                    restored
                }
            }
        } finally {
            scenario?.close()
            scenario = null
        }
    }

    protected fun capture(name: String, dialog: Boolean = false) {
        compose.waitForIdle()
        scenario!!.onActivity { activity ->
            assertTrue("Capture must use the requested font scale",
                activity.resources.configuration.fontScale == expectedFontScale)
        }
        // PixelCopy waits for the Compose root's rendered frame. A raw device
        // screenshot can still show Android's starting window or a stale scroll.
        val target = if (dialog) compose.onNodeWithTag("p3r-settings-dialog") else compose.onRoot()
        val bitmap = target.captureToImage().asAndroidBitmap()
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/dayloop-ui")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = requireNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
        requireNotNull(resolver.openOutputStream(uri)).use {
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        bitmap.recycle()
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }

    protected fun tab(label: String) = compose.onNode(hasContentDescription(label) and
        SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))

    protected fun unchangedClockAndMarks(date: String) {
        assertEquals(date, runBlocking { dependencies.db().profileDao().byId(profileId)?.clockDate })
        assertTrue(runBlocking { dependencies.repo().marksFor(profileId).first().isEmpty() })
    }
}

/** This same fixture also compiles against the unchanged pre-paint production commit. */
class MetaphorBaselineTest : MetaphorReviewFlow() {
    @Test fun originalShellAndFourDestinations() {
        launch("2100-06-12")
        capture("metaphor-before-today")
        tab("Calendar").performClick()
        compose.onNodeWithText("Jun 2100").assertIsDisplayed()
        capture("metaphor-before-calendar")
        compose.onNodeWithTag("calendar-date-2100-06-12").performScrollTo().performClick()
        capture("metaphor-before-source-day")
        compose.onNodeWithContentDescription("Back").performClick()
        tab("Followers").performClick()
        capture("metaphor-before-followers")
        tab("Achievements").performClick()
        capture("metaphor-before-achievements")
        unchangedClockAndMarks("2100-06-12")
    }

    @Test fun originalShellAtDoubleText() {
        launch("2100-06-12", scale = 2f)
        capture("metaphor-before-today-double-text")
        tab("Calendar").performClick()
        capture("metaphor-before-calendar-double-text")
        unchangedClockAndMarks("2100-06-12")
    }
}
