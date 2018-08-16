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

import com.instructure.soseedy.CanvasUser
import com.instructure.teacher.ui.utils.TeacherTest
import com.instructure.teacher.ui.utils.clickProfileMenu
import com.instructure.teacher.ui.utils.seedData
import com.instructure.teacher.ui.utils.tokenLogin
import org.junit.Test

class NavDrawerPageTest: TeacherTest() {

    @Test
    override fun displaysPageObjects() {
        getToNavDrawerMenu()
        navDrawerPage.assertPageObjects()
    }

    @Test
    fun displaysNavDrawerDetails() {
        val teacher = getToNavDrawerMenu()
        navDrawerPage.assertProfileDetails(teacher)
    }

    private fun getToNavDrawerMenu(): CanvasUser {
        val teacher = seedData(teachers = 1, courses = 1).teachersList[0]
        tokenLogin(teacher)
        coursesListPage.clickProfileMenu()
        return teacher
    }
}
