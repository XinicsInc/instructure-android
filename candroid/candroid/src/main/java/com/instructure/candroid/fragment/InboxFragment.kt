/*
 * Copyright (C) 2018 - present Instructure, Inc.
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
package com.instructure.candroid.fragment

import android.content.Context
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.view.*
import com.instructure.candroid.R
import com.instructure.candroid.adapter.InboxAdapter
import com.instructure.candroid.decorations.DividerDecoration
import com.instructure.candroid.dialog.CanvasContextListDialog
import com.instructure.candroid.events.ConversationUpdatedEvent
import com.instructure.candroid.interfaces.AdapterToFragmentCallback
import com.instructure.candroid.util.FragUtils
import com.instructure.canvasapi2.apis.InboxApi
import com.instructure.canvasapi2.apis.InboxApi.Scope
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.Conversation
import com.instructure.interactions.FragmentInteractions
import com.instructure.interactions.Navigation
import com.instructure.canvasapi2.utils.pageview.PageView
import com.instructure.pandautils.utils.*
import kotlinx.android.synthetic.main.fragment_inbox.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

@PageView(url = "conversations")
class InboxFragment : ParentFragment() {

    private var messageType: Scope = Scope.ALL

    private lateinit var adapter: InboxAdapter

    private var onUnreadCountInvalidated: OnUnreadCountInvalidated? = null

    interface OnUnreadCountInvalidated {
        fun invalidateUnreadCount()
    }

    private var adapterToFragmentCallback = object : AdapterToFragmentCallback<Conversation> {
        override fun onRowClicked(conversation: Conversation, position: Int, isOpenDetail: Boolean) {
            showConversation(conversation)
        }

        override fun onRefreshFinished() {
            setRefreshing(false)

            // update the unread count
            onUnreadCountInvalidated?.invalidateUnreadCount()
        }
    }

    override fun getFragmentPlacement() = FragmentInteractions.Placement.MASTER

    override fun allowBookmarking() = false

    override fun title(): String = getString(R.string.inbox)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        navigation?.attachNavigationDrawer(this, toolbar)
        toolbar.title = title()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        try {
            onUnreadCountInvalidated = context as OnUnreadCountInvalidated?
        } catch (e: ClassCastException) {
            throw ClassCastException(context!!.toString() + " must implement OnUnreadCountInvalidated")
        }

    }
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = layoutInflater.inflate(R.layout.fragment_inbox, container, false)
        adapter = InboxAdapter(context, adapterToFragmentCallback)
        val recyclerView = configureRecyclerView(rootView, context, adapter, R.id.swipeRefreshLayout, R.id.emptyView, R.id.inboxRecyclerView)
        recyclerView.addItemDecoration(DividerDecoration(context))
        return rootView
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        setupFilter()
        setupFilterText()
        emptyView.setEmptyViewImage(ContextCompat.getDrawable(context, R.drawable.vd_mail_empty))
        emptyView.setMessageText(R.string.inboxEmptyMessage)
        inboxRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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

    private fun setupFilterText() {
        clearFilterTextView.setTextColor(ThemePrefs.buttonColor)
        adapter.canvasContext?.let {
            courseFilter.text = it.name
            clearFilterTextView.setVisible()
        }
    }

    private fun setupListeners() {
        addMessage.onClickWithRequireNetwork {
            navigation?.addFragment(
                FragUtils.getFrag(InboxComposeMessageFragment::class.java, InboxComposeMessageFragment.createBundleNewConversation())
            )
        }

        clearFilterTextView.setOnClickListener {
            adapter.canvasContext = null
            courseFilter.setText(R.string.allCourses)
            clearFilterTextView.setGone()
            reloadData()
        }
    }

    private fun setupFilter() {
        filterText.text = getTextByScope(adapter.scope)
        filterButton.setOnClickListener(View.OnClickListener {
            if (context == null) return@OnClickListener

            val popup = PopupMenu(context, popupViewPosition)
            popup.menuInflater.inflate(R.menu.inbox_scope, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.inbox_all -> onScopeChanged(Scope.ALL)
                    R.id.inbox_unread -> onScopeChanged(Scope.UNREAD)
                    R.id.inbox_starred -> onScopeChanged(Scope.STARRED)
                    R.id.inbox_sent -> onScopeChanged(Scope.SENT)
                    R.id.inbox_archived -> onScopeChanged(Scope.ARCHIVED)
                }
                true
            }
            popup.show()
        })
    }

    private fun getTextByScope(scope: Scope): String {
        return when (scope) {
            Scope.ALL -> getString(R.string.inboxAllMessages)
            Scope.UNREAD -> getString(R.string.inbox_unread)
            Scope.STARRED -> getString(R.string.inbox_starred)
            Scope.SENT -> getString(R.string.inbox_sent)
            Scope.ARCHIVED -> getString(R.string.inbox_archived)
        }
    }

    private fun onScopeChanged(scope: Scope) {
        filterText.text = getTextByScope(scope)
        adapter.scope = scope

        if (scope == Scope.STARRED) {
            emptyView.setEmptyViewImage(ContextCompat.getDrawable(context, R.drawable.vd_star))
            emptyView.setMessageText(R.string.inboxEmptyStarredMessage)
            emptyView.setTitleText(R.string.inboxEmptyStarredTitle)
        } else {
            emptyView.setEmptyViewImage(ContextCompat.getDrawable(context, R.drawable.vd_mail_empty))
            emptyView.setMessageText(R.string.inboxEmptyMessage)
            emptyView.setTitleText(R.string.inboxEmptyTitle)
        }
    }

    override fun applyTheme() {
        ViewStyler.themeToolbar(activity, toolbar, ThemePrefs.primaryColor, ThemePrefs.primaryTextColor)
        ViewStyler.themeFAB(addMessage, ThemePrefs.buttonColor)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.menu_filter_inbox, menu)
        toolbar.post {
            ViewStyler.themeToolbar(activity, toolbar, ThemePrefs.primaryColor, ThemePrefs.primaryTextColor)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.inboxFilter -> {
                // Let the user select the course/group they want to see
                val dialog = CanvasContextListDialog.getInstance(activity.supportFragmentManager) { canvasContext ->
                    if (adapter.canvasContext?.contextId != canvasContext.contextId) {
                        // We only want to change this up if they are selecting a new context
                        adapter.canvasContext = canvasContext
                        reloadData()
                    }
                }

                dialog.show(activity.supportFragmentManager, CanvasContextListDialog::class.java.simpleName)
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun showConversation(conversation: Conversation) {
        if (activity !is Navigation) return
        val fragment = FragUtils.getFrag(
            InboxConversationFragment::class.java,
            InboxConversationFragment.createBundle(conversation, 0, InboxApi.conversationScopeToString(messageType))
        )
        navigation?.addFragment(fragment)
    }

    @Suppress("unused")
    @Subscribe(sticky = true)
    fun onUpdateConversation(event: ConversationUpdatedEvent) {
        event.once(javaClass.simpleName) {
            reloadData()

            // update the unread count
            onUnreadCountInvalidated?.invalidateUnreadCount()
        }
    }

    override fun reloadData() {
        adapter.refresh()
        setupFilterText()
    }

    companion object {
        fun createBundle(canvasContext: CanvasContext): Bundle {
            return ParentFragment.createBundle(canvasContext)
        }
    }
}
