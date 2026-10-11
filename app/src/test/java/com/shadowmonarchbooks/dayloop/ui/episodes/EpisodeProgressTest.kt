package com.shadowmonarchbooks.dayloop.ui.episodes

import com.shadowmonarchbooks.dayloop.pack.PackLoader
import com.shadowmonarchbooks.dayloop.progress.StepKey
import com.shadowmonarchbooks.dayloop.progress.StepMark
import com.shadowmonarchbooks.dayloop.ui.achievements.completedAchievementEvents
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

class EpisodeProgressTest {
    private val loaded by lazy {
        val root = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("content/packs/p3r")) }
        PackLoader.load(root.resolve("content/packs/p3r"))
    }
    private val catalog get() = assertNotNull(loaded.episodes)
    private val days get() = loaded.walkthroughs.filter { it.routeId == "standard" }.flatMap { it.file.days }.associateBy { it.date }
    private fun id(person: String, number: String) = "p3r.episode." + person + "." + number
    private fun events(ids: List<String>, mark: StepMark = StepMark.DONE, route: String = "standard"): Set<String> {
        val byId = catalog.chains.flatMap { it.steps }.associateBy { it.id }
        val marks = ids.associate { stepId ->
            val anchor = catalog.events.single { it.id == byId.getValue(stepId).completionEvent }
            StepKey(anchor.date, days.getValue(anchor.date).steps.indexOfFirst { it.label == anchor.labelContains }) to mark
        }
        return completedAchievementEvents(catalog.events, days, marks, route)
    }

    @Test fun onlyExactDoneTasksRecordEpisodes() {
        val first = id("junpei", "1")
        assertTrue(episodeProgress(catalog, events(listOf(first))).getValue(first).complete)
        for (mark in listOf(StepMark.SKIP, StepMark.LATER)) {
            assertTrue(episodeProgress(catalog, events(listOf(first), mark)).values.none { it.recorded })
        }
        assertTrue(episodeProgress(catalog, events(listOf(first), route = "another-route")).values.none { it.recorded })
        val ordinary = days.values.flatMap { day -> day.steps.mapIndexedNotNull { index, step ->
            if (step.label.startsWith("Walk Koromaru") || step.label.contains("hangout", ignoreCase = true)) StepKey(day.date, index) to StepMark.DONE else null
        } }.toMap()
        assertTrue(completedAchievementEvents(catalog.events, days, ordinary, "standard").isEmpty())
        assertTrue(episodeProgress(catalog, emptySet()).values.none { it.complete })
    }

    @Test fun laterRecordsWaitForEarlierProofAndUndoKeepsTheOriginalMarks() {
        val ids = (1..5).map { id("junpei", it.toString()) }
        val all = events(ids)
        val chain = catalog.chains.single { it.id == "p3r.episode.junpei" }
        assertNull(nextEpisodeStep(chain, episodeProgress(catalog, all)))
        val firstEvent = catalog.chains.flatMap { it.steps }.single { it.id == ids.first() }.completionEvent
        val reversed = episodeProgress(catalog, all - firstEvent)
        assertEquals(ids.first(), nextEpisodeStep(chain, reversed)?.id)
        assertTrue(ids.drop(1).all { reversed.getValue(it).recorded && !reversed.getValue(it).complete })
        assertEquals("Recorded · earlier step unconfirmed", episodeStatus(chain.steps.single { it.number == 2 }, reversed[ids[1]], "2010-01-31"))
        assertFalse(episodeChainMatches(chain, reversed, "2010-01-31", "Completed"))
        assertNull(nextEpisodeStep(chain, episodeProgress(catalog, all)))
    }

    @Test fun shinjiroFormIsRequiredBeforeTheFourthEpisodeAndFusionReward() {
        val chain = catalog.chains.single { it.id == "p3r.episode.shinjiro" }
        val main = chain.steps.filter { it.number != null }.map { it.id }
        val withoutForm = episodeProgress(catalog, events(main))
        assertEquals(id("shinjiro", "form"), nextEpisodeStep(chain, withoutForm)?.id)
        assertTrue(withoutForm.getValue(id("shinjiro", "4")).recorded)
        assertFalse(withoutForm.getValue(id("shinjiro", "5")).complete)
        val complete = episodeProgress(catalog, events(main + id("shinjiro", "form")))
        assertNull(nextEpisodeStep(chain, complete))
    }

    @Test fun flowerPurchaseHandoffAndRevivalAreSeparateAndOptional() {
        val chain = catalog.chains.single { it.id == "p3r.episode.junpei" }
        val main = chain.steps.filter { it.number != null }.map { it.id }
        val ordinaryBuyIndex = days.getValue("2009-11-08").steps.indexOfFirst { it.label.startsWith("Buy the White Flower") }
        val bought = completedAchievementEvents(catalog.events, days, mapOf(StepKey("2009-11-08", ordinaryBuyIndex) to StepMark.DONE), "standard")
        assertTrue(bought.isEmpty())
        val mainProgress = episodeProgress(catalog, events(main))
        assertNull(nextEpisodeStep(chain, mainProgress))
        assertFalse(mainProgress.getValue(id("junpei", "flowers")).complete)
        val handedOver = episodeProgress(catalog, events(main + id("junpei", "flowers")))
        assertTrue(handedOver.getValue(id("junpei", "flowers")).complete)
        assertFalse(handedOver.getValue(id("junpei", "revival")).complete)
        val viewedWithoutFlowers = episodeProgress(catalog, events(main + id("junpei", "revival")))
        assertTrue(viewedWithoutFlowers.getValue(id("junpei", "revival")).recorded)
        assertFalse(viewedWithoutFlowers.getValue(id("junpei", "revival")).complete)
        assertTrue(episodeProgress(catalog, events(main + listOf(id("junpei", "flowers"), id("junpei", "revival"))))
            .getValue(id("junpei", "revival")).complete)
    }

    @Test fun exactOpportunitiesAndCutoffsAreContextNeverCompletion() {
        val ryoji = catalog.chains.single { it.id == "p3r.episode.ryoji" }
        val second = ryoji.steps.single { it.number == 2 }
        assertFalse(episodeWindowOpen(second, "2009-11-13"))
        assertFalse(episodeWindowOpen(second, "2009-11-15"))
        assertTrue(episodeWindowOpen(second, "2009-11-14"))
        val progress = episodeProgress(catalog, events(listOf(id("ryoji", "1"))))
        assertTrue(episodeChainMatches(ryoji, progress, "2009-11-14", "Open windows"))
        assertFalse(episodeChainMatches(ryoji, progress, "2009-11-13", "Open windows"))
        assertEquals("Window passed · unconfirmed", episodeStatus(second, progress[second.id], "2009-11-17"))
        assertFalse(progress.getValue(second.id).complete)
        assertTrue(episodeProgress(catalog, emptySet()).values.none { it.complete })
    }

    @Test fun allThirtyEpisodesRequireTheFormButNeitherOptionalBranchNorRelatedStories() {
        val mainChains = catalog.chains.filterNot { it.relatedStory }
        val required = mainChains.flatMap { it.steps }.filter { it.required }
        val progress = episodeProgress(catalog, events(required.map { it.id }))
        assertEquals(30, mainChains.flatMap { it.steps }.count { it.number != null && progress.getValue(it.id).complete })
        assertTrue(mainChains.all { nextEpisodeStep(it, progress) == null })
        assertTrue(catalog.chains.single { it.relatedStory }.steps.none { progress.getValue(it.id).recorded })
        assertTrue(mainChains.flatMap { it.steps }.filterNot { it.required }.none { progress.getValue(it.id).recorded })
    }
}
