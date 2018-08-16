/*
 * Copyright (C) 2017 - present Instructure, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.instructure.candroid.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.instructure.candroid.R
import com.instructure.candroid.activity.InterwebsToApplication

class GradesWidgetProvider : CanvasWidgetProvider() {

    override val analyticsName: String
        get() = SIMPLE_NAME

    public override val refreshIntentID: Int
        get() = GRADES_REFRESH_ID

    override fun getRefreshIntent(appWidgetManager: AppWidgetManager): Intent {
        return WidgetUpdater.getGradesWidgetUpdateIntent(appWidgetManager)
    }

    public override fun setWidgetDependentViews(context: Context, remoteViews: RemoteViews, appWidgetId: Int, appWidgetManager: AppWidgetManager, textColor: Int) {
        remoteViews.setRemoteAdapter(R.id.contentList, GradesViewWidgetService.createIntent(context, appWidgetId))
        remoteViews.setTextViewText(R.id.widget_title, context.getString(R.string.gradesWidgetTitle))

        remoteViews.setInt(R.id.widget_root, "setBackgroundResource", BaseRemoteViewsService.getWidgetBackgroundResourceId(appWidgetId))
        remoteViews.setTextColor(R.id.widget_title, textColor)

        val listViewItemIntent = Intent(context, InterwebsToApplication::class.java)
        remoteViews.setPendingIntentTemplate(R.id.contentList, PendingIntent.getActivity(context, CanvasWidgetProvider.cycleBit++, listViewItemIntent, PendingIntent.FLAG_UPDATE_CURRENT))

        val pendingRefreshIntent = PendingIntent.getBroadcast(context, refreshIntentID, getRefreshIntent(appWidgetManager), PendingIntent.FLAG_UPDATE_CURRENT)
        remoteViews.setOnClickPendingIntent(R.id.widget_refresh, pendingRefreshIntent)
    }

    companion object {
        private const val GRADES_REFRESH_ID = 3
        private const val SIMPLE_NAME = "Grades Widget"
    }
}

