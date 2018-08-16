package com.instructure.canvasapi2.managers

import com.instructure.canvasapi2.StatusCallback
import com.instructure.canvasapi2.apis.NotoriousAPI
import com.instructure.canvasapi2.builders.RestBuilder
import com.instructure.canvasapi2.builders.RestParams
import com.instructure.canvasapi2.models.NotoriousConfig
import com.instructure.canvasapi2.models.NotoriousSession
import com.instructure.canvasapi2.models.notorious.NotoriousResultWrapper
import com.instructure.canvasapi2.utils.ApiPrefs
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import java.io.File


object NotoriousManager : BaseManager() {

    private val mTesting = false

    @JvmStatic
    fun getConfiguration(callback: StatusCallback<NotoriousConfig>) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            val adapter = RestBuilder(callback)
            val params = RestParams.Builder().build()
            NotoriousAPI.getConfiguration(adapter, params, callback)
        }
    }

    @JvmStatic
    fun startSession(callback: StatusCallback<NotoriousSession>) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            val adapter = RestBuilder(callback)
            val params = RestParams.Builder().build()
            NotoriousAPI.startSession(adapter, params, callback)
        }
    }

    @JvmStatic
    fun getUploadToken(callback: StatusCallback<NotoriousResultWrapper>) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            val adapter = RestBuilder(callback)
            NotoriousAPI.getUploadToken(adapter, callback)
        }
    }

    @JvmStatic
    fun uploadFileSynchronous(uploadToken: String, file: File, contentType: String): Response<Void>? {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            val requestBody = RequestBody.create(MediaType.parse(contentType), file)
            val filePart = MultipartBody.Part.createFormData("fileData", file.name, requestBody)
            val adapter = RestBuilder()
            return NotoriousAPI.uploadFileSynchronous(ApiPrefs.notoriousToken, uploadToken, filePart, adapter)
        }
        return null
    }

    @JvmStatic
    fun getMediaIdSynchronous(uploadToken: String, fileName: String, mimeType: String): NotoriousResultWrapper? {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            val adapter = RestBuilder()
            return NotoriousAPI.getMediaIdSynchronous(ApiPrefs.notoriousToken, uploadToken, fileName, mimeType, adapter)
        }
        return null
    }

}
