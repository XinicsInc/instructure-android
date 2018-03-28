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
 */    package com.instructure.candroid.fragment

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.instructure.candroid.R
import com.instructure.candroid.events.DiscussionCreatedEvent
import com.instructure.candroid.events.DiscussionUpdatedEvent
import com.instructure.candroid.events.post
import com.instructure.canvasapi2.managers.DiscussionManager
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.DiscussionTopicHeader
import com.instructure.canvasapi2.models.post_models.DiscussionTopicPostBody
import com.instructure.canvasapi2.utils.DateHelper
import com.instructure.canvasapi2.utils.NetworkUtils
import com.instructure.canvasapi2.utils.weave.awaitApi
import com.instructure.canvasapi2.utils.weave.catch
import com.instructure.canvasapi2.utils.weave.tryWeave
import com.instructure.pandautils.dialogs.UnsavedChangesExitDialog
import com.instructure.pandautils.models.FileSubmitObject
import com.instructure.pandautils.utils.*
import kotlinx.android.synthetic.main.fragment_create_announcement.*
import kotlinx.coroutines.experimental.Job
import java.util.*

class CreateAnnouncementFragment : ParentFragment() {

    /* The announcement to be edited. This will be null if we're creating a new announcement */
    private var editAnnouncement by NullableParcelableArg<DiscussionTopicHeader>()

    /* Menu buttons. We don't cache these because the toolbar is reconstructed on configuration change. */
    private val mSaveMenuButton get() = createAnnouncementToolbar.menu.findItem(R.id.menuSaveAnnouncement)
    private val mSaveButtonTextView: TextView? get() = view?.findViewById(R.id.menuSaveAnnouncement)

    private var apiJob: Job? = null
    var isEditing = editAnnouncement != null

    /**
     * The announcement that is being edited/created. Changes should be applied directly to this
     * object. For editing mode this object should be passed to the constructor as a deep copy of
     * the original so that canceled changes are not erroneously propagated back to other pages. In
     * creation mode this object will be generated with the values necessary to distinguish it as
     * an announcement instead of a normal discussion topic header.
     */
    val announcement: DiscussionTopicHeader = editAnnouncement ?: DiscussionTopicHeader().apply {
        isAnnouncement = true
        isPublished = true
        type = DiscussionTopicHeader.DiscussionType.SIDE_COMMENT
    }

    override fun title() = ""
    override fun allowBookmarking() = false
    override fun applyTheme() {
        setupToolbar()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setRetainInstance(this, true)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return layoutInflater.inflate(R.layout.fragment_create_announcement, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupViews()
    }

    private fun setupToolbar() {
        createAnnouncementToolbar.setupAsCloseButton {
            if(announcement.message == announcementRCEView?.html) {
                activity?.onBackPressed()
            } else {
                UnsavedChangesExitDialog.show(fragmentManager, {
                    activity?.onBackPressed()
                })
            }
        }
        createAnnouncementToolbar.title = getString(if (isEditing) R.string.utils_editAnnouncementTitle else R.string.utils_createAnnouncementTitle)
        createAnnouncementToolbar.setMenu(R.menu.create_announcement) { menuItem ->
            when (menuItem.itemId) {
                R.id.menuSaveAnnouncement -> if(NetworkUtils.isNetworkAvailable) { saveAnnouncement() }
            }
        }
        ViewStyler.themeToolbarBottomSheet(activity, isTablet, createAnnouncementToolbar, Color.BLACK, false)
        ViewStyler.setToolbarElevationSmall(context, createAnnouncementToolbar)
        if (isEditing) with(mSaveMenuButton) {
            setIcon(0)
            setTitle(R.string.save)
        }
        mSaveButtonTextView?.setTextColor(ThemePrefs.buttonColor)
    }

    private fun setupViews() {
        setupTitle()
        setupDescription()
    }

    private fun setupTitle() {
        ViewStyler.themeEditText(context, announcementNameEditText, ThemePrefs.brandColor)
        announcementNameTextInput.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL))
        announcementNameEditText.setText(announcement.title)
        announcementNameEditText.onTextChanged { announcement.title = it }
    }

    private fun setupDescription() {
        announcementRCEView.setHtml(
                announcement.message,
                getString(R.string.utils_announcementDetails),
                getString(R.string.rce_empty_description),
                ThemePrefs.brandColor, ThemePrefs.buttonColor
        )
        // when the RCE editor has focus we want the label to be darker so it matches the title's functionality
        announcementRCEView.setLabel(announcementDescLabel, R.color.defaultTextDark, R.color.defaultTextGray)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        announcement.message = announcementRCEView.html
    }

    private fun saveAnnouncement() {
        val description = announcementRCEView.html
        if (description.isNullOrBlank()) {
            toast(R.string.utils_createAnnouncementNoDescription)
            return
        }

        if (announcementNameEditText.text.isBlank()) {
            val noTitleString = getString(R.string.utils_noTitle)
            announcementNameEditText.setText(noTitleString)
            announcement.title = noTitleString
        }

        announcement.message = description
        saveAnnouncementAPI()
    }

    private fun onSaveStarted() {
        mSaveMenuButton.isVisible = false
        savingProgressBar.announceForAccessibility(getString(R.string.utils_saving))
        savingProgressBar.setVisible()
    }

    private fun onSaveError() {
        mSaveMenuButton.isVisible = true
        savingProgressBar.setGone()
        toast(R.string.utils_errorSavingAnnouncement)
    }

    private fun onSaveSuccess() {
        if (isEditing) {
            toast(R.string.utils_announcementSuccessfullyUpdated)
        } else {
            toast(R.string.utils_announcementSuccessfullyCreated)
        }
        announcementNameEditText.hideKeyboard() // close the keyboard
        activity.onBackPressed() // close this fragment
    }

    @Suppress("EXPERIMENTAL_FEATURE_WARNING")
    private fun saveAnnouncementAPI() {
        onSaveStarted()
        apiJob = tryWeave {
            if (isEditing) {
                val postBody = DiscussionTopicPostBody.fromAnnouncement(announcement, false)
                val updatedAnnouncement = awaitApi<DiscussionTopicHeader> { callback ->
                    DiscussionManager.editDiscussionTopic(canvasContext, announcement.id, postBody, callback)
                }
                DiscussionUpdatedEvent(updatedAnnouncement).post()
            } else {
                awaitApi<DiscussionTopicHeader> {
                    DiscussionManager.createStudentDiscussion(canvasContext, announcement, null, it)
                }
                DiscussionCreatedEvent(true).post()
            }
            onSaveSuccess()
        } catch {
            onSaveError()
        }
    }

    companion object {
        @JvmStatic private val DISCUSSION_TOPIC_HEADER = "discussion_topic_header"

        @JvmStatic
        fun makeBundle(canvasContext: CanvasContext, discussionTopicHeader: DiscussionTopicHeader) : Bundle {
            return ParentFragment.createBundle(canvasContext).apply {
                putParcelable(DISCUSSION_TOPIC_HEADER, discussionTopicHeader)
            }
        }

        @JvmStatic
        fun newInstance(args: Bundle) = CreateAnnouncementFragment().apply {
            arguments = args
            editAnnouncement = args.getParcelable(DISCUSSION_TOPIC_HEADER)
        }
    }
}