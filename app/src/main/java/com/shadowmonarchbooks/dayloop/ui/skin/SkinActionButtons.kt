package com.shadowmonarchbooks.dayloop.ui.skin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Skin-aware primary action.
 *
 * Slash-language skins replace Material's tonal button container with a flat
 * accent command strip, white pasted-paper offset and hard keyline. Other
 * skins keep the stock Material button byte-for-byte at the call site.
 */
@Composable
fun SkinActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fillWidth: Boolean = false,
    largeLabel: Boolean = false,
) {
    SlashActionButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        fillWidth = fillWidth,
        largeLabel = largeLabel,
        treatment = SlashActionTreatment.Primary,
        fallback = {
            Button(onClick = onClick, enabled = enabled, modifier = modifier) {
                Text(text)
            }
        },
    )
}

/** Skin-aware outlined action; non-slash skins retain Material OutlinedButton. */
@Composable
fun SkinOutlinedActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fillWidth: Boolean = false,
) {
    SlashActionButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        fillWidth = fillWidth,
        largeLabel = false,
        treatment = SlashActionTreatment.Outlined,
        fallback = {
            OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier) {
                Text(text)
            }
        },
    )
}

/** Skin-aware low-emphasis action; non-slash skins retain Material TextButton. */
@Composable
fun SkinTextActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fillWidth: Boolean = false,
) {
    SlashActionButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        fillWidth = fillWidth,
        largeLabel = false,
        treatment = SlashActionTreatment.Text,
        fallback = {
            TextButton(onClick = onClick, enabled = enabled, modifier = modifier) {
                Text(text)
            }
        },
    )
}

/**
 * Container-free command used when the page artwork should remain visible.
 * It keeps the pack's display face and a full accessible touch target without
 * drawing a plate, border, shadow, or pasted-paper offset.
 */
@Composable
fun SkinBareActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
    largeLabel: Boolean = false,
) {
    val skin = LocalSkin.current
    val label = if (skin.hasSkin) skin.cased(text, "display") else text
    val style = if (skin.hasSkin && largeLabel) {
        MaterialTheme.typography.displaySmall
    } else if (skin.hasSkin) {
        MaterialTheme.typography.displaySmall.copy(
            fontSize = MaterialTheme.typography.titleLarge.fontSize,
            lineHeight = MaterialTheme.typography.titleLarge.lineHeight,
        )
    } else {
        MaterialTheme.typography.labelLarge
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = 52.dp)
            .widthIn(min = 48.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.38f }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(text = label, style = style, color = contentColor, textAlign = TextAlign.Center)
    }
}

private enum class SlashActionTreatment { Primary, Outlined, Text }

@Composable
private fun SlashActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    fillWidth: Boolean,
    largeLabel: Boolean,
    treatment: SlashActionTreatment,
    fallback: @Composable () -> Unit,
) {
    val skin = LocalSkin.current
    if (skin.hasSubmergedChrome()) {
        SubmergedActionButton(text, onClick, modifier, enabled,
            primary = treatment == SlashActionTreatment.Primary, fillWidth = fillWidth, largeLabel = largeLabel)
        return
    }
    if (!skin.hasSkin || skin.motion != "slash") {
        fallback()
        return
    }

    val colors = MaterialTheme.colorScheme
    val primary = treatment == SlashActionTreatment.Primary
    val container = if (primary) colors.primary else colors.background
    val content = if (primary) Color.White else colors.primary
    val border = if (primary) colors.background else colors.onBackground
    val horizontalPadding = if (treatment == SlashActionTreatment.Text) 10.dp else 14.dp
    val buttonModifier = if (fillWidth) Modifier.fillMaxWidth() else Modifier
    val labelModifier = if (fillWidth) {
        Modifier.fillMaxWidth().padding(horizontal = horizontalPadding, vertical = 5.dp)
    } else {
        Modifier.padding(horizontal = horizontalPadding, vertical = 5.dp)
    }
    val labelStyle = if (largeLabel) {
        MaterialTheme.typography.displaySmall
    } else {
        MaterialTheme.typography.displaySmall.copy(
            fontSize = MaterialTheme.typography.titleLarge.fontSize,
            lineHeight = MaterialTheme.typography.titleLarge.lineHeight,
        )
    }

    Box(
        modifier = modifier
            .graphicsLayer { alpha = if (enabled) 1f else 0.38f }
            .padding(end = 3.dp, bottom = 3.dp),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 3.dp)
                .background(colors.onBackground, skin.shapes.header),
        )
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = skin.shapes.header,
            color = container,
            contentColor = content,
            border = BorderStroke(1.dp, border),
            shadowElevation = 0.dp,
            modifier = buttonModifier
                .heightIn(min = 42.dp)
                .graphicsLayer { rotationZ = -1.2f },
        ) {
            Text(
                text = skin.cased(text, "display"),
                style = labelStyle,
                textAlign = TextAlign.Center,
                modifier = labelModifier,
            )
        }
    }
}
