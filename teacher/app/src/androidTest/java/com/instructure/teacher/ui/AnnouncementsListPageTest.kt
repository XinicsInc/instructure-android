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
import com.instructure.teacher.ui.utils.announcements
import com.instructure.teacher.ui.utils.seedData
import com.instructure.teacher.ui.utils.tokenLogin
import org.junit.Test

class AnnouncementsListPageTest : TeacherTest() {

    @Test
    override fun displaysPageObjects() {
        getToAnnouncementsListPage()
        announcementsListPage.assertPageObjects()
    }

    @Test
    fun assertHasAnnouncement() {
        val announcement = getToAnnouncementsListPage().announcements[0]
        announcementsListPage.assertHasAnnouncement(announcement)
    }

    // FIXME: This should probably just be part of the page objects
    @Test
    fun assertDisplaysFloatingActionButton() {
        getToAnnouncementsListPage()
//        val discussion = Data.getNextDiscussion()
//        announcementsListPage.assertHasAnnouncement(discussion)
    }

    private fun getToAnnouncementsListPage(): SeededData {
        val data = seedData(teachers = 1, favoriteCourses = 1, announcements = 1)
        val teacher = data.teachersList[0]
        val course = data.coursesList[0]
        tokenLogin(teacher)

        coursesListPage.openCourse(course)
        courseBrowserPage.openAnnouncementsTab()
        return data
    }
}