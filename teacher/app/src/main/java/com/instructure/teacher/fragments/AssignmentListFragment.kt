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
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import com.instructure.canvasapi2.models.Assignment
import com.instructure.canvasapi2.models.AssignmentGroup
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.GradingPeriod
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.pandautils.fragments.BaseExpandableSyncFragment
import com.instructure.pandautils.utils.ParcelableArg
import com.instructure.pandautils.utils.ColorKeeper
import com.instructure.pandautils.utils.ThemePrefs
import com.instructure.pandautils.utils.ViewStyler
import com.instructure.teacher.BuildConfig
import com.instructure.teacher.R
import com.instructure.teacher.adapters.AssignmentAdapter
import com.instructure.teacher.events.AssignmentUpdatedEvent
import com.instructure.teacher.factory.AssignmentListPresenterFactory
import com.instructure.teacher.presenters.AssignmentListPresenter
import com.instructure.interactions.router.Route
import com.instructure.teacher.router.RouteMatcher
import com.instructure.teacher.utils.*
import com.instructure.teacher.viewinterface.AssignmentListView
import instructure.androidblueprint.PresenterFactory
import kotlinx.android.synthetic.main.fragment_assignment_list.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class AssignmentListFragment : BaseExpandableSyncFragment<
        AssignmentGroup,
        Assignment, AssignmentListView,
        AssignmentListPresenter,
        RecyclerView.ViewHolder,
        AssignmentAdapter>(), AssignmentListView {

    private var mCanvasContext: CanvasContext by ParcelableArg(default = CanvasContext.getGenericContext(CanvasContext.Type.COURSE, -1L, ""))
    private var mPairedWithSubmissions: Boolean = false
    private var mLinearLayoutManager = LinearLayoutManager(context)
    lateinit private var mRecyclerView: RecyclerView
    private val mCourseColor by lazy { ColorKeeper.getOrGenerateColor(mCanvasContext) }

    private var mGradingPeriodMenu: PopupMenu? = null
    private var mNeedToForceNetwork = false

    override fun layoutResId(): Int = R.layout.fragment_assignment_list
    override fun getRecyclerView(): RecyclerView = assignmentRecyclerView
    override fun getPresenterFactory(): PresenterFactory<AssignmentListPresenter> = AssignmentListPresenterFactory(mCanvasContext)
    override fun onPresenterPrepared(presenter: AssignmentListPresenter?) {
        mRecyclerView = RecyclerViewUtils.buildRecyclerView(mRootView, context, adapter, presenter, R.id.swipeRefreshLayout,
                R.id.assignmentRecyclerView, R.id.emptyPandaView, getString(R.string.no_items_to_display_short))
        mRecyclerView.setHeaderVisibilityListener(divider)
    }

    override fun onCreateView(view: View) {
        mLinearLayoutManager.orientation = LinearLayoutManager.VERTICAL
    }

    override fun onReadySetGo(presenter: AssignmentListPresenter) {
        if (recyclerView.adapter == null) {
            mRecyclerView.adapter = adapter
        }
        presenter.loadData(mNeedToForceNetwork)
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

    override fun onPause() {
        if(mGradingPeriodMenu != null) {
            mGradingPeriodMenu?.dismiss()
        }
        super.onPause()
    }

    override fun getAdapter(): AssignmentAdapter {
        if (mAdapter == null) {
            mAdapter = AssignmentAdapter(context, presenter, mCourseColor,
                    { assignment ->
                        if(mPairedWithSubmissions) {
                            val args = AssignmentSubmissionListFragment.makeBundle(assignment)
                            RouteMatcher.route(context, Route(null, AssignmentSubmissionListFragment::class.java, mCanvasContext, args))
                        } else {
                            if(assignment.submissionTypes.contains(Assignment.SUBMISSION_TYPE.ONLINE_QUIZ)) {
                                val args = QuizDetailsFragment.makeBundle(assignment.quizId)
                                RouteMatcher.route(context, Route(null, QuizDetailsFragment::class.java, mCanvasContext, args))
                            } else if(BuildConfig.POINT_FIVE && assignment.submissionTypes.contains(Assignment.SUBMISSION_TYPE.DISCUSSION_TOPIC) && assignment.discussionTopicHeader != null) {
                                val discussionTopicHeader = assignment.discussionTopicHeader

                                assignment.setDiscussionTopic(null)
                                discussionTopicHeader.assignment = assignment
                                val args = DiscussionsDetailsFragment.makeBundle(discussionTopicHeader)
                                RouteMatcher.route(context, Route(null, DiscussionsDetailsFragment::class.java, mCanvasContext, args))
                            } else {
                                val args = AssignmentDetailsFragment.makeBundle(assignment)
                                RouteMatcher.route(context, Route(null, AssignmentDetailsFragment::class.java, mCanvasContext, args))
                            }
                        }
                    })
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
        RecyclerViewUtils.checkIfEmpty(emptyPandaView, mRecyclerView, swipeRefreshLayout, adapter, presenter.isEmpty)
    }

    override fun getDefaultGradingPeriodTitle() = getString(R.string.all_grading_periods)

    override fun setGradingPeriods(gradingPeriods: List<GradingPeriod>) {
        // Do nothing if there are no grading periods
        if(gradingPeriods.isEmpty()) {
            return
        }

        //setup toolbar icon to access this menu
        assignmentListToolbar.setupMenu(R.menu.menu_assignment_list, menuItemCallback)
        ViewStyler.colorToolbarIconsAndText(activity, assignmentListToolbar, Color.WHITE)

        //setup popup menu
        val menuItemView = mRootView.findViewById<View>(R.id.menu_grading_periods_filter)
        mGradingPeriodMenu = PopupMenu(context, menuItemView, Gravity.TOP, 0,
                android.support.v7.appcompat.R.style.Base_Widget_AppCompat_PopupMenu_Overflow)

        mGradingPeriodMenu?.setOnMenuItemClickListener { menuItem ->
            presenter.selectGradingPeriodIndex(menuItem.itemId)
            true
        }

        //add grading periods
        gradingPeriods.forEachIndexed { i, gradingPeriod ->
            mGradingPeriodMenu?.menu?.add(0, i, 0, gradingPeriod.title)
        }

        //set the grading periods to the selection
        val selectedGradingPeriod = presenter.getGradingPeriods().find { it.id == presenter.getSelectedGradingPeriodId() }
        val isFilterVisible = selectedGradingPeriod?.title!! != getDefaultGradingPeriodTitle()
        configureGradingPeriodState(selectedGradingPeriod.title ?: getDefaultGradingPeriodTitle(), true, isFilterVisible)
    }

    override fun perPageCount() = ApiPrefs.perPageCount

    private fun setupToolbar() {
        assignmentListToolbar.title = getString(R.string.assignments)
        assignmentListToolbar.subtitle = mCanvasContext.name
        assignmentListToolbar.setupBackButton(this)

        ViewStyler.themeToolbar(activity, assignmentListToolbar, mCourseColor, Color.WHITE)
    }

    override fun adjustGradingPeriodHeader(gradingPeriod: String, isVisible: Boolean, isFilterVisible: Boolean) {
        configureGradingPeriodState(gradingPeriod, isVisible, isFilterVisible)
    }

    private fun configureGradingPeriodState(gradingPeriod: String, isVisible: Boolean, isFilterVisible: Boolean) {
        if(isVisible) {
            gradingPeriodContainer.visibility = View.VISIBLE

            gradingPeriodTitle.text = gradingPeriod

            if(isFilterVisible) {
                clearGradingPeriodText.visibility = View.VISIBLE
                clearGradingPeriodText.setOnClickListener { presenter.clearGradingPeriodFilter() }
            } else {
                clearGradingPeriodText.visibility = View.INVISIBLE
            }

            clearGradingPeriodText.setTextColor(ThemePrefs.buttonColor)

        } else {
            gradingPeriodContainer.visibility = View.GONE
        }
    }

    val menuItemCallback: (MenuItem) -> Unit = { item ->
        when (item.itemId) {
            R.id.menu_grading_periods_filter -> {
                mGradingPeriodMenu?.show()
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onAssignmentEdited(event: AssignmentUpdatedEvent) {
        event.once(javaClass.simpleName) {
            // need to set a flag here. Because we use the event bus in the fragment instead of the presenter for unit testing purposes,
            // when we come back to this fragment it will go through the life cycle events again and the cached data will immediately
            // overwrite the data from the network if we refresh the presenter from here.
            mNeedToForceNetwork = true
        }
    }

    companion object {
        @JvmStatic val PAIRED_WITH_SUBMISSIONS = "pairedWithSubmissions"

        @JvmStatic
        fun getInstance(canvasContext: CanvasContext, args: Bundle) = AssignmentListFragment().apply {
            mPairedWithSubmissions = args.getBoolean(PAIRED_WITH_SUBMISSIONS, false)
            mCanvasContext = canvasContext
        }
    }
}
