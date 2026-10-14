package com.shadowmonarchbooks.dayloop.ui.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadowmonarchbooks.dayloop.data.LoadedPack
import com.shadowmonarchbooks.dayloop.data.formatDate
import com.shadowmonarchbooks.dayloop.pack.schema.Routes
import com.shadowmonarchbooks.dayloop.ui.DayloopViewModel
import com.shadowmonarchbooks.dayloop.ui.components.PackIcon
import com.shadowmonarchbooks.dayloop.ui.components.rememberAssetImage
import com.shadowmonarchbooks.dayloop.ui.skin.LocalSkin
import com.shadowmonarchbooks.dayloop.ui.skin.packShape
import com.shadowmonarchbooks.dayloop.ui.skin.rememberAnimationsDisabled
import com.shadowmonarchbooks.dayloop.ui.skin.hasSubmergedChrome
import com.shadowmonarchbooks.dayloop.ui.skin.SubmergedLoading
import com.shadowmonarchbooks.dayloop.ui.theme.packColorScheme
import kotlinx.coroutines.delay

/**
 * First-run game picker (docs/ROADMAP-v2.md Phase 7): a swipeable carousel of
 * cover-art cards, one per installed pack. Fully engine-neutral — titles,
 * date ranges, day counts, and the art all come from pack data; Kotlin never
 * names a game.
 *
 * The same screen is the app's single game picker (docs/ROADMAP-v3.md
 * Phase 11): when a game is already selected, Settings' Game section lands
 * here with [onCancel] wired (back arrow, current game preselected, saved
 * profile counts on the cards).
 */
@Composable
fun OnboardingScreen(
    vm: DayloopViewModel,
    onStart: () -> Unit,
    onCancel: (() -> Unit)? = null,
) {
    val state by vm.state.collectAsState()
    val animationsDisabled = rememberAnimationsDisabled()
    val packs = remember(state.packs) { state.packs.sortedBy { it.pack.pickerOrder } }
    val profileCounts by vm.profileCounts.collectAsState()
    val reselecting = state.selectedSlug != null
    var pendingSelection by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = pendingSelection != null) { /* Commit the chosen game before leaving the loading bridge. */ }
    LaunchedEffect(state.selectedSlug, pendingSelection) {
        if (pendingSelection != null && state.selectedSlug == pendingSelection) {
            // Keep a warm pack switch on screen long enough to read as an
            // intentional transition. Cold packs remain covered while loading.
            if (!hasReloadSelection(packs, pendingSelection) && !animationsDisabled) delay(650)
            pendingSelection = null
            onStart()
        }
    }
    if (packs.isEmpty()) {
        GamePickerUnavailable(onCancel)
        return
    }
    if (LocalSkin.current.hasSubmergedChrome()) {
        Box(Modifier.fillMaxSize()) {
            SubmergedGamePicker(packs, state.selectedSlug, profileCounts, onCancel) { slug ->
                if (pendingSelection == null) { pendingSelection = slug; vm.selectPack(slug) }
            }
            pendingSelection?.let { slug -> packs.firstOrNull { it.slug == slug }?.let { PackLoadingOverlay(it) } }
        }
        return
    }
    val pagerState = rememberPagerState(
        initialPage = packs.indexOfFirst { it.slug == state.selectedSlug }.coerceAtLeast(0),
        pageCount = { packs.size },
    )

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(vertical = 28.dp)) {
        Column(Modifier.padding(horizontal = 28.dp)) {
            if (reselecting && onCancel != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to settings")
                    }
                    Text(
                        text = "Choose a game",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            Text(
                text = "dayloop",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (reselecting) {
                    "Switching games never drops saves — each game keeps its own profiles and clock."
                } else {
                    "Track a calendar-JRPG run day by day — what to do now, " +
                        "and what you're about to miss. Your progress is saved per game."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))
            // Skinned packs set the picker's display voice (ROADMAP-v3 Phase
            // 13); token-less packs keep the engine title look byte-stable.
            val skin = LocalSkin.current
            if (skin.hasSkin) {
                Text(
                    text = skin.cased("What are we tracking?", "display"),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Text(
                    text = "What are we tracking?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 16.dp),
            contentPadding = PaddingValues(horizontal = 48.dp),
            pageSpacing = 14.dp,
        ) { page ->
            GameCard(
                pack = packs[page],
                selected = page == pagerState.currentPage,
                savedProfiles = profileCounts[packs[page].slug] ?: 0,
                onSelect = {
                    if (pendingSelection == null) {
                        pendingSelection = packs[page].slug
                        vm.selectPack(packs[page].slug)
                    }
                },
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 4.dp),
        ) {
            repeat(packs.size) { i ->
                val active = i == pagerState.currentPage
                val dotScale by animateFloatAsState(if (active) 1f else 2f / 3f,
                    animationSpec = tween(if (animationsDisabled) 0 else 140), label = "dot")
                Box(
                    Modifier
                        .size(9.dp).graphicsLayer { scaleX = dotScale; scaleY = dotScale }
                        .clip(CircleShape)
                        .background(
                            if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                        ),
                )
            }
        }
        }

        pendingSelection?.let { slug ->
            packs.firstOrNull { it.slug == slug }?.let { PackLoadingOverlay(it) }
        }
    }
}

/** A pack-owned-feeling bridge while the selected guide is initialized. */
@Composable
private fun PackLoadingOverlay(pack: LoadedPack) {
    if (pack.pack.theme?.chrome == "submerged") {
        SubmergedLoading(pack.pack.title)
        return
    }
    val scheme = pack.pack.theme?.let { packColorScheme(it, dark = true) }
    val motif = pack.pack.theme?.motif
    val background = when (motif) {
        "masks" -> Color.Black
        "moon" -> Color(0xFF030A28)
        "crown" -> Color(0xFF11100D)
        else -> scheme?.background ?: MaterialTheme.colorScheme.background
    }
    val foreground = scheme?.onBackground ?: MaterialTheme.colorScheme.onBackground
    val accent = scheme?.primary ?: MaterialTheme.colorScheme.primary
    val animationsDisabled = rememberAnimationsDisabled()
    val progress = remember(pack.slug) { Animatable(0f) }

    LaunchedEffect(pack.slug, animationsDisabled) {
        if (animationsDisabled) {
            progress.snapTo(0.65f)
        } else {
            while (true) {
                progress.snapTo(0f)
                progress.animateTo(1f, tween(300, easing = LinearEasing))
                delay(600)
            }
        }
    }

    val message = when (motif) {
        "masks" -> "LOADING"
        "moon" -> "NOW LOADING"
        "crown" -> "PREPARING JOURNEY"
        else -> "LOADING"
    }
    Surface(
        color = background,
        contentColor = foreground,
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            PackLoadingMotif(
                motif = motif,
                phase = { progress.value },
                accent = accent,
                foreground = foreground,
                modifier = Modifier.size(220.dp),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 64.dp, start = 24.dp, end = 24.dp),
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = if (motif == "masks") 30.sp else 24.sp,
                    ),
                    fontWeight = FontWeight.Black,
                    color = if (motif == "masks") accent else foreground,
                    maxLines = 1,
                )
                Text(
                    text = pack.pack.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = foreground.copy(alpha = 0.76f),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun PackLoadingMotif(
    motif: String?,
    phase: () -> Float,
    accent: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
) {
    when (motif) {
        "masks" -> Canvas(modifier.graphicsLayer { rotationZ = -8f }) {
            val sweep = size.width * (phase() * 1.8f - 0.4f)
            val slash = Path().apply {
                moveTo(sweep - size.width * 0.28f, 0f)
                lineTo(sweep + size.width * 0.08f, 0f)
                lineTo(sweep + size.width * 0.28f, size.height)
                lineTo(sweep - size.width * 0.08f, size.height)
                close()
            }
            drawPath(slash, accent)
            drawLine(
                color = Color.White,
                start = Offset(sweep - size.width * 0.17f, 0f),
                end = Offset(sweep + size.width * 0.05f, size.height),
                strokeWidth = 4.dp.toPx(),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = size.minDimension * 0.22f,
                style = Stroke(width = 5.dp.toPx()),
            )
        }
        "moon" -> Canvas(modifier.graphicsLayer { rotationZ = phase() * 360f }) {
            val radius = size.minDimension * 0.25f
            drawCircle(color = foreground.copy(alpha = 0.16f), radius = size.minDimension * 0.48f)
            drawCircle(color = foreground, radius = radius)
            drawCircle(
                color = Color(0xFF030A28),
                radius = radius,
                center = Offset(center.x + radius * (0.85f - phase() * 0.7f), center.y),
            )
            drawCircle(
                color = accent,
                radius = size.minDimension * 0.42f,
                style = Stroke(width = 3.dp.toPx()),
            )
            drawCircle(
                color = foreground,
                radius = 5.dp.toPx(),
                center = Offset(center.x + size.minDimension * 0.42f, center.y),
            )
        }
        "crown" -> Canvas(modifier.graphicsLayer {
            val pulse = 0.92f + phase() * 0.08f
            scaleX = pulse
            scaleY = pulse
            rotationZ = (phase() - 0.5f) * 5f
        }) {
            val outer = Path().apply {
                moveTo(center.x, size.height * 0.08f)
                lineTo(size.width * 0.88f, center.y)
                lineTo(center.x, size.height * 0.92f)
                lineTo(size.width * 0.12f, center.y)
                close()
            }
            val crown = Path().apply {
                moveTo(size.width * 0.27f, size.height * 0.57f)
                lineTo(size.width * 0.32f, size.height * 0.35f)
                lineTo(center.x, size.height * 0.49f)
                lineTo(size.width * 0.68f, size.height * 0.35f)
                lineTo(size.width * 0.73f, size.height * 0.57f)
                close()
            }
            drawPath(outer, color = accent.copy(alpha = 0.28f))
            drawPath(outer, color = accent, style = Stroke(width = 3.dp.toPx()))
            drawPath(crown, color = foreground)
            drawLine(
                color = accent,
                start = Offset(size.width * 0.28f, size.height * 0.64f),
                end = Offset(size.width * 0.72f, size.height * 0.64f),
                strokeWidth = 4.dp.toPx(),
            )
        }
        else -> Canvas(modifier) {
            drawCircle(
                color = accent,
                radius = size.minDimension * 0.36f,
                style = Stroke(width = 5.dp.toPx()),
            )
            drawArc(
                color = foreground,
                startAngle = phase() * 360f,
                sweepAngle = 80f,
                useCenter = false,
                style = Stroke(width = 6.dp.toPx()),
            )
        }
    }
}

@Composable
private fun GameCard(
    pack: LoadedPack,
    selected: Boolean,
    savedProfiles: Int,
    onSelect: () -> Unit,
) {
    val cover = rememberAssetImage(pack.cardAsset)
    // Each card wears its own pack's accent (docs/ROADMAP-v2.md Phase 10),
    // not the active pack's — the carousel previews every installed skin.
    val packAccent = pack.pack.theme?.let { packColorScheme(it, dark = true).primary }
        ?: MaterialTheme.colorScheme.primary
    // …and each card wears its own pack's card silhouette (ROADMAP-v3 Phase
    // 13) — shape-only resolution, no font loading per preview card.
    val packCardShape = packShape(
        theme = pack.pack.theme,
        slot = "card",
        fallback = RoundedCornerShape(24.dp),
        density = LocalDensity.current,
    )
    Surface(
        onClick = onSelect,
        shape = packCardShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = if (selected) BorderStroke(3.dp, packAccent) else null,
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(Modifier.fillMaxSize()) {
            val coverArt = cover
            if (coverArt != null) {
                Image(
                    bitmap = coverArt,
                    contentDescription = pack.pack.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.55f)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.85f),
                            ),
                        ),
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(18.dp),
                ) {
                    Text(
                        text = pack.pack.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = metaLine(pack, savedProfiles),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                    featureText(pack)?.let { features ->
                        Text(
                            text = features,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }
                }
            } else {
                // Art-free fallback: centered monogram tile with themed text.
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                ) {
                    PackIcon(pack.iconAsset, pack.pack.title, size = 96.dp)
                    Text(
                        text = pack.pack.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = metaLine(pack, savedProfiles),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    featureText(pack)?.let { features ->
                        Text(
                            text = features,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

internal fun metaLine(pack: LoadedPack, savedProfiles: Int): String {
    val base = "${pack.sortedDates(Routes.defaultId(pack.pack)).size} authored days · ${dateRange(pack)}"
    return when (savedProfiles) {
        0 -> base
        1 -> "$base · 1 saved profile"
        else -> "$base · $savedProfiles saved profiles"
    }
}

internal fun dateRange(pack: LoadedPack): String =
    "${formatDate(pack.pack.calendar.startDate, pack.calendar)} → " +
        formatDate(pack.pack.calendar.endDate, pack.calendar)

/** Engine-neutral capability summary; packs without extras get no line at all. */
internal fun featureText(pack: LoadedPack): String? {
    val features = buildList {
        if (pack.pack.capabilities.answers) add("exam answers")
        if (pack.pack.routes.size > 1) add("${pack.pack.routes.size} routes")
    }
    return features.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}
