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
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.view.View
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.Quiz
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.pandautils.fragments.BaseExpandableSyncFragment
import com.instructure.pandautils.utils.ColorKeeper
import com.instructure.pandautils.utils.ViewStyler
import com.instructure.teacher.R
import com.instructure.teacher.adapters.QuizListAdapter
import com.instructure.teacher.events.QuizUpdatedEvent
import com.instructure.teacher.factory.QuizListPresenterFactory
import com.instructure.teacher.presenters.QuizListPresenter
import com.instructure.interactions.router.Route
import com.instructure.teacher.router.RouteMatcher
import com.instructure.pandautils.utils.ParcelableArg
import com.instructure.teacher.utils.RecyclerViewUtils
import com.instructure.teacher.utils.setupBackButton
import com.instructure.teacher.viewinterface.QuizListView
import instructure.androidblueprint.PresenterFactory
import kotlinx.android.synthetic.main.fragment_quiz_list.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class QuizListFragment : BaseExpandableSyncFragment<
        String,
        Quiz, QuizListView,
        QuizListPresenter,
        RecyclerView.ViewHolder,
        QuizListAdapter>(), QuizListView {

    private var mCanvasContext: CanvasContext by ParcelableArg(default = CanvasContext.getGenericContext(CanvasContext.Type.COURSE, -1L, ""))

    private var mLinearLayoutManager = LinearLayoutManager(context)
    lateinit private var mRecyclerView: RecyclerView
    private val mCourseColor by lazy { ColorKeeper.getOrGenerateColor(mCanvasContext) }

    private var mGradingPeriodMenu: PopupMenu? = null

    private var mNeedToForceNetwork = false

    override fun layoutResId(): Int = R.layout.fragment_quiz_list
    override fun getRecyclerView(): RecyclerView = quizRecyclerView
    override fun getPresenterFactory(): PresenterFactory<QuizListPresenter> = QuizListPresenterFactory(mCanvasContext)
    override fun onPresenterPrepared(presenter: QuizListPresenter?) {
        mRecyclerView = RecyclerViewUtils.buildRecyclerView(mRootView, context, adapter, presenter, R.id.swipeRefreshLayout,
                R.id.quizRecyclerView, R.id.emptyPandaView, getString(R.string.no_items_to_display_short))
    }

    override fun onCreateView(view: View) {
        mLinearLayoutManager.orientation = LinearLayoutManager.VERTICAL
    }

    override fun onReadySetGo(presenter: QuizListPresenter) {
        if(recyclerView.adapter == null) {
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

    override fun getAdapter(): QuizListAdapter {
        if (mAdapter == null) {
            mAdapter = QuizListAdapter(context, presenter, mCourseColor) { quiz ->
                val args = QuizDetailsFragment.makeBundle(quiz)
                RouteMatcher.route(context, Route(null, QuizDetailsFragment::class.java, mCanvasContext, args))
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
        RecyclerViewUtils.checkIfEmpty(emptyPandaView, mRecyclerView, swipeRefreshLayout, adapter, presenter.isEmpty)
    }

    override fun perPageCount() = ApiPrefs.perPageCount

    private fun setupToolbar() {
        quizListToolbar.title = getString(R.string.tab_quizzes)
        quizListToolbar.subtitle = mCanvasContext.name
        quizListToolbar.setupBackButton(this)

        ViewStyler.themeToolbar(activity, quizListToolbar, mCourseColor, Color.WHITE)
    }


    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onAssignmentEdited(event: QuizUpdatedEvent) {
        event.once(javaClass.simpleName) {
            // need to set a flag here. Because we use the event bus in the fragment instead of the presenter for unit testing purposes,
            // when we come back to this fragment it will go through the life cycle events again and the cached data will immediately
            // overwrite the data from the network if we refresh the presenter from here.
            mNeedToForceNetwork = true
        }
    }

    companion object {

        @JvmStatic
        fun newInstance(canvasContext: CanvasContext) = QuizListFragment().apply {
            mCanvasContext = canvasContext
        }
    }
}
