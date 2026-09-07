package com.shadowmonarchbooks.dayloop.ui.theme

import androidx.compose.ui.graphics.Color
import com.shadowmonarchbooks.dayloop.pack.GameCalendar
import com.shadowmonarchbooks.dayloop.pack.PackLoader
import com.shadowmonarchbooks.dayloop.pack.schema.PackTheme
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.fileSize
import kotlin.io.path.name
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Phase 10 (docs/ROADMAP-v2.md): the pack's declared theme drives a full
 * Material 3 scheme for both modes, the bundled packs ship valid themes and
 * art slots, and switching packs actually switches the accent. Runs only
 * where the repo checkout is present (no-ops otherwise).
 */
class PackThemeTest {

    /** The repo's content/packs directory, or null when not running in a checkout. */
    private fun contentPacksDir(): Path? {
        var dir: Path? = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
        repeat(5) {
            val candidate = dir?.resolve("content")?.resolve("packs")
            if (candidate != null && candidate.isDirectory()) return candidate
            dir = dir?.parent
        }
        return null
    }

    private fun loadPacks(): List<Pair<String, Path>> {
        val root = contentPacksDir() ?: return emptyList()
        val packs = Files.list(root).use { stream ->
            stream.filter { it.isDirectory() }.sorted()
                .map { it.name to it }.toList()
        }
        assertTrue(packs.isNotEmpty(), "no packs found under $root")
        return packs
    }

    private fun themeOf(slug: String): PackTheme {
        val root = contentPacksDir() ?: error("no content checkout")
        val pack = PackLoader.load(root.resolve(slug)).pack
        assertNotNull(pack, "$slug must decode")
        val theme = pack.theme
        assertNotNull(theme, "$slug must declare a theme (Phase 10)")
        return theme
    }

    @Test
    fun `every bundled pack declares a parseable theme with both seeds`() {
        listOf("p5r", "p3r", "metaphor").forEach { slug ->
            val theme = themeOf(slug)
            assertNotNull(PackTheme.parseHexColor(theme.accent ?: ""), "$slug light accent")
            assertNotNull(PackTheme.parseHexColor(theme.accentDark ?: ""), "$slug dark accent")
            assertNotNull(theme.seedArgb(dark = false), "$slug light seed")
            assertNotNull(theme.seedArgb(dark = true), "$slug dark seed")
            assertTrue("card" in theme.art, "$slug must declare the onboarding card art slot")
            assertTrue("icon" in theme.art, "$slug must declare the icon art slot")
        }
    }

    @Test
    fun `every declared art slot resolves to a real image file`() {
        loadPacks().forEach { (slug, dir) ->
            val pack = PackLoader.load(dir).pack ?: return@forEach
            val theme = pack.theme ?: return@forEach
            theme.art.forEach { (slot, rel) ->
                val file = dir.resolve(rel)
                assertTrue(file.isRegularFile(), "$slug: theme.art['$slot'] missing file $rel")
                assertTrue(
                    rel.substringAfterLast('.').lowercase() in setOf("png", "jpg", "jpeg", "webp"),
                    "$slug: theme.art['$slot'] must be an image: $rel",
                )
            }
        }
    }

    @Test
    fun `pack themes build full dark and light schemes`() {
        listOf("p5r", "p3r", "metaphor").forEach { slug ->
            val theme = themeOf(slug)
            listOf(true, false).forEach { dark ->
                val scheme = packColorScheme(theme, dark)
                assertTrue(scheme.primary != scheme.error, "$slug dark=$dark primary collides with error")
                assertTrue(scheme.primary != scheme.surface, "$slug dark=$dark primary collides with surface")
                assertTrue(scheme.onSurface != scheme.surface, "$slug dark=$dark text unreadable")
            }
        }
    }

    @Test
    fun `on-primary pairs hold readable contrast in both modes`() {
        listOf("p5r", "p3r", "metaphor").forEach { slug ->
            val theme = themeOf(slug)
            listOf(true, false).forEach { dark ->
                val scheme = packColorScheme(theme, dark)
                val ratio = contrastRatio(scheme.primary, scheme.onPrimary)
                assertTrue(ratio >= 3.0, "$slug dark=$dark primary/onPrimary contrast $ratio < 3.0")
            }
        }
    }

    @Test
    fun `switching packs switches the accent`() {
        val primaries = listOf("p5r", "p3r", "metaphor").map { slug ->
            packColorScheme(themeOf(slug), dark = true).primary
        }
        assertEquals(3, primaries.map { it.value }.distinct().size, "all three packs must recolor: $primaries")
    }

    @Test
    fun `theme without colors falls back to the engine skin`() {
        val fallback = packColorScheme(PackTheme(), dark = true)
        assertEquals(Color(0xFFE8B84B), fallback.primary, "engine lantern primary expected")
    }

    // ---- Phase 13 (docs/ROADMAP-v3.md): the Phantom skin data ----

    @Test
    fun `p5r declares the phantom skin data`() {
        val theme = themeOf("p5r")
        assertEquals("masks", theme.motif)
        assertNotNull(theme.shapes, "p5r must declare shapes")
        assertEquals("cut", theme.shapes?.card)
        assertEquals("slash", theme.shapes?.chip)
        assertEquals("slash", theme.shapes?.header)
        assertEquals("cut", theme.shapes?.frame)
        assertEquals("slash", theme.motion, "p5r motion token")
        val accent = assertNotNull(theme.typography?.accent, "p5r must declare its selective accent font role")
        assertEquals("art/fonts/menu.otf", accent.file)
        assertEquals("upper", accent.case, "accent case token")
        val display = assertNotNull(theme.typography?.display, "p5r must declare a display font role")
        assertEquals("art/fonts/display.ttf", display.file)
        assertEquals("upper", display.case, "display case token")
        assertTrue(display.italic, "display must be italic")
        assertTrue("header" !in theme.decor, "p5r header must use the procedural cutline painter")
        assertTrue("divider" in theme.decor, "p5r decor divider slot")
        assertEquals("art/today-day.png", theme.art["today-day"])
        assertEquals("art/today-night.png", theme.art["today-night"])
    }

    @Test
    fun `p5r bundles its display and menu fonts`() {
        val root = contentPacksDir() ?: error("no content checkout")
        val dir = root.resolve("p5r")
        val font = dir.resolve("art/fonts/display.ttf")
        assertTrue(font.isRegularFile(), "bundled display font missing")
        assertTrue(font.fileSize() <= 2L * 1024 * 1024, "font exceeds the 2 MB cap")
        assertTrue(font.fileSize() > 10_000, "font suspiciously small — truncated download?")
        val head = Files.readAllBytes(font).take(4)
        // TTF magic 00 01 00 00 — a real TrueType file, not a stray download.
        assertEquals(listOf<Byte>(0, 1, 0, 0), head, "display.ttf must start with the TTF magic")
        val menuFont = dir.resolve("art/fonts/menu.otf")
        assertTrue(menuFont.isRegularFile(), "bundled top-bar menu font missing")
        assertTrue(menuFont.fileSize() <= 2L * 1024 * 1024, "menu font exceeds the 2 MB cap")
        assertTrue(menuFont.fileSize() > 10_000, "menu font suspiciously small — truncated copy?")
        assertEquals(
            listOf<Byte>(0, 1, 0, 0),
            Files.readAllBytes(menuFont).take(4),
            "menu.otf must start with valid sfnt magic",
        )
        assertTrue(dir.resolve("art/fonts/OFL.txt").isRegularFile(), "OFL license must ship beside the font")
    }

    @Test
    fun `p5r decor art files exist on disk`() {
        val root = contentPacksDir() ?: error("no content checkout")
        val dir = root.resolve("p5r")
        val theme = themeOf("p5r")
        theme.decor.forEach { (slot, rel) ->
            assertTrue(dir.resolve(rel).isRegularFile(), "p5r theme.decor['$slot'] missing file $rel")
        }
    }

    // ---- Phase 15 (docs/ROADMAP-v3.md): the Royal skin data ----

    @Test
    fun `metaphor declares the royal skin data`() {
        val theme = themeOf("metaphor")
        assertEquals("crown", theme.motif)
        assertNotNull(theme.shapes, "metaphor must declare shapes")
        assertEquals("plaque", theme.shapes?.card, "parchment plaques")
        assertEquals("seal", theme.shapes?.chip, "wax-stamp chips")
        assertEquals("plaque", theme.shapes?.header, "ornate plaque headers")
        assertEquals("plaque", theme.shapes?.frame, "plaque frames")
        assertEquals("flip", theme.motion, "royal motion is the page turn")
        val display = assertNotNull(theme.typography?.display, "metaphor must declare a display font role")
        assertEquals("art/fonts/display.ttf", display.file)
        assertEquals("upper", display.case, "display case token")
        assertTrue(!display.italic, "engraved roman type is upright")
        assertTrue((display.tracking ?: 0.0) > 0.0, "generous engraved tracking")
        assertTrue("header" in theme.decor, "metaphor decor header slot")
        assertTrue("panel" in theme.decor, "metaphor decor panel slot")
        assertTrue("divider" in theme.decor, "metaphor decor divider slot")
        // Royal seeds over the expressive scheme (docs/references/metaphor-ui.md §2).
        assertEquals("#9E815F", theme.accent, "brass light seed")
        assertEquals("#BEA52A", theme.accentDark, "ornament-gold dark seed")
        assertEquals("expressive", theme.style)
    }

    @Test
    fun `metaphor bundles its display font and license`() {
        val root = contentPacksDir() ?: error("no content checkout")
        val dir = root.resolve("metaphor")
        val font = dir.resolve("art/fonts/display.ttf")
        assertTrue(font.isRegularFile(), "bundled display font missing")
        assertTrue(font.fileSize() <= 2L * 1024 * 1024, "font exceeds the 2 MB cap")
        assertTrue(font.fileSize() > 10_000, "font suspiciously small — truncated download?")
        val head = Files.readAllBytes(font).take(4)
        assertEquals(listOf<Byte>(0, 1, 0, 0), head, "display.ttf must start with the TTF magic")
        assertTrue(dir.resolve("art/fonts/OFL.txt").isRegularFile(), "OFL license must ship beside the font")
    }

    @Test
    fun `metaphor decor art files exist on disk`() {
        val root = contentPacksDir() ?: error("no content checkout")
        val dir = root.resolve("metaphor")
        val theme = themeOf("metaphor")
        theme.decor.forEach { (slot, rel) ->
            assertTrue(dir.resolve(rel).isRegularFile(), "metaphor theme.decor['$slot'] missing file $rel")
        }
    }

    @Test
    fun `the three skins keep distinct motion languages`() {
        // Each pack's transition grammar stays its own: no skin bleeds into
        // another (Phase 14's slash-language gating, generalized).
        assertEquals(
            listOf("slash", "fade", "flip"),
            listOf("p5r", "p3r", "metaphor").map { themeOf(it).motion },
        )
    }

    @Test
    fun `royal skin leaves the dayCounter month grid intact`() {
        // Phase 15 acceptance: the Royal skin must not break the dayCounter
        // month grid — 30-day game months and the cycle weekday math. The
        // skin touches presentation only; the calendar contract is re-pinned
        // here with the skin data in place.
        val root = contentPacksDir() ?: error("no content checkout")
        val pack = PackLoader.load(root.resolve("metaphor")).pack ?: error("metaphor must decode")
        val cal = assertNotNull(GameCalendar.of(pack.calendar), "travel calendar constructs")
        assertEquals(
            listOf("2100-06", "2100-07", "2100-08", "2100-09", "2100-10"),
            cal.monthKeys,
            "five authored game months",
        )
        // June starts on the 2nd (the journey's first day) — 29 authored
        // dates; July–September are full 30-day game months; October runs to
        // the journey's last day, the 26th.
        assertEquals(29, cal.daysInMonth("2100-06"))
        assertEquals(30, cal.daysInMonth("2100-07"))
        assertEquals(30, cal.daysInMonth("2100-08"))
        assertEquals(30, cal.daysInMonth("2100-09"))
        assertEquals(26, cal.daysInMonth("2100-10"))
        assertEquals(5, cal.cycleTokens.size, "five-day game week")
        assertEquals("watersday", cal.weekdayOf("2100-06-02"), "anchor weekday")
        assertEquals("arboursday", cal.weekdayOf("2100-06-03"), "cycle advances")
        assertEquals("idlesday", cal.weekdayOf("2100-06-05"), "the rest day")
        assertEquals(3, cal.cyclePosition("2100-06-02"), "watersday's position in the cycle")
        // End of calendar: 2010-01-31-style terminus equivalent for the travel pack.
        assertEquals("2100-10-26", cal.endDate, "the journey's last day")
    }

    // P3R-1: replacement shell opt-in; legacy panel/motion work remains staged.

    @Test
    fun `p3r declares replacement chrome without changing other pack opt-ins`() {
        val theme = themeOf("p3r")
        assertEquals("moon", theme.motif)
        assertEquals("submerged", theme.chrome)
        assertEquals("submerged", theme.style)
        assertNotNull(theme.shapes)
        assertEquals("diamond", theme.shapes?.chip)
        assertEquals("diamond", theme.shapes?.header)
        assertEquals("fade", theme.motion)
        val display = assertNotNull(theme.typography?.display)
        assertEquals("art/fonts/menu-italic.ttf", display.file)
        assertEquals(null, display.case, "titles preserve authored case")
        assertEquals(-0.015, display.tracking)
        assertTrue(display.italic, "menu display face is italic")
        assertTrue("header" in theme.decor)
        assertEquals("#09134E", theme.accent)
        assertEquals("#1A46CE", theme.accentDark)
        assertEquals(null, themeOf("p5r").chrome, "protected pack keeps existing chrome")
        assertEquals(null, themeOf("metaphor").chrome, "other pack keeps existing chrome")
        assertEquals(packColorScheme(theme, true).background, packColorScheme(theme, false).background)
        assertEquals(packColorScheme(theme, true).primary, packColorScheme(theme, false).primary)
    }

    @Test
    fun `p3r bundles its display font and license`() {
        val root = contentPacksDir() ?: error("no content checkout")
        val dir = root.resolve("p3r")
        val font = dir.resolve("art/fonts/menu-italic.ttf")
        assertTrue(font.isRegularFile(), "bundled display font missing")
        assertTrue(font.fileSize() <= 2L * 1024 * 1024, "font exceeds the 2 MB cap")
        assertTrue(font.fileSize() > 10_000, "font suspiciously small — truncated download?")
        val head = Files.readAllBytes(font).take(4)
        assertEquals(listOf<Byte>(0, 1, 0, 0), head, "menu-italic.ttf must start with the TTF magic")
        assertTrue(dir.resolve("art/fonts/RobotoCondensed-OFL.txt").isRegularFile(), "OFL license must ship beside the font")
    }

    @Test
    fun `p3r decor art files exist on disk`() {
        val root = contentPacksDir() ?: error("no content checkout")
        val dir = root.resolve("p3r")
        val theme = themeOf("p3r")
        theme.decor.forEach { (slot, rel) ->
            assertTrue(dir.resolve(rel).isRegularFile(), "p3r theme.decor['$slot'] missing file $rel")
        }
    }

    @Test
    fun `moon marker anchors pin exactly the ten accepted dates`() {
        // Phase 14 acceptance: "the moon icon must appear on exactly the nine
        // full-moon dates + 2010-01-31 already anchored in media.json".
        val root = contentPacksDir() ?: error("no content checkout")
        val media = PackLoader.decodeMedia(
            String(Files.readAllBytes(root.resolve("p3r").resolve("media.json"))),
        )?.media.orEmpty()
        val dayAnchored = media.filter { it.kind == "day" && it.dates.isNotEmpty() }
        val dates = dayAnchored.flatMap { it.dates }.toSet()
        assertEquals(10, dates.size, "exactly ten moon-marked dates expected")
        assertEquals(
            setOf(
                "2009-04-18", "2009-05-10", "2009-06-09", "2009-07-08", "2009-08-07",
                "2009-09-06", "2009-10-05", "2009-11-04", "2009-12-03", "2010-01-31",
            ),
            dates,
            "nine full moons + the Promised Day",
        )
    }

    // ---- contrast helper (WCAG relative luminance) ----

    private fun linearChannel(v: Double): Double =
        if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)

    private fun luminance(c: Color): Double =
        0.2126 * linearChannel(c.red.toDouble()) +
            0.7152 * linearChannel(c.green.toDouble()) +
            0.0722 * linearChannel(c.blue.toDouble())

    private fun contrastRatio(a: Color, b: Color): Double {
        val la = luminance(a) + 0.05
        val lb = luminance(b) + 0.05
        return maxOf(la, lb) / minOf(la, lb)
    }
}
