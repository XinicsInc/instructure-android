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
import com.instructure.canvasapi2.models.ErrorReportResult;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;


public class ErrorReportAPI {

    private static final String DEFAULT_DOMAIN = "https://canvas.instructure.com";

    public interface ErrorReportInterface {
        @POST("/error_reports.json")
        Call<ErrorReportResult> postErrorReport(@Query("error[subject]") String subject,
                                                @Query("error[url]") String url,
                                                @Query("error[email]") String email,
                                                @Query("error[comments]") String comments,
                                                @Query("error[user_perceived_severity") String userPerceivedSeverity,
                                                @Query("error[name]") String name,
                                                @Query("error[user_roles]") String userRoles,
                                                @Query("error[become_user]") String becomeUser,
                                                @Body String body);
    }

    private static ErrorReportInterface buildInterface(StatusCallback<?> callback) {
        RestBuilder adapter = new RestBuilder(callback);
        RestParams params = new RestParams.Builder()
                .withShouldIgnoreToken(true)
                .withPerPageQueryParam(false)
                .withForceReadFromCache(false)
                .withForceReadFromNetwork(true)
                .build();
        return adapter.build(ErrorReportInterface.class, params);
    }

    /**
     * Used when we don't want to use the user's domain
     * @param callback
     * @return
     */
    private static ErrorReportInterface buildGenericInterface(StatusCallback<?> callback) {
        RestBuilder adapter = new RestBuilder(callback);
        RestParams params = new RestParams.Builder()
                .withDomain(DEFAULT_DOMAIN)
                .withShouldIgnoreToken(true)
                .withPerPageQueryParam(false)
                .withForceReadFromCache(false)
                .withForceReadFromNetwork(true)
                .build();
        return adapter.build(ErrorReportInterface.class, params);
    }

    public static void postErrorReport(@NonNull String subject,
                                       @NonNull String url,
                                       @NonNull String email,
                                       @NonNull String comments,
                                       @NonNull String userPerceivedSeverity,
                                       @NonNull String name,
                                       @NonNull String userRoles,
                                       @NonNull String becomeUser,
                                       @NonNull StatusCallback<ErrorReportResult> callback) {
        callback.addCall(buildInterface(callback).postErrorReport(subject, url, email, comments, userPerceivedSeverity, name, userRoles, becomeUser, "")).enqueue(callback);
    }

    public static void postGenericErrorReport(@NonNull String subject,
                                              @NonNull String url,
                                              @NonNull String email,
                                              @NonNull String comments,
                                              @NonNull String userPerceivedSeverity,
                                              @NonNull String name,
                                              @NonNull String userRoles,
                                              @NonNull String becomeUser,
                                              @NonNull StatusCallback<ErrorReportResult> callback) {
        callback.addCall(buildGenericInterface(callback).postErrorReport(subject, url, email, comments, userPerceivedSeverity, name, userRoles, becomeUser, "")).enqueue(callback);
    }
}
