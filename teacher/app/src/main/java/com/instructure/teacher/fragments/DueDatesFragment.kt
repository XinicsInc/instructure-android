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
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.RecyclerView
import android.view.View
import com.instructure.canvasapi2.models.Assignment
import com.instructure.canvasapi2.models.Course
import com.instructure.canvasapi2.utils.APIHelper
import com.instructure.pandautils.fragments.BaseSyncFragment
import com.instructure.pandautils.utils.*
import com.instructure.teacher.R
import com.instructure.teacher.adapters.DueDatesAdapter
import com.instructure.teacher.dialog.NoInternetConnectionDialog
import com.instructure.teacher.factory.DueDatesPresenterFactory
import com.instructure.teacher.holders.DueDateViewHolder
import com.instructure.teacher.models.DueDateGroup
import com.instructure.teacher.presenters.DueDatesPresenter
import com.instructure.interactions.router.Route
import com.instructure.teacher.router.RouteMatcher
import com.instructure.teacher.utils.*
import com.instructure.teacher.view.EmptyPandaView
import com.instructure.teacher.viewinterface.DueDatesView
import kotlinx.android.synthetic.main.fragment_assignment_due_dates.*

class DueDatesFragment : BaseSyncFragment<DueDateGroup, DueDatesPresenter, DueDatesView, DueDateViewHolder, DueDatesAdapter>(), DueDatesView {

    var mAssignment: Assignment by ParcelableArg(Assignment())
    var mCourse: Course by ParcelableArg(Course())

    val dueDateRecyclerView by bind<RecyclerView>(R.id.recyclerView)
    val swipeRefreshLayout by bind<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
    val emptyPandaView by bind<EmptyPandaView>(R.id.emptyPandaView)

    override fun layoutResId() = R.layout.fragment_assignment_due_dates
    override fun getList() = presenter.data
    override fun getRecyclerView() = dueDateRecyclerView
    override fun withPagination() = false
    override fun getPresenterFactory() = DueDatesPresenterFactory(mAssignment)
    override fun onCreateView(view: View?) {}

    override fun onResume() {
        super.onResume()
        setupToolbar()
    }

    override fun hideMenu() {
        toolbar.menu.clear()
    }

    private fun setupToolbar() {
        toolbar.setupBackButtonAsBackPressedOnly(this)
        toolbar.title = getString(R.string.page_title_due_dates)
        if(!isTablet) {
            toolbar.subtitle = mCourse.name
        }
        ViewStyler.themeToolbar(activity, toolbar, mCourse.color, Color.WHITE)
    }

    override fun showMenu(assignment: Assignment) {
        toolbar.setupMenu(R.menu.menu_edit_generic) {
            if(APIHelper.hasNetworkConnection()) {
                if(assignment.submissionTypes.contains(Assignment.SUBMISSION_TYPE.ONLINE_QUIZ)) {
                    val args = EditQuizDetailsFragment.makeBundle(assignment.quizId)
                    RouteMatcher.route(context, Route(EditQuizDetailsFragment::class.java, mCourse, args))
                } else if(assignment.submissionTypes.contains(Assignment.SUBMISSION_TYPE.DISCUSSION_TOPIC)) {
                    val discussionTopicHeader = assignment.discussionTopicHeader

                    assignment.setDiscussionTopic(null)
                    discussionTopicHeader.assignment = assignment
                    val args =  CreateDiscussionFragment.makeBundle(mCourse, discussionTopicHeader, true)
                    RouteMatcher.route(context, Route(CreateDiscussionFragment::class.java, mCourse, args))
                } else {
                    val args = EditAssignmentDetailsFragment.makeBundle(assignment, true)
                    RouteMatcher.route(context, Route(EditAssignmentDetailsFragment::class.java, mCourse, args))
                }
            } else {
                NoInternetConnectionDialog.show(fragmentManager)
            }
        }
    }

    override fun onPresenterPrepared(presenter: DueDatesPresenter?) {
        RecyclerViewUtils.buildRecyclerView(mRootView, context, adapter, presenter, R.id.swipeRefreshLayout,
                R.id.recyclerView, R.id.emptyPandaView, getString(R.string.no_items_to_display_short))
        addSwipeToRefresh(swipeRefreshLayout)
    }

    override fun onReadySetGo(presenter: DueDatesPresenter) {
        if(mAdapter == null) {
            dueDateRecyclerView.adapter = adapter
        }
        presenter.loadData(false)
    }

    override fun onRefreshStarted() = emptyPandaView.setLoading()

    override fun onRefreshFinished() {
        swipeRefreshLayout.isRefreshing = false
    }

    override fun checkIfEmpty() {
        RecyclerViewUtils.checkIfEmpty(emptyPandaView, dueDateRecyclerView, swipeRefreshLayout, adapter, presenter.isEmpty)
    }

    override fun getAdapter(): DueDatesAdapter {
        if (mAdapter == null) {
            mAdapter = DueDatesAdapter(context, presenter)
        }
        return mAdapter
    }

    companion object {
        @JvmStatic val ASSIGNMENT = "assignment"

        @JvmStatic
        fun getInstance(course: Course, args: Bundle) = DueDatesFragment().apply {
            mAssignment = args.getParcelable(ASSIGNMENT)
            mCourse = course
        }

        @JvmStatic
        fun makeBundle(assignment: Assignment): Bundle {
            val args = Bundle()
            args.putParcelable(DueDatesFragment.ASSIGNMENT, assignment)
            return args
        }
    }
}
