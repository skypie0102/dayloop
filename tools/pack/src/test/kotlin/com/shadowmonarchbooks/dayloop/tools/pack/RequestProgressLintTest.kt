package com.shadowmonarchbooks.dayloop.tools.pack

import com.shadowmonarchbooks.dayloop.pack.PackLoader
import com.shadowmonarchbooks.dayloop.pack.schema.AchievementEventAnchor
import com.shadowmonarchbooks.dayloop.pack.schema.RequestDefinition
import com.shadowmonarchbooks.dayloop.pack.schema.RequestsFile
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertTrue

class RequestProgressLintTest {
    private val request = RequestDefinition("t1.request.item", 1, "Bring item",
        routeDates = listOf("2016-04-09", "2016-04-10", "2016-04-11", "2016-04-12"),
        completionEvent = "report", acceptanceEvent = "accept",
        preparationEvents = listOf("collect"), readyEvents = listOf("collect"))
    private val anchors = listOf(
        AchievementEventAnchor("accept", "2016-04-09", "Arrive in town"),
        AchievementEventAnchor("collect", "2016-04-10", "Flip the sign"),
        AchievementEventAnchor("report", "2016-04-11", "First infiltration"),
    )
    private fun issues(requests: List<RequestDefinition> = listOf(request), events: List<AchievementEventAnchor> = anchors): List<String> {
        val dir = Files.createTempDirectory("request-progress-test")
        try {
            Fixture.writePack(dir, Fixture.validPack().copy(capabilities = Fixture.validPack().capabilities.copy(requests = true)))
            dir.resolve("requests.json").writeText(PackLoader.json.encodeToString(RequestsFile.serializer(),
                RequestsFile("Requests", "Guide", requests, events)))
            return PackLoader.load(dir).parseIssues.map { it.message }
        } finally { dir.toFile().deleteRecursively() }
    }
    @Test fun `ready proof may reuse its own preparation event`() {
        assertTrue(issues().isEmpty(), issues().joinToString())
    }
    @Test fun `missing and duplicate progress references are rejected`() {
        assertTrue(issues(listOf(request.copy(readyEvents = listOf("missing")))).any { "missing progress event" in it })
        assertTrue(issues(listOf(request.copy(preparationEvents = listOf("collect", "collect")))).any { "duplicate progress reference" in it })
    }
    @Test fun `reporting cannot be reused as acceptance or objective proof`() {
        assertTrue(issues(listOf(request.copy(readyEvents = listOf("report")))).any { "separate from its reporting task" in it })
        val alias = AchievementEventAnchor("alias", "2016-04-11", "infiltration")
        assertTrue(issues(listOf(request.copy(readyEvents = listOf("alias"))), anchors + alias)
            .any { "separate from its reporting task" in it })
    }
    @Test fun `source proof after reporting and unlinked dates are rejected`() {
        val late = anchors.map { if (it.id == "collect") it.copy(date = "2016-04-12", labelContains = "Answer the class question") else it }
        assertTrue(issues(events = late).any { "cannot follow its reporting task" in it })
        assertTrue(issues(listOf(request.copy(routeDates = listOf("2016-04-11")))).any { "progress task needs a route link" in it })
    }
    @Test fun `one semantic event cannot belong to two different requests`() {
        val other = request.copy(id = "t1.request.other", number = 2, completionEvent = null,
            acceptanceEvent = null, preparationEvents = listOf("collect"), readyEvents = emptyList())
        assertTrue(issues(listOf(request, other)).any { "exactly one request" in it })
    }
}
