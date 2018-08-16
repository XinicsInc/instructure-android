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

import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.View
import com.instructure.canvasapi2.models.Course
import com.instructure.pandautils.utils.onClickWithRequireNetwork
import com.instructure.pandautils.utils.setCourseImage
import com.instructure.pandautils.utils.ColorKeeper
import com.instructure.teacher.R
import com.instructure.teacher.utils.*
import kotlinx.android.synthetic.main.adapter_courses.view.*


class CoursesViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    companion object {
        val holderResId = R.layout.adapter_courses
    }

    fun bind(course: Course, callback: com.instructure.teacher.fragments.AllCoursesFragment.CourseBrowserCallback?) = with(itemView) {
        titleTextView.text = course.name
        courseCode.text = course.courseCode

        val courseColor = ColorKeeper.getOrGenerateColor(course)
        cardView.setCardBackgroundColor(courseColor)
        titleTextView.setTextColor(courseColor)

        courseImageView.setCourseImage(course, courseColor)

        cardView.setOnClickListener { callback?.onShowCourseDetails(course) }

        overflow.onClickWithRequireNetwork {
            val popup = PopupMenu(it.context, it, Gravity.START.and(Gravity.TOP))
            val menu = popup.menu

            // Add things to the popup menu
            menu.add(0, COURSE_EDIT_NAME_ID, COURSE_EDIT_NAME_ID, R.string.edit_nickname)
            menu.add(0, COURSE_EDIT_COLOR_ID, COURSE_EDIT_COLOR_ID, R.string.edit_course_color)

            // Add click listener
            popup.setOnMenuItemClickListener { item ->
                if(item.itemId == COURSE_EDIT_NAME_ID) {
                    callback?.onEditCourseNickname(course)
                } else if(item.itemId == COURSE_EDIT_COLOR_ID) {
                    callback?.onPickCourseColor(course)
                }
                true
            }

            popup.show()
        }
    }
}
