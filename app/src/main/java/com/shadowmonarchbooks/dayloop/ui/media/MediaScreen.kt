package com.shadowmonarchbooks.dayloop.ui.media

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shadowmonarchbooks.dayloop.data.LoadedPack
import com.shadowmonarchbooks.dayloop.pack.schema.MediaItem
import com.shadowmonarchbooks.dayloop.pack.schema.MediaKinds
import com.shadowmonarchbooks.dayloop.ui.DayloopViewModel
import com.shadowmonarchbooks.dayloop.ui.components.EmptyState
import com.shadowmonarchbooks.dayloop.ui.components.MediaImage
import com.shadowmonarchbooks.dayloop.ui.components.SkinHeader
import com.shadowmonarchbooks.dayloop.ui.skin.LocalSkin
import com.shadowmonarchbooks.dayloop.ui.skin.skinDecor
import com.shadowmonarchbooks.dayloop.ui.skin.hasSubmergedChrome

/**
 * The pack's bundled graphics, every declared media.json item grouped by
 * serving kind (docs/ROADMAP-v3.md Phase 11). Anchored items state where they
 * surface; unanchored ones are labelled gallery-only.
 */
@Composable
fun MediaScreen(
    vm: DayloopViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val pack = state.selected ?: run {
        EmptyState("No pack selected.")
        return
    }
    if (LocalSkin.current.hasSubmergedChrome()) {
        SubmergedMedia(pack)
        return
    }
    if (pack.media.isEmpty()) {
        EmptyState("This pack ships no graphics.")
        return
    }

    val skin = LocalSkin.current
    val slash = skin.hasSkin && skin.motion == "slash"
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize().padding(16.dp),
    ) {
        pack.mediaByKind().forEach { (kind, items) ->
            item(key = "kind-$kind") {
                val label = kindLabel(kind, items.size, pack)
                if (slash) {
                    SkinHeader(label, modifier = Modifier.padding(top = 8.dp))
                } else {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            items(items, key = { it.id }) { item ->
                MediaGalleryRow(pack = pack, item = item)
            }
        }
    }
}

@Composable
private fun MediaGalleryRow(pack: LoadedPack, item: MediaItem) {
    val skin = LocalSkin.current
    val slash = skin.hasSkin && skin.motion == "slash"
    val cardShape = skin.shapes.card
    Surface(
        shape = cardShape,
        color = if (slash) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (slash) Modifier.border(1.dp, MaterialTheme.colorScheme.onBackground, cardShape)
                else Modifier,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.skinDecor("panel").padding(10.dp),
        ) {
            Surface(
                shape = skin.shapes.chip,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 0.dp,
                modifier = if (slash) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.onBackground, skin.shapes.chip)
                } else {
                    Modifier
                },
            ) {
                MediaImage(
                    assetPath = pack.assetOf(item),
                    title = item.title,
                    size = 64.dp,
                    modifier = Modifier.padding(6.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (slash) skin.cased(item.title, "display") else item.title,
                    style = if (slash) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (slash) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface,
                )
                item.caption?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (slash) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = anchorText(pack, item),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (slash) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

/**
 * Where this item surfaces, in pack vocabulary. Achievement artwork belongs to
 * the media/gallery layer, not the achievement rule layer: once a pack ships a
 * first-class achievement catalog its media month/day anchors are explicitly
 * labelled as imported guide placement so they cannot be mistaken for trophy
 * unlock dates.
 */
internal fun anchorText(pack: LoadedPack, item: MediaItem): String {
    val guidePlacement = item.kind == MediaKinds.ACHIEVEMENT && pack.achievements.isNotEmpty()
    val anchors = buildList {
        if (item.months.isNotEmpty()) {
            val label = if (guidePlacement) "guide placement months" else "months"
            add("$label: ${item.months.joinToString()}")
        }
        if (item.dates.isNotEmpty()) {
            val label = if (guidePlacement) "guide placement days" else "days"
            add("$label: ${item.dates.joinToString()}")
        }
        item.bonds.forEach { bondId ->
            pack.bonds.firstOrNull { it.id == bondId }?.let { add(it.label) }
        }
    }
    return if (anchors.isEmpty()) "gallery only" else anchors.joinToString(" · ")
}

internal fun kindLabel(kind: String, count: Int, pack: LoadedPack): String {
    val noun = when (kind) {
        MediaKinds.ACHIEVEMENT -> "Achievements"
        MediaKinds.MONTH -> "Month art"
        MediaKinds.SECTION -> "Section markers"
        MediaKinds.DAY -> "Day markers"
        MediaKinds.PORTRAIT -> "Portraits"
        MediaKinds.BANNER -> "Banners"
        MediaKinds.BACKDROP -> "Backdrops"
        MediaKinds.GUIDE -> "Guide art"
        else -> kind.replaceFirstChar { it.uppercase() }
    }
    return "$noun · $count"
}
