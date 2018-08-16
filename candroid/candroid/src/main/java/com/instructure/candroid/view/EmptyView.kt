/*
 * Copyright (C) 2018 - present  Instructure, Inc.
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
import com.instructure.pandautils.utils.setGone
import com.instructure.pandautils.utils.setVisible
import kotlinx.android.synthetic.main.empty_view.view.*
import kotlinx.android.synthetic.main.loading_lame.view.*

class EmptyView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), EmptyInterface {

    private var noConnectionText: String? = null
    private var titleText: String? = null
    private var messageText: String? = null
    private var isDisplayNoConnection = false

    init {
        View.inflate(context, R.layout.empty_view, this)
    }

    override fun setLoading() {
        title.setGone()
        message.setGone()
        image.setGone()
        loading.announceForAccessibility(context.getString(R.string.loading))
        loading.setVisible()
    }

    override fun setDisplayNoConnection(isNoConnection: Boolean) {
        isDisplayNoConnection = isNoConnection
    }

    override fun setListEmpty() {
        if (isDisplayNoConnection) {
            noConnection.text = noConnectionText
        } else {
            title.text = titleText
            message.text = messageText
        }
        title.setVisible()
        message.setVisible()
        loading.setGone()
        image.setVisible(image.drawable != null)
    }

    override fun setTitleText(s: String) {
        titleText = s
        title.text = titleText
    }

    override fun setTitleText(sResId: Int) {
        titleText = context.resources.getString(sResId)
        title.text = titleText
    }

    override fun setMessageText(s: String) {
        messageText = s
        message.text = messageText
    }

    override fun setMessageText(sResId: Int) {
        messageText = context.resources.getString(sResId)
        message.text = messageText
    }

    override fun setNoConnectionText(s: String) {
        noConnectionText = s
        noConnection.text = noConnectionText
    }

    override fun getEmptyViewImage(): ImageView? = image

    override fun setEmptyViewImage(drawable: Drawable) {
        image.setImageDrawable(drawable)
    }

    override fun emptyViewText(s: String) {
        setTitleText(s)
    }

    override fun emptyViewText(sResId: Int) {
        setTitleText(sResId)
    }

    override fun emptyViewImage(drawable: Drawable) {
        setEmptyViewImage(drawable)
    }
}
