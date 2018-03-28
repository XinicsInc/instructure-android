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

package com.instructure.candroid.holders

import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import com.instructure.candroid.R
import com.instructure.candroid.binders.BaseBinder
import com.instructure.candroid.interfaces.CourseAdapterToFragmentCallback
import com.instructure.canvasapi2.StatusCallback
import com.instructure.canvasapi2.managers.AccountNotificationManager
import com.instructure.canvasapi2.models.AccountNotification
import com.instructure.pandautils.utils.ThemePrefs
import com.instructure.pandautils.utils.onClick
import com.instructure.pandautils.utils.onClickWithRequireNetwork
import com.instructure.pandautils.utils.setVisible
import kotlinx.android.synthetic.main.viewholder_announcement_card.view.*

class AnnouncementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    companion object {
        fun holderResId(): Int = R.layout.viewholder_announcement_card
        private val expandedIds = hashMapOf<Long, Boolean>()
    }

    fun bind(
            announcement: AccountNotification,
            callback: CourseAdapterToFragmentCallback
    ) = with(itemView) {
        val color = when (announcement.icon) {
            AccountNotification.ACCOUNT_NOTIFICATION_ERROR -> ContextCompat.getColor(context, R.color.notificationTintError)
            AccountNotification.ACCOUNT_NOTIFICATION_WARNING -> ContextCompat.getColor(context, R.color.notificationTintWarning)
            else -> ThemePrefs.brandColor
        }

        val icon = when (announcement.icon) {
            AccountNotification.ACCOUNT_NOTIFICATION_ERROR,
            AccountNotification.ACCOUNT_NOTIFICATION_WARNING -> R.drawable.vd_warning
            AccountNotification.ACCOUNT_NOTIFICATION_CALENDAR -> R.drawable.vd_calendar_announcement
            AccountNotification.ACCOUNT_NOTIFICATION_QUESTION -> R.drawable.vd_question_mark
            else -> R.drawable.vd_info
        }

        announcementIcon.setImageResource(icon)
        DrawableCompat.setTint(DrawableCompat.wrap(background), color)
        DrawableCompat.setTint(DrawableCompat.wrap(announcementIconView.background), color)
        dismissButton.setTextColor(ThemePrefs.buttonColor)

        announcementTitle.text = announcement.subject
        announcementDetails.text = BaseBinder.getHtmlAsText(announcement.message)

        fun refresh() {
            val isExpanded = expandedIds[announcement.id] ?: false
            announcementTitle.setSingleLine(!isExpanded)
            announcementDetails.setVisible(isExpanded)
            dismissButton.setVisible(isExpanded)
            collapseButton.setVisible(isExpanded)
            tapToView.setVisible(!isExpanded)
            dismissImageButton.setVisible(!isExpanded)
        }

        fun dismiss() {
            // Fire and forget
            AccountNotificationManager.deleteAccountNotification(announcement.id, object : StatusCallback<AccountNotification>(){})
            expandedIds.remove(announcement.id)
            callback.onRemoveAnnouncement(announcement, adapterPosition)
        }

        onClick {
            val expanded = expandedIds[announcement.id] ?: false
            expandedIds[announcement.id] = !expanded
            refresh()
        }

        dismissButton.onClickWithRequireNetwork { dismiss() }
        dismissImageButton.onClickWithRequireNetwork { dismiss() }
        refresh()
    }

}
