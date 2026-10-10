package com.shadowmonarchbooks.dayloop.pack

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MementosRequestsSchemaTest {

    @Test
    fun `request catalog decodes exact completion event`() {
        val decoded = PackLoader.decodeMementosRequests(
            """
            {
              "requests": [
                {
                  "id": "fixture.request.bully",
                  "title": "A Bully",
                  "receivedOn": "2016-05-09",
                  "expectedBy": "2016-06-02",
                  "target": "Shadow Bully",
                  "location": "Aiyatsbus: Area 2",
                  "reward": "Protein",
                  "completionEvent": "fixture.event.bully"
                }
              ],
              "events": [
                {
                  "id": "fixture.event.bully",
                  "date": "2016-06-02",
                  "labelContains": "Complete request: A Bully"
                }
              ]
            }
            """.trimIndent(),
        )

        assertNotNull(decoded)
        assertEquals("fixture.event.bully", decoded.requests.single().completionEvent)
        assertEquals("Complete request: A Bully", decoded.events.single().labelContains)
        assertNull(decoded.requests.single().receivedEvent)
        assertEquals(emptyList(), decoded.requests.single().unlockEvents)
    }

    @Test
    fun `source unlock events and guidance survive serialization`() {
        val request = com.shadowmonarchbooks.dayloop.pack.schema.MementosRequestDefinition(
            "fixture.request.cats", "Cats", "2016-05-26", "2016-06-02", completionEvent = "clear",
            receivedEvent = "receipt", unlockEvents = listOf("investigation"), guidance = "Talk to the witness.",
        )
        val file = com.shadowmonarchbooks.dayloop.pack.schema.MementosRequestsFile(listOf(request))
        val json = PackLoader.json.encodeToString(com.shadowmonarchbooks.dayloop.pack.schema.MementosRequestsFile.serializer(), file)
        assertEquals(file, PackLoader.decodeMementosRequests(json))
    }
}
