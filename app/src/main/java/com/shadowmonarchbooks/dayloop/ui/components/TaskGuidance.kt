package com.shadowmonarchbooks.dayloop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.shadowmonarchbooks.dayloop.pack.schema.Step
import com.shadowmonarchbooks.dayloop.ui.skin.LocalSkin
import com.shadowmonarchbooks.dayloop.ui.skin.hasPaintedChrome
import com.shadowmonarchbooks.dayloop.ui.skin.hasSubmergedChrome
import com.shadowmonarchbooks.dayloop.ui.skin.withSkinFont

/** Shared inline/dialog reading surface. Guidance has no progress controls or motion. */
@Composable
internal fun TaskGuidance(step: Step, index: Int? = null) {
    if (!step.hasGuidance) return
    val skin = LocalSkin.current
    val submerged = skin.hasSubmergedChrome()
    val painted = skin.hasPaintedChrome()
    val colors = MaterialTheme.colorScheme
    val bodyStyle = if (submerged) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge
    // Preserve the original plain-text surface (including its semantics/tag) for legacy packs.
    if (!submerged && step.guidanceSections.isEmpty()) {
        step.tip?.let { tip ->
            Text(tip, style = bodyStyle, color = colors.onSurface,
                modifier = Modifier.fillMaxWidth().then(
                    if (index != null) Modifier.testTag("task-guidance-$index") else Modifier))
        }
        return
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().then(
            if (index != null) Modifier.testTag("task-guidance-$index") else Modifier),
    ) {
        step.tip?.let { tip ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (submerged) Text("Guide", style = MaterialTheme.typography.labelMedium,
                    fontStyle = FontStyle.Italic, color = colors.secondary)
                Text(tip, style = bodyStyle, color = colors.onSurface)
            }
        }
        step.guidanceSections.forEachIndexed { sectionIndex, section ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (submerged) Box(Modifier.size(3.dp, 18.dp).background(colors.tertiary))
                    Text(section.title,
                        style = if (submerged || painted) MaterialTheme.typography.titleMedium.withSkinFont(skin.type.display)
                            else MaterialTheme.typography.titleSmall,
                        fontStyle = if (submerged) FontStyle.Italic else null,
                        color = if (submerged || painted) colors.secondary else colors.onSurface,
                        modifier = Modifier.weight(1f).semantics { heading() }.then(
                            if (index != null) Modifier.testTag("task-guidance-$index-section-$sectionIndex-title") else Modifier))
                }
                Text(section.body, style = bodyStyle, color = colors.onSurface,
                    modifier = Modifier.fillMaxWidth().then(
                        if (index != null) Modifier.testTag("task-guidance-$index-section-$sectionIndex-body") else Modifier))
            }
        }
    }
}
