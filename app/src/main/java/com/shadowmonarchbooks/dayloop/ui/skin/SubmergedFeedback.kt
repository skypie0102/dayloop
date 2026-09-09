package com.shadowmonarchbooks.dayloop.ui.skin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A readable result plane, replacing the inherited inverse-color moon disc. */
@Composable
internal fun SubmergedDayComplete(fx: AdvanceFx, progress: Float) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxSize().alpha(progress)
            .background(Brush.verticalGradient(listOf(colors.primaryContainer, colors.surface, colors.background)))
            .drawBehind {
                val plane = Path().apply {
                    moveTo(0f, size.height * 0.22f)
                    lineTo(size.width, size.height * 0.10f)
                    lineTo(size.width, size.height * 0.30f)
                    lineTo(0f, size.height * 0.42f)
                    close()
                }
                drawPath(plane, colors.secondary.copy(alpha = 0.12f))
            }.testTag("submerged-day-complete")
            .semantics { liveRegion = LiveRegionMode.Polite }
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        fx.dateLabel?.let {
            Text(it, style = MaterialTheme.typography.titleLarge, color = colors.secondary)
        }
        Text("DAY\nCOMPLETE", style = MaterialTheme.typography.displayLarge
            .withSkinFont(LocalSkin.current.type.display).copy(fontSize = 56.sp, lineHeight = 55.sp),
            color = colors.primary)
        Box(Modifier.width(72.dp).height(3.dp).background(colors.tertiary))
        Text("Moving to the next day", style = MaterialTheme.typography.titleMedium, color = colors.onBackground)
        Text("Tap to continue", style = MaterialTheme.typography.labelLarge, color = colors.secondary)
    }
}

/** Compact, dismissible white selection strip with no full-screen scrim. */
@Composable
internal fun SubmergedPerfectDay(onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp),
        color = colors.primary,
        modifier = Modifier.widthIn(max = 340.dp)
            .clickable(role = Role.Button, onClickLabel = "Dismiss perfect day", onClick = onDismiss)
            .semantics { liveRegion = LiveRegionMode.Polite }.testTag("submerged-perfect-day"),
    ) {
        Column(Modifier.drawBehind {
            drawLine(colors.tertiary, Offset.Zero, Offset(size.width, 0f), 3.dp.toPx())
        }.padding(horizontal = 22.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("PERFECT DAY", style = MaterialTheme.typography.headlineMedium
                .withSkinFont(LocalSkin.current.type.display).copy(fontStyle = FontStyle.Italic), color = colors.onPrimary)
            Text("All tasks complete", style = MaterialTheme.typography.bodyMedium, color = colors.onPrimary)
        }
    }
}
