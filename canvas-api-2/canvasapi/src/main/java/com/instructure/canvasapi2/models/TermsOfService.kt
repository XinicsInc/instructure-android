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
 */

package com.instructure.canvasapi2.models

import com.google.gson.annotations.SerializedName
import paperparcel.PaperParcel
import paperparcel.PaperParcelable

@PaperParcel
data class TermsOfService(
        var id: Long = 0,
        @SerializedName("terms_type")
        var termsType: String? = null,
        var passive: Boolean = false,
        @SerializedName("account_id")
        var accountId: Long = 0,
        var content: String? = null
) : PaperParcelable {
    companion object {
        @Suppress("unresolved_reference")
        @JvmField val CREATOR = PaperParcelTermsOfService.CREATOR
    }
}