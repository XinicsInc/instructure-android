/*
 * Copyright (C) 2018 - present Instructure, Inc.
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

import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import com.instructure.candroid.R
import com.instructure.candroid.interfaces.AdapterToFragmentCallback
import com.instructure.canvasapi2.models.Conversation
import com.instructure.canvasapi2.utils.DateHelper
import com.instructure.pandautils.utils.*
import kotlinx.android.synthetic.main.viewholder_inbox.view.*
import java.util.*

class InboxViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bind(conversation: Conversation, currentUserId: Long, callback: AdapterToFragmentCallback<Conversation>) = with(itemView){
        ProfileUtils.loadAvatarsForConversation(avatar, conversation) { _, _-> }

        userName.setVisible().text = getConversationTitle(context, currentUserId, conversation)

        message.text = conversation.lastMessagePreview
        message.setVisible(message.text.isNotBlank())

        if (conversation.hasAttachments() || conversation.hasMedia()) {
            attachment.setImageDrawable(ColorUtils.colorIt(ContextCompat.getColor(context, R.color.canvasTextMedium), attachment.drawable))
            attachment.setVisible()
        } else {
            attachment.setGone()
        }

        date.text = getParsedDate(context, conversation.lastAuthoredMessageSent ?: conversation.lastMessageSent)
        date.setVisible(date.text.isNotBlank())

        if (!conversation.subject.isNullOrBlank()) {
            subjectView.setVisible()
            subjectView.text = conversation.subject
            message.maxLines = 1
        } else {
            subjectView.setGone()
            message.maxLines = 2
        }

        if (conversation.workflowState == Conversation.WorkflowState.UNREAD) {
            unreadMark.setVisible()
            unreadMark.setImageDrawable(ColorUtils.colorIt(ThemePrefs.accentColor, unreadMark.drawable))
        } else {
            unreadMark.setGone()
        }

        if (conversation.isStarred) {
            star.setImageDrawable(ColorUtils.colorIt(ThemePrefs.brandColor, star.drawable))
            star.setVisible()
        } else {
            star.setGone()
        }

        onClick { callback.onRowClicked(conversation, adapterPosition, true) }
    }

    private fun getParsedDate(context: Context, date: Date?): String {
        return date?.let { DateHelper.dateToDayMonthYearString(context, it) } ?: ""
    }

    private fun getConversationTitle(context: Context, myUserId: Long, conversation: Conversation): String {
        if (conversation.isMonologue(myUserId)) return context.getString(R.string.monologue)

        val users = conversation.participants

        return when (users.size) {
            0 -> ""
            1 -> users[0].name
            2 -> users[0].name + ", " + users[1].name
            else -> "${users[0].name}, +${users.size - 1}"
        }
    }

    companion object {
        const val holderResId = R.layout.viewholder_inbox
    }

}
