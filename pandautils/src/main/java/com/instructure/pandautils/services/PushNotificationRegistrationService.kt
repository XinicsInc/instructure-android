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

package com.instructure.pandautils.services

import android.content.Context
import android.os.Bundle
import com.crashlytics.android.Crashlytics
import com.firebase.jobdispatcher.*
import com.google.android.gms.gcm.GoogleCloudMessaging
import com.google.android.gms.iid.InstanceID
import com.instructure.canvasapi2.StatusCallback
import com.instructure.canvasapi2.managers.CommunicationChannelsManager
import com.instructure.canvasapi2.utils.Logger
import com.instructure.pandautils.utils.Const
import okhttp3.ResponseBody

class PushNotificationRegistrationService : JobService() {

    class YUNoWorkPushNotifsException(msg: String) : Throwable(msg)

    override fun onStartJob(job: JobParameters): Boolean {
        Logger.e("PushNotificationRegistrationService : onStartJob()")
        val runnable = Runnable {
            try {
                val instanceID = InstanceID.getInstance(applicationContext)
                val token = instanceID.getToken(job.extras?.getString(Const.PROJECT_ID) ?: "", GoogleCloudMessaging.INSTANCE_ID_SCOPE, null)
                val responseBody = CommunicationChannelsManager.addNewPushCommunicationChannelSynchronous(token, object : StatusCallback<ResponseBody>() {})
                if(responseBody != null && responseBody.code() == 200) {
                    Logger.e("PushNotificationRegistrationService : onStartJob() - Success registering push notifications.")
                } else {
                    //We won't reschedule as this will re-register when the app starts.
                    Logger.e("PushNotificationRegistrationService : onStartJob() - Error registering push notifications.")
                }
                jobFinished(job, false)
            } catch (e: Throwable) {
                Crashlytics.logException(YUNoWorkPushNotifsException("PushNotificationRegistrationService : onStartJob() - Error registering push notifications. " + e.message))
                jobFinished(job, false)
            }
        }

        Thread(runnable).start()

        return true // Answers the question: "Is there still work going on?"
    }

    override fun onStopJob(job: JobParameters?): Boolean {
        return false // Answers the question: "Should this job be retried?"
    }

    companion object {
        fun scheduleJob(context: Context, isMasquerading: Boolean, projectId: String) {
            Logger.d("PushNotificationRegistrationService : scheduleJob() " + isMasquerading)

            if(!isMasquerading) {
                val extras = Bundle()
                extras.putString(Const.PROJECT_ID, projectId)
                val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(context))
                val job = dispatcher.newJobBuilder()
                        .setService(PushNotificationRegistrationService::class.java)
                        .setTag(PushNotificationRegistrationService::class.java.simpleName)
                        .setTrigger(Trigger.NOW)
                        .setRecurring(false)
                        .setReplaceCurrent(false)
                        .addConstraint(Constraint.ON_ANY_NETWORK)
                        .setExtras(extras)
                        .build()
                // Attempt to clear out any bad jobs so a real one can work
                dispatcher.cancelAll()
                dispatcher.schedule(job)
            }
        }
    }
}
