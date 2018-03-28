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
package com.instructure.teacher.fragments

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.TextView
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.Course
import com.instructure.canvasapi2.models.DiscussionTopicHeader
import com.instructure.canvasapi2.utils.DateHelper
import com.instructure.canvasapi2.utils.parcelCopy
import com.instructure.pandautils.dialogs.DatePickerDialogFragment
import com.instructure.pandautils.dialogs.TimePickerDialogFragment
import com.instructure.pandautils.dialogs.UnsavedChangesExitDialog
import com.instructure.pandautils.dialogs.UploadFilesDialog
import com.instructure.pandautils.fragments.BasePresenterFragment
import com.instructure.pandautils.utils.*
import com.instructure.pandautils.views.AttachmentView
import com.instructure.teacher.R
import com.instructure.teacher.factory.CreateOrEditAnnouncementPresenterFactory
import com.instructure.interactions.Identity
import com.instructure.teacher.presenters.CreateOrEditAnnouncementPresenter
import com.instructure.teacher.utils.*
import com.instructure.teacher.viewinterface.CreateOrEditAnnouncementView
import instructure.androidblueprint.PresenterFactory
import kotlinx.android.synthetic.main.fragment_create_or_edit_announcement.*
import java.util.*
import kotlin.collections.ArrayList

class CreateOrEditAnnouncementFragment :
        BasePresenterFragment<CreateOrEditAnnouncementPresenter, CreateOrEditAnnouncementView>(),
        CreateOrEditAnnouncementView,
        Identity {

    /* The course this announcement belongs to */
    private var mCanvasContext by ParcelableArg<CanvasContext>(Course())

    /* The announcement to be edited. This will be null if we're creating a new announcement */
    private var mEditAnnouncement by NullableParcelableArg<DiscussionTopicHeader>()

    /* Menu buttons. We don't cache these because the toolbar is reconstructed on configuration change. */
    private val mSaveMenuButton get() = toolbar.menu.findItem(R.id.menuSaveAnnouncement)
    private val mAttachmentButton get() = toolbar.menu.findItem(R.id.menuAddAttachment)
    private val mSaveButtonTextView: TextView? get() = view?.findViewById<TextView>(R.id.menuSaveAnnouncement)

    /* Formats for displaying the delayed post date */
    private val mDateFormat by lazy { DateHelper.getFullMonthNoLeadingZeroDateFormat() }
    private val mTimeFormat by lazy { DateHelper.getPreferredTimeFormat(context) }
    
    /* The default date to show when the user enables delayed posting (the current date just before midnight) */
    private val mDefaultDate: Date
        get() = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

    override val identity = 0L
    override val skipCheck = false
    override fun onRefreshFinished() {}
    override fun onRefreshStarted() {}
    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {}
    override fun onPresenterPrepared(presenter: CreateOrEditAnnouncementPresenter?) {}
    override fun layoutResId(): Int = R.layout.fragment_create_or_edit_announcement

    override fun getPresenterFactory(): PresenterFactory<CreateOrEditAnnouncementPresenter> {
        return CreateOrEditAnnouncementPresenterFactory(mCanvasContext, mEditAnnouncement?.parcelCopy())
    }

    override fun onReadySetGo(presenter: CreateOrEditAnnouncementPresenter?) {
        setupViews()
        setupToolbar()
    }

    fun setupToolbar() {
        toolbar.setupCloseButton {
            if(presenter?.announcement?.message == announcementRCEView?.html) {
                activity?.onBackPressed()
            } else {
                UnsavedChangesExitDialog.show(fragmentManager, {
                    activity?.onBackPressed()
                })
            }
        }
        toolbar.title = getString(if (presenter.isEditing) R.string.editAnnouncementTitle else R.string.create_announcement_title)
        toolbar.setupMenu(R.menu.create_announcement) { menuItem ->
            when (menuItem.itemId) {
                R.id.menuSaveAnnouncement -> withRequireNetwork { saveAnnouncement() }
                R.id.menuAddAttachment -> if (!presenter.isEditing) addAttachment()
            }
        }
        ViewStyler.themeToolbarBottomSheet(activity, isTablet, toolbar, Color.BLACK, false)
        ViewStyler.setToolbarElevationSmall(context, toolbar)
        if (presenter.isEditing) with(mSaveMenuButton) {
            setIcon(0)
            setTitle(R.string.save)
        }
        mSaveButtonTextView?.setTextColor(ThemePrefs.buttonColor)
    }

    private fun setupViews() {
        setupTitle()
        setupDescription()
        setupDeleteButton()
        setupDelaySwitch()
        setupUsersMustPostSwitch()
        updateAttachmentUI()
    }

    private fun setupTitle() {
        ViewStyler.themeEditText(context, announcementNameEditText, ThemePrefs.brandColor)
        announcementNameTextInput.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL))
        announcementNameEditText.setText(presenter.announcement.title)
        announcementNameEditText.onTextChanged { presenter.announcement.title = it }
    }

    private fun setupDescription() {
        announcementRCEView.setHtml(
                presenter.announcement.message,
                getString(R.string.announcementDetails),
                getString(R.string.rce_empty_description),
                ThemePrefs.brandColor, ThemePrefs.buttonColor
        )
        // when the RCE editor has focus we want the label to be darker so it matches the title's functionality
        announcementRCEView.setLabel(announcementDescLabel, R.color.defaultTextDark, R.color.defaultTextGray)
    }

    private fun setupDeleteButton() {
        // Only show delete button in editing mode
        deleteAnnouncementButton
                .setVisible(presenter.isEditing)
                .onClickWithRequireNetwork {
                    AlertDialog.Builder(context)
                            .setTitle(R.string.deleteAnnouncementDialogTitle)
                            .setMessage(R.string.deleteAnnouncementDialogMessage)
                            .setPositiveButton(R.string.delete) { _, _ ->
                                presenter.deleteAnnouncement()
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .showThemed()
                }
    }

    private fun setupDelaySwitch() {
        delaySwitch.applyTheme()
        delaySwitch.isChecked = presenter.announcement.delayedPostAt != null
        updatePostDate()

        delaySwitch.setOnCheckedChangeListener { _, isChecked ->
            presenter.announcement.setDelayedPostAtDate(if (isChecked) mDefaultDate else null)
            updatePostDate()
        }

        postDate.onClick {
            DatePickerDialogFragment.getInstance(activity.supportFragmentManager) { year, month, dayOfMonth ->
                val date = Calendar.getInstance().apply {
                    time = presenter.announcement.delayedPostAt
                    set(year, month, dayOfMonth)
                }.time
                presenter.announcement.setDelayedPostAtDate(date)
                updatePostDate()
            }.show(activity.supportFragmentManager, DatePickerDialogFragment::class.java.simpleName)
        }

        postTime.onClick {
            TimePickerDialogFragment.getInstance(activity.supportFragmentManager) { hour, min ->
                val date = Calendar.getInstance().apply {
                    time = presenter.announcement.delayedPostAt
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, min)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time
                presenter.announcement.setDelayedPostAtDate(date)
                updatePostDate()
            }.show(activity.supportFragmentManager, TimePickerDialogFragment::class.java.simpleName)
        }
    }

    private fun updateAttachmentUI() {
        updateAttachmentButton()
        attachmentLayout.clearAttachmentViews()

        // Show attachment waiting to upload (if any)
        presenter.attachment?.let { attachment ->
            val attachmentView = AttachmentView(context)
            attachmentView.setPendingAttachment(attachment.toAttachment(), true) { action, _ ->
                if (action == AttachmentView.AttachmentAction.REMOVE) {
                    presenter.attachment = null
                    updateAttachmentButton()
                }
            }
            attachmentLayout.addView(attachmentView)
        }

        // Show existing attachment (if any)
        presenter.announcement.attachments?.firstOrNull()?.let {
            val attachmentView = AttachmentView(context)
            attachmentView.setPendingRemoteFile(it, true) { action, attachment ->
                if (action == AttachmentView.AttachmentAction.REMOVE) {
                    presenter.attachmentRemoved = true
                    presenter.announcement.attachments.remove(attachment)
                }
            }
            attachmentLayout.addView(attachmentView)
        }
    }

    private fun updatePostDate() {
        val date = presenter.announcement.delayedPostAt
        if (date == null) {
            postDateWrapper.setGone()
        } else {
            postDateWrapper.setVisible()
            postDate.setText(mDateFormat.format(date))
            postTime.setText(mTimeFormat.format(date))
        }
    }

    private fun setupUsersMustPostSwitch() {
        usersMustPostSwitch.applyTheme()
        usersMustPostSwitch.isChecked = presenter.announcement.isRequireInitialPost
        usersMustPostSwitch.setOnCheckedChangeListener { _, isChecked ->
            presenter.announcement.isRequireInitialPost = isChecked
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        presenter.announcement.message = announcementRCEView.html
    }

    private fun saveAnnouncement() {
        val description = announcementRCEView.html
        if (description.isNullOrBlank()) {
            toast(R.string.create_announcement_no_description)
            return
        }

        if (announcementNameEditText.text.isBlank()) {
            val noTitleString = getString(R.string.no_title)
            announcementNameEditText.setText(noTitleString)
            presenter.announcement.title = noTitleString
        }

        presenter.announcement.message = description
        presenter.saveAnnouncement()
    }

    override fun onSaveStarted() {
        mSaveMenuButton.isVisible = false
        updateAttachmentButton(show = false)
        savingProgressBar.announceForAccessibility(getString(R.string.saving))
        savingProgressBar.setVisible()
    }

    private fun updateAttachmentButton(show: Boolean = true) {
        // Only show if (1) we're in creation mode and (2) we don't already have an attachment
        mAttachmentButton?.isVisible = show && !presenter.isEditing && presenter.attachment == null
    }

    override fun onSaveError() {
        mSaveMenuButton.isVisible = true
        updateAttachmentButton()
        savingProgressBar.setGone()
        toast(R.string.errorSavingAnnouncement)
    }

    override fun onDeleteError() {
        mSaveMenuButton.isVisible = true
        updateAttachmentButton()
        savingProgressBar.setGone()
        toast(R.string.errorDeletingAnnouncement)
    }

    override fun onSaveSuccess() {
        if (presenter.isEditing) {
            toast(R.string.announcementSuccessfullyUpdated)
        } else {
            toast(R.string.announcementSuccessfullyCreated)
        }
        announcementNameEditText.hideKeyboard() // close the keyboard
        activity.onBackPressed() // close this fragment
    }

    override fun onDeleteSuccess() {
        toast(R.string.announcementDeleted)
        announcementNameEditText.hideKeyboard() // close the keyboard
        activity.onBackPressed() // close this fragment
    }

    private fun addAttachment() {
        val bundle = UploadFilesDialog.createDiscussionsBundle(ArrayList())
        UploadFilesDialog.show(fragmentManager, bundle, { event, attachment ->
            if(event == UploadFilesDialog.EVENT_ON_FILE_SELECTED) {
                presenter.attachment = attachment
                updateAttachmentUI()
            }
        })
    }

    companion object {
        @JvmStatic
        fun newInstance(bundle: Bundle) = CreateOrEditAnnouncementFragment().apply {
            arguments = bundle
        }

        @JvmStatic
        fun newInstanceCreate(canvasContext: CanvasContext) = CreateOrEditAnnouncementFragment().apply {
            mCanvasContext = canvasContext
        }

        @JvmStatic
        fun newInstanceEdit(canvasContext: CanvasContext, editAnnouncement: DiscussionTopicHeader)
                = CreateOrEditAnnouncementFragment().apply {
            mCanvasContext = canvasContext
            mEditAnnouncement = editAnnouncement
        }
    }
}
