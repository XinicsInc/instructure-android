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

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v4.content.ContextCompat
import android.text.Html
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.instructure.candroid.R
import com.instructure.candroid.activity.NotificationWidgetRouter
import com.instructure.candroid.util.StringUtilities
import com.instructure.canvasapi2.managers.InboxManager
import com.instructure.canvasapi2.managers.CourseManager
import com.instructure.canvasapi2.managers.GroupManager
import com.instructure.canvasapi2.managers.StreamManager
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.StreamItem
import com.instructure.canvasapi2.utils.*
import com.instructure.pandautils.utils.ColorKeeper
import kotlinx.coroutines.experimental.Job
import java.io.Serializable
import java.util.*

class NotificationViewWidgetService : BaseRemoteViewsService(), Serializable {

    private var apiCallsJob: Job? = null

    override fun onGetViewFactory(intent: Intent): RemoteViewsService.RemoteViewsFactory {
        return NotificationsRowFactory(intent)
    }

    private inner class NotificationsRowFactory(private val intent: Intent) : CanvasWidgetRowFactory<StreamItem>() {

        override val layoutId: Int
            get() = getLayoutIdValue()

        override fun giveMeAppWidgetId(): Int {
            return BaseRemoteViewsService.getAppWidgetId(intent)
        }

        private fun getLayoutIdValue(): Int {
            if(intent.data != null) {
                val appWidgetId = Integer.valueOf(intent.data!!.schemeSpecificPart)
                if (BaseRemoteViewsService.shouldHideDetails(appWidgetId)) {
                    return R.layout.listview_widget_notifications_minimum_item_row
                }
            }
            return R.layout.listview_widget_notifications_item_row
        }

        override fun setViewData(streamItem: StreamItem, row: RemoteViews) {

            val appWidgetId = BaseRemoteViewsService.getAppWidgetId(intent)

            row.setViewVisibility(R.id.icon, View.VISIBLE)
            row.setImageViewResource(R.id.icon, getDrawableId(streamItem))

            row.setTextViewText(R.id.title, streamItem.getTitle(ContextKeeper.appContext))
            row.setTextColor(R.id.title, BaseRemoteViewsService.getWidgetTextColor(appWidgetId, applicationContext))

            if (streamItem.canvasContext != null && streamItem.canvasContext?.type != CanvasContext.Type.USER) {
                row.setInt(R.id.icon, "setColorFilter", ColorKeeper.getOrGenerateColor(streamItem.canvasContext))
            } else if (streamItem.type == StreamItem.Type.CONVERSATION) {
                val color = if(streamItem.canvasContext != null) ColorKeeper.getOrGenerateColor(streamItem.canvasContext)
                            else BaseRemoteViewsService.getWidgetTextColor(appWidgetId, ContextKeeper.appContext)
                row.setInt(R.id.icon, "setColorFilter", color)
            } else {
                val color = if(streamItem.canvasContext != null) ColorKeeper.getOrGenerateColor(streamItem.canvasContext)
                            else ContextCompat.getColor(applicationContext, R.color.canvasRed)
                row.setInt(R.id.icon, "setColorFilter", color)
            }

            if (!BaseRemoteViewsService.shouldHideDetails(appWidgetId)) {
                if (streamItem.getMessage(ContextKeeper.appContext) != null) {
                    row.setTextViewText(R.id.message, StringUtilities.simplifyHTML(Html.fromHtml(streamItem.getMessage(ContextKeeper.appContext))))
                } else {
                    row.setTextViewText(R.id.message, "")
                    row.setViewVisibility(R.id.message, View.GONE)
                }
            }

            var courseAndDate = ""
            if (streamItem.contextType == CanvasContext.Type.COURSE && streamItem.canvasContext != null) {
                courseAndDate = streamItem.canvasContext?.secondaryName + " "
            }
            courseAndDate += DateHelper.getDateTimeString(ContextKeeper.appContext, streamItem.updatedAtDate)
            row.setTextViewText(R.id.course_and_date, courseAndDate)

            row.setOnClickFillInIntent(R.id.widget_root, createIntent(streamItem))

        }

        override fun createIntent(streamItem: StreamItem): Intent {
            return NotificationWidgetRouter.createIntent(ContextKeeper.appContext, streamItem)
        }

        override fun clearViewData(row: RemoteViews) {
            row.setTextViewText(R.id.course_and_date, "")
            row.setTextViewText(R.id.message, "")
            row.setTextViewText(R.id.title, "")
            row.setViewVisibility(R.id.icon, View.GONE)
        }

        private fun getDrawableId(streamItem: StreamItem): Int {
            when (streamItem.type) {
                StreamItem.Type.DISCUSSION_TOPIC -> return R.drawable.vd_discussion
                StreamItem.Type.ANNOUNCEMENT -> return R.drawable.vd_announcement
                StreamItem.Type.SUBMISSION -> return R.drawable.vd_assignment
                StreamItem.Type.CONVERSATION -> return R.drawable.vd_inbox
                StreamItem.Type.MESSAGE ->
                    //a message could be related to an assignment, check the category
                    return if (streamItem.contextType == CanvasContext.Type.COURSE) {
                        R.drawable.vd_assignment
                    } else if (streamItem.notificationCategory.toLowerCase().contains("assignment graded")) {
                        R.drawable.vd_grades
                    } else {
                        R.drawable.vd_user
                    }
                StreamItem.Type.CONFERENCE -> return R.drawable.vd_conferences
                StreamItem.Type.COLLABORATION -> return R.drawable.vd_collaborations
                else -> return R.drawable.vd_announcement
            }
        }

        override fun loadData() {
            if(NetworkUtils.isNetworkAvailable && ApiPrefs.user != null) {
                try {
                    val courses = CourseManager.getCoursesSynchronous(true)
                            .filter { it.isFavorite && !it.isAccessRestrictedByDate && !it.isInvited() }
                    val groups = GroupManager.getFavoriteGroupsSynchronous(false)
                    val userStream = StreamManager.getUserStreamSynchronous(25, false).toMutableList()

                    Collections.sort(userStream)
                    Collections.reverse(userStream)

                    val courseMap = CourseManager.createCourseMap(courses)
                    val groupMap = GroupManager.createGroupMap(groups)

                    for (streamItem in userStream) {
                        streamItem.setCanvasContextFromMap(courseMap, groupMap)

                        // load conversations if needed
                        if (streamItem.type == StreamItem.Type.CONVERSATION) {
                            val conversation = InboxManager.getConversationSynchronous(streamItem.conversationId, true)
                            streamItem.setConversation(ContextKeeper.appContext, conversation, ApiPrefs.user!!.id, ContextKeeper.appContext.resources.getString(R.string.monologue))
                        }
                    }

                    setData(userStream)
                } catch (e: Throwable) {
                    Logger.e("Could not load " + this::class.java.simpleName + " widget. " + e.message)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        apiCallsJob?.cancel()
    }

    companion object {

        fun createIntent(context: Context, appWidgetId: Int): Intent {
            val intent = Intent(context, NotificationViewWidgetService::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            intent.data = Uri.fromParts("appWidgetId", appWidgetId.toString(), null)
            return intent
        }
    }
}
