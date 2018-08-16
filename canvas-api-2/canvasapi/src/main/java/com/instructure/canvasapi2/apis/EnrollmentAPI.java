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
import android.support.annotation.Nullable;

import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.builders.RestBuilder;
import com.instructure.canvasapi2.builders.RestParams;
import com.instructure.canvasapi2.models.Enrollment;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;

public class EnrollmentAPI {

    public static final String STUDENT_ENROLLMENT = "StudentEnrollment";
    public static final String TEACHER_ENROLLMENT = "TeacherEnrollment";
    public static final String TA_ENROLLMENT = "TaEnrollment";
    public static final String DESIGNER_ENROLLMENT = "DesignerEnrollment";
    public static final String OBSERVER_ENROLLMENT = "ObserverEnrollment";

    public static final String STATE_ACTIVE = "active";
    public static final String STATE_INVITED = "invited";

    interface EnrollmentInterface {
        @GET("courses/{courseId}/enrollments?include[]=avatar_url&state[]=active")
        Call<List<Enrollment>> getFirstPageEnrollmentsForCourse (
                @Path("courseId") long courseId,
                @Query("type[]") String enrollmentType);

        @GET("courses/{courseId}/enrollments?user_id")
        Call<List<Enrollment>> getFirstPageEnrollmentsForUserInCourse (
                @Path("courseId") long courseId,
                @Query("user_id") long userId,
                @Query("type[]") String[] enrollmentTypes);

        @GET
        Call<List<Enrollment>> getNextPage(@Url String nextUrl);

        @GET("users/self/enrollments")
        Call<List<Enrollment>> getFirstPageSelfEnrollments(
                @Query("type[]") List<String> types,
                @Query("state[]") List<String> states);

        @POST("courses/{courseId}/enrollments/{enrollmentId}/{action}")
        Call<Void> handleInvite(@Path("courseId") long courseId, @Path("enrollmentId") long enrollmentId, @Path("action") String action);

    }

    public static void getEnrollmentsForCourse(
            @NonNull RestBuilder adapter,
            @NonNull RestParams params,
            long courseId,
            String enrollmentType,
            @NonNull StatusCallback<List<Enrollment>> callback){

        if (StatusCallback.isFirstPage(callback.getLinkHeaders())) {
            callback.addCall(adapter.build(EnrollmentInterface.class, params)
                    .getFirstPageEnrollmentsForCourse(courseId, enrollmentType)).enqueue(callback);
        } else if (callback.getLinkHeaders() != null && StatusCallback.moreCallsExist(callback.getLinkHeaders())) {
            callback.addCall(adapter.build(EnrollmentInterface.class, params)
                    .getNextPage(callback.getLinkHeaders().nextUrl)).enqueue(callback);
        }
    }

    public static void getFirstPageEnrollmentsForCourse(
            @NonNull RestBuilder adapter,
            @NonNull RestParams params,
            long courseId,
            @NonNull String enrollmentType,
            @NonNull StatusCallback<List<Enrollment>> callback){
            callback.addCall(adapter.build(EnrollmentInterface.class, params)
                    .getFirstPageEnrollmentsForCourse(courseId, enrollmentType)).enqueue(callback);
    }

    public static void getFirstPageEnrollmentsForUserInCourse(
            @NonNull RestBuilder adapter,
            @NonNull RestParams params,
            long courseId, long userId,
            @NonNull StatusCallback<List<Enrollment>> callback){

        String[] enrollmentTypes = new String[] { "TeacherEnrollment", "TaEnrollment", "DesignerEnrollment" };

        callback.addCall(adapter.build(EnrollmentInterface.class, params)
                .getFirstPageEnrollmentsForUserInCourse(courseId, userId, enrollmentTypes)).enqueue(callback);
    }

    public static void getNextPageEnrollments(boolean forceNetwork, String nextUrl, RestBuilder adapter, StatusCallback<List<Enrollment>> callback) {
        RestParams params = new RestParams.Builder()
                .withShouldIgnoreToken(false)
                .withPerPageQueryParam(true)
                .withForceReadFromNetwork(forceNetwork)
                .build();

        callback.addCall(adapter.build(EnrollmentInterface.class, params)
                .getNextPage(nextUrl)).enqueue(callback);
    }

    public static void getSelfEnrollments(
            @Nullable List<String> types,
            @Nullable List<String> states,
            @NonNull RestBuilder adapter,
            @NonNull RestParams params,
            @NonNull StatusCallback<List<Enrollment>> callback) {
        if (StatusCallback.isFirstPage(callback.getLinkHeaders())) {
            callback.addCall(adapter.build(EnrollmentInterface.class, params).getFirstPageSelfEnrollments(types, states)).enqueue(callback);
        } else if (callback.getLinkHeaders() != null && StatusCallback.moreCallsExist(callback.getLinkHeaders())) {
            callback.addCall(adapter.build(EnrollmentInterface.class, params).getNextPage(callback.getLinkHeaders().nextUrl)).enqueue(callback);
        }
    }

    public static void handleInvite(long courseId, long enrollmentId, boolean acceptInvite, RestBuilder adapter, RestParams params, StatusCallback<Void> callback) {
        callback.addCall(adapter.build(EnrollmentInterface.class, params).handleInvite(courseId, enrollmentId, acceptInvite ? "accept" : "reject")).enqueue(callback);
    }

}
