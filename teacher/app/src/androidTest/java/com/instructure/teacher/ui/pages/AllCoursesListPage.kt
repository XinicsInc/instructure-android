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
import android.support.test.espresso.matcher.ViewMatchers.withText
import com.instructure.soseedy.Course
import com.instructure.teacher.R
import com.instructure.teacher.ui.utils.*
import com.instructure.teacher.ui.utils.pageAssert.PageAssert
import com.instructure.teacher.ui.utils.pageAssert.SimplePageAssert

@Suppress("unused")
class AllCoursesListPage : BasePage(), PageAssert by SimplePageAssert() {

    private val backButton by OnViewWithContentDescription(android.support.v7.appcompat.R.string.abc_action_bar_up_description)

    private val toolbarTitle by OnViewWithText(R.string.all_courses)

    private val coursesTab by OnViewWithId(R.id.tab_courses)

    private val inboxTab by OnViewWithId(R.id.tab_inbox)

    private val coursesRecyclerView by WaitForViewWithId(R.id.recyclerView)

    fun assertHasCourses(mCourses: List<Course>) {
        coursesRecyclerView.check(RecyclerViewItemCountAssertion(mCourses.size))
        for (course in mCourses) onView(withText(course.name)).assertDisplayed()
    }

}