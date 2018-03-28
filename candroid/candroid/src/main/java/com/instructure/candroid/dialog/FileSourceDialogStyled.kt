/*
 * Copyright (C) 2016 - present Instructure, Inc.
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

package com.instructure.candroid.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.DialogFragment
import android.support.v4.content.FileProvider
import android.support.v7.app.AlertDialog
import android.support.v7.content.res.AppCompatResources
import android.view.LayoutInflater
import android.widget.Toast
import com.instructure.candroid.R
import com.instructure.candroid.util.StudentPrefs
import com.instructure.pandautils.utils.Const
import com.instructure.pandautils.utils.PermissionUtils
import com.instructure.pandautils.utils.RequestCodes
import kotlinx.android.synthetic.main.file_source_dialog.view.*
import java.io.File

class FileSourceDialogStyled : DialogFragment() {
    // Current Permission must be set before making a permission request
    private var mCurrentPermissionRequest = 0
    private var mCapturedImageURI: Uri? = null
    private var listener: SourceClickedListener? = null

    fun setListener(listener: SourceClickedListener) {
        this.listener = listener
    }

    interface SourceClickedListener {
        fun onSourceSelected(intent: Intent, requestCode: Int)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = activity
        val builder = AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.chooseFileSource))

        @SuppressLint("InflateParams") // Suppress lint warning about null parent when inflating layout
        val view = LayoutInflater.from(getActivity()).inflate(R.layout.file_source_dialog, null)

        with (view) {
            fromCamera.setCompoundDrawablesWithIntrinsicBounds(null, AppCompatResources.getDrawable(context, R.drawable.vd_camera), null, null)
            fromCamera.setOnClickListener {
                if (PermissionUtils.hasPermissions(getActivity(), PermissionUtils.WRITE_EXTERNAL_STORAGE, PermissionUtils.CAMERA)) {
                    pickFromCameraBecausePermissionsAlreadyGranted()
                } else {
                    // Prepare state for onResume
                    mCurrentPermissionRequest = CAMERA_REQUEST
                    requestPermissions(PermissionUtils.makeArray(PermissionUtils.WRITE_EXTERNAL_STORAGE, PermissionUtils.CAMERA), PermissionUtils.PERMISSION_REQUEST_CODE)
                }
            }

            fromDevice.setCompoundDrawablesWithIntrinsicBounds(null, AppCompatResources.getDrawable(context, R.drawable.vd_files), null, null)
            fromDevice.setOnClickListener { pickFromDevice() }

            fromOtherApplication.setCompoundDrawablesWithIntrinsicBounds(null, AppCompatResources.getDrawable(context, R.drawable.vd_share), null, null)
            fromOtherApplication.setOnClickListener {
                dismiss()
                ShareInstructionsDialogStyled.show(getActivity())
            }

            fromPhotoGallery.setCompoundDrawablesWithIntrinsicBounds(null, AppCompatResources.getDrawable(context, R.drawable.vd_image), null, null)
            fromPhotoGallery.setOnClickListener {
                if (PermissionUtils.hasPermissions(getActivity(), PermissionUtils.WRITE_EXTERNAL_STORAGE, PermissionUtils.CAMERA)) {
                    pickFromGallery()
                } else {
                    // Prepare state for onResume
                    mCurrentPermissionRequest = GALLERY_REQUEST
                    requestPermissions(PermissionUtils.makeArray(PermissionUtils.WRITE_EXTERNAL_STORAGE, PermissionUtils.CAMERA), PermissionUtils.PERMISSION_REQUEST_CODE)
                }
            }
        }

        builder.setView(view)
        return builder.create()
    }

    override fun onDestroyView() {
        if (retainInstance)
            dialog?.setDismissMessage(null)
        super.onDestroyView()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionUtils.PERMISSION_REQUEST_CODE) {
            if (PermissionUtils.allPermissionsGrantedResultSummary(grantResults)) {
                // Finalize state for onResume
                when (mCurrentPermissionRequest) {
                    CAMERA_REQUEST -> mCameraPermissions = true
                    GALLERY_REQUEST -> {
                        mGalleryPermissions = true
                    }
                }
            } else {
                Toast.makeText(activity, R.string.permissionDenied, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // http://stackoverflow.com/questions/37164415/android-fatal-error-can-not-perform-this-action-after-onsaveinstancestate
        // The activity will be restarted as a result of these permission requests, both of these
        // cases require a clean activity as they perform fragment transactions
        if (mCameraPermissions) {
            pickFromCameraBecausePermissionsAlreadyGranted()
            mCameraPermissions = false
        }

        if (mGalleryPermissions) {
            pickFromGallery()
            mGalleryPermissions = false
        }
    }

    private fun pickFromCameraBecausePermissionsAlreadyGranted() {
        // Let the user take a picture
        // Get the location of the saved picture
        val fileName = "pic_" + System.currentTimeMillis().toString() + ".jpg"
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, fileName)

        try {
            mCapturedImageURI = activity.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        } catch (e: Exception) {
            Toast.makeText(activity, R.string.errorOccurred, Toast.LENGTH_SHORT).show()
            return
        }

        if (mCapturedImageURI != null) {
            // Save the intent information in case we get booted from memory.
            StudentPrefs.tempCaptureUri = mCapturedImageURI!!.toString()
        }

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI)

        if (isIntentAvailable(activity, cameraIntent.action) && listener != null) {
            listener!!.onSourceSelected(cameraIntent, RequestCodes.CAMERA_PIC_REQUEST)
            dismiss()
        }
    }

    private fun pickFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        val file = File(context.filesDir, "/image/*")
        intent.setDataAndType(FileProvider.getUriForFile(context, context.applicationContext.packageName + Const.FILE_PROVIDER_AUTHORITY, file), "image/*")

        listener?.onSourceSelected(intent, RequestCodes.PICK_IMAGE_GALLERY)

        dismiss()
    }

    private fun pickFromDevice() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)

        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        listener?.onSourceSelected(intent, RequestCodes.PICK_FILE_FROM_DEVICE)

        dismiss()
    }

    companion object {
        const val CAMERA_REQUEST = 123
        const val GALLERY_REQUEST = 124
        const val TAG = "fileSourceDialog"

        // Data
        private var mCameraPermissions = false
        private var mGalleryPermissions = false

        fun newInstance(): FileSourceDialogStyled = FileSourceDialogStyled()

        ///////////////////////////////////////////////////////////////////////////
        // Helpers
        ///////////////////////////////////////////////////////////////////////////

        private fun isIntentAvailable(context: Context, action: String?): Boolean {
            val packageManager = context.packageManager
            val intent = Intent(action)
            val list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            return list.size > 0
        }
    }
}
