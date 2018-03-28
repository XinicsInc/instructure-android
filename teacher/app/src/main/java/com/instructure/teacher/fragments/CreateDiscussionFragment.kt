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
import android.os.Handler
import android.support.design.widget.TextInputLayout
import android.support.v7.app.AlertDialog
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ScrollView
import android.widget.TextView
import com.instructure.canvasapi2.models.*
import com.instructure.canvasapi2.models.post_models.AssignmentPostBody
import com.instructure.canvasapi2.models.post_models.DiscussionTopicPostBody
import com.instructure.canvasapi2.utils.APIHelper
import com.instructure.canvasapi2.utils.NumberHelper
import com.instructure.pandautils.dialogs.DatePickerDialogFragment
import com.instructure.pandautils.dialogs.TimePickerDialogFragment
import com.instructure.pandautils.dialogs.UnsavedChangesExitDialog
import com.instructure.pandautils.dialogs.UploadFilesDialog
import com.instructure.pandautils.fragments.BasePresenterFragment
import com.instructure.pandautils.utils.*
import com.instructure.pandautils.views.AttachmentView
import com.instructure.teacher.R
import com.instructure.teacher.dialog.*
import com.instructure.teacher.events.AssigneesUpdatedEvent
import com.instructure.teacher.events.DiscussionCreatedEvent
import com.instructure.teacher.events.DiscussionUpdatedEvent
import com.instructure.teacher.events.post
import com.instructure.teacher.factory.CreateDiscussionPresenterFactory
import com.instructure.interactions.Identity
import com.instructure.teacher.models.DueDateGroup
import com.instructure.teacher.presenters.CreateDiscussionPresenter
import com.instructure.interactions.router.Route
import com.instructure.teacher.router.RouteMatcher
import com.instructure.teacher.utils.*
import com.instructure.teacher.view.AssignmentOverrideView
import com.instructure.teacher.viewinterface.CreateDiscussionView
import instructure.androidblueprint.PresenterFactory
import kotlinx.android.synthetic.main.fragment_create_discussion.*
import kotlinx.android.synthetic.main.view_assignment_override.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import kotlin.collections.ArrayList

class CreateDiscussionFragment : BasePresenterFragment<
        CreateDiscussionPresenter,
        CreateDiscussionView>(), CreateDiscussionView, Identity {

    private var mCanvasContext: CanvasContext by ParcelableArg(Course())
    private var mAssignment: Assignment? = null
    private var mDiscussionTopicHeader: DiscussionTopicHeader? by NullableParcelableArg()
    private val sendButton: TextView? get() = view?.findViewById(R.id.menuSaveDiscussion)
    private val saveButton: TextView? get() = view?.findViewById(R.id.menuSave)
    private val mAttachmentButton get() = toolbar.menu.findItem(R.id.menuAddAttachment)
    private var mIsPublished: Boolean by BooleanArg(false)
    private var mIsSubscribed: Boolean by BooleanArg(true)
    private var mAllowThreaded: Boolean by BooleanArg(false)
    private var mUsersMustPost: Boolean by BooleanArg(false)
    private var mDisplayGradeAs: String? by NullableStringArg()
    private var mDescription by NullableStringArg()
    private var mScrollToDates: Boolean = false
    private var mHasLoadedDataForEdit by BooleanArg()
    private var mRCEHasFocus = false

    //region Graded Discussion variables

    // We maintain a copy of the groupedDueDates to manipulate and use to display
    // overrides. When pushing changes, we update the original assignment object
    // with the changes in the copy.
    private var mEditDateGroups: MutableList<DueDateGroup> = arrayListOf()
    private val groupsMapped = hashMapOf<Long, Group>()
    private val sectionsMapped = hashMapOf<Long, Section>()
    private val studentsMapped = hashMapOf<Long, User>()

    // Keeps track of which override we were editing so we can scroll back to it when the user returns from editing assignees
    private var scrollBackToOverride: AssignmentOverrideView? = null

    private var mScrollHandler: Handler = Handler()

    private var mScrollToRunnable: Runnable = Runnable {
        if(isAdded) scrollView.fullScroll(ScrollView.FOCUS_DOWN)
    }

    //endregion

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

    private val removeOverrideClick: (DueDateGroup) -> Unit = { callback ->
        // Show confirmation dialog
        ConfirmRemoveAssignmentOverrideDialog.show(activity.supportFragmentManager) {
            if (mEditDateGroups.contains(callback)) mEditDateGroups.remove(callback)
            setupOverrides()
        }
    }


    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        mScrollHandler.removeCallbacks(mScrollToRunnable)
    }

    override fun onRefreshFinished() { }

    override fun onRefreshStarted() { }

    override val identity: Long? get() = 0
    override val skipCheck: Boolean get() = false

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        savedInstanceState?.let {
            @Suppress("UNCHECKED_CAST")
            mEditDateGroups = (savedInstanceState.getSerializable(EDIT_DATE_GROUPS) as ArrayList<DueDateGroup>)
            mRCEHasFocus = savedInstanceState.getBoolean(RCE_HAS_FOCUS)
        }
    }

    override fun onReadySetGo(presenter: CreateDiscussionPresenter?) {
        //if we already have something in the edit date groups we already have the full assignment and don't need to get it again.
        if(mAssignment != null && mEditDateGroups.size == 0) {
            //get the full assignment with overrides
            presenter?.getFullAssignment((mAssignment as Assignment).id)
        }
        setupToolbar()
        setupViews()

        if(mRCEHasFocus) {
            descriptionRCEView.requestEditorFocus()
            activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
    }

    override fun getPresenterFactory(): PresenterFactory<CreateDiscussionPresenter> = CreateDiscussionPresenterFactory(mCanvasContext, mAssignment)

    override fun onPresenterPrepared(presenter: CreateDiscussionPresenter?) { }

    override fun layoutResId(): Int = R.layout.fragment_create_discussion

    override fun updateDueDateGroups(groups: HashMap<Long, Group>, sections: HashMap<Long, Section>, students: HashMap<Long, User>) {

        groupsMapped += groups
        sectionsMapped += sections
        studentsMapped += students

        setupOverrides()

        if (mScrollToDates) {
            mScrollToDates = false
            // We came from the Dates page, scroll to the dates for editing
            mScrollHandler.postDelayed(mScrollToRunnable, 300)
        }

        scrollBackToOverride?.let {
            if (!mScrollToDates)
                scrollView.post {
                    scrollView.fullScroll(ScrollView.FOCUS_DOWN)
                }
            scrollBackToOverride = null
        }
    }

    override fun errorOccurred() {
        toast(R.string.error_occurred)
    }

    override fun updatedAssignment() {
        mEditDateGroups.clear()
        setupViews()
    }

    fun setupToolbar() {
        toolbar.setupCloseButton {
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

        toolbar.title = if(mDiscussionTopicHeader == null) getString(R.string.createDiscussion) else getString(R.string.editDiscussion)
        toolbar.setupMenu(if (mDiscussionTopicHeader == null) R.menu.create_discussion else R.menu.menu_save_generic) { menuItem ->
            when (menuItem.itemId) {
                R.id.menuSaveDiscussion, R.id.menuSave -> withRequireNetwork { saveDiscussion() }
                R.id.menuAddAttachment -> if (mDiscussionTopicHeader == null) addAttachment()
            }
        }
        ViewStyler.themeToolbarBottomSheet(activity, isTablet, toolbar, Color.BLACK, false)
        ViewStyler.setToolbarElevationSmall(context, toolbar)
        sendButton?.setTextColor(ThemePrefs.buttonColor)
        saveButton?.setTextColor(ThemePrefs.buttonColor)
    }

    fun setupViews() {
        (view as? ViewGroup)?.descendants<TextInputLayout>()?.forEach {
            it.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL))
        }

        descriptionRCEView.setHtml(mDescription ?: mDiscussionTopicHeader?.message,
                getString(R.string.discussion_details),
                getString(R.string.rce_empty_description),
                ThemePrefs.brandColor, ThemePrefs.buttonColor)

        // when the RCE editor has focus we want the label to be darker so it matches the title's functionality
        descriptionRCEView.setLabel(discussionDescLabel, R.color.defaultTextDark, R.color.defaultTextGray)

        if (!mHasLoadedDataForEdit) mDiscussionTopicHeader?.let {
            editDiscussionName.setText(it.title)
            mIsPublished = it.isPublished
            mAllowThreaded = it.type == DiscussionTopicHeader.DiscussionType.THREADED
            mUsersMustPost = it.isRequireInitialPost
            mIsSubscribed = it.isSubscribed
            mHasLoadedDataForEdit = true
        }

        ViewStyler.themeEditText(context, editDiscussionName, ThemePrefs.brandColor)
        ViewStyler.themeEditText(context, editGradePoints, ThemePrefs.brandColor)

        setupPublishSwitch()
        setupSubscribeSwitch()
        setupAllowThreadedSwitch()
        setupUsersMustPostSwitch()
        updateAttachmentUI()

        if(presenter.getAssignment() == null) {
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
            //make the graded things gone, we can't create a graded discussion
            gradeWrapper.setGone()
            addOverride.setGone()
            subscribeWrapper.setGone()
        } else {
            // Points possible
            val pointsPossible = (presenter.getAssignment() as Assignment).pointsPossible
            editGradePoints.setText(NumberHelper.formatDecimal(pointsPossible, 2, true))

            if(mDisplayGradeAs == null) {
                mDisplayGradeAs = (presenter.getAssignment() as Assignment).gradingType
            }

            setupDisplayGradeAs()

            if (mEditDateGroups.isEmpty()) mEditDateGroups.addAll((presenter.getAssignment() as Assignment).groupedDueDates)

            if (groupsMapped.isEmpty() && sectionsMapped.isEmpty() && studentsMapped.isEmpty()) {
                presenter.getDueDateInfo((presenter.getAssignment() as Assignment).groupCategoryId)
            }

            addOverride.setOnClickListener {
                mEditDateGroups.add(DueDateGroup())
                setupOverrides()
                scrollView.post {
                    scrollView.fullScroll(ScrollView.FOCUS_DOWN)
                }
                // This opens the assignees page to save the user a click.
                overrideContainer.descendants<AssignmentOverrideView>().last().assignTo.performClick()
            }
        }
        setupOverrides()
        setupDelete()
    }

    private fun setupPublishSwitch()  {
        //if a student has submitted something, we can't let the teacher unpublish the discussion
        if (presenter.getAssignment()?.isUnpublishable == true) {
            publishWrapper.setGone()
            mIsPublished = true
            return
        }
        // Publish status
        publishSwitch.applyTheme()

        publishSwitch.isChecked = mIsPublished

        publishSwitch.setOnCheckedChangeListener { _, isChecked -> mIsPublished = isChecked }
    }

    private fun setupSubscribeSwitch()  {

        subscribeSwitch.applyTheme()

        subscribeSwitch.isChecked = mIsSubscribed

        subscribeSwitch.setOnCheckedChangeListener { _, isChecked -> mIsSubscribed = isChecked }
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

    private fun setupOverrides() {
        overrideContainer.removeAllViews()

        if(presenter.getAssignment() == null) {
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

        } else {

            // Load in overrides
            if(groupsMapped.isNotEmpty() || sectionsMapped.isNotEmpty() || studentsMapped.isNotEmpty()) {
                mEditDateGroups.forEachIndexed { index, dueDateGroup ->
                    val assignees = ArrayList<String>()
                    val v = AssignmentOverrideView(activity)
                    if (dueDateGroup.isEveryone) {
                        assignees += getString(if (mEditDateGroups.any { it.hasOverrideAssignees }) R.string.everyone_else else R.string.everyone)
                    }
                    dueDateGroup.groupIds.forEach { assignees.add(groupsMapped[it]?.name!!) }
                    dueDateGroup.sectionIds.forEach { assignees.add(sectionsMapped[it]?.name!!) }
                    dueDateGroup.studentIds.forEach { assignees.add(studentsMapped[it]?.name!!) }

                    v.setupOverride(index, dueDateGroup, mEditDateGroups.size > 1, assignees, datePickerOnClick, timePickerOnClick, removeOverrideClick) {
                        val args = AssigneeListFragment.makeBundle(
                                mEditDateGroups,
                                index,
                                sectionsMapped.values.toList(),
                                groupsMapped.values.toList(),
                                studentsMapped.values.toList())
                        RouteMatcher.route(context, Route(AssigneeListFragment::class.java, mCanvasContext, args))
                        scrollBackToOverride = v
                    }

                    overrideContainer.addView(v)
                }
            }
        }

        overrideContainer.descendants<TextInputLayout>().forEach {
            it.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL))
        }
    }

    private fun setupDisplayGradeAs() {
        // Filters spinner
        val spinnerAdapter = ArrayAdapter.createFromResource(context, R.array.display_grade_as_types_discussion, R.layout.simple_spinner_item)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        displayGradeAsSpinner.adapter = spinnerAdapter
        ViewStyler.themeSpinner(context, displayGradeAsSpinner, ThemePrefs.brandColor)
        displayGradeAsSpinner.onItemSelectedListener = null

        when(mDisplayGradeAs) {
            Assignment.POINTS_TYPE -> displayGradeAsSpinner.setSelection(spinnerAdapter.getPosition(getString(R.string.points)))
            Assignment.GPA_SCALE_TYPE -> displayGradeAsSpinner.setSelection(spinnerAdapter.getPosition(getString(R.string.gpa_scale)))
            Assignment.LETTER_GRADE_TYPE -> displayGradeAsSpinner.setSelection(spinnerAdapter.getPosition(getString(R.string.letter_grade)))
            Assignment.PASS_FAIL_TYPE -> displayGradeAsSpinner.setSelection(spinnerAdapter.getPosition(getString(R.string.complete_incomplete)))
            Assignment.PERCENT_TYPE -> displayGradeAsSpinner.setSelection(spinnerAdapter.getPosition(getString(R.string.percentage)))
        }

        displayGradeAsSpinner.onItemSelectedListener = (object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if(view == null) return
                when((view as TextView).text.toString()) {
                    getString(R.string.points) -> mDisplayGradeAs = Assignment.POINTS_TYPE
                    getString(R.string.gpa_scale) -> mDisplayGradeAs = Assignment.GPA_SCALE_TYPE
                    getString(R.string.letter_grade) -> mDisplayGradeAs = Assignment.LETTER_GRADE_TYPE
                    getString(R.string.complete_incomplete) -> mDisplayGradeAs = Assignment.PASS_FAIL_TYPE
                    getString(R.string.percentage) -> mDisplayGradeAs = Assignment.PERCENT_TYPE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        })
    }

    private fun setupDelete() {
        deleteWrapper.setVisible(mDiscussionTopicHeader != null)
        deleteWrapper.onClickWithRequireNetwork {
            AlertDialog.Builder(context)
                .setTitle(R.string.discussions_delete_title)
                .setMessage(R.string.discussions_delete_message)
                .setPositiveButton(R.string.delete) { _, _ ->
                    if(mDiscussionTopicHeader != null) {
                        presenter?.deleteDiscussionTopicHeader(mDiscussionTopicHeader!!.id)
                    }
                }
                .setNegativeButton(R.string.cancel) { _, _ -> }
                .showThemed()
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
        mDiscussionTopicHeader?.attachments?.firstOrNull()?.let {
            val attachmentView = AttachmentView(context)
            attachmentView.setPendingRemoteFile(it, true) { action, attachment ->
                if (action == AttachmentView.AttachmentAction.REMOVE) {
                    presenter.attachmentRemoved = true
                    mDiscussionTopicHeader?.attachments?.remove(attachment)
                }
            }
            attachmentLayout.addView(attachmentView)
        }
    }

    private fun updateAttachmentButton(show: Boolean = true) {
        // Only show if (1) we're in creation mode and (2) we don't already have an attachment
        mAttachmentButton?.isVisible = show && mDiscussionTopicHeader == null && presenter.attachment == null
    }

    private fun addAttachment() {
        // set the description here. When we ask for permission to use the camera the app can call readySetGo and reset the description
        mDescription = descriptionRCEView.html

        val bundle = UploadFilesDialog.createDiscussionsBundle(ArrayList())
        UploadFilesDialog.show(fragmentManager, bundle, { event, attachment ->
            if(event == UploadFilesDialog.EVENT_ON_FILE_SELECTED) {
                presenter.attachment = attachment
                updateAttachmentUI()
            }
        })
    }

    override fun startSavingDiscussion() {
        sendButton?.setGone()
        savingProgressBar.announceForAccessibility(getString(R.string.saving))
        savingProgressBar.setVisible()
    }

    override fun errorSavingDiscussion() {
        sendButton?.setVisible()
        savingProgressBar.setGone()
    }

    override fun discussionSavedSuccessfully(discussionTopic: DiscussionTopicHeader?) {
        if(discussionTopic == null) {
            DiscussionCreatedEvent(true).post() // Post bus event
            toast(R.string.discussionSuccessfullyCreated) // let the user know the discussion was saved
        } else {
            discussionTopic.assignment = presenter.getAssignment()
            DiscussionUpdatedEvent(discussionTopic).post()
            toast(R.string.discussionSuccessfullyUpdated)
        }

        editDiscussionName.hideKeyboard() // close the keyboard
        activity.onBackPressed() // close this fragment
    }

    private fun saveDiscussion() {

        if(mDiscussionTopicHeader != null) {
            val postData = DiscussionTopicPostBody()
            //discussion title isn't required
            if(editDiscussionName.text.isEmpty()) {
                postData.title = getString(R.string.no_title)
            } else {
                postData.title = editDiscussionName.text?.toString() ?: getString(R.string.no_title)
            }
            postData.message = descriptionRCEView.html
            postData.published = mIsPublished
            postData.discussionType = if (mAllowThreaded) DiscussionTopicHeader.DiscussionType.THREADED.toString().toLowerCase() else DiscussionTopicHeader.DiscussionType.SIDE_COMMENT.toString().toLowerCase()
            postData.requireInitialPost = mUsersMustPost

            if (presenter.getAssignment() == null) {
                postData.delayedPostAt = APIHelper.dateToString(mEditDateGroups[0].coreDates.unlockDate)
                postData.lockAt = mEditDateGroups[0].coreDates.lockDate
            } else {
                val assignmentPostData = AssignmentPostBody()
                assignmentPostData.gradingType = mDisplayGradeAs
                assignmentPostData.setGroupedDueDates(mEditDateGroups)
                assignmentPostData.pointsPossible = editGradePoints.text.toString().toDouble()

                postData.assignment = assignmentPostData
            }

            presenter.editDiscussion((mDiscussionTopicHeader as DiscussionTopicHeader).id, postData)
        } else {
            val discussionTopicHeader = DiscussionTopicHeader()

            if(editDiscussionName.text.isEmpty()) {
                discussionTopicHeader.title = getString(R.string.no_title)
            } else {
                discussionTopicHeader.title = editDiscussionName.text.toString()
            }
            discussionTopicHeader.message = descriptionRCEView.html
            discussionTopicHeader.isPublished = mIsPublished
            discussionTopicHeader.isSubscribed = mIsSubscribed
            discussionTopicHeader.type = if (mAllowThreaded) DiscussionTopicHeader.DiscussionType.THREADED else DiscussionTopicHeader.DiscussionType.SIDE_COMMENT
            discussionTopicHeader.isRequireInitialPost = mUsersMustPost

            //if the assignment is null, that means we're creating/editing a discussion. When we do this we initialize mEditDateGroups with an empty DueDateGroup
            if (presenter.getAssignment() == null) {
                discussionTopicHeader.setDelayedPostAtDate(mEditDateGroups[0].coreDates.unlockDate)
                discussionTopicHeader.setLockAtDate(mEditDateGroups[0].coreDates.lockDate)
            }
            presenter.saveDiscussion(discussionTopicHeader)
        }
    }

    override fun discussionDeletedSuccessfully(discussionTopicHeaderId: Long) {
        activity?.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putSerializable(EDIT_DATE_GROUPS, ArrayList<DueDateGroup>(mEditDateGroups))
        outState?.putBoolean(RCE_HAS_FOCUS, descriptionRCEView.hasFocus())
        mDescription = descriptionRCEView.html
        super.onSaveInstanceState(outState)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onAssigneesChanged(event: AssigneesUpdatedEvent) {
        // Update grouped due dates (EditDateGroups)
        event.once(javaClass.simpleName) { dates ->
            mEditDateGroups = dates.toMutableList()
            setupOverrides()
            //remove it so when we go to another assignment or discussion it won't show up there too
            EventBus.getDefault().removeStickyEvent(event)
        }
    }

    companion object {
        private val EDIT_DATE_GROUPS = "editDateGroups"
        private val RCE_HAS_FOCUS = "rceHasFocus"

        @JvmStatic private val CANVAS_CONTEXT = "canvas_context"
        @JvmStatic private val DISCUSSION_TOPIC_HEADER = "discussion_topic_header"
        @JvmStatic private val SHOULD_SCROLL_TO_DATES = "shouldScrollToDates"

        @JvmStatic
        fun newInstance(args: Bundle) = CreateDiscussionFragment().apply {
            if(args.containsKey(CANVAS_CONTEXT)) {
                mCanvasContext = args.getParcelable(CANVAS_CONTEXT)
            }
            if(args.containsKey(DISCUSSION_TOPIC_HEADER)) {
                mDiscussionTopicHeader = args.getParcelable(DISCUSSION_TOPIC_HEADER)
                mAssignment = mDiscussionTopicHeader?.assignment
            }
            if(args.containsKey(SHOULD_SCROLL_TO_DATES)) {
                mScrollToDates = args.getBoolean(SHOULD_SCROLL_TO_DATES)
            }
        }

        @JvmStatic
        fun makeBundle(canvasContext: CanvasContext) : Bundle {
            return Bundle().apply {
                putParcelable(CANVAS_CONTEXT, canvasContext)
            }
        }

        @JvmStatic
        fun makeBundle(canvasContext: CanvasContext, discussionTopicHeader: DiscussionTopicHeader) : Bundle {
            return Bundle().apply {
                putParcelable(CANVAS_CONTEXT, canvasContext)
                putParcelable(DISCUSSION_TOPIC_HEADER, discussionTopicHeader)
            }
        }

        @JvmStatic
        fun makeBundle(canvasContext: CanvasContext, discussionTopicHeader: DiscussionTopicHeader, shouldScrollToDates: Boolean) : Bundle {
            return Bundle().apply {
                putParcelable(CANVAS_CONTEXT, canvasContext)
                putParcelable(DISCUSSION_TOPIC_HEADER, discussionTopicHeader)
                putBoolean(SHOULD_SCROLL_TO_DATES, shouldScrollToDates)
            }
        }
    }
}
