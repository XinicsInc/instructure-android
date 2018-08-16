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
 */
package com.instructure.canvasapi2.utils

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.instructure.canvasapi2.apis.EnrollmentAPI
import com.instructure.canvasapi2.models.*
import java.util.*

fun Parcel.writeBoolean(bool: Boolean) = writeByte((if (bool) 1 else 0).toByte())
fun Parcel.readBoolean() = readByte() != 0.toByte()

fun Date?.toApiString(): String? = APIHelper.dateToString(this)

fun Assignment.SUBMISSION_TYPE.prettyPrint(context: Context): String
        = Assignment.submissionTypeToPrettyPrintString(this, context)

/**
 * The global course name. This may be different from [Course.name] which could be the the user's
 * nickname for this course.
 * */
var Course.globalName: String
    get() = originalName.takeUnless(String::isNullOrBlank) ?: name
    set(value) {
        if (originalName.isNullOrBlank()) name = value else originalName = value
    }

/**
 *  If the term is concluded, it can't be favorited. So if it was favorited, and then the term concluded, we don't want it favorited now.
 *  We also don't want it included in the list of favorite courses
 *
 *  BUT.....it can be overridden by a section, so we need to check that section's end date too
 */
fun Course.isValidTerm(): Boolean = term?.endAt?.after(Date()) ?: true || hasValidSection()

/* If there is no valid section, we don't want this value to override the term's end date */
fun Course.hasValidSection(): Boolean = sections.any { it.endAt?.after(Date()) ?: false }

fun Course.hasActiveEnrollment(): Boolean = enrollments.any { it.enrollmentState == EnrollmentAPI.STATE_ACTIVE }

fun Course.isInvited(): Boolean = enrollments.any { it.enrollmentState == EnrollmentAPI.STATE_INVITED }

fun MediaComment.asAttachment() = Attachment().also {
    it.contentType = contentType ?: ""
    it.displayName = displayName
    it.filename = _fileName
    it.url = url
}

inline fun <reified T : Parcelable> T.parcelCopy(): T {
    val parcel = Parcel.obtain()
    parcel.writeParcelable(this, 0)
    parcel.setDataPosition(0)
    val copy = parcel.readParcelable<T>(T::class.java.classLoader)
    parcel.recycle()
    return copy
}

@JvmName("mapToAttachmentRemoteFile")
fun RemoteFile.mapToAttachment(): Attachment {
    val attachment = Attachment()
    attachment.id = this.id
    attachment.contentType = this.contentType
    attachment.filename = this.fileName
    attachment.displayName = this.displayName
    attachment.createdAt = this.createdAt
    attachment.size = this.size
    attachment.previewUrl = this.previewUrl
    attachment.thumbnailUrl = this.thumbnailUrl
    attachment.url = this.url
    return attachment
}

@JvmName("mapToRemoteFileFromAttachment")
fun Attachment.mapToRemoteFile(): RemoteFile {
    val remoteFile = RemoteFile()
    remoteFile.id = this.id
    remoteFile.contentType = this.contentType
    remoteFile.displayName = this.displayName
    remoteFile.size = this.size
    remoteFile.previewUrl = previewUrl
    remoteFile.thumbnailUrl = this.thumbnailUrl
    remoteFile.url = this.url
    if(this.createdAt != null) remoteFile.setCreatedAt(APIHelper.dateToString(this.createdAt))
    return remoteFile
}