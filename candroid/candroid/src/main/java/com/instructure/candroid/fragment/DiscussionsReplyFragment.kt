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
package com.instructure.candroid.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.*
import com.instructure.candroid.R
import com.instructure.candroid.util.Const
import com.instructure.canvasapi2.managers.DiscussionManager
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.DiscussionEntry
import com.instructure.canvasapi2.models.DiscussionParticipant
import com.instructure.canvasapi2.utils.APIHelper
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.canvasapi2.utils.weave.awaitApiResponse
import com.instructure.canvasapi2.utils.weave.catch
import com.instructure.canvasapi2.utils.weave.tryWeave
import com.instructure.loginapi.login.dialog.NoInternetConnectionDialog
import com.instructure.pandautils.dialogs.UploadFilesDialog
import com.instructure.pandautils.discussions.DiscussionCaching
import com.instructure.pandautils.models.FileSubmitObject
import com.instructure.pandautils.utils.*
import com.instructure.pandautils.views.AttachmentView
import kotlinx.android.synthetic.main.fragment_discussions_reply.*
import kotlinx.coroutines.experimental.Job
import retrofit2.Response
import java.io.File

class DiscussionsReplyFragment : ParentFragment() {

    private var postDiscussionJob: Job? = null

    private var discussionTopicHeaderId: Long by LongArg(default = 0L) //The topic the discussion belongs too
    private var discussionEntryId: Long by LongArg(default = 0L) //The future parent of the discussion entry we are creating
    private var attachment: FileSubmitObject? = null
    private var canAttach: Boolean by BooleanArg()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return layoutInflater.inflate(R.layout.fragment_discussions_reply, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rceTextEditor.setHint(R.string.rce_empty_message)
    }

    override fun title(): String {
        return getString(R.string.reply)
    }

    override fun allowBookmarking(): Boolean {
        return false
    }

    override fun applyTheme() {
        toolbar.title = getString(R.string.reply)
        toolbar.setupAsCloseButton(this)
        if(canAttach) {
            toolbar.setMenu(R.menu.menu_discussion_reply, menuItemCallback)
        } else {
            toolbar.setMenu(R.menu.menu_discussion_reply_no_attach, menuItemCallback)
        }
        ViewStyler.themeToolbarBottomSheet(activity, isTablet, toolbar, Color.BLACK, false)
        ViewStyler.setToolbarElevationSmall(context, toolbar)
    }

    private val menuItemCallback: (MenuItem) -> Unit = { item ->
        when (item.itemId) {
            R.id.menu_send -> {
                if(APIHelper.hasNetworkConnection()) {
                    sendMessage(rceTextEditor.html)
                } else {
                    NoInternetConnectionDialog.show(fragmentManager)
                }
            }
            R.id.menu_attachment -> {
                if(APIHelper.hasNetworkConnection()) {
                    val attachments = ArrayList<FileSubmitObject>()
                    if (attachment != null) attachments.add(attachment!!)

                    val bundle = UploadFilesDialog.createDiscussionsBundle(attachments)
                    UploadFilesDialog.show(fragmentManager, bundle, { event, attachment ->
                        if(event == UploadFilesDialog.EVENT_ON_FILE_SELECTED) {
                            handleAttachment(attachment)
                        }
                    })
                } else {
                    NoInternetConnectionDialog.show(fragmentManager)
                }
            }
        }
    }

    private fun handleAttachment(file: FileSubmitObject?) {
        if(file != null) {
            this@DiscussionsReplyFragment.attachment = file
            attachments.setAttachment(file.toAttachment()) { action, _ ->
                if (action == AttachmentView.AttachmentAction.REMOVE) {
                    this@DiscussionsReplyFragment.attachment = null
                }
            }
        } else {
            this@DiscussionsReplyFragment.attachment = null
            attachments.clearAttachmentViews()
        }
    }

    private fun sendMessage(message: String?) {
        if(postDiscussionJob?.isActive == true) return

        postDiscussionJob = tryWeave {
            if(attachment == null) {
                if (discussionEntryId == discussionTopicHeaderId) {
                    messageSentResponse(awaitApiResponse<DiscussionEntry> { DiscussionManager.postToDiscussionTopic(canvasContext, discussionTopicHeaderId, message, it) })
                } else {
                    messageSentResponse(awaitApiResponse<DiscussionEntry> { DiscussionManager.replyToDiscussionEntry(canvasContext, discussionTopicHeaderId, discussionEntryId, message, it) })
                }
            } else {
                if(discussionEntryId == discussionTopicHeaderId) {
                    messageSentResponse(awaitApiResponse<DiscussionEntry> { DiscussionManager.postToDiscussionTopic(canvasContext, discussionTopicHeaderId, message, File(attachment!!.fullPath), attachment?.contentType ?: "multipart/form-data", it) })
                } else {
                    messageSentResponse(awaitApiResponse<DiscussionEntry> { DiscussionManager.replyToDiscussionEntry(canvasContext, discussionTopicHeaderId, discussionEntryId, message, File(attachment!!.fullPath), attachment?.contentType ?: "multipart/form-data", it) })
                }
            }
        } catch {
            if(isAdded) toast(R.string.utils_discussionSentFailure)
        }
    }

    private fun messageSentResponse(response: Response<DiscussionEntry>) {
        if (response.code() in 200..299 && response.body() != null) {
            val discussionEntry = response.body()

            ApiPrefs.user?.let {
                // The author does not come back in the response, we add the current user so things will display from cache properly
                discussionEntry!!.author = DiscussionParticipant().apply {
                    id = it.id
                    displayName = it.shortName
                    avatarImageUrl = it.avatarUrl
                }
            }

            //post successful
            DiscussionCaching(discussionTopicHeaderId).saveEntry(discussionEntry)// Save to cache
            DiscussionEntryEvent(discussionEntry!!.id).postSticky()// Notify about new reply
            toast(R.string.utils_discussionSentSuccess)
            activity?.onBackPressed()
        } else {
            //post failure
            toast(R.string.utils_discussionSentFailure)
        }
    }

    companion object {
        private const val DISCUSSION_TOPIC_HEADER_ID = "DISCUSSION_TOPIC_HEADER_ID"
        private const val DISCUSSION_ENTRY_ID = "DISCUSSION_ENTRY_ID"
        private const val IS_ANNOUNCEMENT = "IS_ANNOUNCEMENT"
        private const val CAN_ATTACH = "CAN_ATTACH"

        @JvmStatic
        fun makeBundle(
                discussionTopicHeaderId: Long,
                discussionEntryId: Long,
                isAnnouncement: Boolean,
                canAttach: Boolean): Bundle = Bundle().apply {

            putLong(DISCUSSION_TOPIC_HEADER_ID, discussionTopicHeaderId)
            putLong(DISCUSSION_ENTRY_ID, discussionEntryId)
            putBoolean(IS_ANNOUNCEMENT, isAnnouncement)
            putBoolean(CAN_ATTACH, canAttach)
        }

        @JvmStatic
        fun newInstance(canvasContext: CanvasContext, args: Bundle) = DiscussionsReplyFragment().apply {
            arguments = Bundle().apply {
                putParcelable(Const.CANVAS_CONTEXT, canvasContext)
            }
            discussionTopicHeaderId = args.getLong(DISCUSSION_TOPIC_HEADER_ID)
            discussionEntryId = args.getLong(DISCUSSION_ENTRY_ID)
            canAttach = args.getBoolean(CAN_ATTACH)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        postDiscussionJob?.cancel()
    }
}
