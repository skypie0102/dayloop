package com.shadowmonarchbooks.dayloop

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.shadowmonarchbooks.dayloop.data.PackStore
import com.shadowmonarchbooks.dayloop.ui.AppRoot
import com.shadowmonarchbooks.dayloop.ui.DayloopViewModel
import com.shadowmonarchbooks.dayloop.ui.skin.skinDecor
import com.shadowmonarchbooks.dayloop.ui.skin.LocalSkin
import com.shadowmonarchbooks.dayloop.ui.skin.hasSubmergedChrome
import com.shadowmonarchbooks.dayloop.ui.skin.SubmergedLoading
import com.shadowmonarchbooks.dayloop.ui.startupContentReady
import com.shadowmonarchbooks.dayloop.ui.theme.DayloopTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.shadowmonarchbooks.dayloop.widget.OpenWidgetAction
import com.shadowmonarchbooks.dayloop.widget.WidgetPackExtra
import com.shadowmonarchbooks.dayloop.widget.WidgetOpenRequest

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var packStore: PackStore

    /** Once the first matching UI projection renders, later pack switches use the normal in-app path. */
    private var startupCommitted = false
    private var widgetRequest by mutableStateOf<WidgetOpenRequest?>(null)

    private fun acceptWidgetIntent(intent: Intent?) {
        if (intent?.action == OpenWidgetAction) widgetRequest = WidgetOpenRequest(intent.getStringExtra(WidgetPackExtra))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        acceptWidgetIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Phase 17a: keep the platform-owned splash visible while DataStore
        // resolves the saved pack. Compose may initialize behind it, but no
        // app frame is exposed until the active skin is known.
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        acceptWidgetIntent(intent)
        splash.setKeepOnScreenCondition { !packStore.state.value.selectionReady }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
        )
        setContent {
            // Theme directly from PackStore during the initial handoff. This is
            // the earliest resolved source of truth for the selected pack and
            // prevents an engine-theme first frame. Once startup commits, the
            // normal ViewModel projection owns in-session pack changes again.
            val packsState by packStore.state.collectAsState()
            val vm: DayloopViewModel = hiltViewModel()
            val uiState by vm.state.collectAsState()
            val request = widgetRequest
            LaunchedEffect(request?.token, packsState.selectionReady) {
                if (request != null && packsState.selectionReady) {
                    val slug = request.packSlug
                    if (slug == null) Unit // Empty widget opens the current game or first-run picker.
                    else if (packsState.packs.any { it.slug == slug }) packStore.select(slug)
                    else { widgetRequest = null; intent.action = Intent.ACTION_MAIN }
                }
            }
            val widgetReady = request != null && packsState.selectionReady && uiState.selectionReady &&
                (request.packSlug == null || (packsState.selectedSlug == request.packSlug && uiState.selectedSlug == request.packSlug))
            val contentReady = startupContentReady(
                packSelectionReady = packsState.selectionReady,
                packSelectedSlug = packsState.selectedSlug,
                uiSelectionReady = uiState.selectionReady,
                uiSelectedSlug = uiState.selectedSlug,
            )
            val requestedPack = request?.packSlug?.let { slug -> packsState.packs.firstOrNull { it.slug == slug } }
            if (contentReady && (requestedPack == null || widgetReady)) startupCommitted = true
            // A valid widget target owns the loading shell until its own UI projection is ready.
            // This also prevents another game's saved selection from becoming the first exposed frame.
            val waitingForWidget = requestedPack != null && !widgetReady
            val themePack = if (waitingForWidget) requestedPack else if (startupCommitted) uiState.selected else packsState.selected

            DayloopTheme(pack = themePack) {
                if (startupCommitted && !waitingForWidget) {
                    AppRoot(vm = vm, openTodayRequest = request?.token?.takeIf { widgetReady },
                        onOpenTodayHandled = { widgetRequest = null; intent.action = Intent.ACTION_MAIN })
                } else {
                    StartupShell(themePack?.pack?.title)
                }
            }
        }
    }
}

/** Phase 17a shell exposed internally so Phase 17d/18 can render the exact startup surface. */
@Composable
internal fun StartupShell(packTitle: String? = null) {
    if (LocalSkin.current.hasSubmergedChrome()) {
        SubmergedLoading(packTitle)
        return
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .skinDecor("panel"),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
