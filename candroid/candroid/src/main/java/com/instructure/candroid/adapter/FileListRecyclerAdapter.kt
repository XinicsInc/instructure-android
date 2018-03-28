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
package com.instructure.candroid.adapter

import android.content.Context
import android.view.View
import com.instructure.candroid.holders.FileViewHolder
import com.instructure.candroid.util.StudentPrefs
import com.instructure.canvasapi2.managers.FileFolderManager
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.FileFolder
import com.instructure.canvasapi2.utils.weave.WeaveJob
import com.instructure.canvasapi2.utils.weave.awaitPaginated
import com.instructure.canvasapi2.utils.weave.catch
import com.instructure.canvasapi2.utils.weave.tryWeave
import com.instructure.pandautils.utils.ColorKeeper

open class FileListRecyclerAdapter(
    context: Context,
    canvasContext: CanvasContext,
    private val allowOptionMenu: Boolean,
    private val folder: FileFolder,
    private val fileFolderCallback: FileFolderCallback
) : BaseListRecyclerAdapter<FileFolder, FileViewHolder>(context, FileFolder::class.java) {

    private var isTesting = false
    private val contextColor = ColorKeeper.getOrGenerateColor(canvasContext)

    private var apiCall: WeaveJob? = null

    /* This overloaded constructor is for testing purposes ONLY, and should not be used to create instances of this adapter. */
    protected constructor(
        context: Context,
        canvasContext: CanvasContext,
        allowOptionMenu: Boolean,
        folder: FileFolder,
        itemCallback: FileFolderCallback,
        isTesting: Boolean
    ) : this(context, canvasContext, allowOptionMenu, folder, itemCallback) {
        this.isTesting = isTesting
    }

    init {
        itemCallback = object : BaseListRecyclerAdapter.ItemComparableCallback<FileFolder>() {
            override fun compare(o1: FileFolder, o2: FileFolder) = o1.compareTo(o2)
            override fun areContentsTheSame(item1: FileFolder, item2: FileFolder) = compareFileFolders(item1, item2)
            override fun areItemsTheSame(item1: FileFolder, item2: FileFolder) = item1.id == item2.id
            override fun getUniqueItemId(fileFolder: FileFolder) = fileFolder.id
        }
        if (!isTesting) loadData()
    }

    override fun bindHolder(item: FileFolder, holder: FileViewHolder, position: Int) {
        // Student cannot edit or delete submission folders/files so don't show the option menu for these
        val isForSubmissions = item.forSubmissions || folder.forSubmissions
        holder.bind( item, contextColor, context, allowOptionMenu && !isForSubmissions, fileFolderCallback)
    }

    override fun createViewHolder(v: View, viewType: Int) = FileViewHolder(v)

    override fun itemLayoutResId(viewType: Int) = FileViewHolder.holderResId()

    override fun isPaginated() = true

    override fun loadFirstPage() {
        apiCall?.cancel()
        apiCall = tryWeave {

            // Check if the folder is marked as stale (i.e. items were added/changed/removed)
            val isStale = StudentPrefs.staleFolderIds?.contains(folder.id) == true

            // Force network for pull-to-refresh and stale folders
            val forceNetwork = isRefresh || isStale

            // Get folders
            awaitPaginated<List<FileFolder>> {
                onRequestFirst { FileFolderManager.getFirstPageFolders(folder.id, forceNetwork, it) }
                onRequestNext { url, callback -> FileFolderManager.getNextPageFilesFolder(url, forceNetwork, callback) }
                onResponse {
                    setNextUrl("")
                    addAll(it)
                }
            }

            // Get files
            awaitPaginated<List<FileFolder>> {
                onRequestFirst { FileFolderManager.getFirstPageFiles(folder.id, forceNetwork, it) }
                onRequestNext { url, callback -> FileFolderManager.getNextPageFilesFolder(url, forceNetwork, callback) }
                onResponse {
                    setNextUrl("")
                    addAll(it)
                }
            }

            // Mark folder as no longer stale
            if (isStale) StudentPrefs.staleFolderIds = StudentPrefs.staleFolderIds - folder.id

            isAllPagesLoaded = true
            setNextUrl(null)
            fileFolderCallback.onRefreshFinished()
            onCallbackFinished()
        } catch {
            fileFolderCallback.onRefreshFinished()
            onCallbackFinished()
        }
    }

    override fun loadNextPage(nextURL: String) {
        apiCall?.next()
    }

    private fun compareFileFolders(oldItem: FileFolder, newItem: FileFolder): Boolean {
        // File items
        if (oldItem.isFile && newItem.isFile) {
            val sameName = oldItem.displayName == newItem.displayName
            val sameSize = oldItem.size == newItem.size
            return sameName && sameSize
        }

        // Folder item
        if (oldItem.name != null && newItem.name != null) {
            val sameName = oldItem.name == newItem.name
            val sameSize = oldItem.size == newItem.size
            return sameName && sameSize
        }

        // If old and new aren't one of the same object types then contents have changed
        return false
    }

    override fun cancel() {
        apiCall?.cancel()
    }
}

interface FileFolderCallback {
    fun onItemClicked(item: FileFolder)
    fun onOpenItemMenu(item: FileFolder, anchorView: View)
    fun onRefreshFinished()
}
