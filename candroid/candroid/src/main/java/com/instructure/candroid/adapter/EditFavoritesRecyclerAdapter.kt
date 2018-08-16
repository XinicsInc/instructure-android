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
package com.instructure.candroid.adapter

import android.app.Activity
import android.view.View
import com.instructure.candroid.holders.EditFavoritesViewHolder
import com.instructure.candroid.interfaces.AdapterToFragmentCallback
import com.instructure.canvasapi2.managers.CourseManager
import com.instructure.canvasapi2.models.Course
import com.instructure.canvasapi2.utils.isInvited
import com.instructure.canvasapi2.utils.weave.WeaveJob
import com.instructure.canvasapi2.utils.weave.awaitApi
import com.instructure.canvasapi2.utils.weave.catch
import com.instructure.canvasapi2.utils.weave.tryWeave

class EditFavoritesRecyclerAdapter(
        context: Activity,
        private val mAdapterToFragmentCallback: AdapterToFragmentCallback<Course>
) : BaseListRecyclerAdapter<Course, EditFavoritesViewHolder>(context, Course::class.java) {

    private var mApiCalls: WeaveJob? = null

    init {
        itemCallback = object : ItemComparableCallback<Course>() {
            override fun compare(o1: Course, o2: Course) = o1.name.orEmpty().compareTo(o2.name.orEmpty())
            override fun areContentsTheSame(oldItem: Course, newItem: Course) = false
            override fun areItemsTheSame(item1: Course, item2: Course) = item1.contextId == item2.contextId
            override fun getUniqueItemId(item: Course) = item.contextId.hashCode().toLong()
        }
        loadData()
    }

    override fun itemLayoutResId(viewType: Int): Int = EditFavoritesViewHolder.holderResId()

    override fun createViewHolder(v: View, viewType: Int) = EditFavoritesViewHolder(v)

    override fun bindHolder(model: Course, holder: EditFavoritesViewHolder, position: Int) {
        holder.bind(context, model, mAdapterToFragmentCallback)
    }

    override fun loadData() {
        mApiCalls?.cancel()
        mApiCalls = tryWeave {
            val rawCourses = awaitApi<List<Course>>{ CourseManager.getCourses(true, it) }
            val validCourses = rawCourses.filter { !it.isAccessRestrictedByDate && !it.isInvited() }
            addAll(validCourses)
            notifyDataSetChanged()
            isAllPagesLoaded = true
            if (itemCount == 0) adapterToRecyclerViewCallback.setIsEmpty(true)
            mAdapterToFragmentCallback.onRefreshFinished()
        } catch {
            onNoNetwork()
        }
    }

    override fun refresh() {
        mApiCalls?.cancel()
        super.refresh()
    }

    override fun cancel() {
        mApiCalls?.cancel()
    }
}
