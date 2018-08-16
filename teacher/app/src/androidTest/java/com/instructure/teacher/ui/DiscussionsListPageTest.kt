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
package com.instructure.teacher.ui

import com.instructure.soseedy.SeededData
import com.instructure.teacher.ui.utils.TeacherTest
import com.instructure.teacher.ui.utils.seedData
import com.instructure.teacher.ui.utils.tokenLogin
import org.junit.Test

class DiscussionsListPageTest : TeacherTest() {

    @Test override fun displaysPageObjects() {
        getToDiscussionsListPage()
        discussionsListPage.assertPageObjects()
    }

    @Test fun assertHasDiscussion() {
        val discussion = getToDiscussionsListPage().discussionsList[0]
        discussionsListPage.assertHasDiscussion(discussion)
    }

    private fun getToDiscussionsListPage(): SeededData {
        val data = seedData(teachers = 1, favoriteCourses = 1, discussions = 1)
        val teacher = data.teachersList[0]
        val course = data.coursesList[0]
        tokenLogin(teacher)

        coursesListPage.openCourse(course)
        courseBrowserPage.openDiscussionsTab()
        return data
    }
}
