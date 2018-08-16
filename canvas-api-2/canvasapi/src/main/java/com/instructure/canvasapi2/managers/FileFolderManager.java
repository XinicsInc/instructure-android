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

package com.instructure.canvasapi2.managers;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.apis.FileFolderAPI;
import com.instructure.canvasapi2.builders.RestBuilder;
import com.instructure.canvasapi2.builders.RestParams;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.CreateFolder;
import com.instructure.canvasapi2.models.FileFolder;
import com.instructure.canvasapi2.models.FileUploadParams;
import com.instructure.canvasapi2.models.License;
import com.instructure.canvasapi2.models.RemoteFile;
import com.instructure.canvasapi2.models.UpdateFileFolder;
import com.instructure.canvasapi2.models.UsageRights;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.ExhaustiveListCallback;
import com.instructure.canvasapi2.utils.LinkHeaders;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import okhttp3.RequestBody;
import retrofit2.Response;

public class FileFolderManager extends BaseManager {

    private static boolean mTesting = false;

    public static void getFileFolderFromURL(String url, StatusCallback<FileFolder> callback) {

        if (isTesting() || mTesting) {
            //TODO:
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .build();

            FileFolderAPI.getFileFolderFromURL(adapter, url, callback, params);
        }
    }

    public static FileFolder getFileFolderFromURLSynchronous(String url) {

        if (isTesting() || mTesting) {
            //TODO:
        } else {
            RestBuilder adapter = new RestBuilder(new StatusCallback() {});
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .build();

           return FileFolderAPI.getFileFolderFromURLSynchronous(adapter, url, params);
        }

        return null;
    }

    public static void getFileFolderFromURLAirwolf(String airwolfDomain, String url, StatusCallback<FileFolder> callback) {

        if (isTesting() || mTesting) {
            //TODO:
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .withDomain(airwolfDomain)
                    .withAPIVersion("")
                    .build();

            FileFolderAPI.getFileFolderFromURL(adapter, url, callback, params);
        }
    }

    public static void getRootFolderForContext(CanvasContext canvasContext, boolean forceNetwork, StatusCallback<FileFolder> callback) {

        if (isTesting() || mTesting) {
            //TODO:
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withCanvasContext(canvasContext)
                    .withForceReadFromNetwork(forceNetwork)
                    .withPerPageQueryParam(false)
                    .build();

            FileFolderAPI.getRootFolderForContext(adapter, canvasContext, callback, params);
        }
    }

    public static void getRootFolderForUser(boolean forceNetwork, StatusCallback<FileFolder> callback) {

        if (isTesting() || mTesting) {
            //TODO
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .withPerPageQueryParam(false)
                    .build();

            FileFolderAPI.getRootFolderForUser(adapter, callback, params);
        }
    }

    public static void getFolder(long folderId, boolean forceNetwork, StatusCallback<FileFolder> callback) {
        if (isTesting() || mTesting) {

        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .build();

            FileFolderAPI.getFolder(folderId, adapter, params, callback);
        }
    }

    public static void getFirstPageFolders(long folderId, boolean forceNetwork, StatusCallback<List<FileFolder>> callback) {

        if (isTesting() || mTesting) {
            //TODO
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .withPerPageQueryParam(true)
                    .build();

            FileFolderAPI.getFirstPageFolders(adapter, folderId, callback, params);
        }
    }

    public static void getFirstPageFiles(long folderId, boolean forceNetwork, StatusCallback<List<FileFolder>> callback) {

        if (isTesting() || mTesting) {
            //TODO
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .withPerPageQueryParam(true)
                    .build();

            FileFolderAPI.getFirstPageFiles(adapter, folderId, callback, params);
        }
    }

    public static void getNextPageFilesFolder(String url, boolean forceNetwork, StatusCallback<List<FileFolder>> callback) {

        if (isTesting() || mTesting) {
            //TODO
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .withPerPageQueryParam(false)
                    .build();

            FileFolderAPI.getNextPageFilesFolder(adapter, url, callback, params);
        }
    }

    public static void getFirstPageFoldersRoot(CanvasContext canvasContext, boolean forceNetwork, final StatusCallback<List<FileFolder>> callback) {
        if (isTesting() || mTesting) {
            //TODO
        } else {
            final RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withCanvasContext(canvasContext)
                    .withForceReadFromNetwork(forceNetwork)
                    .withPerPageQueryParam(true)
                    .build();

            final RestParams folderParams = new RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .build();

            FileFolderAPI.getRootFolderForContext(adapter, canvasContext, new StatusCallback<FileFolder>() {
                @Override
                public void onResponse(@NonNull Response<FileFolder> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                    FileFolderAPI.getFirstPageFolders(adapter, response.body().getId(), callback, folderParams);
                }
            }, params);
        }
    }

    public static void getFirstPageFilesRoot(CanvasContext canvasContext, boolean forceNetwork, final StatusCallback<List<FileFolder>> callback) {
        if (isTesting() || mTesting) {
            //TODO
        } else {
            final RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withCanvasContext(canvasContext)
                    .withForceReadFromNetwork(forceNetwork)
                    .withPerPageQueryParam(true)
                    .build();

            final RestParams fileParams = new RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .build();

            FileFolderAPI.getRootFolderForContext(adapter, canvasContext, new StatusCallback<FileFolder>() {
                @Override
                public void onResponse(@NonNull Response<FileFolder> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                    FileFolderAPI.getFirstPageFiles(adapter, response.body().getId(), callback, fileParams);
                }
            }, params);
        }
    }

    public static void deleteFile(long fileId, StatusCallback<FileFolder> callback) {

        if (isTesting() || mTesting) {
            //TODO
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder().build();

            FileFolderAPI.deleteFile(adapter, fileId, callback, params);
        }
    }

    public static void getAllFoldersRoot(CanvasContext canvasContext, final boolean forceNetwork, final StatusCallback<List<FileFolder>> callback) {
        if (isTesting() || mTesting) {
            //TODO
        } else {
            final RestBuilder adapter = new RestBuilder(callback);
            final RestParams params = new RestParams.Builder()
                    .withCanvasContext(canvasContext)
                    .withForceReadFromNetwork(forceNetwork)
                    .withPerPageQueryParam(true)
                    .build();

            FileFolderAPI.getRootFolderForContext(adapter, canvasContext, new StatusCallback<FileFolder>() {
                @Override
                public void onResponse(@NonNull Response<FileFolder> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                    getAllFolders(response.body().getId(), forceNetwork, callback);
                }
            }, params);
        }
    }

    public static void getAllFolders(long folderId, boolean forceNetwork, StatusCallback<List<FileFolder>> callback) {
        if (isTesting() || mTesting) {
            //TODO
        } else {
            final RestBuilder adapter = new RestBuilder(callback);
            final RestParams params = new RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .withPerPageQueryParam(true)
                    .build();

            final StatusCallback<List<FileFolder>> depaginatedCallback = new ExhaustiveListCallback<FileFolder>(callback) {
                @Override
                public void getNextPage(@NonNull StatusCallback<List<FileFolder>> callback, @NonNull String nextUrl, boolean isCached) {
                    FileFolderAPI.getNextPageFilesFolder(adapter, nextUrl, callback, params);
                }
            };

            FileFolderAPI.getFirstPageFolders(adapter, folderId, depaginatedCallback, params);
        }
    }

    public static void getAllFilesRoot(CanvasContext canvasContext, final boolean forceNetwork, final StatusCallback<List<FileFolder>> callback) {
        if (isTesting() || mTesting) {
            //TODO
        } else {
            final RestBuilder adapter = new RestBuilder(callback);
            final RestParams params = new RestParams.Builder()
                    .withCanvasContext(canvasContext)
                    .withForceReadFromNetwork(forceNetwork)
                    .withPerPageQueryParam(true)
                    .build();

            FileFolderAPI.getRootFolderForContext(adapter, canvasContext, new StatusCallback<FileFolder>() {
                @Override
                public void onResponse(@NonNull Response<FileFolder> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                    getAllFiles(response.body().getId(), forceNetwork, callback);
                }
            }, params);
        }
    }

    public static void getAllFiles(long folderId, boolean forceNetwork, StatusCallback<List<FileFolder>> callback) {
        if (isTesting() || mTesting) {
            //TODO
        } else {
            final RestBuilder adapter = new RestBuilder(callback);
            final RestParams params = new RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .withPerPageQueryParam(true)
                    .build();

            final StatusCallback<List<FileFolder>> depaginatedCallback = new ExhaustiveListCallback<FileFolder>(callback) {
                @Override
                public void getNextPage(@NonNull StatusCallback<List<FileFolder>> callback, @NonNull String nextUrl, boolean isCached) {
                    FileFolderAPI.getNextPageFilesFolder(adapter, nextUrl, callback, params);
                }
            };

            FileFolderAPI.getFirstPageFiles(adapter, folderId, depaginatedCallback, params);
        }
    }

    public static void createFolder(long folderId, @NotNull CreateFolder folder, final StatusCallback<FileFolder> callback) {
        if (isTesting() || mTesting) {
            //TODO
        } else {
            final RestBuilder adapter = new RestBuilder(callback);
            final RestParams params = new RestParams.Builder()
                    .build();

            FileFolderAPI.createFolder(folderId, folder, adapter, callback, params);
        }
    }

    public static void deleteFolder(long folderId, final StatusCallback<FileFolder> callback) {
        if (isTesting() || mTesting) {
            //TODO
        } else {
            final RestBuilder adapter = new RestBuilder(callback);
            final RestParams params = new RestParams.Builder()
                    .build();

            FileFolderAPI.deleteFolder(folderId, adapter, callback, params);
        }
    }

    public static void updateFolder(long folderId, UpdateFileFolder updateFileFolder, final StatusCallback<FileFolder> callback) {
        if (isTesting() || mTesting) {
            //TODO
        } else {
            final RestBuilder adapter = new RestBuilder(callback);
            final RestParams params = new RestParams.Builder()
                    .build();

            FileFolderAPI.updateFolder(folderId, updateFileFolder, adapter, callback, params);
        }
    }

    public static void updateUsageRights(long courseId, Map<String, Object> formParams, final StatusCallback<UsageRights> callback) {
        if (isTesting() || mTesting) {
            //TODO
        } else {
            final RestBuilder adapter = new RestBuilder(callback);
            final RestParams params = new RestParams.Builder()
                    .build();

            FileFolderAPI.updateUsageRights(courseId, formParams, adapter, callback, params);
        }
    }

    public static void updateFile(long fileId, UpdateFileFolder updateFileFolder, final StatusCallback<FileFolder> callback) {
        if (isTesting() || mTesting) {
            //TODO
        } else {
            final RestBuilder adapter = new RestBuilder(callback);
            final RestParams params = new RestParams.Builder()
                    .build();

            FileFolderAPI.updateFile(fileId, updateFileFolder, adapter, callback, params);
        }
    }

    public static void getCourseFileLicenses(long courseId, final StatusCallback<ArrayList<License>> callback) {
        if (isTesting() || mTesting) {
            //TODO
        } else {
            final RestBuilder adapter = new RestBuilder(callback);
            final RestParams params = new RestParams.Builder()
                    .build();

            FileFolderAPI.getCourseFileLicenses(courseId, adapter, callback, params);
        }
    }

    @Nullable
    @WorkerThread
    public static FileUploadParams getFileUploadParamsSynchronous(long courseId, String fileName, long size, String contentType, Long parentFolderId) throws IOException {
        RestBuilder adapter = new RestBuilder(new StatusCallback() {});
        if (isTesting() || mTesting) {
            return null;
        } else {
            return FileFolderAPI.getFileUploadParamsSynchronous(adapter, courseId, fileName, size, contentType, parentFolderId);
        }
    }

    @Nullable
    @WorkerThread
    public static FileUploadParams getFileUploadParamsSynchronous(long courseId, String fileName, long size, String contentType, String parentFolderPath) throws IOException {
        RestBuilder adapter = new RestBuilder(new StatusCallback() {});
        if (isTesting() || mTesting) {
            return null;
        } else {
            return FileFolderAPI.getFileUploadParamsSynchronous(adapter, courseId, fileName, size, contentType, parentFolderPath);
        }
    }

    @Nullable
    @WorkerThread
    public static RemoteFile uploadFileSynchronous(String uploadUrl, Map<String, RequestBody> uploadParams, String mimeType, File file) throws IOException {
        RestBuilder adapter = new RestBuilder(new StatusCallback() {});
        if (isTesting() || mTesting) {
            return null;
        } else {
            return FileFolderAPI.uploadFileSynchronous(adapter, uploadUrl, uploadParams, mimeType, file);
        }
    }

    @Nullable
    @WorkerThread
    public static RemoteFile uploadFileSynchronousNoRedirect(String uploadUrl, Map<String, RequestBody> uploadParams, String mimeType, File file) throws IOException {
        RestBuilder adapter = new RestBuilder(new StatusCallback() {});
        if (isTesting() || mTesting) {
            return null;
        } else {
            return FileFolderAPI.uploadFileSynchronousNoRedirect(adapter, uploadUrl, uploadParams, mimeType, file);
        }
    }
}
