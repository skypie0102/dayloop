package com.shadowmonarchbooks.dayloop.pack

import com.shadowmonarchbooks.dayloop.pack.schema.EpisodesFile
import com.shadowmonarchbooks.dayloop.pack.schema.Pack
import com.shadowmonarchbooks.dayloop.pack.schema.Routes

internal fun validateEpisodes(pack: Pack, catalog: EpisodesFile?, walkthroughs: List<LoadedWalkthrough>): List<LintIssue> = buildList {
    fun issue(message: String) { add(LintIssue(LintIssue.Severity.ERROR, "episodes.json", message)) }
    val shipped = catalog?.chains?.isNotEmpty() == true
    if (pack.capabilities.episodes != shipped) issue("capabilities.episodes must match a nonempty episode catalog")
    if (catalog == null) return@buildList
    if (catalog.title.isBlank()) issue("episode catalog needs a title")
    val calendar = GameCalendar.of(pack.calendar)
    val routes = Routes.effective(pack).map { it.id }.toSet()
    val allSteps = catalog.chains.flatMap { it.steps }
    val stepsById = allSteps.associateBy { it.id }
    val chainIds = mutableSetOf<String>()
    val stepIds = mutableSetOf<String>()
    val eventIds = mutableSetOf<String>()
    val eventsById = catalog.events.associateBy { it.id }
    catalog.events.forEach { event ->
        if (event.id.isBlank() || !eventIds.add(event.id)) issue("duplicate or blank episode event '${event.id}'")
        val route = event.routeId ?: Routes.DEFAULT
        if (route !in routes) issue("episode event '${event.id}' references an unknown route")
        if (calendar != null && event.date !in calendar) issue("episode event '${event.id}' has an invalid date")
        val matches = walkthroughs.filter { it.routeId == route }.flatMap { it.file.days }
            .filter { it.date == event.date }.flatMap { it.steps }
            .filter { event.labelContains.isNotBlank() && it.label.contains(event.labelContains, ignoreCase = true) }
        if (matches.size != 1 || matches.singleOrNull()?.label != event.labelContains) {
            issue("episode event '${event.id}' must select exactly one complete task label")
        }
        if (allSteps.count { it.completionEvent == event.id } != 1) issue("episode event '${event.id}' must belong to exactly one step")
    }
    catalog.chains.forEach { chain ->
        if (!chain.id.startsWith("${pack.packId}.episode.") || !chainIds.add(chain.id)) issue("invalid or duplicate episode chain '${chain.id}'")
        if (chain.character.isBlank() || chain.steps.isEmpty()) issue("episode chain '${chain.id}' needs a character and steps")
        if (chain.reward?.isBlank() == true || chain.notes?.isBlank() == true) issue("episode chain '${chain.id}' has blank guidance")
        if (chain.steps.none { it.required }) issue("episode chain '${chain.id}' needs a required step")
        val chainStepIds = chain.steps.map { it.id }.toSet()
        val numbers = chain.steps.mapNotNull { it.number }
        if (numbers.any { it <= 0 } || numbers != numbers.distinct().sorted()) issue("episode numbers in '${chain.id}' must increase uniquely")
        chain.steps.forEach { step ->
            if (!step.id.startsWith("${chain.id}.") || !stepIds.add(step.id)) issue("invalid or duplicate episode step '${step.id}'")
            if (step.title.isBlank() || step.timeOfDay.isBlank() || step.location.isBlank() || step.solution.isBlank() || step.reward?.isBlank() == true) issue("episode step '${step.id}' needs readable guidance")
            if (step.number != null && (!step.required || chain.relatedStory)) issue("numbered episodes must be required members of an episode chain")
            val dates = listOf(step.availableFrom, step.availableUntil) + step.availableDates
            if (step.availableUntil < step.availableFrom || dates.any { calendar != null && it !in calendar } ||
                step.availableDates != step.availableDates.distinct().sorted() || step.availableDates.any { it !in step.availableFrom..step.availableUntil }) {
                issue("episode step '${step.id}' has an invalid availability window")
            }
            val event = eventsById[step.completionEvent]
            if (event == null || event.date !in step.availableFrom..step.availableUntil ||
                (step.availableDates.isNotEmpty() && event.date !in step.availableDates)) issue("episode step '${step.id}' must have an exact source event inside its window")
            if (step.prerequisiteSteps != step.prerequisiteSteps.distinct() || step.prerequisiteSteps.any { it == step.id || it !in chainStepIds }) issue("episode step '${step.id}' has an invalid prerequisite")
            step.prerequisiteSteps.forEach { id ->
                val prerequisite = stepsById[id]
                if (step.required && prerequisite?.required == false) issue("required episode '${step.id}' cannot depend on an optional branch")
                val before = prerequisite?.completionEvent?.let(eventsById::get)
                if (event != null && before != null && before.date > event.date) issue("episode prerequisite '$id' follows its dependent source event")
            }
        }
    }
    val visited = mutableSetOf<String>()
    val visiting = mutableSetOf<String>()
    fun visit(id: String) {
        if (id in visited) return
        if (!visiting.add(id)) { issue("cyclic episode prerequisites at '$id'"); return }
        stepsById[id]?.prerequisiteSteps?.filter { it in stepsById }?.forEach { visit(it) }
        visiting.remove(id)
        visited.add(id)
    }
    stepsById.keys.forEach { visit(it) }
}
