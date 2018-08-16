/*
 * Copyright (C) 2017 - present Instructure, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.instructure.teacher.activities;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import com.instructure.canvasapi2.models.AccountDomain;
import com.instructure.canvasapi2.utils.ApiPrefs;
import com.instructure.loginapi.login.activities.BaseLoginLandingPageActivity;
import com.instructure.loginapi.login.snicker.SnickerDoodle;
import com.instructure.pandautils.services.PushNotificationRegistrationService;
import com.instructure.teacher.BuildConfig;
import com.instructure.teacher.R;

public class LoginLandingPageActivity extends BaseLoginLandingPageActivity {

    @Override
    protected Intent launchApplicationMainActivityIntent() {
        PushNotificationRegistrationService.Companion.scheduleJob(this, ApiPrefs.isMasquerading(), BuildConfig.PUSH_SERVICE_PROJECT_ID);

        return SplashActivity.Companion.createIntent(this, null);
    }

    @Override
    protected Intent beginFindSchoolFlow() {
        return FindSchoolActivity.createIntent(this);
    }

    @Override
    protected int appTypeName() {
        return R.string.appUserTypeTeacher;
    }

    @Override
    protected int themeColor() {
        return ContextCompat.getColor(this, R.color.login_teacherAppTheme);
    }

    public static Intent createIntent(Context context) {
        return new Intent(context, LoginLandingPageActivity.class);
    }

    @Override
    protected Intent signInActivityIntent(@NonNull SnickerDoodle snickerDoodle) {
        return SignInActivity.createIntent(this, new AccountDomain(snickerDoodle.domain));
    }
}
