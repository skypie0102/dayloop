package com.shadowmonarchbooks.dayloop.ui.skin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Skin-aware section heading used by dense utility screens such as Settings.
 *
 * Slash-language skins render the heading as a layered pasted-paper ribbon:
 * a bright accent shard over an offset white edge with a black keyline. The
 * visual layer rotates slightly, while the surrounding layout remains stable.
 * Other skins retain the ordinary Material section heading.
 */
@Composable
fun SkinSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    val skin = LocalSkin.current
    if (skin.hasSubmergedChrome()) {
        SubmergedSectionHeading(text, modifier)
        return
    }
    if (!skin.hasSkin || skin.motion != "slash") {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = modifier,
        )
        return
    }

    val colors = MaterialTheme.colorScheme
    val labelStyle = MaterialTheme.typography.displaySmall.copy(
        fontSize = MaterialTheme.typography.titleLarge.fontSize,
        lineHeight = MaterialTheme.typography.titleLarge.lineHeight,
    )
    Box(modifier = modifier.padding(end = 3.dp, bottom = 3.dp)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 3.dp)
                .background(colors.onBackground, skin.shapes.header),
        )
        Surface(
            shape = skin.shapes.header,
            color = colors.primary,
            contentColor = Color.White,
            shadowElevation = 0.dp,
            modifier = Modifier
                .graphicsLayer { rotationZ = -1.2f }
                .border(1.dp, colors.background, skin.shapes.header),
        ) {
            Text(
                text = skin.cased(text, "display"),
                style = labelStyle,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
            )
        }
    }
}
