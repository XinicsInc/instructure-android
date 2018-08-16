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
import android.widget.Toast
import com.instructure.candroid.R
import com.instructure.candroid.util.Const
import com.instructure.canvasapi2.managers.DiscussionManager
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.DiscussionEntry
import com.instructure.canvasapi2.models.DiscussionTopic
import com.instructure.canvasapi2.models.post_models.DiscussionEntryPostBody
import com.instructure.canvasapi2.utils.APIHelper
import com.instructure.canvasapi2.utils.weave.awaitApiResponse
import com.instructure.canvasapi2.utils.weave.catch
import com.instructure.canvasapi2.utils.weave.tryWeave
import com.instructure.pandautils.dialogs.UnsavedChangesExitDialog
import com.instructure.pandautils.discussions.DiscussionCaching
import com.instructure.pandautils.utils.*
import com.instructure.pandautils.views.AttachmentView
import kotlinx.android.synthetic.main.fragment_discussions_update.*
import kotlinx.coroutines.experimental.Job

class DiscussionsUpdateFragment : ParentFragment() {

    private var updateDiscussionJob: Job? = null

    private var discussionTopicHeaderId: Long by LongArg(default = 0L) //The topic the discussion belongs too
    private var discussionEntry: DiscussionEntry by ParcelableArg(default = DiscussionEntry())
    private var discussionTopic: DiscussionTopic by ParcelableArg(default = DiscussionTopic())
    private var attachmentRemoved: Boolean by BooleanArg(default = false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return layoutInflater.inflate(R.layout.fragment_discussions_update, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rceTextEditor.setHint(R.string.rce_empty_description)
        rceTextEditor.setHtml(discussionEntry.message, "", "", ThemePrefs.brandColor, ThemePrefs.buttonColor)

        discussionEntry.attachments?.firstOrNull()?.let {
            val attachmentView = AttachmentView(context)
            attachmentView.setPendingRemoteFile(it, true) { action, attachment ->
                if (action == AttachmentView.AttachmentAction.REMOVE) {
                    attachmentRemoved = true
                    discussionEntry.attachments.remove(attachment)
                }
            }
            attachmentLayout.addView(attachmentView)
        }
    }

    override fun title(): String = getString(R.string.edit)

    override fun allowBookmarking(): Boolean = false

    override fun applyTheme() {
        toolbar.title = getString(R.string.edit)
        toolbar.setupAsCloseButton {
            if(discussionEntry.message == rceTextEditor?.html) {
                activity?.onBackPressed()
            } else {
                UnsavedChangesExitDialog.show(fragmentManager, {
                    activity?.onBackPressed()
                })
            }
        }
        toolbar.setMenu(R.menu.menu_discussion_update, menuItemCallback)
        ViewStyler.themeToolbarBottomSheet(activity, isTablet, toolbar, Color.BLACK, false)
        ViewStyler.setToolbarElevationSmall(context, toolbar)
    }

    private val menuItemCallback: (MenuItem) -> Unit = { item ->
        when (item.itemId) {
            R.id.menu_save -> {
                if(APIHelper.hasNetworkConnection()) {
                    editMessage(rceTextEditor.html)
                } else {
                    Toast.makeText(context, R.string.noInternetConnectionMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun editMessage(message: String?) {
        if(updateDiscussionJob?.isActive == true) return

        updateDiscussionJob = tryWeave {
            if (attachmentRemoved) discussionEntry.attachments = null

            val response = awaitApiResponse<DiscussionEntry> {
                DiscussionManager.updateDiscussionEntry(canvasContext, discussionTopicHeaderId, discussionEntry.id,
                        DiscussionEntryPostBody(message, discussionEntry.attachments), it) }

            if (response.code() in 200..299) {
                //post successful
                response.body()?.let {
                    DiscussionCaching(discussionTopicHeaderId).saveEntry(it)// Save to cache
                    DiscussionEntryEvent(it.id).postSticky()// Notify about the updated reply
                    toast(R.string.utils_discussionUpdateSuccess)
                    activity?.onBackPressed()
                }
            } else {
                //post failure
                toast(R.string.utils_discussionSentFailure)
            }

        } catch  {
            //Message update failure
            toast(R.string.utils_discussionSentFailure)
        }
    }

    companion object {
        private const val DISCUSSION_TOPIC_HEADER_ID = "DISCUSSION_TOPIC_HEADER_ID"
        private const val DISCUSSION_ENTRY = "DISCUSSION_ENTRY"
        private const val DISCUSSION_TOPIC = "DISCUSSION_TOPIC"
        private const val IS_ANNOUNCEMENT = "IS_ANNOUNCEMENT"

        @JvmStatic
        fun makeBundle(
                discussionTopicHeaderId: Long,
                discussionEntryId: DiscussionEntry?,
                isAnnouncement: Boolean,
                discussionTopic: DiscussionTopic): Bundle = Bundle().apply {

            putLong(DISCUSSION_TOPIC_HEADER_ID, discussionTopicHeaderId)
            putParcelable(DISCUSSION_ENTRY, discussionEntryId)
            putBoolean(IS_ANNOUNCEMENT, isAnnouncement)
            putParcelable(DISCUSSION_TOPIC, discussionTopic)
        }

        @JvmStatic
        fun newInstance(canvasContext: CanvasContext, args: Bundle) = DiscussionsUpdateFragment().apply {
            args.putParcelable(Const.CANVAS_CONTEXT, canvasContext)
            arguments = args
            discussionTopicHeaderId = args.getLong(DISCUSSION_TOPIC_HEADER_ID)
            discussionEntry = args.getParcelable(DISCUSSION_ENTRY)
            discussionTopic = args.getParcelable(DISCUSSION_TOPIC)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        updateDiscussionJob?.cancel()
    }
}
