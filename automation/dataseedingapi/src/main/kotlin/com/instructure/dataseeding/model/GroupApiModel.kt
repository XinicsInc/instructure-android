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

package com.instructure.dataseeding.model

import com.google.gson.annotations.SerializedName

object ContextTypes {
        const val ACCOUNT = "Account"
        const val COURSE = "Course"
}

object WorkflowStates {
        const val ACCEPTED = "accepted"
        const val INVITED = "invited"
        const val REQUESTED = "requested"
}

data class GroupApiModel(
        val id: Long,
        val name: String,
        val description: String,
        @SerializedName("context_type")
        val contextType: String,
        @SerializedName("course_id")
        val courseId: Long,
        @SerializedName("group_category_id")
        val groupCategoryId: Long
)

data class CreateGroup(
        val name: String,
        val description: String
)
