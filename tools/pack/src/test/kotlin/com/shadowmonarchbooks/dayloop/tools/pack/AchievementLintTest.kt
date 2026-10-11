package com.shadowmonarchbooks.dayloop.tools.pack

import com.shadowmonarchbooks.dayloop.pack.LintIssue
import com.shadowmonarchbooks.dayloop.pack.schema.Day
import com.shadowmonarchbooks.dayloop.pack.schema.Step
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AchievementLintTest {

    @Test fun `achievement reading fields reject blank text duplicate reminders and nonexistent days`() {
        val dir = tempDir()
        Fixture.writePack(dir)
        dir.resolve("achievements.json").writeText("""{
          "achievements":[{"id":"t1.bad","title":"Bad","guidance":" ",
            "routeDates":["2016-04-30","2016-04-30"]}],
          "events":[{"id":"t1.source","date":"2016-04-10","labelContains":"Flip the sign","displayLabel":" "}]
        }""")
        val messages = PackLint.runOn(dir).filter { it.severity == LintIssue.Severity.ERROR }.map { it.message }
        assertTrue(messages.any { "guidance must not be blank" in it })
        assertTrue(messages.any { "duplicate routeDates" in it })
        assertTrue(messages.any { "has no walkthrough day" in it })
        assertTrue(messages.any { "displayLabel must not be blank" in it })
    }

    private fun tempDir() = Files.createTempDirectory("achievement-lint-test")

    @Test
    fun `valid achievement event catalog lints clean`() {
        val dir = tempDir()
        Fixture.writePack(dir)
        dir.resolve("achievements.json").writeText(
            """
            {
              "achievements": [
                {
                  "id": "t1.achievement.flip-sign",
                  "title": "Flip It",
                  "availableFrom": "2016-04-10",
                  "tracking": { "type": "event", "event": "t1.event.flip-sign" }
                }
              ],
              "events": [
                {
                  "id": "t1.event.flip-sign",
                  "date": "2016-04-10",
                  "labelContains": "Flip the sign"
                }
              ]
            }
            """.trimIndent(),
        )

        val errors = PackLint.runOn(dir).filter { it.severity == LintIssue.Severity.ERROR }
        assertEquals(emptyList(), errors, errors.toString())
    }

    @Test
    fun `achievement lint rejects broken types dates icons and event refs`() {
        val dir = tempDir()
        Fixture.writePack(dir)
        dir.resolve("achievements.json").writeText(
            """
            {
              "achievements": [
                {
                  "id": "t1.achievement.bad",
                  "title": "Bad",
                  "availableFrom": "2016-04-30",
                  "iconMediaRef": "t1.media.missing",
                  "tracking": {
                    "type": "mystery",
                    "event": "t1.event.missing",
                    "items": [
                      { "id": "same", "label": "One" },
                      { "id": "same", "label": "Two", "dueBy": "2016-05-01" }
                    ]
                  }
                }
              ]
            }
            """.trimIndent(),
        )

        val messages = PackLint.runOn(dir)
            .filter { it.severity == LintIssue.Severity.ERROR && it.location == "achievements.json" }
            .map { it.message }

        assertTrue(messages.any { "unknown tracking type 'mystery'" in it }, messages.toString())
        assertTrue(messages.any { "2016-04-30" in it && "outside the pack calendar" in it }, messages.toString())
        assertTrue(messages.any { "iconMediaRef 't1.media.missing'" in it }, messages.toString())
        assertTrue(messages.any { "duplicate tracking item id 'same'" in it }, messages.toString())
        assertTrue(messages.any { "2016-05-01" in it && "outside the pack calendar" in it }, messages.toString())
        assertTrue(messages.any { "unknown event 't1.event.missing'" in it }, messages.toString())
    }

    @Test
    fun `achievement event selector must resolve exactly one walkthrough step`() {
        val dir = tempDir()
        val walkthrough = Fixture.validWalkthroughApril().copy(
            days = Fixture.validWalkthroughApril().days.map { day ->
                if (day.date == "2016-04-10") {
                    Day(
                        date = day.date,
                        weekday = day.weekday,
                        dayKind = day.dayKind,
                        steps = listOf(
                            Step("Flip the sign once"),
                            Step("Flip the sign again"),
                        ),
                    )
                } else {
                    day
                }
            },
        )
        Fixture.writePack(dir, walkthroughs = listOf(walkthrough))
        dir.resolve("achievements.json").writeText(
            """
            {
              "achievements": [
                {
                  "id": "t1.achievement.flip-sign",
                  "title": "Flip It",
                  "tracking": { "type": "event", "event": "t1.event.flip-sign" }
                }
              ],
              "events": [
                {
                  "id": "t1.event.flip-sign",
                  "date": "2016-04-10",
                  "labelContains": "Flip the sign"
                }
              ]
            }
            """.trimIndent(),
        )

        val errors = PackLint.runOn(dir)
            .filter { it.severity == LintIssue.Severity.ERROR && it.location == "achievements.json" }
        assertTrue(
            errors.any { "must match exactly one step" in it.message && "found 2" in it.message },
            errors.toString(),
        )
    }

    @Test
    fun `choice tracking validates state and accepted items`() {
        val dir = tempDir()
        Fixture.writePack(dir)
        dir.resolve("achievements.json").writeText(
            """
            {
              "achievements": [
                {
                  "id": "t1.achievement.choice",
                  "title": "Choose",
                  "tracking": {
                    "type": "choice",
                    "acceptedItems": ["missing"],
                    "items": [
                      { "id": "a", "label": "A" },
                      { "id": "b", "label": "B" }
                    ]
                  }
                }
              ]
            }
            """.trimIndent(),
        )

        val messages = PackLint.runOn(dir)
            .filter { it.severity == LintIssue.Severity.ERROR && it.location == "achievements.json" }
            .map { it.message }
        assertTrue(messages.any { "needs a stateKey" in it }, messages.toString())
        assertTrue(messages.any { "accepted choice 'missing'" in it }, messages.toString())
    }
}
