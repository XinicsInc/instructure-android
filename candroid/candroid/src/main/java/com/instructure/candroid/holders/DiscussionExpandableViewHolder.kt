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
 */    package com.instructure.candroid.holders

import android.animation.AnimatorInflater
import android.animation.ObjectAnimator
import android.support.v7.widget.RecyclerView
import android.view.View
import com.instructure.candroid.R
import com.instructure.candroid.adapter.DiscussionListRecyclerAdapter
import com.instructure.pandautils.utils.setVisible
import kotlinx.android.synthetic.main.viewholder_discussion_group_header.view.*

class DiscussionExpandableViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    var mIsExpanded = true

    fun bind(isExpanded: Boolean,
             isDiscussion: Boolean,
             holder: DiscussionExpandableViewHolder,
             group: String,
             callback: (String) -> Unit) = with(itemView) {

        if(!isDiscussion) {
            this.setVisible(false)
        } else {
            this.setVisible()
        }

        mIsExpanded = isExpanded

        var title = ""

        when(group) {
            DiscussionListRecyclerAdapter.PINNED -> title = context.getString(R.string.utils_pinnedDiscussions)
            DiscussionListRecyclerAdapter.UNPINNED -> title = context.getString(R.string.utils_discussionUnpinned)
            DiscussionListRecyclerAdapter.CLOSED_FOR_COMMENTS -> title = context.getString(R.string.closed_discussion)
        }

        groupName.text = title

        holder.itemView.setOnClickListener {
            val animationType = if (mIsExpanded) R.animator.rotation_from_0_to_neg90 else R.animator.rotation_from_neg90_to_0
            mIsExpanded = !mIsExpanded
            val flipAnimator = AnimatorInflater.loadAnimator(context, animationType) as ObjectAnimator
            flipAnimator.target = collapseIcon
            flipAnimator.duration = 200
            flipAnimator.start()
            callback(group)
        }
    }

    companion object {
        var holderResId: Int = R.layout.viewholder_discussion_group_header
    }
}