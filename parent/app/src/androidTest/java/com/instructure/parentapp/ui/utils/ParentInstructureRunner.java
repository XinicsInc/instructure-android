/*
 * Copyright (C) 2016 - present Instructure, Inc.
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

package com.instructure.parentapp.ui.utils;

import android.os.Bundle;
import android.support.multidex.MultiDex;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResource;
import android.support.test.runner.AndroidJUnitRunner;
import android.support.v4.content.ContextCompat;

import com.instructure.canvasapi2.AppManager;
import com.instructure.canvasapi2.CanvasRestAdapter;
import com.instructure.espresso.EspressoScreenshot;
import com.jakewharton.espresso.OkHttp3IdlingResource;
import com.linkedin.android.testbutler.TestButler;

import okhttp3.OkHttpClient;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

// custom runner extends AndroidJUnitRunner which means we don't need
// the @RunWith(AndroidJUnit4.class) annotation.
//
// must be public to fix:
// java.lang.IllegalAccessException: java.lang.Class<com.instructure.parentapp.ui.utils.ParentInstructureRunner>
// is not accessible from java.lang.Class<android.app.ActivityThread>
public class ParentInstructureRunner extends AndroidJUnitRunner {
    private IdlingResource resource;

    // Must explicitly enable MultiDex support in the custom test runner.
    // http://stackoverflow.com/a/35214158/3846858
    @Override
    public void onCreate(Bundle arguments) {
        MultiDex.install(getTargetContext());
        super.onCreate(arguments);
    }

    private boolean permissionGranted(String permission) {
        int checkPermission = ContextCompat.checkSelfPermission(getTargetContext(), permission);
        return checkPermission == PERMISSION_GRANTED;
    }

    private void grantPermission(String permission) {
        // If we already have permission, then don't ask again.
        // Calling grantPermission on Firebase test lab will crash the app.
        if (permissionGranted(permission)) return;

        TestButler.grantPermission(getTargetContext(), permission);

        if (!permissionGranted(permission)) {
            throw new RuntimeException("Failed to grant " + permission);
        }
    }

    // Both read & write permission are required for saving screenshots
    // otherwise the code will error with permission denied.
    private void enableScreenshots() {
        grantPermission(READ_EXTERNAL_STORAGE);
        grantPermission(WRITE_EXTERNAL_STORAGE);
    }

    @Override
    public void onStart() {
        TestButler.setup(InstrumentationRegistry.getTargetContext());

        enableScreenshots();

        OkHttpClient client = CanvasRestAdapter.getOkHttpClient();
        resource = OkHttp3IdlingResource.create("okhttp", client);
        Espresso.registerIdlingResources(resource);
        super.onStart();
    }

    @Override
    public void finish(int resultCode, Bundle results) {
        TestButler.teardown(InstrumentationRegistry.getTargetContext());
        Espresso.unregisterIdlingResources(resource);
        super.finish(resultCode, results);
    }
}
