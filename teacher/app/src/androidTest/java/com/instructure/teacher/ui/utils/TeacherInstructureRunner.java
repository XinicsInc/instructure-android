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

package com.instructure.teacher.ui.utils;

import android.os.Bundle;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResource;
import android.support.test.runner.AndroidJUnitRunner;
import android.support.test.runner.MonitoringInstrumentation;

import com.instructure.canvasapi2.CanvasRestAdapter;
import com.instructure.dataseeding.InProcessServer;
import com.instructure.espresso.EspressoScreenshot;
import com.instructure.soseedy.HealthCheckRequest;
import com.jakewharton.espresso.OkHttp3IdlingResource;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import okhttp3.OkHttpClient;


public class TeacherInstructureRunner extends AndroidJUnitRunner {
    private IdlingResource resource;

    static {
        try {
            int START_ACTIVITY_TIMEOUT_SECONDS = 120;
            // private static final int START_ACTIVITY_TIMEOUT_SECONDS = 45;
            // https://android.googlesource.com/platform/frameworks/testing/+/7a552ffc0bce492a7b87755490f3df7490dc357c/support/src/android/support/test/runner/MonitoringInstrumentation.java#78
            Field field = MonitoringInstrumentation.class.getDeclaredField("START_ACTIVITY_TIMEOUT_SECONDS");
            field.setAccessible(true);
            field.set(null, START_ACTIVITY_TIMEOUT_SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void healthCheck() {
        boolean healthy = InProcessServer.INSTANCE.getGeneralClient().getHealthCheck(HealthCheckRequest.getDefaultInstance()).getHealthy();

        if (!healthy) {
            throw new RuntimeException("Health check failed");
        }
    }

    @Override
    public void onStart() {
        healthCheck();

        OkHttpClient client = CanvasRestAdapter.getOkHttpClient();
        resource = OkHttp3IdlingResource.create("okhttp", client);
        Espresso.registerIdlingResources(resource);
        super.onStart();
    }

    @Override
    public void finish(int resultCode, Bundle results) {
        Espresso.unregisterIdlingResources(resource);
        super.finish(resultCode, results);
    }
}
