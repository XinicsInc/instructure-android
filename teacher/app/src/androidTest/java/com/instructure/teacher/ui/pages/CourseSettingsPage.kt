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
package com.instructure.teacher.ui.pages

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.assertion.ViewAssertions.matches
import com.instructure.teacher.R
import com.instructure.teacher.ui.utils.*
import com.instructure.teacher.ui.utils.pageAssert.PageAssert
import com.instructure.teacher.ui.utils.pageAssert.SimplePageAssert
import org.hamcrest.Matchers.`is`

class CourseSettingsPage : BasePage(), PageAssert by SimplePageAssert() {

    private val courseImage by OnViewWithId(R.id.courseImage)
    private val editCourseNameRootView by OnViewWithId(R.id.editCourseNameRoot)
    private val editHomeRootView by OnViewWithId(R.id.editHomeRoot)
    private val courseNameLabel by OnViewWithId(R.id.courseNameLabel)
    private val courseHomePageText by OnViewWithId(R.id.courseHomePage)
    private val courseNameText by OnViewWithId(R.id.courseName)
    private val toolbar by OnViewWithId(R.id.toolbar)

    fun clickCourseName() {
        editCourseNameRootView.click()
    }

    fun editCourseName(): String {
        val dialogNameEntry = onViewWithId(R.id.newCourseName)
        val dialogOkButton = onViewWithText(android.R.string.ok)
        val newName = randomString()
        dialogNameEntry.replaceText(newName)
        dialogOkButton.click()
        return newName
    }

    fun clickSetHomePage() {
        editHomeRootView.click()
    }

    fun selectNewHomePage(): String {
        var newHomePageString = ""
        val unselectedRadioButton =
                onView(checked(false) { newHomePageString = it })
        val dialogOkButton = onViewWithText(android.R.string.ok)
        unselectedRadioButton.click()
        dialogOkButton.click()

        return newHomePageString
    }

    fun assertHomePageChanged(newHomePage: String) {
        courseHomePageText.assertHasText(newHomePage)
    }

    fun assertCourseNameChanged(newCourseName: String) {
        courseNameText.assertHasText(newCourseName)
        assertToolbarSubtitleHasText(newCourseName)
    }

    fun assertToolbarSubtitleHasText(newCourseName: String) {
        toolbar.check(matches(matchToolbarText(`is`(newCourseName), false)))
    }
}