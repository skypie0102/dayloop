package com.shadowmonarchbooks.dayloop.ui.skin

import android.content.ContentValues
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.compose
import androidx.glance.appwidget.provideContent
import androidx.test.platform.app.InstrumentationRegistry
import com.shadowmonarchbooks.dayloop.data.LoadedPack
import com.shadowmonarchbooks.dayloop.pack.PackLoader
import com.shadowmonarchbooks.dayloop.ui.media.SubmergedMedia
import com.shadowmonarchbooks.dayloop.ui.media.SubmergedMediaPreview
import com.shadowmonarchbooks.dayloop.ui.onboarding.GamePickerUnavailable
import com.shadowmonarchbooks.dayloop.ui.theme.DayloopTheme
import com.shadowmonarchbooks.dayloop.widget.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

/** Component evidence. Widget images come from actual Glance RemoteViews, not a Compose imitation. */
class SubmergedUtilityTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val pack by lazy { LoadedPack("p3r", requireNotNull(PackLoader.decodePack(
        context.assets.open("p3r/pack.json").bufferedReader().use { it.readText() }))) }

    private fun capture(name: String) {
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/dayloop-ui")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = requireNotNull(context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
        requireNotNull(context.contentResolver.openOutputStream(uri)).use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
        values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0)
        context.contentResolver.update(uri, values, null, null)
    }

    @Test fun loadingNamesUnknownProgressAndRemainsStaticAtNarrowDoubleText() {
        val fontReady = AtomicBoolean(false)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                DayloopTheme(pack) {
                    val skin = LocalSkin.current
                    SideEffect { fontReady.set(skin.type.display?.family != null) }
                    Box(Modifier.fillMaxSize()) { Box(Modifier.width(220.dp).fillMaxHeight()) { SubmergedLoading(pack.pack.title) } }
                }
            }
        }
        compose.waitUntil(15_000) { fontReady.get() }
        compose.onNodeWithTag("p3r-loading").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate))
        compose.onNodeWithText("NOW LOADING", useUnmergedTree = true).assertIsDisplayed()
        capture("p3r-loading-narrow-double-text")
        val before = compose.onRoot().captureToImage().asAndroidBitmap()
        compose.mainClock.advanceTimeBy(3_000)
        val after = compose.onRoot().captureToImage().asAndroidBitmap()
        assertTrue("Loading has no idle animation", before.sameAs(after))
        before.recycle(); after.recycle()
    }

    @Test fun missingGuidesEmptyGalleryAndBrokenArtworkExposeReadableFallbacks() {
        val mode = mutableStateOf(0)
        var back = false
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                DayloopTheme(pack) { Box(Modifier.fillMaxSize().widthIn(max = 240.dp)) {
                    when (mode.value) {
                        0 -> GamePickerUnavailable { back = true }
                        1 -> SubmergedMedia(pack)
                        else -> SubmergedMediaPreview("p3r/not-a-real-asset.png", Modifier.fillMaxWidth().height(112.dp))
                    }
                } }
            }
        }
        compose.onNodeWithText("No guides available").assertIsDisplayed()
        compose.onNodeWithText("Back", ignoreCase = true).performScrollTo().performClick()
        compose.runOnIdle { assertTrue(back) }
        capture("p3r-missing-guides-double-text")
        compose.runOnIdle { mode.value = 1 }
        compose.onNodeWithText("This guide has no gallery graphics.").assertIsDisplayed()
        capture("p3r-media-empty-double-text")
        compose.runOnIdle { mode.value = 2 }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Artwork unavailable").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Artwork unavailable").assertIsDisplayed()
        capture("p3r-media-unavailable-double-text")
    }

    private class FixtureWidget(private val snapshot: WidgetSnapshot) : GlanceAppWidget() {
        override val sizeMode = SizeMode.Single
        override suspend fun provideGlance(context: Context, id: GlanceId) { provideContent { DayloopWidgetContent(snapshot) } }
    }

    @Test fun actualWidgetsFitAllSupportedSizesAtNormalAndDoubleTextWithUniquePackActions() {
        val snapshot = WidgetSnapshot(packSlug = "p3r", packTitle = pack.pack.title,
            date = "2009-05-19", dateLabel = "Tue, May 19", profileName = "Android review",
            doneCount = 3, totalCount = 5, deadlineLabel = "Full Moon — Arqa", deadlineDays = 2,
            skin = widgetSkinSnapshot(pack.pack.theme))
        assertFalse(widgetLaunchIntent(context, "p3r").filterEquals(widgetLaunchIntent(context, "p5r")))
        val cases = listOf(180 to 75, 250 to 110, 320 to 160).flatMap { size -> listOf(1f, 2f).map { size to it } }
        data class Host(val index: Int, val scaled: Context, val remote: android.widget.RemoteViews, val size: DpSize)
        val selected = mutableStateOf<Host?>(null)
        var rendered: View? = null
        compose.setContent {
            DayloopTheme(pack) {
                Column(Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background).padding(16.dp)) {
                    selected.value?.let { host ->
                        Text("${host.size.width.value.toInt()} × ${host.size.height.value.toInt()} · ${cases[host.index].second} text")
                        key(host.index) { AndroidView(factory = {
                            FrameLayout(host.scaled).also { frame ->
                                rendered = host.remote.apply(host.scaled, frame)
                                frame.addView(rendered)
                            }
                        }, modifier = Modifier.size(host.size).testTag("widget-host")) }
                    }
                }
            }
        }
        cases.forEachIndexed { index, (dimensions, scale) ->
            val scaled = context.createConfigurationContext(Configuration(context.resources.configuration).apply { fontScale = scale })
            val size = DpSize(dimensions.first.dp, dimensions.second.dp)
            val remote = runBlocking { FixtureWidget(snapshot).compose(scaled, size = size) }
            compose.runOnIdle { selected.value = Host(index, scaled, remote, size) }
            compose.onNodeWithTag("widget-host").assertIsDisplayed().assertWidthIsEqualTo(size.width).assertHeightIsEqualTo(size.height)
            compose.runOnIdle {
                val root = requireNotNull(rendered)
                fun descendants(view: View): List<View> = listOf(view) + if (view is ViewGroup)
                    (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) } else emptyList()
                val views = descendants(root)
                assertTrue(views.any { it.contentDescription?.toString() == widgetDescription(snapshot) && it.isClickable })
                val visible = views.filterIsInstance<TextView>().filter { it.visibility == View.VISIBLE && it.text.isNotBlank() }
                assertTrue(visible.any { it.text.contains("May 19") })
                val rootLocation = IntArray(2).also { root.getLocationOnScreen(it) }
                visible.forEach { text ->
                    val layout = requireNotNull(text.layout)
                    assertTrue("Widget text must not ellipsize: ${text.text}", (0 until layout.lineCount).all { layout.getEllipsisCount(it) == 0 })
                    val location = IntArray(2).also { text.getLocationOnScreen(it) }
                    assertTrue("Widget text must fit its height: ${text.text}", location[1] + text.height <= rootLocation[1] + root.height)
                }
            }
            capture("p3r-widget-${dimensions.first}x${dimensions.second}-${if (scale == 2f) "double-text" else "normal"}")
        }
    }
}
