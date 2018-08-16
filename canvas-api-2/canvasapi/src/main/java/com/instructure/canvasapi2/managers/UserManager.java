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
import com.instructure.canvasapi2.apis.AvatarAPI;
import com.instructure.canvasapi2.apis.UserAPI;
import com.instructure.canvasapi2.builders.RestBuilder;
import com.instructure.canvasapi2.builders.RestParams;
import com.instructure.canvasapi2.models.AccountRole;
import com.instructure.canvasapi2.models.CanvasColor;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.Enrollment;
import com.instructure.canvasapi2.models.FileUploadParams;
import com.instructure.canvasapi2.models.Parent;
import com.instructure.canvasapi2.models.ParentResponse;
import com.instructure.canvasapi2.models.RemoteFile;
import com.instructure.canvasapi2.models.ResetParent;
import com.instructure.canvasapi2.models.Student;
import com.instructure.canvasapi2.models.TermsOfService;
import com.instructure.canvasapi2.models.User;
import com.instructure.canvasapi2.tests.UserManager_Test;
import com.instructure.canvasapi2.utils.ExhaustiveListCallback;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;

public class UserManager extends BaseManager {

    private static boolean mTesting = false;

    public static void getColors(@NonNull StatusCallback<CanvasColor> callback, boolean forceNetwork) {
        RestBuilder adapter = new RestBuilder(callback);
        RestParams params = new RestParams.Builder()
                .withPerPageQueryParam(false)
                .withForceReadFromCache(!forceNetwork)
                .withForceReadFromNetwork(forceNetwork)
                .build();

        if(isTesting() || mTesting) {
            UserManager_Test.getColors(adapter, callback, params);
        } else {
            UserAPI.getColors(adapter, callback, params);
        }
    }

    public static void setColors(@NonNull StatusCallback<CanvasColor> callback, @NonNull String contextId, int color) {
        RestBuilder adapter = new RestBuilder(callback);

        if(isTesting() || mTesting) {
            UserManager_Test.setColors(adapter, callback, contextId, color);
        } else {
            UserAPI.setColor(adapter, callback, contextId, color);
        }
    }

    @Nullable
    @WorkerThread
    public static RemoteFile uploadUserFileSynchronous(String uploadUrl, Map<String, RequestBody> uploadParams, String mimeType, File file) throws IOException {
        RestBuilder adapter = new RestBuilder();
        if (isTesting() || mTesting) {
            return UserManager_Test.uploadUserFileSynchronous(adapter, uploadUrl, uploadParams, mimeType, file);
        } else {
            return UserAPI.uploadUserFileSynchronous(adapter, uploadUrl, uploadParams, mimeType, file);
        }
    }

    @Nullable
    @WorkerThread
    public static FileUploadParams getFileUploadParamsSynchronous(String fileName, long size, String contentType, Long parentFolderId) throws IOException {
        RestBuilder adapter = new RestBuilder();
        if (isTesting() || mTesting) {
            return UserManager_Test.getFileUploadParamsSynchronous(adapter, fileName, size, contentType, parentFolderId);
        } else {
            return UserAPI.getFileUploadParamsSynchronous(adapter, fileName, size, contentType, parentFolderId);
        }
    }

    @Nullable
    @WorkerThread
    public static FileUploadParams getFileUploadParamsSynchronous(String fileName, long size, String contentType, String parentFolderPath) throws IOException {
        RestBuilder adapter = new RestBuilder();
        if (isTesting() || mTesting) {
            return UserManager_Test.getFileUploadParamsSynchronous(adapter, fileName, size, contentType, parentFolderPath);
        } else {
            return UserAPI.getFileUploadParamsSynchronous(adapter, fileName, size, contentType, parentFolderPath);
        }
    }

    public static void getSelf(StatusCallback<User> callback) {
        if (isTesting() || mTesting) {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withShouldIgnoreToken(false)
                    .withPerPageQueryParam(false)
                    .build();
            UserManager_Test.getSelf(adapter, params, callback);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withShouldIgnoreToken(false)
                    .withPerPageQueryParam(false)
                    .build();
            UserAPI.getSelf(adapter, params, callback );
        }
    }

    public static void getSelf(boolean forceNetwork, StatusCallback<User> callback) {
        if (isTesting() || mTesting) {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withShouldIgnoreToken(false)
                    .withPerPageQueryParam(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();
            UserManager_Test.getSelf(adapter, params, callback);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withShouldIgnoreToken(false)
                    .withPerPageQueryParam(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();
            UserAPI.getSelf(adapter, params, callback );
        }
    }

    public static void getSelfEnrollments(boolean forceNetwork, StatusCallback<List<Enrollment>> callback) {
        if (isTesting() || mTesting) {
            //TODO
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withShouldIgnoreToken(false)
                    .withPerPageQueryParam(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();
            UserAPI.getSelfEnrollments(adapter, params, callback);
        }
    }

    public static void getSelfWithPermissions(boolean forceNetwork, StatusCallback<User> callback) {
        if (isTesting() || mTesting) {

        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withShouldIgnoreToken(false)
                    .withPerPageQueryParam(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();
            UserAPI.getSelfWithPermissions(adapter, params, callback );
        }
    }

    public static void getUser(Long userId, StatusCallback<User> callback, boolean forceNetwork) {
        if (isTesting() || mTesting) {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withShouldIgnoreToken(false)
                    .withPerPageQueryParam(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();
            UserManager_Test.getUser(adapter, params, userId, callback);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withShouldIgnoreToken(false)
                    .withPerPageQueryParam(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();
            UserAPI.getUser(adapter, params, userId, callback);
        }
    }

    public static void getUserForContextId(CanvasContext canvasContext, Long userId, StatusCallback<User> callback, boolean forceNetwork) {
        if (isTesting() || mTesting) {
            //TODO
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withShouldIgnoreToken(false)
                    .withPerPageQueryParam(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();
            UserAPI.getUserForContextId(adapter, params, canvasContext, userId, callback);
        }
    }

    public static void getPeopleList(CanvasContext canvasContext, StatusCallback<List<User>> callback, boolean forceNetwork) {

        if (isTesting() || mTesting) {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .build();

            UserManager_Test.getPeopleList(adapter, params, canvasContext.getId(), callback);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withCanvasContext(canvasContext)
                    .withPerPageQueryParam(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();

            UserAPI.getPeopleList(adapter, params, canvasContext.getId(), callback);
        }
    }

    public static void getAllPeopleList(final CanvasContext canvasContext, StatusCallback<List<User>> callback, boolean forceNetwork) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            final RestParams params = new RestParams.Builder()
                    .withCanvasContext(canvasContext)
                    .withForceReadFromNetwork(forceNetwork)
                    .withPerPageQueryParam(true)
                    .withShouldIgnoreToken(false)
                    .build();

            // We don't want the canvas context on the paginated params
            final RestParams paginatedParams = new RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .withPerPageQueryParam(true)
                    .withShouldIgnoreToken(false)
                    .build();

            final RestBuilder adapter = new RestBuilder(callback);
            StatusCallback<List<User>> depaginatedCallback = new ExhaustiveListCallback<User>(callback) {
                @Override
                public void getNextPage(@NonNull StatusCallback<List<User>> callback, @NonNull String nextUrl, boolean isCached) {
                    UserAPI.getPeopleList(adapter, paginatedParams, canvasContext.getId(), callback);
                }
            };
            adapter.setStatusCallback(depaginatedCallback);
            UserAPI.getPeopleList(adapter, params, canvasContext.getId(), depaginatedCallback);
        }
    }

    public static void getAllEnrollmentsPeopleList(final CanvasContext canvasContext, StatusCallback<List<User>> callback, final boolean forceNetwork) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            final RestParams params = new RestParams.Builder()
                    .withCanvasContext(canvasContext)
                    .withForceReadFromNetwork(forceNetwork)
                    .withPerPageQueryParam(true)
                    .withShouldIgnoreToken(false)
                    .build();

            // We don't want the canvas context on the paginated params
            final RestParams paginatedParams = new RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .withPerPageQueryParam(true)
                    .withShouldIgnoreToken(false)
                    .build();

            final RestBuilder adapter = new RestBuilder(callback);
            StatusCallback<List<User>> depaginatedCallback = new ExhaustiveListCallback<User>(callback) {
                @Override
                public void getNextPage(@NonNull StatusCallback<List<User>> callback, @NonNull String nextUrl, boolean isCached) {

                    UserAPI.getAllPeopleList(adapter, paginatedParams, canvasContext.getId(), callback);
                }
            };
            adapter.setStatusCallback(depaginatedCallback);
            UserAPI.getAllPeopleList(adapter, params, canvasContext.getId(), depaginatedCallback);
        }
    }

    public static void getFirstPagePeopleList(@NonNull CanvasContext canvasContext, UserAPI.ENROLLMENT_TYPE enrollmentType, boolean forceNetwork, @NonNull StatusCallback<List<User>> callback) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            final RestParams params = new RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .withPerPageQueryParam(true)
                    .withShouldIgnoreToken(false)
                    .withCanvasContext(canvasContext)
                    .build();
            final RestBuilder adapter = new RestBuilder(callback);
            UserAPI.getFirstPagePeopleList(adapter, params, canvasContext.getId(), enrollmentType, callback);
        }

    }

    public static void getFirstPagePeopleList(@NonNull CanvasContext canvasContext, boolean forceNetwork, @NonNull StatusCallback<List<User>> callback) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            final RestParams params = new RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .withPerPageQueryParam(true)
                    .withShouldIgnoreToken(false)
                    .withCanvasContext(canvasContext)
                    .build();
            final RestBuilder adapter = new RestBuilder(callback);
            UserAPI.getFirstPagePeopleList(adapter, params, canvasContext.getId(), callback);
        }

    }

    public static void getNextPagePeopleList(boolean forceNetwork, @NonNull String nextUrl, @NonNull StatusCallback<List<User>> callback) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            final RestParams params = new RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .withPerPageQueryParam(true)
                    .withShouldIgnoreToken(false)
                    .build();
            final RestBuilder adapter = new RestBuilder(callback);
            UserAPI.getNextPagePeopleList(adapter, params, nextUrl, callback);
        }

    }

    public static void updateUserShortNameAndEmail(String shortName, String email, String bio, StatusCallback<User> callback) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .build();

            UserAPI.updateUserShortNameAndEmail(adapter, params, shortName, email, bio, callback);
        }
    }

    public static void updateUserShortName(String shortName, StatusCallback<User> callback) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .build();

            UserAPI.updateUserShortName(adapter, params, shortName, callback);
        }
    }

    public static void updateUserEmail(String email, StatusCallback<User> callback) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .build();

            UserAPI.updateUserEmail(adapter, params, email, callback);
        }
    }

    public static void updateUsersAvatar(String urlPath, StatusCallback<User> callback) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .build();

            AvatarAPI.updateAvatar(adapter, params,urlPath, callback);
        }
    }

    public static void getTermsOfService(StatusCallback<TermsOfService> callback, boolean forceNetwork) {
        if (isTesting() || mTesting) {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withShouldIgnoreToken(false)
                    .withPerPageQueryParam(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();
            UserManager_Test.getTermsOfService(adapter, params, callback);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withShouldIgnoreToken(false)
                    .withPerPageQueryParam(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();
            UserAPI.getTermsOfService(adapter, params, callback);
        }
    }

    public static void addStudentToParentAirwolf(String airwolfDomain, String parentId, String studentDomain, StatusCallback<ResponseBody> callback) {

        if (isTesting() || mTesting) {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withDomain(airwolfDomain)
                    .withPerPageQueryParam(false)
                    .withAPIVersion("")
                    .withForceReadFromNetwork(true)
                    .build();
            UserManager_Test.addStudentToParentAirwolf(airwolfDomain, adapter, params, parentId, studentDomain, callback);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withDomain(airwolfDomain)
                    .withPerPageQueryParam(false)
                    .withAPIVersion("")
                    .withForceReadFromNetwork(true)
                    .build();
            UserAPI.addStudentToParentAirwolf(adapter, params, parentId, studentDomain, callback);
        }
    }

    public static void removeStudentAirwolf(String airwolfDomain, String parentId, String studentId, StatusCallback<ResponseBody> callback) {

        if (isTesting() || mTesting) {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withShouldIgnoreToken(false)
                    .withPerPageQueryParam(false)
                    .withDomain(airwolfDomain)
                    .withAPIVersion("")
                    .build();
            UserManager_Test.removeStudentAirwolf(airwolfDomain, adapter, params, parentId, studentId, callback);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withShouldIgnoreToken(false)
                    .withPerPageQueryParam(false)
                    .withDomain(airwolfDomain)
                    .withAPIVersion("")
                    .withForceReadFromNetwork(true)
                    .build();
            UserAPI.removeStudentAirwolf(adapter, params, parentId, studentId, callback);
        }
    }

    public static void addParentAirwolf(String airwolfDomain, Parent parent, StatusCallback<ParentResponse> callback) {
        if (isTesting() || mTesting) {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withShouldIgnoreToken(false)
                    .withPerPageQueryParam(false)
                    .withDomain(airwolfDomain)
                    .withAPIVersion("")
                    .withForceReadFromNetwork(true)
                    .build();
            UserManager_Test.addParentAirwolf(airwolfDomain, adapter, params, parent, callback);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withShouldIgnoreToken(false)
                    .withPerPageQueryParam(false)
                    .withDomain(airwolfDomain)
                    .withAPIVersion("")
                    .withForceReadFromNetwork(true)
                    .build();
            UserAPI.addParentAirwolf(adapter, params, parent, callback);
        }
    }

    public static void getStudentsForParentAirwolf(String airwolfDomain, String parentId, StatusCallback<List<Student>> callback) {
        if (isTesting() || mTesting) {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withShouldIgnoreToken(false)
                    .withPerPageQueryParam(true)
                    .withDomain(airwolfDomain)
                    .withAPIVersion("")
                    .build();
            UserManager_Test.getStudentsForParentAirwolf(airwolfDomain, adapter, params, parentId, callback);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withShouldIgnoreToken(false)
                    .withPerPageQueryParam(true)
                    .withDomain(airwolfDomain)
                    .withAPIVersion("")
                    .withForceReadFromNetwork(true)
                    .build();
            UserAPI.getStudentsForParentAirwolf(adapter, params, parentId, callback);
        }
    }

    public static void sendPasswordResetForParentAirwolf(String airwolfDomain, String userName, StatusCallback<ResponseBody> callback) {
        if (isTesting() || mTesting) {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withShouldIgnoreToken(true)
                    .withPerPageQueryParam(false)
                    .withDomain(airwolfDomain)
                    .withAPIVersion("")
                    .withForceReadFromNetwork(true)
                    .build();
            UserManager_Test.sendPasswordResetForParentAirwolf(airwolfDomain, adapter, params, userName, callback);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withShouldIgnoreToken(true)
                    .withPerPageQueryParam(false)
                    .withDomain(airwolfDomain)
                    .withAPIVersion("")
                    .withForceReadFromNetwork(true)
                    .build();
            UserAPI.sendPasswordResetForParentAirwolf(adapter, params, userName, callback);
        }
    }

    public static void authenticateParentAirwolf(String airwolfDomain, String email, String password, StatusCallback<ParentResponse> callback) {
        if (isTesting() || mTesting) {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withShouldIgnoreToken(false)
                    .withPerPageQueryParam(false)
                    .withDomain(airwolfDomain)
                    .withAPIVersion("")
                    .withForceReadFromNetwork(true)
                    .build();
            UserManager_Test.authenticateParentAirwolf(airwolfDomain, adapter, params, email, password, callback);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withShouldIgnoreToken(false)
                    .withPerPageQueryParam(false)
                    .withDomain(airwolfDomain)
                    .withAPIVersion("")
                    .withForceReadFromNetwork(true)
                    .build();
            UserAPI.authenticateParentAirwolf(adapter, params, email, password, callback);
        }
    }

    public static void resetParentPasswordAirwolf(String airwolfDomain, String username, String password, StatusCallback<ResetParent> callback) {
        if (isTesting() || mTesting) {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withShouldIgnoreToken(false)
                    .withPerPageQueryParam(false)
                    .withDomain(airwolfDomain)
                    .withAPIVersion("")
                    .withForceReadFromNetwork(true)
                    .build();
            UserManager_Test.resetParentPassword(airwolfDomain, adapter, params, username, password, callback);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withShouldIgnoreToken(false)
                    .withPerPageQueryParam(false)
                    .withDomain(airwolfDomain)
                    .withAPIVersion("")
                    .withForceReadFromNetwork(true)
                    .build();
            UserAPI.resetParentPassword(adapter, params, username, password, callback);
        }
    }

    public static void authenticateCanvasParentAirwolf(String airwolfDomain, String domain, StatusCallback<ParentResponse> callback) {
        if (isTesting() || mTesting) {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withShouldIgnoreToken(false)
                    .withPerPageQueryParam(false)
                    .withDomain(airwolfDomain)
                    .withAPIVersion("")
                    .withForceReadFromNetwork(true)
                    .build();
            UserManager_Test.authenticateCanvasParentAirwolf(domain, adapter, params, callback);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withShouldIgnoreToken(false)
                    .withPerPageQueryParam(false)
                    .withDomain(airwolfDomain)
                    .withAPIVersion("")
                    .withForceReadFromNetwork(true)
                    .build();
            UserAPI.authenticateCavnasParentAirwolf(adapter, params, domain, callback);
        }
    }

    public static void getSelfAccountRoles(boolean forceNetwork, StatusCallback<List<AccountRole>> callback) {
        if (isTesting()) {

        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .build();
            UserAPI.getSelfAccountRoles(adapter, params, callback);
        }
    }
}
