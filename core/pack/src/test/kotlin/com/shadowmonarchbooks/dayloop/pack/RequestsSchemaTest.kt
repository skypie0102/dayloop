package com.shadowmonarchbooks.dayloop.pack

import com.shadowmonarchbooks.dayloop.pack.schema.RequestsFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RequestsSchemaTest {
    @Test fun `old request catalogs decode without solution and retain completion evidence`() {
        val original = """{"title":"Requests","issuer":"Guide","requests":[{"id":"test.1","number":1,"title":"Find item","completionEvent":"test.report"}]}"""
        val catalog = PackLoader.json.decodeFromString(RequestsFile.serializer(), original)
        val request = catalog.requests.single()
        assertNull(request.solution)
        val enriched = catalog.copy(requests = listOf(request.copy(solution = "Collect the item.\n\nReturn to the issuer.")))
        val decoded = PackLoader.json.decodeFromString(RequestsFile.serializer(),
            PackLoader.json.encodeToString(RequestsFile.serializer(), enriched))
        assertEquals(enriched, decoded)
        assertEquals(request, decoded.requests.single().copy(solution = null))
    }
}
