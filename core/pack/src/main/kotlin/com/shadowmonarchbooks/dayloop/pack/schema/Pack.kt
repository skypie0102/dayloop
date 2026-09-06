package com.shadowmonarchbooks.dayloop.pack.schema

import kotlinx.serialization.Serializable

/**
 * pack.json — pack identity, calendar, and capability manifest (docs/PLAN.md §3.2).
 */
@Serializable
data class Pack(
    /** Immutable short slug, e.g. "p5r". Never reused across packs. */
    val packId: String,
    /** Display title of the game this pack covers. */
    val title: String,
    /** Bumped on every content change; saves stamp packId @ contentVersion. */
    val contentVersion: Int,
    /** Display order in the game picker; lower values appear first. */
    val pickerOrder: Int = Int.MAX_VALUE,
    /** "weekdayGrid" (P3R/P5R) or "dayCounter" (Metaphor). */
    val timeModel: String,
    val calendar: CalendarRange,
    /** Time slots the engine advances through, e.g. afternoon/evening. */
    val slots: List<Slot>,
    /** Social stats, engine-neutral ids with pack-supplied labels. */
    val stats: List<StatDef>,
    /**
     * Authored walkthrough routes (docs/PLAN.md Phase 5 "routes"): e.g. a
     * completion route and a casual one. Empty means the pack ships a single
     * implicit default route ("standard") at walkthrough/ top level.
     */
    val routes: List<RouteDef> = emptyList(),
    val capabilities: Capabilities = Capabilities(),
    /** Pack-supplied display vocabulary for engine terms (docs/PLAN.md §3.1). */
    val labels: Labels = Labels(),
    /**
     * Pack-supplied visual identity (docs/ROADMAP-v2.md Phase 10 / PLAN.md
     * §3.5): seed colors, scheme style, and named art slots. Null = the
     * engine's own skin. Switching packs switches the skin with zero code
     * change — the pack supplies everything.
     */
    val theme: PackTheme? = null,
)

@Serializable
data class RouteDef(
    /** Immutable route id, e.g. "standard". Profiles pin one (docs/PLAN.md §3.7). */
    val id: String,
    /** Pack-supplied display label, e.g. "Completion" / "Casual". */
    val label: String,
    val description: String? = null,
)

/** Route identity helpers shared by the loader, lint, and the app. */
object Routes {
    /** The implicit route every pack has, even when none are declared. */
    const val DEFAULT = "standard"

    /** Declared routes, or the single implicit default when none are declared. */
    fun effective(pack: Pack): List<RouteDef> =
        pack.routes.ifEmpty { listOf(RouteDef(DEFAULT, "Standard")) }

    /** The id a profile should fall back to for [pack]. */
    fun defaultId(pack: Pack): String =
        pack.routes.firstOrNull()?.id ?: DEFAULT
}

@Serializable
data class CalendarRange(
    /** Inclusive ISO dates, e.g. "2016-04-09". */
    val startDate: String,
    val endDate: String,
    /** Days inside the range the player cannot act on (story-only, travel). */
    val nonPlayableDates: List<String> = emptyList(),
    /**
     * Game-days per month for `dayCounter` packs whose months differ from the
     * real calendar, starting at [startDate]'s month. A day-of-month above 31
     * is representable as long as it is declared here. Empty = real month
     * lengths; `weekdayGrid` packs must leave this empty.
     */
    val monthLengths: List<Int> = emptyList(),
    /**
     * In-game weekday cycle for `dayCounter` packs whose week differs from the
     * real 7-day week — engine-neutral lowercase tokens, e.g. a 5-day week.
     * Empty = mon..sun; `weekdayGrid` packs must leave this empty.
     */
    val weekdayCycle: List<String> = emptyList(),
    /** The date + cycle token [weekdayCycle] is anchored to; required when the cycle is declared. */
    val weekdayAnchor: WeekdayAnchor? = null,
)

@Serializable
data class WeekdayAnchor(
    /** ISO date inside the calendar the anchor applies to. */
    val date: String,
    /** One of [CalendarRange.weekdayCycle]'s tokens. */
    val weekday: String,
)

@Serializable
data class Slot(val id: String, val label: String)

@Serializable
data class StatDef(val id: String, val label: String)

/**
 * Closed-set capability manifest (docs/PLAN.md §3.1): additive booleans the
 * engine reads, never per-game flags. packlint cross-checks every declared
 * capability against the files the pack actually ships.
 */
@Serializable
data class Capabilities(
    val exams: Boolean = false,
    val weather: Boolean = false,
    /** Pack ships structured answer sheets (exams + class questions) in answers.json. */
    val answers: Boolean = false,
    /** Pack ships a task-linked Mementos request catalog. */
    val mementosRequests: Boolean = false,
    val requests: Boolean = false,
)

@Serializable
data class Labels(
    /** e.g. "Confidant" (P5R) / "Social Link" (P3/P4) / "Follower" (Metaphor). */
    val bond: String = "Bond",
    /** e.g. "Social Stat" / "Royal Virtue". */
    val stat: String = "Stat",
    /**
     * Pack display names for the closed-set deadline kinds (docs/ROADMAP-v2.md
     * Phase 10: vocabulary the UI prints stays pack-driven), e.g.
     * `{ "palace": "Mission" }` for a pack whose dungeon deadlines aren't
     * "palaces". Kinds not listed fall back to the capitalized token.
     */
    val deadlineKinds: Map<String, String> = emptyMap(),
) {
    /** Display name for a deadline kind token: pack override, else capitalized token. */
    fun deadlineKind(kind: String): String =
        deadlineKinds[kind] ?: kind.replaceFirstChar { it.uppercase() }
}

/**
 * Pack-supplied visual identity (docs/PLAN.md §3.5 / ROADMAP-v2 Phase 10).
 * The engine maps [accent]/[accentDark] seeds to full hand-tuned Material 3
 * dark/light schemes — no colors or game names live in Kotlin. Art slots name
 * bundled files relative to the pack dir so swapping art is a content change;
 * the engine reads the slots it knows ("card", "icon"), extra slots ride
 * along for future surfaces.
 *
 * ROADMAP-v3 Phase 12 grows this into the skin DSL: [shapes], [typography],
 * [decor] and [motion] are optional layers a pack composes; absent layers
 * fall back to [motif]'s family defaults, then to the engine look. Packs
 * without any of them render byte-identically to the engine.
 */
@Serializable
data class PackTheme(
    /** Seed color for the light scheme: "#RRGGBB" or "#AARRGGBB". */
    val accent: String? = null,
    /** Seed color for the dark scheme; falls back to [accent] when omitted. */
    val accentDark: String? = null,
    /**
     * Closed-set scheme character token: "tonalSpot" (calm default),
     * "vibrant" (bold), "expressive" (playful), "content" (source-anchored).
     * packlint validates the token; the engine maps it to a scheme variant.
     */
    val style: String? = null,
    /**
     * Decorative selector (docs/ROADMAP-v3.md Phase 12: promoted from a
     * reserved token): a closed-set token the engine maps to a decoration
     * family — silhouettes, painter and motion defaults. Explicit [shapes]/
     * [decor]/[motion] tokens override the family per slot. packlint
     * validates the token; packs declaring none keep the engine look.
     */
    val motif: String? = null,
    /** Named art slots, e.g. `{ "card": "art/card.png", "icon": "art/icon.png" }`. */
    val art: Map<String, String> = emptyMap(),
    /** Per-slot silhouette tokens (docs/ROADMAP-v3.md Phase 12). Null = family/engine default. */
    val shapes: SkinShapes? = null,
    /** Pack-bundled fonts + role tuning (docs/ROADMAP-v3.md Phase 12). Null roles = engine type. */
    val typography: SkinTypography? = null,
    /** Named decoration art slots, e.g. `{ "header": "art/header.png", "panel": "art/panel.png" }`. */
    val decor: Map<String, String> = emptyMap(),
    /** Closed-set motion token: "slash" | "fade" | "flip" | "none". Null = engine default. */
    val motion: String? = null,
    /**
     * Named sound slots (docs/ROADMAP-v3.md Phase 16), e.g.
     * `{ "tap": "art/sfx/tap.ogg" }`. Slots are the closed set in
     * [SkinTokens.SFX_SLOTS]; every declared file is linted for existence,
     * extension (.ogg) and a ≤100 KB budget. Optional — packs that ship no
     * audio keep the app silent, and playback only ever happens after the
     * user enables "Skin sounds" in Settings.
     */
    val sfx: Map<String, String> = emptyMap(),
    /** Optional chrome composition token; null preserves the existing shell. */
    val chrome: String? = null,
) {
    /** The scheme seed for [dark] mode as an ARGB int, or null when undeclared. */
    fun seedArgb(dark: Boolean): Int? {
        val hex = if (dark) accentDark ?: accent else accent
        return hex?.let { parseHexColor(it) }
    }

    companion object {
        /** Parses "#RRGGBB"/"RRGGBB"/"#AARRGGBB"/"AARRGGBB" into an ARGB int; null when malformed. */
        fun parseHexColor(hex: String): Int? {
            val digits = hex.removePrefix("#")
            val value = digits.toLongOrNull(16) ?: return null
            return when (digits.length) {
                6 -> (0xFF000000L or value).toInt()
                8 -> value.toInt()
                else -> null
            }
        }
    }
}

/**
 * Per-slot silhouette tokens (docs/ROADMAP-v3.md Phase 12). Every slot is
 * optional; a null slot resolves to the [PackTheme.motif] family default,
 * then to the engine's rounded-corner look. Tokens come from the closed set
 * in `pack.theme.SkinTokens` (jagged / slash / cut / ribbon / diamond …),
 * validated by packlint.
 */
@Serializable
data class SkinShapes(
    /** Cards and wide panels (step rows, dossier, banners). */
    val card: String? = null,
    /** Small pill/tag containers (day-kind chips, slot tags). */
    val chip: String? = null,
    /** Full-width section/page headers. */
    val header: String? = null,
    /** Bordered emphasis containers (deadline banners, plaques). */
    val frame: String? = null,
)

/**
 * Pack-bundled font roles (docs/ROADMAP-v3.md Phase 12). Broad roles override
 * matching Material typography throughout the app; the accent role is
 * opt-in at selected surfaces. Null roles keep the engine type. Files live
 * under the pack dir (conventionally `art/fonts/`), linted for existence,
 * extension and size.
 */
@Serializable
data class SkinTypography(
    /** Selective accent text — special dates and celebratory moments. */
    val accent: SkinFont? = null,
    /** Hero/display type — big headers and moment text. */
    val display: SkinFont? = null,
    /** Titles — screen titles, card headings. */
    val title: SkinFont? = null,
    /** Body text — step labels, notes. Null = engine default font. */
    val body: SkinFont? = null,
)

/** One bundled font role: the file plus its tuning knobs (all optional). */
@Serializable
data class SkinFont(
    /** Pack-relative path to a .ttf/.otf file, e.g. "art/fonts/display.ttf". */
    val file: String,
    /** Closed-set case transform applied at render: "upper" today. */
    val case: String? = null,
    /** Slant the role's text. */
    val italic: Boolean = false,
    /** Extra letter spacing in em units (0.0–0.30, linted). */
    val tracking: Double? = null,
)
