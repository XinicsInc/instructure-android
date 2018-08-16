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

package com.instructure.canvasapi2.tests;

import android.support.annotation.NonNull;

import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.builders.RestBuilder;
import com.instructure.canvasapi2.builders.RestParams;
import com.instructure.canvasapi2.models.CanvasColor;
import com.instructure.canvasapi2.models.FileUploadParams;
import com.instructure.canvasapi2.models.Parent;
import com.instructure.canvasapi2.models.ParentResponse;
import com.instructure.canvasapi2.models.RemoteFile;
import com.instructure.canvasapi2.models.ResetParent;
import com.instructure.canvasapi2.models.Student;
import com.instructure.canvasapi2.models.TermsOfService;
import com.instructure.canvasapi2.models.User;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.LinkHeaders;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;


public class UserManager_Test {

    public static void getColors(@NonNull RestBuilder adapter, @NonNull StatusCallback<CanvasColor> callback, @NonNull RestParams params) {
        adapter.setStatusCallback(callback);

        Response response = new Response.Builder()
                .code(200)
                .message("todo")
                .protocol(Protocol.HTTP_1_0)
                .body(ResponseBody.create(MediaType.parse("application/json"), "todo".getBytes()))
                .addHeader("content-type", "application/json")
                .build();

        CanvasColor canvasColor = new CanvasColor();

        retrofit2.Response<CanvasColor> response1 = retrofit2.Response.success(canvasColor, response);
        callback.onResponse(response1, new LinkHeaders(), ApiType.CACHE);
    }

    public static void setColors(@NonNull RestBuilder adapter, @NonNull StatusCallback<CanvasColor> callback, @NonNull String contextId, int color) {
        //TODO: not sure what to do about PUTs
    }

    public static RemoteFile uploadUserFileSynchronous(@NonNull RestBuilder adapter, String uploadUrl, Map<String, RequestBody> uploadParams, String mimeType, File file) throws IOException {
        // TODO
        return null;
    }

    public static FileUploadParams getFileUploadParamsSynchronous(@NonNull RestBuilder adapter, String fileName, long size, String contentType, Long parentFolderId) throws IOException {
        // TODO
        return null;
    }

    public static FileUploadParams getFileUploadParamsSynchronous(@NonNull RestBuilder adapter, String fileName, long size, String contentType, String parentFolderPath) throws IOException {
        // TODO
        return null;
    }

    public static void getSelf(RestBuilder adapter, RestParams params, StatusCallback<User> callback) {
        // TODO
    }

    public static void getUser(RestBuilder adapter, RestParams params, Long userId, StatusCallback<User> callback) {
        // TODO
    }

    public static void getPeopleList(RestBuilder adapter, RestParams params, long canvasContextId, StatusCallback<List<User>> callback) {
        // TODO
    }

    public static void getTermsOfService(RestBuilder adapter, RestParams params, StatusCallback<TermsOfService> callback) {
        // TODO
    }

    public static void addStudentToParentAirwolf(String airwolfDomain, RestBuilder adapter, RestParams params, String parentId, String studentDomain, StatusCallback<ResponseBody> callback) {
        adapter.setStatusCallback(callback);

        Response response = new Response.Builder()
                .code(200)
                .message("success")
                .protocol(Protocol.HTTP_1_0)
                .body(ResponseBody.create(MediaType.parse("application/json"), "success".getBytes()))
                .addHeader("content-type", "application/json")
                .build();


        retrofit2.Response response1 = retrofit2.Response.success(response);
        callback.onResponse(response1, new LinkHeaders(), ApiType.API);
    }

    public static void removeStudentAirwolf(String airwolfDomain, RestBuilder adapter, RestParams params, String parentId, String studentId, StatusCallback<ResponseBody> callback) {
        adapter.setStatusCallback(callback);

        Response response = new Response.Builder()
                .code(200)
                .message("success")
                .protocol(Protocol.HTTP_1_0)
                .body(ResponseBody.create(MediaType.parse("application/json"), "success".getBytes()))
                .addHeader("content-type", "application/json")
                .build();


        retrofit2.Response response1 = retrofit2.Response.success(response);
        callback.onResponse(response1, new LinkHeaders(), ApiType.API);
    }

    public static void addParentAirwolf(String airwolfDomain, RestBuilder adapter, RestParams params, Parent parent, StatusCallback<ParentResponse> callback) {
        adapter.setStatusCallback(callback);

        Response response = new Response.Builder()
                .code(200)
                .message("success")
                .protocol(Protocol.HTTP_1_0)
                .body(ResponseBody.create(MediaType.parse("application/json"), "success".getBytes()))
                .addHeader("content-type", "application/json")
                .build();


        retrofit2.Response response1 = retrofit2.Response.success(response);
        callback.onResponse(response1, new LinkHeaders(), ApiType.API);
    }

    public static void getStudentsForParentAirwolf(String airwolfDomain, RestBuilder adapter, RestParams params, String parentId, StatusCallback<List<Student>> callback) {
        adapter.setStatusCallback(callback);

        Response response = new Response.Builder()
                .code(200)
                .message("success")
                .protocol(Protocol.HTTP_1_0)
                .body(ResponseBody.create(MediaType.parse("application/json"), "success".getBytes()))
                .addHeader("content-type", "application/json")
                .build();


        retrofit2.Response response1 = retrofit2.Response.success(response);
        callback.onResponse(response1, new LinkHeaders(), ApiType.API);
    }


    public static void getObserveesForParent(String airwolfDomain, RestBuilder adapter, RestParams params, String parentId, StatusCallback<List<Student>> callback) {
        adapter.setStatusCallback(callback);

        Response response = new Response.Builder()
                .code(200)
                .message("success")
                .protocol(Protocol.HTTP_1_0)
                .body(ResponseBody.create(MediaType.parse("application/json"), "success".getBytes()))
                .addHeader("content-type", "application/json")
                .build();


        retrofit2.Response response1 = retrofit2.Response.success(response);
        callback.onResponse(response1, new LinkHeaders(), ApiType.API);
    }

    public static void sendPasswordResetForParentAirwolf(String airwolfDomain, RestBuilder adapter, RestParams params, String userName, StatusCallback<ResponseBody> callback) {
        adapter.setStatusCallback(callback);

        Response response = new Response.Builder()
                .code(200)
                .message("success")
                .protocol(Protocol.HTTP_1_0)
                .body(ResponseBody.create(MediaType.parse("application/json"), "success".getBytes()))
                .addHeader("content-type", "application/json")
                .build();


        retrofit2.Response response1 = retrofit2.Response.success(response);
        callback.onResponse(response1, new LinkHeaders(), ApiType.API);
    }

    public static void authenticateParentAirwolf(String airwolfDomain, RestBuilder adapter, RestParams params, String email, String password, StatusCallback<ParentResponse> callback) {
        adapter.setStatusCallback(callback);

        Response response = new Response.Builder()
                .code(200)
                .message("success")
                .protocol(Protocol.HTTP_1_0)
                .body(ResponseBody.create(MediaType.parse("application/json"), "success".getBytes()))
                .addHeader("content-type", "application/json")
                .build();


        retrofit2.Response response1 = retrofit2.Response.success(response);
        callback.onResponse(response1, new LinkHeaders(), ApiType.API);
    }

    public static void resetParentPassword(String airwolfDomain, RestBuilder adapter, RestParams params, String email, String password, StatusCallback<ResetParent> callback) {
        adapter.setStatusCallback(callback);

        Response response = new Response.Builder()
                .code(200)
                .message("success")
                .protocol(Protocol.HTTP_1_0)
                .body(ResponseBody.create(MediaType.parse("application/json"), "success".getBytes()))
                .addHeader("content-type", "application/json")
                .build();


        retrofit2.Response response1 = retrofit2.Response.success(response);
        callback.onResponse(response1, new LinkHeaders(), ApiType.API);
    }

    public static void authenticateCanvasParentAirwolf(String domain, RestBuilder adapter, RestParams params, StatusCallback<ParentResponse> callback) {
        adapter.setStatusCallback(callback);

        Response response = new Response.Builder()
                .code(200)
                .message("success")
                .protocol(Protocol.HTTP_1_0)
                .body(ResponseBody.create(MediaType.parse("application/json"), "success".getBytes()))
                .addHeader("content-type", "application/json")
                .build();


        retrofit2.Response response1 = retrofit2.Response.success(response);
        callback.onResponse(response1, new LinkHeaders(), ApiType.API);
    }
}
