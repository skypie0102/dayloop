package com.shadowmonarchbooks.dayloop.ui.skin

import android.content.ContentValues
import android.graphics.Rect
import android.os.Build
import android.os.Debug
import android.os.Handler
import android.os.HandlerThread
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.MediaStore
import android.view.FrameMetrics
import android.view.Window
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.shadowmonarchbooks.dayloop.MainActivity
import com.shadowmonarchbooks.dayloop.data.progress.ProfileEntity
import com.shadowmonarchbooks.dayloop.review.ReviewDependencies
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

/** Real Window frames and accessibility actions; no Compose test clock or performance dependency. */
class P3rFrameReviewTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val automation = instrumentation.uiAutomation
    private val dependencies by lazy {
        EntryPointAccessors.fromApplication(context.applicationContext, ReviewDependencies::class.java)
    }

    private fun shell(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(
        automation.executeShellCommand(command),
    ).use { it.readBytes().toString(Charsets.UTF_8).trim() }

    private fun text(value: CharSequence?): String = value?.toString()?.replace('\n', ' ')?.trim().orEmpty()

    private fun nodes(label: String): List<AccessibilityNodeInfo> = automation.rootInActiveWindow
        ?.findAccessibilityNodeInfosByText(label).orEmpty().filter {
            text(it.text).equals(label, true) || text(it.contentDescription).equals(label, true)
        }

    private fun clickable(label: String): AccessibilityNodeInfo? = nodes(label).firstNotNullOfOrNull { child ->
        var node: AccessibilityNodeInfo? = child
        while (node != null && !node.isClickable) node = node.parent
        node
    }

    private fun await(label: String, condition: () -> Boolean) {
        val end = SystemClock.uptimeMillis() + 15_000
        while (!condition() && SystemClock.uptimeMillis() < end) SystemClock.sleep(50)
        assertTrue(label, condition())
    }

    private fun click(label: String) {
        await("Accessible action: $label") { clickable(label) != null }
        assertTrue("Click $label", clickable(label)!!.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }

    private fun tab(label: String) {
        // Large labels can lie outside the accessibility tree until the row is panned.
        repeat(4) { attempt ->
            if (clickable(label) == null) {
                val metrics = context.resources.displayMetrics
                val active = listOf("Today", "Calendar", "Achievements", "Social Links", "Requests")
                    .mapNotNull(::clickable).firstOrNull { it.isSelected }
                val bounds = Rect().also { active?.getBoundsInScreen(it) }
                val y = if (bounds.isEmpty) (metrics.heightPixels * 0.89f).toInt() else bounds.centerY()
                val left = (metrics.widthPixels * 0.15f).toInt()
                val right = (metrics.widthPixels * 0.85f).toInt()
                val forward = attempt >= 2
                shell("input swipe ${if (forward) right else left} $y ${if (forward) left else right} $y 250")
                SystemClock.sleep(200)
            }
        }
        click(label)
        await("Selected tab: $label") { clickable(label)?.isSelected == true }
        // The 220 ms navigation transition finishes in real time, without synthetic clock ticks.
        SystemClock.sleep(350)
    }

    private fun swipeContent(forward: Boolean) {
        val metrics = context.resources.displayMetrics
        val x = metrics.widthPixels / 2
        val top = (metrics.heightPixels * 0.36f).toInt()
        val bottom = (metrics.heightPixels * 0.78f).toInt()
        shell("input swipe $x ${if (forward) bottom else top} $x ${if (forward) top else bottom} 250")
        SystemClock.sleep(300)
    }

    private fun pssKb(): Int = Debug.MemoryInfo().also(Debug::getMemoryInfo).totalPss

    private class Frames(private val refreshRate: Float) : Window.OnFrameMetricsAvailableListener {
        private val lock = Any()
        private val durations = mutableListOf<Long>()
        private var startedAt = Long.MAX_VALUE
        private var overDeadline = 0
        private var firstDraw = 0
        private var dropped = 0
        override fun onFrameMetricsAvailable(window: Window, frame: FrameMetrics, lost: Int) {
            // Copy primitive metrics inside the callback: the framework reuses its FrameMetrics object.
            val timestamp = frame.getMetric(FrameMetrics.INTENDED_VSYNC_TIMESTAMP)
            val total = frame.getMetric(FrameMetrics.TOTAL_DURATION)
            val initial = frame.getMetric(FrameMetrics.FIRST_DRAW_FRAME) == 1L
            val deadline = if (Build.VERSION.SDK_INT >= 31) frame.getMetric(FrameMetrics.DEADLINE) else 0
            synchronized(lock) {
                if (timestamp < startedAt || total < 0) return
                dropped += lost
                if (initial) { firstDraw++; return }
                durations += total
                if (total > (deadline.takeIf { it > 0 } ?: (1_000_000_000.0 / refreshRate).toLong())) overDeadline++
            }
        }
        fun start() = synchronized(lock) {
            durations.clear(); overDeadline = 0; firstDraw = 0; dropped = 0
            startedAt = System.nanoTime()
        }
        fun report(name: String, elapsed: Long, pss: Int): JSONObject = synchronized(lock) {
            val values = durations.sorted()
            fun percentile(fraction: Double): Any = if (values.isEmpty()) JSONObject.NULL else
                values[(ceil(values.size * fraction).toInt() - 1).coerceIn(values.indices)].toDouble() / 1_000_000.0
            JSONObject().put("scenario", name).put("frames", values.size)
                .put("p50Ms", percentile(0.50)).put("p95Ms", percentile(0.95))
                .put("overDeadline", overDeadline).put("firstDrawExcluded", firstDraw)
                .put("droppedReports", dropped).put("elapsedMs", elapsed).put("pssKb", pss)
                .put("overDeadlinePercent", if (values.isEmpty()) JSONObject.NULL else overDeadline * 100.0 / values.size)
        }
    }

    @Test @SdkSuppress(minSdkVersion = 31)
    fun recordSteadyStateFramesForTheAuthoredP3rScreens() {
        // The dedicated workflow runs this same test against baseline and current APKs on one AVD.
        assertTrue("Frame review requires the API 34 review device", Build.VERSION.SDK_INT >= 31)
        val arguments = InstrumentationRegistry.getArguments()
        val variant = arguments.getString("reviewVariant", "optimized")!!
        require(variant in setOf("baseline", "optimized"))
        val originalFont = shell("settings --user current get system font_scale").toFloatOrNull() ?: 1f
        val motionKeys = listOf("animator_duration_scale", "transition_animation_scale")
        val originalMotion = motionKeys.associateWith { shell("settings get global $it").toFloatOrNull() ?: 1f }
        val reports = JSONArray()
        var recordedRefreshRate = 0f
        try {
            motionKeys.forEach { shell("settings put global $it 1") }
            for (scale in listOf(1f, 2f)) {
                shell("settings --user current put system font_scale $scale")
                val store = dependencies.store()
                val profileId = runBlocking {
                    val ready = store.state.first { it.selectionReady && it.packs.any { pack -> pack.slug == "p3r" } }
                    val pack = ready.packs.single { it.slug == "p3r" }
                    val id = dependencies.db().profileDao().insert(ProfileEntity(
                        packId = "p3r", name = "Frame review $variant $scale", routeId = pack.routes.first().id,
                        clockDate = "2009-06-13", contentVersion = pack.pack.contentVersion,
                        createdAt = System.currentTimeMillis(),
                    ))
                    dependencies.repo().selectProfile("p3r", id)
                    dependencies.repo().selectPack("p3r")
                    dependencies.repo().activeProfileId("p3r").first { it == id }
                    store.state.first { it.selectedSlug == "p3r" }
                    id
                }
                val activity = ActivityScenario.launch(MainActivity::class.java)
                val thread = HandlerThread("P3rFrameReview").apply { start() }
                val handler = Handler(thread.looper)
                var collector: Frames? = null
                try {
                    await("P3R Today ready") { clickable("End day") != null }
                    activity.onActivity {
                        assertEquals(scale, it.resources.configuration.fontScale, 0f)
                        recordedRefreshRate = it.window.decorView.display.refreshRate
                    }
                    // Warm fonts, art and all five surfaces before collecting steady-state data.
                    listOf("Calendar", "Achievements", "Social Links", "Requests", "Today").forEach(::tab)
                    val frames = Frames(recordedRefreshRate)
                    collector = frames
                    activity.onActivity { it.window.addOnFrameMetricsAvailableListener(frames, handler) }
                    fun drain() {
                        val latch = CountDownLatch(1)
                        handler.post { latch.countDown() }
                        assertTrue("Frame collector drained", latch.await(5, TimeUnit.SECONDS))
                    }
                    fun measure(name: String, actions: () -> Unit) {
                        drain(); frames.start()
                        val start = SystemClock.uptimeMillis()
                        actions()
                        SystemClock.sleep(300); drain()
                        val report = frames.report(name, SystemClock.uptimeMillis() - start, pssKb())
                        if (!name.startsWith("idle")) assertTrue("Active scenario emits Window frames", report.getInt("frames") > 0)
                        reports.put(report)
                    }
                    measure(if (scale == 1f) "tabs-normal" else "tabs-double-text") {
                        repeat(3) { listOf("Calendar", "Achievements", "Social Links", "Requests", "Today").forEach(::tab) }
                    }
                    if (scale == 1f) {
                        tab("Requests")
                        measure("requests-scroll") { repeat(4) { swipeContent(true) }; repeat(2) { swipeContent(false) } }
                        tab("Today")
                        measure("tartarus-scroll") { repeat(4) { swipeContent(true) }; repeat(2) { swipeContent(false) } }
                        measure("day-complete") {
                            click("End day")
                            await("Dated result plane") { nodes("Tap to continue").isNotEmpty() }
                            SystemClock.sleep(400)
                            click("Tap to continue")
                            await("Exactly one next day") {
                                runBlocking { dependencies.db().profileDao().byId(profileId)?.clockDate == "2009-06-14" }
                            }
                        }
                        SystemClock.sleep(500)
                        measure("idle-today") { SystemClock.sleep(2_000) }
                    }
                    assertTrue("Navigation and scrolling do not mark tasks",
                        runBlocking { dependencies.repo().marksFor(profileId).first().isEmpty() })
                } finally {
                    collector?.let { listener -> activity.onActivity { it.window.removeOnFrameMetricsAvailableListener(listener) } }
                    activity.close(); thread.quitSafely(); thread.join(2_000)
                }
            }
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val output = JSONObject().put("variant", variant).put("source", arguments.getString("reviewSource"))
                .put("version", packageInfo.versionName).put("sdk", Build.VERSION.SDK_INT)
                .put("model", Build.MODEL).put("refreshRateHz", recordedRefreshRate)
                .put("motionScale", 1).put("coldStartupMeasured", false).put("debuggable", true)
                .put("conditions", "Warmed debug Window-frame review; renderer/device conditions are recorded by the invoking workflow")
                .put("scenarios", reports)
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$variant.json")
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/dayloop-performance")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = requireNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
            requireNotNull(resolver.openOutputStream(uri)).bufferedWriter().use { it.write(output.toString(2)) }
            values.clear(); values.put(MediaStore.MediaColumns.IS_PENDING, 0); resolver.update(uri, values, null, null)
        } finally {
            shell("settings --user current put system font_scale $originalFont")
            originalMotion.forEach { (key, value) -> shell("settings put global $key $value") }
        }
    }
}
