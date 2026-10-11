package com.shadowmonarchbooks.dayloop.pack

import com.shadowmonarchbooks.dayloop.pack.schema.AchievementTrackingTypes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.serialization.encodeToString

class AchievementsSchemaTest {

    @Test fun `optional reading fields round trip while legacy catalogs keep empty defaults`() {
        val legacy = PackLoader.decodeAchievements("""{"achievements":[{"id":"old","title":"Old"}]}""")!!
        assertEquals(null, legacy.achievements.single().guidance)
        assertEquals(emptyList(), legacy.achievements.single().routeDates)
        val enriched = PackLoader.decodeAchievements("""{
            "achievements":[{"id":"new","title":"New","guidance":"Actually finish the action.","routeDates":["2009-04-18"]}],
            "events":[{"id":"source","date":"2009-04-18","labelContains":"Fool reaches rank 1","displayLabel":"Fool · start"}]
        }""")!!
        assertEquals(enriched, PackLoader.decodeAchievements(PackLoader.json.encodeToString(enriched)))
        assertEquals("Fool · start", enriched.events.single().displayLabel)
    }

    @Test
    fun `achievement catalog decodes rules and semantic anchors`() {
        val decoded = PackLoader.decodeAchievements(
            """
            {
              "achievements": [
                {
                  "id": "fixture.gourmand",
                  "title": "Gourmand",
                  "expectedBy": "2009-06-28",
                  "tracking": { "type": "event", "event": "fixture.event.gourmand" }
                }
              ],
              "events": [
                {
                  "id": "fixture.event.gourmand",
                  "date": "2009-06-28",
                  "labelContains": "Seafood Full Course"
                }
              ]
            }
            """.trimIndent(),
        )

        assertNotNull(decoded)
        assertEquals(1, decoded.achievements.size)
        assertEquals(AchievementTrackingTypes.EVENT, decoded.achievements.single().tracking.type)
        assertEquals("fixture.event.gourmand", decoded.achievements.single().tracking.event)
        assertEquals("Seafood Full Course", decoded.events.single().labelContains)
    }
}
