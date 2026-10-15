package com.shadowmonarchbooks.dayloop.data

import com.shadowmonarchbooks.dayloop.pack.GameCalendar
import com.shadowmonarchbooks.dayloop.pack.schema.Deadline
import java.time.temporal.ChronoUnit

/** A date/window notice, independent of task completion or the profile clock. */
data class DeadlineNotice(
    val deadline: Deadline,
    val startsIn: Long,
    val endsIn: Long,
) {
    val isOpen: Boolean get() = startsIn <= 0
    val targetDate: String get() = requireNotNull(if (isOpen) deadlineEnd(deadline) else deadlineStart(deadline))
}

/** Keep an open window until its inclusive END, rather than dropping it after opening. */
fun deadlineNotices(deadlines: List<Deadline>, date: String, calendar: GameCalendar?): List<DeadlineNotice> {
    fun distance(target: String): Long? = if (calendar != null) calendar.diffDays(date, target)?.toLong() else {
        val from = parseDateOrNull(date)
        val to = parseDateOrNull(target)
        if (from == null || to == null) null else ChronoUnit.DAYS.between(from, to)
    }
    return deadlines.mapNotNull { deadline ->
        val start = deadlineStart(deadline)?.let(::distance) ?: return@mapNotNull null
        val end = deadlineEnd(deadline)?.let(::distance) ?: return@mapNotNull null
        if (end < 0) null else DeadlineNotice(deadline, start, end)
    }.sortedWith(compareBy<DeadlineNotice> { if (it.isOpen) it.endsIn else it.startsIn }.thenBy { it.deadline.id })
}
