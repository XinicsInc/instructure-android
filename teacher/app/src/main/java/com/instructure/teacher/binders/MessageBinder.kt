/*
 * Copyright (C) 2017 - present  Instructure, Inc.
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

package com.instructure.teacher.binders

import android.content.Context
import android.support.v7.widget.PopupMenu
import android.text.format.DateFormat
import android.view.Gravity
import android.view.View
import android.widget.TextView
import com.instructure.canvasapi2.models.BasicUser
import com.instructure.canvasapi2.models.Conversation
import com.instructure.canvasapi2.models.Message
import com.instructure.canvasapi2.utils.APIHelper
import com.instructure.canvasapi2.utils.ContextKeeper
import com.instructure.pandautils.utils.ProfileUtils
import com.instructure.pandautils.utils.ThemePrefs
import com.instructure.pandautils.utils.setVisible
import com.instructure.pandautils.utils.setupAvatarA11y
import com.instructure.teacher.R
import com.instructure.teacher.holders.MessageHolder
import com.instructure.teacher.interfaces.MessageAdapterCallback
import com.instructure.teacher.utils.linkifyTextView
import java.text.SimpleDateFormat
import java.util.*


object MessageBinder : BaseBinder() {

    fun bind(context: Context, message: Message, conversation: Conversation, author: BasicUser?, holder: MessageHolder, position: Int, callback: MessageAdapterCallback) {

        // Set author info
        if (author != null) {
            holder.authorName.text = getAuthorTitle(context, author.id, conversation, message)
            ProfileUtils.loadAvatarForUser(holder.authorAvatar, author)
            holder.authorAvatar.setupAvatarA11y(author.name)
            holder.authorAvatar.setOnClickListener { callback.onAvatarClicked(author) }
        } else {
            holder.authorName.text = ""
            holder.authorAvatar.setImageDrawable(null)
            holder.authorAvatar.setOnClickListener(null)
        }

        // Set attachments
        if (message.attachments == null || message.attachments.isEmpty()) {
            holder.attachmentContainer.visibility = View.GONE
        } else {
            holder.attachmentContainer.visibility = View.VISIBLE
            holder.attachmentContainer.setAttachments(message.attachments) { action, attachment -> callback.onAttachmentClicked(action, attachment) }
        }

        // Set body
        holder.body.setText(message.body, TextView.BufferType.SPANNABLE)
        holder.body.linkifyTextView()
        // Set message date/time
        val messageDate = APIHelper.stringToDate(message.createdAt)
        holder.dateTime.text = dateFormat.format(messageDate)

        // Set up message options
        holder.messageOptions.setOnClickListener { v ->
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

        holder.reply.setTextColor(ThemePrefs.buttonColor)
        holder.reply.setVisible(position == 0)
        holder.reply.setOnClickListener { callback.onMessageAction(MessageAdapterCallback.MessageClickAction.REPLY, message) }
    }

    private var dateFormat = SimpleDateFormat(if (DateFormat.is24HourFormat(ContextKeeper.appContext)) "MMM d, yyyy, HH:mm" else "MMM d, yyyy, h:mm a",
                    Locale.getDefault())

    fun getAuthorTitle(context: Context, myUserId: Long, conversation: Conversation, message: Message): String {

        // we don't want to filter by the messages participating user ids because they don't always contain the correct information
        var users = conversation.participants

        val author = users.firstOrNull { it.id == myUserId }

        //we want the author first
        if(author != null) {
            users.remove(author)
            users.add(0, author)
        }

        if (users.isEmpty()) {
            return ""
        } else {
            return when (users.size) {
                in 0..2 -> users.joinToString { it.name }
                else -> context.getString(R.string.conversation_message_title, users.first().name, users.lastIndex.toString())
            }
        }
    }
}
