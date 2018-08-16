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
import android.support.annotation.WorkerThread;

import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.builders.RestBuilder;
import com.instructure.canvasapi2.builders.RestParams;
import com.instructure.canvasapi2.managers.FileFolderManager;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.CreateFolder;
import com.instructure.canvasapi2.models.FileFolder;
import com.instructure.canvasapi2.models.FileUploadParams;
import com.instructure.canvasapi2.models.License;
import com.instructure.canvasapi2.models.RemoteFile;
import com.instructure.canvasapi2.models.UpdateFileFolder;
import com.instructure.canvasapi2.models.UsageRights;
import com.instructure.canvasapi2.utils.APIHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;


public class FileFolderAPI {

    interface FilesFoldersInterface {
        @GET("{contextId}/folders/root")
        Call<FileFolder> getRootFolderForContext(@Path("contextId") long contextId);

        @GET("self/folders/root")
        Call<FileFolder> getRootUserFolder();

        @GET("folders/{folderId}")
        Call<FileFolder> getFolder(@Path("folderId") long folderId);

        @GET("folders/{folderId}/folders")
        Call<List<FileFolder>> getFirstPageFolders(@Path("folderId") long folderId);

        @GET("folders/{folderId}/files?include[]=usage_rights")
        Call<List<FileFolder>> getFirstPageFiles(@Path("folderId") long folderId);

        @GET("{fileUrl}")
        Call<FileFolder> getFileFolderFromURL(@Path(value = "fileUrl", encoded = true) String fileURL);

        @GET
        Call<List<FileFolder>> getNextPageFileFoldersList(@Url String nextURL);

        @POST("folders/{folderId}/files")
        Call<FileFolder> uploadFile(@Path("folderId") long folderId);

        @DELETE("files/{fileId}")
        Call<FileFolder> deleteFile(@Path("fileId") long fileId);

        @PUT("files/{fileId}?include[]=usage_rights")
        Call<FileFolder> updateFile(@Path("fileId") long fileId, @Body UpdateFileFolder updateFileFolder);

        @POST("folders/{folderId}/folders")
        Call<FileFolder> createFolder(@Path("folderId") long folderId, @Body CreateFolder newFolderName);

        @DELETE("folders/{folderId}?force=true")
        Call<FileFolder> deleteFolder(@Path("folderId") long folderId);

        @PUT("folders/{folderId}")
        Call<FileFolder> updateFolder(@Path("folderId") long folderId, @Body UpdateFileFolder updateFileFolder);

        @FormUrlEncoded
        @PUT("courses/{courseId}/usage_rights")
        Call<UsageRights> updateUsageRights(@Path("courseId") long courseId, @FieldMap Map<String, Object> params);

        @GET("courses/{courseId}/content_licenses")
        Call<ArrayList<License>> getCourseFileLicenses(@Path("courseId") long courseId);


        @POST("courses/{courseId}/files")
        Call<FileUploadParams> getFileUploadParams(
                @Path("courseId") long courseId,
                @Query("size") long size,
                @Query("name") String fileName,
                @Query("content_type") String content_type,
                @Query("parent_folder_id") Long parentFolderId,
                @Query("on_duplicate") String overwriteStrategy);

        @POST("courses/{courseId}/files")
        Call<FileUploadParams> getFileUploadParams(
                @Path("courseId") long courseId,
                @Query("size") long size,
                @Query("name") String fileName,
                @Query("content_type") String content_type,
                @Query("parent_folder_path") String parentFolderPath,
                @Query("on_duplicate") String overwriteStrategy);

        @Multipart
        @POST("/")
        Call<RemoteFile> uploadFile(@PartMap Map<String, RequestBody> params, @Part("file") RequestBody file);

        @GET
        Call<RemoteFile> postNewlyCreatedFile(@Url String url);
    }

    public static void getFileFolderFromURL(@NonNull RestBuilder adapter, String url, StatusCallback<FileFolder> callback, @NonNull RestParams params) {
        if (APIHelper.paramIsNull(callback, url)) {
            return;
        }
        //TODO: add pagination
        callback.addCall(adapter.build(FilesFoldersInterface.class, params).getFileFolderFromURL(url)).enqueue(callback);
    }

    @WorkerThread
    public static FileFolder getFileFolderFromURLSynchronous(@NonNull RestBuilder adapter, @NonNull String url, @NonNull RestParams params) {
        if (APIHelper.paramIsNull(url)) {
            return null;
        }
        //TODO: add pagination
        try {
            return adapter.build(FilesFoldersInterface.class, params).getFileFolderFromURL(url).execute().body();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void getRootFolderForContext(@NonNull RestBuilder adapter, CanvasContext canvasContext, StatusCallback<FileFolder> callback, @NonNull RestParams params) {
        if (APIHelper.paramIsNull(callback)) {
            return;
        }
        if (canvasContext.getType() == CanvasContext.Type.USER) {
            callback.addCall(adapter.build(FilesFoldersInterface.class, params).getRootUserFolder()).enqueue(callback);
        } else {
            callback.addCall(adapter.build(FilesFoldersInterface.class, params).getRootFolderForContext(canvasContext.getId())).enqueue(callback);
        }
    }

    public static void getRootFolderForUser(@NonNull RestBuilder adapter, StatusCallback<FileFolder> callback, @NonNull RestParams params) {
        if (APIHelper.paramIsNull(callback)) {
            return;
        }
        callback.addCall(adapter.build(FilesFoldersInterface.class, params).getRootUserFolder()).enqueue(callback);
    }

    public static void getFolder(long folderId, @NonNull RestBuilder adapter, @NonNull RestParams params, @NonNull StatusCallback<FileFolder> callback) {
        callback.addCall(adapter.build(FilesFoldersInterface.class, params).getFolder(folderId)).enqueue(callback);
    }

    public static void getFirstPageFolders(@NonNull RestBuilder adapter, long folderId, StatusCallback<List<FileFolder>> callback, @NonNull RestParams params) {
        if (APIHelper.paramIsNull(callback)) {
            return;
        }
        callback.addCall(adapter.build(FilesFoldersInterface.class, params).getFirstPageFolders(folderId)).enqueue(callback);
    }

    public static void getFirstPageFiles(@NonNull RestBuilder adapter, long folderId, StatusCallback<List<FileFolder>> callback, @NonNull RestParams params) {
        if (APIHelper.paramIsNull(callback)) {
            return;
        }
        callback.addCall(adapter.build(FilesFoldersInterface.class, params).getFirstPageFiles(folderId)).enqueue(callback);
    }

    public static void getNextPageFilesFolder(@NonNull RestBuilder adapter, String nextUrl, StatusCallback<List<FileFolder>> callback, @NonNull RestParams params) {
        if (APIHelper.paramIsNull(callback, nextUrl)) {
            return;
        }
        callback.addCall(adapter.build(FilesFoldersInterface.class, params).getNextPageFileFoldersList(nextUrl)).enqueue(callback);
    }

    public static void deleteFile(@NonNull RestBuilder adapter, long fileId, StatusCallback<FileFolder> callback, @NonNull RestParams params) {
        if (APIHelper.paramIsNull(callback)) {
            return;
        }
        callback.addCall(adapter.build(FilesFoldersInterface.class, params).deleteFile(fileId)).enqueue(callback);
    }

    public static void updateFile(long fileId, UpdateFileFolder updateFileFolder, RestBuilder adapter, StatusCallback<FileFolder> callback, RestParams params) {
        if (APIHelper.paramIsNull(callback)) return;
        callback.addCall(adapter.build(FilesFoldersInterface.class, params).updateFile(fileId, updateFileFolder)).enqueue(callback);
    }

    public static void createFolder(long folderId, CreateFolder folder, RestBuilder adapter, StatusCallback<FileFolder> callback, RestParams params) {
        if (APIHelper.paramIsNull(callback)) return;
        callback.addCall(adapter.build(FilesFoldersInterface.class, params).createFolder(folderId, folder)).enqueue(callback);
    }

    public static void deleteFolder(long folderId, RestBuilder adapter, StatusCallback<FileFolder> callback, RestParams params) {
        if (APIHelper.paramIsNull(callback)) return;
        callback.addCall(adapter.build(FilesFoldersInterface.class, params).deleteFolder(folderId)).enqueue(callback);
    }

    public static void updateFolder(long folderId, UpdateFileFolder updateFileFolder, RestBuilder adapter, StatusCallback<FileFolder> callback, RestParams params) {
        if (APIHelper.paramIsNull(callback)) return;
        callback.addCall(adapter.build(FilesFoldersInterface.class, params).updateFolder(folderId, updateFileFolder)).enqueue(callback);
    }

    public static void updateUsageRights(long courseId, Map<String, Object> formParams, RestBuilder adapter, StatusCallback<UsageRights> callback, RestParams params) {
        if (APIHelper.paramIsNull(callback)) return;
        callback.addCall(adapter.build(FilesFoldersInterface.class, params).updateUsageRights(courseId, formParams)).enqueue(callback);
    }

    public static void getCourseFileLicenses(long courseId, RestBuilder adapter, StatusCallback<ArrayList<License>> callback, RestParams params) {
        if (APIHelper.paramIsNull(callback)) return;
        callback.addCall(adapter.build(FilesFoldersInterface.class, params).getCourseFileLicenses(courseId)).enqueue(callback);
    }

    @WorkerThread
    public static FileUploadParams getFileUploadParamsSynchronous(@NonNull RestBuilder adapter, long courseId, String fileName, long size, String contentType, Long parentFolderId) throws IOException {
        RestParams params = new RestParams.Builder().withPerPageQueryParam(false).build();
        return adapter.build(FilesFoldersInterface.class, params).getFileUploadParams(courseId, size, fileName, contentType, parentFolderId, "rename").execute().body();
    }

    @WorkerThread
    public static FileUploadParams getFileUploadParamsSynchronous(@NonNull RestBuilder adapter, long courseId, String fileName, long size, String contentType, String parentFolderPath) throws IOException {
        RestParams params = new RestParams.Builder().withPerPageQueryParam(false).build();
        return adapter.build(FilesFoldersInterface.class, params).getFileUploadParams(courseId, size, fileName, contentType, parentFolderPath, "rename").execute().body();
    }

    @WorkerThread
    public static RemoteFile uploadFileSynchronous(@NonNull RestBuilder adapter, String uploadUrl, Map<String, RequestBody> uploadParams, String mimeType, File file) throws IOException {
        RestParams params = new RestParams.Builder().withShouldIgnoreToken(true).withDomain(uploadUrl).withPerPageQueryParam(false).build();
        RequestBody fileBody = RequestBody.create(MediaType.parse(mimeType), file);
        return adapter.build(FilesFoldersInterface.class, params).uploadFile(uploadParams, fileBody).execute().body();
    }

    @WorkerThread
    /* Used to manually handle the last redirect */
    public static RemoteFile uploadFileSynchronousNoRedirect(@NonNull RestBuilder adapter, String uploadUrl, Map<String, RequestBody> uploadParams, String mimeType, File file) throws IOException {
        RestParams params = new RestParams.Builder().withShouldIgnoreToken(true).withDomain(uploadUrl).withPerPageQueryParam(false).build();
        RequestBody fileBody = RequestBody.create(MediaType.parse(mimeType), file);
        Headers headers = adapter.buildNoRedirects(FilesFoldersInterface.class, params).uploadFile(uploadParams, fileBody).execute().headers();
        String redirect = headers.get("Location");
        String newFileUrl = redirect.split("/create_success")[0];
        // POST to the redirect... according to the docs we need to do this to finalize the file upload process
        RemoteFile newFile = adapter.build(FilesFoldersInterface.class, params).postNewlyCreatedFile(redirect).execute().body();
        // We weren't receiving a url in the response from the POST to the redirect - here we grab the full file info as a work around
        FileFolder fileFolder = FileFolderManager.getFileFolderFromURLSynchronous(newFileUrl);
        newFile.setUrl(fileFolder.getUrl());
        newFile.setThumbnailUrl(fileFolder.getThumbnailUrl());
        return newFile;
    }
}
