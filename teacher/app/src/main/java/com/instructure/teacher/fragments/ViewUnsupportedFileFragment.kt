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
import android.net.Uri
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.instructure.pandautils.utils.*
import com.instructure.teacher.R
import com.instructure.teacher.events.FileFolderDeletedEvent
import com.instructure.teacher.events.FileFolderUpdatedEvent
import com.instructure.teacher.models.EditableFile
import com.instructure.interactions.router.Route
import com.instructure.teacher.router.RouteMatcher
import com.instructure.teacher.utils.*
import kotlinx.android.synthetic.main.fragment_unsupported_file_type.*
import org.greenrobot.eventbus.EventBus

class ViewUnsupportedFileFragment : Fragment() {

    private var mUri by ParcelableArg(Uri.EMPTY)
    private var mDisplayName by StringArg()
    private var mContentType by StringArg()
    private var mPreviewUri by ParcelableArg(Uri.EMPTY)
    private var mFallbackIcon by IntArg(R.drawable.vd_utils_attachment)
    private var mEditableFile: EditableFile? by NullableParcelableArg()
    private var mToolbarColor by IntArg(0)

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_unsupported_file_type, container, false)
    }

    override fun onResume() {
        super.onResume()
        // If returning from editing this file, check if it was deleted so we can immediately go back
        val fileFolderDeletedEvent = EventBus.getDefault().getStickyEvent(FileFolderDeletedEvent::class.java)
        if (fileFolderDeletedEvent != null)
            activity.finish()

        mEditableFile?.let { setupToolbar() } ?: toolbar.setGone()
    }
    private fun setupToolbar() {

        mEditableFile?.let {
            // Check if we need to update the file name
            val fileFolderUpdatedEvent = EventBus.getDefault().getStickyEvent(FileFolderUpdatedEvent::class.java)
            fileFolderUpdatedEvent?.let { event ->
                it.file = event.updatedFileFolder
            }

            toolbar.title = it.file.displayName

            //update the name that is displayed above the open button
            fileNameView.text = it.file.displayName
            toolbar.setupMenu(R.menu.menu_edit_generic) { _ ->
                val args = EditFileFolderFragment.makeBundle(it.file, it.usageRights, it.licenses, it.canvasContext!!.id)
                RouteMatcher.route(context, Route(EditFileFolderFragment::class.java, it.canvasContext, args))
            }
        }

        if(isTablet && mToolbarColor != 0) {
            ViewStyler.themeToolbar(activity, toolbar, mToolbarColor, Color.WHITE)
        } else {
            toolbar.setupBackButton {
                activity.onBackPressed()
            }
            ViewStyler.themeToolbar(activity, toolbar, Color.WHITE, Color.BLACK)
            ViewStyler.setToolbarElevationSmall(context, toolbar)
        }
    }

    override fun onStart() {
        super.onStart()
        Glide.with(context)
                .load(mPreviewUri)
                .apply(RequestOptions().error(mFallbackIcon))
                .into(previewImageView.setVisible())
        fileNameView.text = mDisplayName
        ViewStyler.themeButton(openExternallyButton)
        openExternallyButton.onClick { mUri.viewExternally(context, mContentType) }
    }

    companion object {
        @JvmStatic
        @JvmOverloads
        fun newInstance(
                uri: Uri,
                displayName: String,
                contentType: String,
                previewUri: Uri?,
                @DrawableRes fallbackIcon: Int,
                toolbarColor: Int = 0,
                editableFile: EditableFile? = null
        ) = ViewUnsupportedFileFragment().apply {
            mUri = uri
            mDisplayName = displayName
            mContentType = contentType
            if (previewUri != null) mPreviewUri = previewUri
            mFallbackIcon = fallbackIcon
            mToolbarColor = toolbarColor
            mEditableFile = editableFile
        }

        @JvmStatic
        fun newInstance(bundle: Bundle) = ViewUnsupportedFileFragment().apply { arguments = bundle }
    }
}
