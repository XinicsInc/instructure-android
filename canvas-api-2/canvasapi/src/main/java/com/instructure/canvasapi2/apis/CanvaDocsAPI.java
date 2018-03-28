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
package com.instructure.canvasapi2.apis;

import android.support.annotation.NonNull;

import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.builders.RestBuilder;
import com.instructure.canvasapi2.builders.RestParams;
import com.instructure.canvasapi2.models.CanvaDocs.CanvaDocAnnotation;
import com.instructure.canvasapi2.models.CanvaDocs.CanvaDocAnnotationResponse;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Url;

public class CanvaDocsAPI {

    interface CanvaDocsInterFace {
        @GET
        Call<ResponseBody> getCanvaDoc(@Url String url);

        @GET("/2018-03-07/sessions/{sessionId}/annotations")
        Call<CanvaDocAnnotationResponse> getAnnotations(@Path("sessionId") String sessionId);

        @PUT("/2018-03-07/sessions/{sessionId}/annotations/{annotationId}")
        Call<CanvaDocAnnotation> putAnnotation(@Path("sessionId") String sessionId, @Path("annotationId") String annotationId, @Body CanvaDocAnnotation annotation);

        @DELETE("/1/sessions/{sessionId}/annotations/{annotationId}")
        Call<ResponseBody> deleteAnnotation(@Path("sessionId") String sessionId, @Path("annotationId") String annotationId);
    }

    public static void getCanvaDoc(
            @NonNull String previewUrl,
            @NonNull RestBuilder adapter,
            @NonNull RestParams params,
            @NonNull StatusCallback<ResponseBody> callback) {
        callback.addCall(adapter.build(CanvaDocsInterFace.class, params).getCanvaDoc(previewUrl)).enqueue(callback);
    }

    public static void getAnnotations(
            @NonNull String sessionId,
            @NonNull RestBuilder adapter,
            @NonNull RestParams params,
            @NonNull StatusCallback<CanvaDocAnnotationResponse> callback) {
        callback.addCall(adapter.build(CanvaDocsInterFace.class, params).getAnnotations(sessionId)).enqueue(callback);
    }

    public static void putAnnotation(
            @NonNull String sessionId,
            @NonNull String annotationId,
            @NonNull CanvaDocAnnotation annotation,
            @NonNull RestBuilder adapter,
            @NonNull RestParams params,
            @NonNull StatusCallback<CanvaDocAnnotation> callback) {
        callback.addCall(adapter.build(CanvaDocsInterFace.class, params).putAnnotation(sessionId, annotationId, annotation)).enqueue(callback);
    }

    public static void deleteAnnotation(
            @NonNull String sessionId,
            @NonNull String annotationId,
            @NonNull RestBuilder adapter,
            @NonNull RestParams params,
            @NonNull StatusCallback<ResponseBody> callback) {
        callback.addCall(adapter.build(CanvaDocsInterFace.class, params).deleteAnnotation(sessionId, annotationId)).enqueue(callback);
    }
}
