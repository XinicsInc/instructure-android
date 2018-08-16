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
package com.instructure.candroid.fragment

import android.app.AlertDialog
import android.app.DialogFragment
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.support.v7.widget.PopupMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.animation.AnimationUtils
import android.widget.Toast
import com.instructure.candroid.R
import com.instructure.candroid.adapter.FileFolderCallback
import com.instructure.candroid.adapter.FileListRecyclerAdapter
import com.instructure.candroid.dialog.EditTextDialog
import com.instructure.candroid.util.*
import com.instructure.canvasapi2.managers.FileFolderManager
import com.instructure.canvasapi2.models.*
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.canvasapi2.utils.pageview.PageView
import com.instructure.canvasapi2.utils.pageview.PageViewEvent
import com.instructure.canvasapi2.utils.pageview.PageViewUrl
import com.instructure.canvasapi2.utils.pageview.PageViewUtils
import com.instructure.canvasapi2.utils.weave.awaitApi
import com.instructure.canvasapi2.utils.weave.catch
import com.instructure.canvasapi2.utils.weave.tryWeave
import com.instructure.pandautils.dialogs.UploadFilesDialog
import com.instructure.candroid.util.AppManager
import com.instructure.candroid.util.DownloadMedia
import com.instructure.candroid.util.FragUtils
import com.instructure.candroid.util.Param
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.FileFolder
import com.instructure.interactions.FragmentInteractions
import com.instructure.pandautils.utils.*
import com.instructure.pandautils.utils.Const
import kotlinx.android.synthetic.main.fragment_file_list.*
import kotlinx.android.synthetic.main.fragment_file_list.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

@PageView
class FileListFragment : ParentFragment() {

    @Suppress("unused")
    @PageViewUrl
    private fun makePageViewUrl() =
        if (canvasContext.type == CanvasContext.Type.USER) {
            "${ApiPrefs.fullDomain}/files"
        } else {
            "${ApiPrefs.fullDomain}/${canvasContext.contextId.replace("_", "s/")}/files"
        }

    private var recyclerAdapter: FileListRecyclerAdapter? = null

    private var folder: FileFolder? by NullableParcelableArg(key = Const.FOLDER)
    private var folderId: Long by LongArg(key = Const.FOLDER_ID)

    private lateinit var adapterCallback: FileFolderCallback
    private var mFabOpen = false

    // FAB animations
    private val fabRotateForward by lazy { AnimationUtils.loadAnimation(activity, R.anim.fab_rotate_forward) }
    private val fabRotateBackwards by lazy { AnimationUtils.loadAnimation(activity, R.anim.fab_rotate_backward) }
    private val fabReveal by lazy { AnimationUtils.loadAnimation(activity, R.anim.fab_reveal) }
    private val fabHide by lazy { AnimationUtils.loadAnimation(activity, R.anim.fab_hide) }

    override fun getFragmentPlacement(): FragmentInteractions.Placement {
        // We only want full screen dialog if its user files
        return if(canvasContext.type == CanvasContext.Type.USER) {
            FragmentInteractions.Placement.FULLSCREEN
        } else {
            FragmentInteractions.Placement.MASTER
        }
    }
    override fun title(): String = getString(R.string.files)
    override fun getSelectedParamName(): String = Param.FILE_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // We only want full screen dialog style if its user files
        if (canvasContext.type == CanvasContext.Type.USER) {
            setStyle(DialogFragment.STYLE_NORMAL, R.style.LightStatusBarDialog)
        }
        setUpCallbacks()
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = layoutInflater.inflate(R.layout.fragment_file_list, container, false)
        rootView.toolbar.title = title()
        rootView.toolbar.subtitle = canvasContext.name
        rootView.addFab.setInvisible()
        return rootView
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (canvasContext.type == CanvasContext.Type.USER) applyTheme()
        if (folder != null) {
            configureViews()
        } else {
            tryWeave {
                folder = if (folderId != 0L) {
                    // If folderId is valid, get folder by ID
                    awaitApi { FileFolderManager.getFolder(folderId, true, it) }
                } else {
                    // Otherwise get root folder of the CanvasContext
                    awaitApi { FileFolderManager.getRootFolderForContext(canvasContext, true, it) }
                }
                configureViews()
            } catch {
                toast(R.string.errorOccurred)
                activity?.onBackPressed()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    private fun setUpCallbacks() {
        adapterCallback = object : FileFolderCallback{

            override fun onItemClicked(item: FileFolder) {
                if (item.fullName != null) {
                    val bundle = createBundle(item, canvasContext)
                    navigation?.addFragment(FragUtils.getFrag(FileListFragment::class.java, bundle))
                } else {
                    recordFilePreviewEvent(item)
                    openMedia(item.contentType, item.url, item.displayName)
                }
            }

            override fun onOpenItemMenu(item: FileFolder, anchorView: View) {
                showOptionMenu(item, anchorView)
            }

            override fun onRefreshFinished() {
                setRefreshing(false)
            }
        }
    }

    private fun recordFilePreviewEvent(file: FileFolder) {
        val event = PageViewEvent("FilePreview", "${makePageViewUrl()}?preview=${file.id}", ApiPrefs.user!!.id)
        PageViewUtils.saveSingleEvent(event)
    }

    override fun applyTheme() {
        themeToolbar()
        if (canvasContext.type == CanvasContext.Type.USER) ViewStyler.setToolbarElevationSmall(context, toolbar)
        toolbar.setupAsBackButton(this)
        ViewStyler.themeFAB(addFab, ThemePrefs.buttonColor)
        ViewStyler.themeFAB(addFileFab, ThemePrefs.buttonColor)
        ViewStyler.themeFAB(addFolderFab, ThemePrefs.buttonColor)
    }

    private fun themeToolbar() {
        // We style the toolbar white for user files
        if (canvasContext.type == CanvasContext.Type.USER) {
            ViewStyler.themeToolbar(activity, toolbar, Color.WHITE, Color.BLACK, false)
        } else {
            ViewStyler.themeToolbar(activity, toolbar, canvasContext)
        }
    }

    private fun configureViews() {
        val isUserFiles = canvasContext.type == CanvasContext.Type.USER

        if (recyclerAdapter == null) {
            recyclerAdapter = FileListRecyclerAdapter(context, canvasContext, isUserFiles, folder!!, adapterCallback)
        }
        configureRecyclerView(view, context, recyclerAdapter, R.id.swipeRefreshLayout, R.id.emptyPandaView, R.id.listView)

        setupToolbarMenu(toolbar)

        // Update toolbar title with folder name if it's not a root folder
        if (!folder!!.isRoot) toolbar.title = folder?.name

        themeToolbar()

        // Only show FAB for user files
        if (isUserFiles) {
            addFab.setVisible()
            addFab.onClickWithRequireNetwork { animateFabs() }
            addFileFab.onClickWithRequireNetwork {
                animateFabs()
                uploadFile()
            }
            addFolderFab.onClickWithRequireNetwork {
                animateFabs()
                createFolder()
            }

            // Add padding to bottom of RecyclerView to account for FAB
            listView.post {
                var bottomPad = addFab.height
                bottomPad += (addFab.layoutParams as? MarginLayoutParams)?.let { it.topMargin + it.bottomMargin }
                        ?: context.DP(32).toInt()
                listView.setPadding(
                        listView.paddingLeft,
                        listView.paddingTop,
                        listView.paddingRight,
                        bottomPad
                )
            }
        }
    }

    private fun showOptionMenu(item: FileFolder, anchorView: View) {
        val popup = PopupMenu(context, anchorView)
        popup.inflate(R.menu.file_folder_options)
        with (popup.menu) {
            // Only show alternate-open option for PDF files
            findItem(R.id.openAlternate).isVisible = item.isFile && "pdf" in item.contentType.orEmpty()

            // Only show download option if it's a file and the download manager is available
            findItem(R.id.download).isVisible = item.isFile && AppManager.isDownloadManagerAvailable(context)
        }

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.openAlternate -> {
                    recordFilePreviewEvent(item)
                    openMedia(item.contentType, item.url, item.displayName, true)
                }
                R.id.download -> downloadItem(item)
                R.id.rename -> renameItem(item)
                R.id.delete -> confirmDeleteItem(item)
            }
            true
        }

        popup.show()
    }

    private fun downloadItem(item: FileFolder) {
        if (PermissionUtils.hasPermissions(activity, PermissionUtils.WRITE_EXTERNAL_STORAGE)) {
            DownloadMedia.downloadMedia(context, item.url, item.displayName, item.displayName)
        } else {
            requestPermissions(PermissionUtils.makeArray(PermissionUtils.WRITE_EXTERNAL_STORAGE), PermissionUtils.WRITE_FILE_PERMISSION_REQUEST_CODE)
        }
    }

    private fun renameItem(item: FileFolder) {
        val title = getString(if (item.isFile) R.string.renameFile else R.string.renameFolder)
        EditTextDialog.show(fragmentManager, title, item.displayName ?: item.name ?: "") {
            if (it.isBlank()) {
                toast(R.string.blankName)
                return@show
            }
            tryWeave {
                val body = UpdateFileFolder(name = it)
                val updateItem: FileFolder = if (item.isFile) {
                    awaitApi { FileFolderManager.updateFile(item.id, body, it) }
                } else {
                    awaitApi { FileFolderManager.updateFolder(item.id, body, it) }
                }
                recyclerAdapter?.add(updateItem)
                StudentPrefs.staleFolderIds = StudentPrefs.staleFolderIds + folder!!.id
            } catch {
                toast(R.string.errorOccurred)
            }
        }
    }

    private fun confirmDeleteItem(item: FileFolder) {
        val message = when {
            item.isFile -> getString(R.string.confirmDeleteFile, item.displayName)
            item.filesCount + item.foldersCount == 0 -> getString(R.string.confirmDeleteEmptyFolder, item.name)
            else -> {
                val itemCount = item.filesCount + item.foldersCount
                resources.getQuantityString(R.plurals.confirmDeleteFolder, itemCount, item.name, itemCount)
            }
        }

        val dialog = AlertDialog.Builder(context)
                .setTitle(R.string.confirm)
                .setMessage(message)
                .setPositiveButton(R.string.delete) { _, _ -> deleteItem(item) }
                .setNegativeButton(R.string.cancel, null)
                .create()

        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(ThemePrefs.buttonColor)
            dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(ThemePrefs.buttonColor)
        }

        dialog.show()
    }

    private fun deleteItem(item: FileFolder) {
        tryWeave {
            val deletedItem: FileFolder = if (item.isFile) {
                awaitApi { FileFolderManager.deleteFile(item.id, it) }
            } else {
                awaitApi { FileFolderManager.deleteFolder(item.id, it) }
            }
            recyclerAdapter?.remove(deletedItem)
            StudentPrefs.staleFolderIds = StudentPrefs.staleFolderIds + folder!!.id
        } catch {
            toast(R.string.errorOccurred)
        }
    }

    private fun uploadFile() {
        folder?.let {
            val bundle = UploadFilesDialog.createFilesBundle(null, it.id)
            UploadFilesDialog.show(fragmentManager, bundle, { _ -> })
        }
    }

    private fun createFolder() {
        EditTextDialog.show(fragmentManager, getString(R.string.createFolder), "") { name ->
            tryWeave {
                val newFolder = awaitApi<FileFolder> {
                    FileFolderManager.createFolder(folder!!.id, CreateFolder(name), it)
                }
                recyclerAdapter?.add(newFolder)
                StudentPrefs.staleFolderIds = StudentPrefs.staleFolderIds + folder!!.id
            } catch {
                toast(R.string.folderCreationError)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionUtils.WRITE_FILE_PERMISSION_REQUEST_CODE) {
            if (PermissionUtils.permissionGranted(permissions, grantResults, PermissionUtils.WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(activity, R.string.filePermissionGranted, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(activity, R.string.filePermissionDenied, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun allowBookmarking(): Boolean {
        return canvasContext.type == CanvasContext.Type.COURSE || canvasContext.type == CanvasContext.Type.GROUP
    }

    private fun animateFabs() = if (mFabOpen) {
        addFab.startAnimation(fabRotateBackwards)
        addFolderFab.startAnimation(fabHide)
        addFolderFab.isClickable = false

        addFileFab.startAnimation(fabHide)
        addFileFab.isClickable = false

        // Needed for accessibility
        addFileFab.setInvisible()
        addFolderFab.setInvisible()
        mFabOpen = false
    } else {
        addFab.startAnimation(fabRotateForward)
        addFolderFab.apply {
            startAnimation(fabReveal)
            isClickable = true
        }

        addFileFab.apply {
            startAnimation(fabReveal)
            isClickable = true
        }

        // Needed for accessibility
        addFileFab.setVisible()
        addFolderFab.setVisible()

        mFabOpen = true
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: FileUploadEvent) {
        event.get {
            recyclerAdapter?.loadData()
            folder?.let {
                StudentPrefs.staleFolderIds = StudentPrefs.staleFolderIds + it.id
            }
        }
    }

    companion object {

        fun newInstance(args: Bundle): FileListFragment {
            val fragment = FileListFragment()
            fragment.arguments = args
            return fragment
        }

        @JvmStatic
        fun createBundle(folder: FileFolder, canvasContext: CanvasContext): Bundle {
            val extras = ParentFragment.createBundle(canvasContext)
            extras.putParcelable(Const.FOLDER, folder)
            return extras
        }

        @JvmStatic
        fun createBundle(folderId: Long, canvasContext: CanvasContext): Bundle {
            val extras = ParentFragment.createBundle(canvasContext)
            extras.putLong(Const.FOLDER_ID, folderId)
            return extras
        }
    }
}