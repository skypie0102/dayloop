package com.shadowmonarchbooks.dayloop.widget

import com.shadowmonarchbooks.dayloop.data.PackStore
import com.shadowmonarchbooks.dayloop.data.formatDate
import com.shadowmonarchbooks.dayloop.data.nextDeadline
import com.shadowmonarchbooks.dayloop.data.progress.ProgressRepository
import com.shadowmonarchbooks.dayloop.pack.schema.Routes
import com.shadowmonarchbooks.dayloop.progress.StepMark
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/** Everything the home-screen widget renders (docs/PLAN.md §6.3). */
data class WidgetSnapshot(
    val packSlug: String? = null,
    val date: String? = null,
    val packTitle: String? = null,
    val profileName: String? = null,
    val routeLabel: String? = null,
    val dateLabel: String? = null,
    val doneCount: Int = 0,
    val totalCount: Int = 0,
    val deadlineLabel: String? = null,
    /** Days until the next deadline; null when nothing is upcoming. */
    val deadlineDays: Long? = null,
    /**
     * Glance-safe skin snapshot resolved entirely from generic pack theme
     * tokens (ROADMAP-v3 Phase 17b). Theme-less packs keep the legacy widget.
     */
    val skin: WidgetSkinSnapshot = WidgetSkinSnapshot(),
) {
    val isEmpty: Boolean get() = packTitle == null
}

/**
 * Computes the widget snapshot from the same sources the app renders
 * (bundled packs + persisted progress), so the widget stays honest even
 * after the process dies — provideGlance re-reads everything.
 */
@Singleton
class WidgetSnapshotter @Inject constructor(
    private val store: PackStore,
    private val repo: ProgressRepository,
) {
    suspend fun snapshot(): WidgetSnapshot {
        val packs = store.state.first { it.selectionReady }
        val slug = packs.selectedSlug ?: return WidgetSnapshot()
        val pack = packs.packs.firstOrNull { it.slug == slug } ?: return WidgetSnapshot()
        val profiles = repo.profilesFor(slug).first()
        val activeId = repo.activeProfileId(slug).first()
        val profile = profiles.firstOrNull { it.id == activeId } ?: profiles.firstOrNull()
        val routeId = profile?.routeId?.takeIf { it.isNotBlank() } ?: Routes.defaultId(pack.pack)
        val date = profile?.clockDate ?: pack.sortedDates(routeId).firstOrNull()
        val rows = profile?.let { repo.marksFor(it.id).first() }.orEmpty()
        val day = date?.let { pack.day(routeId, it) }
        val total = day?.steps?.size ?: 0
        var done = 0
        for (row in rows) {
            if (row.date == date && row.mark == StepMark.DONE.name && row.stepIndex in 0 until total) done++
        }
        val upcoming = date?.let { nextDeadline(pack.deadlines, it, pack.calendar) }
        return WidgetSnapshot(
            packSlug = slug,
            date = date,
            packTitle = pack.pack.title,
            profileName = profile?.name,
            routeLabel = if (pack.routes.size > 1) pack.routeLabel(routeId) else null,
            dateLabel = date?.let { formatDate(it, pack.calendar) },
            doneCount = done,
            totalCount = total,
            deadlineLabel = upcoming?.first?.label,
            deadlineDays = upcoming?.second,
            skin = widgetSkinSnapshot(pack.pack.theme),
        )
    }
}

/** Bridge for Glance's [provideGlance], which runs outside the injection graph. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun snapshotter(): WidgetSnapshotter
}
