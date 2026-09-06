package com.shadowmonarchbooks.dayloop.ui.skin

import android.content.ContentValues
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import android.provider.MediaStore
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.shadowmonarchbooks.dayloop.review.ReviewDependencies
import com.shadowmonarchbooks.dayloop.MainActivity
import com.shadowmonarchbooks.dayloop.data.formatDate
import com.shadowmonarchbooks.dayloop.data.progress.ProfileEntity
import com.shadowmonarchbooks.dayloop.progress.StepMark
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Real activity, navigation, ViewModel and Room/DataStore; no replacement screen content. */
class SubmergedAppFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val dependencies by lazy {
        EntryPointAccessors.fromApplication(context.applicationContext, ReviewDependencies::class.java)
    }
    private var scenario: ActivityScenario<MainActivity>? = null
    private var profileId = 0L

    /** Seed a separate test profile before launching; subsequent actions use the real UI. */
    private fun launch(date: String, slug: String = "p3r", scale: Float = 1f) {
        instrumentation.uiAutomation.executeShellCommand("settings put system font_scale $scale").close()
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
        compose.waitUntil(20_000) {
            compose.onAllNodesWithText("End day", ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("End day", ignoreCase = true).assertIsDisplayed()
    }

    @After fun close() {
        scenario?.close()
        instrumentation.uiAutomation.executeShellCommand("settings put system font_scale 1.0").close()
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        // PixelCopy waits for the Compose root's rendered frame. A raw device
        // screenshot can still show Android's starting window or a stale scroll.
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
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

    private fun dateIs(date: String) {
        val pack = dependencies.store().state.value.selected!!
        compose.waitUntil(15_000) {
            runBlocking { dependencies.db().profileDao().byId(profileId)?.clockDate == date }
        }
        compose.waitUntil(15_000) {
            compose.onAllNodesWithContentDescription(formatDate(date, pack.calendar)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription(formatDate(date, pack.calendar)).assertIsDisplayed()
    }

    private fun markIs(index: Int, mark: StepMark, date: String) {
        compose.waitUntil(10_000) {
            runBlocking {
                dependencies.repo().marksFor(profileId).first().any {
                    it.date == date && it.stepIndex == index && it.mark == mark.name
                }
            }
        }
    }

    private fun tab(label: String) = compose.onNode(
        hasText(label) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab),
    )

    @Test fun schoolDayNavigationAndSavedDayFlow() {
        launch("2009-04-21")
        dateIs("2009-04-21")
        // The opening composition must show an actionable task without a scroll.
        compose.onNodeWithText("Stay awake in class").assertIsDisplayed()
        compose.onAllNodesWithText("Later")[0].assertIsDisplayed()
        capture("p3r-app-school")
        compose.onAllNodesWithText("Later")[0].performScrollTo().performClick()
        markIs(0, StepMark.LATER, "2009-04-21")
        compose.onNodeWithText("End day", ignoreCase = true).performClick()
        dateIs("2009-04-22")
        markIs(0, StepMark.LATER, "2009-04-21")
        markIs(1, StepMark.SKIP, "2009-04-21")
        capture("p3r-app-next-day")
        compose.onNodeWithText("Back").performClick()
        dateIs("2009-04-21")
        tab("Calendar").performClick().assertIsSelected()
        tab("Today").performClick().assertIsSelected()
        dateIs("2009-04-21")
        scenario!!.recreate()
        dateIs("2009-04-21")
        compose.onAllNodesWithText("Later")[0].performScrollTo().assertIsSelected()
        capture("p3r-app-restored")
    }

    @Test fun largeTextControlsAndCheckAll() {
        launch("2009-04-26", scale = 1.5f)
        dateIs("2009-04-26")
        capture("p3r-app-large-text")
        compose.onNodeWithText("Check all").performScrollTo().assertIsDisplayed().performClick()
        compose.waitUntil(10_000) {
            runBlocking {
                val rows = dependencies.repo().marksFor(profileId).first()
                rows.size == 3 && rows.all { it.mark == StepMark.DONE.name }
            }
        }
        compose.onNodeWithText("Check all").assertIsNotEnabled()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Perfect day").fetchSemanticsNodes().isEmpty()
        }
        compose.onAllNodesWithText("Done")[2].performScrollTo().assertIsDisplayed()
        compose.onNode(hasScrollAction()).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy) {
            it(0f, 100_000f)
        }
        val taskBottom = compose.onAllNodesWithText("Done")[2].fetchSemanticsNode().boundsInRoot.bottom
        val railTop = compose.onNodeWithText("End day", ignoreCase = true).fetchSemanticsNode().boundsInRoot.top
        assertTrue("Last task must scroll above the pinned control rail", taskBottom <= railTop)
        capture("p3r-app-large-text-controls")
        compose.onNodeWithText("End day", ignoreCase = true).performClick()
        dateIs("2009-04-27")
    }

    @Test fun operationAndExamScreens() {
        launch("2009-05-09")
        dateIs("2009-05-09")
        capture("p3r-app-operation")
        scenario!!.close()
        scenario = null
        launch("2009-05-18")
        dateIs("2009-05-18")
        capture("p3r-app-exam")
    }

    @Test fun calendarBoundarySkipsNonPlayableSpan() {
        launch("2010-01-31")
        compose.onNodeWithText("End day", ignoreCase = true).performClick()
        dateIs("2010-03-04")
        capture("p3r-app-epilogue")
        compose.onNodeWithText("Back").performClick()
        dateIs("2010-01-31")
    }

    @Test fun p5rSameBuildControlCapture() {
        launch("2016-04-15", "p5r")
        capture("p5r-app-control")
        assertEquals("p5r", dependencies.store().state.value.selectedSlug)
        assertEquals("2016-04-15", runBlocking { dependencies.db().profileDao().byId(profileId)?.clockDate })
    }
}
