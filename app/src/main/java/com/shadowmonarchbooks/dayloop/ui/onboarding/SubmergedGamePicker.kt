package com.shadowmonarchbooks.dayloop.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shadowmonarchbooks.dayloop.data.LoadedPack
import com.shadowmonarchbooks.dayloop.pack.schema.Routes
import com.shadowmonarchbooks.dayloop.ui.components.PackIcon
import com.shadowmonarchbooks.dayloop.ui.components.rememberAssetImage
import com.shadowmonarchbooks.dayloop.ui.skin.SubmergedTopBar
import com.shadowmonarchbooks.dayloop.ui.skin.SubmergedUtilityMessage
import com.shadowmonarchbooks.dayloop.ui.skin.SkinOutlinedActionButton

/** Readable list choices replace a tall, cropped carousel only in explicitly Reload-owned chrome. */
@Composable
internal fun SubmergedGamePicker(
    packs: List<LoadedPack>, selectedSlug: String?, profileCounts: Map<String, Int>,
    onCancel: (() -> Unit)?, onSelect: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().testTag("p3r-game-picker")) {
        SubmergedTopBar("Choose game", onCancel != null, { onCancel?.invoke() }, {}, false, {}, showUtilities = false)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f).testTag("p3r-game-list")) {
            item(key = "intro") { SubmergedUtilityMessage("Your guides", "Each game keeps its own profiles and clock. Choose a guide to continue your saved run.") }
            items(packs, key = { it.slug }) { pack ->
                val count = profileCounts[pack.slug] ?: 0
                val current = pack.slug == selectedSlug
                val art = rememberAssetImage(pack.cardAsset)
                Surface(color = colors.surface, modifier = Modifier.fillMaxWidth().testTag("game-choice-${pack.slug}")
                    .clickable(role = Role.Button, onClickLabel = "Open ${pack.pack.title}", onClick = { onSelect(pack.slug) })
                    .semantics { stateDescription = if (current) "Current game" else "Saved game" }) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(64.dp).clearAndSetSemantics {}) {
                                if (art != null) Image(art, null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                                else PackIcon(pack.iconAsset, pack.pack.title, size = 64.dp)
                            }
                            Text(pack.pack.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold,
                                color = colors.secondary, modifier = Modifier.weight(1f))
                        }
                        Text("${pack.sortedDates(Routes.defaultId(pack.pack)).size} authored days", style = MaterialTheme.typography.bodyMedium)
                        Text(dateRange(pack), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        featureText(pack)?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        Text(buildString {
                            if (current) append("Current game · ")
                            append("$count ${if (count == 1) "saved profile" else "saved profiles"}")
                        }, style = MaterialTheme.typography.labelLarge, color = if (current) colors.tertiary else colors.secondary)
                    }
                }
            }
        }
    }
}

/** The delay belongs to the old carousel's transition; Reload selection waits only for real readiness. */
internal fun hasReloadSelection(packs: List<LoadedPack>, slug: String?): Boolean =
    packs.firstOrNull { it.slug == slug }?.pack?.theme?.chrome == "submerged"

@Composable
internal fun GamePickerUnavailable(onCancel: (() -> Unit)?) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("No guides available", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() })
        Text("Your game guides could not be opened. Reopen dayloop to try again. Your saved progress is kept.",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
        onCancel?.let { SkinOutlinedActionButton("Back", onClick = it) }
    }
}
