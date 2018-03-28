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
import android.support.design.widget.TextInputLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.instructure.candroid.R
import com.instructure.candroid.dialog.UnsavedChangesExitDialog
import com.instructure.candroid.events.DiscussionCreatedEvent
import com.instructure.candroid.events.post
import com.instructure.candroid.view.AssignmentOverrideView
import com.instructure.canvasapi2.StatusCallback
import com.instructure.canvasapi2.managers.*
import com.instructure.canvasapi2.models.*
import com.instructure.canvasapi2.models.post_models.DiscussionTopicPostBody
import com.instructure.canvasapi2.utils.*
import com.instructure.canvasapi2.utils.weave.*
import com.instructure.pandautils.dialogs.DatePickerDialogFragment
import com.instructure.pandautils.dialogs.TimePickerDialogFragment
import com.instructure.pandautils.dialogs.UploadFilesDialog
import com.instructure.pandautils.models.DueDateGroup
import com.instructure.pandautils.models.FileSubmitObject
import com.instructure.pandautils.utils.*
import com.instructure.pandautils.views.AttachmentView
import kotlinx.android.synthetic.main.fragment_create_discussion.*
import kotlinx.coroutines.experimental.Job
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import java.io.File
import java.util.*

class CreateDiscussionFragment : ParentFragment() {

    private val sendButton: TextView? get() = view?.findViewById(R.id.menuSaveDiscussion)
    private val saveButton: TextView? get() = view?.findViewById(R.id.menuSave)
//    private val mAttachmentButton: TextView? get() = view?.findViewById(R.id.menuAddAttachment) BLOCKED COMMS 868

    private var mDiscussionTopicHeader: DiscussionTopicHeader? by NullableParcelableArg()
    private var mAllowThreaded: Boolean by BooleanArg(false)
    private var mUsersMustPost: Boolean by BooleanArg(false)
    private var mDescription by NullableStringArg()
    private var mHasLoadedDataForEdit by BooleanArg()

    private var mEditDateGroups: MutableList<DueDateGroup> = arrayListOf()

    private var mCreateDiscussionCall: Job? = null


    /**
     * (Creation mode only) An attachment to be uploaded alongside the discussion. Note that this
     * can only be used when creating new discussions. Setting/changing attachments on existing
     * discussions (editing mode) is currently unsupported.
     */
    var attachment: FileSubmitObject? = null

    /** (Editing mode only) Set to *true* if the existing discussions's attachment should be removed */
    var attachmentRemoved = false

    private val datePickerOnClick: (date: Date?, (Int, Int, Int) -> Unit) -> Unit = { date, callback ->
        DatePickerDialogFragment.getInstance(activity.supportFragmentManager, date) { year, month, dayOfMonth ->
            callback(year, month, dayOfMonth)
        }.show(activity.supportFragmentManager, DatePickerDialogFragment::class.java.simpleName)
    }

    private val timePickerOnClick: (date: Date?, (Int, Int) -> Unit) -> Unit = { date, callback ->
        TimePickerDialogFragment.getInstance(activity.supportFragmentManager, date) { hour, min ->
            callback(hour, min)
        }.show(activity.supportFragmentManager, TimePickerDialogFragment::class.java.simpleName)
    }

    override fun title() = ""
    override fun allowBookmarking() = false
    override fun applyTheme() { setupToolbar() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mDiscussionTopicHeader = arguments.getParcelable(DISCUSSION_TOPIC_HEADER)
        setRetainInstance(this, true)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return layoutInflater.inflate(R.layout.fragment_create_discussion, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupViews()
        attachmentLayout.setGone()
    }

    private fun setupToolbar() {
        createDiscussionToolbar.setupAsCloseButton {
            if(mDiscussionTopicHeader == null) {
                activity?.onBackPressed()
            } else {
                if (mDiscussionTopicHeader?.message == descriptionRCEView?.html) {
                    activity?.onBackPressed()
                } else {
                    UnsavedChangesExitDialog.show(fragmentManager, {
                        activity?.onBackPressed()
                    })
                }
            }
        }

        createDiscussionToolbar.title = if(mDiscussionTopicHeader == null) getString(R.string.utils_createDiscussion) else getString(R.string.utils_editDiscussion)
        createDiscussionToolbar.setMenu(if (mDiscussionTopicHeader == null) R.menu.create_discussion else R.menu.menu_save_generic) { menuItem ->
            when (menuItem.itemId) {
                R.id.menuSaveDiscussion, R.id.menuSave -> if(NetworkUtils.isNetworkAvailable) saveDiscussion()
                //R.id.menuAddAttachment -> if (mDiscussionTopicHeader == null) addAttachment() BLOCKED COMMS 868
            }
        }
        ViewStyler.themeToolbarBottomSheet(activity, isTablet, createDiscussionToolbar, Color.BLACK, false)
        ViewStyler.setToolbarElevationSmall(context, createDiscussionToolbar)
        sendButton?.setTextColor(ThemePrefs.buttonColor)
        saveButton?.setTextColor(ThemePrefs.buttonColor)
    }

    fun setupViews() {
        (view as? ViewGroup)?.descendants<TextInputLayout>()?.forEach {
            it.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL))
        }

        descriptionRCEView.setHtml(mDescription ?: mDiscussionTopicHeader?.message,
                getString(R.string.utils_discussionDetails),
                getString(R.string.rce_empty_description),
                ThemePrefs.brandColor, ThemePrefs.buttonColor)

        // when the RCE editor has focus we want the label to be darker so it matches the title's functionality
        descriptionRCEView.setLabel(discussionDescLabel, R.color.defaultTextDark, R.color.defaultTextGray)

        if (!mHasLoadedDataForEdit) mDiscussionTopicHeader?.let {
            editDiscussionName.setText(it.title)
            mAllowThreaded = it.type == DiscussionTopicHeader.DiscussionType.THREADED
            mUsersMustPost = it.isRequireInitialPost
            mHasLoadedDataForEdit = true
        }

        ViewStyler.themeEditText(context, editDiscussionName, ThemePrefs.brandColor)

        setupAllowThreadedSwitch()
        setupUsersMustPostSwitch()
//        updateAttachmentUI() BLOCKED COMMS 868

        if(mEditDateGroups.isEmpty()) {
            //if the dateGroups is empty, we want to add a due date so that we can set the available from and to fields
            mEditDateGroups.clear()
            val dueDateGroup = DueDateGroup()
            if(mDiscussionTopicHeader != null) {
                //populate the availability dates if we have them, the assignment is null, so this is an ungraded assignment
                dueDateGroup.coreDates.lockDate = (mDiscussionTopicHeader as DiscussionTopicHeader).lockAt
                dueDateGroup.coreDates.unlockDate = (mDiscussionTopicHeader as DiscussionTopicHeader).delayedPostAt
            }
            mEditDateGroups.add(dueDateGroup)
        }

        setupOverrides()

        setupDelete()
    }

    private fun setupOverrides() {
        overrideContainer.removeAllViews()

        // Load in overrides
        mEditDateGroups.forEachIndexed { index, dueDateGroup ->
            val assignees = ArrayList<String>()
            val v = AssignmentOverrideView(activity)

            v.toAndFromDatesOnly()

            v.setupOverride(index, dueDateGroup, mEditDateGroups.size > 1, assignees, datePickerOnClick, timePickerOnClick, {
                if (mEditDateGroups.contains(it)) mEditDateGroups.remove(it)
                setupOverrides()
            }) { }

            overrideContainer.addView(v)
        }
    }

    private fun setupAllowThreadedSwitch()  {

        threadedSwitch.applyTheme()

        threadedSwitch.isChecked = mAllowThreaded

        threadedSwitch.setOnCheckedChangeListener { _, isChecked -> mAllowThreaded = isChecked }
    }

    private fun setupUsersMustPostSwitch()  {

        usersMustPostSwitch.applyTheme()

        usersMustPostSwitch.isChecked = mUsersMustPost

        usersMustPostSwitch.setOnCheckedChangeListener { _, isChecked -> mUsersMustPost = isChecked }
    }

    private fun setupDelete() {
        // TODO - For now we set it to be gone, in the future we will revisit after COMMS-868
        deleteWrapper.setGone()
        /*
        deleteWrapper.setVisible(mDiscussionTopicHeader != null)
        deleteWrapper.onClickWithRequireNetwork {
            AlertDialog.Builder(context)
                    .setTitle(R.string.discussionsDeleteTitle)
                    .setMessage(R.string.discussionsDeleteMessage)
                    .setPositiveButton(R.string.delete) { _, _ ->
                        if(mDiscussionTopicHeader != null) {
                            deleteDiscussionTopicHeader(mDiscussionTopicHeader!!.id)
                        }
                    }
                    .setNegativeButton(R.string.cancel) { _, _ -> }
                    .showThemed()
        }
        */
    }


    /* Blocked COMMS 868
    private fun updateAttachmentUI() {
        updateAttachmentButton()
        attachmentLayout.clearAttachmentViews()

        // Show attachment waiting to upload (if any)
        attachment?.let { attachment ->
            val attachmentView = AttachmentView(context)
            attachmentView.setPendingAttachment(attachment.toAttachment(), true) { action, _ ->
                if (action == AttachmentView.AttachmentAction.REMOVE) {
                    this@CreateDiscussionFragment.attachment = null
                    updateAttachmentButton()
                }
            }
            attachmentLayout.addView(attachmentView)
        }

        // Show existing attachment (if any)
        mDiscussionTopicHeader?.attachments?.firstOrNull()?.let {
            val attachmentView = AttachmentView(context)
            attachmentView.setPendingRemoteFile(it, true) { action, attachment ->
                if (action == AttachmentView.AttachmentAction.REMOVE) {
                    this@CreateDiscussionFragment.attachmentRemoved = true
                    mDiscussionTopicHeader?.attachments?.remove(attachment)
                }
            }
            attachmentLayout.addView(attachmentView)
        }
    }
    */

    /* blocked COMMS 868
    private fun updateAttachmentButton(show: Boolean = true) {
        val quantity = if(attachment == null) 0 else 1
        mAttachmentButton?.text = resources.getQuantityString(R.plurals.utils_addAttachment, quantity, quantity)
        // Only show if (1) we're in creation mode and (2) we don't already have an attachment
        mAttachmentButton?.setVisible(show && mDiscussionTopicHeader == null && attachment == null)
    }
    */

    private fun saveDiscussion() {

        if(mDiscussionTopicHeader != null) {
            val postData = DiscussionTopicPostBody()
            //discussion title isn't required
            if(editDiscussionName.text.isEmpty()) {
                postData.title = getString(R.string.utils_noTitle)
            } else {
                postData.title = editDiscussionName.text?.toString() ?: getString(R.string.utils_noTitle)
            }
            postData.message = descriptionRCEView.html
            postData.discussionType = if (mAllowThreaded) DiscussionTopicHeader.DiscussionType.THREADED.toString().toLowerCase() else DiscussionTopicHeader.DiscussionType.SIDE_COMMENT.toString().toLowerCase()
            postData.requireInitialPost = mUsersMustPost

            editDiscussion((mDiscussionTopicHeader as DiscussionTopicHeader).id, postData)
        } else {
            val discussionTopicHeader = DiscussionTopicHeader()

            if(editDiscussionName.text.isEmpty()) {
                discussionTopicHeader.title = getString(R.string.utils_noTitle)
            } else {
                discussionTopicHeader.title = editDiscussionName.text.toString()
            }
            discussionTopicHeader.message = descriptionRCEView.html
            discussionTopicHeader.type = if (mAllowThreaded) DiscussionTopicHeader.DiscussionType.THREADED else DiscussionTopicHeader.DiscussionType.SIDE_COMMENT
            discussionTopicHeader.isRequireInitialPost = mUsersMustPost

            if(mEditDateGroups[0].coreDates.unlockDate != null) {
                discussionTopicHeader.setDelayedPostAtDate(mEditDateGroups[0].coreDates.unlockDate)
            }

            if(mEditDateGroups[0].coreDates.lockDate != null) {
                discussionTopicHeader.setLockAtDate(mEditDateGroups[0].coreDates.lockDate)
            }

            saveDiscussion(discussionTopicHeader)
        }

    }

    private fun saveDiscussion(discussionTopicHeader: DiscussionTopicHeader) {
        startSavingDiscussion()
        @Suppress("EXPERIMENTAL_FEATURE_WARNING")
        mCreateDiscussionCall = tryWeave {
            var filePart: MultipartBody.Part? = null
            attachment?.let {
                val file = File(it.fullPath)
                val requestBody = RequestBody.create(MediaType.parse(it.contentType), file)
                filePart = MultipartBody.Part.createFormData("attachment", file.name, requestBody)
            }
            awaitApi<DiscussionTopicHeader> { DiscussionManager.createStudentDiscussion(canvasContext, discussionTopicHeader, filePart, it) }
            discussionSavedSuccessfully(null)

        } catch {
            if(it is StatusCallbackError) {
                val statusCode = it.response?.code()
                if(statusCode == 500) { //Quota has been reached. Likely the discussion as indeed created.
                    errorSavingDiscussionAttachment()
                } else {
                    errorSavingDiscussion()
                }
            }
        }
    }

    private fun editDiscussion(topicId: Long, discussionTopicPostBody: DiscussionTopicPostBody) {
        startSavingDiscussion()
        @Suppress("EXPERIMENTAL_FEATURE_WARNING")
        mCreateDiscussionCall = weave {
            try {
                if (attachmentRemoved) discussionTopicPostBody.removeAttachment = ""
                val discussionTopic = awaitApi<DiscussionTopicHeader> { DiscussionManager.editDiscussionTopic(canvasContext, topicId, discussionTopicPostBody, it) }
                discussionSavedSuccessfully(discussionTopic)

            } catch (e: Throwable) {
                errorSavingDiscussion()
            }
        }
    }

    private fun deleteDiscussionTopicHeader(discussionTopicHeaderId: Long) {
        DiscussionManager.deleteDiscussionTopicHeader(canvasContext, discussionTopicHeaderId, object: StatusCallback<Void>(){
            override fun onResponse(response: Response<Void>, linkHeaders: LinkHeaders, type: ApiType) {
                if(response.code() in 200..299) {
//                    DiscussionTopicHeaderDeletedEvent(discussionTopicHeaderId, (DiscussionsDetailsFragment::class.java.toString() + ".onResume()")).post() // Todo -re-add after COMSS-868
                    discussionDeletedSuccessfully(discussionTopicHeaderId)
                }
            }
        })
    }

    private fun startSavingDiscussion() {
        sendButton?.setGone()
//        mAttachmentButton?.setGone() Blocked COMMS 868
        savingProgressBar.announceForAccessibility(getString(R.string.utils_saving))
        savingProgressBar.setVisible()
    }

    private fun errorSavingDiscussion() {
        sendButton?.setVisible()
        /* blocked COMMS 868
        val quantity = if(attachment == null) 0 else 1
        mAttachmentButton?.text = resources.getQuantityString(R.plurals.utils_addAttachment, quantity, quantity)
        mAttachmentButton?.setVisible()
        */
        savingProgressBar.setGone()
    }

    private fun errorSavingDiscussionAttachment() {
        toast(R.string.utils_discussionSuccessfulAttachmentNot)
        editDiscussionName.hideKeyboard() // close the keyboard
        navigation?.popCurrentFragment()
    }

    private fun discussionSavedSuccessfully(discussionTopic: DiscussionTopicHeader?) {
        if(discussionTopic == null) {
            DiscussionCreatedEvent(true).post() // Post bus event
            toast(R.string.utils_discussionSuccessfullyCreated) // let the user know the discussion was saved
        } else {
//            discussionTopic.assignment = getAssignment()
//            DiscussionUpdatedEvent(discussionTopic).post() TODO - re-add after COMMS-868
            toast(R.string.utils_discussionSuccessfullyUpdated)
        }

        editDiscussionName.hideKeyboard() // close the keyboard
        navigation?.popCurrentFragment()
    }

    fun discussionDeletedSuccessfully(discussionTopicHeaderId: Long) {
        activity?.onBackPressed()
    }

    /* Blocked COMMS 868
    private fun addAttachment() {
        val bundle = UploadFilesDialog.createDiscussionsBundle(ArrayList())
        UploadFilesDialog.show(fragmentManager, bundle, { event, attachment ->
            if(event == UploadFilesDialog.EVENT_ON_FILE_SELECTED) {
                this.attachment = attachment
                updateAttachmentUI()
            }
        })
    }
    */

    companion object {
        @JvmStatic private val DISCUSSION_TOPIC_HEADER = "discussion_topic_header"

        @JvmStatic
        fun makeBundle(canvasContext: CanvasContext, discussionTopicHeader: DiscussionTopicHeader) : Bundle {
            return ParentFragment.createBundle(canvasContext).apply {
                putParcelable(DISCUSSION_TOPIC_HEADER, discussionTopicHeader)
            }
        }

        @JvmStatic
        fun newInstance(args: Bundle) = CreateDiscussionFragment().apply {
            arguments = args
            mDiscussionTopicHeader = args.getParcelable(DISCUSSION_TOPIC_HEADER)
        }
    }

}