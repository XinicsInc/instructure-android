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

import com.instructure.teacher.ui.utils.*
import org.junit.Test

class AddMessagePageTest: TeacherTest() {
    @Test
    override fun displaysPageObjects() {
        getToReply()
        addMessagePage.assertPageObjects()
    }

    @Test
    fun addReply() {
        getToReply()
        addMessagePage.addReply()
        inboxMessagePage.assertHasReply()
    }

    @Test
    fun displayPageObjectsNewMessage() {
        logIn()
        coursesListPage.clickInboxTab()
        inboxPage.clickAddMessageFAB()
        addMessagePage.assertComposeNewMessageObjectsDisplayed()
    }

    private fun getToReply() {
        val data = seedData(teachers = 1, courses = 1, students = 1)
        val teacher = data.teachersList[0]
        val student = data.studentsList[0]

        val conversation = seedConversation(student, listOf(teacher))

        tokenLogin(teacher)
        coursesListPage.clickInboxTab()

        inboxPage.clickConversation(conversation)
        inboxMessagePage.clickReply()
    }
}
