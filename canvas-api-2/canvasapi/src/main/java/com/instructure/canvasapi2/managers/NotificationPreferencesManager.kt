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
package com.instructure.canvasapi2.managers

import com.instructure.canvasapi2.StatusCallback
import com.instructure.canvasapi2.apis.NotificationPreferencesAPI
import com.instructure.canvasapi2.builders.RestBuilder
import com.instructure.canvasapi2.builders.RestParams
import com.instructure.canvasapi2.models.NotificationPreferenceResponse

object NotificationPreferencesManager : BaseManager() {

    const val IMMEDIATELY = "immediately"
    const val NEVER = "never"

    private val mTesting = false

    @JvmStatic
    fun getNotificationPreferences(
            userId: Long,
            commChannelId: Long,
            forceNetwork: Boolean,
            callback: StatusCallback<NotificationPreferenceResponse>) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            val adapter = RestBuilder(callback)
            val params = RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .build()
            NotificationPreferencesAPI.getNotificationPreferences(
                    userId,
                    commChannelId,
                    adapter,
                    params,
                    callback
            )
        }
    }

    @JvmStatic
    fun updateMultipleNotificationPreferences(
            commChannelId: Long,
            notifications: List<String>,
            frequency: String,
            callback: StatusCallback<NotificationPreferenceResponse>) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            val adapter = RestBuilder(callback)
            val params = RestParams.Builder().build()
            NotificationPreferencesAPI.updateMultipleNotificationPreferences(
                    commChannelId,
                    notifications,
                    frequency,
                    adapter,
                    params,
                    callback
            )
        }
    }

    @JvmStatic
    fun updatePreferenceCategory(
            categoryName: String,
            channelId: Long,
            frequency: String,
            callback: StatusCallback<NotificationPreferenceResponse>) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            val adapter = RestBuilder(callback)
            val params = RestParams.Builder().build()
            NotificationPreferencesAPI.updatePreferenceCategory(
                    categoryName,
                    channelId,
                    frequency,
                    adapter,
                    params,
                    callback
            )
        }
    }
}
