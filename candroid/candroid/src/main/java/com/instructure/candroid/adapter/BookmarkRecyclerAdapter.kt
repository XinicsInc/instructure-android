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
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.instructure.candroid.R

import com.instructure.candroid.interfaces.BookmarkAdapterToFragmentCallback
import com.instructure.candroid.util.CacheControlFlags
import com.instructure.candroid.util.RouterUtils
import com.instructure.canvasapi2.StatusCallback
import com.instructure.canvasapi2.managers.BookmarkManager
import com.instructure.canvasapi2.models.Bookmark
import com.instructure.canvasapi2.utils.APIHelper
import com.instructure.canvasapi2.utils.ApiType
import com.instructure.canvasapi2.utils.LinkHeaders
import com.instructure.pandautils.utils.ColorKeeper
import com.instructure.pandautils.utils.ColorUtils

import retrofit2.Call
import retrofit2.Response

class BookmarkRecyclerAdapter(context: Context, isShortcutActivity: Boolean, private val mAdapterToFragmentCallback: BookmarkAdapterToFragmentCallback<Bookmark>)
    : BaseListRecyclerAdapter<Bookmark, BookmarkViewHolder>(context, Bookmark::class.java) {

    private var bookmarksCallback: StatusCallback<List<Bookmark>>? = null
    private var mIsShortcutActivity = false

    init {
        mIsShortcutActivity = isShortcutActivity
        itemCallback = object : BaseListRecyclerAdapter.ItemComparableCallback<Bookmark>() {
            override fun compare(o1: Bookmark, o2: Bookmark): Int {
                return o1.name.toLowerCase().compareTo(o2.name.toLowerCase())
            }

            override fun areContentsTheSame(item1: Bookmark, item2: Bookmark): Boolean {
                return item1.name.toLowerCase() == item2.name.toLowerCase()
            }

            override fun getUniqueItemId(bookmark: Bookmark): Long {
                return bookmark.id
            }
        }

        if(CacheControlFlags.forceRefreshBookmarks) isRefresh = true
        setupCallbacks()
        loadData()
    }

    override fun bindHolder(bookmark: Bookmark, holder: BookmarkViewHolder, position: Int) {
        BookmarkBinder.bind(mIsShortcutActivity, holder, bookmark, mAdapterToFragmentCallback)
    }

    override fun createViewHolder(v: View, viewType: Int): BookmarkViewHolder {
        return BookmarkViewHolder(v)
    }

    override fun itemLayoutResId(viewType: Int): Int {
        return BookmarkViewHolder.holderResId()
    }

    override fun setupCallbacks() {
        bookmarksCallback = object : StatusCallback<List<Bookmark>>() {

            override fun onResponse(response: retrofit2.Response<List<Bookmark>>, linkHeaders: LinkHeaders, type: ApiType) {
                addAll(response.body())
                setNextUrl(linkHeaders.nextUrl)
                mAdapterToFragmentCallback.onRefreshFinished()
            }

            override fun onFail(call: Call<List<Bookmark>>?, error: Throwable, response: Response<*>?) {
                if (response != null && !APIHelper.isCachedResponse(response) || !APIHelper.hasNetworkConnection()) {
                    adapterToRecyclerViewCallback.setIsEmpty(true)
                }
            }

            override fun onFinished(type: ApiType) {
                this@BookmarkRecyclerAdapter.onCallbackFinished()
            }
        }
    }

    override fun loadData() {
        BookmarkManager.getBookmarks(bookmarksCallback!!, isRefresh)
    }
}

class BookmarkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    var title: TextView = itemView.findViewById<TextView>(R.id.title)
    var icon: ImageView = itemView.findViewById<ImageView>(R.id.icon)
    var overflow: FrameLayout = itemView.findViewById<FrameLayout>(R.id.overflow)

    companion object {
        fun holderResId(): Int {
            return R.layout.viewholder_bookmark
        }
    }
}

object BookmarkBinder {

    fun bind(
            isShortcutActivity: Boolean,
            holder: BookmarkViewHolder,
            bookmark: Bookmark,
            adapterToFragmentCallback: BookmarkAdapterToFragmentCallback<Bookmark>) {

        var courseId = bookmark.courseId

        if (courseId == 0L) {
            courseId = RouterUtils.getCourseIdFromURL(bookmark.url)
            bookmark.courseId = courseId
        }

        holder.title.text = bookmark.name
        val courseColor = ColorKeeper.getOrGenerateColor(RouterUtils.getContextIdFromURL(bookmark.url))

        holder.icon.setImageDrawable(ColorUtils.colorIt(courseColor, holder.icon.drawable))
        holder.overflow.visibility = if (isShortcutActivity) View.INVISIBLE else View.VISIBLE

        holder.itemView.setOnClickListener { adapterToFragmentCallback.onRowClicked(bookmark, holder.adapterPosition, false) }

        holder.overflow.setOnClickListener { view -> adapterToFragmentCallback.onOverflowClicked(bookmark, holder.adapterPosition, view) }
    }
}
