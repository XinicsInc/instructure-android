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

package com.instructure.teacher.holders

import android.content.Context
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import com.instructure.canvasapi2.models.Assignment
import com.instructure.canvasapi2.utils.DateHelper
import com.instructure.canvasapi2.utils.NumberHelper
import com.instructure.pandautils.utils.ThemePrefs
import com.instructure.pandautils.utils.setGone
import com.instructure.pandautils.utils.setVisible
import com.instructure.teacher.R
import com.instructure.teacher.utils.getAssignmentIcon
import kotlinx.android.synthetic.main.adapter_assignment.view.*
import java.util.*

class AssignmentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    companion object {
        val holderResId = R.layout.adapter_assignment
    }

    init {
        itemView.ungradedCount.setTextColor(ThemePrefs.brandColor)
        DrawableCompat.setTint(itemView.ungradedCount.background, ThemePrefs.brandColor)
    }

    fun bind(context: Context, assignment: Assignment, courseColor: Int, callback: (Assignment) -> Unit) = with(itemView){
        assignmentLayout.setOnClickListener { callback(assignment) }
        assignmentTitle.text = assignment.name
        assignmentIcon.setIcon(assignment.getAssignmentIcon(), courseColor)
        assignmentIcon.setPublishedStatus(assignment.isPublished)
        publishedBar.visibility = if (assignment.isPublished) View.VISIBLE else View.INVISIBLE


        // String to track if the assignment is closed. If it isn't, we'll prepend the due date string with an empty string and it will look the same
        // Otherwise, we want it to say "Closed" and the due date with a dot as a separator
        var closedString : String = ""
        if(assignment.lockAt?.before(Date()) ?: false) {
            closedString = context.getString(R.string.cmp_closed) + context.getString(R.string.dot_with_spaces)
        }

        if(assignment.allDates.size > 1) {
            //we have multiple due dates
            dueDate.text = closedString + context.getString(R.string.multiple_due_dates)
        } else {
            if (assignment.dueAt != null) {
                dueDate.text = closedString + context.getString(R.string.due, DateHelper.getMonthDayAtTime(context, assignment.dueAt, context.getString(R.string.at)))
            } else if (assignment.allDates.size == 1 && assignment.allDates.get(0).dueAt != null) {
                // If a due date is to one section and nothing for everyone else, we can still show the due date
                dueDate.text = closedString + context.getString(R.string.due, DateHelper.getMonthDayAtTime(context, assignment.allDates.get(0).dueAt, context.getString(R.string.at)))
            } else {
                dueDate.text = closedString + context.getString(R.string.no_due_date)
            }
        }

        if (assignment.needsGradingCount == 0L || assignment.gradingType == Assignment.NOT_GRADED_TYPE) {
            ungradedCount.setGone().text = ""
        } else {
            ungradedCount.setVisible().text = context.resources.getQuantityString(
                    R.plurals.needsGradingCount,
                    assignment.needsGradingCount.toInt(),
                    NumberHelper.formatInt(assignment.needsGradingCount))
            ungradedCount.setAllCaps(true)
        }

        // set the content description on the container so we can tell the user that it is published as the last piece of information. When a content description is on a container
        assignmentLayout.contentDescription = assignment.name + " " + dueDate.text + " " + ungradedCount.text + " " + if(assignment.isPublished) context.getString(R.string.published) else context.getString(R.string.not_published)
    }
}
