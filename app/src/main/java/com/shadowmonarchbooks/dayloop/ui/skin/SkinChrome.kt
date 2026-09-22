package com.shadowmonarchbooks.dayloop.ui.skin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** Engine-neutral navigation item consumed by the skin-aware bottom chrome. */
data class SkinNavItem(val route: String, val label: String, val icon: ImageVector)

private fun SkinSpec.isSlashChrome(): Boolean = hasSkin && motion == "slash"

/**
 * App-wide background treatment. Slash-language skins use a black field with
 * a restrained red haze instead of opaque geometric fields. Today can opt out
 * of the haze because it supplies its own pack-authored day/night scene.
 *
 * The content itself stays on the ordinary layout grid: these shapes are a
 * composition layer only, so readable text and accessible touch bounds never
 * inherit the visual skew.
 */
@Composable
fun Modifier.skinBackdrop(skin: SkinSpec, showHaze: Boolean = true): Modifier {
    if (skin.hasSubmergedChrome()) return this.submergedBackdrop()
    if (!skin.isSlashChrome()) return this
    val colors = MaterialTheme.colorScheme
    val background = Color.Black
    if (!showHaze) return this.background(background)
    return this.background(background).drawBehind {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(colors.primary.copy(alpha = 0.18f), Color.Transparent),
                center = Offset(size.width * 0.88f, size.height * 0.10f),
                radius = size.width * 0.92f,
            ),
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(colors.primary.copy(alpha = 0.09f), Color.Transparent),
                center = Offset(size.width * 0.05f, size.height * 0.88f),
                radius = size.width * 0.72f,
            ),
        )
    }
}

/**
 * Top app chrome. The slash family intentionally stops looking like a stock
 * Material toolbar: black field, layered red cut title plate, offset
 * white paper edge, and hard icon treatment. All other skins keep the previous
 * Material bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkinTopBar(
    title: String,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    settingsEnabled: Boolean = true,
    onOpenSettings: () -> Unit,
) {
    val skin = LocalSkin.current
    val colors = MaterialTheme.colorScheme
    // Top-bar destinations use the pack display face. Selective accent fonts
    // are reserved for date and moment treatments rather than app chrome.
    val bannerTitleStyle = MaterialTheme.typography.displaySmall.copy(
        fontSize = MaterialTheme.typography.headlineLarge.fontSize,
        lineHeight = MaterialTheme.typography.headlineLarge.lineHeight,
    )
    Column(modifier = Modifier.background(colors.surface)) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars),
        )
        if (skin.hasSubmergedChrome()) {
            SubmergedTopBar(title, canGoBack, onBack, onOpenSearch, settingsEnabled, onOpenSettings)
        } else if (!skin.isSlashChrome()) {
            TopAppBar(
                modifier = Modifier
                    .background(colors.surface)
                    .skinDecor("header"),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    if (canGoBack) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                title = { Text(title, style = bannerTitleStyle, maxLines = 1) },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        enabled = settingsEnabled,
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        } else {
            Surface(color = colors.background, shadowElevation = 0.dp) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = PersonaToolbarHeight)
                        .drawBehind {
                            drawLine(
                                color = colors.onBackground,
                                start = Offset(0f, size.height - 1f),
                                end = Offset(size.width, size.height - 1f),
                                strokeWidth = 2f,
                            )
                        }
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                ) {
                    if (canGoBack) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = colors.onBackground,
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 3.dp, bottom = 3.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .offset(x = 3.dp, y = 3.dp)
                                .background(colors.onBackground, skin.shapes.header),
                        )
                        Surface(
                            shape = skin.shapes.header,
                            color = colors.primary,
                            shadowElevation = 0.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer { rotationZ = -1.2f }
                                .border(1.dp, colors.background, skin.shapes.header),
                        ) {
                            Text(
                                text = skin.cased(title, "display"),
                                style = bannerTitleStyle,
                                color = colors.onPrimary,
                                maxLines = 1,
                                modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 1.dp, bottom = 1.dp),
                            )
                        }
                    }
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "Search", tint = colors.onBackground)
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        enabled = settingsEnabled,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = colors.onBackground,
                            disabledContentColor = colors.onBackground.copy(alpha = 0.38f),
                        ),
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            }
        }
    }
}

/**
 * Bottom navigation for all skins. Slash-language packs replace Material's
 * pill indicator and tinted container with black ink, a visually rotated red
 * selection shard, and white keylines. The parent touch targets remain square
 * and stable; only the decorative selection layer rotates.
 */
@Composable
fun SkinBottomBar(
    items: List<SkinNavItem>,
    selectedRoute: String?,
    onSelect: (String) -> Unit,
) {
    val skin = LocalSkin.current
    val colors = MaterialTheme.colorScheme
    if (skin.hasSubmergedChrome()) {
        SubmergedBottomBar(items, selectedRoute, onSelect)
        return
    }
    if (!skin.isSlashChrome()) {
        NavigationBar {
            items.forEach { item ->
                NavigationBarItem(
                    selected = selectedRoute == item.route,
                    onClick = {
                        if (selectedRoute != item.route) onSelect(item.route)
                    },
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    label = null,
                    alwaysShowLabel = false,
                )
            }
        }
        return
    }

    Surface(
        color = colors.background,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = colors.onBackground,
                    start = Offset(0f, 1f),
                    end = Offset(size.width, 1f),
                    strokeWidth = 2f,
                )
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 5.dp, vertical = 5.dp),
        ) {
            items.forEachIndexed { index, item ->
                val selected = selectedRoute == item.route
                val content = if (selected) colors.onPrimary else colors.onBackground
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 54.dp)
                        .clickable {
                            if (!selected) onSelect(item.route)
                        }
                        .padding(horizontal = 2.dp, vertical = 5.dp),
                ) {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .padding(horizontal = 3.dp, vertical = 2.dp)
                                .graphicsLayer { rotationZ = if (index % 2 == 0) -2.4f else 2.0f }
                                .background(colors.primary, skin.shapes.chip)
                                .border(1.dp, colors.onBackground, skin.shapes.chip),
                        )
                    }
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = content,
                        modifier = Modifier.size(23.dp),
                    )
                }
            }
        }
    }
}

/** A small route/source banner that follows the same generic skin vocabulary. */
@Composable
fun SkinRouteBadge(text: String, modifier: Modifier = Modifier) {
    val skin = LocalSkin.current
    val colors = MaterialTheme.colorScheme
    if (!skin.isSlashChrome()) {
        Surface(
            shape = if (skin.hasSkin) skin.shapes.chip else MaterialTheme.shapes.small,
            color = colors.surfaceVariant,
            contentColor = colors.onSurfaceVariant,
            shadowElevation = 0.dp,
            modifier = modifier,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
        return
    }

    Box(modifier = modifier.padding(end = 3.dp, bottom = 3.dp)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 3.dp)
                .background(colors.onBackground, skin.shapes.chip),
        )
        Surface(
            shape = skin.shapes.chip,
            color = colors.primary,
            contentColor = colors.onPrimary,
            shadowElevation = 0.dp,
            modifier = Modifier
                .graphicsLayer { rotationZ = -1.2f }
                .border(1.dp, colors.background, skin.shapes.chip),
        ) {
            Text(
                text = skin.cased(text, "display"),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
    }
}
