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
package com.instructure.teacher.presenters

import com.instructure.canvasapi2.managers.DiscussionManager
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.DiscussionEntry
import com.instructure.canvasapi2.utils.weave.awaitApiResponse
import com.instructure.canvasapi2.utils.weave.catch
import com.instructure.canvasapi2.utils.weave.tryWeave
import com.instructure.pandautils.models.FileSubmitObject
import com.instructure.teacher.viewinterface.DiscussionsReplyView
import instructure.androidblueprint.FragmentPresenter
import kotlinx.coroutines.experimental.Job
import retrofit2.Response
import java.io.File

class DiscussionsReplyPresenter(
        val canvasContext: CanvasContext,
        val discussionTopicHeaderId: Long,
        val discussionEntryId: Long) : FragmentPresenter<DiscussionsReplyView>() {

    private var postDiscussionJob: Job? = null

    override fun loadData(forceNetwork: Boolean) {}
    override fun refresh(forceNetwork: Boolean) {}

    fun sendMessage(message: String?) {
        if(postDiscussionJob?.isActive == true) {
            viewCallback?.messageFailure(REASON_MESSAGE_IN_PROGRESS)
            return
        }

        if(message == null) {
            viewCallback?.messageFailure(REASON_MESSAGE_EMPTY)
        } else {
            postDiscussionJob = tryWeave {
                if (attachment == null) {
                    if (discussionEntryId == discussionTopicHeaderId) {
                        messageSentResponse(awaitApiResponse { DiscussionManager.postToDiscussionTopic(canvasContext, discussionTopicHeaderId, message, it) })
                    } else {
                        messageSentResponse(awaitApiResponse { DiscussionManager.replyToDiscussionEntry(canvasContext, discussionTopicHeaderId, discussionEntryId, message, it) })
                    }
                } else {
                    if (discussionEntryId == discussionTopicHeaderId) {
                        messageSentResponse(awaitApiResponse { DiscussionManager.postToDiscussionTopic(canvasContext, discussionTopicHeaderId, message, File(attachment!!.fullPath), attachment?.contentType ?: "multipart/form-data", it) })
                    } else {
                        messageSentResponse(awaitApiResponse { DiscussionManager.replyToDiscussionEntry(canvasContext, discussionTopicHeaderId, discussionEntryId, message, File(attachment!!.fullPath), attachment?.contentType  ?: "multipart/form-data", it) })
                    }
                }
            } catch { }
        }
    }

    private fun messageSentResponse(response: Response<DiscussionEntry>) {
        if (response.code() in 200..299) {
            response.body()?.let { viewCallback?.messageSuccess(it) }
        } else {
            viewCallback?.messageFailure(REASON_MESSAGE_FAILED_TO_SEND)
        }
    }

    fun setAttachment(fileSubmitObject: FileSubmitObject?) {
        attachment = fileSubmitObject
    }

    fun getAttachment(): FileSubmitObject? {
        return attachment
    }

    fun hasAttachment(): Boolean {
        return attachment != null
    }

    companion object {
        val REASON_MESSAGE_IN_PROGRESS = 1
        val REASON_MESSAGE_EMPTY = 2
        val REASON_MESSAGE_FAILED_TO_SEND = 3
        var attachment: FileSubmitObject? = null
    }

    override fun onDestroyed() {
        super.onDestroyed()
        postDiscussionJob?.cancel()
    }
}
