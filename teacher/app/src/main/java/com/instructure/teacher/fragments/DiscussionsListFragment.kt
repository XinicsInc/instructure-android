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
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.DiscussionTopicHeader
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.pandautils.fragments.BaseExpandableSyncFragment
import com.instructure.pandautils.utils.*
import com.instructure.teacher.R
import com.instructure.teacher.adapters.DiscussionListAdapter
import com.instructure.teacher.dialog.DiscussionsMoveToDialog
import com.instructure.teacher.events.*
import com.instructure.teacher.factory.DiscussionListPresenterFactory
import com.instructure.teacher.presenters.DiscussionListPresenter
import com.instructure.interactions.router.Route
import com.instructure.teacher.router.RouteMatcher
import com.instructure.teacher.utils.RecyclerViewUtils
import com.instructure.teacher.utils.setupBackButton
import com.instructure.teacher.viewinterface.DiscussionListView
import instructure.androidblueprint.PresenterFactory
import kotlinx.android.synthetic.main.fragment_discussion_list.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

open class DiscussionsListFragment : BaseExpandableSyncFragment<
        String,
        DiscussionTopicHeader,
        DiscussionListView,
        DiscussionListPresenter,
        RecyclerView.ViewHolder,
        DiscussionListAdapter>(), DiscussionListView {

    protected var mCanvasContext: CanvasContext by ParcelableArg(default = CanvasContext.getGenericContext(CanvasContext.Type.COURSE, -1L, ""))

    private var mLinearLayoutManager = LinearLayoutManager(context)
    lateinit private var mRecyclerView: RecyclerView
    private val mCourseColor by lazy { ColorKeeper.getOrGenerateColor(mCanvasContext) }

    private var mNeedToForceNetwork = false
    private var mForceRefresh = false
    protected var mIsAnnouncements by BooleanArg()

    override fun layoutResId(): Int = R.layout.fragment_discussion_list
    override fun getRecyclerView(): RecyclerView = discussionRecyclerView

    override fun getPresenterFactory(): PresenterFactory<DiscussionListPresenter>  = DiscussionListPresenterFactory(mCanvasContext, mIsAnnouncements)

    override fun onPresenterPrepared(presenter: DiscussionListPresenter?) {
        mRecyclerView = RecyclerViewUtils.buildRecyclerView(mRootView, context, adapter, presenter, R.id.swipeRefreshLayout,
                R.id.discussionRecyclerView, R.id.emptyPandaView, getString(R.string.no_items_to_display_short))

        mRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && createNewDiscussion.visibility == View.VISIBLE) {
                    createNewDiscussion.hide()
                } else if (dy < 0 && createNewDiscussion.visibility != View.VISIBLE) {
                    createNewDiscussion.show()
                }
            }
        })

        setupViews()
    }

    override fun onCreateView(view: View?) {
        mLinearLayoutManager.orientation = LinearLayoutManager.VERTICAL
    }

    override fun onReadySetGo(presenter: DiscussionListPresenter) {
        if(mAdapter == null) {
            mRecyclerView.adapter = adapter
        }
        if (mForceRefresh) {
            presenter.refresh(true)
            mForceRefresh = false
        } else {
            presenter.loadData(mNeedToForceNetwork)
            mNeedToForceNetwork = false
        }
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

    override fun getAdapter(): DiscussionListAdapter {
        if(mAdapter == null) {
            mAdapter = DiscussionListAdapter(context, presenter, mCourseColor, mIsAnnouncements,
                    { discussionTopicHeader ->
                        val args = DiscussionsDetailsFragment.makeBundle(discussionTopicHeader, mIsAnnouncements)
                        RouteMatcher.route(context, Route(null, DiscussionsDetailsFragment::class.java, mCanvasContext, args))
                    },
                    { group, discussionTopicHeaderOverflow ->
                        if(group != null) {
                            DiscussionsMoveToDialog.show(fragmentManager, group, discussionTopicHeaderOverflow, { newGroup ->
                                presenter?.requestMoveDiscussionTopicToGroup(newGroup, group, discussionTopicHeaderOverflow)
                            })
                        }
                    })
        }
        return mAdapter
    }

    override fun perPageCount() = ApiPrefs.perPageCount

    override fun onRefreshStarted() {
        //this prevents two loading spinners from happening during pull to refresh
        if(!swipeRefreshLayout.isRefreshing) {
            emptyPandaView.setVisible()
        }
        emptyPandaView.setLoading()
        createNewDiscussion.setGone()
    }

    override fun onRefreshFinished() {
        emptyPandaView.setGone()
        swipeRefreshLayout.isRefreshing = false

        EventBus.getDefault().getStickyEvent(DiscussionTopicHeaderEvent::class.java)?.get {
            //After we load the data we look to see if anything is out of date and update it. Only used on phones typically.
            adapter.addOrUpdateItem(it)
        }

        //Fix for when loading groups and pinned items don't show up in the first api call.
        recyclerView.scrollToPosition(0)
        createNewDiscussion.setVisible()
    }

    override fun checkIfEmpty() {
        // We don't want to leave the fab hidden if the list is empty
        if(presenter.isEmpty) {
            createNewDiscussion.show()
        }
        RecyclerViewUtils.checkIfEmpty(emptyPandaView, mRecyclerView, swipeRefreshLayout, adapter, presenter.isEmpty)
    }

    private fun setupToolbar() {
        discussionListToolbar.title = if(mIsAnnouncements) getString(R.string.tab_announcements) else getString(R.string.tab_discussions)
        discussionListToolbar.subtitle = mCanvasContext.name
        discussionListToolbar.setupBackButton(this)

        ViewStyler.themeToolbar(activity, discussionListToolbar, mCourseColor, Color.WHITE)
    }

    private fun setupViews() {
        createNewDiscussion.setGone()
        createNewDiscussion.backgroundTintList = ViewStyler.makeColorStateListForButton()
        createNewDiscussion.setImageDrawable(ColorUtils.colorIt(ThemePrefs.buttonTextColor, createNewDiscussion.drawable))
        createNewDiscussion.onClickWithRequireNetwork {
            if(mIsAnnouncements) {
                val args = CreateOrEditAnnouncementFragment.newInstanceCreate(mCanvasContext).arguments
                RouteMatcher.route(context, Route(CreateOrEditAnnouncementFragment::class.java, null, args))
            } else {
                val args = CreateDiscussionFragment.makeBundle(mCanvasContext)
                RouteMatcher.route(context, Route(CreateDiscussionFragment::class.java, null, args))
            }
        }
    }

    override fun askToDeleteDiscussionTopicHeader(discussionTopicHeader: DiscussionTopicHeader) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.discussions_delete_title)
        builder.setMessage(R.string.discussions_delete_message)
        builder.setPositiveButton(R.string.delete, { _, _ ->
            presenter?.deleteDiscussionTopicHeader(discussionTopicHeader)
        })
        builder.setNegativeButton(R.string.cancel, { _, _ -> })
        builder.showThemed()
    }

    override fun moveToGroup(group: String, discussionTopicHeader: DiscussionTopicHeader) {
        adapter.addOrUpdateItem(group, discussionTopicHeader)
    }

    override fun discussionDeletedSuccessfully(discussionTopicHeader: DiscussionTopicHeader) {
        adapter.removeItem(discussionTopicHeader, false)
        DiscussionTopicHeaderDeletedEvent(discussionTopicHeader.id, (DiscussionsDetailsFragment::class.java.toString() + ".onPost()")).post()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onDiscussionCreated(event: DiscussionCreatedEvent) {
        event.once(javaClass.simpleName) {
            // need to set a flag here. Because we use the event bus in the fragment instead of the presenter for unit testing purposes,
            // when we come back to this fragment it will go through the life cycle events again and the cached data will immediately
            // overwrite the data from the network if we refresh the presenter from here.
            mNeedToForceNetwork = true
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onDiscussionTopicCountChange(event: DiscussionTopicHeaderEvent) {
        event.get {
            //Gets written over on phones - added also to {@link #onRefreshFinished()}
            adapter.addOrUpdateItem(it)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onDiscussionUpdated(event: DiscussionUpdatedEvent) {
        event.once(javaClass.simpleName) {
            // need to set a flag here. Because we use the event bus in the fragment instead of the presenter for unit testing purposes,
            // when we come back to this fragment it will go through the life cycle events again and the cached data will immediately
            // overwrite the data from the network if we refresh the presenter from here.
            mNeedToForceNetwork = true
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onDiscussionTopicHeaderDeleted(event: DiscussionTopicHeaderDeletedEvent) {
        event.get {
            val discussionTopicHeader = adapter.getItem(it)
            if(discussionTopicHeader != null) {
                adapter.removeItem(discussionTopicHeader, false)
                mNeedToForceNetwork = true
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(canvasContext: CanvasContext) = DiscussionsListFragment().apply {
            mCanvasContext = canvasContext
        }
    }
}
