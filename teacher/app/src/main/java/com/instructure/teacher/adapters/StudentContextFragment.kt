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
package com.instructure.teacher.adapters

import android.graphics.Color
import android.os.Bundle
import android.view.*
import com.instructure.canvasapi2.StudentContextCardQuery.*
import com.instructure.canvasapi2.models.BasicUser
import com.instructure.canvasapi2.models.GradeableStudentSubmission
import com.instructure.canvasapi2.models.StudentAssignee
import com.instructure.canvasapi2.type.EnrollmentType
import com.instructure.canvasapi2.utils.DateHelper
import com.instructure.canvasapi2.utils.isValid
import com.instructure.pandautils.utils.*
import com.instructure.teacher.R
import com.instructure.teacher.activities.SpeedGraderActivity
import com.instructure.teacher.factory.StudentContextPresenterFactory
import com.instructure.teacher.fragments.AddMessageFragment
import com.instructure.teacher.holders.StudentContextSubmissionView
import com.instructure.interactions.MasterDetailInteractions
import com.instructure.teacher.presenters.StudentContextPresenter
import com.instructure.interactions.router.Route
import com.instructure.interactions.router.RouteContext
import com.instructure.teacher.router.RouteMatcher
import com.instructure.teacher.utils.displayText
import com.instructure.teacher.utils.setupBackButton
import com.instructure.teacher.utils.setupBackButtonWithExpandCollapseAndBack
import com.instructure.teacher.utils.updateToolbarExpandCollapseIcon
import com.instructure.teacher.viewinterface.StudentContextView
import instructure.androidblueprint.PresenterFragment
import kotlinx.android.synthetic.main.fragment_student_context.*


class StudentContextFragment : PresenterFragment<StudentContextPresenter, StudentContextView>(), StudentContextView {

    private var mStudentId by LongArg()
    private var mCourseId by LongArg()
    private var mLaunchSubmissions by BooleanArg()

    private var mHasLoaded = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mHasLoaded = false
        return inflater.inflate(R.layout.fragment_student_context, container, false)
    }

    override fun getPresenterFactory() = StudentContextPresenterFactory(mStudentId, mCourseId)

    override fun onRefreshStarted() {
        toolbar.setGone()
        contentContainer.setGone()
        loadingView.setVisible()
        loadingView.announceForAccessibility(getString(R.string.Loading))
    }

    override fun onRefreshFinished() {
        loadingView.setGone()
        toolbar.setVisible()
        contentContainer.setVisible()
    }

    override fun onPresenterPrepared(presenter: StudentContextPresenter) {}

    override fun onReadySetGo(presenter: StudentContextPresenter) {
        if (!mHasLoaded) {
            presenter.loadData(false)
            mHasLoaded = true
        }
    }

    override fun setData(course: AsCourse, student: User, summary: Analytics?, isStudent: Boolean) {
        val courseColor = ColorKeeper.getOrGenerateColor("course_${course.id}")

        setupScrollListener()

        // Toolbar setup
        if (activity is MasterDetailInteractions) {
            toolbar.setupBackButtonWithExpandCollapseAndBack(this) {
                toolbar.updateToolbarExpandCollapseIcon(this)
                ViewStyler.themeToolbar(activity, toolbar, courseColor, Color.WHITE)
                (activity as MasterDetailInteractions).toggleExpandCollapse()
            }
        } else {
            toolbar.setupBackButton(this)
        }
        toolbar.title = student.name
        toolbar.subtitle = course.name
        ViewStyler.themeToolbar(activity, toolbar, courseColor, Color.WHITE)

        // Message FAB
        messageButton.setVisible()
        ViewStyler.themeFAB(messageButton, ThemePrefs.buttonColor)
        messageButton.setOnClickListener {
            val basicUser = BasicUser().apply {
                id = student.id.toLong()
                name = student.name
                avatarUrl = student.avatarUrl
            }
            val args = AddMessageFragment.createBundle(arrayListOf(basicUser), "", "course_${course.id}", true)
            RouteMatcher.route(context, Route(AddMessageFragment::class.java, null, args))
        }

        studentNameView.text = student.name
        studentEmailView.setVisible(student.email.isValid()).text = student.email

        // Avatar
        ProfileUtils.loadAvatarForUser(avatarView, student.name, student.avatarUrl)

        // Course and section names
        courseNameView.text = course.name
        sectionNameView.text = if (isStudent) {
            getString(R.string.sectionFormatted, student.enrollments.joinToString { it.section?.name ?: "" })
        } else {
            val enrollmentsString = student.enrollments.joinToString { "${it.section?.name} (${it.type.displayText})" }
            getString(R.string.sectionFormatted, enrollmentsString)
        }

        // Latest activity
        student.enrollments
            .filter { it.lastActivityAt != null }
            .sortedBy { it.lastActivityAt }
            .firstOrNull()?.lastActivityAt
            ?.let {
                val dateString = DateHelper.getFormattedDate(context, it)
                val timeString = DateHelper.getFormattedTime(context, it)
                lastActivityView.text = getString(R.string.latestStudentActivityAtFormatted, dateString, timeString)
            } ?: lastActivityView.setGone()

        if (isStudent) {
            // Grade
            gradeView.text = student.enrollments
                .find { it.type == EnrollmentType.StudentEnrollment }
                ?.grades?.let { it.currentGrade ?: it.currentScore?.toString() } ?: "-"

            // Missing
            missingCountView.text = summary?.tardinessBreakdown?.missing?.toInt()?.toString() ?: "-"

            // Late
            lateCountView.text = summary?.tardinessBreakdown?.late?.toInt()?.toString() ?: "-"
        } else {
            messageButton.setGone()
            val lastIdx = scrollContent.indexOfChild(additionalInfoContainer)
            scrollContent.children.forEachIndexed { idx, v -> if (idx > lastIdx) v.setGone() }
        }
    }

    private fun setupScrollListener() {
        contentContainer.viewTreeObserver.addOnScrollChangedListener(scrollListener)
    }

    private val scrollListener = object : ViewTreeObserver.OnScrollChangedListener {

        private var triggered = false
        private val touchSlop by lazy { ViewConfiguration.get(context).scaledTouchSlop }

        override fun onScrollChanged() {
            if (!isAdded || contentContainer.height == 0 || scrollContent.height == 0 || loadMoreContainer.height == 0) return
            val threshold = scrollContent.height - loadMoreContainer.top
            val bottomOffset = contentContainer.height + contentContainer.scrollY - scrollContent.bottom
            if (scrollContent.height <= contentContainer.height) {
                presenter?.loadMoreSubmissions()
            } else if (triggered && (threshold + touchSlop + bottomOffset < 0)) {
                triggered = false
            } else if (!triggered && (threshold + bottomOffset > 0)) {
                triggered = true
                presenter?.loadMoreSubmissions()
            }
        }

    }

    override fun addSubmissions(submissions: List<Submission>, course: AsCourse, student: User) {
        val courseColor = ColorKeeper.getOrGenerateColor("course_${course.id}")
        submissions.forEach { submission ->
            val view = StudentContextSubmissionView(context, submission, courseColor)
            if (mLaunchSubmissions) view.onClick {
                val user = com.instructure.canvasapi2.models.User().apply {
                    avatarUrl = student.avatarUrl
                    id = student.id.toLongOrNull() ?: 0
                    name = student.name
                    shortName = student.shortName
                    email = student.email
                }
                val studentSubmission = GradeableStudentSubmission(StudentAssignee(user), null)
                val bundle = SpeedGraderActivity.makeBundle(
                    course.id.toLongOrNull() ?: 0,
                    submission.assignment?.id?.toLongOrNull() ?: 0,
                    listOf(studentSubmission), 0)
                RouteMatcher.route(context, Route(bundle, RouteContext.SPEED_GRADER))
            }
            submissionListContainer.addView(view)
        }
        contentContainer.post { scrollListener.onScrollChanged() }
    }

    override fun showLoadMoreIndicator(show: Boolean) {
        loadMoreIndicator.setVisible(show)
    }

    override fun onErrorLoading(isDesigner: Boolean) {
        if (isDesigner) {
            toast(R.string.errorIsDesigner)
        } else {
            toast(R.string.errorLoadingStudentContextCard)
        }
        activity.onBackPressed()
    }

    companion object {
        @JvmStatic
        fun makeBundle(studentId: Long, courseId: Long, launchSubmissions: Boolean = false) = StudentContextFragment().apply {
            mStudentId = studentId
            mCourseId = courseId
            mLaunchSubmissions = launchSubmissions
        }.nonNullArgs

        @JvmStatic
        fun newInstance(bundle: Bundle) = StudentContextFragment().apply { arguments = bundle }
    }

}
