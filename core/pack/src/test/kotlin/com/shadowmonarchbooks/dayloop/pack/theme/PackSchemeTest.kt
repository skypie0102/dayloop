package com.shadowmonarchbooks.dayloop.pack.theme

import com.shadowmonarchbooks.dayloop.pack.schema.PackTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins the shared seed→scheme mapping (docs/ROADMAP-v3.md Phase 12): the app
 * and packlint must see byte-identical colors for a pack's declared palette,
 * and the contrast rule must hold for every shipped theme.
 */
class PackSchemeTest {

    private fun theme(accent: String, dark: String = accent, style: String? = null) =
        PackTheme(accent = accent, accentDark = dark, style = style)

    @Test
    fun `style tokens are the closed set`() {
        assertEquals(setOf("tonalSpot", "vibrant", "expressive", "content", "ink", "submerged"), THEME_STYLES)
    }

    @Test
    fun `scheme materializes every role for both modes`() {
        val t = theme("#A61E22", "#D9433C", "vibrant")
        assertNotNull(schemeArgb(t, dark = true))
        assertNotNull(schemeArgb(t, dark = false))
        val roles = schemeArgb(t, dark = true)!!
        assertTrue(
            roles.keys.containsAll(CONTRAST_PAIRS.flatMap { listOf(it.first, it.second) }),
            "every contrast pair role must exist in the materialized scheme",
        )
    }

    @Test
    fun `ink scheme stays within black white and accent shades`() {
        val t = theme("#D81800", style = "ink")
        for (dark in listOf(true, false)) {
            val roles = schemeArgb(t, dark)!!
            val allowed = setOf(
                0xFF000000.toInt(),
                0xFF181818.toInt(),
                0xFFF0F0F0.toInt(),
                0xFFFFFFFF.toInt(),
                0xFFD81800.toInt(),
                0xFF8F1000.toInt(),
                0xFFF21B00.toInt(),
            )
            assertTrue(roles.values.all { it in allowed }, "ink scheme introduced an unrelated hue: $roles")
        }
    }

    @Test
    fun `submerged keeps the same palette across system modes`() {
        val t = theme("#09134E", "#1A46CE", "submerged")
        assertEquals(schemeArgb(t, true), schemeArgb(t, false))
        val roles = assertNotNull(schemeArgb(t, true))
        assertTrue(Wcag.relativeLuminance(roles.getValue("background")) < 0.02)
        assertTrue(Wcag.contrastRatio(roles.getValue("primary"), roles.getValue("background")) >= 4.5)
    }

    @Test
    fun `submerged backdrop and deadline labels remain readable on the brightest plane`() {
        // The wash and decorative light blend towards primaryContainer; the
        // normal Material pairs do not cover text rendered over that backdrop.
        for (seed in listOf("#1A46CE", "#FFFFFF", "#000000", "#DCA11E", "#1B5E20")) {
            val roles = assertNotNull(schemeArgb(theme(seed, style = "submerged"), true))
            for (foreground in listOf("primary", "secondary", "onSurfaceVariant", "onBackground", "error")) {
                val ratio = Wcag.contrastRatio(roles.getValue(foreground), roles.getValue("primaryContainer"))
                assertTrue(ratio >= Wcag.AA_NORMAL, "$seed: $foreground on blue frame = $ratio")
            }
        }
    }

    @Test
    fun `scheme without parseable seed is null`() {
        assertNull(schemeArgb(PackTheme(accent = "nothex"), dark = true))
        assertNull(schemeArgb(PackTheme(), dark = false))
    }

    @Test
    fun `wcag math matches known values`() {
        // Black vs white is the maximum ratio; identical colors the minimum.
        assertEquals(21.0, Wcag.contrastRatio(0xFF000000.toInt(), 0xFFFFFFFF.toInt()), 0.01)
        assertEquals(1.0, Wcag.contrastRatio(0xFF808080.toInt(), 0xFF808080.toInt()), 0.001)
        // #767676 on white is the canonical lowest AA-passing gray (~4.54).
        assertTrue(Wcag.contrastRatio(0xFF767676.toInt(), 0xFFFFFFFF.toInt()) >= 4.5)
        assertTrue(Wcag.contrastRatio(0xFF777777.toInt(), 0xFFFFFFFF.toInt()) < 4.5)
    }

    @Test
    fun `every text pair passes AA for a representative seed and all styles`() {
        // Representative seeds across the hue wheel + the lightness extremes a
        // pack might declare; every scheme character must produce AA pairs.
        val seeds = listOf(
            "#A61E22", "#D9433C", "#2E5C8A", "#5C8FCB", "#9A6D07", "#DCA11E",
            "#1B5E20", "#4A148C", "#000000", "#FFFFFF",
        )
        for (seed in seeds) {
            for (style in THEME_STYLES) {
                for (dark in listOf(true, false)) {
                    val t = theme(seed, style = style)
                    val failing = contrastResults(t, dark).filter { it.ratio < Wcag.AA_NORMAL }
                    assertTrue(
                        failing.isEmpty(),
                        "seed $seed style $style dark=$dark fails AA: " +
                            failing.joinToString { "${it.foreground} on ${it.background} = ${it.ratio}" },
                    )
                }
            }
        }
    }

    @Test
    fun `contrast results are empty without seeds`() {
        assertTrue(contrastResults(PackTheme(), dark = true).isEmpty())
    }
}
