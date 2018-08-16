/*
 * Copyright (C) 2016 - present Instructure, Inc.
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

package com.instructure.candroid.test;

import android.support.test.runner.AndroidJUnit4;

import com.instructure.candroid.activity.LoginActivity;
import com.instructure.candroid.test.page.PageObjects;
import com.instructure.espresso.ScreenshotActivityTestRule;

import static com.instructure.candroid.test.utils.UserProfile.*;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class LoginPageTest extends PageObjects {
    @Rule
    public ScreenshotActivityTestRule<LoginActivity> mActivityRule =
            new ScreenshotActivityTestRule<>(LoginActivity.class);

    @Test
    public void loginRejectsInvalidCredentials() {
        domainPickerPage.loadDefaultSchool();
        loginPage.login(INVALID_USER);
        loginPage.assertIncorrectUserOrPasswordError();
    }

    @Test
    public void loginRejectsNoPassword() {
        domainPickerPage.loadDefaultSchool();
        loginPage.clickSubmitButton();
        loginPage.assertNoPasswordError();
    }

    @Test
    public void displaysPasswordResetForm() {
        domainPickerPage.loadDefaultSchool();
        loginPage.togglePasswordResetForm();
    }

    @Test
    public void displaysDomainToolbar() {
        domainPickerPage.loadDefaultSchool();
        loginPage.assertPageObjects();
    }

    @Test
    @Ignore
    public void loginForReal() {
        domainPickerPage.loadDefaultSchool();
        loginPage.login(STUDENT_1);
        loginPage.authorizeApp();
    }
}
