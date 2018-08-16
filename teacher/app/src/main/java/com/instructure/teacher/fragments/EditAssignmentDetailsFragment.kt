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
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatDelegate
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ScrollView
import android.widget.TextView
import com.instructure.canvasapi2.managers.AssignmentManager
import com.instructure.canvasapi2.managers.GroupCategoriesManager
import com.instructure.canvasapi2.managers.SectionManager
import com.instructure.canvasapi2.managers.UserManager
import com.instructure.canvasapi2.models.*
import com.instructure.canvasapi2.models.Assignment.*
import com.instructure.canvasapi2.models.post_models.AssignmentPostBody
import com.instructure.canvasapi2.utils.NumberHelper
import com.instructure.canvasapi2.utils.weave.awaitApi
import com.instructure.canvasapi2.utils.weave.weave
import com.instructure.pandautils.dialogs.DatePickerDialogFragment
import com.instructure.pandautils.dialogs.TimePickerDialogFragment
import com.instructure.pandautils.fragments.BaseFragment
import com.instructure.pandautils.utils.*
import com.instructure.teacher.R
import com.instructure.teacher.dialog.ConfirmRemoveAssignmentOverrideDialog
import com.instructure.teacher.events.AssigneesUpdatedEvent
import com.instructure.teacher.events.AssignmentUpdatedEvent
import com.instructure.teacher.events.post
import com.instructure.teacher.models.DueDateGroup
import com.instructure.interactions.router.Route
import com.instructure.teacher.router.RouteMatcher
import com.instructure.teacher.utils.*
import com.instructure.teacher.view.AssignmentOverrideView
import kotlinx.android.synthetic.main.fragment_edit_assignment_details.*
import kotlinx.android.synthetic.main.view_assignment_override.view.*
import kotlinx.coroutines.experimental.Job
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

class EditAssignmentDetailsFragment : BaseFragment() {
    private val EDIT_DATE_GROUPS = "editdategroups"

    private var mCourse: Course by ParcelableArg(Course())
    private var mAssignment: Assignment by ParcelableArg(Assignment())
    private var mIsPublished: Boolean = true
    private var mScrollToDates: Boolean = false
    private var mDisplayGradeAs: String? = null
    private var mDisplayGradeAsType: GRADING_TYPE = GRADING_TYPE.POINTS

    private val saveButton: TextView? get() = view?.findViewById<TextView>(R.id.menuSave)

    val groupsMapped = hashMapOf<Long, Group>()
    val sectionsMapped = hashMapOf<Long, Section>()
    val studentsMapped = hashMapOf<Long, User>()

    // Keeps track of which override we were editing so we can scroll back to it when the user returns from editing assignees
    private var scrollBackToOverride: AssignmentOverrideView? = null

    var mDueDateApiCalls: Job? = null
    var mPutAssignmentCall: Job? = null

    var mScrollHandler: Handler = Handler()

    var mScrollToRunnable: Runnable = Runnable {
        if(isAdded) scrollView.fullScroll(ScrollView.FOCUS_DOWN)
    }

    // We maintain a copy of the groupedDueDates to manipulate and use to display
    // overrides. When pushing changes, we update the original assignment object
    // with the changes in the copy.
    private var mEditDateGroups: MutableList<DueDateGroup> = arrayListOf()

    val datePickerOnClick: (date: Date?, (Int, Int, Int) -> Unit) -> Unit = { date, callback ->
        DatePickerDialogFragment.getInstance(activity.supportFragmentManager, date) { year, month, dayOfMonth ->
            callback(year, month, dayOfMonth)
        }.show(activity.supportFragmentManager, DatePickerDialogFragment::class.java.simpleName)
    }

    val timePickerOnClick: (date: Date?, (Int, Int) -> Unit) -> Unit = { date, callback ->
        TimePickerDialogFragment.getInstance(activity.supportFragmentManager, date) { hour, min ->
            callback(hour, min)
        }.show(activity.supportFragmentManager, TimePickerDialogFragment::class.java.simpleName)
    }

    val removeOverrideClick: (DueDateGroup) -> Unit = { callback ->
        // Show confirmation dialog
        ConfirmRemoveAssignmentOverrideDialog.show(activity.supportFragmentManager) {
            if (mEditDateGroups.contains(callback)) mEditDateGroups.remove(callback)
            setupOverrides()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    override fun layoutResId() = R.layout.fragment_edit_assignment_details

    override fun onCreateView(view: View?) {}

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        savedInstanceState?.let {
            @Suppress("UNCHECKED_CAST")
            mEditDateGroups = (savedInstanceState.getSerializable(EDIT_DATE_GROUPS) as ArrayList<DueDateGroup>)
        }

        // Hide Keyboard
        activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        setupViews()
        setupToolbar()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    private fun setupToolbar() {
        toolbar.setupCloseButton(this)
        toolbar.title = getString(R.string.edit_assignment)
        toolbar.setupMenu(R.menu.menu_save_generic) { saveAssignment() }
        ViewStyler.themeToolbarBottomSheet(activity, isTablet, toolbar, Color.BLACK, false)
        ViewStyler.setToolbarElevationSmall(context, toolbar)
        saveButton?.setTextColor(ThemePrefs.buttonColor)
    }

    private fun setupPublishSwitch() = with(mAssignment) {
        //if a student has submitted something, we can't let the teacher unpublish the assignment
        if (!mAssignment.isUnpublishable) {
            publishWrapper.setGone()
            mIsPublished = true
            return
        }
        // Publish status
        publishSwitch.applyTheme()
        publishSwitch.isChecked = isPublished
        mIsPublished = isPublished

        publishSwitch.setOnCheckedChangeListener { _, isChecked -> mIsPublished = isChecked }
    }

    private fun setupDisplayGradeAs() {
        // Filters spinner
        val spinnerAdapter = ArrayAdapter.createFromResource(context, R.array.display_grade_as_types, R.layout.simple_spinner_item)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        displayGradeAsSpinner.adapter = spinnerAdapter
        ViewStyler.themeSpinner(context, displayGradeAsSpinner, ThemePrefs.brandColor)
        displayGradeAsSpinner.onItemSelectedListener = null

        when(mDisplayGradeAs) {
            POINTS_TYPE -> displayGradeAsSpinner.setSelection(spinnerAdapter.getPosition(getString(R.string.points)))
            GPA_SCALE_TYPE -> displayGradeAsSpinner.setSelection(spinnerAdapter.getPosition(getString(R.string.gpa_scale)))
            LETTER_GRADE_TYPE -> displayGradeAsSpinner.setSelection(spinnerAdapter.getPosition(getString(R.string.letter_grade)))
            PASS_FAIL_TYPE -> displayGradeAsSpinner.setSelection(spinnerAdapter.getPosition(getString(R.string.complete_incomplete)))
            PERCENT_TYPE-> displayGradeAsSpinner.setSelection(spinnerAdapter.getPosition(getString(R.string.percentage)))
            NOT_GRADED_TYPE -> displayGradeAsSpinner.setSelection(spinnerAdapter.getPosition(getString(R.string.not_graded)))
        }

        displayGradeAsSpinner.onItemSelectedListener = (object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if(view == null) return
                when((view as TextView).text.toString()) {
                    getString(R.string.points) -> mDisplayGradeAs = POINTS_TYPE
                    getString(R.string.gpa_scale) -> mDisplayGradeAs = GPA_SCALE_TYPE
                    getString(R.string.letter_grade) -> mDisplayGradeAs = LETTER_GRADE_TYPE
                    getString(R.string.complete_incomplete) -> mDisplayGradeAs = PASS_FAIL_TYPE
                    getString(R.string.percentage) -> mDisplayGradeAs = PERCENT_TYPE
                    getString(R.string.not_graded) -> mDisplayGradeAs = NOT_GRADED_TYPE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        })
    }

    @Suppress("EXPERIMENTAL_FEATURE_WARNING")
    private fun setupViews() = with(mAssignment) {

        (view as? ViewGroup)?.descendants<TextInputLayout>()?.forEach {
            it.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL))
        }

        // Assignment name
        editAssignmentName.setText(name)
        // set the underline to be the brand color
        ViewStyler.themeEditText(context, editAssignmentName, ThemePrefs.brandColor)
        editAssignmentName.onTextChanged {
            if (it.isBlank()) {
                assignmentNameTextInput.error = getString(R.string.assignment_name_must_be_set)
            } else {
                assignmentNameTextInput.isErrorEnabled = false
            }
        }
        // Points possible
        editGradePoints.setText(NumberHelper.formatDecimal(pointsPossible, 2, true))
        // set the underline to be the brand color
        ViewStyler.themeEditText(context, editGradePoints, ThemePrefs.brandColor)
        editGradePoints.onTextChanged {
            if (it.isBlank()) {
                gradeTotalTextInput.error = getString(R.string.assignment_points_must_be_set)
            } else {
                gradeTotalTextInput.isErrorEnabled = false
            }
        }
        setupPublishSwitch()

        if(mDisplayGradeAs == null) {
            mDisplayGradeAs = gradingType
        }
        setupDisplayGradeAs()

        ViewStyler.themeInputTextLayout(assignmentNameTextInput, ContextCompat.getColor(context, R.color.defaultTextGray))
        ViewStyler.themeInputTextLayout(gradeTotalTextInput, ContextCompat.getColor(context, R.color.defaultTextGray))
        ViewStyler.setToolbarElevation(context, toolbar, R.dimen.toolbar_elevation_small)

        // Description
        setupDescription()

        if (mEditDateGroups.isEmpty()) mEditDateGroups.addAll(mAssignment.groupedDueDates)

        mDueDateApiCalls = weave {
            try {
                if (groupsMapped.isEmpty() && sectionsMapped.isEmpty() && studentsMapped.isEmpty()) {
                    val sections = awaitApi<List<Section>> { SectionManager.getAllSectionsForCourse(courseId, it, false) }
                    val groups = if (groupCategoryId > 0L) awaitApi<List<Group>> { GroupCategoriesManager.getAllGroupsForCategory(groupCategoryId, it, false) } else emptyList()
                    val students = awaitApi<List<User>> { UserManager.getAllPeopleList(mCourse, it, false) }
                    groupsMapped += groups.associateBy { it.id }
                    sectionsMapped += sections.associateBy { it.id }
                    studentsMapped += students.associateBy { it.id }
                }
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
            } catch (e: Throwable) {
                if (isAdded) {
                    toast(R.string.error_occurred)
                }
            }
        }

        // Theme add button and plus image
        addOverrideText.setTextColor(ThemePrefs.buttonColor)
        plus.setColorFilter(ThemePrefs.buttonColor)

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

    private fun setupOverrides() {
        overrideContainer.removeAllViews()
        // Load in overrides
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
                RouteMatcher.route(context, Route(AssigneeListFragment::class.java, mCourse, args))
                scrollBackToOverride = v
            }

            overrideContainer.addView(v)
        }

        overrideContainer.descendants<TextInputLayout>().forEach {
            it.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL))
        }
    }

    private fun setupDescription() {
        // Show progress bar while loading description
        descriptionProgressBar.announceForAccessibility(getString(R.string.loading))
        descriptionProgressBar.setVisible()
        // Load description
        descriptionEditor.setHtml(mAssignment.description,
                getString(R.string.assignmentDescriptionContentDescription),
                getString(R.string.rce_empty_description),
                ThemePrefs.brandColor, ThemePrefs.buttonColor)

        // when the RCE editor has focus we want the label to be darker so it matches the title's functionality
        descriptionEditor.setLabel(assignmentDescLabel, R.color.defaultTextDark, R.color.defaultTextGray)
    }

    private fun saveAssignment() {
        // Both name and points are required
        if (editAssignmentName.text.isBlank() || editGradePoints.text.isBlank()) return

        // Check due, unlock, and lock dates
        if (overrideContainer.children<AssignmentOverrideView>().any { it.validateInput() }) return

        val postData = AssignmentPostBody()
        postData.name = editAssignmentName.text.toString()
        postData.pointsPossible = editGradePoints.text.toString().toDouble()
        postData.setGroupedDueDates(mEditDateGroups)
        postData.description = descriptionEditor.html ?: mAssignment.description
        postData.notifyOfUpdate = false
        postData.gradingType = mDisplayGradeAs

        // TODO: remove this section when we support editing submission types
        // There is some weirdness with the API dealing with not graded stuff. When you change it from not graded you also
        // need to set the submission type to be something. When we implement submission type editing we won't need this here
        if(mAssignment.gradingType == NOT_GRADED_TYPE && mDisplayGradeAs != NOT_GRADED_TYPE) {
            val type = "none"
            val submissionList = listOf(type)
            postData.submissionTypes = submissionList
        } else {
            postData.submissionTypes = mAssignment.submissionAPITypes
        }

        // if we want to set the type as not graded, we don't want a submission type or points possible
        if(mDisplayGradeAs == NOT_GRADED_TYPE) {
            //set points to 0 if we aren't grading it
            postData.pointsPossible = null
            val type = NOT_GRADED_TYPE
            val submissionList = listOf(type)
            postData.submissionTypes = submissionList
        }

        // only set the published flag if we can unpublish/publish the assignment
        if (mAssignment.isUnpublishable) postData.published = mIsPublished
        else postData.published = mAssignment.isPublished

        postData.isMuted = mAssignment.isMuted

        @Suppress("EXPERIMENTAL_FEATURE_WARNING")
        mPutAssignmentCall = weave {
            try {
                saveButton?.setGone()
                savingProgressBar.announceForAccessibility(getString(R.string.saving))
                savingProgressBar.setVisible()
                mAssignment = awaitApi<Assignment> { AssignmentManager.editAssignment(mAssignment.courseId, mAssignment.id, postData, it, false) }
                AssignmentUpdatedEvent(mAssignment.id).post() // Post bus event
                toast(R.string.successfully_updated_assignment) // let the user know the assignment was saved
                editAssignmentName.hideKeyboard() // close the keyboard
                activity.onBackPressed() // close this fragment
            } catch (e: Throwable) {
                saveButton?.setVisible()
                savingProgressBar.setGone()
                toast(R.string.error_saving_assignment)
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onAssigneesChanged(event: AssigneesUpdatedEvent) {
        // Update grouped due dates (EditDateGroups)
        event.once(javaClass.simpleName) { dates ->
            mEditDateGroups = dates.toMutableList()
            setupOverrides()
            //remove it so when we go to another assignment it won't show up there too
            EventBus.getDefault().removeStickyEvent(event)
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putSerializable(EDIT_DATE_GROUPS, ArrayList<DueDateGroup>(mEditDateGroups))
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        mDueDateApiCalls?.cancel()
        mPutAssignmentCall?.cancel()
        mScrollHandler.removeCallbacks(mScrollToRunnable)
    }

    companion object {
        @JvmStatic val ASSIGNMENT = "assignment"
        @JvmStatic val SHOULD_SCROLL_TO_DATES = "shouldScrollToDates"

        @JvmStatic
        fun newInstance(course: Course, args: Bundle) = EditAssignmentDetailsFragment().apply {
            mCourse = course
            mAssignment = args.getParcelable(ASSIGNMENT)
            mScrollToDates = args.getBoolean(SHOULD_SCROLL_TO_DATES, false)
        }


        @JvmStatic
        fun makeBundle(assignment: Assignment, scrollToDates: Boolean): Bundle {
            val args = Bundle()
            args.putBoolean(EditAssignmentDetailsFragment.SHOULD_SCROLL_TO_DATES, scrollToDates)
            args.putParcelable(EditAssignmentDetailsFragment.ASSIGNMENT, assignment)
            return args
        }
    }
}
