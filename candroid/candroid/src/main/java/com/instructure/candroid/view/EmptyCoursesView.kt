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
package com.instructure.candroid.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import com.instructure.candroid.R
import com.instructure.pandarecycler.interfaces.EmptyInterface
import com.instructure.pandautils.utils.ViewStyler
import kotlinx.android.synthetic.main.empty_courses_view.view.*


class EmptyCoursesView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), EmptyInterface {

    private var noConnectionText: String? = null
    private var isDisplayNoConnection = false

    init {
        View.inflate(context, R.layout.empty_courses_view, this)
    }

    override fun setLoading() {
        emptyMessageLayout.visibility = View.GONE
        loading.visibility = View.VISIBLE
        loading.announceForAccessibility(context.getString(R.string.loading))
    }

    override fun setDisplayNoConnection(isNoConnection: Boolean) {
        isDisplayNoConnection = isNoConnection
    }

    override fun setListEmpty() {
        if (isDisplayNoConnection) {
            noConnectionView.text = noConnectionText
        }

        emptyMessageLayout.visibility = View.VISIBLE
        loading.visibility = View.GONE
    }

    override fun setTitleText(s: String) {}

    override fun setTitleText(sResId: Int) {}

    override fun setMessageText(s: String) {}

    override fun setMessageText(sResId: Int) {}

    override fun setNoConnectionText(s: String) {
        noConnectionText = s
        noConnectionView.text = s
    }

    override fun getEmptyViewImage(): ImageView? {
        return null
    }

    override fun setEmptyViewImage(drawable: Drawable) {}

    override fun emptyViewText(s: String) {
        setTitleText(s)
    }

    override fun emptyViewText(sResId: Int) {
        setTitleText(sResId)
    }

    override fun emptyViewImage(drawable: Drawable) {
        setEmptyViewImage(drawable)
    }

    fun onClickAddCourses(onClick: () -> Unit) {
        ViewStyler.themeButton(addCoursesButton)
        addCoursesButton.setOnClickListener { onClick() }
    }
}
