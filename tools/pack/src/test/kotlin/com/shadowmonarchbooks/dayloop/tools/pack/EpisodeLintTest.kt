package com.shadowmonarchbooks.dayloop.tools.pack

import com.shadowmonarchbooks.dayloop.pack.PackLoader
import com.shadowmonarchbooks.dayloop.pack.schema.*
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.*

class EpisodeLintTest {
    private val first = EpisodeStep("t1.episode.person.1", "Episode 1", 1, availableFrom = "2016-04-09", availableUntil = "2016-04-12",
        timeOfDay = "Daytime", location = "Town", solution = "Watch the introduction.", completionEvent = "first")
    private val second = first.copy(id = "t1.episode.person.2", number = 2, completionEvent = "second", prerequisiteSteps = listOf(first.id))
    private val chain = EpisodeChain("t1.episode.person", "Person", listOf(first, second))
    private val catalog = EpisodesFile(chains = listOf(chain), events = listOf(
        AchievementEventAnchor("first", "2016-04-09", "Arrive in town"), AchievementEventAnchor("second", "2016-04-10", "Flip the sign")))
    private fun issues(value: EpisodesFile?, capability: Boolean = true): List<String> {
        val dir = Files.createTempDirectory("episode-lint-test")
        try {
            Fixture.writePack(dir, Fixture.validPack().copy(capabilities = Fixture.validPack().capabilities.copy(episodes = capability)))
            value?.let { dir.resolve("episodes.json").writeText(PackLoader.json.encodeToString(EpisodesFile.serializer(), it)) }
            return PackLoader.load(dir).parseIssues.map { it.message }
        } finally { dir.toFile().deleteRecursively() }
    }
    private fun steps(vararg steps: EpisodeStep) = catalog.copy(chains = listOf(chain.copy(steps = steps.toList())))

    @Test fun catalogIsOptionalButMustMatchItsCapability() {
        assertTrue(issues(null, false).isEmpty())
        assertTrue(issues(catalog).isEmpty())
        assertTrue(issues(null).any { "capabilities.episodes" in it })
        assertTrue(issues(catalog, false).any { "capabilities.episodes" in it })
        assertFalse(assertNotNull(PackLoader.decodePack("""{"packId":"old","title":"Old","contentVersion":1,"timeModel":"weekdayGrid","calendar":{"startDate":"2016-04-09","endDate":"2016-04-12"},"slots":[],"stats":[]}""")).capabilities.episodes)
    }

    @Test fun prerequisitesRejectCyclesMissingIdsAndRequiredOptionalDependencies() {
        assertTrue(issues(steps(first, second.copy(prerequisiteSteps = listOf("missing")))).any { "invalid prerequisite" in it })
        assertTrue(issues(steps(first.copy(prerequisiteSteps = listOf(second.id)), second)).any { "cyclic" in it })
        assertTrue(issues(steps(first.copy(number = null, required = false), second)).any { "cannot depend on an optional" in it })
    }

    @Test fun sourceMustBeUniqueCompleteAndInsideTheActualOpportunities() {
        assertTrue(issues(catalog.copy(events = catalog.events.map { it.copy(labelContains = it.labelContains.take(5)) })).any { "complete task label" in it })
        assertTrue(issues(steps(first, second.copy(availableDates = listOf("2016-04-09", "2016-04-11")))).any { "inside its window" in it })
        assertTrue(issues(steps(first, second.copy(availableFrom = "2016-04-11"))).any { "inside its window" in it })
        assertTrue(issues(catalog.copy(events = catalog.events + catalog.events.first())).any { "duplicate or blank" in it })
        assertTrue(issues(steps(first, second.copy(completionEvent = first.completionEvent))).any { "exactly one step" in it })
    }

    @Test fun invalidIdentityOrderWindowsAndEmptyGuidanceCannotShip() {
        assertTrue(issues(steps(first, second.copy(id = first.id))).any { "duplicate episode step" in it })
        assertTrue(issues(steps(first, second.copy(number = 1))).any { "increase uniquely" in it })
        assertTrue(issues(steps(first, second.copy(availableUntil = "2016-04-08"))).any { "invalid availability" in it })
        assertTrue(issues(steps(first, second.copy(solution = ""))).any { "readable guidance" in it })
        assertTrue(issues(steps(first, second.copy(availableDates = listOf("2016-04-11", "2016-04-10")))).any { "invalid availability" in it })
    }
}
