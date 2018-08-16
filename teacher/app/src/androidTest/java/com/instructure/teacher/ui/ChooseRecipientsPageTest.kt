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
import com.instructure.teacher.ui.utils.clickInboxTab
import com.instructure.teacher.ui.utils.seedData
import com.instructure.teacher.ui.utils.tokenLogin
import org.junit.Test

class ChooseRecipientsPageTest: TeacherTest() {
    @Test
    override fun displaysPageObjects() {
        getToChooseRecipients()
        chooseRecipientsPage.assertPageObjects()
    }

    @Test
    fun hasStudentCategory() {
        getToChooseRecipients()
        chooseRecipientsPage.assertHasStudent()
    }

    @Test
    fun addRecipient() {
        val student = getToChooseRecipients().studentsList[0]
        chooseRecipientsPage.clickStudentCategory()
        chooseRecipientsPage.clickStudent(student)
        chooseRecipientsPage.clickDone()
        addMessagePage.assertHasStudentRecipient(student)
    }

    private fun getToChooseRecipients(): SeededData {
        val data = seedData(teachers = 1, courses = 1, students = 1)
        val course = data.coursesList[0]
        val teacher = data.teachersList[0]
        tokenLogin(teacher)

        coursesListPage.clickInboxTab()
        inboxPage.clickAddMessageFAB()
        addMessagePage.clickCourseSpinner()
        addMessagePage.selectCourseFromSpinner(course)
        addMessagePage.clickAddContacts()
        return data
    }
}
