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

package com.instructure.candroid.holders

import android.support.v7.widget.RecyclerView
import android.view.View
import com.instructure.candroid.R
import com.instructure.candroid.interfaces.CourseAdapterToFragmentCallback
import com.instructure.pandautils.utils.ThemePrefs
import com.instructure.pandautils.utils.onClick
import kotlinx.android.synthetic.main.viewholder_course_header.view.*

class CourseHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    companion object {
        fun holderResId(): Int = R.layout.viewholder_course_header
    }

    fun bind(callback: CourseAdapterToFragmentCallback) = with(itemView) {
        seeAllTextView.setTextColor(ThemePrefs.buttonColor)
        seeAllTextView.onClick { callback.onSeeAllCourses() }
    }

}
