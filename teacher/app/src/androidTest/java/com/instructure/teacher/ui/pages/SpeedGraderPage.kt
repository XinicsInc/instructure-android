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
 */
package com.instructure.teacher.ui.pages

import android.support.annotation.StringRes
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.assertion.ViewAssertions
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.espresso.matcher.ViewMatchers.*
import com.instructure.espresso.ClickUntilMethod
import com.instructure.soseedy.CanvasUser
import com.instructure.soseedy.CourseAssignmentSubmission
import com.instructure.teacher.R
import com.instructure.teacher.ui.models.Submission
import com.instructure.teacher.ui.utils.*
import com.instructure.teacher.ui.utils.pageAssert.PageAssert
import com.instructure.teacher.ui.utils.pageAssert.SimplePageAssert
import com.instructure.teacher.utils.getSubmissionFormattedDate
import org.hamcrest.Matchers

@Suppress("unused")
class SpeedGraderPage : BasePage(), PageAssert by SimplePageAssert() {

    private val speedGraderActivityToolbar by OnViewWithId(R.id.speedGraderToolbar)
    private val slidingUpPanelLayout by OnViewWithId(R.id.slidingUpPanelLayout)
    private val submissionPager by OnViewWithId(R.id.submissionContentPager)

    private val gradeTab by OnViewWithStringText(getStringFromResource(R.string.sg_tab_grade).toUpperCase())
    private val commentsTab by OnViewWithStringText(getStringFromResource(R.string.sg_tab_comments).toUpperCase())
    private val filesTab by OnViewWithStringText(getStringFromResource(R.string.sg_tab_files).toUpperCase())

    private val submissionDropDown by WaitForViewWithId(R.id.submissionVersionsButton)
    private val submissionVersionDialogTitle by WaitForViewWithText(R.string.submission_versions)

    fun assertHasSubmissionDropDown() {
        submissionDropDown.assertDisplayed()
    }

    fun assertSubmissionDialogDisplayed() {
        submissionVersionDialogTitle.assertDisplayed()
    }

    fun assertSubmissionSelected(submission: Submission) {
        submissionDropDown.check(ViewAssertions
                .matches(ViewMatchers.withText(submission.submittedAt.getSubmissionFormattedDate(InstrumentationRegistry.getContext()))))
    }

    fun openSubmissionsDialog() {
        ClickUntilMethod.run(
                onView(withId(R.id.submissionVersionsButton)),
                onView(withText(R.string.submission_versions))
        )
    }

    fun selectSubmissionFromDialog(submission: Submission) {
        waitForViewWithText(submission.submittedAt.getSubmissionFormattedDate(InstrumentationRegistry.getTargetContext())).click()
        waitForViewWithText(android.R.string.ok).click()
    }

    fun selectGradesTab() {
        gradeTab.click()
    }

    fun selectCommentsTab() {
        commentsTab.click()
    }

    fun selectFilesTab() {
        filesTab.click()
    }

    fun assertGradingStudent(student: CanvasUser) {
        onViewWithText(student.name).assertCompletelyDisplayed()
    }

    fun goToSubmissionPage(index: Int) {
        submissionPager.pageToItem(index)
    }

    fun clickBackButton() {
        Espresso.onView(Matchers.allOf(
                ViewMatchers.withContentDescription(android.support.v7.appcompat.R.string.abc_action_bar_up_description),
                ViewMatchers.isCompletelyDisplayed()

        )).click()
    }

    fun assertPageCount(count: Int) {
        submissionPager.check(ViewPagerItemCountAssertion(count))
    }

    fun assertDisplaysTextSubmissionView() {
        waitForViewWithId(R.id.textSubmissionWebView).assertVisible()
    }

    fun assertDisplaysEmptyState(@StringRes stringRes: Int) {
        waitForViewWithText(stringRes).assertCompletelyDisplayed()
    }

    fun assertDisplaysUrlSubmissionLink(submission: CourseAssignmentSubmission) {
        waitForViewWithId(R.id.urlTextView).assertCompletelyDisplayed().assertHasText(submission.url)
    }

    fun assertDisplaysUrlWebView() {
        waitForViewWithId(R.id.urlTextView).click()
        waitForViewWithId(R.id.canvasWebView).assertCompletelyDisplayed()
    }

}
