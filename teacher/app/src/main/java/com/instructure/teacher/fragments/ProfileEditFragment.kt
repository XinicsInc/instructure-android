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
package com.instructure.teacher.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.text.InputType
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.instructure.canvasapi2.StatusCallback
import com.instructure.canvasapi2.managers.FileUploadManager
import com.instructure.canvasapi2.managers.UserManager
import com.instructure.canvasapi2.models.AvatarWrapper
import com.instructure.canvasapi2.models.User
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.canvasapi2.utils.ApiPrefs.user
import com.instructure.canvasapi2.utils.ApiType
import com.instructure.canvasapi2.utils.LinkHeaders
import com.instructure.canvasapi2.utils.validOrNull
import com.instructure.pandautils.fragments.BasePresenterFragment
import com.instructure.pandautils.utils.*
import com.instructure.pandautils.utils.Const
import com.instructure.pandautils.utils.ProfileUtils
import com.instructure.teacher.R
import com.instructure.teacher.factory.ProfileEditFragmentPresenterFactory
import com.instructure.teacher.presenters.ProfileEditFragmentPresenter
import com.instructure.teacher.utils.*
import com.instructure.teacher.viewinterface.ProfileEditFragmentView
import kotlinx.android.synthetic.main.fragment_profile_edit.*
import retrofit2.Response
import java.io.File

class ProfileEditFragment : BasePresenterFragment<
        ProfileEditFragmentPresenter,
        ProfileEditFragmentView>(), ProfileEditFragmentView, android.support.v4.app.LoaderManager.LoaderCallbacks<AvatarWrapper> {


    private var mLoaderBundle: Bundle? = null

    override fun getPresenterFactory() = ProfileEditFragmentPresenterFactory()

    override fun onReadySetGo(presenter: ProfileEditFragmentPresenter?) {
        presenter?.loadData(false)
    }

    private val saveButton: TextView? get() = view?.findViewById(R.id.menu_save)

    override fun layoutResId() = R.layout.fragment_profile_edit

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        profileBanner.setImageResource(
                if(isTablet) R.drawable.teacher_profile_banner_image_tablet
                else R.drawable.teacher_profile_banner_image_phone)

        val user = ApiPrefs.user

        if(ProfileUtils.shouldLoadAltAvatarImage(user?.avatarUrl)) {
            val initials = ProfileUtils.getUserInitials(user?.shortName ?: "")
            val color = ContextCompat.getColor(context, R.color.defaultTextGray)
            val drawable = TextDrawable.builder()
                    .beginConfig()
                    .height(context.resources.getDimensionPixelSize(R.dimen.profileAvatarSize))
                    .width(context.resources.getDimensionPixelSize(R.dimen.profileAvatarSize))
                    .toUpperCase()
                    .useFont(Typeface.DEFAULT_BOLD)
                    .textColor(color)
                    .endConfig()
                    .buildRound(initials, Color.WHITE)
            usersAvatar.borderColor = ContextCompat.getColor(context, R.color.defaultTextGray)
            usersAvatar.borderWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6F, context.resources.displayMetrics).toInt()
            usersAvatar.setImageDrawable(drawable)
        } else {
            updateAvatarImage(user?.avatarUrl)
        }

        usersName.setText(user?.shortName)
        usersName.hint = user?.shortName

        ViewStyler.themeEditText(context, usersName, ThemePrefs.brandColor)
        ViewStyler.colorImageView(profileCameraIcon, ThemePrefs.buttonColor)
        ViewStyler.themeProgressBar(profileCameraLoadingIndicator, ThemePrefs.brandColor)

        //Restore loader if necessary
        LoaderUtils.restoreLoaderFromBundle(loaderManager, savedInstanceState, this, R.id.avatarLoaderId)
        activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        LoaderUtils.saveLoaderBundle(outState, mLoaderBundle)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        setupToolbar()
    }

    fun setupToolbar() {
        toolbar.setupCloseButton(this)
        toolbar.title = getString(R.string.editProfile)
        toolbar.setupMenu(R.menu.menu_save_generic) { saveProfile() }
        ViewStyler.themeToolbarBottomSheet(activity, isTablet, toolbar, Color.BLACK, false)
        ViewStyler.setToolbarElevationSmall(context, toolbar)
        saveButton?.setTextColor(ThemePrefs.buttonColor)
    }

    override fun readyToLoadUI(user: User?) {
        profileCameraIconWrapper.setVisible(user?.canUpdateAvatar())
        profileCameraIconWrapper.onClickWithRequireNetwork {
            pickAvatar()
        }
        if(profileCameraLoadingIndicator.isShown) { profileCameraLoadingIndicator.announceForAccessibility(getString(R.string.loading))}

        if(user != null && !user.canUpdateName()) {
            usersName.isEnabled = false
            usersName.inputType = InputType.TYPE_NULL
        }
    }

    private fun saveProfile(){
        val name = usersName.text.toString().validOrNull() ?: user?.shortName ?: ""
        presenter.saveChanges(name, user?.bio ?: "")
    }

    @SuppressLint("InflateParams")
    private fun pickAvatar() {
        val root = LayoutInflater.from(context).inflate(R.layout.profile_choose_photo, null)
        val dialog = AlertDialog.Builder(context)
                .setView(root)
                .create()
        root.findViewById<View>(R.id.takePhotoItem).onClick {
            newPhoto()
            dialog.dismiss()
        }
        root.findViewById<View>(R.id.chooseFromGalleryItem).onClick {
            chooseFromGallery()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun updateAvatarImage(url: String?) {
        usersAvatar.borderColor = Color.WHITE
        usersAvatar.borderWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6F, context.resources.displayMetrics).toInt()
        Glide.with(context).load(url).into(usersAvatar)
    }

    private fun chooseFromGallery() {
        if (PermissionUtils.hasPermissions(activity, PermissionUtils.WRITE_EXTERNAL_STORAGE)) {
            chooseFromGalleryBecausePermsissionsAlreadyGranted()
        } else {
            requestPermissions(PermissionUtils.makeArray(PermissionUtils.WRITE_EXTERNAL_STORAGE), PermissionUtils.WRITE_FILE_PERMISSION_REQUEST_CODE)
        }
    }

    private fun newPhoto() {
        //check to see if the device has a camera
        if (!Utils.hasCameraAvailable(activity)) {
            //this device doesn't have a camera, show a crouton that lets the user know
            showToast(R.string.noCameraOnDevice)
            return
        }

        if (PermissionUtils.hasPermissions(activity, PermissionUtils.WRITE_EXTERNAL_STORAGE, PermissionUtils.CAMERA)) {
            takeNewPhotoBecausePermissionsAlreadyGranted()
        } else {
            requestPermissions(PermissionUtils.makeArray(PermissionUtils.WRITE_EXTERNAL_STORAGE, PermissionUtils.CAMERA), PermissionUtils.PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionUtils.PERMISSION_REQUEST_CODE) {
            if (PermissionUtils.allPermissionsGrantedResultSummary(grantResults)) {
                takeNewPhotoBecausePermissionsAlreadyGranted()
            } else {
                Toast.makeText(activity, R.string.permissionDenied, Toast.LENGTH_LONG).show()
            }
        } else if(requestCode == PermissionUtils.WRITE_FILE_PERMISSION_REQUEST_CODE) {
            if (PermissionUtils.allPermissionsGrantedResultSummary(grantResults)) {
                chooseFromGalleryBecausePermsissionsAlreadyGranted()
            } else {
                Toast.makeText(activity, R.string.permissionDenied, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun takeNewPhotoBecausePermissionsAlreadyGranted() {
        //let the user take a picture
        //get the location of the saved picture
        val fileName = "profilePic_" + System.currentTimeMillis().toString() + ".jpg"
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, fileName)

        presenter?.capturedImageUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        if (presenter?.capturedImageUri != null) {
            //save the intent information in case we get booted from memory.
            FilePrefs.tempCaptureUri = presenter?.capturedImageUri.toString()
        }
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, presenter?.capturedImageUri)
        cameraIntent.putExtra(Const.IS_OVERRIDDEN, true)
        cameraIntent.putExtra("android.intent.extras.CAMERA_FACING", 1) //Requests front camera on some apps
        startActivityForResult(cameraIntent, RequestCodes.CAMERA_PIC_REQUEST)
    }

    private fun chooseFromGalleryBecausePermsissionsAlreadyGranted() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        val file = File(context.filesDir, "/image/*")
        intent.setDataAndType(FileProvider.getUriForFile(context, context.applicationContext.packageName + Const.FILE_PROVIDER_AUTHORITY, file), "image/*")
        startActivityForResult(intent, RequestCodes.PICK_IMAGE_GALLERY)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        setupToolbar()

        if (requestCode == RequestCodes.CROP_IMAGE && resultCode == Activity.RESULT_OK) {
            data?.data?.let {
                mLoaderBundle = createLoaderBundle("profilePic.jpg", "image/jpeg", it.path, File(it.path).length())
                showProgressBar()
                LoaderUtils.restartLoaderWithBundle(loaderManager, mLoaderBundle, this, R.id.avatarLoaderId)
            }

        } else if (requestCode == RequestCodes.CAMERA_PIC_REQUEST && resultCode == Activity.RESULT_OK) {
            if (presenter?.capturedImageUri == null) {
                presenter?.capturedImageUri = Uri.parse(FilePrefs.tempCaptureUri)
            }

            if (presenter?.capturedImageUri == null) {
                showToast(R.string.errorGettingPhoto)
                return
            }

            presenter?.capturedImageUri?.let {
                val cropConfig = AvatarCropConfig(it)
                startActivityForResult(AvatarCropActivity.createIntent(context, cropConfig), RequestCodes.CROP_IMAGE)
            }

        } else if (requestCode == RequestCodes.PICK_IMAGE_GALLERY && resultCode == Activity.RESULT_OK && data != null) {
            val u = data.data
            var urlPath = u.path

            if (u.path.contains("googleusercontent")) {
                urlPath = changeGoogleURL(urlPath)
                ApiPrefs.user?.avatarUrl = urlPath
                UserManager.updateUsersAvatar(urlPath, mAvatarPostedCallback)
                return
            }

            val cropConfig = AvatarCropConfig(u)
            startActivityForResult(AvatarCropActivity.createIntent(context, cropConfig), RequestCodes.CROP_IMAGE)
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun successSavingProfile() {
        Toast.makeText(context, R.string.profileEditSuccess, Toast.LENGTH_SHORT).show()
        activity.onBackPressed()
    }

    override fun errorSavingProfile() {
        Toast.makeText(context, R.string.profileEditFailure, Toast.LENGTH_SHORT).show()
    }

    companion object {
        @JvmStatic
        fun newInstance(args: Bundle) = ProfileEditFragment().apply {
            arguments = args
        }
    }

    override fun onRefreshStarted() {}

    override fun onRefreshFinished() {}

    override fun onPresenterPrepared(presenter: ProfileEditFragmentPresenter?) {}

    private fun hideProgressBar() {
        profileCameraLoadingIndicator.setGone()
        profileCameraIcon.setVisible()
        profileCameraIconWrapper.isClickable = true
    }

    private fun showProgressBar() {
        profileCameraLoadingIndicator.setVisible()
        profileCameraIcon.setGone()
        profileCameraIconWrapper.isClickable = false
    }

    //region Avatar Resizing, Conversion and Helpers

    private fun changeGoogleURL(url: String): String {
        val start = url.indexOf("http")
        val end = url.indexOf("-d")
        return url.substring(start, end + 1)
    }

    //endregion

    //region Loaders and Callbacks

    class PostAvatarLoader(context: Context, internal val name: String, internal val contentType: String, internal val path: String, internal val size: Long) :
            android.support.v4.content.AsyncTaskLoader<AvatarWrapper>(context) {

        override fun loadInBackground() = FileUploadManager.uploadAvatarSynchronous(name, size, contentType, path)

        override fun onStopLoading() {
            // Attempt to cancel the current load task if possible.
            cancelLoad()
        }
    }

    /**
     * Used to instantiate the PostAvatarLoader
     * @param id - a unique ID
     * *
     * @param args - a bundle, containing:
     * *             -String name
     * *             -String content type
     * *             -String path
     * *             -int size
     * *
     * @return
     */
    override fun onCreateLoader(id: Int, args: Bundle) = PostAvatarLoader(
            activity,
            args.getString(Const.NAME),
            args.getString(Const.CONTENT_TYPE),
            args.getString(Const.PATH),
            args.getLong(Const.SIZE)
    )

    override fun onLoadFinished(loader: android.support.v4.content.Loader<AvatarWrapper>, data: AvatarWrapper?) {
        hideProgressBar()

        data?.avatar?.let {
            ApiPrefs.user?.avatarUrl = it.url
            updateAvatarImage(it.url)
            //Notify canvas of the change in avatar url
            UserManager.updateUsersAvatar(it.url, mAvatarPostedCallback)
        } ?: data?.let {
            //check to see the error messages
            when(it.error) {
                AvatarWrapper.ERROR_QUOTA_EXCEEDED -> showToast(R.string.fileQuotaExceeded)
                AvatarWrapper.ERROR_UNKNOWN -> showToast(R.string.errorUploadingFile)
            }
        }
    }

    override fun onLoaderReset(loader: android.support.v4.content.Loader<AvatarWrapper>) {}

    private fun createLoaderBundle(name: String, contentType: String, path: String, size: Long): Bundle {
        val bundle = Bundle()
        bundle.putString(Const.NAME, name)
        bundle.putString(Const.CONTENT_TYPE, contentType)
        bundle.putString(Const.PATH, path)
        bundle.putLong(Const.SIZE, size)
        return bundle
    }

    private val mAvatarPostedCallback = object: StatusCallback<User>() {
        override fun onResponse(response: Response<User>, linkHeaders: LinkHeaders, type: ApiType) {
            response.body()?.let {
                ApiPrefs.user = it
                updateAvatarImage(it.avatarUrl)
            }
        }
    }
    //endregion
}


