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
import com.instructure.canvasapi2.models.DiscussionTopic
import com.instructure.canvasapi2.models.post_models.DiscussionEntryPostBody
import com.instructure.canvasapi2.utils.weave.awaitApiResponse
import com.instructure.canvasapi2.utils.weave.catch
import com.instructure.canvasapi2.utils.weave.tryWeave
import com.instructure.pandautils.models.FileSubmitObject
import com.instructure.teacher.viewinterface.DiscussionsUpdateView
import instructure.androidblueprint.FragmentPresenter
import kotlinx.coroutines.experimental.Job

class DiscussionsUpdatePresenter(
        val canvasContext: CanvasContext,
        val discussionTopicHeaderId: Long,
        val discussionEntry: DiscussionEntry,
        val discussionTopic: DiscussionTopic) : FragmentPresenter<DiscussionsUpdateView>() {

    private var updateDiscussionJob: Job? = null
    var attachmentRemoved = false

    override fun loadData(forceNetwork: Boolean) {}

    override fun refresh(forceNetwork: Boolean) {}

    fun editMessage(message: String?) {
        if(updateDiscussionJob?.isActive == true) {
            viewCallback?.messageFailure(REASON_MESSAGE_IN_PROGRESS)
            return
        }

        updateDiscussionJob = tryWeave {
            if (attachmentRemoved) discussionEntry.attachments = null

            if(message == null) {
                viewCallback?.messageFailure(REASON_MESSAGE_EMPTY)
            } else {
                val response = awaitApiResponse<DiscussionEntry> {
                    DiscussionManager.updateDiscussionEntry(canvasContext, discussionTopicHeaderId, discussionEntry.id,
                            DiscussionEntryPostBody(message, discussionEntry.attachments), it) }

                if (response.code() in 200..299) {
                    response.body()?.let {
                        // Server doesn't send back attachments in the response for some reason
                        it.attachments = discussionEntry.attachments
                        viewCallback?.messageSuccess(it)
                    }
                } else {
                    viewCallback?.messageFailure(REASON_MESSAGE_FAILED_TO_SEND)
                }
            }
        } catch  {
            //Message update failure
            viewCallback?.messageFailure(REASON_MESSAGE_FAILED_TO_SEND)
        }
    }

    companion object {
        val REASON_MESSAGE_IN_PROGRESS = 1
        val REASON_MESSAGE_EMPTY = 2
        val REASON_MESSAGE_FAILED_TO_SEND = 3
        var attachment: FileSubmitObject? = null
    }

    override fun onDestroyed() {
        super.onDestroyed()
        updateDiscussionJob?.cancel()
    }
}
