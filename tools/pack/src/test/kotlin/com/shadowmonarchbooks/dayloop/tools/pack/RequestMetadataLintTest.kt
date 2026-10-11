package com.shadowmonarchbooks.dayloop.tools.pack

import com.shadowmonarchbooks.dayloop.pack.PackLoader
import com.shadowmonarchbooks.dayloop.pack.schema.*
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertTrue

class RequestMetadataLintTest {
    private val request = RequestDefinition("request.1", 1, "Request", reward = "Item")
    private val person = MissingPersonDefinition("rescue.1", "Person", 50, "2016-04-09", "2016-04-12", "Reward",
        routeDates = listOf("2016-04-10"), rescueEvent = "save")
    private val event = AchievementEventAnchor("save", "2016-04-10", "Flip the sign")
    private fun issues(catalog: RequestsFile, tip: String = "Rescue the person on 50F before leaving."): List<String> {
        val dir = Files.createTempDirectory("request-metadata-test")
        try {
            val month = Fixture.validWalkthroughApril()
            val days = month.days.map { day -> if (day.date != event.date) day else day.copy(steps = day.steps.map { it.copy(tip = tip) }) }
            Fixture.writePack(dir, Fixture.validPack().copy(capabilities = Fixture.validPack().capabilities.copy(requests = true)),
                listOf(month.copy(days = days)))
            dir.resolve("requests.json").writeText(PackLoader.json.encodeToString(RequestsFile.serializer(), catalog))
            return PackLoader.load(dir).parseIssues.map { it.message }
        } finally { dir.toFile().deleteRecursively() }
    }
    private fun catalog(requests: List<RequestDefinition> = listOf(request), people: List<MissingPersonDefinition> = emptyList(), events: List<AchievementEventAnchor> = emptyList()) =
        RequestsFile("Requests", "Guide", requests, events, people)

    @Test fun `missing self-referencing and cyclic prerequisites cannot ship`() {
        assertTrue(issues(catalog(listOf(request.copy(prerequisiteRequests = listOf("missing"))))).any { "invalid prerequisite" in it })
        assertTrue(issues(catalog(listOf(request.copy(prerequisiteRequests = listOf(request.id))))).any { "invalid prerequisite" in it })
        val second = request.copy(id = "request.2", number = 2, prerequisiteRequests = listOf(request.id))
        assertTrue(issues(catalog(listOf(request.copy(prerequisiteRequests = listOf(second.id)), second))).any { "cyclic" in it })
    }
    @Test fun `invalid item windows and impossible availability are rejected`() {
        assertTrue(issues(catalog(listOf(request.copy(itemWindow = RequestItemWindow("2016-04-12", "2016-04-10", "Offer"))))).any { "invalid item window" in it })
        assertTrue(issues(catalog(listOf(request.copy(availableFrom = "2016-99-10")))).any { "invalid availability" in it })
        assertTrue(issues(catalog(listOf(request.copy(availableFrom = "2016-04-12", deadline = "2016-04-10")))).any { "availability follows" in it })
    }
    @Test fun `rescue anchors require a real rescue objective and the right floor within its window`() {
        assertTrue(issues(catalog(people = listOf(person), events = listOf(event))).isEmpty())
        assertTrue(issues(catalog(people = listOf(person), events = listOf(event)), "Reach 50F.").any { "explicitly cover" in it })
        assertTrue(issues(catalog(people = listOf(person), events = listOf(event)), "Rescue the person on 150F.").any { "explicitly cover" in it })
        assertTrue(issues(catalog(people = listOf(person.copy(availableFrom = "2016-04-11")), events = listOf(event))).any { "within its window" in it })
        assertTrue(issues(catalog(people = listOf(person.copy(routeDates = emptyList())), events = listOf(event))).any { "within its window" in it })
    }
    @Test fun `rescue identities and event ownership cannot collide with requests`() {
        assertTrue(issues(catalog(people = listOf(person.copy(id = request.id)), events = listOf(event))).any { "duplicate missing-person id" in it })
        assertTrue(issues(catalog(listOf(request.copy(completionEvent = event.id, routeDates = listOf(event.date))), listOf(person), listOf(event)))
            .any { "exactly one request or missing person" in it })
    }
}
