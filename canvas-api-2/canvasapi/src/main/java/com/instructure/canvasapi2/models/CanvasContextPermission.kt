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

@PaperParcel
data class CanvasContextPermission(
        @SerializedName(CREATE_DISCUSSION_TOPIC)
        var canCreateDiscussionTopic: Boolean = false,

        @SerializedName(MANAGE_GRADES)
        var manage_grades: Boolean = false,

        @SerializedName(SEND_MESSAGES)
        var send_messages: Boolean = false,

        @SerializedName(SEND_MESSAGES_ALL)
        var send_messages_all: Boolean = false,

        @SerializedName(VIEW_ALL_GRADES)
        var view_all_grades: Boolean = false,

        @SerializedName(VIEW_ANALYTICS)
        var view_analytics: Boolean = false,

        @SerializedName(BECOME_USER)
        var become_user: Boolean = false,

        @SerializedName(CAN_UPDATE_NAME)
        var canUpdateName: Boolean = false,

        @SerializedName(CAN_UPDATE_AVATAR)
        var canUpdateAvatar: Boolean = false,

        @SerializedName(CREATE_ANNOUNCEMENT)
        var canCreateAnnouncement: Boolean = false
) : PaperParcelable {
    companion object {

        const val BECOME_USER = "become_user"
        const val CAN_UPDATE_AVATAR = "can_update_avatar"
        const val CAN_UPDATE_NAME = "can_update_name"
        const val CREATE_ANNOUNCEMENT = "create_announcement"
        const val CREATE_DISCUSSION_TOPIC = "create_discussion_topic"
        const val DELETE = "delete"
        const val MANAGE_GRADES = "manage_grades"
        const val SEND_MESSAGES = "send_messages"
        const val SEND_MESSAGES_ALL = "send_messages_all"
        const val VIEW_ALL_GRADES = "view_all_grades"
        const val VIEW_ANALYTICS = "view_analytics"

        @Suppress("unresolved_reference")
        @JvmField val CREATOR = PaperParcelCanvasContextPermission.CREATOR
    }
}
