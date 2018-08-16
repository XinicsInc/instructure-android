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
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.MenuItem
import android.view.View
import com.instructure.canvasapi2.models.Assignment
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.Course
import com.instructure.canvasapi2.models.GradeableStudentSubmission
import com.instructure.canvasapi2.utils.NumberHelper
import com.instructure.pandautils.fragments.BaseSyncFragment
import com.instructure.pandautils.utils.*
import com.instructure.teacher.R
import com.instructure.teacher.activities.SpeedGraderActivity
import com.instructure.teacher.adapters.GradeableStudentSubmissionAdapter
import com.instructure.teacher.dialog.FilterSubmissionByPointsDialog
import com.instructure.teacher.dialog.PeopleListFilterDialog
import com.instructure.teacher.dialog.RadioButtonDialog
import com.instructure.teacher.events.AssignmentGradedEvent
import com.instructure.teacher.events.SubmissionCommentsUpdated
import com.instructure.teacher.factory.AssignmentSubmissionListPresenterFactory
import com.instructure.teacher.holders.GradeableStudentSubmissionViewHolder
import com.instructure.teacher.presenters.AssignmentSubmissionListPresenter
import com.instructure.teacher.presenters.AssignmentSubmissionListPresenter.SubmissionListFilter
import com.instructure.interactions.router.Route
import com.instructure.interactions.router.RouteContext
import com.instructure.teacher.router.RouteMatcher
import com.instructure.teacher.utils.*
import com.instructure.teacher.viewinterface.AssignmentSubmissionListView
import instructure.androidblueprint.PresenterFactory
import kotlinx.android.synthetic.main.fragment_assignment_submission_list.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class AssignmentSubmissionListFragment : BaseSyncFragment<
        GradeableStudentSubmission,
        AssignmentSubmissionListPresenter,
        AssignmentSubmissionListView,
        GradeableStudentSubmissionViewHolder,
        GradeableStudentSubmissionAdapter>(), AssignmentSubmissionListView {

    private var mAssignment: Assignment by ParcelableArg(Assignment())
    private var mCourse: Course by ParcelableArg(Course())
    lateinit private var mRecyclerView: RecyclerView
    private val mCourseColor by lazy { ColorKeeper.getOrGenerateColor(mCourse) }
    private var mFilter by SerializableArg(SubmissionListFilter.ALL)
    private var mCanvasContextsSelected = ArrayList<CanvasContext>()

    private var mNeedToForceNetwork = false

    private val mSubmissionFilters: Map<Int, String> by lazy {
        sortedMapOf(
                Pair(SubmissionListFilter.ALL.ordinal, getString(R.string.all_submissions)),
                Pair(SubmissionListFilter.LATE.ordinal, getString(R.string.submitted_late)),
                Pair(SubmissionListFilter.MISSING.ordinal, getString(R.string.not_submitted)),
                Pair(SubmissionListFilter.NOT_GRADED.ordinal, getString(R.string.not_graded)),
                Pair(SubmissionListFilter.BELOW_VALUE.ordinal, getString(R.string.scored_less_than)),
                Pair(SubmissionListFilter.ABOVE_VALUE.ordinal, getString(R.string.scored_more_than))

        )
    }

    override fun layoutResId(): Int = R.layout.fragment_assignment_submission_list
    override fun getRecyclerView(): RecyclerView = submissionsRecyclerView
    override fun getList() = presenter.data
    override fun getPresenterFactory(): PresenterFactory<AssignmentSubmissionListPresenter> = AssignmentSubmissionListPresenterFactory(mAssignment, mFilter)
    override fun onCreateView(view: View?) = Unit
    override fun onPresenterPrepared(presenter: AssignmentSubmissionListPresenter) {
        mRecyclerView = RecyclerViewUtils.buildRecyclerView(mRootView, context, adapter, presenter, R.id.swipeRefreshLayout,
                R.id.submissionsRecyclerView, R.id.emptyPandaView, getString(R.string.no_items_to_display_short))
        mRecyclerView.setHeaderVisibilityListener(divider)

        mRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && addMessage.visibility == View.VISIBLE) {
                    addMessage.hide()
                } else if (dy < 0 && addMessage.visibility != View.VISIBLE) {
                    addMessage.show()
                }
            }
        })
    }

    override fun onReadySetGo(presenter: AssignmentSubmissionListPresenter) {
        if(mRecyclerView.adapter == null) {
            mRecyclerView.adapter = adapter
        }

        presenter.refresh(mNeedToForceNetwork)
        mNeedToForceNetwork = false

        updateFilterTitle()
        clearFilterTextView.setTextColor(ThemePrefs.buttonColor)
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun getAdapter(): GradeableStudentSubmissionAdapter {
        if(mAdapter == null) {
            mAdapter = GradeableStudentSubmissionAdapter(mAssignment, mCourse.id, context, presenter) { gradeableStudentSubmission ->
                doWithNetworkRequired(context) {
                    val filteredSubmissions = (0 until presenter.data.size()).map { presenter.data[it] }
                    val selectedIdx = filteredSubmissions.indexOf(gradeableStudentSubmission)
                    val bundle = SpeedGraderActivity.makeBundle(mCourse.id, mAssignment.id, filteredSubmissions, selectedIdx)
                    RouteMatcher.route(context, Route(bundle, RouteContext.SPEED_GRADER))
                }
            }
        }
        return mAdapter
    }

    override fun onRefreshStarted() {
        //this prevents two loading spinners from happening during pull to refresh
        if(!swipeRefreshLayout.isRefreshing) {
            emptyPandaView.visibility  = View.VISIBLE
        }
        emptyPandaView.setLoading()
    }

    override fun onRefreshFinished() {
        swipeRefreshLayout.isRefreshing = false
    }

    override fun checkIfEmpty() {
        // We don't want to leave the fab hidden if the list is empty
        if(presenter.isEmpty) {
            addMessage.show()
        }
        RecyclerViewUtils.checkIfEmpty(emptyPandaView, mRecyclerView, swipeRefreshLayout, adapter, presenter.isEmpty)
    }

    private fun setupToolbar() {
        //setup toolbar icon to access this menu
        assignmentSubmissionListToolbar.setupMenu(R.menu.menu_filter_submissions, menuItemCallback)
        assignmentSubmissionListToolbar.setupBackButtonAsBackPressedOnly(this)

        if(isTablet) {
            assignmentSubmissionListToolbar.title = mAssignment.name
        } else {
            assignmentSubmissionListToolbar.setNavigationIcon(R.drawable.vd_back_arrow)
            assignmentSubmissionListToolbar.title = getString(R.string.submissions)
            assignmentSubmissionListToolbar.subtitle = mCourse.name
        }
        ViewStyler.themeToolbar(activity, assignmentSubmissionListToolbar, mCourseColor, Color.WHITE)
        ViewStyler.themeFAB(addMessage, ThemePrefs.buttonColor)
    }

    private fun setupListeners() {
        clearFilterTextView.setOnClickListener {
            presenter.setFilter(SubmissionListFilter.ALL)
            presenter.clearFilterList()
            filterTitle.setText(R.string.all_submissions)
            clearFilterTextView.setGone()
        }

        addMessage.setOnClickListener {
            val args = AddMessageFragment.createBundle(presenter.getStudents(), filterTitle.text.toString() + " " + getString(R.string.on) + " " + mAssignment.name, mCourse.contextId, false)
            RouteMatcher.route(context, Route(AddMessageFragment::class.java, null, args))
        }
    }
    override fun onResume() {
        super.onResume()
        setupToolbar()
        setupListeners()
        updateStatuses()
    }

    private fun updateFilterTitle() {
        clearFilterTextView.setVisible()
        when (presenter?.getFilter()) {
            SubmissionListFilter.ALL -> {
                filterTitle.setText(R.string.all_submissions)
                clearFilterTextView.setGone()
            }
            SubmissionListFilter.LATE -> filterTitle.setText(R.string.submitted_late)
            SubmissionListFilter.MISSING -> filterTitle.setText(R.string.havent_submitted_yet)
            SubmissionListFilter.NOT_GRADED -> filterTitle.setText(R.string.havent_been_graded)
            SubmissionListFilter.GRADED -> filterTitle.setText(R.string.graded)
            SubmissionListFilter.BELOW_VALUE -> {
                filterTitle.text = activity.resources.getString(
                        R.string.scored_less_than_value,
                        NumberHelper.formatDecimal(presenter.getFilterPoints(), 2, true)
                )
            }
            SubmissionListFilter.ABOVE_VALUE -> {
                filterTitle.text = activity.resources.getString(
                        R.string.scored_more_than_value,
                        NumberHelper.formatDecimal(presenter.getFilterPoints(), 2, true)
                )
            }
        }

        filterTitle.text = filterTitle.text.toString().plus(presenter?.getSectionFilterText())
    }

    fun setFilter(filterIndex: Int) {
        when(filterIndex) {
            SubmissionListFilter.ALL.ordinal -> {
                presenter.setFilter(SubmissionListFilter.ALL)
                updateFilterTitle()
            }
            SubmissionListFilter.LATE.ordinal -> {
                presenter.setFilter(SubmissionListFilter.LATE)
                updateFilterTitle()
            }
            SubmissionListFilter.MISSING.ordinal -> {
                presenter.setFilter(SubmissionListFilter.MISSING)
                updateFilterTitle()
            }
            SubmissionListFilter.NOT_GRADED.ordinal -> {
                presenter.setFilter(SubmissionListFilter.NOT_GRADED)
                updateFilterTitle()
            }
            SubmissionListFilter.BELOW_VALUE.ordinal -> {
                FilterSubmissionByPointsDialog.getInstance(fragmentManager, getString(R.string.scored_less_than), mAssignment.pointsPossible, { points ->
                    presenter?.setFilter(SubmissionListFilter.BELOW_VALUE, points)
                    updateFilterTitle()
                }).show(activity.supportFragmentManager, FilterSubmissionByPointsDialog::class.java.simpleName)
            }
            SubmissionListFilter.ABOVE_VALUE.ordinal -> {
                FilterSubmissionByPointsDialog.getInstance(fragmentManager, getString(R.string.scored_more_than), mAssignment.pointsPossible, { points ->
                    presenter?.setFilter(SubmissionListFilter.ABOVE_VALUE, points)
                    updateFilterTitle()
                }).show(activity.supportFragmentManager, FilterSubmissionByPointsDialog::class.java.simpleName)
            }
        }
    }

    val menuItemCallback: (MenuItem) -> Unit = { item ->
        when (item.itemId) {
            R.id.menuMuteGrades -> withRequireNetwork { presenter.toggleMuted() }
            R.id.menuAnonGrading -> {
                val anonGradingPreference = TeacherPrefs.getGradeAssignmentAnonymously(mCourse.id, mAssignment.id)
                TeacherPrefs.setGradeAssignmentAnonymously(mCourse.id, mAssignment.id, !anonGradingPreference)
                presenter.loadData(false)
                updateStatuses()
            }
            R.id.filterSubmissions -> {
                val (keys, values) = mSubmissionFilters.toList().unzip()
                val dialog = RadioButtonDialog.getInstance(activity.supportFragmentManager, getString(R.string.filter_submissions), values as ArrayList<String>, keys.indexOf(presenter.getFilter().ordinal)) { idx ->
                    setFilter(keys[idx])
                }
                dialog.show(activity.supportFragmentManager, RadioButtonDialog::class.java.simpleName)

            }
            R.id.filterBySection -> {
                //let the user select the course/group they want to see
                PeopleListFilterDialog.getInstance(activity.supportFragmentManager, presenter.getSectionListIds(), mCourse, false) { canvasContexts ->
                    mCanvasContextsSelected = ArrayList()
                    mCanvasContextsSelected.addAll(canvasContexts)

                    presenter.setSections(canvasContexts)

                    filterTitle.text = filterTitle.text.toString().plus(presenter.getSectionFilterText())
                    clearFilterTextView.setVisible()
                }.show(activity.supportFragmentManager, PeopleListFilterDialog::class.java.simpleName)
            }
        }
    }

    fun updateStatuses() {
        val isMuted = presenter.mAssignment.isMuted
        assignmentSubmissionListToolbar.menu.findItem(R.id.menuMuteGrades)?.let {
            it.title = getString(if (isMuted) R.string.unmuteGrades else R.string.muteGrades)
        }

        val anonGrade = TeacherPrefs.shouldGradeAnonymously(mCourse.id, mAssignment.id)
        assignmentSubmissionListToolbar.menu.findItem(R.id.menuAnonGrading)?.let {
            it.title = getString(if (anonGrade) R.string.turnOffAnonymousGrading else R.string.turnOnAnonymousGrading)
        }

        val statuses = mutableListOf<String>()
        if (anonGrade) statuses += getString(R.string.anonymousGradingLabel)
        if (isMuted) statuses += getString(R.string.gradesMutedLabel)
        mutedStatusView.setVisible(statuses.isNotEmpty()).text = statuses.joinToString()
    }

    override fun onMuteUpdated(success: Boolean, isMuted: Boolean) {
        if (success) {
            mAssignment.isMuted = isMuted
            updateStatuses()
        } else {
            toast(R.string.error_occurred)
        }
    }

    /**
     * Called when a course or account has the feature flag to grade anonymously turned on
     */
    override fun removeAnonymousGradingOption(shouldRemove: Boolean) {
        if(shouldRemove) {
            assignmentSubmissionListToolbar.menu.removeItem(R.id.menuAnonGrading)
        } else {
            setupToolbar()
        }
        updateStatuses()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onAssignmentGraded(event: AssignmentGradedEvent) {
        event.once(javaClass.simpleName) {
            //force network call on resume
            if(presenter.mAssignment.id == it) mNeedToForceNetwork = true
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onSubmissionCommentUpdated(event: SubmissionCommentsUpdated) {
        event.once(AssignmentSubmissionListFragment::class.java.simpleName) {
            mNeedToForceNetwork = true
        }
    }

    companion object {
        @JvmStatic
        val ASSIGNMENT = "assignment"
        @JvmStatic val FILTER_TYPE = "filter_type"

        @JvmStatic
        fun newInstance(course: Course, args: Bundle) = AssignmentSubmissionListFragment().apply {
            mAssignment = args.getParcelable(ASSIGNMENT)
            if(args.containsKey(FILTER_TYPE)) {
                mFilter = args.getSerializable(FILTER_TYPE) as SubmissionListFilter
            } else {
                mFilter = SubmissionListFilter.ALL
            }
            mCourse = course
        }

        @JvmStatic
        fun makeBundle(assignment: Assignment): Bundle {
            return makeBundle(assignment, SubmissionListFilter.ALL)
        }

        @JvmStatic
        fun makeBundle(assignment: Assignment, filter: SubmissionListFilter): Bundle {
            val args = Bundle()
            args.putSerializable(FILTER_TYPE, filter)
            args.putParcelable(ASSIGNMENT, assignment)
            return args
        }
    }
}
