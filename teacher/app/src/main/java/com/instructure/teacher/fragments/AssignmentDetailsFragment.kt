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

import android.graphics.Color
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.instructure.canvasapi2.models.Assignment
import com.instructure.canvasapi2.models.Course
import com.instructure.canvasapi2.utils.*
import com.instructure.pandautils.fragments.BasePresenterFragment
import com.instructure.pandautils.utils.*
import com.instructure.pandautils.views.CanvasWebView
import com.instructure.teacher.R
import com.instructure.teacher.activities.InternalWebViewActivity
import com.instructure.teacher.dialog.NoInternetConnectionDialog
import com.instructure.teacher.events.AssignmentDeletedEvent
import com.instructure.teacher.events.AssignmentGradedEvent
import com.instructure.teacher.events.AssignmentUpdatedEvent
import com.instructure.teacher.events.post
import com.instructure.teacher.factory.AssignmentDetailPresenterFactory
import com.instructure.interactions.Identity
import com.instructure.interactions.MasterDetailInteractions
import com.instructure.teacher.presenters.AssignmentDetailsPresenter
import com.instructure.teacher.presenters.AssignmentSubmissionListPresenter
import com.instructure.interactions.router.Route
import com.instructure.teacher.router.RouteMatcher
import com.instructure.teacher.utils.*
import com.instructure.teacher.viewinterface.AssignmentDetailsView
import kotlinx.android.synthetic.main.fragment_assignment_details.*
import kotlinx.android.synthetic.main.view_submissions_donut_group.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

class AssignmentDetailsFragment : BasePresenterFragment<
        AssignmentDetailsPresenter,
        AssignmentDetailsView>(), AssignmentDetailsView, Identity {

    private var mAssignment: Assignment by ParcelableArg(Assignment())
    private var mCourse: Course by ParcelableArg(Course())
    private var mAssignmentId: Long by LongArg(default = 0L)

    private var mNeedToForceNetwork = false

    override fun layoutResId() = R.layout.fragment_assignment_details

    override fun onRefreshFinished() {}

    override fun onRefreshStarted() {
        toolbar.menu.clear()
        clearListeners()
    }

    override fun onReadySetGo(presenter: AssignmentDetailsPresenter) {
        // if we don't have an assignmentId that means we have an assignment, so we can load the data
        if(mAssignmentId == 0L) {
            presenter.loadData(mNeedToForceNetwork)
        } else {
            presenter.getAssignment(mAssignmentId, mCourse)
        }
    }

    override fun populateAssignmentDetails(assignment: Assignment) {
        mAssignment = assignment
        toolbar.setupMenu(R.menu.menu_edit_generic) { openEditPage(assignment) }
        swipeRefreshLayout.isRefreshing = false
        setupViews(assignment)
        setupListeners(assignment)
        ViewStyler.themeToolbar(activity, toolbar, mCourse.color, Color.WHITE)
    }

    override fun getPresenterFactory() = AssignmentDetailPresenterFactory(mAssignment)

    override fun onPresenterPrepared(presenter: AssignmentDetailsPresenter) {}

    private fun setupToolbar() {
        toolbar.setupBackButtonWithExpandCollapseAndBack(this) {
            toolbar.updateToolbarExpandCollapseIcon(this)
            ViewStyler.themeToolbar(activity, toolbar, mCourse.color, Color.WHITE)
            (activity as MasterDetailInteractions).toggleExpandCollapse()
        }

        toolbar.title = getString(R.string.assignment_details)
        if(!isTablet) {
            toolbar.subtitle = mCourse.name
        }
        ViewStyler.themeToolbar(activity, toolbar, mCourse.color, Color.WHITE)
    }

    private fun setupViews(assignment: Assignment) {
        swipeRefreshLayout.setOnRefreshListener {
            presenter.loadData(true)

            // Send out bus events to trigger a refresh for assignment list and submission list
            AssignmentGradedEvent(assignment.id, javaClass.simpleName).post()
            AssignmentUpdatedEvent(assignment.id, javaClass.simpleName).post()
        }

        availabilityLayout.setGone()
        availableFromLayout.setGone()
        availableToLayout.setGone()
        dueForLayout.setGone()
        dueDateLayout.setGone()
        otherDueDateTextView.setGone()

        // Assignment name
        assignmentNameTextView.text = assignment.name

        // See Configure Assignment Region
        configurePointsPossible(assignment)
        configurePublishStatus(assignment)
        configureLockStatus(assignment)
        configureDueDates(assignment)
        configureSubmissionTypes(assignment)
        configureDescription(assignment)
        configureSubmissionDonuts(assignment)
    }

    // region Configure Assignment
    private fun configurePointsPossible(assignment: Assignment) = with(assignment) {
        pointsTextView.text = resources.getQuantityString(
                R.plurals.quantityPointsAbbreviated,
                pointsPossible.toInt(),
                NumberHelper.formatDecimal(pointsPossible, 1, true)
        )
        pointsTextView.contentDescription = resources.getQuantityString(
                R.plurals.quantityPointsFull,
                pointsPossible.toInt(),
                NumberHelper.formatDecimal(pointsPossible, 1, true))
    }

    private fun configurePublishStatus(assignment: Assignment) = with(assignment) {
        if (isPublished) {
            publishStatusIconView.setImageResource(R.drawable.vd_published)
            publishStatusIconView.setColorFilter(context.getColorCompat(R.color.publishedGreen))
            publishStatusTextView.setText(R.string.published)
            publishStatusTextView.setTextColor(ContextCompat.getColor(context, R.color.published_green))
        } else {
            publishStatusIconView.setImageResource(R.drawable.vd_unpublished)
            publishStatusIconView.setColorFilter(context.getColorCompat(R.color.defaultTextGray))
            publishStatusTextView.setText(R.string.not_published)
            publishStatusTextView.setTextColor(ContextCompat.getColor(context, R.color.defaultTextGray))
        }
    }

    private fun configureLockStatus(assignment: Assignment) = assignment.allDates?.singleOrNull()?.apply {
        val atSeparator = getString(R.string.at)

        if (lockAt?.before(Date()) ?: false) {
            availabilityLayout.setVisible()
            availabilityTextView.setText(R.string.closed)
        } else {
            availableFromLayout.setVisible()
            availableToLayout.setVisible()
            availableFromTextView.text = if (unlockAt != null)
                DateHelper.getMonthDayAtTime(context, unlockAt, atSeparator) else getString(R.string.no_date_filler)
            availableToTextView.text = if (lockAt!= null)
                DateHelper.getMonthDayAtTime(context, lockAt, atSeparator) else getString(R.string.no_date_filler)
        }
    }

    private fun configureDueDates(assignment: Assignment) = with(assignment) {
        val atSeparator = getString(R.string.at)

        if (allDates.size > 1) {
            otherDueDateTextView.setVisible()
            otherDueDateTextView.setText(R.string.multiple_due_dates)
        } else {
            if (allDates.size == 0 || allDates[0].dueAt == null) {
                otherDueDateTextView.setVisible()
                otherDueDateTextView.setText(R.string.no_due_date)

                dueForLayout.setVisible()
                dueForTextView.text = if (allDates.size == 0 || allDates[0].isBase) getString(R.string.everyone) else allDates[0].title ?: ""

            } else with(allDates[0]) {
                dueDateLayout.setVisible()
                dueDateTextView.text = DateHelper.getMonthDayAtTime(context, dueAt, atSeparator)

                dueForLayout.setVisible()
                dueForTextView.text = if (isBase) getString(R.string.everyone) else title ?: ""
            }
        }
    }

    private fun configureSubmissionTypes(assignment: Assignment) = with(assignment) {
        if (submissionTypes.isEmpty()) {
            setSubmissionTypes(listOf(Assignment.SUBMISSION_TYPE.NONE.toString().toLowerCase(Locale.getDefault())))
        }
        submissionTypesTextView.text = submissionTypes.map { it.prettyPrint(context) }.joinToString("\n")

        if(submissionTypes.contains(Assignment.SUBMISSION_TYPE.EXTERNAL_TOOL)) {
            //External tool
            submissionTypesArrowIcon.setVisible()
            submissionTypesLayout.onClickWithRequireNetwork {
                // if the user is a designer we don't want to let them look at LTI tools
                if (mCourse.isDesigner) {
                    toast(R.string.errorIsDesigner)
                    return@onClickWithRequireNetwork
                }
                val ltiUrl = assignment.url.validOrNull() ?: assignment.htmlUrl
                if(!ltiUrl.isNullOrBlank()) {
                    val args = LTIWebViewFragment.makeLTIBundle(ltiUrl)
                    RouteMatcher.route(context, Route(LTIWebViewFragment::class.java, mCourse, args))
                }
            }
        }

        submissionsLayout.setVisible(!mCourse.isDesigner)
    }

    @Suppress("UsePropertyAccessSyntax")
    private fun configureDescription(assignment: Assignment): Unit = with(assignment) {
        // Show "No description" layout if there is no description
        if (assignment.description.isNullOrBlank()) {
            descriptionWebView.setGone()
            noDescriptionTextView.setVisible()
            return
        }

        // Show progress bar while loading description
        noDescriptionTextView.setGone()
        descriptionProgressBar.announceForAccessibility(getString(R.string.loading))
        descriptionProgressBar.setVisible()
        descriptionWebView.setWebChromeClient(object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                if (newProgress >= 100) {
                    descriptionProgressBar?.setGone()
                    descriptionWebView?.setVisible()
                }
            }
        })

        descriptionWebView.canvasWebViewClientCallback = object : CanvasWebView.CanvasWebViewClientCallback {
            override fun openMediaFromWebView(mime: String?, url: String?, filename: String?) {}
            override fun onPageStartedCallback(webView: WebView?, url: String?) {}
            override fun onPageFinishedCallback(webView: WebView?, url: String?) {}
            override fun routeInternallyCallback(url: String?) { RouteMatcher.canRouteInternally(activity, url!!, ApiPrefs.domain, true) }
            override fun canRouteInternallyDelegate(url: String?): Boolean = RouteMatcher.canRouteInternally(activity, url!!, ApiPrefs.domain, false)
        }

        descriptionWebView.canvasEmbeddedWebViewCallback = object : CanvasWebView.CanvasEmbeddedWebViewCallback {
            override fun launchInternalWebViewFragment(url: String) = activity.startActivity(InternalWebViewActivity.createIntent(activity, url, "", true))
            override fun shouldLaunchInternalWebViewFragment(url: String): Boolean = true
        }

        //make the WebView background transparent
        descriptionWebView.setBackgroundColor(0)
        descriptionWebView.setBackgroundResource(android.R.color.transparent)

        // Load description
        descriptionWebView.loadHtml(assignment.description, assignment.name)
    }

    private fun configureSubmissionDonuts(assignment: Assignment): Unit = with(assignment) {
        if(Assignment.getGradingTypeFromString(assignment.gradingType, context) == Assignment.GRADING_TYPE.NOT_GRADED) {
            // If the grading type is NOT_GRADED we don't want to show anything for the grading dials
            submissionsLayout.setGone()
            submissionsLayoutDivider.setGone()
        } else if(!isOnlineSubmissionType) {
            // Only show graded dial if the assignment submission type is not online
            notSubmittedWrapper.setGone()
            ungradedWrapper.setGone()
            assigneesWithoutGradesTextView.setVisible()
        }
    }
    //endregion

    override fun updateSubmissionDonuts(totalStudents: Int, gradedStudents: Int, needsGradingCount: Int, notSubmitted: Int) {
        // Submission section
        gradedChart.setSelected(gradedStudents)
        gradedChart.setTotal(totalStudents)
        gradedChart.setSelectedColor(ThemePrefs.brandColor)
        gradedChart.setCenterText(gradedStudents.toString())
        gradedWrapper.contentDescription = context.getString(R.string.content_description_submission_donut_graded).format(gradedStudents, totalStudents)
        gradedProgressBar.setGone()
        gradedChart.invalidate()

        ungradedChart.setSelected(needsGradingCount)
        ungradedChart.setTotal(totalStudents)
        ungradedChart.setSelectedColor(ThemePrefs.brandColor)
        ungradedChart.setCenterText(needsGradingCount.toString())
        ungradedLabel.text = context.resources.getQuantityText(R.plurals.needsGradingNoQuantity, needsGradingCount)
        ungradedWrapper.contentDescription = context.getString(R.string.content_description_submission_donut_needs_grading).format(needsGradingCount, totalStudents)
        ungradedProgressBar.setGone()
        ungradedChart.invalidate()

        notSubmittedChart.setSelected(notSubmitted)
        notSubmittedChart.setTotal(totalStudents)
        notSubmittedChart.setSelectedColor(ThemePrefs.brandColor)
        notSubmittedChart.setCenterText(notSubmitted.toString())

        notSubmittedWrapper.contentDescription = context.getString(R.string.content_description_submission_donut_unsubmitted).format(notSubmitted, totalStudents)
        notSubmittedProgressBar.setGone()
        notSubmittedChart.invalidate()


        // Only show graded dial if the assignment submission type is not online
        if (!presenter.mAssignment.isOnlineSubmissionType) {
            val totalCount = needsGradingCount + notSubmitted
            assigneesWithoutGradesTextView.text = context.resources.getQuantityString(R.plurals.assignees_without_grades, totalCount, totalCount)
        }
    }

    private fun clearListeners() {
        dueLayout.setOnClickListener {}
        submissionsLayout.setOnClickListener {}
        gradedWrapper.setOnClickListener {}
        ungradedWrapper.setOnClickListener {}
        notSubmittedWrapper.setOnClickListener {}
        noDescriptionTextView.setOnClickListener {}
        assigneesWithoutGradesTextView.setOnClickListener {}
    }

    private fun setupListeners(assignment: Assignment) {
        dueLayout.setOnClickListener {
            val args = DueDatesFragment.makeBundle(assignment)
            RouteMatcher.route(context, Route(null, DueDatesFragment::class.java, mCourse, args))
        }

        submissionsLayout.setOnClickListener {
            navigateToSubmissions(mCourse, assignment, AssignmentSubmissionListPresenter.SubmissionListFilter.ALL)
        }
        viewAllSubmissions.onClick { submissionsLayout.performClick() } // Separate click listener for a11y
        gradedWrapper.setOnClickListener {
            navigateToSubmissions(mCourse, assignment, AssignmentSubmissionListPresenter.SubmissionListFilter.GRADED)
        }
        ungradedWrapper.setOnClickListener {
            navigateToSubmissions(mCourse, assignment, AssignmentSubmissionListPresenter.SubmissionListFilter.NOT_GRADED)
        }
        notSubmittedWrapper.setOnClickListener {
            navigateToSubmissions(mCourse, assignment, AssignmentSubmissionListPresenter.SubmissionListFilter.MISSING)
        }
        noDescriptionTextView.setOnClickListener { openEditPage(assignment) }

        assigneesWithoutGradesTextView.setOnClickListener {
            submissionsLayout.performClick()
        }
    }

    private fun openEditPage(assignment: Assignment) {
        if(APIHelper.hasNetworkConnection()) {
            val args = EditAssignmentDetailsFragment.makeBundle(assignment, false)
            RouteMatcher.route(context, Route(EditAssignmentDetailsFragment::class.java, mCourse, args))
        } else {
            NoInternetConnectionDialog.show(fragmentManager)
        }
    }

    private fun navigateToSubmissions(course: Course, assignment: Assignment, filter: AssignmentSubmissionListPresenter.SubmissionListFilter) {
        val args = AssignmentSubmissionListFragment.makeBundle(assignment, filter)
        RouteMatcher.route(context, Route(null, AssignmentSubmissionListFragment::class.java, course, args))
    }

    override fun onResume() {
        super.onResume()
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

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onAssignmentEdited(event: AssignmentUpdatedEvent) {
        event.once(javaClass.simpleName) {
            if (it == presenter.mAssignment.id) mNeedToForceNetwork = true
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onAssignmentDeleted(event: AssignmentDeletedEvent) {
        event.once(javaClass.simpleName) {
            if (it == presenter.mAssignment.id) activity?.onBackPressed()
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onAssignmentGraded(event: AssignmentGradedEvent) {
        event.once(javaClass.simpleName) {
            if(presenter.mAssignment.id == it) mNeedToForceNetwork = true
        }
    }

    //Because of the presenter lifecycle using the assignment from there will result in random crashes.
    override val identity: Long? get() = if(mAssignmentId != 0L) mAssignmentId else mAssignment.id
    override val skipCheck: Boolean get() = false

    companion object {
        @JvmStatic val ASSIGNMENT = "assignment"
        @JvmStatic val ASSIGNMENT_ID = "assignmentId"

        @JvmStatic
        fun newInstance(course: Course, args: Bundle) = AssignmentDetailsFragment().apply {
            if(args.containsKey(ASSIGNMENT)) {
                mAssignment = args.getParcelable(ASSIGNMENT)
            }
            if(args.containsKey(ASSIGNMENT_ID)) {
                mAssignmentId = args.getLong(ASSIGNMENT_ID)
            }
            mCourse = course
        }

        @JvmStatic
        fun makeBundle(assignment: Assignment): Bundle {
            val args = Bundle()
            args.putParcelable(AssignmentDetailsFragment.ASSIGNMENT, assignment)
            return args
        }

        @JvmStatic
        fun makeBundle(assignmentId: Long): Bundle {
            val args = Bundle()
            args.putLong(AssignmentDetailsFragment.ASSIGNMENT_ID, assignmentId)
            return args
        }
    }
}
