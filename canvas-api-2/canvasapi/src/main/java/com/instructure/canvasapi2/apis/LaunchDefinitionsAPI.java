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
import com.instructure.canvasapi2.models.LaunchDefinition;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public class LaunchDefinitionsAPI {

    interface LaunchDefinitionsInterface {
        @GET("accounts/self/lti_apps/launch_definitions?placements[]=global_navigation")
        Call<List<LaunchDefinition>> getLaunchDefinitions();

        @GET("courses/{courseId}/lti_apps/launch_definitions?placements[]=course_navigation")
        Call<List<LaunchDefinition>> getLaunchDefinitionsForCourse(@Path("courseId") long courseId);
    }

    public static void getLaunchDefinitions(@NonNull RestBuilder adapter, StatusCallback<List<LaunchDefinition>> callback, @NonNull RestParams params) {
        callback.addCall(adapter.build(LaunchDefinitionsInterface.class, params).getLaunchDefinitions()).enqueue(callback);
    }

    public static void getLaunchDefinitionsForCourse(long courseId, @NonNull RestBuilder adapter, StatusCallback<List<LaunchDefinition>> callback, @NonNull RestParams params) {
        callback.addCall(adapter.build(LaunchDefinitionsInterface.class, params).getLaunchDefinitionsForCourse(courseId)).enqueue(callback);
    }
}
