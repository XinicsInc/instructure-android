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

package com.instructure.dataseeding.soseedy

import com.instructure.dataseeding.InProcessServer
import com.instructure.dataseeding.model.EnrollmentTypes.STUDENT_ENROLLMENT
import com.instructure.dataseeding.model.EnrollmentTypes.TEACHER_ENROLLMENT
import com.instructure.soseedy.*
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConversationsTest {
    private val course = InProcessServer.courseClient.createCourse(CreateCourseRequest.newBuilder().build())
    private val student: CanvasUser = InProcessServer.userClient.createCanvasUser(CreateCanvasUserRequest.getDefaultInstance())
    private val teacher: CanvasUser = InProcessServer.userClient.createCanvasUser(CreateCanvasUserRequest.getDefaultInstance())

    @Before
    fun setUp() {
        val studentRequest = EnrollUserRequest.newBuilder()
                .setCourseId(course.id)
                .setUserId(student.id).setEnrollmentType(STUDENT_ENROLLMENT)
                .build()
        val teacherRequest = EnrollUserRequest.newBuilder()
                .setCourseId(course.id)
                .setUserId(teacher.id).setEnrollmentType(TEACHER_ENROLLMENT)
                .build()
        InProcessServer.enrollmentClient.enrollUserInCourse(studentRequest)
        InProcessServer.enrollmentClient.enrollUserInCourse(teacherRequest)
    }

    @Test
    fun createConversation() {
        val request = CreateConversationRequest.newBuilder()
                .setToken(student.token)
                .addAllRecipients(listOf("course_" + course.id))
                .build()
        val conversation = InProcessServer.conversationClient.createConversation(request)
        assertThat(conversation, instanceOf(Conversation::class.java))
        assertTrue(conversation.id >= 1)
        assertTrue(conversation.subject.isNotEmpty())
    }
}
