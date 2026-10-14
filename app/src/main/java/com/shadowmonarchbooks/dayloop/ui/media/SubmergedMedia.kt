package com.shadowmonarchbooks.dayloop.ui.media

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shadowmonarchbooks.dayloop.data.LoadedPack
import com.shadowmonarchbooks.dayloop.pack.schema.MediaItem
import com.shadowmonarchbooks.dayloop.ui.skin.SubmergedSectionHeading
import com.shadowmonarchbooks.dayloop.ui.skin.SubmergedUtilityMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A keyed, lazy gallery; enlarged type gives descriptions the whole row instead of squeezing them beside art. */
@Composable
internal fun SubmergedMedia(pack: LoadedPack) {
    val groups = remember(pack) { pack.mediaByKind() }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize().testTag("p3r-media-list")) {
        item(key = "intro") { SubmergedUtilityMessage(pack.pack.title,
            if (pack.media.isEmpty()) "This guide has no gallery graphics."
            else "Guide artwork and where it appears. Guide placement dates describe the source layout; they do not unlock achievements.") }
        groups.forEach { (kind, media) ->
            item(key = "kind-$kind") { SubmergedSectionHeading(kindLabel(kind, media.size, pack),
                Modifier.semantics(mergeDescendants = true) { heading() }) }
            items(media, key = { it.id }) { item -> SubmergedMediaRow(pack, item) }
        }
    }
}

@Composable
internal fun SubmergedMediaRow(pack: LoadedPack, item: MediaItem) {
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("media-${item.id}")) {
        val stacked = maxWidth / LocalDensity.current.fontScale < 300.dp
        val text: @Composable (Modifier) -> Unit = { modifier ->
            Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold,
                    color = colors.secondary, modifier = Modifier.semantics { heading() })
                item.caption?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                Text(anchorText(pack, item), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
        Surface(color = colors.surface) {
            if (stacked) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SubmergedMediaPreview(pack.assetOf(item), Modifier.fillMaxWidth().height(112.dp))
                    text(Modifier.fillMaxWidth())
                }
            } else {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    SubmergedMediaPreview(pack.assetOf(item), Modifier.size(96.dp))
                    text(Modifier.weight(1f))
                }
            }
        }
    }
}

private data class PreviewImage(val loading: Boolean = true, val bitmap: ImageBitmap? = null)

/** Decode once per visible asset on IO. Fit preserves its bytes, colors and full aspect ratio. */
@Composable
internal fun SubmergedMediaPreview(assetPath: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
    val density = LocalDensity.current
    val targetWidth = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
    val targetHeight = with(density) { maxHeight.roundToPx() }.coerceAtLeast(1)
    val image by produceState(PreviewImage(), assetPath, targetWidth, targetHeight) {
        val bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.assets.open(assetPath).use { BitmapFactory.decodeStream(it, null, options) }
                var sample = 1
                while (options.outWidth / (sample * 2) >= targetWidth || options.outHeight / (sample * 2) >= targetHeight) sample *= 2
                options.inJustDecodeBounds = false; options.inSampleSize = sample
                context.assets.open(assetPath).use { BitmapFactory.decodeStream(it, null, options)?.asImageBitmap() }
            }.getOrNull()
        }
        value = PreviewImage(loading = false, bitmap = bitmap)
    }
        image.bitmap?.let { Image(it, null, contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().testTag("media-image-$assetPath")) } ?: Text(
            if (image.loading) "Opening artwork…" else "Artwork unavailable",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("media-preview-status"))
    }
}
