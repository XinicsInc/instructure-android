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

import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.LinearLayout
import com.instructure.canvasapi2.models.Assignment
import com.instructure.canvasapi2.models.Course
import com.instructure.canvasapi2.models.GradeableStudentSubmission
import com.instructure.canvasapi2.models.ToDo
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.pandarecycler.util.UpdatableSortedList
import com.instructure.pandautils.fragments.BaseSyncFragment
import com.instructure.pandautils.utils.requestAccessibilityFocus
import com.instructure.pandautils.utils.toast
import com.instructure.teacher.R
import com.instructure.teacher.activities.InitActivity
import com.instructure.teacher.activities.SpeedGraderActivity
import com.instructure.teacher.adapters.ToDoAdapter
import com.instructure.teacher.events.AssignmentGradedEvent
import com.instructure.teacher.factory.ToDoPresenterFactory
import com.instructure.teacher.holders.ToDoViewHolder
import com.instructure.teacher.interfaces.AdapterToFragmentCallback
import com.instructure.teacher.presenters.ToDoPresenter
import com.instructure.interactions.router.Route
import com.instructure.interactions.router.RouteContext
import com.instructure.teacher.router.RouteMatcher
import com.instructure.teacher.utils.RecyclerViewUtils
import com.instructure.teacher.viewinterface.ToDoView
import instructure.androidblueprint.PresenterFactory
import kotlinx.android.synthetic.main.fragment_todo.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class ToDoFragment : BaseSyncFragment<ToDo, ToDoPresenter, ToDoView, ToDoViewHolder, ToDoAdapter>(), ToDoView {


    lateinit private var mRecyclerView: RecyclerView
    private var mNeedToForceNetwork = false

    override fun layoutResId(): Int = R.layout.fragment_todo
    override fun getList(): UpdatableSortedList<ToDo> = presenter.data
    override fun withPagination() = true
    override fun getRecyclerView(): RecyclerView = toDoRecyclerView
    override fun checkIfEmpty() {

        RecyclerViewUtils.checkIfEmpty(emptyPandaView, mRecyclerView, swipeRefreshLayout, adapter, presenter.isEmpty)
    }
    override fun getPresenterFactory(): PresenterFactory<ToDoPresenter> = ToDoPresenterFactory()
    override fun onCreateView(view: View?) {}


    override fun onPresenterPrepared(presenter: ToDoPresenter) {
        mRecyclerView = RecyclerViewUtils.buildRecyclerView(mRootView, context, adapter,
                presenter, R.id.swipeRefreshLayout, R.id.toDoRecyclerView, R.id.emptyPandaView, getString(R.string.toDoEmpty))
        emptyPandaView.setEmptyViewImage(ContextCompat.getDrawable(context, R.drawable.vd_to_do_empty))
        emptyPandaView.emptyViewImage.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        addSwipeToRefresh(swipeRefreshLayout)
        addPagination()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    //pagination
    override fun hitRockBottom() {
        presenter.nextPage()
    }

    override fun onReadySetGo(presenter: ToDoPresenter) {
        if(recyclerView.adapter == null) {
            recyclerView.adapter = adapter
        }

        presenter.refresh(mNeedToForceNetwork)
        mNeedToForceNetwork = false

        setupToolbar()
    }


    private fun setupToolbar() {
        (activity as? InitActivity)?.attachNavigationDrawer(toDoToolbar)
        toDoToolbar.requestAccessibilityFocus()
    }


    public override fun getAdapter(): ToDoAdapter {
        if (mAdapter == null) {
            mAdapter = ToDoAdapter(activity, presenter, mAdapterCallback)
        }
        return mAdapter
    }

    private val mAdapterCallback = AdapterToFragmentCallback<ToDo> { toDo, _ ->
        // if the layout is refreshing we don't want them to select a different item
        if (swipeRefreshLayout.isRefreshing) return@AdapterToFragmentCallback

        if (toDo.assignment == null) {
            toast(R.string.errorOccurred)
            return@AdapterToFragmentCallback
        }
        presenter.goToUngradedSubmissions(toDo.assignment, toDo.canvasContext.id)
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

    override fun perPageCount(): Int = ApiPrefs.perPageCount

    override fun onRouteSuccessfully(course: Course, assignment: Assignment, submissions: List<GradeableStudentSubmission>) {
        if (submissions.isEmpty()) {
            showToast(R.string.toDoNoSubmissions)
            return
        }
        val bundle = SpeedGraderActivity.makeBundle(course.id, assignment.id, submissions, 0)
        RouteMatcher.route(context, Route(bundle, RouteContext.SPEED_GRADER))
    }

    override fun onRouteFailed() {
        toast(R.string.errorToDoRouteFailed)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onAssignmentGraded(event: AssignmentGradedEvent) {
        event.once(javaClass.simpleName) {
            // force network call on resume
            mNeedToForceNetwork = true
        }
    }

    companion object {
        fun newInstance() = ToDoFragment()
    }
}
