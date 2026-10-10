package com.shadowmonarchbooks.dayloop.ui.skin

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/** One shared observer per theme, rather than an observer per task or button. */
internal val LocalAnimationsDisabled = staticCompositionLocalOf<Boolean?> { null }

internal fun animationsDisabled(animatorScale: Float, transitionScale: Float): Boolean =
    animatorScale <= 0f || transitionScale <= 0f

@Composable
internal fun rememberSystemAnimationsDisabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    fun readSetting() = animationsDisabled(
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f),
        Settings.Global.getFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f),
    )
    var disabled by remember(resolver) { mutableStateOf(readSetting()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { disabled = readSetting() }
        }
        listOf(Settings.Global.ANIMATOR_DURATION_SCALE, Settings.Global.TRANSITION_ANIMATION_SCALE).forEach {
            resolver.registerContentObserver(Settings.Global.getUriFor(it), false, observer)
        }
        disabled = readSetting()
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return disabled
}

/** Responds to system changes without restarting the screen. */
@Composable
fun rememberAnimationsDisabled(): Boolean =
    LocalAnimationsDisabled.current ?: rememberSystemAnimationsDisabled()
