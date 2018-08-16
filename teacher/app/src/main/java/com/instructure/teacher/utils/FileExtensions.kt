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
package com.instructure.teacher.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v4.content.FileProvider
import com.instructure.canvasapi2.models.FileFolder
import com.instructure.canvasapi2.models.RemoteFile
import com.instructure.canvasapi2.utils.APIHelper
import com.instructure.pandautils.utils.Const
import com.instructure.teacher.R
import java.io.File

/** Whether or not this Uri is exposed */
fun Uri.isExposed() = "file" == scheme && !path.startsWith("/system/")

/** Wraps this file in a safe-to-expose Uri using FileProvider */
fun File.provided(context: Context): Uri = FileProvider.getUriForFile(context, context.packageName + Const.FILE_PROVIDER_AUTHORITY, this)

/** Wraps this Uri in a safe-to-expose Uri (if necessary) using FileProvider */
fun Uri.provided(context: Context): Uri = takeUnless { it.isExposed() } ?: File(path).provided(context)

/**
 * Launches an intent to view the contents of this Uri in another app.
 * @param context A valid Android Context
 * @param contentType The MIME type of the content
 * @param onNoApps Called when no apps can handle the intent. Default behavior shows a toast.
 */
fun Uri.viewExternally(context: Context, contentType: String, onNoApps: () -> Unit = { context.toast(R.string.noApps)} ) {
    val uri = provided(context)
    val intent = Intent(Intent.ACTION_VIEW, uri)
    intent.setDataAndType(uri, contentType)
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    val appCount = context.packageManager.queryIntentActivities(intent, 0).size
    if (appCount > 0) context.startActivity(intent) else onNoApps()
}

fun RemoteFile.mapToFileFolder(): FileFolder {
    val fileFolder = FileFolder()
    fileFolder.id = id
    fileFolder.folderId = folderId
    fileFolder.displayName = displayName
    fileFolder.contentType = contentType
    fileFolder.url = url
    fileFolder.size = size
    fileFolder.createdAt = createdAt
    fileFolder.updatedAt = updatedAt
    fileFolder.unlockAt = unlockAt
    fileFolder.isLocked = isLocked
    fileFolder.isLockedForUser = isLockedForUser
    fileFolder.isHidden = isHidden
    fileFolder.thumbnailUrl = thumbnailUrl

    if(!lockAt.isNullOrBlank()) {
        fileFolder.lockAt = APIHelper.stringToDate(lockAt)
    }
    return fileFolder
}