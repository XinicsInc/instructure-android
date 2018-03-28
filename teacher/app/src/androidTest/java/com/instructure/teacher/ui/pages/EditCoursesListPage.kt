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

import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.withContentDescription
import android.support.test.espresso.matcher.ViewMatchers.withText
import com.instructure.soseedy.Course
import com.instructure.teacher.R
import com.instructure.teacher.ui.utils.*
import com.instructure.teacher.ui.utils.pageAssert.PageAssert
import com.instructure.teacher.ui.utils.pageAssert.SimplePageAssert
import java.util.*

@Suppress("unused")
class EditCoursesListPage : BasePage(), PageAssert by SimplePageAssert() {

    private val favoritesRecyclerView by WaitForViewWithId(R.id.favoritesRecyclerView)

    private val star by WaitForViewWithId(R.id.star)

    fun assertHasCourses(mCourses: List<Course>) {

        // Check that the recyclerview count matches course count
        favoritesRecyclerView.check(RecyclerViewItemCountAssertion(mCourses.size))

        for (course in mCourses) onView(withText(course.name)).assertDisplayed()
    }

    fun assertCourseFavorited(course: Course) {
        val resources = InstrumentationRegistry.getTargetContext()
        val match = String.format(Locale.getDefault(), resources.getString(R.string.favorited_content_description), course.name, resources.getString(R.string.content_description_favorite))
        onViewWithText(course.name).check(matches(withContentDescription(match)))
    }

    fun assertCourseUnfavorited(course: Course) {
        val resources = InstrumentationRegistry.getTargetContext()
        val match = String.format(Locale.getDefault(), resources.getString(R.string.favorited_content_description), course.name, resources.getString(R.string.content_description_not_favorite))
        onViewWithText(course.name).check(matches(withContentDescription(match)))
    }

    fun toggleFavoritingCourse(course: Course) {
        waitForViewWithText(course.name).click()
    }
}