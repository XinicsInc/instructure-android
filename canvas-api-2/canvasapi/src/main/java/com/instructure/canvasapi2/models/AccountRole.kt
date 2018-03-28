/*
 * Copyright (C) 2018 - present Instructure, Inc.
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
 */    package com.instructure.canvasapi2.models

import com.google.gson.annotations.SerializedName
import paperparcel.PaperParcel
import paperparcel.PaperParcelable
import java.util.*

@PaperParcel
data class AccountRole(
    private var id: Long = 0,
    var role: String = "",
    var label: String? = null,
    @SerializedName("base_role_type")
    var baseRoleType: String? = null,
    @SerializedName("workflow_state")
    var workflowState: String? = null,
    var permissions: Map<String, AccountPermission> = emptyMap()
) : CanvasModel<AccountRole>(), PaperParcelable {

    override fun getId() = id

    override fun getComparisonDate(): Date? = null

    override fun getComparisonString(): String? = null

    override fun describeContents() = 0

    companion object {
        @Suppress("UNRESOLVED_REFERENCE")
        @JvmField val CREATOR = PaperParcelAccountRole.CREATOR
    }

}

@PaperParcel
data class AccountPermission(
    var enabled: Boolean = false,
    var locked: Boolean = false,
    var readonly: Boolean = false,
    var explicit: Boolean = false,
    @SerializedName("applies_to_descendants")
    var appliesToDescendants: Boolean = false,
    @SerializedName("applies_to_self")
    var appliesToSelf: Boolean = false
) : PaperParcelable {
    companion object {
        @Suppress("UNRESOLVED_REFERENCE")
        @JvmField val CREATOR = PaperParcelAccountPermission.CREATOR
    }
}
