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

import android.app.Activity;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.FinishingActivityTestRule;

import com.instructure.espresso.UiControllerSingleton;
import com.instructure.teacher.BuildConfig;

import static android.support.test.InstrumentationRegistry.getInstrumentation;

public class TeacherActivityTestRule<T extends Activity> extends FinishingActivityTestRule<T> {

    public TeacherActivityTestRule(Class<T> activityClass) {
        super(activityClass);
    }

    @Override
    protected void beforeActivityLaunched() {
        loopMainThreadUntilIdle();
        finishAllActivities();
        new TeacherAppResetter().performReset(InstrumentationRegistry.getTargetContext(), BuildConfig.IS_TESTING);
    }

    @Override
    protected void afterActivityLaunched() {
        loopMainThreadUntilIdle();
    }

    private void loopMainThreadUntilIdle() {
        if (UiControllerSingleton.INSTANCE.exists()) {
            getInstrumentation().runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    UiControllerSingleton.INSTANCE.get().loopMainThreadUntilIdle();
                }
            });
        }
    }
}
