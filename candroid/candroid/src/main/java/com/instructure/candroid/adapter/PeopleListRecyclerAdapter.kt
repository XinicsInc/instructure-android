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

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import com.instructure.candroid.R
import com.instructure.candroid.binders.PeopleBinder
import com.instructure.candroid.binders.PeopleHeaderBinder
import com.instructure.candroid.holders.PeopleHeaderViewHolder
import com.instructure.candroid.holders.PeopleViewHolder
import com.instructure.candroid.interfaces.AdapterToFragmentCallback
import com.instructure.canvasapi2.apis.UserAPI
import com.instructure.canvasapi2.managers.UserManager
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.Group
import com.instructure.canvasapi2.models.User
import com.instructure.canvasapi2.utils.weave.WeaveJob
import com.instructure.canvasapi2.utils.weave.awaitPaginated
import com.instructure.canvasapi2.utils.weave.catch
import com.instructure.canvasapi2.utils.weave.tryWeave
import com.instructure.pandarecycler.util.GroupSortedList
import com.instructure.pandarecycler.util.Types
import com.instructure.pandautils.utils.ColorKeeper
import com.instructure.pandautils.utils.toast

class PeopleListRecyclerAdapter(
        context: Context,
        private val mCanvasContext: CanvasContext,
        private val mAdapterToFragmentCallback: AdapterToFragmentCallback<User>
) : ExpandableRecyclerAdapter<String, User, RecyclerView.ViewHolder>(context, String::class.java, User::class.java) {

    private val mCourseColor = ColorKeeper.getOrGenerateColor(mCanvasContext)
    private val mEnrollmentPriority = mapOf( "Teacher" to 4, "Ta" to 3, "Student" to 2, "Observer" to 1)
    private var mApiCalls: WeaveJob? = null

    init {
        isExpandedByDefault = true
        loadData()
    }

    @Suppress("EXPERIMENTAL_FEATURE_WARNING")
    override fun loadFirstPage() {
        mApiCalls = tryWeave {
            var canvasContext = mCanvasContext

            // If the canvasContext is a group, and has a course we want to add the Teachers and TAs from that course to the peoples list
            if (CanvasContext.Type.isGroup(mCanvasContext) && (mCanvasContext as Group).courseId > 0) {
                // We build a generic CanvasContext with type set to COURSE and give it the CourseId from the group, so that it wil use the course API not the group API
                canvasContext = CanvasContext.getGenericContext(CanvasContext.Type.COURSE, mCanvasContext.courseId, "")!!
            }

            // Get Teachers
            awaitPaginated<List<User>> {
                onRequestFirst { UserManager.getFirstPagePeopleList(canvasContext, UserAPI.ENROLLMENT_TYPE.TEACHER, true, it) }
                onRequestNext { nextUrl, callback -> UserManager.getNextPagePeopleList(true, nextUrl, callback) }
                onResponse { setNextUrl(""); populateAdapter(it) }
            }

            // Get TAs
            awaitPaginated<List<User>> {
                onRequestFirst { UserManager.getFirstPagePeopleList(canvasContext, UserAPI.ENROLLMENT_TYPE.TA, true, it) }
                onRequestNext { nextUrl, callback -> UserManager.getNextPagePeopleList(true, nextUrl, callback) }
                onResponse { setNextUrl(""); populateAdapter(it) }
            }

            // Get others
            awaitPaginated<List<User>> {
                onRequestFirst { UserManager.getFirstPagePeopleList(mCanvasContext, true, it) }
                onRequestNext { nextUrl, callback -> UserManager.getNextPagePeopleList(true, nextUrl, callback) }
                onResponse { setNextUrl(""); populateAdapter(it) }
            }

            setNextUrl(null)
        } catch {
            context.toast(R.string.errorOccurred)
        }
    }

    override fun loadNextPage(nextURL: String) {
        mApiCalls?.next()
    }

    override fun isPaginated() = true

    override fun resetData() {
        mApiCalls?.cancel()
        super.resetData()
    }

    override fun cancel() {
        mApiCalls?.cancel()
    }

    private fun populateAdapter(result: List<User>) {
        val (enrolled, unEnrolled) = result.partition { it.enrollments?.isNotEmpty() == true }
        enrolled.onEach { it.enrollments.sortByDescending { mEnrollmentPriority[it.type] ?: 0 } }
                .groupBy { it.enrollments[0].type }
                .forEach { (type, users) -> addOrUpdateAllItems(type, users) }
        if (CanvasContext.Type.isGroup(mCanvasContext)) addOrUpdateAllItems(context.getString(R.string.groupMembers), unEnrolled)
        notifyDataSetChanged()
        mAdapterToFragmentCallback.onRefreshFinished()
    }

    override fun createViewHolder(v: View, viewType: Int): RecyclerView.ViewHolder =
            if (viewType == Types.TYPE_HEADER) PeopleHeaderViewHolder(v) else PeopleViewHolder(v)

    override fun itemLayoutResId(viewType: Int): Int =
            if (viewType == Types.TYPE_HEADER) PeopleHeaderViewHolder.holderResId() else PeopleViewHolder.holderResId()

    override fun contextReady() = Unit

    override fun onBindChildHolder(holder: RecyclerView.ViewHolder, enrollmentType: String, user: User) {
        val groupItemCount = getGroupItemCount(enrollmentType)
        val itemPosition = storedIndexOfItem(enrollmentType, user)
        PeopleBinder.bind(user, context, holder as PeopleViewHolder, mAdapterToFragmentCallback, mCourseColor, itemPosition == 0, itemPosition == groupItemCount - 1)
    }

    override fun onBindHeaderHolder(holder: RecyclerView.ViewHolder, enrollmentType: String, isExpanded: Boolean) {
        PeopleHeaderBinder.bind(context, mCanvasContext, holder as PeopleHeaderViewHolder, enrollmentType, getHeaderTitle(enrollmentType), isExpanded, viewHolderHeaderClicked)
    }

    override fun createGroupCallback(): GroupSortedList.GroupComparatorCallback<String> {
        return object : GroupSortedList.GroupComparatorCallback<String> {
            override fun compare(o1: String, o2: String) = getHeaderTitle(o2).compareTo(getHeaderTitle(o1))
            override fun areContentsTheSame(oldGroup: String, newGroup: String) = getHeaderTitle(oldGroup) == getHeaderTitle(newGroup)
            override fun areItemsTheSame(group1: String, group2: String) = getHeaderTitle(group1) == getHeaderTitle(group2)
            override fun getUniqueGroupId(group: String) = getHeaderTitle(group).hashCode().toLong()
            override fun getGroupType(group: String) = Types.TYPE_HEADER
        }
    }

    override fun createItemCallback(): GroupSortedList.ItemComparatorCallback<String, User> {
        return object : GroupSortedList.ItemComparatorCallback<String, User> {
            override fun compare(group: String, o1: User, o2: User) = o1.sortableName.toLowerCase().compareTo(o2.sortableName.toLowerCase())
            override fun areContentsTheSame(oldItem: User, newItem: User) = oldItem.sortableName == newItem.sortableName
            override fun areItemsTheSame(item1: User, item2: User) = item1.id == item2.id
            override fun getUniqueItemId(item: User) = item.id
            override fun getChildType(group: String, item: User) = Types.TYPE_ITEM
        }
    }

    private fun getHeaderTitle(enrollmentType: String): String = when (enrollmentType) {
        "Teacher", "Ta" -> context.getString(R.string.teachersTas)
        "Student" -> context.getString(R.string.students)
        "Observer" -> context.getString(R.string.observers)
        else -> enrollmentType
    }
}
