/*
 * Copyright (C) 2016 - present Instructure, Inc.
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

package com.instructure.candroid.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import com.instructure.candroid.R
import com.instructure.candroid.binders.EmptyBinder
import com.instructure.candroid.holders.*
import com.instructure.candroid.interfaces.AdapterToFragmentCallback
import com.instructure.canvasapi2.StatusCallback
import com.instructure.canvasapi2.managers.AnnouncementManager
import com.instructure.canvasapi2.managers.DiscussionManager
import com.instructure.canvasapi2.models.*
import com.instructure.canvasapi2.utils.ApiType
import com.instructure.canvasapi2.utils.LinkHeaders
import com.instructure.canvasapi2.utils.weave.*
import com.instructure.pandarecycler.util.GroupSortedList
import com.instructure.pandarecycler.util.Types
import com.instructure.pandautils.utils.ColorKeeper
import kotlinx.coroutines.experimental.Job
import retrofit2.Response
import java.util.*

open class DiscussionListRecyclerAdapter(
        context: Context,
        private val canvasContext: CanvasContext,
        private val isDiscussions: Boolean,
        private val callback: AdapterToDiscussionsCallback)
    : ExpandableRecyclerAdapter<String, DiscussionTopicHeader, RecyclerView.ViewHolder>(context, String::class.java, DiscussionTopicHeader::class.java) {

    private var discussionsListJob: Job? = null

    interface AdapterToDiscussionsCallback : AdapterToFragmentCallback<DiscussionTopicHeader>{
        fun discussionOverflow(group: String?, discussionTopicHeader: DiscussionTopicHeader)
        fun askToDeleteDiscussion(discussionTopicHeader: DiscussionTopicHeader)
        fun onRefreshStarted()
    }

    init {
        isExpandedByDefault = true
        loadData()
    }

    override fun createViewHolder(v: View, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == Types.TYPE_HEADER) {
            if(isDiscussions) DiscussionExpandableViewHolder(v) else NoViewholder(v)
        } else {
            DiscussionListHolder(v)
        }
    }

    override fun itemLayoutResId(viewType: Int): Int {
        return if (viewType == Types.TYPE_HEADER) {
            if(isDiscussions) DiscussionExpandableViewHolder.holderResId else NoViewholder.holderResId
        } else {
            DiscussionListHolder.holderResId
        }
    }

    override fun onBindChildHolder(holder: RecyclerView.ViewHolder, group: String, discussionTopicHeader: DiscussionTopicHeader) {
        context?.let { (holder as DiscussionListHolder).bind(it, discussionTopicHeader, group, ColorKeeper.getOrGenerateColor(canvasContext), isDiscussions, callback) }
    }

    override fun onBindHeaderHolder(holder: RecyclerView.ViewHolder, group: String, isExpanded: Boolean) {
        if (isDiscussions) {
            (holder as DiscussionExpandableViewHolder).bind(isExpanded, isDiscussions, holder, group, {
                discussionGroup ->
                expandCollapseGroup(discussionGroup)
            })
        }
    }

    override fun onBindEmptyHolder(holder: RecyclerView.ViewHolder?, group: String?) {
        EmptyBinder.bind(holder as EmptyViewHolder, context.resources.getString(R.string.utils_emptyDiscussions))
    }

    override fun loadData() {
        callback.onRefreshStarted()
        discussionsListJob = tryWeave {
            val response = awaitApi<List<DiscussionTopicHeader>> {
                if (isDiscussions) DiscussionManager.getAllDiscussionTopicHeaders(canvasContext, isRefresh, it)
                else AnnouncementManager.getAllAnnouncements(canvasContext, isRefresh, it)
            }

            if(isDiscussions) {
                addOrUpdateAllItems(PINNED, response.filter { getHeaderType(it) == PINNED })
                addOrUpdateAllItems(CLOSED_FOR_COMMENTS, response.filter { getHeaderType(it) == CLOSED_FOR_COMMENTS })
                addOrUpdateAllItems(UNPINNED, response.filter { getHeaderType(it) == UNPINNED })
            } else {
                addOrUpdateAllItems(ANNOUNCEMENTS, response)
            }

            callback.onRefreshFinished()
            adapterToRecyclerViewCallback.setIsEmpty(size() == 0)
        } catch {
            callback.onRefreshFinished()
            adapterToRecyclerViewCallback.setIsEmpty(size() == 0)
        }
    }

    private fun getHeaderType(discussionTopicHeader: DiscussionTopicHeader): String {
        if(discussionTopicHeader.isPinned) return PINNED
        if(discussionTopicHeader.isLocked) return CLOSED_FOR_COMMENTS
        return UNPINNED
    }

    companion object {
        //Named funny to preserve the order.
        const val PINNED = "1_PINNED"
        const val UNPINNED = "2_UNPINNED"
        const val CLOSED_FOR_COMMENTS = "3_CLOSED_FOR_COMMENTS"
        const val ANNOUNCEMENTS = "ANNOUNCEMENTS"
        const val DELETE = "delete"
    }

    private val mDiscussionTopicHeaderPinnedCallback = object : StatusCallback<DiscussionTopicHeader>() {
        override fun onResponse(response: Response<DiscussionTopicHeader>, linkHeaders: LinkHeaders, type: ApiType) {
            response.body()?.let { addOrUpdateItem(PINNED, it) }
        }
    }

    private val mDiscussionTopicHeaderUnpinnedCallback = object : StatusCallback<DiscussionTopicHeader>() {
        override fun onResponse(response: Response<DiscussionTopicHeader>, linkHeaders: LinkHeaders, type: ApiType) {
            response.body()?.let { addOrUpdateItem(UNPINNED, it) }
        }
    }

    private val mDiscussionTopicHeaderClosedForCommentsCallback = object : StatusCallback<DiscussionTopicHeader>() {
        override fun onResponse(response: Response<DiscussionTopicHeader>, linkHeaders: LinkHeaders, type: ApiType) {
            response.body()?.let { addOrUpdateItem(CLOSED_FOR_COMMENTS, it) }
        }
    }

    private val mDiscussionTopicHeaderOpenedForCommentsCallback = object : StatusCallback<DiscussionTopicHeader>() {
        override fun onResponse(response: Response<DiscussionTopicHeader>, linkHeaders: LinkHeaders, type: ApiType) {
            response.body()?.let { addOrUpdateItem(if(it.isPinned) PINNED else UNPINNED, it) }
        }
    }

    fun requestMoveDiscussionTopicToGroup(groupTo: String, groupFrom: String, discussionTopicHeader: DiscussionTopicHeader) {
        //Move from this group into another
        when(groupFrom) {
            PINNED -> {
                when(groupTo) {
                    UNPINNED -> DiscussionManager.unpinDiscussionTopicHeader(canvasContext, discussionTopicHeader.id, mDiscussionTopicHeaderUnpinnedCallback)
                    CLOSED_FOR_COMMENTS -> DiscussionManager.lockDiscussionTopicHeader(canvasContext, discussionTopicHeader.id, mDiscussionTopicHeaderClosedForCommentsCallback)
                    DELETE -> { callback.askToDeleteDiscussion(discussionTopicHeader) }
                }
            }
            UNPINNED -> {
                when(groupTo) {
                    PINNED -> DiscussionManager.pinDiscussionTopicHeader(canvasContext, discussionTopicHeader.id, mDiscussionTopicHeaderPinnedCallback)
                    CLOSED_FOR_COMMENTS -> DiscussionManager.lockDiscussionTopicHeader(canvasContext, discussionTopicHeader.id, mDiscussionTopicHeaderClosedForCommentsCallback)
                    DELETE -> { callback.askToDeleteDiscussion(discussionTopicHeader) }
                }
            }
            CLOSED_FOR_COMMENTS -> {
                when(groupTo) {
                    PINNED -> DiscussionManager.pinDiscussionTopicHeader(canvasContext, discussionTopicHeader.id, mDiscussionTopicHeaderPinnedCallback)
                    CLOSED_FOR_COMMENTS -> DiscussionManager.unlockDiscussionTopicHeader(canvasContext, discussionTopicHeader.id, mDiscussionTopicHeaderOpenedForCommentsCallback)
                    DELETE -> { callback.askToDeleteDiscussion(discussionTopicHeader) }
                }
            }
            DELETE -> { callback.askToDeleteDiscussion(discussionTopicHeader) }
        }
    }

    fun deleteDiscussionTopicHeader(discussionTopicHeader: DiscussionTopicHeader) {
        DiscussionManager.deleteDiscussionTopicHeader(canvasContext, discussionTopicHeader.id, object : StatusCallback<Void>() {
            override fun onResponse(response: Response<Void>, linkHeaders: LinkHeaders, type: ApiType) {
                removeItem(discussionTopicHeader, false)
            }
        })
    }

    override fun createGroupCallback(): GroupSortedList.GroupComparatorCallback<String> {
        return object : GroupSortedList.GroupComparatorCallback<String> {
            override fun compare(group1: String?, group2: String?): Int {
                if(group1 == null || group2 == null) return -1
                return group1.compareTo(group2)
            }

            override fun areContentsTheSame(oldGroup: String, newGroup: String): Boolean {
                return oldGroup == newGroup
            }

            override fun areItemsTheSame(group1: String, group2: String): Boolean {
                return group1 == group2
            }

            override fun getUniqueGroupId(group: String): Long {
                return group.hashCode().toLong()
            }

            override fun getGroupType(group: String): Int {
                return Types.TYPE_HEADER
            }
        }
    }

    override fun createItemCallback(): GroupSortedList.ItemComparatorCallback<String, DiscussionTopicHeader> {
        return object : GroupSortedList.ItemComparatorCallback<String, DiscussionTopicHeader> {
            override fun compare(group: String, item1: DiscussionTopicHeader, item2: DiscussionTopicHeader): Int {
                if(PINNED == group) {
                    return item1.position.compareTo(item2.position)
                } else {
                    return if(isDiscussions) item2.lastReplyAt?.compareTo(item1.lastReplyAt ?: Date(0)) ?: -1
                    else -1
                }
            }

            override fun areContentsTheSame(item1: DiscussionTopicHeader, item2: DiscussionTopicHeader): Boolean {
                return item1.title == item2.title && item1.status == item2.status
            }

            override fun areItemsTheSame(item1: DiscussionTopicHeader?, item2: DiscussionTopicHeader?): Boolean {
                return item1?.id == item2?.id
            }

            override fun getUniqueItemId(discussionTopicHeader: DiscussionTopicHeader): Long {
                return discussionTopicHeader.id
            }

            override fun getChildType(group: String, item: DiscussionTopicHeader): Int {
                return Types.TYPE_ITEM
            }
        }
    }
}
