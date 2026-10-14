package com.shadowmonarchbooks.dayloop.ui.skin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp

/** Flat, wrapping utility text shares the Reload heading voice without another page backdrop. */
@Composable
internal fun SubmergedUtilityMessage(title: String, message: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.surface, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.drawWithCache {
            onDrawBehind { drawLine(colors.tertiary, Offset.Zero, Offset(size.width, 0f), 3.dp.toPx()) }
        }.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge.withSkinFont(LocalSkin.current.type.display),
                color = colors.secondary, modifier = Modifier.semantics { heading() })
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Static loading planes: unknown progress is named, never represented by a fictitious percentage or idle loop. */
@Composable
internal fun SubmergedLoading(title: String?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(modifier.fillMaxSize().background(colors.background).testTag("p3r-loading")
        .semantics(mergeDescendants = true) {
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            liveRegion = LiveRegionMode.Polite
        }, contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.fillMaxWidth().height(4.dp).background(colors.tertiary))
            Text("NOW LOADING", style = MaterialTheme.typography.headlineLarge.withSkinFont(LocalSkin.current.type.display),
                color = colors.onBackground)
            title?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = colors.secondary) }
            Text("Opening your saved guide…", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
