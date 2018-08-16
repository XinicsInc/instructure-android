/*
 * Copyright (C) 2018 - present  Instructure, Inc.
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
 */
package com.instructure.candroid.holders

import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.text.format.DateFormat
import android.view.Gravity
import android.view.View
import android.widget.TextView
import com.instructure.candroid.R
import com.instructure.candroid.interfaces.MessageAdapterCallback
import com.instructure.candroid.view.ViewUtils
import com.instructure.canvasapi2.models.BasicUser
import com.instructure.canvasapi2.models.Conversation
import com.instructure.canvasapi2.models.Message
import com.instructure.canvasapi2.utils.APIHelper
import com.instructure.pandautils.utils.*
import kotlinx.android.synthetic.main.viewholder_message.view.*
import java.text.SimpleDateFormat
import java.util.*

class InboxMessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bind(
        message: Message,
        conversation: Conversation,
        author: BasicUser?,
        position: Int,
        callback: MessageAdapterCallback
    ) = with(itemView) {

        // Set author info
        if (author != null) {
            authorName.text = getAuthorTitle(author.id, conversation)
            ProfileUtils.loadAvatarForUser(authorAvatar, author)
            authorAvatar.setupAvatarA11y(author.name)
            authorAvatar.onClick { callback.onAvatarClicked(author) }
        } else {
            authorName.text = ""
            authorAvatar.clearAvatarA11y()
            authorAvatar.setImageDrawable(null)
            authorAvatar.setOnClickListener(null)
        }

        // Set attachments
        if (message.attachments == null || message.attachments.isEmpty()) {
            attachmentContainer.visibility = View.GONE
        } else {
            attachmentContainer.visibility = View.VISIBLE
            attachmentContainer.setAttachments(message.attachments) { action, attachment ->
                callback.onAttachmentClicked(action, attachment)
            }
        }

        // Set body
        messageBody.setText(message.body, TextView.BufferType.SPANNABLE)
        ViewUtils.linkifyTextView(messageBody)

        // Set message date/time
        val messageDate = APIHelper.stringToDate(message.createdAt)
        dateTime.text = dateFormat.format(messageDate)

        // Set up message options
        messageOptions.onClick { v ->
            // Set up popup menu
            val actions = MessageAdapterCallback.MessageClickAction.values()
            val popup = PopupMenu(v.context, v, Gravity.START)
            val menu = popup.menu
            for (action in actions) {
                menu.add(0, action.ordinal, action.ordinal, action.labelResId)
            }

            // Add click listener
            popup.setOnMenuItemClickListener { item ->
                callback.onMessageAction(actions[item.itemId], message)
                true
            }

            // Show
            popup.show()
        }

        reply.setTextColor(ThemePrefs.buttonColor)
        reply.setVisible(position == 0)
        reply.onClick { callback.onMessageAction(MessageAdapterCallback.MessageClickAction.REPLY, message) }
    }

    private val dateFormat = SimpleDateFormat(
        DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMMdyyyyjmm"),
        Locale.getDefault()
    )

    private fun getAuthorTitle(myUserId: Long, conversation: Conversation): String {
        // We don't want to filter by the messages participating user ids because they don't always contain the correct information
        val users = conversation.participants

        // We want the author first
        users.find { it.id == myUserId }?.let {
            users.remove(it)
            users.add(0, it)
        }

        return when (users.size) {
            0 -> ""
            1, 2 -> users.joinToString { it.name }
            else -> "${users[0].name}, +${users.lastIndex}"
        }
    }

    companion object {
        fun holderResId(): Int = R.layout.viewholder_message
    }
}
