package com.shadowmonarchbooks.dayloop.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.shadowmonarchbooks.dayloop.MainActivity

internal const val OpenWidgetAction = "com.shadowmonarchbooks.dayloop.OPEN_WIDGET"
internal const val WidgetPackExtra = "widget_pack"

/** The URI also distinguishes PendingIntents: extras alone do not distinguish game targets. */
internal fun widgetLaunchIntent(context: Context, packSlug: String?): Intent =
    Intent(context, MainActivity::class.java).apply {
        action = OpenWidgetAction
        data = Uri.Builder().scheme("dayloop").authority("widget").appendPath(packSlug ?: "current").build()
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra(WidgetPackExtra, packSlug)
    }

internal data class WidgetOpenRequest(val packSlug: String?, val token: Long = System.nanoTime())
