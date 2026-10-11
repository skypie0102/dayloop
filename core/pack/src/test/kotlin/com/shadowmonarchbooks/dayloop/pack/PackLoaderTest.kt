package com.shadowmonarchbooks.dayloop.pack

import com.shadowmonarchbooks.dayloop.pack.schema.Capabilities
import com.shadowmonarchbooks.dayloop.pack.schema.CalendarRange
import com.shadowmonarchbooks.dayloop.pack.schema.Day
import com.shadowmonarchbooks.dayloop.pack.schema.GuidanceSection
import com.shadowmonarchbooks.dayloop.pack.schema.Labels
import com.shadowmonarchbooks.dayloop.pack.schema.Pack
import com.shadowmonarchbooks.dayloop.pack.schema.PackTheme
import com.shadowmonarchbooks.dayloop.pack.schema.Slot
import com.shadowmonarchbooks.dayloop.pack.schema.StatDef
import com.shadowmonarchbooks.dayloop.pack.schema.Step
import com.shadowmonarchbooks.dayloop.pack.schema.WalkthroughFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Decoding contract shared by the lint tool and the Android app. */
class PackLoaderTest {

    private val packJson = """
        {
          "packId": "t1",
          "title": "Test Game",
          "contentVersion": 1,
          "timeModel": "weekdayGrid",
          "calendar": { "startDate": "2016-04-09", "endDate": "2016-04-10" },
          "slots": [ { "id": "afternoon", "label": "Afternoon" } ],
          "stats": [ { "id": "knowledge", "label": "Knowledge" } ],
          "capabilities": { "exams": true },
          "labels": { "bond": "Confidant", "stat": "Social Stat" }
        }
    """.trimIndent()

    @Test
    fun `decodePack parses a valid pack`() {
        val pack = PackLoader.decodePack(packJson)
        assertNotNull(pack)
        assertEquals("t1", pack.packId)
        assertEquals("Confidant", pack.labels.bond)
        assertTrue(pack.capabilities.exams)
    }

    @Test
    fun `decodePack returns null on garbage instead of throwing`() {
        assertNull(PackLoader.decodePack("{ not json"))
        assertNull(PackLoader.decodePack("""{"packId": 42}"""))
    }

    @Test
    fun `decodeError reports the offending file`() {
        val error = PackLoader.decodeError("{ not json", "pack.json", Pack.serializer())
        assertNotNull(error)
        assertTrue("pack.json" in error)
    }

    @Test
    fun `decodeWalkthrough round-trips days`() {
        val wt = WalkthroughFile(
            month = "2016-04",
            days = listOf(
                Day(
                    "2016-04-09",
                    "sat",
                    "story",
                    steps = listOf(Step("Arrive in town", tip = "Take the evening train.",
                        guidanceSections = listOf(GuidanceSection("Prepare", "Bring a ticket.")))),
                    displayOrder = listOf(0),
                ),
            ),
        )
        val text = PackLoader.json.encodeToString(WalkthroughFile.serializer(), wt)
        val parsed = PackLoader.decodeWalkthrough(text)
        assertNotNull(parsed)
        assertEquals(1, parsed.days.size)
        assertEquals("Arrive in town", parsed.days.first().steps.first().label)
        assertEquals("Take the evening train.", parsed.days.first().steps.first().tip)
        assertEquals(listOf(GuidanceSection("Prepare", "Bring a ticket.")), parsed.days.first().steps.first().guidanceSections)
        assertEquals(listOf(0), parsed.days.first().displayOrder)
    }

    @Test
    fun `legacy packs and walkthroughs retain their old presentation defaults`() {
        assertEquals(false, PackLoader.decodePack(packJson)?.walkthroughPresentation?.inlineTips)
        val wt = PackLoader.decodeWalkthrough("""{"month":"2016-04","days":[{"date":"2016-04-09","weekday":"sat","steps":[{"label":"Arrive"}]}]}""")
        assertNotNull(wt)
        assertEquals(emptyList(), wt.days.single().displayOrder)
        assertEquals(emptyList(), wt.days.single().steps.single().guidanceSections)
        assertEquals(false, wt.days.single().steps.single().hasGuidance)
    }

    @Test
    fun `structured guidance is available without a legacy tip`() {
        val step = Step("Fight", guidanceSections = listOf(GuidanceSection("Prepare", "Recover first.")))
        val decoded = PackLoader.json.decodeFromString(Step.serializer(), PackLoader.json.encodeToString(Step.serializer(), step))
        assertNull(decoded.tip)
        assertTrue(decoded.hasGuidance)
        assertEquals(step, decoded)
    }

    @Test
    fun `pack can opt into inline guidance without a game-specific capability`() {
        val text = packJson.replace("\"packId\":", "\"walkthroughPresentation\": {\"inlineTips\": true}, \"packId\":")
        assertEquals(true, PackLoader.decodePack(text)?.walkthroughPresentation?.inlineTips)
    }

    @Test
    fun `unknown keys are ignored`() {
        val text = packJson.replace("\"exams\": true", "\"exams\": true, \"futureFlag\": true")
        assertNotNull(PackLoader.decodePack(text))
    }

    @Test
    fun `calendar helpers agree with the real calendar`() {
        assertEquals("sat", Cal.weekdayOf("2016-04-09"))
        assertEquals("thu", Cal.weekdayOf("2016-05-12"))
        assertNull(Cal.parseDate("2016-13-40"))
        assertEquals(2, Cal.datesBetween("2016-04-09", "2016-04-10").size)
        assertEquals(emptyList(), Cal.datesBetween("2016-04-10", "2016-04-09"))
    }

    @Test
    fun `pack with defaults only needs identity and calendar`() {
        val minimal = PackLoader.decodePack(
            """
            {
              "packId": "t2",
              "title": "Minimal",
              "contentVersion": 1,
              "timeModel": "dayCounter",
              "calendar": { "startDate": "2100-06-01", "endDate": "2100-06-07" },
              "slots": [ { "id": "day", "label": "Day" } ],
              "stats": [ { "id": "courage", "label": "Courage" } ]
            }
            """.trimIndent(),
        )
        assertNotNull(minimal)
        assertEquals(Capabilities(), minimal.capabilities)
        assertEquals(Labels(), minimal.labels)
        assertNotNull(CalendarRange(startDate = "2100-06-01", endDate = "2100-06-07"))
        assertEquals(1, minimal.slots.size)
        assertEquals(StatDef("courage", "Courage"), minimal.stats.first())
    }

    @Test
    fun `pack without theme decodes to null theme`() {
        val pack = PackLoader.decodePack(packJson)
        assertNotNull(pack)
        assertNull(pack.theme)
    }

    @Test
    fun `theme block decodes seeds, style, motif, and art slots`() {
        val pack = PackLoader.decodePack(
            """
            {
              "packId": "t3",
              "title": "Themed",
              "contentVersion": 1,
              "timeModel": "weekdayGrid",
              "calendar": { "startDate": "2016-04-09", "endDate": "2016-04-10" },
              "slots": [ { "id": "afternoon", "label": "Afternoon" } ],
              "stats": [ { "id": "knowledge", "label": "Knowledge" } ],
              "theme": {
                "accent": "#A61E22",
                "accentDark": "#D9433C",
                "style": "vibrant",
                "motif": "masks",
                "art": { "card": "art/card.jpg", "icon": "art/icon.png" }
              }
            }
            """.trimIndent(),
        )
        assertNotNull(pack)
        val theme = pack.theme
        assertNotNull(theme)
        assertEquals("#A61E22", theme.accent)
        assertEquals("#D9433C", theme.accentDark)
        assertEquals("vibrant", theme.style)
        assertEquals("masks", theme.motif)
        assertEquals(mapOf("card" to "art/card.jpg", "icon" to "art/icon.png"), theme.art)
    }

    @Test
    fun `seedArgb picks the mode-specific color with light fallback`() {
        val theme = PackTheme(accent = "#FF0000", accentDark = "#00FF00")
        assertEquals(0xFFFF0000.toInt(), theme.seedArgb(dark = false))
        assertEquals(0xFF00FF00.toInt(), theme.seedArgb(dark = true))
        assertEquals(0xFFFF0000.toInt(), theme.copy(accentDark = null).seedArgb(dark = true))
        assertNull(PackTheme().seedArgb(dark = true))
    }

    @Test
    fun `parseHexColor accepts the documented shapes only`() {
        assertEquals(0xFFA61E22.toInt(), PackTheme.parseHexColor("#A61E22"))
        assertEquals(0xFFA61E22.toInt(), PackTheme.parseHexColor("A61E22"))
        assertEquals(0x80A61E22.toInt(), PackTheme.parseHexColor("#80A61E22"))
        assertNull(PackTheme.parseHexColor("#A61E2"))
        assertNull(PackTheme.parseHexColor("#GGGGGG"))
        assertNull(PackTheme.parseHexColor(""))
    }

    @Test
    fun `deadline kind labels prefer the pack override`() {
        val labels = Labels(bond = "Follower", stat = "Royal Virtue", deadlineKinds = mapOf("palace" to "Mission"))
        assertEquals("Mission", labels.deadlineKind("palace"))
        assertEquals("Missable", labels.deadlineKind("missable"))
        assertEquals(Labels().deadlineKind("palace"), "Palace")
    }
}
