package com.shadowmonarchbooks.dayloop.tools.pack

import com.shadowmonarchbooks.dayloop.pack.PackLoader
import com.shadowmonarchbooks.dayloop.pack.schema.AchievementEventAnchor
import com.shadowmonarchbooks.dayloop.pack.schema.MementosRequestDefinition
import com.shadowmonarchbooks.dayloop.pack.schema.MementosRequestsFile
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertTrue

class MementosUnlockLintTest {
    private val request = MementosRequestDefinition("t1.request.target", "Target", "2016-04-09", "2016-04-11",
        completionEvent = "clear", receivedEvent = "receipt", unlockEvents = listOf("investigate"))
    private val events = listOf(
        AchievementEventAnchor("receipt", "2016-04-09", "Arrive in town"),
        AchievementEventAnchor("investigate", "2016-04-10", "Flip the sign"),
        AchievementEventAnchor("clear", "2016-04-11", "First infiltration"),
    )

    private fun issues(request: MementosRequestDefinition, anchors: List<AchievementEventAnchor> = events): List<String> {
        val dir = Files.createTempDirectory("request-unlock-test")
        Fixture.writePack(dir)
        dir.resolve("mementos-requests.json").writeText(PackLoader.json.encodeToString(MementosRequestsFile.serializer(),
            MementosRequestsFile(listOf(request), anchors)))
        return PackLoader.load(dir).parseIssues.map { it.message }
    }

    @Test fun `valid source receipt and investigation anchors are accepted`() {
        assertTrue(issues(request).isEmpty())
    }
    @Test fun `unknown preparation and self unlocking completion are rejected`() {
        assertTrue(issues(request.copy(unlockEvents = listOf("missing"))).any { "unknown prerequisite" in it })
        assertTrue(issues(request.copy(unlockEvents = listOf("clear"))).any { "cannot unlock" in it })
    }
    @Test fun `receipt and completion dates must agree with route checkpoints`() {
        assertTrue(issues(request.copy(receivedOn = "2016-04-10")).any { "receipt event date" in it })
        assertTrue(issues(request.copy(expectedBy = "2016-04-12")).any { "completion event date" in it })
    }
    @Test fun `preparation after the target clear is rejected`() {
        val anchors = events.map { if (it.id == "investigate") it.copy(date = "2016-04-12", labelContains = "Answer the class question") else it }
        assertTrue(issues(request, anchors).any { "after its guide clear" in it })
    }
}
