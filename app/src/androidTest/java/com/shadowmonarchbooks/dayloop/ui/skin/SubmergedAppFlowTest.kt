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
    }

    @After fun close() {
        try {
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

    private fun capture(name: String) {
        compose.waitForIdle()
        scenario!!.onActivity { activity ->
            assertTrue("Capture must use the requested font scale",
                activity.resources.configuration.fontScale == expectedFontScale)
        }
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
        hasText(label) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab) and
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
        compose.onNodeWithTag("episode-list").performScrollToNode(hasTestTag("episode-chain-$id"))
        compose.onNodeWithTag("episode-chain-$id").performClick()
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
        assertTrue("selected whole-word tab must remain in the viewport at 200% text",
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
