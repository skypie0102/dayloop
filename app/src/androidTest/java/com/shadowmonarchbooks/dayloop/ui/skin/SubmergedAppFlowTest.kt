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
class SubmergedAppFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val dependencies by lazy {
        EntryPointAccessors.fromApplication(context.applicationContext, ReviewDependencies::class.java)
    }
    private var scenario: ActivityScenario<MainActivity>? = null
    private var profileId = 0L
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
    private fun launch(date: String, slug: String = "p3r", scale: Float = 1f) {
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
            val index = day.displayOrder.first()
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

    private fun capture(name: String, dialog: Boolean = false) {
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
        hasContentDescription(label) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab) and
            hasAnyAncestor(hasTestTag("bond-sections")).not(),
    )

    private fun openRequest(title: String) {
        compose.onNodeWithText("Search name or number").assertDoesNotExist()
        compose.onNodeWithTag("request-list").performScrollToNode(hasText(title))
        compose.onNodeWithText(title).performClick()
    }

    private fun requestCountIs(count: Int) {
        // Expanded details and section state now retain their scroll position.
        // Reveal the summary before querying a LazyColumn item that may be uncomposed.
        compose.onNodeWithTag("request-list").performScrollToIndex(0)
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("$count / 101 reported").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("$count / 101 reported").performScrollTo().assertIsDisplayed()
    }

    private fun openEpisodeChain(id: String) {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("episode-list").fetchSemanticsNodes().size == 1
        }
        compose.onNodeWithTag("episode-list").performScrollToNode(hasTestTag("episode-chain-$id"))
        // Wait for the transition/recomposition to settle before the touch query.
        // Duplicate transitional nodes can make the test framework traverse its
        // diagnostic tree on the instrumentation thread while the owner lays out.
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("episode-chain-$id").fetchSemanticsNodes().size == 1
        }
        compose.onNodeWithTag("episode-chain-$id").assertIsDisplayed().performClick()
    }

    private fun openEpisodeStep(id: String) {
        compose.onNodeWithTag("episode-detail").performScrollToNode(hasTestTag("episode-step-$id"))
        compose.onNodeWithTag("episode-step-$id").performClick()
    }

    private fun episodeChainCountIs(count: Int) {
        compose.onNodeWithTag("episode-detail").performScrollToIndex(0)
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("$count / 5 episodes complete").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("episode-chain-summary").assertTextEquals("$count / 5 episodes complete")
    }

    @Test fun searchAnswerSourceAndBackRetainQueryAtDoubleTextWithoutAwardingProgress() {
        launch("2009-05-19", scale = 2f)
        compose.onNodeWithContentDescription("Search").performClick()
        compose.onNodeWithText("Find a source day").assertIsDisplayed()
        compose.onNodeWithTag("p3r-search-input").performTextInput("An error in translation")
        compose.onNodeWithTag("p3r-search-input").performImeAction()
        val hit = "search-answer-2009-07-15"
        compose.onNodeWithTag("p3r-search-results").performScrollToNode(hasTestTag(hit))
        compose.onNodeWithTag(hit).assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        capture("p3r-search-answer-double-text")
        compose.onNodeWithTag(hit).performClick()
        compose.onNodeWithTag("answer-sheet-p3r.answers.exam.2009-07-15").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithTag(hit).assertIsDisplayed()
        compose.onNodeWithTag("p3r-search-input").assertTextContains("An error in translation")
        scenario!!.recreate()
        compose.onNodeWithTag(hit).assertIsDisplayed()
        compose.onNodeWithTag("p3r-search-input").assertTextContains("An error in translation")
        capture("p3r-search-restored-double-text")
        compose.onNodeWithContentDescription("Clear search").performClick()
        compose.onNodeWithText("Find a source day").assertIsDisplayed()
        compose.onNodeWithTag("p3r-search-input").performTextInput("zz-no-source-match")
        compose.onNodeWithTag("p3r-search-input").performImeAction()
        compose.onNodeWithText("No matches").assertIsDisplayed()
        capture("p3r-search-no-matches-double-text")
        compose.onNodeWithContentDescription("Back").performClick()
        dateIs("2009-05-19")
        assertTrue(runBlocking { dependencies.repo().marksFor(profileId).first().isEmpty() })
        assertTrue(runBlocking { dependencies.repo().earnedAchievements(profileId).first().isEmpty() })
    }

    @Test fun searchIncludesLaterSourceDatesBeyondTheOldSixResultLimit() {
        launch("2009-05-19")
        compose.onNodeWithContentDescription("Search").performClick()
        compose.onNodeWithTag("p3r-search-input").performTextInput("2009")
        compose.onNodeWithTag("p3r-search-input").performImeAction()
        capture("p3r-search-source-days")
        val hit = "search-day-2009-12-31"
        compose.onNodeWithTag("p3r-search-results").performScrollToNode(hasTestTag(hit))
        compose.onNodeWithTag(hit).assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithTag(hit).assertIsDisplayed()
        compose.onNodeWithTag("p3r-search-input").assertTextContains("2009")
        assertEquals("2009-05-19", runBlocking { dependencies.db().profileDao().byId(profileId)!!.clockDate })
    }

    @Test fun reloadPickerCancelAndWidgetPackReentryPreserveTheSavedRun() {
        launch("2009-05-19", scale = 2f)
        val originalId = profileId
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Change game · saves are kept").performScrollTo().performClick()
        compose.onNodeWithTag("p3r-game-picker").assertIsDisplayed()
        compose.onNodeWithTag("p3r-game-list").performScrollToNode(hasTestTag("game-choice-p3r"))
        compose.onNodeWithTag("game-choice-p3r").assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Current game"))
        capture("p3r-game-picker-double-text")
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithTag("settings-content").assertIsDisplayed()
        compose.onNodeWithText("Change game · saves are kept").performScrollTo().performClick()
        compose.onNodeWithTag("p3r-game-list").performScrollToNode(hasTestTag("game-choice-p5r"))
        compose.onNodeWithTag("game-choice-p5r").performClick()
        compose.waitUntil(20_000) { dependencies.store().state.value.selectedSlug == "p5r" }
        compose.waitUntil(20_000) {
            compose.onAllNodesWithTag("p3r-game-picker").fetchSemanticsNodes().isEmpty() &&
                compose.onAllNodesWithTag("p3r-toolbar-title").fetchSemanticsNodes().isEmpty() &&
                compose.onAllNodesWithText("End day", ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
        }
        instrumentation.runOnMainSync { context.startActivity(widgetLaunchIntent(context, "p3r")) }
        compose.waitUntil(20_000) { dependencies.store().state.value.selectedSlug == "p3r" }
        dateIs("2009-05-19")
        compose.onNodeWithTag("p3r-toolbar-title").assertTextEquals("TODAY")
        compose.onNodeWithTag("p3r-game-picker").assertDoesNotExist()
        compose.onNodeWithTag("settings-content").assertDoesNotExist()
        val snapshot = runBlocking { EntryPointAccessors.fromApplication(context.applicationContext,
            WidgetEntryPoint::class.java).snapshotter().snapshot() }
        assertEquals("p3r", snapshot.packSlug)
        assertEquals("2009-05-19", snapshot.date)
        assertEquals(0, snapshot.doneCount)
        capture("p3r-widget-return-today-double-text")
        instrumentation.runOnMainSync { context.startActivity(widgetLaunchIntent(context, "missing-guide")) }
        compose.waitForIdle()
        assertEquals("p3r", dependencies.store().state.value.selectedSlug)
        assertEquals(originalId, runBlocking { dependencies.repo().activeProfileId("p3r").first() })
        assertEquals("2009-05-19", runBlocking { dependencies.db().profileDao().byId(originalId)!!.clockDate })
        assertTrue(runBlocking { dependencies.repo().marksFor(originalId).first().isEmpty() })
        scenario!!.close(); scenario = null
        runBlocking {
            dependencies.repo().selectPack("p5r")
            dependencies.store().state.first { it.selectedSlug == "p5r" }
        }
        scenario = ActivityScenario.launch(widgetLaunchIntent(context, "p3r"))
        compose.waitUntil(20_000) { dependencies.store().state.value.selectedSlug == "p3r" }
        dateIs("2009-05-19")
        compose.onNodeWithTag("p3r-toolbar-title").assertTextEquals("TODAY")
        capture("p3r-widget-fresh-activity-double-text")
        scenario!!.recreate()
        dateIs("2009-05-19")
        assertEquals(originalId, runBlocking { dependencies.repo().activeProfileId("p3r").first() })
        assertTrue(runBlocking { dependencies.repo().marksFor(originalId).first().isEmpty() })
    }

    @Test fun mediaArtworkAndPlacementStayReadableAndRetainPositionAtDoubleText() {
        launch("2009-05-19", scale = 2f)
        val pack = dependencies.store().state.value.selected!!
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("${pack.media.size} images").performScrollTo().performClick()
        val marker = pack.media.single { it.id == "p3r.media.full-moon" }
        compose.onNodeWithTag("p3r-media-list").performScrollToNode(hasTestTag("media-${marker.id}"))
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("media-image-${pack.assetOf(marker)}").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Full Moon").assertIsDisplayed()
        capture("p3r-media-full-moon-double-text")
        val portrait = pack.media.single { it.id == "p3r.media.portrait-protagonist" }
        compose.onNodeWithTag("p3r-media-list").performScrollToNode(hasTestTag("media-${portrait.id}"))
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("media-image-${pack.assetOf(portrait)}").fetchSemanticsNodes().isNotEmpty() }
        scenario!!.recreate()
        compose.onNodeWithTag("media-${portrait.id}").assertIsDisplayed()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("media-image-${pack.assetOf(portrait)}").fetchSemanticsNodes().isNotEmpty() }
        capture("p3r-media-restored-portrait-double-text")
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithTag("settings-content").assertIsDisplayed()
        assertEquals("2009-05-19", runBlocking { dependencies.db().profileDao().byId(profileId)!!.clockDate })
        assertTrue(runBlocking { dependencies.repo().marksFor(profileId).first().isEmpty() })
    }

    @Test fun pickerAndMediaUseReloadPlanesAtDefaultTextWithoutChangingProgress() {
        launch("2009-05-19")
        val pack = dependencies.store().state.value.selected!!
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Change game · saves are kept").performScrollTo().performClick()
        compose.onNodeWithTag("p3r-game-list").performScrollToNode(hasTestTag("game-choice-p3r"))
        compose.onNodeWithTag("game-choice-p3r").assertIsDisplayed()
        capture("p3r-game-picker-normal")
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("${pack.media.size} images").performScrollTo().performClick()
        val marker = pack.media.single { it.id == "p3r.media.full-moon" }
        compose.onNodeWithTag("p3r-media-list").performScrollToNode(hasTestTag("media-${marker.id}"))
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("media-image-${pack.assetOf(marker)}").fetchSemanticsNodes().isNotEmpty() }
        capture("p3r-media-full-moon-normal")
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithContentDescription("Back").performClick()
        dateIs("2009-05-19")
        assertTrue(runBlocking { dependencies.repo().marksFor(profileId).first().isEmpty() })
    }

    @Test fun settingsProfilesAndDialogsStayNamedAtDoubleText() {
        launch("2009-05-19", scale = 2f)
        val original = profileId
        val other = runBlocking {
            dependencies.repo().setMark(original, StepKey("2009-05-19", 0), StepMark.DONE)
            dependencies.db().profileDao().insert(ProfileEntity(
                packId = "p3r", name = "A long saved profile name for enlarged text", routeId = "standard",
                clockDate = "2009-04-18", contentVersion = 19, createdAt = System.currentTimeMillis()))
        }
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithContentDescription("Settings").assertDoesNotExist()
        compose.onNodeWithText("Previous day").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("settings-profile-$original").performScrollTo()
            .assertIsSelected().assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        compose.onNodeWithTag("settings-profile-$other").performScrollTo().assertIsNotSelected()
        compose.onNodeWithContentDescription("Rename A long saved profile name for enlarged text")
            .performScrollTo().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithTag("p3r-settings-dialog").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performClick().performTextReplacement("Second review profile")
        compose.onNodeWithText("Cancel").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        compose.onNodeWithText("Rename", substring = false).assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        capture("p3r-settings-rename-keyboard-double-text", dialog = true)
        compose.onNodeWithText("Cancel").performClick()
        assertEquals("A long saved profile name for enlarged text", runBlocking { dependencies.db().profileDao().byId(other)!!.name })
        compose.onNodeWithContentDescription("Rename A long saved profile name for enlarged text")
            .performScrollTo().performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("Second review profile")
        compose.onNodeWithText("Rename", substring = false).performClick()
        compose.waitUntil(10_000) { runBlocking { dependencies.db().profileDao().byId(other)?.name == "Second review profile" } }
        compose.onNodeWithTag("settings-profile-$original").performScrollTo().assertIsSelected()
        compose.onNodeWithTag("settings-profile-$other").performScrollTo().assertIsNotSelected().performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasTestTag("settings-profile-$other") and isSelected()).fetchSemanticsNodes().isNotEmpty()
        }
        capture("p3r-settings-profiles-double-text")
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithContentDescription(formatDate("2009-04-18", dependencies.store().state.value.selected!!.calendar)).assertIsDisplayed()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithTag("settings-profile-$original").performScrollTo().performClick()
        compose.onNodeWithText("Reset profile").performScrollTo().performClick()
        compose.onNodeWithText("Reset profile?").assertIsDisplayed()
        compose.onNodeWithText("Reset").assertIsDisplayed()
        capture("p3r-settings-reset-double-text", dialog = true)
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithContentDescription("Delete Second review profile").performScrollTo().performClick()
        compose.onNodeWithText("Delete Second review profile?").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        assertEquals("2009-05-19", runBlocking { dependencies.db().profileDao().byId(original)!!.clockDate })
        assertEquals("2009-04-18", runBlocking { dependencies.db().profileDao().byId(other)!!.clockDate })
        assertEquals(1, runBlocking { dependencies.repo().marksFor(original).first().size })
        assertTrue(runBlocking { dependencies.repo().marksFor(other).first().isEmpty() })
        compose.onNodeWithContentDescription("Back").performClick()
        dateIs("2009-05-19")
    }

    @Test fun newProfileAndDeleteUseReloadDialogsWithoutChangingTheOriginalSave() {
        launch("2009-05-19")
        compose.onNodeWithContentDescription("Settings").performClick()
        capture("p3r-settings-normal-text")
        compose.onNodeWithText("+ New profile").performScrollTo().performClick()
        compose.onNodeWithText("New profile").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performTextReplacement("Created review profile")
        capture("p3r-settings-new-profile", dialog = true)
        compose.onNodeWithText("Create").performClick()
        compose.waitUntil(10_000) {
            runBlocking { dependencies.db().profileDao().all().any { it.name == "Created review profile" } }
        }
        val created = runBlocking { dependencies.db().profileDao().all().single { it.name == "Created review profile" } }
        assertEquals("2009-04-08", created.clockDate)
        assertEquals("standard", created.routeId)
        compose.onNodeWithTag("settings-profile-${created.id}").performScrollTo().assertIsNotSelected()
        compose.onNodeWithContentDescription("Delete Created review profile").performScrollTo().performClick()
        capture("p3r-settings-delete-profile", dialog = true)
        compose.onNodeWithText("Delete").performClick()
        compose.waitUntil(10_000) { runBlocking { dependencies.db().profileDao().byId(created.id) == null } }
        compose.onNodeWithTag("settings-profile-$profileId").performScrollTo().assertIsSelected()
        assertEquals("2009-05-19", runBlocking { dependencies.db().profileDao().byId(profileId)!!.clockDate })
        assertTrue(runBlocking { dependencies.repo().marksFor(profileId).first().isEmpty() })
    }

    @Test fun answerCollectionOpensAtItsSourceAndRetainsPositionAtDoubleText() {
        launch("2009-05-19", scale = 2f)
        val pack = dependencies.store().state.value.selected!!
        val source = "answer-sheet-p3r.answers.exam.2009-05-19"
        compose.onNodeWithTag(source).performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("p3r-toolbar-title").assertTextEquals("ANSWERS").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").assertIsDisplayed()
        compose.onNodeWithTag("p3r-answer-list").assertIsDisplayed()
        compose.onNodeWithTag(source).assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        capture("p3r-answer-collection-source-double-text")
        val next = "answer-sheet-p3r.answers.exam.2009-07-15"
        compose.onNodeWithTag("p3r-answer-list").performScrollToNode(hasTestTag(next))
        compose.onNodeWithTag("answer-body-p3r.answers.exam.2009-07-15-0", useUnmergedTree = true)
            .assertTextEquals("An error in translation").assertIsDisplayed()
        compose.onNode(hasText(formatDate("2009-07-15", pack.calendar)) and hasAnyAncestor(hasTestTag(next)),
            useUnmergedTree = true).assertIsDisplayed()
        capture("p3r-answer-collection-july-double-text")
        compose.onNodeWithTag(next).performClick()
        compose.onNodeWithTag(next).performScrollTo().assertIsDisplayed()
        assertEquals("2009-05-19", runBlocking { dependencies.db().profileDao().byId(profileId)!!.clockDate })
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithTag(next).assertIsDisplayed()
        scenario!!.recreate()
        compose.onNodeWithTag("p3r-answer-list").assertIsDisplayed()
        compose.onNodeWithTag(next).assertIsDisplayed()
        capture("p3r-answer-collection-restored-double-text")
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithTag(source).assertIsDisplayed()
        assertEquals("2009-05-19", runBlocking { dependencies.db().profileDao().byId(profileId)!!.clockDate })
        assertTrue(runBlocking { dependencies.repo().marksFor(profileId).first().isEmpty() })
        assertTrue(runBlocking { dependencies.repo().earnedAchievements(profileId).first().isEmpty() })
    }

    @Test fun linkedEpisodeSourceNavigationRetainsTheClockAndReversesChainProof() {
        launch("2009-05-12")
        compose.onAllNodesWithText("Done")[0].performScrollTo().performClick()
        markIs(0, StepMark.DONE, "2009-05-12")
        tab("Social Links").performScrollTo().performClick()
        compose.onNodeWithText("Linked Episodes").performClick()
        compose.onNodeWithTag("episode-summary").assertTextEquals("1 / 30 episodes complete")
        capture("p3r-episode-catalog")
        openEpisodeChain("p3r.episode.junpei")
        episodeChainCountIs(1)
        openEpisodeStep("p3r.episode.junpei.2")
        compose.onNodeWithTag("episode-detail").performScrollToNode(hasTestTag("episode-guide-p3r.episode.junpei.2"))
        compose.onNodeWithTag("episode-guide-p3r.episode.junpei.2").performClick()
        val pack = dependencies.store().state.value.selected!!
        val source = pack.episodes!!.events.single { it.id == "p3r.episode.event.junpei.2" }
        val index = pack.day("standard", source.date)!!.steps.indexOfFirst { it.label == source.labelContains }
        compose.onNodeWithText(source.labelContains).performScrollTo().assertIsDisplayed()
        assertEquals("2009-05-12", runBlocking { dependencies.db().profileDao().byId(profileId)!!.clockDate })
        capture("p3r-episode-route-guidance")
        compose.onAllNodesWithText("Done")[index].performScrollTo().performClick()
        markIs(index, StepMark.DONE, source.date)
        compose.onNodeWithContentDescription("Back").performClick()
        episodeChainCountIs(2)
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithTag("episode-list").performScrollToIndex(0)
        compose.onNodeWithTag("episode-summary").assertTextEquals("2 / 30 episodes complete")
        scenario!!.recreate()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("episode-summary").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("episode-summary").assertTextEquals("2 / 30 episodes complete")
        openEpisodeChain("p3r.episode.junpei")
        openEpisodeStep("p3r.episode.junpei.1")
        compose.onNodeWithTag("episode-detail").performScrollToNode(hasTestTag("episode-guide-p3r.episode.junpei.1"))
        compose.onNodeWithTag("episode-guide-p3r.episode.junpei.1").performClick()
        compose.onAllNodesWithText("Done")[0].performScrollTo().performClick()
        compose.waitUntil(10_000) { runBlocking { dependencies.repo().marksFor(profileId).first().none { it.date == "2009-05-12" && it.stepIndex == 0 } } }
        compose.onNodeWithContentDescription("Back").performClick()
        episodeChainCountIs(0)
        openEpisodeStep("p3r.episode.junpei.2")
        compose.onNodeWithTag("episode-status-p3r.episode.junpei.2", useUnmergedTree = true)
            .assertTextEquals("Recorded · earlier step unconfirmed")
        capture("p3r-episode-reversed-chain")
        assertEquals("2009-05-12", runBlocking { dependencies.db().profileDao().byId(profileId)!!.clockDate })
    }

    @Test fun linkedEpisodeFormPrerequisiteAndGuidanceAreReachableAtTwoHundredPercentText() {
        launch("2009-09-24", scale = 2f)
        val pack = dependencies.store().state.value.selected!!
        runBlocking {
            for (number in 1..3) {
                val event = pack.episodes!!.events.single { it.id == "p3r.episode.event.shinjiro.$number" }
                val index = pack.day("standard", event.date)!!.steps.indexOfFirst { it.label == event.labelContains }
                dependencies.repo().setMark(profileId, com.shadowmonarchbooks.dayloop.progress.StepKey(event.date, index), StepMark.DONE)
            }
        }
        tab("Social Links").performScrollTo().performClick()
        compose.onNodeWithText("Linked Episodes").performClick()
        openEpisodeChain("p3r.episode.shinjiro")
        episodeChainCountIs(3)
        openEpisodeStep("p3r.episode.shinjiro.form")
        compose.onNodeWithTag("episode-window-p3r.episode.shinjiro.form").performScrollTo().assertIsDisplayed()
        capture("p3r-episode-form-window-large-text")
        compose.onNodeWithTag("episode-solution-p3r.episode.shinjiro.form").performScrollTo().assertIsDisplayed()
        capture("p3r-episode-form-guidance-large-text")
        compose.onNodeWithTag("episode-prerequisite-p3r.episode.shinjiro.3").performScrollTo().performClick()
        compose.onNodeWithTag("episode-status-p3r.episode.shinjiro.3", useUnmergedTree = true).assertTextEquals("Completed")
        capture("p3r-episode-prerequisite-large-text")
        openEpisodeStep("p3r.episode.shinjiro.form")
        compose.onNodeWithTag("episode-detail").performScrollToNode(hasTestTag("episode-guide-p3r.episode.shinjiro.form"))
        compose.onNodeWithTag("episode-guide-p3r.episode.shinjiro.form").performClick()
        compose.onAllNodesWithText("Done")[1].performScrollTo().performClick()
        markIs(1, StepMark.DONE, "2009-09-24")
        compose.onNodeWithContentDescription("Back").performClick()
        episodeChainCountIs(3)
        compose.onNodeWithText("Next checkpoint · Episode 4").performScrollTo().assertIsDisplayed()
        assertEquals("2009-09-24", runBlocking { dependencies.db().profileDao().byId(profileId)!!.clockDate })
    }

    @Test fun schoolDayNavigationAndSavedDayFlow() {
        launch("2009-04-21")
        dateIs("2009-04-21")
        // The opening composition must show an actionable task without a scroll.
        compose.onNodeWithText("Stay awake in class").assertIsDisplayed()
        compose.onAllNodesWithText("Skip")[0].assertIsDisplayed()
        compose.onNodeWithText("Later").assertDoesNotExist()
        capture("p3r-app-school")
        compose.onAllNodesWithText("Skip")[0].performScrollTo().performClick()
        markIs(0, StepMark.SKIP, "2009-04-21")
        compose.onNodeWithText("End day", ignoreCase = true).performClick()
        dateIs("2009-04-22")
        markIs(0, StepMark.SKIP, "2009-04-21")
        markIs(1, StepMark.SKIP, "2009-04-21")
        capture("p3r-app-next-day")
        compose.onNodeWithText("Back").performClick()
        dateIs("2009-04-21")
        tab("Calendar").performClick().assertIsSelected()
        tab("Today").performClick().assertIsSelected()
        dateIs("2009-04-21")
        scenario!!.recreate()
        dateIs("2009-04-21")
        compose.onAllNodesWithText("Skip")[0].performScrollTo().assertIsSelected()
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
            compose.onAllNodesWithText("Perfect day", ignoreCase = true).fetchSemanticsNodes().isEmpty()
        }
        compose.onAllNodesWithText("Done")[2].performScrollTo().assertIsDisplayed()
        compose.onNode(hasScrollAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.ScrollBy) {
            it(0f, 100_000f)
        }
        val taskBottom = compose.onAllNodesWithText("Done")[2].fetchSemanticsNode().boundsInRoot.bottom
        val railTop = compose.onNodeWithText("End day", ignoreCase = true).fetchSemanticsNode().boundsInRoot.top
        assertTrue("Last task must scroll above the pinned control rail", taskBottom <= railTop)
        capture("p3r-app-large-text-controls")
        compose.onNodeWithText("End day", ignoreCase = true).performClick()
        dateIs("2009-04-27")
    }

    @Test fun requestStagesAreExplicitReversibleAndSaved() {
        launch("2009-05-10")
        tab("Requests").performClick().assertIsSelected()
        openRequest("Bring me a Muscle Drink")
        compose.onNodeWithText("Accepted").performScrollTo().performClick()
        compose.onNodeWithText("Ready to report").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            runBlocking { dependencies.repo().requestStages(profileId).first()["p3r.request.001"] == "ready" }
        }
        requestCountIs(0)
        capture("p3r-app-request-ready")
        compose.onAllNodesWithText("Reported").onLast().performScrollTo().performClick()
        compose.waitUntil(10_000) {
            runBlocking { dependencies.repo().requestStages(profileId).first()["p3r.request.001"] == "reported" }
        }
        scenario!!.recreate()
        tab("Requests").assertIsSelected()
        // The summary now scrolls with the list; explicitly reveal it after saved-scroll restoration.
        compose.onNodeWithTag("request-list").performScrollToIndex(0)
        compose.waitUntil(10_000) { compose.onAllNodesWithText("1 / 101 reported").fetchSemanticsNodes().isNotEmpty() }
        requestCountIs(1)
        capture("p3r-app-requests")
        // Clearing a confirmation reverses the count; it never checks a route task.
        compose.onAllNodesWithText("Reported").filter(hasClickAction()).onLast().performScrollTo().performClick()
        compose.waitUntil(10_000) { runBlocking { dependencies.repo().requestStages(profileId).first().isEmpty() } }
        assertTrue(runBlocking { dependencies.repo().marksFor(profileId).first().isEmpty() })
        val previousProfile = profileId
        runBlocking {
            dependencies.repo().setRequestStage(previousProfile, "p3r.request.001", "reported")
            dependencies.repo().setRequestStage(previousProfile, "p3r.rescue.050", "reward-claimed")
        }
        scenario!!.close()
        scenario = null
        launch("2009-05-10")
        assertTrue(runBlocking { dependencies.repo().requestStages(profileId).first().isEmpty() })
        assertEquals("reported", runBlocking { dependencies.repo().requestStages(previousProfile).first()["p3r.request.001"] })
        assertEquals("reward-claimed", runBlocking { dependencies.repo().requestStages(previousProfile).first()["p3r.rescue.050"] })
        val pack = dependencies.store().state.value.selected!!
        val calendar = pack.pack.calendar
        val seed = com.shadowmonarchbooks.dayloop.data.progress.PackSeed(
            pack.slug, pack.pack.contentVersion,
            com.shadowmonarchbooks.dayloop.progress.CalendarSpan(calendar.startDate, calendar.endDate, calendar.nonPlayableDates.toSet(), calendar.monthLengths),
            pack.routes.first().id,
        )
        runBlocking {
            dependencies.repo().setRequestStage(profileId, "p3r.request.001", "ready")
            dependencies.repo().setRequestStage(profileId, "p3r.rescue.056", "rescued")
            dependencies.repo().resetProfile(profileId, seed)
        }
        assertTrue(runBlocking { dependencies.repo().requestStages(profileId).first().isEmpty() })
        assertEquals("reported", runBlocking { dependencies.repo().requestStages(previousProfile).first()["p3r.request.001"] })
    }

    @Test fun achievementArtAndRequestTabKeepDailyAnswersReachable() {
        launch("2009-05-18")
        tab("Achievements").performClick()
        capture("p3r-app-achievement-art")
        tab("Requests").performClick()
        compose.onNodeWithText("REQUESTS").assertIsDisplayed()
        compose.onNodeWithText("ELIZABETH'S REQUESTS").assertDoesNotExist()
        capture("p3r-app-request-catalog")
        openRequest("Bring me pine resin")
        compose.onNodeWithText("Report by ${formatDate("2009-06-06", dependencies.store().state.value.selected!!.calendar)}")
            .performScrollTo().assertIsDisplayed()
        capture("p3r-app-request-deadline")
        tab("Today").performClick()
        compose.onNodeWithText("End day", ignoreCase = true).assertIsDisplayed()
    }

    @Test fun achievementProofNavigationRecordsOnlyItsSourceAndRemainsReversible() {
        launch("2010-01-08", scale = 1.5f)
        val pack = dependencies.store().state.value.selected!!
        val aeon = pack.achievementEvents.single { it.id == "p3r.event.social-all-unlocked" }
        val aeonIndex = pack.day("standard", aeon.date)!!.steps.indexOfFirst { it.label.contains(aeon.labelContains, true) }
        runBlocking { dependencies.repo().setMark(profileId, StepKey(aeon.date, aeonIndex), StepMark.DONE) }
        tab("Achievements").performClick()
        val id = "p3r.achievement.people-person"
        val list = compose.onNodeWithTag("achievement-list")
        list.performScrollToNode(hasTestTag("achievement-$id"))
        compose.onNodeWithTag("achievement-$id").performClick()
        val proof = "achievement-proof-$id"
        compose.onNodeWithText(pack.achievements.single { it.id == id }.tracking.prompt!!).assertDoesNotExist()
        list.performScrollToNode(hasTestTag(proof))
        compose.onNodeWithTag(proof).assertTextEquals("1 / 22 checkpoints recorded")
        val source = "achievement-checkpoint-p3r.event.social-start-fool"
        list.performScrollToNode(hasTestTag(source))
        compose.onNodeWithTag(source).assertIsDisplayed()
        capture("p3r-app-achievement-proof-large-text")
        compose.onNodeWithTag(source).performClick()
        compose.onNodeWithTag("task-label-1").assertTextContains("Fool reaches rank 1", substring = true)
        assertEquals("2010-01-08", runBlocking { dependencies.db().profileDao().byId(profileId)?.clockDate })
        assertEquals(1, runBlocking { dependencies.repo().marksFor(profileId).first().size })
        compose.onAllNodesWithContentDescription("Done")[1].performScrollTo().performClick()
        markIs(1, StepMark.DONE, "2009-04-18")
        capture("p3r-app-achievement-proof-source-day")
        compose.onNodeWithContentDescription("Back").performClick()
        list.performScrollToNode(hasTestTag(proof))
        compose.onNodeWithTag(proof).assertTextEquals("2 / 22 checkpoints recorded")
        list.performScrollToNode(hasText("Review all checkpoints"))
        compose.onNodeWithText("Review all checkpoints").performClick()
        list.performScrollToNode(hasTestTag(source))
        compose.onNodeWithTag(source).performClick()
        compose.onAllNodesWithContentDescription("Done")[1].performScrollTo().assertIsSelected().performClick()
        compose.waitUntil(10_000) { runBlocking { dependencies.repo().marksFor(profileId).first().none { it.date == "2009-04-18" } } }
        compose.onNodeWithContentDescription("Back").performClick()
        list.performScrollToNode(hasTestTag(proof))
        compose.onNodeWithTag(proof).assertTextEquals("1 / 22 checkpoints recorded")
        capture("p3r-app-achievement-proof-reversal")
        assertTrue(runBlocking { dependencies.repo().earnedAchievements(profileId).first().isEmpty() })
    }

    @Test fun completedWagesStayEditableAndExplicitConfirmationCanBeCleared() {
        launch("2009-12-29", scale = 1.5f)
        val id = "p3r.achievement.grindset-mindset"
        runBlocking {
            dependencies.repo().setAchievementCount(profileId, id, 50_001)
            dependencies.repo().setAchievementEarned(profileId, id, true)
        }
        tab("Achievements").performClick()
        val list = compose.onNodeWithTag("achievement-list")
        list.performScrollToNode(hasTestTag("achievement-$id"))
        compose.onNodeWithTag("achievement-$id").performClick()
        list.performScrollToNode(hasText("Clear confirmation"))
        compose.onNodeWithText("Clear confirmation").performClick()
        compose.waitUntil(10_000) { runBlocking { id !in dependencies.repo().earnedAchievements(profileId).first() } }
        list.performScrollToNode(hasSetTextAction())
        compose.onNode(hasSetTextAction()).assertIsEnabled().performTextReplacement("50000")
        compose.waitUntil(10_000) { runBlocking { dependencies.repo().achievementProgress(profileId).first().counts[id] == 50_000 } }
        // Returning to the detail after the sort change must retain the correction.
        list.performScrollToNode(hasTestTag("achievement-$id"))
        compose.onNodeWithTag("achievement-$id").assertTextContains("¥50000 / ¥50001 tracked", substring = true)
        list.performScrollToNode(hasSetTextAction())
        compose.onNode(hasSetTextAction()).assertIsEnabled()
        // Dismiss the numeric keyboard before reviewing the actual detail layout.
        instrumentation.uiAutomation.executeShellCommand("input keyevent 111").close()
        capture("p3r-app-achievement-wages-corrected")
        assertEquals("2009-12-29", runBlocking { dependencies.db().profileDao().byId(profileId)?.clockDate })
        assertTrue(runBlocking { dependencies.repo().marksFor(profileId).first().isEmpty() })
    }

    @Test fun theurgyChecklistAndShinjiroDeadlineWrapAtDoubleText() {
        launch("2009-10-01", scale = 2f)
        val id = "p3r.achievement.strength-of-hearts"
        val achievement = dependencies.store().state.value.selected!!.achievements.single { it.id == id }
        runBlocking {
            achievement.tracking.items.filter { it.id != "shinjiro" }.forEach {
                dependencies.repo().setAchievementChecklistItem(profileId, id, it.id, true)
            }
        }
        tab("Achievements").performClick()
        val list = compose.onNodeWithTag("achievement-list")
        val pinned = compose.onNodeWithTag("p3r-achievement-summary").getUnclippedBoundsInRoot()
        val viewport = list.getUnclippedBoundsInRoot()
        assertTrue("The achievement summary must not cover list controls", viewport.top >= pinned.bottom)
        list.performScrollToNode(hasTestTag("achievement-$id"))
        compose.onNodeWithTag("achievement-$id").assertTextContains("8 / 9 tracked", substring = true).performClick()
        compose.onNodeWithTag("achievement-$id").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Expanded"))
        list.performScrollToNode(hasText("Shinjiro · by 2009-10-03"))
        compose.onNodeWithText("Shinjiro · by 2009-10-03").assertIsDisplayed()
        capture("p3r-app-achievement-theurgy-deadline-double-text")
        assertTrue(runBlocking { dependencies.repo().earnedAchievements(profileId).first().isEmpty() })
        assertEquals(8, runBlocking { dependencies.repo().achievementProgress(profileId).first().checkedItems.getValue(id).size })
    }

    @Test fun achievementDetailsRequireExplicitConfirmationAtLargeText() {
        launch("2009-05-18", scale = 1.5f)
        tab("Achievements").performClick()
        val id = "p3r.achievement.birthday-present"
        val rowTag = "achievement-$id"
        compose.onNodeWithTag("achievement-list").performScrollToNode(hasTestTag(rowTag))
        compose.onNodeWithTag(rowTag).performClick()
        // Inspecting artwork or guidance must never award the achievement.
        assertTrue(runBlocking { id !in dependencies.repo().earnedAchievements(profileId).first() })
        compose.onNodeWithTag("achievement-list").performScrollToNode(hasText("Confirm earned"))
        compose.onNodeWithText("Confirm earned").assertIsDisplayed().performClick()
        compose.waitUntil(10_000) {
            runBlocking { id in dependencies.repo().earnedAchievements(profileId).first() }
        }
        // Earned rows move in the sorted list; the same entry stays expanded.
        compose.onNodeWithTag("achievement-list").performScrollToNode(hasText("Clear confirmation"))
        compose.onNodeWithText("Clear confirmation").assertIsDisplayed().performClick()
        compose.waitUntil(10_000) {
            runBlocking { id !in dependencies.repo().earnedAchievements(profileId).first() }
        }
    }

    @Test fun requestDetailsRemainReachableWithLargeText() {
        launch("2009-05-18", scale = 1.5f)
        tab("Requests").performClick()
        openRequest("Bring me pine resin")
        val solution = dependencies.store().state.value.selected!!.requests!!.requests
            .single { it.number == 12 }.solution!!
        compose.onNodeWithText("How to complete").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(solution).performScrollTo().assertIsDisplayed()
        // Reading instructions must not accept or report a request.
        assertTrue(runBlocking { dependencies.repo().requestStages(profileId).first().isEmpty() })
        compose.onNodeWithText("Report by ${formatDate("2009-06-06", dependencies.store().state.value.selected!!.calendar)}")
            .performScrollTo().assertIsDisplayed()
        capture("p3r-app-request-large-text")
        compose.onNodeWithText("Ready to report").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            runBlocking { dependencies.repo().requestStages(profileId).first()["p3r.request.012"] == "ready" }
        }
        compose.onNodeWithText("Walkthrough mentions").performScrollTo().assertIsDisplayed()
        capture("p3r-app-request-large-text-controls")
    }

    @Test fun exactHandInUpdatesRequestsAndUncheckingReversesIt() {
        Log.i("P3rReview", "Hand-in: launching May 10")
        launch("2009-05-10")
        Log.i("P3rReview", "Hand-in: activity ready")
        val pack = dependencies.store().state.value.selected!!
        val event = pack.requests!!.events.single { it.id == "p3r.request.reported.002" }
        val day = pack.day(pack.routes.first().id, event.date)!!
        val index = day.steps.indexOfFirst { it.label == event.labelContains }
        Log.i("P3rReview", "Hand-in: scrolling to task $index")
        compose.onAllNodesWithText("Done")[index].performScrollTo()
        Log.i("P3rReview", "Hand-in: marking task $index")
        compose.onAllNodesWithText("Done")[index].performClick()
        markIs(index, StepMark.DONE, event.date)
        Log.i("P3rReview", "Hand-in: opening Requests")
        tab("Requests").performClick()
        requestCountIs(1)
        openRequest("Retrieve the First Old Document")
        compose.onNodeWithText("Reported by the walkthrough. To reverse this, uncheck the linked hand-in task.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("0 / 1 guide preparation tasks checked").assertDoesNotExist()
        compose.onNodeWithTag("request-checkpoint-p3r.request.002").performScrollTo().assertIsDisplayed()
        capture("p3r-app-request-automatic")
        compose.onNodeWithTag("request-checkpoint-p3r.request.002").performClick()
        compose.onAllNodesWithText("Done")[index].performScrollTo().performClick()
        compose.onNodeWithContentDescription("Back").performClick()
        requestCountIs(0)
        assertEquals("2009-05-10", runBlocking { dependencies.db().profileDao().byId(profileId)!!.clockDate })
    }

    @Test fun sourceReadinessLinksReverseAtTwoHundredPercentTextWithoutAdvancingClock() {
        launch("2009-07-09", scale = 2f)
        compose.onAllNodesWithText("Done")[1].performScrollTo().performClick()
        markIs(1, StepMark.DONE, "2009-07-09")
        // A previously collected item is objective proof, independently of acceptance.
        runBlocking { dependencies.repo().setMark(profileId,
            com.shadowmonarchbooks.dayloop.progress.StepKey("2009-07-01", 1), StepMark.DONE) }
        markIs(1, StepMark.DONE, "2009-07-01")
        tab("Requests").performScrollTo().performClick()
        requestCountIs(0)
        val rootBounds = compose.onRoot().getUnclippedBoundsInRoot()
        val selectedTabBounds = tab("Requests").getUnclippedBoundsInRoot()
        assertTrue("selected icon tab must remain in the viewport at 200% text",
            selectedTabBounds.left >= rootBounds.left && selectedTabBounds.right <= rootBounds.right)
        openRequest("I want to eat some chilled Taiyaki")
        compose.onNodeWithText("Guide stage: Ready to report. Uncheck this source task to reverse it.")
            .performScrollTo().assertIsDisplayed()
        capture("p3r-source-ready-large-text")
        compose.onNodeWithTag("request-checkpoint-p3r.request.038").performScrollTo().performClick()
        val pack = dependencies.store().state.value.selected!!
        compose.onNodeWithContentDescription(formatDate("2009-07-01", pack.calendar)).assertIsDisplayed()
        compose.onAllNodesWithText("Done")[1].performScrollTo().performClick()
        compose.waitUntil(10_000) { runBlocking {
            dependencies.repo().marksFor(profileId).first().none { it.date == "2009-07-01" && it.stepIndex == 1 }
        } }
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Guide stage: In progress. Uncheck this source task to reverse it.")
            .performScrollTo().assertIsDisplayed()
        assertTrue(runBlocking { dependencies.repo().requestStages(profileId).first().isEmpty() })
        assertEquals("2009-07-09", runBlocking { dependencies.db().profileDao().byId(profileId)!!.clockDate })
        capture("p3r-source-readiness-reversed-large-text")
        tab("Today").performScrollTo().performClick()
        compose.onAllNodesWithText("Done")[2].performScrollTo().performClick()
        markIs(2, StepMark.DONE, "2009-07-09")
        tab("Requests").performScrollTo().performClick()
        requestCountIs(1)
        scenario!!.recreate()
        requestCountIs(1)
        assertEquals("2009-07-09", runBlocking { dependencies.db().profileDao().byId(profileId)!!.clockDate })
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

    @Test fun rescueVisitAndPoliceRewardHaveIndependentProgressAndUndo() {
        launch("2009-06-27")
        compose.onAllNodesWithText("Done")[2].performScrollTo().performClick()
        markIs(2, StepMark.DONE, "2009-06-27")
        tab("Requests").performClick()
        compose.onNodeWithText("Missing persons").performScrollTo().performClick()
        compose.onNodeWithText("3 / 20 rescued").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("0 rewards claimed").assertIsDisplayed()
        capture("p3r-rescue-catalog")
        compose.onNodeWithTag("rescue-list").performScrollToNode(hasText("50F · Ayako Yoshimoto"))
        compose.onNodeWithText("50F · Ayako Yoshimoto").performClick()
        compose.onNodeWithTag("rescue-reward-p3r.rescue.050").performScrollTo().assertTextEquals("Reward: Recarm skill card")
        assertTrue(runBlocking { dependencies.repo().requestStages(profileId).first().isEmpty() })
        compose.onNodeWithTag("rescue-confirm-p3r.rescue.050-reward-claimed").performScrollTo().performClick()
        compose.waitUntil(10_000) { runBlocking { dependencies.repo().requestStages(profileId).first()["p3r.rescue.050"] == "reward-claimed" } }
        capture("p3r-rescue-reward-confirmed")
        scenario!!.recreate()
        compose.onNodeWithTag("rescue-list").performScrollToIndex(0)
        compose.onNodeWithText("3 / 20 rescued").assertIsDisplayed()
        compose.onNodeWithText("1 reward claimed").assertIsDisplayed()
        compose.onNodeWithTag("rescue-checkpoint-p3r.rescue.050").performScrollTo().performClick()
        compose.onAllNodesWithText("Done")[2].performScrollTo().performClick()
        compose.waitUntil(10_000) { runBlocking { dependencies.repo().marksFor(profileId).first().none { it.date == "2009-06-27" && it.stepIndex == 2 } } }
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithTag("rescue-list").performScrollToIndex(0)
        compose.onNodeWithText("1 / 20 rescued").assertIsDisplayed()
        // This person's independent reward confirmation survives reversing the group task.
        compose.onNodeWithTag("rescue-confirm-p3r.rescue.050-reward-claimed").performScrollTo().performClick()
        compose.waitUntil(10_000) { runBlocking { dependencies.repo().requestStages(profileId).first().isEmpty() } }
        compose.onNodeWithTag("rescue-list").performScrollToIndex(0)
        compose.onNodeWithText("0 / 20 rescued").assertIsDisplayed()
        assertEquals("2009-06-27", runBlocking { dependencies.db().profileDao().byId(profileId)!!.clockDate })
        capture("p3r-rescue-visit-reversed")
    }

    @Test fun rescueWindowsAndSocialLinkConsequencesRemainReadableAtLargeText() {
        launch("2009-10-01", scale = 2f)
        tab("Requests").performScrollTo().performClick()
        compose.onNodeWithText("Missing persons").performScrollTo().performClick()
        compose.onNodeWithText("Available").performScrollTo().performClick()
        compose.onNodeWithText("3 shown").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("rescue-list").performScrollToNode(hasText("120F · Bunkichi Kitamura"))
        compose.onNodeWithText("120F · Bunkichi Kitamura").performClick()
        compose.onNodeWithText("Rescue Bunkichi before the cutoff to keep the Hierophant Social Link available.")
            .performScrollTo().assertIsDisplayed()
        capture("p3r-rescue-social-link-large-text")
        compose.onNodeWithTag("rescue-confirm-p3r.rescue.120-rescued").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("rescue-confirm-p3r.rescue.120-reward-claimed").performScrollTo().assertIsDisplayed()
        assertTrue(runBlocking { dependencies.repo().requestStages(profileId).first().isEmpty() })
        capture("p3r-rescue-controls-large-text")
        compose.onNodeWithTag("rescue-list").performScrollToIndex(0)
        compose.onNodeWithText("Elizabeth").performScrollTo().performClick()
        openRequest("Bring me a Muscle Drink")
        compose.onNodeWithTag("request-list").performScrollToIndex(0)
        compose.onNodeWithText("Missing persons").performScrollTo().performClick()
        // Switching catalog sections retains the filter, expanded person and scroll state.
        compose.onNodeWithTag("rescue-list").performScrollToNode(hasText("120F · Bunkichi Kitamura"))
        compose.onNodeWithText("Rescue Bunkichi before the cutoff to keep the Hierophant Social Link available.")
            .performScrollTo().assertIsDisplayed()
    }

    @Test fun requestMetadataAndPrerequisiteNavigationDoNotAwardProgress() {
        launch("2009-11-17")
        tab("Requests").performClick()
        openRequest("I’d Like to Try Oden Juice")
        compose.onNodeWithTag("request-item-window-p3r.request.096").performScrollTo().assertIsDisplayed()
        capture("p3r-request-kyoto-window")
        openRequest("I’d Like to Visit your Room")
        compose.onNodeWithTag("request-challenge-p3r.request.081").performScrollTo().assertTextEquals("0 / 185 challenge points recorded")
        compose.onNodeWithTag("request-prerequisite-p3r.request.080").performScrollTo().performClick()
        compose.onNodeWithTag("request-reward-p3r.request.080").performScrollTo().assertTextEquals("Reward: Sorcerer's Mark")
        assertTrue(runBlocking { dependencies.repo().requestStages(profileId).first().isEmpty() })
        assertTrue(runBlocking { dependencies.repo().marksFor(profileId).first().isEmpty() })
        capture("p3r-request-prerequisite")
    }

    @Test fun calendarBoundarySkipsNonPlayableSpan() {
        launch("2010-01-31")
        compose.onNodeWithText("End day", ignoreCase = true).performClick()
        dateIs("2010-03-04")
        capture("p3r-app-epilogue")
        compose.onNodeWithText("Back").performClick()
        dateIs("2010-01-31")
    }

    @Test fun calendarBrowseOpenAndReturn() {
        launch("2009-04-21")
        tab("Calendar").performClick()
        compose.onNodeWithTag("p3r-date-2009-04-01").assertIsNotEnabled()
        compose.onNodeWithTag("p3r-date-2009-04-21").assertIsEnabled()
        capture("p3r-app-calendar-april")
        // A gesture across the date grid must change the month, not pan its cells.
        compose.onNodeWithTag("p3r-month-grid").performTouchInput { swipeLeft() }
        compose.onNodeWithTag("p3r-month-heading").assertContentDescriptionEquals("May 2009")
        compose.onNodeWithTag("p3r-date-2009-05-09").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Full moon, Calendar event"))
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("p3r-full-moon-2009-05-09", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("p3r-full-moon-2009-05-09", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("p3r-month-grid").performTouchInput { swipeRight() }
        compose.onNodeWithTag("p3r-month-heading").assertContentDescriptionEquals("April 2009")
        compose.onNodeWithContentDescription("Next month").performClick()
        capture("p3r-app-calendar-may")
        compose.onNodeWithTag("p3r-date-2009-05-09").performClick()
        compose.onNodeWithContentDescription("Back").performClick()
        tab("Calendar").assertIsSelected()
        compose.onNodeWithTag("p3r-month-heading").assertContentDescriptionEquals("May 2009")
        assertEquals("2009-04-21", runBlocking { dependencies.db().profileDao().byId(profileId)?.clockDate })
        capture("p3r-app-calendar-returned")
        tab("Social Links").performClick()
        capture("p3r-app-social-links-audit")
    }

    @Test fun combatGuidanceIsSharedByTodayAndDayAtLargeTextWithoutAdvancingProgress() {
        launch("2009-05-09", scale = 2f)
        compose.onNodeWithText("Prepare · Priestess").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Summoned Tiaras").performScrollTo().assertIsDisplayed()
        assertTrue(runBlocking { dependencies.repo().marksFor(profileId).first().isEmpty() })
        assertEquals("2009-05-09", runBlocking { dependencies.db().profileDao().byId(profileId)?.clockDate })
        capture("p3r-app-combat-today-large-text")
        compose.onAllNodesWithContentDescription("Done")[0].performScrollTo()
            .assertWidthIsEqualTo(androidx.compose.ui.unit.Dp(40f))
            .assertHeightIsEqualTo(androidx.compose.ui.unit.Dp(40f)).performClick()
        markIs(0, StepMark.DONE, "2009-05-09")
        tab("Calendar").performClick()
        compose.onNodeWithTag("p3r-date-2009-05-09").performClick()
        compose.onNodeWithText("Fight · Priestess").performScrollTo().assertIsDisplayed()
        capture("p3r-app-combat-day-large-text")
        compose.onAllNodesWithContentDescription("Done")[0].performScrollTo().assertIsSelected().performClick()
        compose.waitUntil(10_000) { runBlocking { dependencies.repo().marksFor(profileId).first().isEmpty() } }
        assertEquals("2009-05-09", runBlocking { dependencies.db().profileDao().byId(profileId)?.clockDate })
    }

    @Test fun calendarHeadingAndGridStayUsableAtDoubleTextWithIconTabs() {
        launch("2009-05-18", scale = 2f)
        tab("Calendar").performClick().assertIsSelected()
        listOf("Today", "Calendar", "Achievements", "Social Links", "Requests").forEach { label ->
            tab(label).assertIsDisplayed().assertHasClickAction()
        }
        compose.onNodeWithText("CALENDAR").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        repeat(7) { compose.onNodeWithContentDescription("Next month").performClick() }
        compose.onNodeWithTag("p3r-month-heading").assertContentDescriptionEquals("December 2009")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        compose.onNodeWithContentDescription("Previous month").assertIsDisplayed()
        compose.onNodeWithContentDescription("Next month").assertIsDisplayed()
        compose.onNodeWithContentDescription("Sunday").assertIsDisplayed()
        val grid = compose.onNodeWithTag("p3r-month-grid").fetchSemanticsNode().config[SemanticsProperties.CollectionInfo]
        assertEquals(5, grid.rowCount)
        assertEquals(7, grid.columnCount)
        val position = compose.onNodeWithTag("p3r-date-2009-12-31").fetchSemanticsNode().config[SemanticsProperties.CollectionItemInfo]
        assertEquals(4, position.rowIndex)
        assertEquals(4, position.columnIndex)
        capture("p3r-calendar-december-double-text-icons")
        compose.onNodeWithTag("p3r-date-2009-12-31").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithTag("p3r-month-heading").assertContentDescriptionEquals("December 2009")
        assertEquals("2009-05-18", runBlocking { dependencies.db().profileDao().byId(profileId)!!.clockDate })
        assertTrue(runBlocking { dependencies.repo().marksFor(profileId).first().isEmpty() })
    }

    @Test fun socialLinkRankAndListPositionReturnAtDoubleTextWithMatchedArt() {
        launch("2009-05-18", scale = 2f)
        tab("Social Links").performClick()
        val row = "bond-p3r.bond.empress"
        compose.onNodeWithTag("bond-list").performScrollToNode(hasTestTag(row))
        val before = compose.onNodeWithTag(row).getUnclippedBoundsInRoot().top.value
        compose.onNodeWithTag(row).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Rank 0 of 10")).performClick()
        compose.onNodeWithText("Empress").assertIsDisplayed()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        compose.onNodeWithText("Mitsuru Kirijo").assertIsDisplayed()
        compose.onNodeWithContentDescription("Rank 0 of 10").assertIsDisplayed()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("bond-portrait-p3r.bond.empress", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("bond-portrait-p3r.bond.empress", useUnmergedTree = true).assertIsDisplayed()
        capture("p3r-links-empress-double-text")
        compose.onNodeWithTag("bond-detail").performScrollToNode(hasTestTag("bond-rank-3"))
        compose.onNodeWithTag("bond-rank-3").performClick().assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Expanded"))
        val date = dependencies.store().state.value.selected!!.bonds.single { it.id == "p3r.bond.empress" }
            .ranks.single { it.rank == 3 }.scheduledFor!!
        compose.onNodeWithText("Open walkthrough").performScrollTo().performClick()
        val pack = dependencies.store().state.value.selected!!
        compose.onNodeWithContentDescription(formatDate(date, pack.calendar)).assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithTag("bond-rank-3").assertIsSelected()
        compose.onNodeWithTag("bond-guidance-3").assertExists()
        capture("p3r-links-rank-returned-double-text")
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithTag(row).assertIsDisplayed().assertIsSelected()
        assertEquals(before, compose.onNodeWithTag(row).getUnclippedBoundsInRoot().top.value, 1f)
        compose.onNodeWithText("Linked Episodes").performClick()
        compose.onNode(hasText("Social Links") and hasAnyAncestor(hasTestTag("bond-sections"))).performClick()
        compose.onNodeWithTag(row).assertIsDisplayed().assertIsSelected()
        capture("p3r-links-list-returned-double-text")
        assertEquals("2009-05-18", runBlocking { dependencies.db().profileDao().byId(profileId)!!.clockDate })
        assertTrue(runBlocking { dependencies.repo().marksFor(profileId).first().isEmpty() })
    }

    @Test fun calendarAndNavigationAtLargeText() {
        launch("2009-05-18", scale = 1.5f)
        tab("Calendar").performClick()
        compose.onNodeWithTag("p3r-date-2009-05-18").assertIsDisplayed()
        tab("Achievements").assertIsDisplayed()
        tab("Social Links").assertIsDisplayed()
        tab("Requests").assertIsDisplayed()
        capture("p3r-app-calendar-large-text")
        compose.onNodeWithContentDescription("Next month").performClick()
        compose.onNodeWithTag("p3r-month-heading").assertContentDescriptionEquals("June 2009")
        compose.onNodeWithTag("p3r-date-2009-06-06").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Back").performClick()
        capture("p3r-app-calendar-large-text-returned")
    }

    @Test fun tabSwitchesRestoreRequestDetailAndBackReturnsToTodayAtDoubleText() {
        launch("2009-05-18", scale = 2f)
        tab("Requests").performScrollTo().performClick()
        openRequest("Bring me pine resin")
        val solution = "request-solution-p3r.request.012"
        compose.onNodeWithTag("request-list").performScrollToNode(hasTestTag(solution))
        compose.onNodeWithTag(solution).assertIsDisplayed()
        val before = compose.onNodeWithTag(solution).getUnclippedBoundsInRoot()
        repeat(3) {
            tab("Calendar").performScrollTo().performClick()
            tab("Achievements").performScrollTo().performClick()
            tab("Requests").performScrollTo().performClick()
            compose.onNodeWithTag(solution).assertIsDisplayed()
        }
        val after = compose.onNodeWithTag(solution).getUnclippedBoundsInRoot()
        assertEquals("Returning must retain the reading position", before.top.value, after.top.value, 1f)
        val tabBounds = tab("Requests").getUnclippedBoundsInRoot()
        val barBounds = compose.onNodeWithTag("p3r-bottom-navigation").getUnclippedBoundsInRoot()
        assertTrue("The selected icon tab fits", tabBounds.left >= barBounds.left && tabBounds.right <= barBounds.right)
        capture("p3r-app-tabs-restored-request-double-text")
        scenario!!.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("End day", ignoreCase = true).assertIsDisplayed()
        dateIs("2009-05-18")
        assertTrue(runBlocking { dependencies.repo().marksFor(profileId).first().isEmpty() })
    }

    @Test fun animatedDayAdvanceKeepsCelebrationSeparateAndSkipsToOnlyOneNextDay() {
        setMotionScale(1)
        launch("2009-05-18")
        compose.onNodeWithText("Check all", ignoreCase = true).performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("submerged-perfect-day").fetchSemanticsNodes().isNotEmpty()
        }
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("End day", ignoreCase = true).performClick()
        compose.mainClock.advanceTimeBy(SkinFxTiming.ADVANCE_COVER_MS.toLong() + 32)
        compose.onNodeWithTag("submerged-day-complete").assertExists()
        compose.onNodeWithTag("submerged-perfect-day").assertDoesNotExist()
        capture("p3r-app-motion-day-complete")
        compose.onNodeWithTag("day-advance").performClick()
        compose.mainClock.advanceTimeBy(3_000)
        compose.mainClock.autoAdvance = true
        dateIs("2009-05-19")
        compose.onNodeWithTag("submerged-day-complete").assertDoesNotExist()
        compose.onNodeWithTag("submerged-perfect-day").assertDoesNotExist()
        capture("p3r-app-motion-next-day")
    }

    @Test fun p5rSourceReadingOrderAndGuidanceStaySavedAcrossTodayAndDay() {
        launch("2016-05-07", "p5r")
        compose.onNodeWithTag("task-guidance-3").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("TIPS").assertDoesNotExist()
        // The third displayed task was appended at stored index 3, after Night.
        compose.onAllNodesWithContentDescription("Done")[2].performScrollTo().performClick()
        markIs(3, StepMark.DONE, "2016-05-07")
        assertTrue(runBlocking { dependencies.repo().marksFor(profileId).first().none { it.stepIndex == 2 } })
        val todayGuidance = compose.onNodeWithTag("task-guidance-3").performScrollTo().fetchSemanticsNode().boundsInRoot
        val endDayTop = compose.onNodeWithText("End day", ignoreCase = true).fetchSemanticsNode().boundsInRoot.top
        assertTrue("guidance must scroll above the pinned day controls", todayGuidance.bottom <= endDayTop)
        capture("p5r-app-today-request")
        scenario!!.recreate()
        compose.waitUntil(20_000) {
            compose.onAllNodesWithText("End day", ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithContentDescription("Done")[2].performScrollTo().assertIsSelected()
        compose.onNodeWithContentDescription("Calendar").performClick()
        capture("p5r-app-calendar-entry")
        compose.onNodeWithTag("calendar-date-2016-05-07").performScrollTo().performClick()
        capture("p5r-app-day-entry")
        compose.onRoot().printToLog("P5rReview-Day")
        // Wait until navigation has composed the destination's task list.
        compose.waitUntil(20_000) {
            compose.onAllNodesWithTag("task-guidance-3").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("task-guidance-3").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Done")[2].performScrollTo().assertIsSelected()
        val dayGuidance = compose.onNodeWithTag("task-guidance-3").performScrollTo().fetchSemanticsNode().boundsInRoot
        val browseTop = compose.onNodeWithContentDescription("Next authored day").fetchSemanticsNode().boundsInRoot.top
        assertTrue("guidance must scroll above the pinned browse controls", dayGuidance.bottom <= browseTop)
        capture("p5r-app-day-request")
        compose.onAllNodesWithContentDescription("Done")[2].performScrollTo().performClick()
        compose.waitUntil(10_000) {
            runBlocking { dependencies.repo().marksFor(profileId).first().none { it.date == "2016-05-07" && it.stepIndex == 3 } }
        }
        val nextDay = dependencies.store().state.value.selected!!.day("standard", "2016-05-08")!!
        val firstLabel = nextDay.steps[nextDay.displayOrder.first()].label
        compose.onNodeWithContentDescription("Next authored day").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText(firstLabel).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("task-label-0").assertIsDisplayed()
        capture("p5r-app-next-day-browse")
        assertEquals("2016-05-07", runBlocking { dependencies.db().profileDao().byId(profileId)?.clockDate })
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithContentDescription("Today").performClick()
        compose.onAllNodesWithContentDescription("Done")[2].performScrollTo().assertIsNotSelected()
        compose.onNodeWithText("End day", ignoreCase = true).performClick()
        compose.waitUntil(15_000) {
            runBlocking { dependencies.db().profileDao().byId(profileId)?.clockDate == "2016-05-08" }
        }
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText(firstLabel).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("task-label-0").assertIsDisplayed()
        capture("p5r-app-next-day-start")
    }

    @Test fun p5rMementosReceiptInvestigationAndClearRemainReversible() {
        launch("2016-05-26", "p5r", 2f)
        val title = "If Cats Disappeared from the City"
        val id = "p5r.request.if-cats-disappeared-from-the-city"
        val pack = dependencies.store().state.value.selected!!
        fun openTracker() {
            compose.onNodeWithContentDescription("Mementos Requests").performClick()
            compose.onNodeWithTag("mementos-request-list").performScrollToNode(hasText(title))
        }
        fun status(text: String) {
            compose.onNodeWithTag("mementos-request-list").performScrollToNode(hasText(title))
            compose.onNodeWithTag(id).assertTextContains(text)
        }
        fun mark(date: String, index: Int) {
            val day = pack.day("standard", date)!!
            compose.waitUntil(15_000) { compose.onAllNodesWithTag("task-label-$index").fetchSemanticsNodes().isNotEmpty() }
            val position = day.displayOrder.indexOf(index)
            compose.onAllNodesWithContentDescription("Done")[position].performScrollTo().performClick()
        }
        openTracker()
        status("TO RECEIVE")
        capture("p5r-mementos-receipt-large-text")
        compose.onNodeWithTag(id).performClick()
        mark("2016-05-26", 1)
        markIs(1, StepMark.DONE, "2016-05-26")
        compose.onNodeWithContentDescription("Back").performClick()
        status("PREPARATION")
        compose.onNodeWithTag(id).performClick()
        compose.onNodeWithText("Listen to the woman and girl near Sojiro's house").assertExists()
        compose.onNodeWithTag("task-guidance-1").performScrollTo().assertTextContains("man on the bench", substring = true)
        mark("2016-05-28", 1)
        markIs(1, StepMark.DONE, "2016-05-28")
        compose.onNodeWithContentDescription("Back").performClick()
        status("AVAILABLE")
        capture("p5r-mementos-available-large-text")
        compose.onNodeWithTag(id).performClick()
        val clearIndex = pack.day("standard", "2016-06-02")!!.steps.indexOfFirst { it.label == "Complete request: $title" }
        assertTrue(clearIndex >= 0)
        mark("2016-06-02", clearIndex)
        markIs(clearIndex, StepMark.DONE, "2016-06-02")
        compose.onNodeWithContentDescription("Back").performClick()
        status("COMPLETED")
        scenario!!.recreate()
        compose.waitUntil(20_000) { compose.onAllNodesWithTag("mementos-request-list").fetchSemanticsNodes().isNotEmpty() }
        status("COMPLETED")
        capture("p5r-mementos-completed-large-text")
        compose.onNodeWithTag(id).performClick()
        mark("2016-06-02", clearIndex)
        compose.waitUntil(10_000) {
            runBlocking { dependencies.repo().marksFor(profileId).first().none { it.date == "2016-06-02" && it.stepIndex == clearIndex } }
        }
        compose.onNodeWithContentDescription("Back").performClick()
        status("AVAILABLE")
        assertEquals("2016-05-26", runBlocking { dependencies.db().profileDao().byId(profileId)?.clockDate })
    }

    @Test fun p5rSameBuildControlCapture() {
        launch("2016-04-15", "p5r")
        capture("p5r-app-control")
        assertEquals("p5r", dependencies.store().state.value.selectedSlug)
        assertEquals("2016-04-15", runBlocking { dependencies.db().profileDao().byId(profileId)?.clockDate })
    }
}
