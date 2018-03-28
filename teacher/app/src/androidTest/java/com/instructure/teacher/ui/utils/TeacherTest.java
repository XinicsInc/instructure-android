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

import android.Manifest;
import android.support.test.rule.GrantPermissionRule;

import com.instructure.espresso.ScreenshotTestRule;
import com.instructure.espresso.UiControllerSingleton;
import com.instructure.teacher.BuildConfig;
import com.instructure.teacher.activities.InitLoginActivity;
import com.instructure.teacher.ui.pages.AddMessagePage;
import com.instructure.teacher.ui.pages.AllCoursesListPage;
import com.instructure.teacher.ui.pages.AnnouncementsListPage;
import com.instructure.teacher.ui.pages.AssigneeListPage;
import com.instructure.teacher.ui.pages.AssignmentDetailsPage;
import com.instructure.teacher.ui.pages.AssignmentDueDatesPage;
import com.instructure.teacher.ui.pages.AssignmentListPage;
import com.instructure.teacher.ui.pages.AssignmentSubmissionListPage;
import com.instructure.teacher.ui.pages.ChooseRecipientsPage;
import com.instructure.teacher.ui.pages.CourseBrowserPage;
import com.instructure.teacher.ui.pages.CourseSettingsPage;
import com.instructure.teacher.ui.pages.CoursesListPage;
import com.instructure.teacher.ui.pages.DiscussionsListPage;
import com.instructure.teacher.ui.pages.EditAssignmentDetailsPage;
import com.instructure.teacher.ui.pages.EditCoursesListPage;
import com.instructure.teacher.ui.pages.EditQuizDetailsPage;
import com.instructure.teacher.ui.pages.InboxMessagePage;
import com.instructure.teacher.ui.pages.InboxPage;
import com.instructure.teacher.ui.pages.LoginFindSchoolPage;
import com.instructure.teacher.ui.pages.LoginLandingPage;
import com.instructure.teacher.ui.pages.LoginSignInPage;
import com.instructure.teacher.ui.pages.NavDrawerPage;
import com.instructure.teacher.ui.pages.NotATeacherPage;
import com.instructure.teacher.ui.pages.QuizDetailsPage;
import com.instructure.teacher.ui.pages.QuizListPage;
import com.instructure.teacher.ui.pages.QuizSubmissionListPage;
import com.instructure.teacher.ui.pages.SpeedGraderCommentsPage;
import com.instructure.teacher.ui.pages.SpeedGraderFilesPage;
import com.instructure.teacher.ui.pages.SpeedGraderGradePage;
import com.instructure.teacher.ui.pages.SpeedGraderPage;
import com.instructure.teacher.ui.pages.SpeedGraderQuizSubmissionPage;
import com.instructure.teacher.ui.pages.WebViewLoginPage;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

public abstract class TeacherTest {
    /**
     * Required for auto complete of page objects within tests
     **/
    protected CoursesListPage coursesListPage = new CoursesListPage();
    protected AllCoursesListPage allCoursesListPage = new AllCoursesListPage();
    protected AssignmentListPage assignmentListPage = new AssignmentListPage();
    protected AssignmentSubmissionListPage assignmentSubmissionListPage = new AssignmentSubmissionListPage();
    protected AssignmentDetailsPage assignmentDetailsPage = new AssignmentDetailsPage();
    protected AssignmentDueDatesPage assignmentDueDatesPage = new AssignmentDueDatesPage();
    protected CourseBrowserPage courseBrowserPage = new CourseBrowserPage();
    protected EditCoursesListPage editCoursesListPage = new EditCoursesListPage();
    protected CourseSettingsPage courseSettingsPage = new CourseSettingsPage();
    protected EditAssignmentDetailsPage editAssignmentDetailsPage = new EditAssignmentDetailsPage();
    protected AssigneeListPage assigneeListPage = new AssigneeListPage();
    protected LoginLandingPage loginLandingPage = new LoginLandingPage();
    protected LoginFindSchoolPage loginFindSchoolPage = new LoginFindSchoolPage();
    protected LoginSignInPage loginSignInPage = new LoginSignInPage();
    protected NotATeacherPage notATeacherPage = new NotATeacherPage();
    protected InboxPage inboxPage = new InboxPage();
    protected NavDrawerPage navDrawerPage = new NavDrawerPage();
    protected SpeedGraderPage speedGraderPage = new SpeedGraderPage();
    protected SpeedGraderGradePage speedGraderGradePage = new SpeedGraderGradePage();
    protected SpeedGraderCommentsPage speedGraderCommentsPage = new SpeedGraderCommentsPage();
    protected SpeedGraderFilesPage speedGraderFilesPage = new SpeedGraderFilesPage();
    protected QuizListPage quizListPage = new QuizListPage();
    protected QuizDetailsPage quizDetailsPage = new QuizDetailsPage();
    protected EditQuizDetailsPage editQuizDetailsPage = new EditQuizDetailsPage();
    protected DiscussionsListPage discussionsListPage = new DiscussionsListPage();
    protected QuizSubmissionListPage quizSubmissionListPage = new QuizSubmissionListPage();
    protected InboxMessagePage inboxMessagePage = new InboxMessagePage();
    protected AddMessagePage addMessagePage = new AddMessagePage();
    protected ChooseRecipientsPage chooseRecipientsPage = new ChooseRecipientsPage();
    protected SpeedGraderQuizSubmissionPage speedGraderQuizSubmissionPage = new SpeedGraderQuizSubmissionPage();
    protected WebViewLoginPage webViewLoginPage = new WebViewLoginPage();
    protected AnnouncementsListPage announcementsListPage = new AnnouncementsListPage();

    protected TeacherActivityTestRule<InitLoginActivity> mActivityRule =
            new TeacherActivityTestRule<>(InitLoginActivity.class);

    public TeacherActivityTestRule<InitLoginActivity> getActivityRule() {
        return mActivityRule;
    }


    @Rule
    public GrantPermissionRule rule = GrantPermissionRule.grant(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            // Both read & write permission are required for saving screenshots
            // otherwise the code will error with permission denied.
            // Read also required due to a bug specifically with full read/write external storage not being granted
            // https://issuetracker.google.com/issues/64389280
            Manifest.permission.READ_EXTERNAL_STORAGE);


    @Rule
    public TestRule chain = RuleChain
            .outerRule(GrantPermissionRule.grant())
            .around(mActivityRule)
            .around(new ScreenshotTestRule());


    private static boolean configChecked = false;

    @Before
    public void launchActivity() {
        if (!configChecked) {
            checkBuildConfig();
            configChecked = true;
        }
        mActivityRule.launchActivity(null);
        UiControllerSingleton.INSTANCE.get();
    }

    private static void checkBuildConfig() {
        if (!BuildConfig.IS_TESTING) {
            throw new RuntimeException("Build config must be IS_TESTING! (qaDebug)");
        }
    }

    @Test
    public abstract void displaysPageObjects();
}
