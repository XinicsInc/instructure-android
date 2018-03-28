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
 *
 */
package com.instructure.teacher.fragments

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.graphics.Palette
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.instructure.pandautils.utils.*
import com.instructure.teacher.R
import com.instructure.teacher.events.FileFolderDeletedEvent
import com.instructure.teacher.events.FileFolderUpdatedEvent
import com.instructure.teacher.interfaces.ShareableFile
import com.instructure.teacher.models.EditableFile
import com.instructure.interactions.router.Route
import com.instructure.teacher.router.RouteMatcher
import com.instructure.teacher.utils.*
import kotlinx.android.synthetic.main.fragment_view_image.*
import org.greenrobot.eventbus.EventBus

class ViewImageFragment : Fragment(), ShareableFile {

    private var mUri by ParcelableArg(Uri.EMPTY)
    private var mContentType by StringArg()
    private var mTitle by StringArg()
    private var mShowToolbar by BooleanArg()
    private var mToolbarColor by IntArg()
    private var mEditableFile: EditableFile? by NullableParcelableArg()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_view_image, container, false)

    override fun onResume() {
        super.onResume()

        // If returning from editing this file, check if it was deleted so we can immediately go back
        val fileFolderDeletedEvent = EventBus.getDefault().getStickyEvent(FileFolderDeletedEvent::class.java)
        if (fileFolderDeletedEvent != null)
            activity.finish()

        if (mShowToolbar) setupToolbar() else toolbar.setGone()
    }

    private fun setupToolbar() {

        mEditableFile?.let {

            // Check if we need to update the file name
            val fileFolderUpdatedEvent = EventBus.getDefault().getStickyEvent(FileFolderUpdatedEvent::class.java)
            fileFolderUpdatedEvent?.let { event ->
                it.file = event.updatedFileFolder
            }

            toolbar.title = it.file.displayName
            toolbar.setupMenu(R.menu.menu_edit_generic) { _ ->
                val args = EditFileFolderFragment.makeBundle(it.file, it.usageRights, it.licenses, it.canvasContext!!.id)
                RouteMatcher.route(context, Route(EditFileFolderFragment::class.java, it.canvasContext, args))
            }
        }

        if (isTablet && mToolbarColor != 0) {
            ViewStyler.themeToolbar(activity, toolbar, mToolbarColor, Color.WHITE)
        } else {
            toolbar.setupBackButton {
                activity.onBackPressed()
            }
            ViewStyler.themeToolbar(activity, toolbar, Color.WHITE, Color.BLACK)
            ViewStyler.setToolbarElevationSmall(context, toolbar)
        }
    }

    private val requestListener = object : RequestListener<Bitmap> {

        override fun onLoadFailed(p0: GlideException?, p1: Any?, p2: Target<Bitmap>?, p3: Boolean): Boolean {
            photoView.setGone()
            progressBar.setGone()
            errorContainer.setVisible()
            ViewStyler.themeButton(openExternallyButton)
            openExternallyButton.onClick { mUri.viewExternally(context, mContentType) }
            return false
        }

        override fun onResourceReady(bitmap: Bitmap?, p1: Any?, p2: Target<Bitmap>?, p3: DataSource?, p4: Boolean): Boolean {
            progressBar.setGone()

            // Try to set the background color using palette if we can
            bitmap?.let { colorBackground(it) }
            return false
        }
    }

    override fun onStart() {
        super.onStart()
        progressBar.announceForAccessibility(getString(R.string.loading))
        Glide.with(this)
                .asBitmap()
                .load(mUri)
                .listener(requestListener)
                .into(photoView)
    }

    override fun viewExternally() {
        mUri.viewExternally(context, mContentType)
    }

    fun colorBackground(bitmap: Bitmap) {
        // Generate palette asynchronously
        Palette.from(bitmap).generate({ viewImageRootView.setBackgroundColor(it.getDarkMutedColor(Color.WHITE)) })
    }

    companion object {
        @JvmStatic
        @JvmOverloads
        fun newInstance(title: String, uri: Uri, contentType: String, showToolbar: Boolean = true, toolbarColor: Int = 0, editableFile: EditableFile? = null) = ViewImageFragment().apply {
            mTitle = title
            mUri = uri
            mContentType = contentType
            mShowToolbar = showToolbar
            mToolbarColor = toolbarColor
            mEditableFile = editableFile
        }

        @JvmStatic
        fun newInstance(bundle: Bundle) = ViewImageFragment().apply { arguments = bundle }
    }
}
