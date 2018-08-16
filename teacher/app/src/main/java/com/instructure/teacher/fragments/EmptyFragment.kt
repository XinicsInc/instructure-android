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

import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.instructure.canvasapi2.models.Course
import com.instructure.pandautils.utils.ThemePrefs
import com.instructure.pandautils.utils.ViewStyler
import com.instructure.teacher.R
import com.instructure.pandautils.utils.NullableParcelableArg
import com.instructure.pandautils.utils.StringArg
import com.instructure.pandautils.utils.color
import kotlinx.android.synthetic.main.fragment_empty.*

class EmptyFragment: Fragment() {

    private var mCourse: Course? by NullableParcelableArg()
    private var mTitle: String by StringArg()

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_empty, container, false)
    }

    override fun onResume() {
        super.onResume()
        setupToolbar()
    }

    private fun setupToolbar() {
        toolbar.title = ""
        emptyTitle.text = mTitle
        emptyMessage.text = getString(R.string.emptyDetailsMessage)
        ViewStyler.themeToolbar(activity, toolbar, mCourse?.color ?: ThemePrefs.primaryColor, Color.WHITE)
    }

    companion object {
        @JvmStatic
        fun newInstance(course: Course, title: String) = EmptyFragment().apply {
            mCourse = course
            mTitle = title
        }

        @JvmStatic
        fun newInstance(title: String) = EmptyFragment().apply {
            mTitle = title
        }
    }
}
