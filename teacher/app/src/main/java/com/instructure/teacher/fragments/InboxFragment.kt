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

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.view.MenuItem
import android.view.View
import com.instructure.canvasapi2.apis.InboxApi
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.Conversation
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.canvasapi2.utils.parcelCopy
import com.instructure.pandarecycler.util.UpdatableSortedList
import com.instructure.pandautils.fragments.BaseSyncFragment
import com.instructure.pandautils.utils.*
import com.instructure.teacher.R
import com.instructure.teacher.activities.InitActivity
import com.instructure.teacher.adapters.InboxAdapter
import com.instructure.teacher.dialog.CanvasContextListDialog
import com.instructure.teacher.events.ConversationDeletedEvent
import com.instructure.teacher.events.ConversationUpdatedEvent
import com.instructure.teacher.events.ConversationUpdatedEventTablet
import com.instructure.teacher.factory.InboxPresenterFactory
import com.instructure.teacher.holders.InboxViewHolder
import com.instructure.teacher.interfaces.AdapterToFragmentCallback
import com.instructure.teacher.presenters.InboxPresenter
import com.instructure.interactions.router.Route
import com.instructure.teacher.router.RouteMatcher
import com.instructure.teacher.utils.RecyclerViewUtils
import com.instructure.teacher.utils.setupMenu
import com.instructure.teacher.viewinterface.InboxView
import instructure.androidblueprint.PresenterFactory
import kotlinx.android.synthetic.main.fragment_inbox.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class InboxFragment : BaseSyncFragment<Conversation, InboxPresenter, InboxView, InboxViewHolder, InboxAdapter>(), InboxView {

    private val CANVAS_CONTEXT = "canvas_context"
    private var mCanvasContextSelected: CanvasContext? = null
    lateinit private var mRecyclerView: RecyclerView

    override fun layoutResId(): Int = R.layout.fragment_inbox
    override fun getList(): UpdatableSortedList<Conversation> = presenter.data
    override fun withPagination() = true
    override fun getRecyclerView(): RecyclerView = inboxRecyclerView
    override fun checkIfEmpty() {
        // We don't want to leave the fab hidden if the list is empty
        if(presenter.isEmpty) {
            addMessage.show()
        }
        RecyclerViewUtils.checkIfEmpty(emptyPandaView, mRecyclerView, swipeRefreshLayout, adapter, presenter.isEmpty)
    }
    override fun getPresenterFactory(): PresenterFactory<InboxPresenter> = InboxPresenterFactory()
    override fun onCreateView(view: View?) {}

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putParcelable(CANVAS_CONTEXT, mCanvasContextSelected)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(savedInstanceState != null) {
            mCanvasContextSelected = savedInstanceState.getParcelable(CANVAS_CONTEXT)
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onPresenterPrepared(presenter: InboxPresenter) {
        mRecyclerView = RecyclerViewUtils.buildRecyclerView(mRootView, context, adapter,
                presenter, R.id.swipeRefreshLayout, R.id.inboxRecyclerView, R.id.emptyPandaView, getString(R.string.inbox_empty_title))
        emptyPandaView.setEmptyViewImage(ContextCompat.getDrawable(context, R.drawable.vd_mail_empty))
        emptyPandaView.setMessageText(R.string.inbox_empty_message)

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

        addSwipeToRefresh(swipeRefreshLayout)
        addPagination()
    }

    //pagination
    override fun hitRockBottom() {
        presenter.nextPage()
    }

    override fun onReadySetGo(presenter: InboxPresenter) {
        if(recyclerView.adapter == null) {
            recyclerView.adapter = adapter
        }
        
        presenter.canvasContext = mCanvasContextSelected
        setFilterText()
        presenter.loadData(false)
        setupFilter(presenter)
        setupToolbar()
        setupListeners()
        setupConversationEvents()
    }

    private fun setupConversationEvents() {
        //phone specific event for updates (archives/read/unread/stars)
        val event = EventBus.getDefault().getStickyEvent(ConversationUpdatedEvent::class.java)
        event?.once(javaClass.simpleName) {
            if(presenter.scope == event.scope && presenter.scope != InboxApi.Scope.UNREAD)
            //for removed stars and archives, we need to update the list completely
                presenter.refresh(true)
            else
                presenter.data.addOrUpdate(it)
        }

        //phone specific event for deletion
        EventBus.getDefault().getStickyEvent(ConversationDeletedEvent::class.java)?.once(javaClass.simpleName + ".onResume()") {
            presenter.data.removeItemAt(it)
        }
    }

    private fun setupToolbar() {
        toolbar.setupMenu(R.menu.menu_filter_inbox, menuItemCallback)
        (activity as? InitActivity)?.attachNavigationDrawer(toolbar)
        ViewStyler.themeFAB(addMessage, ThemePrefs.buttonColor)
        toolbar.requestAccessibilityFocus()
    }

    private fun setupListeners() {
        addMessage.setOnClickListener {
            val args = AddMessageFragment.createBundle()
            RouteMatcher.route(context, Route(AddMessageFragment::class.java, null, args))
        }

        clearFilterTextView.setOnClickListener {
            presenter.canvasContext = null
            mCanvasContextSelected = null
            courseFilter.setText(R.string.all_courses)
            clearFilterTextView.setGone()
            presenter.refresh(true)
        }
    }
    public override fun getAdapter(): InboxAdapter {
        if (mAdapter == null) {
            mAdapter = InboxAdapter(activity, presenter, mAdapterCallback)
        }
        return mAdapter
    }

    private val mAdapterCallback = AdapterToFragmentCallback<Conversation> { conversation, position ->
        //we send a parcel copy so that we can properly propagate updates through our events
        if (resources.getBoolean(R.bool.is_device_tablet)) { //but tablets need reference, since the detail view remains in view
            val args = MessageThreadFragment.createBundle(conversation, position, InboxApi.conversationScopeToString(presenter.scope))
            RouteMatcher.route(context, Route(null, MessageThreadFragment::class.java, null, args))
        } else { //phones use the parcel copy
            val args = MessageThreadFragment.createBundle(conversation.parcelCopy(), position, InboxApi.conversationScopeToString(presenter.scope))
            RouteMatcher.route(context, Route(null, MessageThreadFragment::class.java, null, args))
        }
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
        setFilterText()
    }

    private fun setFilterText() {
        clearFilterTextView.setTextColor(ThemePrefs.buttonColor)
        if (presenter.canvasContext != null) {
            courseFilter.text = (presenter.canvasContext as CanvasContext).name
            clearFilterTextView.setVisible()
        }
    }

    private fun setupFilter(presenter: InboxPresenter) {
        filterText.text = getTextByScope(presenter.scope)
        filterIndicator.setImageDrawable(ColorUtils.colorIt(ContextCompat.getColor(context, R.color.inbox_filter_gray), filterIndicator.drawable))
        filterButton.setOnClickListener(View.OnClickListener {
            if (context == null) return@OnClickListener

            val popup = PopupMenu(context, popupViewPosition)
            popup.menuInflater.inflate(R.menu.conversation_scope, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.inbox_all -> onScopeChanged(InboxApi.Scope.ALL)
                    R.id.inbox_unread -> onScopeChanged(InboxApi.Scope.UNREAD)
                    R.id.inbox_starred -> onScopeChanged(InboxApi.Scope.STARRED)
                    R.id.inbox_sent -> onScopeChanged(InboxApi.Scope.SENT)
                    R.id.inbox_archived -> onScopeChanged(InboxApi.Scope.ARCHIVED)
                }

                true
            }

            popup.show()
        })
    }

    val menuItemCallback: (MenuItem) -> Unit = { item ->
        when (item.itemId) {
            R.id.inboxFilter -> {
                //let the user select the course/group they want to see
                val dialog = CanvasContextListDialog.getInstance(activity.supportFragmentManager) { canvasContext: CanvasContext ->
                    mCanvasContextSelected = canvasContext
                    if (presenter.canvasContext?.id != mCanvasContextSelected?.id) {
                        //we only want to change this up if they are selecting a new context
                        presenter.canvasContext = canvasContext
                        presenter.refresh(true)
                    }
                }

                dialog.show(activity.supportFragmentManager, CanvasContextListDialog::class.java.simpleName)
            }
        }
    }

    private fun onScopeChanged(scope: InboxApi.Scope) {
        filterText.text = getTextByScope(scope)
        presenter.scope = scope

        if(scope == InboxApi.Scope.STARRED) {
            emptyPandaView.setEmptyViewImage(ContextCompat.getDrawable(context, R.drawable.vd_star_empty))
            emptyPandaView.setMessageText(R.string.inbox_empty_starred_message)
            emptyPandaView.setTitleText(R.string.inbox_empty_starred_title)
        } else {
            emptyPandaView.setEmptyViewImage(ContextCompat.getDrawable(context, R.drawable.vd_mail_empty))
            emptyPandaView.setMessageText(R.string.inbox_empty_message)
            emptyPandaView.setTitleText(R.string.inbox_empty_title)
        }
    }

    private fun getTextByScope(scope: InboxApi.Scope): String {
        when (scope) {
            InboxApi.Scope.ALL -> return getString(R.string.inbox_all_messages)
            InboxApi.Scope.UNREAD -> return getString(R.string.inbox_unread)
            InboxApi.Scope.STARRED -> return getString(R.string.inbox_starred)
            InboxApi.Scope.SENT -> return getString(R.string.inbox_sent)
            InboxApi.Scope.ARCHIVED -> return getString(R.string.inbox_archived)
            else -> return getString(R.string.inbox_all_messages)
        }
    }

    //tablet specific event for updates
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onConversationUpdated(event: ConversationUpdatedEventTablet) {
        event.once(javaClass.simpleName) {
            if(presenter.scope == event.scope && presenter.scope != InboxApi.Scope.UNREAD)
                //for removed stars and archives, we need to update the list completely
                presenter.refresh(true)
            else
                mAdapter.notifyItemChanged(it)
        }
    }

    //tablet specific event for deletion
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onConversationDeleted(event: ConversationDeletedEvent) {
        event.once(javaClass.simpleName + ".onPost()") {
            presenter.data.removeItemAt(it)
            //pop current detail fragment if tablet
            if (resources.getBoolean(R.bool.is_device_tablet)) {
                val currentFrag = fragmentManager.findFragmentById(R.id.detail)
                if(currentFrag != null) {
                    val transaction = fragmentManager.beginTransaction()
                    transaction.remove(currentFrag)
                    transaction.commit()
                    fragmentManager.popBackStack()
                }
            }
        }
    }

    override fun perPageCount(): Int = ApiPrefs.perPageCount

    companion object {
        fun newInstance() = InboxFragment()
    }
}
