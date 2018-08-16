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
package com.instructure.pandautils.models

import android.os.Parcel
import android.os.Parcelable
import com.instructure.canvasapi2.models.CanvasComparable
import com.instructure.canvasapi2.utils.readBoolean
import com.instructure.canvasapi2.utils.toApiString
import java.util.*

data class CoreDates(
        var dueDate: Date? = null,
        var lockDate: Date? = null,
        var unlockDate: Date? = null
) : Parcelable {
    companion object {
        @JvmField val CREATOR: Parcelable.Creator<CoreDates> = object : Parcelable.Creator<CoreDates> {
            override fun createFromParcel(source: Parcel): CoreDates = CoreDates(source)
            override fun newArray(size: Int): Array<CoreDates?> = arrayOfNulls(size)
        }
    }

    constructor(source: Parcel) : this(source.readSerializable() as Date?, source.readSerializable() as Date?, source.readSerializable() as Date?)

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeSerializable(dueDate)
        dest?.writeSerializable(lockDate)
        dest?.writeSerializable(unlockDate)
    }
}

data class DueDateGroup(
        var isEveryone: Boolean = false,
        var sectionIds: List<Long> = emptyList(),
        var groupIds: List<Long> = emptyList(),
        var studentIds: List<Long> = emptyList(),
        var coreDates: CoreDates = CoreDates()
) : CanvasComparable<DueDateGroup>() {
    override fun getId() = hashCode().toLong()

    override fun getComparisonDate() = coreDates.dueDate ?: coreDates.unlockDate ?: coreDates.lockDate

    override fun getComparisonString() = coreDates.dueDate?.toApiString() ?: coreDates.unlockDate?.toApiString() ?: coreDates.unlockDate?.toApiString() ?: ""

    val hasOverrideAssignees: Boolean
        get() = sectionIds.isNotEmpty() || groupIds.isNotEmpty() || studentIds.isNotEmpty()

    companion object {
        @JvmField val CREATOR: Parcelable.Creator<DueDateGroup> = object : Parcelable.Creator<DueDateGroup> {
            override fun createFromParcel(source: Parcel): DueDateGroup = DueDateGroup(source)
            override fun newArray(size: Int): Array<DueDateGroup?> = arrayOfNulls(size)
        }
    }

    constructor(source: Parcel) : this(
            source.readBoolean(),
            ArrayList<Long>().apply{ source.readList(this, Long::class.java.classLoader) },
            ArrayList<Long>().apply{ source.readList(this, Long::class.java.classLoader) },
            ArrayList<Long>().apply{ source.readList(this, Long::class.java.classLoader) },
            source.readParcelable<CoreDates>(CoreDates::class.java.classLoader))

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeInt((if (isEveryone) 1 else 0))
        dest?.writeList(sectionIds)
        dest?.writeList(groupIds)
        dest?.writeList(studentIds)
        dest?.writeParcelable(coreDates, 0)
    }
}