package com.shadowmonarchbooks.dayloop.data

import com.shadowmonarchbooks.dayloop.pack.GameCalendar
import com.shadowmonarchbooks.dayloop.pack.PackLoader
import com.shadowmonarchbooks.dayloop.pack.schema.AllOf
import com.shadowmonarchbooks.dayloop.pack.schema.AnyOf
import com.shadowmonarchbooks.dayloop.pack.schema.BondRankGte
import com.shadowmonarchbooks.dayloop.pack.schema.Condition
import com.shadowmonarchbooks.dayloop.pack.schema.MediaKinds
import com.shadowmonarchbooks.dayloop.pack.schema.StatGte
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Cross-reference tests over the bundled content packs (docs/ROADMAP-v2.md
 * Phase 9: "served accordingly" is checkable, not vibes). packlint enforces
 * the full rule set; these JVM tests pin the cross-references the app
 * resolves at render time, so a broken ref fails the build, not the UI.
 * Runs only where the repo checkout is present (no-ops otherwise).
 */
class PackContentTest {

    private fun contentPacksDir(): Path? {
        var dir: Path? = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
        repeat(5) {
            val candidate = dir?.resolve("content")?.resolve("packs")
            if (candidate != null && candidate.isDirectory()) return candidate
            dir = dir?.parent
        }
        return null
    }

    private fun loadPacks(): List<Triple<String, Path, com.shadowmonarchbooks.dayloop.pack.PackLoadResult>> {
        val root = contentPacksDir() ?: return emptyList()
        val results = Files.list(root).use { stream ->
            stream.filter { it.isDirectory() }
                .sorted()
                .map { dir -> Triple(dir.name, dir, PackLoader.load(dir)) }
                .toList()
        }
        assertTrue(results.isNotEmpty(), "no packs found under $root")
        return results
    }

    @Test
    fun `all three packs load without parse errors`() {
        val names = loadPacks().map { it.first }
        assertTrue(
            listOf("p5r", "p3r", "metaphor").all { it in names },
            "expected p5r, p3r and metaphor; found $names",
        )
        loadPacks().forEach { (slug, _, loaded) ->
            assertEquals(emptyList(), loaded.parseIssues, "$slug must decode cleanly")
        }
    }

    @Test
    fun `every answer sheet deadlineRef resolves to a real deadline`() {
        loadPacks().forEach { (slug, _, loaded) ->
            val deadlineIds = loaded.deadlines?.deadlines?.map { it.id }?.toSet().orEmpty()
            loaded.answers?.answers?.forEach { sheet ->
                sheet.deadlineRef?.let { ref ->
                    assertTrue(ref in deadlineIds, "$slug: answer sheet '${sheet.id}' references unknown deadline '$ref'")
                }
            }
        }
    }

    @Test
    fun `every walkthrough activityRef resolves to a real activity`() {
        loadPacks().forEach { (slug, _, loaded) ->
            val activityIds = loaded.activities?.activities?.map { it.id }?.toSet().orEmpty()
            loaded.walkthroughs.forEach { wt ->
                wt.file.days.forEach { day ->
                    day.steps.forEach { step ->
                        step.activityRef?.let { ref ->
                            assertTrue(
                                ref in activityIds,
                                "$slug: ${wt.location} day '${day.date}' references unknown activity '$ref'",
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `every step slot resolves to a declared slot`() {
        loadPacks().forEach { (slug, _, loaded) ->
            val pack = loaded.pack ?: return@forEach
            val slotIds = pack.slots.map { it.id }.toSet()
            loaded.walkthroughs.forEach { wt ->
                wt.file.days.forEach { day ->
                    day.steps.forEach { step ->
                        step.slot?.let { slot ->
                            assertTrue(slot in slotIds, "$slug: ${wt.location} day '${day.date}' uses undeclared slot '$slot'")
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `gate references resolve to declared stats and bonds`() {
        loadPacks().forEach { (slug, _, loaded) ->
            val pack = loaded.pack ?: return@forEach
            val statIds = pack.stats.map { it.id }.toSet()
            val bondIds = loaded.bonds?.bonds?.map { it.id }?.toSet().orEmpty()

            fun check(condition: Condition, where: String) {
                when (condition) {
                    is AllOf -> condition.allOf.forEach { check(it, where) }
                    is AnyOf -> condition.anyOf.forEach { check(it, where) }
                    is StatGte -> assertTrue(
                        condition.stat in statIds,
                        "$slug: $where gate references unknown stat '${condition.stat}'",
                    )
                    is BondRankGte -> assertTrue(
                        condition.bond in bondIds,
                        "$slug: $where gate references unknown bond '${condition.bond}'",
                    )
                    else -> Unit
                }
            }

            loaded.bonds?.bonds?.forEach { bond ->
                bond.ranks.forEach { step ->
                    step.gates?.let { check(it, "bond '${bond.id}' rank ${step.rank}") }
                }
            }
        }
    }

    @Test
    fun `bond route dates are calendar dates and respect explicit availability windows`() {
        loadPacks().forEach { (slug, _, loaded) ->
            val pack = loaded.pack ?: return@forEach
            val calendar = GameCalendar.of(pack.calendar) ?: return@forEach
            loaded.bonds?.bonds?.forEach { bond ->
                bond.ranks.forEach { step ->
                    step.scheduledFor?.let { routeDate ->
                        assertTrue(routeDate in calendar, "$slug: ${bond.id} rank ${step.rank} route date $routeDate is outside the calendar")
                        step.availableFrom?.let { from ->
                            assertTrue(routeDate >= from, "$slug: ${bond.id} rank ${step.rank} route date $routeDate is before $from")
                        }
                        step.availableUntil?.let { until ->
                            assertTrue(routeDate <= until, "$slug: ${bond.id} rank ${step.rank} route date $routeDate is after $until")
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `p5r keeps route-selected confidant dates separate from game availability`() {
        val p5r = loadPacks().firstOrNull { it.first == "p5r" }?.third ?: return
        val bonds = p5r.bonds?.bonds?.associateBy { it.id }.orEmpty()
        val fool = bonds.getValue("p5r.bond.fool")
        val magician = bonds.getValue("p5r.bond.magician")
        val chariot = bonds.getValue("p5r.bond.chariot")
        val justice = bonds.getValue("p5r.bond.justice")
        val councilor = bonds.getValue("p5r.bond.councilor")

        assertEquals("Igor", fool.characterLabel)
        assertEquals("2016-04-12", fool.ranks.first { it.rank == 1 }.availableFrom)
        assertEquals("2016-04-24", fool.ranks.first { it.rank == 2 }.scheduledFor)
        listOf(2, 4, 6).forEach { rank ->
            val step = fool.ranks.first { it.rank == rank }
            assertNull(step.availableFrom, "Palace-triggered Fool rank $rank must not masquerade as calendar availability")
            assertNull(step.availableUntil, "Palace-triggered Fool rank $rank must not invent a calendar cutoff")
        }
        listOf(2, 3, 5, 9).forEach { rank ->
            val step = magician.ranks.first { it.rank == rank }
            assertNull(step.availableFrom, "Palace-triggered Magician rank $rank must not masquerade as calendar availability")
            assertNull(step.availableUntil, "Palace-triggered Magician rank $rank must not invent a calendar cutoff")
        }
        assertNull(chariot.ranks.first { it.rank == 2 }.availableFrom, "route-selected Chariot rank date must not masquerade as availability")
        assertTrue(justice.ranks.first { it.rank == 8 }.notes.orEmpty().contains("not Akechi"), "Justice copy must not claim Akechi unlocks third semester")
        assertEquals("2016-11-17", councilor.ranks.first { it.rank == 9 }.availableUntil)
    }

    @Test
    fun `p5r activity gains use actual stat points rather than displayed note counts`() {
        val p5r = loadPacks().firstOrNull { it.first == "p5r" }?.third ?: return
        val pack = p5r.pack ?: return
        val activities = p5r.activities?.activities.orEmpty()
        val byId = activities.associateBy { it.id }

        assertEquals(14, pack.contentVersion)

        val statBooks = activities.filter { it.id.startsWith("p5r.activity.book.") && it.statGains.isNotEmpty() }
        assertTrue(statBooks.isNotEmpty())
        assertTrue(
            statBooks.all { activity -> activity.statGains.values.all { it == 5 || it == 7 } },
            "P5R stat books must store actual 5/7-point rewards, not displayed note counts",
        )

        val dvds = activities.filter { it.id.startsWith("p5r.activity.dvd.") }
        assertTrue(dvds.isNotEmpty())
        assertTrue(dvds.all { it.statGains.values.singleOrNull() == 3 }, "every Royal DVD viewing grants 3 base stat points")
        assertTrue(dvds.all { it.notes.orEmpty().contains("Two viewings") }, "Royal DVDs require two viewings")
        assertTrue(dvds.all { it.notes.orEmpty().contains("no return deadline") }, "Royal DVD subscription has no return deadline")

        val movies = activities.filter { it.id.startsWith("p5r.activity.movie.") }
        assertTrue(movies.isNotEmpty())
        assertTrue(movies.all { it.statGains.values.singleOrNull() == 5 }, "first-time Royal movie viewings grant 5 base stat points")

        val games = activities.filter { it.id.startsWith("p5r.activity.videoGame.") }
        assertTrue(games.isNotEmpty())
        assertTrue(games.all { it.statGains.values.singleOrNull() == 3 }, "Royal retro-game clears grant 3 stat points")
        assertTrue(games.all { it.notes.orEmpty().contains("makes the minigame easier") }, "Game Secrets must be described as an assist, not a guaranteed win")

        assertEquals(7, byId.getValue("p5r.activity.book.social-thought").statGains.getValue("knowledge"))
        assertEquals(7, byId.getValue("p5r.activity.book.master-swordsman").statGains.getValue("guts"))
        assertTrue(byId.getValue("p5r.activity.book.craft-of-cinema").notes.orEmpty().contains("+2"))
        assertTrue(byId.getValue("p5r.activity.book.knowing-the-heart").notes.orEmpty().contains("Technical"))
        assertEquals("Yongen-Jaya movie theater", byId.getValue("p5r.activity.movie.back-to-the-ninja").location)
        assertEquals("Shinjuku movie theater", byId.getValue("p5r.activity.movie.pach-saw").location)
    }

    @Test
    fun `p5r April route uses audited actual point values`() {
        val p5r = loadPacks().firstOrNull { it.first == "p5r" }?.third ?: return
        val activities = p5r.activities?.activities?.associateBy { it.id }.orEmpty()
        val days = p5r.walkthroughs.flatMap { it.file.days }.filter { it.date.startsWith("2016-04") }

        for (day in days) {
            for (step in day.steps) {
                val ref = step.activityRef ?: continue
                val base = activities[ref]?.statGains.orEmpty()
                if (base.isNotEmpty() && step.statGains.isNotEmpty()) {
                    assertEquals(base, step.statGains, "P5R ${day.date}: '${step.label}' must use the activity's actual base stat points")
                }
            }
        }

        fun gain(date: String, text: String): Map<String, Int> =
            days.first { it.date == date }.steps
                .first { it.label.contains(text) && it.statGains.isNotEmpty() }
                .statGains

        assertEquals(mapOf("knowledge" to 2), gain("2016-04-12", "class question"))
        assertEquals(mapOf("proficiency" to 3), gain("2016-04-17", "craft one lock pick"))
        assertEquals(mapOf("knowledge" to 5), gain("2016-04-20", "rainy-day bonus"))
        assertEquals(mapOf("knowledge" to 2, "guts" to 2), gain("2016-04-22", "school library"))
        assertEquals(mapOf("charm" to 2), gain("2016-04-25", "class question"))
        assertEquals(mapOf("knowledge" to 7), gain("2016-04-30", "Social Thought"))
    }

    @Test
    fun `p5r May route restores source activities and actual point values`() {
        val p5r = loadPacks().firstOrNull { it.first == "p5r" }?.third ?: return
        val activities = p5r.activities?.activities?.associateBy { it.id }.orEmpty()
        val days = p5r.walkthroughs.flatMap { it.file.days }.filter { it.date.startsWith("2016-05") }

        for (day in days) {
            for (step in day.steps) {
                val ref = step.activityRef ?: continue
                val base = activities[ref]?.statGains.orEmpty()
                if (base.isNotEmpty() && step.statGains.isNotEmpty()) {
                    assertEquals(base, step.statGains, "P5R ${day.date}: '${step.label}' must match the pre-Craft activity base points")
                }
            }
        }

        fun gain(date: String, text: String): Map<String, Int> =
            days.first { it.date == date }.steps
                .first { it.label.contains(text) && it.statGains.isNotEmpty() }
                .statGains

        assertEquals(mapOf("proficiency" to 3), gain("2016-05-01", "Guy McVer"))
        assertEquals(mapOf("guts" to 2), gain("2016-05-08", "Sunday drink"))
        assertEquals(mapOf("kindness" to 3), gain("2016-05-08", "Hierophant reaches rank 2"))
        assertEquals(mapOf("proficiency" to 5), gain("2016-05-21", "Beef Bowl"))
        assertEquals(mapOf("kindness" to 2), gain("2016-05-22", "Sunday drink"))
        assertEquals(mapOf("knowledge" to 2), gain("2016-05-29", "Sunday drink"))
        assertEquals(mapOf("kindness" to 5), gain("2016-05-29", "Cake Knight"))
        assertEquals(mapOf("knowledge" to 2, "guts" to 2, "proficiency" to 2, "charm" to 2), gain("2016-05-31", "Big Bang Burger"))

        val fatalWoman = p5r.answers?.answers?.single { it.id == "p5r.answers.class.2016-05-07" }
        assertEquals("2016-05-07", fatalWoman?.date)
    }

    @Test
    fun `p5r June route applies Craft of Cinema only after it is read`() {
        val p5r = loadPacks().firstOrNull { it.first == "p5r" }?.third ?: return
        val activities = p5r.activities?.activities?.associateBy { it.id }.orEmpty()
        val days = p5r.walkthroughs.flatMap { it.file.days }.filter { it.date.startsWith("2016-06") }

        for (day in days) {
            for (step in day.steps) {
                val ref = step.activityRef ?: continue
                val base = activities[ref]?.statGains.orEmpty()
                if (base.isEmpty() || step.statGains.isEmpty()) continue

                val expected = if (day.date > "2016-06-23" && ref.startsWith("p5r.activity.dvd.")) {
                    base.mapValues { (_, value) -> value + 2 }
                } else {
                    base
                }
                assertEquals(expected, step.statGains, "P5R ${day.date}: '${step.label}' has the wrong activity point total")
            }
        }

        fun gain(date: String, text: String): Map<String, Int> =
            days.first { it.date == date }.steps
                .first { it.label.contains(text) && it.statGains.isNotEmpty() }
                .statGains

        assertEquals(mapOf("charm" to 3), gain("2016-06-01", "Not-so-hot Betsy"))
        assertEquals(mapOf("charm" to 2), gain("2016-06-05", "Sunday drink"))
        assertEquals(mapOf("charm" to 3), gain("2016-06-05", "Sun reaches rank 2"))
        assertEquals(mapOf("proficiency" to 3), gain("2016-06-07", "Baton Pass rank 3"))
        assertEquals(mapOf("guts" to 3), gain("2016-06-20", "Star Forneus"))
        assertEquals(mapOf("charm" to 3, "guts" to 2), gain("2016-06-21", "bathhouse"))
        assertEquals(mapOf("kindness" to 5), gain("2016-06-22", "Mega Fertilizer"))
        assertEquals(mapOf("kindness" to 5), gain("2016-06-25", "ICU"))
        assertEquals(mapOf("guts" to 2), gain("2016-06-26", "Sunday drink"))
        assertEquals(mapOf("charm" to 3), gain("2016-06-26", "Sun reaches rank 4"))
        assertEquals(mapOf("guts" to 3), gain("2016-06-28", "Star Forneus"))
        assertTrue(days.first { it.date == "2016-06-25" }.steps.any { it.activityRef == "p5r.activity.book.game-secrets" })
    }

    @Test
    fun `p5r August route uses actual point values and active modifiers`() {
        val p5r = loadPacks().firstOrNull { it.first == "p5r" }?.third ?: return
        val days = p5r.walkthroughs.flatMap { it.file.days }.filter { it.date.startsWith("2016-08") }

        fun gain(date: String, text: String): Map<String, Int> =
            days.first { it.date == date }.steps
                .first { it.label.contains(text) && it.statGains.isNotEmpty() }
                .statGains

        assertEquals(mapOf("charm" to 4), gain("2016-08-02", "Devil reaches rank 3"))
        assertEquals(mapOf("knowledge" to 2), gain("2016-08-03", "Island"))
        assertEquals(mapOf("knowledge" to 10), gain("2016-08-04", "Saraemon"))
        assertEquals(mapOf("kindness" to 7), gain("2016-08-05", "flower shop"))
        assertEquals(mapOf("kindness" to 10), gain("2016-08-05", "Le Misérables"))
        assertEquals(mapOf("proficiency" to 2), gain("2016-08-07", "Sunday drink"))
        assertEquals(mapOf("charm" to 7), gain("2016-08-07", "convenience store"))
        assertEquals(mapOf("charm" to 4, "kindness" to 2), gain("2016-08-07", "Crossroads Bar"))
        assertEquals(mapOf("knowledge" to 2), gain("2016-08-08", "Courage"))
        assertEquals(mapOf("charm" to 3, "kindness" to 3), gain("2016-08-08", "Crossroads Bar"))
        assertEquals(mapOf("kindness" to 7), gain("2016-08-10", "flower shop"))
        assertEquals(mapOf("kindness" to 7), gain("2016-08-10", "Mega Fertilizer"))
        assertEquals(mapOf("charm" to 3), gain("2016-08-11", "Sun reaches rank 7"))
        assertEquals(mapOf("charm" to 4), gain("2016-08-12", "convenience store"))
        assertEquals(mapOf("charm" to 4), gain("2016-08-12", "Devil reaches rank 4"))
        assertEquals(mapOf("guts" to 2), gain("2016-08-14", "Sunday drink"))
        assertEquals(mapOf("knowledge" to 2), gain("2016-08-16", "Star reaches rank 5"))
        assertEquals(mapOf("guts" to 2), gain("2016-08-17", "Death reaches rank 8"))
        assertEquals(mapOf("knowledge" to 2), gain("2016-08-17", "Sunburn"))
        assertEquals(mapOf("proficiency" to 2), gain("2016-08-18", "batting cages"))
        assertEquals(mapOf("charm" to 7), gain("2016-08-20", "Sun reaches rank 8"))
        assertEquals(mapOf("charm" to 5), gain("2016-08-21", "D.Housewives"))
        assertEquals(mapOf("knowledge" to 2), gain("2016-08-22", "Star reaches rank 6"))
        assertEquals(mapOf("knowledge" to 2), gain("2016-08-24", "Star reaches rank 7"))
        assertEquals(mapOf("knowledge" to 2), gain("2016-08-25", "Sweltering"))
        assertEquals(mapOf("kindness" to 5), gain("2016-08-26", "Mega Fertilizer"))
        assertEquals(mapOf("kindness" to 2), gain("2016-08-28", "Sunday drink"))
        assertEquals(mapOf("charm" to 7), gain("2016-08-28", "Sun reaches rank 10"))
        assertEquals(mapOf("charm" to 5), gain("2016-08-30", "Devil reaches rank 7"))
        assertEquals(mapOf("charm" to 5), gain("2016-08-31", "D.Housewives"))

        val dHousewives = days.flatMap { it.steps }.filter { it.activityRef == "p5r.activity.dvd.d-housewives" }
        assertEquals(2, dHousewives.size)
        assertTrue(dHousewives[0].label.contains("first viewing"))
        assertTrue(dHousewives[1].label.contains("second viewing"))
    }

    @Test
    fun `p5r September route uses actual point values and removes bogus stat rewards`() {
        val p5r = loadPacks().firstOrNull { it.first == "p5r" }?.third ?: return
        val days = p5r.walkthroughs.flatMap { it.file.days }.filter { it.date.startsWith("2016-09") }

        fun gain(date: String, text: String): Map<String, Int> =
            days.first { it.date == date }.steps
                .first { it.label.contains(text) && it.statGains.isNotEmpty() }
                .statGains

        assertEquals(mapOf("knowledge" to 2), gain("2016-09-02", "Typhoon"))
        assertEquals(mapOf("proficiency" to 3), gain("2016-09-02", "broken laptop"))
        assertEquals(mapOf("knowledge" to 2), gain("2016-09-03", "class question"))
        assertEquals(mapOf("knowledge" to 2), gain("2016-09-04", "Sunday drink"))
        assertEquals(mapOf("knowledge" to 2), gain("2016-09-06", "class question"))
        assertEquals(mapOf("kindness" to 5), gain("2016-09-12", "Mega Fertilizer"))
        assertEquals(mapOf("knowledge" to 2), gain("2016-09-14", "class question"))
        assertEquals(mapOf("knowledge" to 2), gain("2016-09-17", "class question"))
        assertEquals(mapOf("kindness" to 5), gain("2016-09-18", "Mouse M.D."))
        assertEquals(mapOf("kindness" to 3), gain("2016-09-19", "Tower reaches rank 1"))
        assertEquals(mapOf("knowledge" to 2), gain("2016-09-19", "Festival"))
        assertEquals(mapOf("charm" to 7), gain("2016-09-19", "Showtime Redemption"))
        assertEquals(mapOf("kindness" to 3), gain("2016-09-20", "Hierophant reaches rank 4"))
        assertEquals(mapOf("knowledge" to 2), gain("2016-09-21", "class question"))
        assertTrue(
            days.first { it.date == "2016-09-21" }.steps.first { it.label.contains("Councilor reaches rank 7") }.statGains.isEmpty(),
            "Maruki rank 7 must not invent a Knowledge social-stat reward",
        )
        assertEquals(mapOf("knowledge" to 2), gain("2016-09-21", "Star reaches rank 8"))
        assertEquals(mapOf("knowledge" to 2), gain("2016-09-22", "TV game show"))
        assertEquals(mapOf("charm" to 5), gain("2016-09-22", "Devil reaches rank 8"))
        assertEquals(mapOf("guts" to 5), gain("2016-09-24", "Cry of Cthulhu"))
        assertEquals(mapOf("knowledge" to 2), gain("2016-09-24", "class question"))
        assertEquals(mapOf("kindness" to 3), gain("2016-09-24", "Tower reaches rank 2"))
        assertEquals(mapOf("charm" to 3, "guts" to 2), gain("2016-09-24", "Sincere Omelette"))
        assertEquals(mapOf("charm" to 2), gain("2016-09-25", "Sunday drink"))
        assertEquals(mapOf("kindness" to 3), gain("2016-09-26", "Tower reaches rank 3"))
        assertEquals(mapOf("kindness" to 3), gain("2016-09-26", "Hierophant reaches rank 5"))
        assertEquals(mapOf("knowledge" to 2), gain("2016-09-28", "class question"))
        assertEquals(mapOf("knowledge" to 2), gain("2016-09-28", "Ranking"))
        assertEquals(mapOf("kindness" to 5), gain("2016-09-28", "Mega Fertilizer"))
        assertEquals(mapOf("knowledge" to 2), gain("2016-09-29", "class question"))
        assertEquals(mapOf("knowledge" to 2), gain("2016-09-29", "TV game show"))
        assertEquals(mapOf("guts" to 7), gain("2016-09-30", "Master Swordsman"))
    }

    // ---- Media (docs/ROADMAP-v3.md Phase 11): bundled graphics all serve ----

    @Test
    fun `every bundled image is declared exactly once in media json`() {
        loadPacks().forEach { (slug, dir, loaded) ->
            val images = dir.resolve("images")
            if (!images.isDirectory()) return@forEach
            val declared = loaded.media?.media?.map { it.file }?.toSet().orEmpty()
            val bundled = Files.walk(images).use { stream ->
                stream.filter { Files.isRegularFile(it) }
                    .map { dir.relativize(it).joinToString("/") { part -> part.toString() } }
                    .toList()
            }
            assertTrue(bundled.isNotEmpty(), "$slug ships no images but has an images/ dir")
            bundled.forEach { file ->
                assertTrue(file in declared, "$slug: $file is bundled but not declared in media.json")
            }
            assertEquals(bundled.size, declared.size, "$slug: media.json must declare exactly the bundled images")
        }
    }

    @Test
    fun `p3r bond portraits match the named social link character`() {
        val loaded = loadPacks().first { it.first == "p3r" }.third
        val bonds = loaded.bonds!!.bonds.associateBy { it.id }
        val portraits = loaded.media!!.media.filter { it.kind == MediaKinds.PORTRAIT && it.bonds.isNotEmpty() }
        assertTrue(portraits.isNotEmpty())
        portraits.forEach { art ->
            art.bonds.forEach { id ->
                assertEquals(bonds.getValue(id).characterLabel, art.title, "$id must not show another character")
            }
        }
    }

    @Test
    fun `every media bond anchor resolves to a real bond`() {
        loadPacks().forEach { (slug, _, loaded) ->
            val bondIds = loaded.bonds?.bonds?.map { it.id }?.toSet().orEmpty()
            loaded.media?.media?.forEach { item ->
                item.bonds.forEach { bond ->
                    assertTrue(bond in bondIds, "$slug: media '${item.id}' references unknown bond '$bond'")
                }
            }
        }
    }

    @Test
    fun `p5r serves supplied confidant backgrounds on unique bond pages`() {
        val loaded = loadPacks().firstOrNull { it.first == "p5r" }?.third ?: return
        val backgrounds = loaded.media?.media.orEmpty().filter {
            it.kind == MediaKinds.BANNER && it.id.startsWith("p5r.media.confidant.")
        }

        assertEquals(23, backgrounds.size, "all supplied confidant graphics must be declared")
        assertTrue(backgrounds.all { it.bonds.size == 1 }, "each confidant background must target one bond")
        assertEquals(
            23,
            backgrounds.flatMap { it.bonds }.distinct().size,
            "each supplied confidant background must target a different bond",
        )
    }

    @Test
    fun `every pack ships a media manifest covering its graphics`() {
        val counts = mutableMapOf<String, Int>()
        loadPacks().forEach { (slug, _, loaded) ->
            counts[slug] = loaded.media?.media?.size ?: 0
        }
        assertTrue((counts["p5r"] ?: 0) >= 101, "p5r must declare its 101 bundled graphics, found ${counts["p5r"]}")
        assertTrue((counts["p3r"] ?: 0) >= 16, "p3r must declare its 16 guide graphics, found ${counts["p3r"]}")
        assertTrue((counts["metaphor"] ?: 0) >= 47, "metaphor must declare its 47 guide graphics, found ${counts["metaphor"]}")
    }
}
