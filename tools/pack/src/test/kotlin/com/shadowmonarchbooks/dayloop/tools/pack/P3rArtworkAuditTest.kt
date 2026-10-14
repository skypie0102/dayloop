package com.shadowmonarchbooks.dayloop.tools.pack

import com.shadowmonarchbooks.dayloop.pack.PackLoader
import com.shadowmonarchbooks.dayloop.pack.schema.MediaKinds
import kotlinx.serialization.json.*
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.test.*

/** Protect reviewed identities and bytes, especially Junpei's former Kenji association. */
class P3rArtworkAuditTest {
    @Test fun portraitLedgerMatchesBundledAssetsAndSocialLinkIdentities() {
        val root = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("content/packs/p3r")) }
        val packRoot = root.resolve("content/packs/p3r")
        val loaded = PackLoader.load(packRoot)
        val bonds = assertNotNull(loaded.bonds).bonds.associateBy { it.id }
        val portraits = assertNotNull(loaded.media).media.filter { it.kind == MediaKinds.PORTRAIT }.associateBy { it.id }
        val ledger = Json.parseToJsonElement(Files.readString(root.resolve("docs/references/p3r-social-link-artwork.json"))).jsonObject
        fun JsonObject.label(key: String) = getValue(key).jsonPrimitive.content
        fun JsonObject.labels(key: String) = getValue(key).jsonArray.map { it.jsonPrimitive.content }

        val reviewed = ledger.getValue("portraits").jsonArray.map { it.jsonObject }
        assertEquals(portraits.keys, reviewed.map { it.label("mediaId") }.toSet())
        reviewed.forEach { entry ->
            val media = portraits.getValue(entry.label("mediaId"))
            assertEquals(media.file, entry.label("asset"))
            assertEquals(media.title, entry.label("character"))
            assertEquals(media.bonds, entry.labels("bonds"))
            val sha = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(packRoot.resolve(media.file)))
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
            assertEquals(entry.label("sha256"), sha, media.id)
            media.bonds.forEach { id ->
                assertEquals(bonds.getValue(id).characterLabel, media.title,
                    "${media.id} must depict the named character, not another party member")
            }
        }
        val links = ledger.getValue("socialLinks").jsonArray.map { it.jsonObject }
        assertEquals(bonds.keys, links.map { it.label("bondId") }.toSet())
        links.forEach { entry ->
            val bond = bonds.getValue(entry.label("bondId"))
            val matches = portraits.values.filter { bond.id in it.bonds }.map { it.id }
            assertEquals(bond.label, entry.label("arcana"))
            assertEquals(bond.characterLabel, entry.label("character"))
            assertEquals(matches, entry.labels("portraitMediaIds"))
            assertEquals(if (matches.isEmpty()) "name-fallback" else "matched-portrait", entry.label("status"))
        }
        assertTrue(portraits.getValue("p3r.media.portrait-junpei").bonds.isEmpty())
        assertTrue(links.single { it.label("bondId") == "p3r.bond.magician" }.labels("portraitMediaIds").isEmpty())
    }
}
