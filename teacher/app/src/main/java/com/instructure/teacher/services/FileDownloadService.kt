/*
 * Copyright (C) 2018 - present Instructure, Inc.
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

package com.instructure.teacher.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import android.util.Log
import android.widget.Toast
import com.instructure.canvasapi2.managers.FileFolderManager
import com.instructure.canvasapi2.utils.ContextKeeper
import com.instructure.canvasapi2.utils.HttpHelper
import com.instructure.interactions.router.Route
import com.instructure.interactions.router.RouterParams
import com.instructure.pandautils.loaders.OpenMediaAsyncTaskLoader
import com.instructure.pandautils.services.FileUploadService
import com.instructure.pandautils.services.FileUploadService.Companion.ACTION_CANCEL_UPLOAD
import com.instructure.pandautils.utils.Const
import com.instructure.pandautils.utils.Utils
import com.instructure.teacher.R
import com.instructure.teacher.activities.RouteValidatorActivity
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class FileDownloadService @JvmOverloads constructor(name: String = FileUploadService::class.java.simpleName) : IntentService(name) {

    private var isCanceled = false
    private var url = ""
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle 'cancel' action in onStartCommand instead of onHandleIntent, because threading.
        if (ACTION_CANCEL_UPLOAD == intent!!.action) isCanceled = true
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onHandleIntent(intent: Intent?) {

        // Skip if canceled
        if (isCanceled) return

        val bundle = intent?.extras
        val route: Route = bundle!!.getParcelable(Route.ROUTE)
        url = bundle.getString(Const.URL)

        showNotification()

        startDownload(route)
    }

    //region Download functionality

    /**
     * We want to download this with the same filename and in the same location as we do with the normal routing so the user
     * can tap the notification and view the file in our app
     */
    private fun startDownload(route: Route) {

        try {

            // Handle download cancellation
            if (isCanceled) {
                stopForeground(true)
                notificationManager.cancel(NOTIFICATION_ID)
                stopSelf()
                return
            }
            if (route.queryParamsHash.containsKey(RouterParams.VERIFIER) && route.queryParamsHash.containsKey(RouterParams.DOWNLOAD_FRD)) {
                if (route.url != null) {
                    downloadFile(this, route.url.toString(), getFilename(route.url.toString()))
                } else if (route.uri != null) {
                    downloadFile(this, route.uri.toString(), getFilename(route.uri.toString()))
                }
            } else {
                if (route.queryParamsHash.containsKey(RouterParams.PREVIEW)) {
                    // This is a link for a file preview, so we need to get the file id from the preview query param
                    getFile(this, route.queryParamsHash[RouterParams.PREVIEW] ?: "")
                } else {
                    getFile(this, route.paramsHash[RouterParams.FILE_ID] ?: "")
                }
            }

        } catch (exception: Exception) {
            updateNotificationError(getString(R.string.errorDownloadingFile))
        }

        stopSelf()
    }


    //endregion

    private fun getFile(context: Context, fileId: String) {
        val file = FileFolderManager.getFileFolderFromURLSynchronous("files/" + fileId)
        val fileUrl = file?.url
        if (fileUrl != null) {
            downloadFile(context, fileUrl, getFilename(fileUrl))
        } else {
            Toast.makeText(context, R.string.errorDownloadingFile, Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFilename(url: String) : String {
        val hc = URL(url).openConnection() as HttpURLConnection
        val connection = HttpHelper.redirectURL(hc)
        var filename = ""
        // parse filename from Content-Disposition header which is a response field that is normally used to set the file name
        val headerField = connection.getHeaderField("Content-Disposition")
        if (headerField != null) {
            filename = OpenMediaAsyncTaskLoader.parseFilename(headerField)
            filename = OpenMediaAsyncTaskLoader.makeFilenameUnique(filename, url)
        } else {
            filename = "" + url.hashCode()
        }

        return filename
    }
    @Throws(Exception::class)
    private fun downloadFile(context: Context, url: String, filename: String): File {
        //They have to download the content first... gross.
        //Download it if the file doesn't exist in the external cache....
        Log.d("FileDownload", "downloadFile URL: " + url)
        val attachmentFile = File(Utils.getAttachmentsDirectory(context), filename)
        Log.d("FileDownload", "File " + attachmentFile)
        if (!attachmentFile.exists()) {
            //Download the content from the url
            if (writeAttachmentsDirectoryFromURL(url, attachmentFile)) {
                Log.d("FileDownload", "file not cached")
                updateNotificationComplete()
                return attachmentFile
            }
        }

        updateNotificationComplete()

        return attachmentFile
    }

    @Throws(Exception::class)
    private fun writeAttachmentsDirectoryFromURL(url2: String, toWriteTo: File): Boolean {
        //create the new connection
        val url = URL(url2)
        val urlConnection = url.openConnection() as HttpURLConnection
        //set up some things on the connection
        urlConnection.requestMethod = "GET"

        //and connect!
        urlConnection.connect()
        val connection = HttpHelper.redirectURL(urlConnection)

        //this will be used to write the downloaded uri into the file we created
        val name = toWriteTo.name
        toWriteTo.parentFile.mkdirs()
        var fileOutput: FileOutputStream? = null
        //if there is an external cache, we want to write to that
        if (applicationContext.externalCacheDir != null) {
            fileOutput = FileOutputStream(toWriteTo)
        } else {            //otherwise, use internal cache.
            fileOutput = applicationContext.openFileOutput(name,
                    Activity.MODE_WORLD_READABLE or Activity.MODE_WORLD_WRITEABLE)
        }

        fileOutput?.let {
            //this will be used in reading the uri from the internet
            val inputStream = connection.inputStream

            //create a buffer...
            val buffer = ByteArray(1024)
            var bufferLength = 0 //used to store a temporary size of the buffer

            //now, read through the input buffer and write the contents to the file
            while (true) {
                bufferLength = inputStream.read(buffer)
                if (bufferLength == -1) break
                it.write(buffer, 0, bufferLength)
            }

            it.flush()
            it.close()
            inputStream.close() // close after fileOuput.flush() or else the fileOutput won't actually close leading to open failed: EBUSY (Device or resource busy)
        }
        return true
    }

    //region Notifications

    private fun createNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        // Prevents recreation of notification channel if it exists.
        if (notificationManager.notificationChannels?.any { it.id == channelId } == true) return

        val name = ContextKeeper.appContext.getString(com.instructure.pandautils.R.string.notificationChannelNameFileUploadsName)
        val description = ContextKeeper.appContext.getString(com.instructure.pandautils.R.string.notificationChannelNameFileUploadsDescription)

        // Create the channel and add the group
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(channelId, name, importance)
        channel.description = description
        channel.enableLights(false)
        channel.enableVibration(false)

        // Create the channel
        notificationManager.createNotificationChannel(channel)
    }

    private fun showNotification() {
        createNotificationChannel(CHANNEL_ID)

        notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.canvas_logo_white)
                .setContentTitle(getString(R.string.downloadingFile))
                .setProgress(0, 0, true)
        startForeground(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun updateNotificationError(message: String) {
        notificationBuilder.setContentText(message)
                .setProgress(0, 0, false)
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun updateNotificationComplete() {
        val intent = RouteValidatorActivity.createIntent(this, Uri.parse(url))
        val bundle = Bundle()
        bundle.putBoolean(Const.FILE_DOWNLOADED, true)
        intent.putExtras(bundle)

        val contentIntent = PendingIntent.getActivity(this@FileDownloadService, NOTIFICATION_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        notificationBuilder.setProgress(0, 0, false)
                .setContentTitle(getString(R.string.fileDownloadedSuccessfully))
                .setContentText(getString(R.string.tapToView))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }


    //endregion

    override fun onDestroy() {
        if (isCanceled) {
            notificationManager.cancel(NOTIFICATION_ID)
        } else {
            notificationBuilder.setOngoing(false)
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
        }
    }

    companion object {
        private val NOTIFICATION_ID = 2
        const val CHANNEL_ID = "fileDownloadChannel"
        // Download broadcasts
        val DOWNLOAD_COMPLETED = "DOWNLOAD_COMPLETED"
        val DOWNLOAD_ERROR = "DOWNLOAD_ERROR"


    }

    //endregion
}