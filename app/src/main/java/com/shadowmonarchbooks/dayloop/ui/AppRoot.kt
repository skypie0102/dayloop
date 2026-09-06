package com.shadowmonarchbooks.dayloop.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.shadowmonarchbooks.dayloop.data.LoadedPack
import com.shadowmonarchbooks.dayloop.data.formatDate
import com.shadowmonarchbooks.dayloop.ui.achievements.AchievementsScreen
import com.shadowmonarchbooks.dayloop.ui.activities.ActivitiesScreen
import com.shadowmonarchbooks.dayloop.ui.activities.ActivityDetailScreen
import com.shadowmonarchbooks.dayloop.ui.answers.AnswersScreen
import com.shadowmonarchbooks.dayloop.ui.bonds.BondDetailScreen
import com.shadowmonarchbooks.dayloop.ui.bonds.BondsScreen
import com.shadowmonarchbooks.dayloop.ui.day.DayScreen
import com.shadowmonarchbooks.dayloop.ui.deadlines.DeadlinesScreen
import com.shadowmonarchbooks.dayloop.ui.media.MediaScreen
import com.shadowmonarchbooks.dayloop.ui.mementos.MementosRequestsScreen
import com.shadowmonarchbooks.dayloop.ui.month.MonthScreen
import com.shadowmonarchbooks.dayloop.ui.onboarding.OnboardingScreen
import com.shadowmonarchbooks.dayloop.ui.search.SearchScreen
import com.shadowmonarchbooks.dayloop.ui.settings.SettingsScreen
import com.shadowmonarchbooks.dayloop.ui.skin.LocalSkin
import com.shadowmonarchbooks.dayloop.ui.skin.LocalSkinFx
import com.shadowmonarchbooks.dayloop.ui.skin.SkinBottomBar
import com.shadowmonarchbooks.dayloop.ui.skin.SkinNavItem
import com.shadowmonarchbooks.dayloop.ui.skin.SkinTopBar
import com.shadowmonarchbooks.dayloop.ui.skin.navMotion
import com.shadowmonarchbooks.dayloop.ui.skin.rememberAnimationsDisabled
import com.shadowmonarchbooks.dayloop.ui.skin.skinBackdrop
import com.shadowmonarchbooks.dayloop.ui.today.TodayScreen

/** Every top-level destination stays registered, whatever the active pack ships. */
internal val TopLevelRoutes = setOf("today", "calendar", "achievements", "bonds", "answers", "mementos", "requests")

/** Prevent a top-level control from reloading the destination already on screen. */
internal fun shouldNavigate(currentRoute: String?, destination: String): Boolean =
    currentRoute != destination

/**
 * Tabs the active pack earns. Achievements is a first-class tracker for every
 * pack; packs without achievement data show an explicit empty state rather
 * than falling back to the old Activities browser. Activities stay reachable
 * through authored step references and Search, where they have context.
 */
internal fun topLevelTabs(pack: LoadedPack?): List<SkinNavItem> = buildList {
    add(SkinNavItem("today", "Today", Icons.Filled.Home))
    add(SkinNavItem("calendar", "Calendar", Icons.Filled.DateRange))
    add(SkinNavItem("achievements", "Achievements", Icons.Filled.Star))
    if (pack == null || pack.hasBondsFile) {
        add(SkinNavItem("bonds", pack?.pack?.labels?.bond?.let { it + "s" } ?: "Bonds", Icons.Filled.Person))
    }
    if (pack?.pack?.capabilities?.mementosRequests == true) {
        add(SkinNavItem("mementos", "Mementos Requests", Icons.Filled.List))
    } else if (pack?.pack?.capabilities?.requests == true) {
        add(SkinNavItem("requests", "Requests", Icons.Filled.List))
    } else if (pack == null || pack.pack.capabilities.answers) {
        add(SkinNavItem("answers", "Answers", Icons.Filled.Info))
    }
}

/** The banner carries the active destination name now that tab labels are icon-only. */
private fun bannerTitle(
    route: String?,
    tabs: List<SkinNavItem>,
    pack: LoadedPack?,
    pinnedTodayDate: String? = null,
): String {
    if (route == "today" && pinnedTodayDate != null) return "Today · $pinnedTodayDate"
    if (route == "requests") return pack?.requests?.title ?: "Requests"
    tabs.firstOrNull { it.route == route }?.let { return it.label }
    return when (route?.substringBefore('/')) {
        "day" -> "Day"
        "bond" -> pack?.pack?.labels?.bond ?: "Bond"
        "activity", "activities" -> "Activities"
        "search" -> "Search"
        "settings" -> "Settings"
        "media" -> "Media"
        else -> pack?.pack?.title ?: "dayloop"
    }
}

@Composable
fun AppRoot(vm: DayloopViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()

    // Cold start: wait for the persisted pack selection instead of flashing
    // the onboarding grid at returning users (docs/ROADMAP-v2.md Phase 7).
    if (!state.selectionReady) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val pack = state.selected
    val tabs = remember(pack) { topLevelTabs(pack) }
    var todayDatePinned by remember(pack?.slug) { mutableStateOf(false) }
    var calendarMonth by rememberSaveable(pack?.slug) { mutableStateOf<String?>(null) }
    LaunchedEffect(route) {
        if (route != "today") todayDatePinned = false
    }

    val startDestination = remember(state.selectionReady) {
        if (state.selectedSlug == null) "onboarding" else "today"
    }

    CompositionLocalProvider(LocalSkinFx provides vm.skinFx) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                if (route != "onboarding") {
                    SkinTopBar(
                        title = bannerTitle(
                            route = route,
                            tabs = tabs,
                            pack = pack,
                            pinnedTodayDate = state.currentDate
                                ?.takeIf { route == "today" && todayDatePinned }
                                ?.let { formatDate(it, pack?.calendar) },
                        ),
                        canGoBack = route != null && route !in TopLevelRoutes,
                        onBack = { nav.popBackStack() },
                        onOpenSearch = { nav.navigate("search") { launchSingleTop = true } },
                        settingsEnabled = shouldNavigate(route, "settings"),
                        onOpenSettings = {
                            if (shouldNavigate(route, "settings")) {
                                nav.navigate("settings") { launchSingleTop = true }
                            }
                        },
                    )
                }
            },
            bottomBar = {
                if (route in TopLevelRoutes) {
                    SkinBottomBar(
                        items = tabs,
                        selectedRoute = route,
                        onSelect = { destination ->
                            if (shouldNavigate(route, destination)) {
                                nav.navigate(destination) { launchSingleTop = true }
                            }
                        },
                    )
                }
            },
        ) { padding ->
            val skin = LocalSkin.current
            val animationsDisabled = rememberAnimationsDisabled()
            val motion = remember(skin, animationsDisabled) { skin.navMotion(animationsDisabled) }
            NavHost(
                navController = nav,
                startDestination = startDestination,
                modifier = Modifier
                    .padding(padding)
                    .skinBackdrop(skin, showHaze = route != "today"),
                enterTransition = {
                    if (
                        initialState.destination.route == "onboarding" &&
                        targetState.destination.route == "today"
                    ) {
                        fadeIn(tween(360)) + slideInVertically(
                            initialOffsetY = { it / 12 },
                            animationSpec = tween(360),
                        )
                    } else {
                        motion.enter
                    }
                },
                exitTransition = {
                    if (
                        initialState.destination.route == "onboarding" &&
                        targetState.destination.route == "today"
                    ) {
                        fadeOut(tween(220))
                    } else {
                        motion.exit
                    }
                },
                popEnterTransition = { motion.popEnter },
                popExitTransition = { motion.popExit },
            ) {
                composable("onboarding") {
                    OnboardingScreen(
                        vm = vm,
                        onStart = {
                            nav.navigate("today") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        },
                        onCancel = if (state.selectedSlug != null) {
                            { nav.popBackStack() }
                        } else {
                            null
                        },
                    )
                }
                composable("today") {
                    TodayScreen(
                        vm = vm,
                        onOpenSettings = { nav.navigate("settings") { launchSingleTop = true } },
                        onOpenActivity = { ref -> nav.navigate("activity/$ref") },
                        onOpenAnswers = if (pack?.pack?.capabilities?.mementosRequests == true) null else {
                            { nav.navigate("answers") { launchSingleTop = true } }
                        },
                        onDatePinnedChange = { todayDatePinned = it },
                    )
                }
                composable("day/{date}") { entry ->
                    DayScreen(
                        date = entry.arguments?.getString("date").orEmpty(),
                        vm = vm,
                        onOpenDay = { next ->
                            nav.navigate("day/$next") {
                                popUpTo("day/{date}") { inclusive = true }
                            }
                        },
                        onOpenAnswers = if (pack?.pack?.capabilities?.mementosRequests == true) null else {
                            { nav.navigate("answers") { launchSingleTop = true } }
                        },
                        onOpenActivity = { ref -> nav.navigate("activity/$ref") },
                    )
                }
                composable("calendar") {
                    MonthScreen(
                        vm = vm,
                        selectedMonth = calendarMonth,
                        onSelectedMonthChange = { calendarMonth = it },
                        onOpenDay = { date ->
                            calendarMonth = date.take(7)
                            nav.navigate("day/$date")
                        },
                    )
                }
                composable("achievements") {
                    AchievementsScreen(vm = vm)
                }
                composable("bonds") {
                    BondsScreen(
                        pack = pack,
                        days = state.days,
                        marks = state.marks,
                        onOpenBond = { id -> nav.navigate("bond/$id") },
                    )
                }
                composable("bond/{bondId}") { entry ->
                    BondDetailScreen(
                        bondId = entry.arguments?.getString("bondId").orEmpty(),
                        pack = pack,
                        days = state.days,
                        marks = state.marks,
                    )
                }
                composable("deadlines") {
                    DeadlinesScreen(vm = vm)
                }
                composable("answers") {
                    AnswersScreen(vm = vm, onOpenDay = { date -> nav.navigate("day/$date") })
                }
                composable("requests") {
                    com.shadowmonarchbooks.dayloop.ui.requests.RequestsScreen(vm = vm, onOpenDay = { date -> nav.navigate("day/$date") })
                }
                composable("mementos") {
                    MementosRequestsScreen(vm = vm, onOpenDay = { date -> nav.navigate("day/$date") })
                }
                composable("activities") {
                    ActivitiesScreen(
                        vm = vm,
                        onOpenActivity = { id -> nav.navigate("activity/$id") },
                    )
                }
                composable("activity/{activityId}") { entry ->
                    ActivityDetailScreen(
                        activityId = entry.arguments?.getString("activityId").orEmpty(),
                        vm = vm,
                    )
                }
                composable("search") {
                    SearchScreen(
                        vm = vm,
                        onOpenDay = { date -> nav.navigate("day/$date") },
                        onOpenBond = { id -> nav.navigate("bond/$id") },
                        onOpenActivity = { id -> nav.navigate("activity/$id") },
                        onOpenDeadlines = { nav.navigate("deadlines") { launchSingleTop = true } },
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        vm = vm,
                        onSwitchGame = { nav.navigate("onboarding") { launchSingleTop = true } },
                        onOpenMedia = { nav.navigate("media") { launchSingleTop = true } },
                    )
                }
                composable("media") {
                    MediaScreen(vm = vm)
                }
            }
        }
    }
}
