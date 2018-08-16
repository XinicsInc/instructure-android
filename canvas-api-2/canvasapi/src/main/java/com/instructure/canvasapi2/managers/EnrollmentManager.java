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

import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.apis.EnrollmentAPI;
import com.instructure.canvasapi2.builders.RestBuilder;
import com.instructure.canvasapi2.builders.RestParams;
import com.instructure.canvasapi2.models.Enrollment;
import com.instructure.canvasapi2.utils.ExhaustiveListCallback;

import java.util.List;

public class EnrollmentManager extends BaseManager {

    private static boolean mTesting = false;

    public static void getEnrollmentsForCourse(long courseId, String enrollmentType, boolean forceNetwork, StatusCallback<List<Enrollment>> callback) {
        if (isTesting() || mTesting) {
            //TODO
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(true)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();
            EnrollmentAPI.getEnrollmentsForCourse(adapter, params, courseId, enrollmentType, callback);
        }
    }

    public static void getAllEnrollmentsForCourse(final long courseId, final String enrollmentType, final boolean forceNetwork, StatusCallback<List<Enrollment>> callback) {
        if (isTesting() || mTesting) {
            //TODO
        } else {
            final RestBuilder adapter = new RestBuilder(callback);
            final RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(true)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();
            StatusCallback<List<Enrollment>> depaginatedCallback = new ExhaustiveListCallback<Enrollment>(callback) {
                @Override
                public void getNextPage(@NonNull StatusCallback<List<Enrollment>> callback, @NonNull String nextUrl, boolean isCached) {
                    EnrollmentAPI.getNextPageEnrollments(forceNetwork, nextUrl, adapter, callback);
                }
            };
            adapter.setStatusCallback(depaginatedCallback);
            EnrollmentAPI.getFirstPageEnrollmentsForCourse(adapter, params, courseId, enrollmentType, depaginatedCallback);
        }
    }

    public static void getAllEnrollmentsForUserInCourse(final long courseId, final long userId, final boolean forceNetwork, StatusCallback<List<Enrollment>> callback) {
        if (isTesting() || mTesting) {
            //TODO
        } else {
            final RestBuilder adapter = new RestBuilder(callback);
            final RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(true)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();
            StatusCallback<List<Enrollment>> depaginatedCallback = new ExhaustiveListCallback<Enrollment>(callback) {
                @Override
                public void getNextPage(@NonNull StatusCallback<List<Enrollment>> callback, @NonNull String nextUrl, boolean isCached) {
                    EnrollmentAPI.getNextPageEnrollments(forceNetwork, nextUrl, adapter, callback);
                }
            };
            adapter.setStatusCallback(depaginatedCallback);
            EnrollmentAPI.getFirstPageEnrollmentsForUserInCourse(adapter, params, courseId, userId, depaginatedCallback);
        }
    }

    public static void getSelfEnrollments(final @Nullable List<String> types, final @Nullable List<String> states, boolean forceNetwork, @NonNull StatusCallback<List<Enrollment>> callback) {
        if (isTesting() || mTesting) {

        } else {
            final RestBuilder adapter = new RestBuilder(callback);
            final RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(true)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();
            StatusCallback<List<Enrollment>> depaginatedCallback = new ExhaustiveListCallback<Enrollment>(callback) {
                @Override
                public void getNextPage(@NonNull StatusCallback<List<Enrollment>> callback, @NonNull String nextUrl, boolean isCached) {
                    EnrollmentAPI.getSelfEnrollments(types, states, adapter, params, callback);
                }
            };
            adapter.setStatusCallback(depaginatedCallback);
            EnrollmentAPI.getSelfEnrollments(types, states, adapter, params, callback);
        }
    }

    public static void handleInvite(long courseId, long enrollmentId, boolean acceptInvite, @NonNull StatusCallback<Void> callback) {
        if (isTesting() || mTesting) {

        } else {
            final RestBuilder adapter = new RestBuilder(callback);
            final RestParams params = new RestParams.Builder().build();
            EnrollmentAPI.handleInvite(courseId, enrollmentId, acceptInvite, adapter, params, callback);
        }
    }
}
