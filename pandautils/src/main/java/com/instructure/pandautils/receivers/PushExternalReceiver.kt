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

package com.instructure.pandautils.receivers

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import android.text.TextUtils
import com.google.android.gms.gcm.GcmListenerService
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.canvasapi2.utils.Logger
import com.instructure.pandautils.R
import com.instructure.pandautils.models.PushNotification
import java.net.MalformedURLException
import java.net.URL

abstract class PushExternalReceiver : GcmListenerService() {

    abstract fun getAppName(context: Context): String
    abstract fun getStartingActivityClass(): Class<out Activity>

    override fun onMessageReceived(sender: String?, msg: Bundle?) {
        Logger.d("PushExternalReceiver onReceive()")
        if (msg != null) {
            val htmlUrl = msg.getString(PushNotification.HTML_URL, "")
            val from = msg.getString(PushNotification.FROM, "")
            val alert = msg.getString(PushNotification.ALERT, "")
            val collapseKey = msg.getString(PushNotification.COLLAPSE_KEY, "")
            val userId = msg.getString(PushNotification.USER_ID, "")

            val push = PushNotification(htmlUrl, from, alert, collapseKey, userId)
            if (PushNotification.store(this, push)) {
                postNotification(this, msg, getAppName(this), getStartingActivityClass())
            }
        }
    }

    companion object {

        const val NEW_PUSH_NOTIFICATION = "newPushNotification"

        fun postNotification(context: Context, extras: Bundle?, appName: String, startingActivity: Class<out Activity>) {

            val user = ApiPrefs.user
            val userDomain = ApiPrefs.domain
            val url = getHtmlUrl(extras)
            val notificationUserId = PushNotification.getUserIdFromPush(getUserId(extras))

            var incomingDomain = ""

            try {
                incomingDomain = URL(url).host
            } catch (e: MalformedURLException) {
                Logger.e("HTML URL MALFORMED")
            } catch (e: NullPointerException) {
                Logger.e("HTML URL IS NULL")
            }

            if (user != null && !TextUtils.isEmpty(notificationUserId)) {
                val currentUserId = java.lang.Long.toString(user.id)
                if (!notificationUserId.equals(currentUserId, ignoreCase = true)) {
                    Logger.e("USER IDS MISMATCHED")
                    return
                }
            } else {
                Logger.e("USER WAS NULL OR USER_ID WAS NULL")
                return
            }

            if (TextUtils.isEmpty(incomingDomain) || TextUtils.isEmpty(userDomain) || !incomingDomain.equals(userDomain, ignoreCase = true)) {
                Logger.e("DOMAINS DID NOT MATCH")
                return
            }

            val pushes = PushNotification.getStoredPushes(context)

            if (pushes.size == 0 && extras == null) {
                // Nothing to post, situation would occur from the BootReceiver
                return
            }

            val contentIntent = Intent(context, startingActivity)
            contentIntent.putExtra(NEW_PUSH_NOTIFICATION, true)
            if (extras != null) {
                contentIntent.putExtras(extras)
            }

            val deleteIntent = Intent(context, PushDeleteReceiver::class.java)

            val contentPendingIntent = PendingIntent.getActivity(context, 0, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            val deletePendingIntent = PendingIntent.getBroadcast(context, 0, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT)

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channelId = "generalNotifications"

            val inboxStyle = NotificationCompat.InboxStyle()
            inboxStyle.setBigContentTitle(context.getString(R.string.notificationPrimaryInboxTitle))
            for (push in pushes) {
                inboxStyle.addLine(push.alert)
            }

            val notification = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_notification_canvas_logo)
                    .setContentTitle(appName)
                    .setContentText(getMessage(extras))
                    .setContentIntent(contentPendingIntent)
                    .setDeleteIntent(deletePendingIntent)
                    .setAutoCancel(true)
                    .setStyle(inboxStyle)
                    .build()

            createNotificationChannel(context, channelId, user.loginId, nm)

            nm.notify(555443, notification)
        }

        private fun createNotificationChannel(context: Context, channelId: String, userEmail: String, nm: NotificationManager) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

            // Prevents recreation of notification channel if it exists.
            val channelList = nm.notificationChannels
            for (channel in channelList) {
                if (channelId == channel.id) {
                    return
                }
            }

            val name = context.getString(R.string.notificationChannelNamePrimary)
            val description = context.getString(R.string.notificationChannelDescriptionPrimary)

            // Create a group for the user, this enables support for multiple users
            nm.createNotificationChannelGroup(NotificationChannelGroup(userEmail, name))

            // Create the channel and add the group
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance)
            channel.description = description
            channel.enableLights(false)
            channel.enableVibration(false)
            channel.group = userEmail

            // Create the channel
            nm.createNotificationChannel(channel)
        }

        private fun getMessage(extras: Bundle?): String = extras?.getString(PushNotification.ALERT, "") ?: ""

        private fun getUserId(extras: Bundle?): String = extras?.getString(PushNotification.USER_ID, "") ?: ""

        private fun getHtmlUrl(extras: Bundle?): String = extras?.getString(PushNotification.HTML_URL, "") ?: ""

        fun postStoredNotifications(context: Context, appName: String, startingActivity: Class<out Activity>) {
            postNotification(context, null, appName, startingActivity)
        }
    }
}
