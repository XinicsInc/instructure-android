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
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ScrollView
import android.widget.TextView
import com.instructure.canvasapi2.models.Assignment
import com.instructure.canvasapi2.models.Course
import com.instructure.canvasapi2.models.Quiz
import com.instructure.canvasapi2.models.post_models.AssignmentPostBody
import com.instructure.canvasapi2.models.post_models.QuizPostBody
import com.instructure.pandautils.dialogs.DatePickerDialogFragment
import com.instructure.pandautils.dialogs.TimePickerDialogFragment
import com.instructure.pandautils.fragments.BasePresenterFragment
import com.instructure.pandautils.utils.*
import com.instructure.teacher.R
import com.instructure.teacher.dialog.ConfirmRemoveAssignmentOverrideDialog
import com.instructure.teacher.events.AssigneesUpdatedEvent
import com.instructure.teacher.factory.EditQuizDetailsPresenterFactory
import com.instructure.interactions.Identity
import com.instructure.teacher.models.DueDateGroup
import com.instructure.teacher.presenters.EditQuizDetailsPresenter
import com.instructure.interactions.router.Route
import com.instructure.teacher.router.RouteMatcher
import com.instructure.teacher.utils.*
import com.instructure.teacher.view.AssignmentOverrideView
import com.instructure.teacher.viewinterface.EditQuizDetailsView
import instructure.androidblueprint.PresenterFactory
import kotlinx.android.synthetic.main.fragment_edit_quiz_details.*
import kotlinx.android.synthetic.main.view_assignment_override.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

class EditQuizDetailsFragment : BasePresenterFragment<
        EditQuizDetailsPresenter,
        EditQuizDetailsView>(), EditQuizDetailsView, Identity {

    private var mCourse: Course by ParcelableArg(Course())

    private var mQuizId: Long by LongArg(0L)
    private var mQuiz: Quiz by ParcelableArg(Quiz())
    private var mAssignment: Assignment by ParcelableArg(Assignment().apply { id = -1L })

    private var mNeedToForceNetwork = false

    private var mIsPublished = true
    private var mHasAccessCode = false
    private var mQuizType: String? = null
    private var mScrollToDates = false

    private val saveButton: TextView? get() = view?.findViewById<TextView>(R.id.menu_save)

    // Keeps track of which override we were editing so we can scroll back to it when the user returns from editing assignees
    private var scrollBackToOverride: AssignmentOverrideView? = null

    var mScrollHandler: Handler = Handler()

    var mScrollToRunnable: Runnable = Runnable {
        if (isAdded) scrollView.fullScroll(ScrollView.FOCUS_DOWN)
    }

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
            if (presenter.mEditDateGroups.contains(callback)) presenter.mEditDateGroups.remove(callback)
            setupOverrides()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    override fun layoutResId() = R.layout.fragment_edit_quiz_details

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide Keyboard
        activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        setupToolbar()
    }

    override fun populateQuizDetails() {
        setupViews()
    }

    override fun onReadySetGo(presenter: EditQuizDetailsPresenter) {
        if (mQuizId == 0L) {
            // We have a Quiz object, possibly no assignment object though - Check it
            if (mQuiz.assignmentId == 0L) {
                // This quiz does not have an assignment (non-graded) Load like normal
                presenter.loadData(mNeedToForceNetwork)
            } else if (presenter.mAssignment.id == -1L) {
                // Else this quiz has an assignment and we don't have the assignment
                presenter.getAssignment(mQuiz.assignmentId, mCourse.id)
            } else {
                //we have all that we need, make sure we set up the views. This is needed when we come back to this fragment from assignees
                setupViews()
            }
        } else {
            presenter.getQuiz(mQuizId, mCourse.id)
        }
    }

    override fun getPresenterFactory(): PresenterFactory<EditQuizDetailsPresenter> = EditQuizDetailsPresenterFactory(mQuiz, mAssignment, mCourse)

    override fun onPresenterPrepared(presenter: EditQuizDetailsPresenter?) {}
    override fun onRefreshFinished() {}
    override fun onRefreshStarted() {}

    override fun scrollCheck() {
        if (mScrollToDates) {
            mScrollToDates = false
            // We came from the Dates page, scroll to the dates for editing
            mScrollHandler.postDelayed(mScrollToRunnable, 300)
        } else {
            scrollBackToOverride?.let {
                scrollView.post {
                    scrollView.fullScroll(ScrollView.FOCUS_DOWN)
                }
            }

            scrollBackToOverride = null
        }
    }

    private fun setupToolbar() {
        toolbar.setupCloseButton(this)
        toolbar.title = getString(R.string.editQuiz)
        toolbar.setupMenu(R.menu.menu_save_generic) { saveQuiz() }
        ViewStyler.themeToolbarBottomSheet(activity, isTablet, toolbar, Color.BLACK, false)
        ViewStyler.setToolbarElevationSmall(context, toolbar)
        saveButton?.setTextColor(ThemePrefs.buttonColor)
    }

    private fun setupPublishSwitch() = with(presenter.mQuiz) {
        //if a student has submitted something, we can't let the teacher unpublish the quiz
        if (!isUnpublishable) {
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

    private fun setupAccessCodeSwitch() = with(mQuiz) {
        accessCodeSwitch.isChecked = isHasAccessCode
        mHasAccessCode = isHasAccessCode
        accessCodeTextInput.setVisible(isHasAccessCode)
        ViewStyler.themeEditText(context, editAccessCode, ThemePrefs.brandColor)

        if (accessCode != null) {
            editAccessCode.setText(accessCode)
        }

        accessCodeSwitch.applyTheme()

        accessCodeSwitch.setOnCheckedChangeListener { _, isChecked ->
            mHasAccessCode = isChecked
            accessCodeTextInput.setVisible(isChecked)
        }
    }

    private fun setupQuizTypeSpinner() {
        // Filters spinner
        val filtersAdapter = ArrayAdapter.createFromResource(context, R.array.quizTypes, android.R.layout.simple_spinner_item)
        filtersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        quizTypeFilter.adapter = filtersAdapter
        quizTypeFilter.onItemSelectedListener = null

        when(mQuizType) {
            Quiz.TYPE_ASSIGNMENT -> quizTypeFilter.setSelection(filtersAdapter.getPosition(getString(R.string.gradedQuiz)))
            Quiz.TYPE_PRACTICE -> quizTypeFilter.setSelection(filtersAdapter.getPosition(getString(R.string.practiceQuiz)))
            Quiz.TYPE_SURVEY -> quizTypeFilter.setSelection(filtersAdapter.getPosition(getString(R.string.ungradedSurvey)))
            Quiz.TYPE_GRADED_SURVEY-> quizTypeFilter.setSelection(filtersAdapter.getPosition(getString(R.string.gradedSurvey)))
        }

        quizTypeFilter.onItemSelectedListener = (object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if(view == null) return
                val mPrevQuizType = mQuizType
                when((view as TextView).text.toString()) {
                    getString(R.string.gradedSurvey) -> mQuizType = Quiz.TYPE_GRADED_SURVEY
                    getString(R.string.practiceQuiz) -> mQuizType = Quiz.TYPE_PRACTICE
                    getString(R.string.ungradedSurvey) -> mQuizType = Quiz.TYPE_SURVEY
                    getString(R.string.gradedQuiz) -> mQuizType = Quiz.TYPE_ASSIGNMENT
                }

                if (mPrevQuizType != mQuizType)
                    updateOverridesForQuizType(true)
            }
            override fun onNothingSelected(parent: AdapterView<*>) = Unit
        })
    }

    /**
     * We can only support overrides for quizzes with an assignment.
     * If the quiz does not have an assignment, hide the override stuff.
     */
    private fun updateOverridesForQuizType(setupOverrides: Boolean = false) {
        when (mQuizType) {
            Quiz.TYPE_GRADED_SURVEY, Quiz.TYPE_ASSIGNMENT -> {
                overrideContainer.setVisible()
                addOverride.setVisible()
            }
            Quiz.TYPE_SURVEY, Quiz.TYPE_PRACTICE -> {
                overrideContainer.setGone()
                addOverride.setGone()
            }
        }
        if (setupOverrides) setupOverrides()
    }

    @Suppress("EXPERIMENTAL_FEATURE_WARNING")
    private fun setupViews() = with(presenter.mQuiz) {
        (view as? ViewGroup)?.descendants<TextInputLayout>()?.forEach {
            it.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL))
        }

        // Quiz name
        editQuizTitle.setText(title)
        // set the underline to be the brand color
        ViewStyler.themeEditText(context, editQuizTitle, ThemePrefs.brandColor)

        editQuizTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(editedString: Editable) {
                if (editedString.isBlank()) {
                    quizTitleTextInput.error = getString(R.string.assignment_name_must_be_set)
                } else {
                    quizTitleTextInput.isErrorEnabled = false
                }
            }
        })

        editAccessCode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(editedString: Editable) {
                if (editedString.isBlank()) {
                    accessCodeTextInput.error = getString(R.string.mustHaveAccessCode)
                } else {
                    accessCodeTextInput.isErrorEnabled = false
                }
            }
        })

        if(mQuizType == null) {
            mQuizType = quizType
        }
        updateOverridesForQuizType()

        setupPublishSwitch()
        setupAccessCodeSwitch()
        setupQuizTypeSpinner()

        ViewStyler.themeInputTextLayout(quizTitleTextInput, ContextCompat.getColor(context, R.color.defaultTextGray))
        ViewStyler.setToolbarElevation(context, toolbar, R.dimen.toolbar_elevation_small)

        // Description
        setupDescription(this)

        with(presenter) {if (mEditDateGroups.isEmpty()) mEditDateGroups.addAll(mAssignment.groupedDueDates)}
        presenter.getStudentsGroupsAndSections()

        // Theme add button and plus image
        addOverrideText.setTextColor(ThemePrefs.buttonColor)
        plus.setColorFilter(ThemePrefs.buttonColor)

        addOverride.setOnClickListener {
            presenter.mEditDateGroups.add(DueDateGroup())
            setupOverrides()
            scrollView.post {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN)
            }
            // This opens the assignees page to save the user a click.
            overrideContainer.descendants<AssignmentOverrideView>().last().assignTo.performClick()
        }
    }

    private fun setupDescription(quiz: Quiz) {
        // Show progress bar while loading description
        descriptionProgressBar.announceForAccessibility(getString(R.string.loading))
        descriptionProgressBar.setVisible()

        // Load description
        descriptionWebView.setHtml(quiz.description,
                getString(R.string.quizDescriptionContentDescription),
                getString(R.string.rce_empty_description),
                ThemePrefs.brandColor, ThemePrefs.buttonColor)

        // when the RCE editor has focus we want the label to be darker so it matches the title's functionality
        descriptionWebView.setLabel(quizDescLabel, R.color.defaultTextDark, R.color.defaultTextGray)
    }

    override fun setupOverrides() {
        overrideContainer.removeAllViews()
        // Load in overrides
        with(presenter) {
            mEditDateGroups.forEachIndexed { index, dueDateGroup ->
                val assignees = ArrayList<String>()
                val v = AssignmentOverrideView(activity)
                if (dueDateGroup.isEveryone) {
                    // && (dueDateGroup.coreDates.dueDate != null || dueDateGroup.coreDates.lockDate != null || dueDateGroup.coreDates.unlockDate != null)
                    assignees += getString(if (mEditDateGroups.any { it.hasOverrideAssignees }) R.string.everyone_else else R.string.everyone)
                }
                dueDateGroup.groupIds.forEach { assignees.add(groupsMapped[it]?.name ?: "") }
                dueDateGroup.sectionIds.forEach { assignees.add(sectionsMapped[it]?.name ?: "") }
                dueDateGroup.studentIds.forEach { (assignees.add(studentsMapped[it]?.name ?: "")) }

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
        }

        overrideContainer.descendants<TextInputLayout>().forEach {
            it.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL))
        }
    }

    private fun saveQuiz() {
        // Title is required
        if (editQuizTitle.text.isBlank()) return

        // Check due, unlock, and lock dates
        if (overrideContainer.children<AssignmentOverrideView>().any { it.validateInput() }) return


        // If a quiz requires an access code it has to have an access code entered
        if (mHasAccessCode && editAccessCode.text.isBlank()) {
            // setting the text blank will trigger the error
            editAccessCode.setText("")
            return
        }

        val quizPostData = assembleQuizPostData()
        val assignmentPostData = assembleAssignmentPostData()
        presenter.saveQuiz(quizPostData, assignmentPostData)
    }

    fun assembleQuizPostData(): QuizPostBody = QuizPostBody().apply {
        title = editQuizTitle.text.toString()
        description = descriptionWebView.html ?: presenter.mQuiz.description
        notifyOfUpdate = false
        if (mHasAccessCode) {
            accessCode = editAccessCode.text.toString()
        } else {
            accessCode = null
        }
        quizType = mQuizType

        // only set the published flag if we can unpublish/publish the assignment
        if (mQuiz.isUnpublishable) published = mIsPublished
        else published = presenter.mQuiz.isPublished
    }

    /**
     * Assembles the data we need to update the quiz's assignment overrides
     */
    fun assembleAssignmentPostData(): AssignmentPostBody {
        return AssignmentPostBody().apply {
            setGroupedDueDates(presenter.mEditDateGroups)
            notifyOfUpdate = false

            // only set the published flag if we can unpublish/publish the assignment
            if (presenter.mAssignment.isUnpublishable) published = mIsPublished
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mScrollHandler.removeCallbacks(mScrollToRunnable)
    }

    override fun startSavingQuiz() {
        saveButton?.setGone()
        savingProgressBar.announceForAccessibility(getString(R.string.saving))
        savingProgressBar.setVisible()
    }

    override fun quizSavedSuccessfully() {
        toast(R.string.successfully_updated_quiz) // let the user know the quiz was saved
        editQuizTitle.hideKeyboard() // close the keyboard
        activity.onBackPressed() // close this fragment
    }

    override fun errorSavingQuiz() {
        saveButton?.setVisible()
        savingProgressBar.setGone()
        toast(R.string.error_saving_quiz)
    }

    override val identity: Long? get() = if(mQuizId != 0L) mQuizId else mQuiz.id
    override val skipCheck: Boolean get() = false

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onAssigneesChanged(event: AssigneesUpdatedEvent) {
        // Update grouped due dates (EditDateGroups)
        event.once(javaClass.simpleName) { dates ->
            presenter.mEditDateGroups = dates.toMutableList()
            setupOverrides()
            //remove it so when we go to another assignment it won't show up there too
            EventBus.getDefault().removeStickyEvent(event)
        }
    }

    companion object {
        @JvmStatic val QUIZ = "quiz"
        @JvmStatic val QUIZ_ID = "quiz_id"
        @JvmStatic val ASSIGNMENT = "assignment"
        @JvmStatic val SHOULD_SCROLL_TO_DATES = "shouldScrollToDates"

        @JvmStatic
        fun newInstance(course: Course, args: Bundle) = EditQuizDetailsFragment().apply {
            if (args.containsKey(QUIZ)) {
                mQuiz = args.getParcelable(QUIZ)
            }

            if (args.containsKey(ASSIGNMENT)) {
                mAssignment = args.getParcelable(ASSIGNMENT)
            }

            if (args.containsKey(SHOULD_SCROLL_TO_DATES)) {
                mScrollToDates = args.getBoolean(SHOULD_SCROLL_TO_DATES, false)
            }

            if(args.containsKey(QUIZ_ID)) {
                mQuizId = args.getLong(QUIZ_ID)
            }
            mCourse = course
        }

        @JvmStatic
        fun makeBundle(quiz: Quiz, scrollToDates: Boolean): Bundle {
            val args = Bundle()
            args.putBoolean(EditQuizDetailsFragment.SHOULD_SCROLL_TO_DATES, scrollToDates)
            args.putParcelable(EditQuizDetailsFragment.QUIZ, quiz)
            return args
        }

        @JvmStatic
        fun makeBundle(quiz: Quiz): Bundle {
            return Bundle().apply {
                putParcelable(QUIZ, quiz)
            }
        }

        @JvmStatic
        fun makeBundle(quizId: Long): Bundle {
            return Bundle().apply {
                putLong(QUIZ_ID, quizId)
                putBoolean(EditQuizDetailsFragment.SHOULD_SCROLL_TO_DATES, true)
            }
        }
    }
}
