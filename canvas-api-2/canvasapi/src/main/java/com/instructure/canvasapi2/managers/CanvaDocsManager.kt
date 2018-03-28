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
import com.instructure.canvasapi2.apis.CanvaDocsAPI
import com.instructure.canvasapi2.builders.RestBuilder
import com.instructure.canvasapi2.builders.RestParams
import com.instructure.canvasapi2.models.CanvaDocs.CanvaDocAnnotation
import com.instructure.canvasapi2.models.CanvaDocs.CanvaDocAnnotationResponse
import com.instructure.canvasapi2.utils.ApiPrefs
import okhttp3.ResponseBody

object CanvaDocsManager : BaseManager() {

    private val mTesting = false

    fun getCanvaDoc(previewUrl: String, callback: StatusCallback<ResponseBody>) {
        if (BaseManager.isTesting() || mTesting) {
            //todo
        } else {
            val adapter = RestBuilder(callback)
            val params = RestParams.Builder()
                    .withDomain(ApiPrefs.fullDomain)
                    .withAPIVersion("")
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .build()

            CanvaDocsAPI.getCanvaDoc(previewUrl, adapter, params, callback)
        }
    }

    fun getAnnotations(sessionId: String, canvaDocDomain: String, callback: StatusCallback<CanvaDocAnnotationResponse>) {
        if (BaseManager.isTesting() || mTesting) {
            //todo
        } else {
            val adapter = RestBuilder(callback)
            val params = RestParams.Builder()
                    .withDomain(canvaDocDomain)
                    .withAPIVersion("")
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .build()

            CanvaDocsAPI.getAnnotations(sessionId, adapter, params, callback)
        }
    }

    fun putAnnotation(sessionId: String, annotationId: String, annotation: CanvaDocAnnotation, canvaDocDomain: String, callback: StatusCallback<CanvaDocAnnotation>) {
        if (BaseManager.isTesting() || mTesting) {
            //todo
        } else {
            val adapter = RestBuilder(callback)
            val params = RestParams.Builder()
                    .withDomain(canvaDocDomain)
                    .withAPIVersion("")
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .build()

            CanvaDocsAPI.putAnnotation(sessionId, annotationId, annotation, adapter, params, callback)
        }
    }

    fun deleteAnnotation(sessionId: String, annotationId: String, canvaDocDomain: String, callback: StatusCallback<ResponseBody>) {
        if (BaseManager.isTesting() || mTesting) {
            //todo
        } else {
            val adapter = RestBuilder(callback)
            val params = RestParams.Builder()
                    .withDomain(canvaDocDomain)
                    .withAPIVersion("")
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .build()

            CanvaDocsAPI.deleteAnnotation(sessionId, annotationId, adapter, params, callback)
        }
    }

}