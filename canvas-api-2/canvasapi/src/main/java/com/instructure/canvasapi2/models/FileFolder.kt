/*
 * Copyright (C) 2017 - present Instructure, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.instructure.canvasapi2.models

import com.google.gson.annotations.SerializedName
import paperparcel.PaperParcel
import paperparcel.PaperParcelable
import java.util.*

@PaperParcel
data class FileFolder(
    // Common Attributes
    private var id: Long = 0,
    @SerializedName("created_at")
    var createdAt: Date? = null,
    @SerializedName("updated_at")
    var updatedAt: Date? = null,
    @SerializedName("unlock_at")
    var unlockAt: Date? = null,
    @SerializedName("lock_at")
    var lockAt: Date? = null,
    @SerializedName("locked")
    var isLocked: Boolean = false,
    @SerializedName("hidden")
    var isHidden: Boolean = false,
    @SerializedName("locked_for_user")
    var isLockedForUser: Boolean = false,
    @SerializedName("hidden_for_user")
    var isHiddenForUser: Boolean = false,

    // File Attributes
    @SerializedName("folder_id")
    var folderId: Long = 0,
    var size: Long = 0,
    @SerializedName("content-type")
    var contentType: String? = null,
    var url: String? = null,
    @SerializedName("display_name")
    var displayName: String? = null,
    @SerializedName("thumbnail_url")
    var thumbnailUrl: String? = null,
    @SerializedName("lock_info")
    var lockInfo: LockInfo? = null,

    // Folder Attributes
    @SerializedName("parent_folder_id")
    var parentFolderId: Long = 0,
    @SerializedName("context_id")
    var contextId: Long = 0,
    @SerializedName("files_count")
    var filesCount: Int = 0,
    var position: Int = 0,
    @SerializedName("folders_count")
    var foldersCount: Int = 0,
    @SerializedName("context_type")
    var contextType: String? = null,
    var name: String? = null,
    @SerializedName("folders_url")
    var foldersUrl: String? = null,
    @SerializedName("files_url")
    var filesUrl: String? = null,
    @SerializedName("full_name")
    var fullName: String? = null,
    @SerializedName("usage_rights")
    var usageRights: UsageRights? = null,
    @SerializedName("for_submissions")
    var forSubmissions: Boolean = false
) : CanvasModel<FileFolder>(), PaperParcelable {

    val isRoot: Boolean get() = parentFolderId == 0L

    val isFile: Boolean get() = !displayName.isNullOrBlank()

    override fun getId() = id

    fun setId(id: Long) {
        this.id = id
    }

    override fun getComparisonDate(): Date? = null

    override fun getComparisonString(): String? = null

    /* We override compareTo instead of using Canvas Comparable methods */
    override fun compareTo(other: FileFolder) = comparator.compare(this, other)

    override fun describeContents() = 0

    companion object {
        @Suppress("UNRESOLVED_REFERENCE")
        @JvmField
        val CREATOR = PaperParcelFileFolder.CREATOR

        private val comparator = compareBy<FileFolder>(
            { if (it.fullName == null) 1 else -1 }, // Folders go before files
            { it.fullName?.toLowerCase() }, // When both are folders
            { it.displayName?.toLowerCase() } // When both are files
        )
    }

}
