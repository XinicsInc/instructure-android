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

package com.instructure.teacher.presenters

import com.android.ex.chips.RecipientEntry
import com.instructure.canvasapi2.StatusCallback
import com.instructure.canvasapi2.managers.InboxManager
import com.instructure.canvasapi2.managers.CourseManager
import com.instructure.canvasapi2.managers.GroupManager
import com.instructure.canvasapi2.models.*
import com.instructure.canvasapi2.utils.ApiType
import com.instructure.canvasapi2.utils.LinkHeaders
import com.instructure.canvasapi2.utils.mapToRemoteFile
import com.instructure.canvasapi2.utils.weave.inParallel
import com.instructure.canvasapi2.utils.weave.weave
import com.instructure.teacher.viewinterface.AddMessageView
import instructure.androidblueprint.FragmentPresenter
import kotlinx.coroutines.experimental.Job
import retrofit2.Call
import retrofit2.Response
import java.util.*

class AddMessagePresenter(val conversation: Conversation?, private val mParticipants: ArrayList<BasicUser>?, private val mMessages: ArrayList<Message>?, val isReply: Boolean) : FragmentPresenter<AddMessageView>() {

    private val mAttachments = ArrayList<RemoteFile>()
    private var mCourse: Course? = null

    private var mAPICalls: Job? = null


    override fun loadData(forceNetwork: Boolean) {}

    override fun refresh(forceNetwork: Boolean) {}

    fun getAllCoursesAndGroups(forceNetwork: Boolean) {

        mAPICalls = weave {
            viewCallback?.onRefreshStarted()
            try {
                var courses: ArrayList<Course>? = null
                var groups: ArrayList<Group>? = null
                inParallel {

                    // Get Courses
                    await<List<Course>>({ CourseManager.getAllFavoriteCourses(forceNetwork, it) }) {
                        courses = it as ArrayList<Course>
                    }


                    // Get graded submission count
                    await<List<Group>>({ GroupManager.getFavoriteGroups(it, forceNetwork) }) {
                        groups = it as ArrayList<Group>
                    }

                }
                viewCallback?.addCoursesAndGroups(courses, groups)
            } catch (ignore: Throwable) {
            }
        }
    }

    private val mCreateConversationCallback = object : StatusCallback<List<Conversation>>() {
        override fun onResponse(response: Response<List<Conversation>>, linkHeaders: LinkHeaders, type: ApiType) {
            if (viewCallback != null) {
                viewCallback?.messageSuccess()
            }
        }

        override fun onFail(call: Call<List<Conversation>>?, error: Throwable, response: Response<*>?) {
            if (viewCallback != null) {
                viewCallback?.messageFailure()
            }
        }
    }

    fun sendNewMessage(selectedRecipients: List<RecipientEntry>, message: String, subject: String, contextId: String, isBulk: Boolean) {

        val attachmentIDs = LongArray(mAttachments.size)
        for (i in attachmentIDs.indices) {
            attachmentIDs[i] = mAttachments[i].id
        }
        // Assemble list of recipient IDs
        val recipientIds = ArrayList<String>(selectedRecipients.size)
        for (entry in selectedRecipients) {
            recipientIds.add(entry.destination)
        }

        InboxManager.createConversation(recipientIds, message, subject, contextId, attachmentIDs, isBulk, mCreateConversationCallback)
    }

    fun sendMessage(selectedRecipients: List<RecipientEntry>, message: String) {

        if (mAddConversationCallback.isCallInProgress) return

        // Assemble list of recipient IDs
        val recipientIds = ArrayList<String>()
        for (entry in selectedRecipients) {
            recipientIds.add(entry.destination)
        }

        // Assemble list of attachment IDs
        val attachmentIDs = LongArray(mAttachments.size)
        for (i in attachmentIDs.indices) {
            attachmentIDs[i] = mAttachments[i].id
        }

        // Assemble list of Message IDs
        val messageIds = LongArray(mMessages?.size ?: 0)
        for (i in messageIds.indices) {
            messageIds[i] = mMessages?.get(i)?.id ?: 0
        }

        // Send message
        InboxManager.addMessage(conversation?.id ?: 0, message, recipientIds, messageIds, attachmentIDs, mAddConversationCallback)
    }

    private val mAddConversationCallback = object : StatusCallback<Conversation>() {
        override fun onResponse(response: Response<Conversation>, linkHeaders: LinkHeaders, type: ApiType) {
            if (viewCallback != null) {
                viewCallback!!.messageSuccess()
            }
        }

        override fun onFail(call: Call<Conversation>?, error: Throwable, response: Response<*>?) {
            if (viewCallback != null) {
                viewCallback!!.messageFailure()
            }
        }
    }

    val attachments: List<RemoteFile>
        get() = mAttachments

    fun addRemoteFileAttachments(attachments: List<RemoteFile>) {
        mAttachments.addAll(attachments)
        if (viewCallback != null) {
            viewCallback!!.refreshAttachments()
        }
    }

    fun addAttachments(attachments: List<Attachment>) {
        attachments.forEach { mAttachments.add(it.mapToRemoteFile()) }
        if (viewCallback != null) {
            viewCallback!!.refreshAttachments()
        }
    }

    fun removeAttachment(attachment: RemoteFile) {
        mAttachments.remove(attachment)
    }

    val course: Course?
        get() {
            if (mCourse == null) {
                var courseId: Long = 0

                if (conversation?.contextCode == null) {
                    mCourse = Course()
                    return mCourse
                }
                try {
                    courseId = java.lang.Long.parseLong(conversation.contextCode.replace("course_", ""))
                } catch (ignore: NumberFormatException) {
                }

                mCourse = Course()
                mCourse?.id = courseId
                mCourse?.name = conversation.contextName
            }
            return mCourse
        }

    fun getParticipantById(recipientId: Long): BasicUser? {
        for (participant in mParticipants ?: ArrayList<BasicUser>()) {
            if (participant.id == recipientId) {
                return participant
            }
        }
        return null
    }
}
