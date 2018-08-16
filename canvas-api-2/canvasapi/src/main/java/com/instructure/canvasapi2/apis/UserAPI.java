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
import com.instructure.canvasapi2.models.AccountRole;
import com.instructure.canvasapi2.models.CanvasColor;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.Enrollment;
import com.instructure.canvasapi2.models.FileUploadParams;
import com.instructure.canvasapi2.models.Parent;
import com.instructure.canvasapi2.models.ParentResponse;
import com.instructure.canvasapi2.models.ParentWrapper;
import com.instructure.canvasapi2.models.RemoteFile;
import com.instructure.canvasapi2.models.ResetParent;
import com.instructure.canvasapi2.models.Student;
import com.instructure.canvasapi2.models.TermsOfService;
import com.instructure.canvasapi2.models.User;
import com.instructure.canvasapi2.utils.APIHelper;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;


public class UserAPI {

    public enum ENROLLMENT_TYPE {STUDENT, TEACHER, TA, OBSERVER, DESIGNER}

    interface UsersInterface {

        @GET("users/self/colors")
        Call<CanvasColor> getColors();

        @PUT("users/self/colors/{context_id}")
        Call<CanvasColor> setColor(@Path("context_id") String contextId, @Query(value = "hexcode") String color);

        @PUT("users/self")
        Call<User> updateUserShortNameAndEmail(@Query("user[short_name]") String shortName, @Query("user[email]") String email, @Body String body);

        @PUT("users/self")
        Call<User> updateUserShortName(@Query("user[short_name]") String shortName, @Body String body);

        @PUT("users/self")
        Call<User> updateUserEmail(@Query("user[email]") String email, @Body String body);

        @Multipart
        @POST("/")
        Call<RemoteFile> uploadUserFile(@PartMap Map<String, RequestBody> params, @Part("file") RequestBody file);

        @POST("users/self/files")
        Call<FileUploadParams> getFileUploadParams(@Query("size") long size, @Query("name") String fileName, @Query("content_type") String content_type, @Query("parent_folder_id") Long parentFolderId, @Body String body);

        @POST("users/self/files")
        Call<FileUploadParams> getFileUploadParams( @Query("size") long size, @Query("name") String fileName, @Query("content_type") String content_type, @Query("parent_folder_path") String parentFolderPath, @Body String body);

        @GET("users/self/profile")
        Call<User> getSelf();

        @GET("users/self/enrollments?state[]=active&state[]=invited&state[]=completed")
        Call<List<Enrollment>> getSelfEnrollments();

        @GET("users/self")
        Call<User> getSelfWithPermissions();

        @GET("users/{user_id}/profile")
        Call<User> getUser(@Path("user_id") Long userId);

        @GET("{contextType}/{contextId}/users/{userId}?include[]=avatar_url&include[]=user_id&include[]=email&include[]=bio")
        Call<User> getUserForContextId(@Path("contextType") String contextType, @Path("contextId") long contextId, @Path("userId")long userId);

        @GET("{context_id}/users?include[]=enrollments&include[]=avatar_url&include[]=user_id&include[]=email&include[]=bio")
        Call<List<User>> getFirstPagePeopleList(@Path("context_id") long context_id, @Query("enrollment_type") String enrollmentType);

        @GET("{context_id}/users?include[]=enrollments&include[]=avatar_url&include[]=user_id&include[]=email&include[]=bio")
        Call<List<User>> getFirstPageAllPeopleList(@Path("context_id") long context_id);

        @GET
        Call<List<User>> next(@Url String nextURL);

        @GET("accounts/self/terms_of_service")
        Call<TermsOfService> getTermsOfService();

        @GET("accounts/self/roles")
        Call<List<AccountRole>> getAccountRoles();

        //region Airwolf
        @DELETE("student/{parentId}/{studentId}")
        Call<ResponseBody> removeStudentAirwolf(@Path("parentId") String parentId, @Path("studentId") String studentId);

        @PUT("newparent")
        Call<ParentResponse> addParentAirwolf(@Body ParentWrapper body);

        @POST("authenticate")
        Call<ParentResponse> authenticateParentAirwolf(@Body Parent body);

        @GET("students/{parentId}")
        Call<List<Student>> getStudentsForParentAirwolf(@Path("parentId") String parentId);

        @GET("add_student/{parentId}")
        Call<ResponseBody> addStudentToParentAirwolf(@Path("parentId") String parentId, @Query(value = "student_domain", encoded = false) String studentDomain);

        @POST("send_password_reset/{userName}")
        Call<ResponseBody> sendPasswordResetForParentAirwolf(@Path(value = "userName", encoded = false) String userName, @Body JSONObject body);

        @POST("reset_password")
        Call<ResetParent> resetParentPasswordAirwolf(@Body Parent parent);

        @GET("canvas/authenticate")
        Call<ParentResponse> authenticateCanvasParentAirwolf(@Query(value = "domain") String domain);

        //Only used for observer role in Canvas
        @GET("students/{observerId}")
        Call<List<Student>> getObserveesForParent(@Path("observerId") String observerId);
        //endregion
    }

    public static void getColors(@NonNull RestBuilder adapter, @NonNull StatusCallback<CanvasColor> callback, @NonNull RestParams params) {
        callback.addCall(adapter.build(UsersInterface.class, params).getColors()).enqueue(callback);
    }

    public static void setColor(@NonNull RestBuilder adapter, @NonNull StatusCallback<CanvasColor> callback, @NonNull String contextId, int color) {
        if (APIHelper.paramIsNull(adapter, callback, contextId)) { return; }

        //Modifies a color into a RRGGBB color string with no #.
        String hexColor = Integer.toHexString(color);
        hexColor = hexColor.substring(hexColor.length() - 6);

        if(hexColor.contains("#")) {
            hexColor = hexColor.replaceAll("#", "");
        }

        RestParams.Builder builder = new RestParams.Builder().withPerPageQueryParam(false);

        adapter.build(UsersInterface.class, builder.build()).setColor(contextId, hexColor).enqueue(callback);
    }

    @WorkerThread
    public static RemoteFile uploadUserFileSynchronous(@NonNull RestBuilder adapter, String uploadUrl, Map<String, RequestBody> uploadParams, String mimeType, File file) throws IOException {
        RestParams params = new RestParams.Builder().withShouldIgnoreToken(true).withDomain(uploadUrl).withPerPageQueryParam(false).build();
        RequestBody fileBody = RequestBody.create(MediaType.parse(mimeType), file);
        return adapter.build(UsersInterface.class, params).uploadUserFile(uploadParams, fileBody).execute().body();
    }

    @WorkerThread
    public static FileUploadParams getFileUploadParamsSynchronous(@NonNull RestBuilder adapter, String fileName, long size, String contentType, Long parentFolderId) throws IOException {
        RestParams params = new RestParams.Builder().withPerPageQueryParam(false).build();
        return adapter.build(UsersInterface.class, params).getFileUploadParams(size, fileName, contentType, parentFolderId, "").execute().body();
    }

    @WorkerThread
    public static FileUploadParams getFileUploadParamsSynchronous(@NonNull RestBuilder adapter, String fileName, long size, String contentType, String parentFolderPath) throws IOException {
        RestParams params = new RestParams.Builder().withPerPageQueryParam(false).build();
        return adapter.build(UsersInterface.class, params).getFileUploadParams(size, fileName, contentType, parentFolderPath, "").execute().body();
    }

    public static void getSelf(RestBuilder adapter, RestParams params, StatusCallback<User> callback) {
        callback.addCall(adapter.build(UsersInterface.class, params).getSelf()).enqueue(callback);
    }

    public static void getSelfEnrollments(RestBuilder adapter, RestParams params, StatusCallback<List<Enrollment>> callback) {
        callback.addCall(adapter.build(UsersInterface.class, params).getSelfEnrollments()).enqueue(callback);
    }

    public static void getSelfWithPermissions(RestBuilder adapter, RestParams params, StatusCallback<User> callback) {
        callback.addCall(adapter.build(UsersInterface.class, params).getSelfWithPermissions()).enqueue(callback);
    }

    public static void getUser(RestBuilder adapter, RestParams params, Long userId, StatusCallback<User> callback) {
        callback.addCall(adapter.build(UsersInterface.class, params).getUser(userId)).enqueue(callback);
    }

    public static void getUserForContextId(RestBuilder adapter, RestParams params, CanvasContext canvasContext, long userId, StatusCallback<User> callback) {
        callback.addCall(adapter.build(UsersInterface.class, params).getUserForContextId(canvasContext.apiContext(), canvasContext.getId(), userId)).enqueue(callback);
    }

    public static void updateUserShortNameAndEmail(RestBuilder adapter, RestParams params, String shortName, String email, String bio, StatusCallback<User> callback) {
        //At the time of creation the bio field is not supported via Canvas API, left here in hopes that one day all will be made right in the sight of engineers.
        callback.addCall(adapter.build(UsersInterface.class, params).updateUserShortNameAndEmail(shortName, email, "")).enqueue(callback);
    }

    public static void updateUserShortName(RestBuilder adapter, RestParams params, String shortName, StatusCallback<User> callback) {
        callback.addCall(adapter.build(UsersInterface.class, params).updateUserShortName(shortName, "")).enqueue(callback);
    }

    public static void updateUserEmail(RestBuilder adapter, RestParams params, String email, StatusCallback<User> callback) {
        callback.addCall(adapter.build(UsersInterface.class, params).updateUserEmail(email, "")).enqueue(callback);
    }

    public static void getPeopleList(@NonNull RestBuilder adapter, @NonNull RestParams params, @NonNull Long contextId, @NonNull StatusCallback<List<User>> callback) {
        if (StatusCallback.isFirstPage(callback.getLinkHeaders())) {
            callback.addCall(adapter.build(UsersInterface.class, params).getFirstPagePeopleList(contextId, getEnrollmentTypeString(ENROLLMENT_TYPE.STUDENT))).enqueue(callback);
        } else if (StatusCallback.moreCallsExist(callback.getLinkHeaders()) && callback.getLinkHeaders() != null) {
            callback.addCall(adapter.build(UsersInterface.class, params).next(callback.getLinkHeaders().nextUrl)).enqueue(callback);
        }
    }

    public static void getFirstPagePeopleList(@NonNull RestBuilder adapter, @NonNull RestParams params, @NonNull Long contextId, @NonNull ENROLLMENT_TYPE enrollmentType, @NonNull StatusCallback<List<User>> callback) {
        callback.addCall(adapter.build(UsersInterface.class, params).getFirstPagePeopleList(contextId, getEnrollmentTypeString(enrollmentType))).enqueue(callback);
    }

    public static void getNextPagePeopleList(@NonNull RestBuilder adapter, @NonNull RestParams params, @NonNull String nextUrl, @NonNull StatusCallback<List<User>> callback) {
        callback.addCall(adapter.build(UsersInterface.class, params).next(nextUrl)).enqueue(callback);
    }

    public static void getFirstPagePeopleList(@NonNull RestBuilder adapter, @NonNull RestParams params, @NonNull Long contextId, @NonNull StatusCallback<List<User>> callback) {
        callback.addCall(adapter.build(UsersInterface.class, params).getFirstPageAllPeopleList(contextId)).enqueue(callback);
    }

    public static void getAllPeopleList(@NonNull RestBuilder adapter, @NonNull RestParams params, @NonNull Long contextId, @NonNull StatusCallback<List<User>> callback) {
        if (StatusCallback.isFirstPage(callback.getLinkHeaders())) {
            callback.addCall(adapter.build(UsersInterface.class, params).getFirstPageAllPeopleList(contextId)).enqueue(callback);
        } else if (StatusCallback.moreCallsExist(callback.getLinkHeaders()) && callback.getLinkHeaders() != null) {
            callback.addCall(adapter.build(UsersInterface.class, params).next(callback.getLinkHeaders().nextUrl)).enqueue(callback);
        }
    }

    public static void getTermsOfService(RestBuilder adapter, RestParams params, StatusCallback<TermsOfService> callback) {
        callback.addCall(adapter.build(UsersInterface.class, params).getTermsOfService()).enqueue(callback);
    }

    public static void getSelfAccountRoles(RestBuilder adapter, RestParams params, StatusCallback<List<AccountRole>> callback) {
        callback.addCall(adapter.build(UsersInterface.class, params).getAccountRoles()).enqueue(callback);
    }

    private static String getEnrollmentTypeString(ENROLLMENT_TYPE enrollmentType){
        String enrollmentString = "";
        switch (enrollmentType){
            case DESIGNER:
                enrollmentString = "designer";
                break;
            case OBSERVER:
                enrollmentString = "observer";
                break;
            case STUDENT:
                enrollmentString = "student";
                break;
            case TA:
                enrollmentString = "ta";
                break;
            case TEACHER:
                enrollmentString = "teacher";
                break;
        }
        return enrollmentString;
    }

    //region Airwolf

    //region Airwolf
    /**
     * Adds a student to a parent Airwolf. Currently only used in the Parent App
     *
     * @param parentId
     * @param studentDomain
     * @param callback - 200 if successful
     */
    public static void addStudentAirwolf(
            @NonNull RestBuilder adapter,
            @NonNull RestParams params,
            @NonNull String parentId,
            @NonNull String studentDomain,
            @NonNull StatusCallback<ResponseBody> callback) {
        callback.addCall(adapter.build(UsersInterface.class, params).addStudentToParentAirwolf(parentId, studentDomain)).enqueue(callback);
    }

    /**
     * Remove student from Airwolf. Currently only used in the Parent App
     *
     * @param parentId
     * @param studentId
     * @param callback - 200 if successful
     */
    public static void removeStudentAirwolf(
            @NonNull RestBuilder adapter,
            @NonNull RestParams params,
            @NonNull String parentId,
            @NonNull String studentId,
            @NonNull StatusCallback<ResponseBody> callback) {
        callback.addCall(adapter.build(UsersInterface.class, params).removeStudentAirwolf(parentId, studentId)).enqueue(callback);
    }

    /**
     * Add parent to Airwolf/Canvas. Currently only used in the Parent App.
     * @param body
     * @param callback
     */
    public static void addParentAirwolf(
            @NonNull RestBuilder adapter,
            @NonNull RestParams params,
            @NonNull Parent body,
            @NonNull StatusCallback<ParentResponse> callback) {
        ParentWrapper parentWrapper = new ParentWrapper();
        parentWrapper.setParent(body);

        callback.addCall(adapter.build(UsersInterface.class, params).addParentAirwolf(parentWrapper)).enqueue(callback);
    }

    public static void authenticateParentAirwolf(
            @NonNull RestBuilder adapter,
            @NonNull RestParams params,
            @NonNull String email,
            @NonNull String password,
            @NonNull StatusCallback<ParentResponse> callback) {

        Parent parent = new Parent();
        parent.setUsername(email);
        parent.setPassword(password);
        callback.addCall(adapter.build(UsersInterface.class, params).authenticateParentAirwolf(parent)).enqueue(callback);
    }

    public static void getStudentsForParentAirwolf(
            @NonNull RestBuilder adapter,
            @NonNull RestParams params,
            @NonNull String parentId,
            @NonNull StatusCallback<List<Student>> callback) {

        callback.addCall(adapter.build(UsersInterface.class, params).getStudentsForParentAirwolf(parentId)).enqueue(callback);
    }

    /**
     * Add a student to a parent's account so the parent can observe the student
     *
     * @param parentId - ID of the parent
     * @param studentDomain - Domain of the student
     * @param callback
     */
    public static void addStudentToParentAirwolf(
            @NonNull RestBuilder adapter,
            @NonNull RestParams params,
            @NonNull String parentId,
            @NonNull String studentDomain,
            @NonNull StatusCallback<ResponseBody> callback) {


        //TODO: In canvasAPI 1 we had to make a special interface that didn't allow redirects so we could catch a particular status code. Double check that this works still
        callback.addCall(adapter.buildNoRedirects(UsersInterface.class, params).addStudentToParentAirwolf(parentId, studentDomain)).enqueue(callback);
    }

    /**
     * Used only for the observer role in Canvas, not the typical student we use with Airwolf
     * @param adapter
     * @param params
     * @param parentId
     * @param callback
     */
    public static void getObserveesForParent(
            @NonNull RestBuilder adapter,
            @NonNull RestParams params,
            @NonNull String parentId,
            @NonNull StatusCallback<List<Student>> callback) {

        callback.addCall(adapter.build(UsersInterface.class, params).getObserveesForParent(parentId)).enqueue(callback);
    }

    /**
     * Let the user request a password if they forgot it.
     *
     * Will return a 404 if there is no record of the e-mail address.
     *
     * @param userName The user's email address
     * @param callback
     */
    public static void sendPasswordResetForParentAirwolf(
            @NonNull RestBuilder adapter,
            @NonNull RestParams params,
            @NonNull String userName,
            @NonNull StatusCallback<ResponseBody> callback) {

        //include an empty json object to pass the parsing on airwolf. It doesn't like an empty string.
        JSONObject object = new JSONObject();
        callback.addCall(adapter.build(UsersInterface.class, params).sendPasswordResetForParentAirwolf(userName, object)).enqueue(callback);
    }

    /**
     * The API call to actually reset the parent's password to the one they just created.
     *
     * @param userName
     * @param password
     * @param callback
     */
    public static void resetParentPassword(
            @NonNull RestBuilder adapter,
            @NonNull RestParams params,
            @NonNull String userName,
            @NonNull String password,
            @NonNull StatusCallback<ResetParent> callback) {

        Parent parent = new Parent();
        parent.setUsername(userName);
        parent.setPassword(password);
        callback.addCall(adapter.build(UsersInterface.class, params).resetParentPasswordAirwolf(parent)).enqueue(callback);
    }

    /**
     * API call for the parent app to get the login page for a Canvas user
     *
     * @param domain Domain of the parent's observer role
     */
    public static void authenticateCavnasParentAirwolf(
            @NonNull RestBuilder adapter,
            @NonNull RestParams params,
            @NonNull String domain,
            @NonNull StatusCallback<ParentResponse> callback) {

        callback.addCall(adapter.buildNoRedirects(UsersInterface.class, params).authenticateCanvasParentAirwolf(domain)).enqueue(callback);
    }

    //endregion
}
