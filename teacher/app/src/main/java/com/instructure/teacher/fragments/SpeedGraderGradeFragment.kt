/*
 * Copyright (C) 2017 - present  Instructure, Inc.
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
 */
package com.instructure.teacher.fragments

import android.support.v4.content.ContextCompat
import com.instructure.canvasapi2.models.*
import com.instructure.canvasapi2.utils.NumberHelper
import com.instructure.pandautils.fragments.BasePresenterFragment
import com.instructure.pandautils.utils.*
import com.instructure.teacher.R
import com.instructure.teacher.dialog.CustomizeGradeDialog
import com.instructure.teacher.dialog.PassFailGradeDailog
import com.instructure.teacher.factory.SpeedGraderGradePresenterFactory
import com.instructure.teacher.presenters.SpeedGraderGradePresenter
import com.instructure.teacher.utils.TeacherPrefs
import com.instructure.teacher.utils.getGradeText
import com.instructure.teacher.view.QuizSubmissionGradedEvent
import com.instructure.teacher.viewinterface.SpeedGraderGradeView
import kotlinx.android.synthetic.main.fragment_speedgrader_grade.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class SpeedGraderGradeFragment : BasePresenterFragment<SpeedGraderGradePresenter, SpeedGraderGradeView>(), SpeedGraderGradeView {

    private var mSubmission: Submission? by NullableParcelableArg(default = Submission())
    private var mAssignment: Assignment by ParcelableArg(default = Assignment())
    private var mAssignee: Assignee by ParcelableArg(default = StudentAssignee(User()))
    private var mCourse: Course by ParcelableArg(default = Course())

    override fun layoutResId() = R.layout.fragment_speedgrader_grade
    override fun getPresenterFactory() = SpeedGraderGradePresenterFactory(mSubmission, mAssignment, mCourse, mAssignee)
    override fun onReadySetGo(presenter: SpeedGraderGradePresenter) {}

    override fun onPresenterPrepared(presenter: SpeedGraderGradePresenter) {
        setupViews()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onQuizSaved(event: QuizSubmissionGradedEvent) {
        event.once("GradeFragment|${mCourse.id}|${mAssignment.id}|${mSubmission?.id}|${mAssignee.id}") {
            if (it.id == mSubmission?.id) {
                presenter.submission = it
                setupViews()
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(submission: Submission?, assignment: Assignment, course: Course, assignee: Assignee) = SpeedGraderGradeFragment().apply {
            mSubmission = submission
            mAssignment = assignment
            mCourse = course
            mAssignee = assignee
        }
    }

    override fun updateGradeText() {
        val grade = presenter.assignment.getGradeText(presenter.submission, context)
        //toggle visibility and set text
        if(grade == "null" || grade == "") {
            gradeValueText.setVisible(false)
            addGradeIcon.setVisible(true)
            gradeValueText.text = ""
        } else {
            gradeValueText.setVisible(true)
            addGradeIcon.setVisible(false)

            var currentGrade = grade
            // check to see if this submission has a late penalty
            if(presenter.submission?.pointsDeducted != null && presenter.submission?.pointsDeducted as Double > 0.0) {
                // make the late policy information visible
                finalGradeValueText.setVisible(true)
                latePenaltyValue.setVisible(true)
                latePolicy.setVisible(true)
                finalGradeContainer.setVisible(true)

                latePenalty.setTextColor(ThemePrefs.brandColor)
                latePenaltyValue.setTextColor(ThemePrefs.brandColor)

                finalGradeValueText.text = grade
                latePenaltyValue.text = context.resources.getQuantityString(R.plurals.latePolicyPenalty, if(presenter.submission?.pointsDeducted as Double == 1.0) 1 else 2, NumberHelper.formatDecimal(presenter.submission?.pointsDeducted as Double, 2, true))
                latePenaltyValue.contentDescription = context.resources.getQuantityString(R.plurals.latePolicyPenaltyFull, if(presenter.submission?.pointsDeducted as Double == 1.0) 1 else 2, NumberHelper.formatDecimal(presenter.submission?.pointsDeducted as Double, 2, true))

                // change the currentGrade variable to the entered grade because the actual grade on the submission applies the late penalty
                currentGrade = presenter.assignment.getGradeText(presenter.submission, context, true, true)
            }
            gradeValueText.text = currentGrade

            // if the submission is late, we want to show the entered score here
            var score = if (presenter.submission?.pointsDeducted == null) presenter.submission?.score as Double else presenter.submission?.enteredScore as Double
            gradeValueText.contentDescription = when (grade.contains("/")) {
                true -> context.getString(R.string.grade_value_format_talk_back, NumberHelper.formatDecimal(score, 2, true), NumberHelper.formatDecimal(presenter.assignment.pointsPossible, 2, true))
                false -> grade
            }

            finalGradeValueText.contentDescription = when (finalGradeValueText.text.contains("/")) {
                true -> context.getString(R.string.grade_value_format_talk_back, NumberHelper.formatDecimal(presenter.submission?.score as Double, 2, true), NumberHelper.formatDecimal(presenter.assignment.pointsPossible, 2, true))
                false -> grade
            }

            if(grade == getString(R.string.not_graded) || presenter.submission?.hasRealSubmission() ?: false) {
                //we need to set the text to be gray
                gradeValueText.setTextColor(ContextCompat.getColor(context, R.color.defaultTextGray))
            }
        }
    }

    override fun updateGradeError() {
        toast(R.string.error_occurred)
    }

    private fun setupViews() {

        /* Mobile doesn't support moderated grading yet. If moderated grading is enabled
        for this assignment, hide all grading options and display a message to the user */
        if (presenter.assignment.isModeratedGrading) {
            gradeContainer.setGone()
            rubricEditView.setGone()
            moderatedGradingMessage.setVisible()
            return
        }

        updateGradeText()

        if (presenter.assignment.isUseRubricForGrading) {
            gradeSubtext.setVisible(true)
            gradeSubtext.text = getString(R.string.calculated_by_rubric)
        } else {
            gradeSubtext.setVisible(false)
        }

        gradeTextContainer.onClickWithRequireNetwork {
            // Scores for submitted quizzes must be edited in the WebView, so we disallow editing here
            if (presenter.assignment.quizId > 0 && presenter.assignment.getGradeText(presenter.submission, context).isNotBlank()) {
                return@onClickWithRequireNetwork
            }

            //launch grading dialog; no grading if it's not a graded type
            if (gradeValueText.text != getString(R.string.not_graded)) {
                if (Assignment.getGradingTypeFromAPIString(presenter.assignment.gradingType) == Assignment.GRADING_TYPE.PASS_FAIL) {
                    showPassFailGradeDialog()
                } else {
                    showCustomizeGradeDialog()
                }
            }
        }

        rubricEditView.setData(presenter.assignment, presenter.submission, presenter.assignee, TeacherPrefs.shouldGradeAnonymously(mCourse.id, mAssignment.id))
        rubricEditView.onAssessmentSaved = { presenter.updateSubmission(it) }
    }

    private fun showCustomizeGradeDialog() {
        val pointsPossible: String = NumberHelper.formatDecimal(presenter.assignment.pointsPossible, 2, true)
        var grade: String? = ""
        if(presenter.submission != null) {
            grade = presenter.submission?.grade
        }

        val dialog = CustomizeGradeDialog.getInstance(activity.supportFragmentManager,
                pointsPossible, grade, presenter.assignment.gradingType, presenter.assignee is GroupAssignee) { grade, isExcused ->
            presenter.updateGrade(grade, isExcused)
        }

        dialog.show(activity.supportFragmentManager, CustomizeGradeDialog::class.java.simpleName)
    }

    private fun showPassFailGradeDialog() {
        val dialog = PassFailGradeDailog.getInstance(activity.supportFragmentManager, presenter.submission?.grade) { grade, isExcused ->
            presenter.updateGrade(grade, isExcused)
        }
        dialog.show(activity.supportFragmentManager, PassFailGradeDailog::class.java.simpleName)
    }

    override fun onRefreshStarted() {
        gradeValueText.setGone()
        addGradeIcon.setGone()
        gradeProgressSpinner.announceForAccessibility(getString(R.string.loading))
        gradeProgressSpinner.setVisible()
    }

    val  hasUnsavedChanges: Boolean get() = rubricEditView?.hasUnsavedChanges ?: false

    override fun onRefreshFinished() {
        gradeProgressSpinner.setGone()
    }

}
