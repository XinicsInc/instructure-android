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
import com.instructure.dataseeding.model.EnrollmentTypes.TEACHER_ENROLLMENT
import com.instructure.soseedy.*
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DiscussionTopicsTest {
    private val course: Course = InProcessServer.courseClient.createCourse(CreateCourseRequest.newBuilder().build())
    private val teacher: CanvasUser = InProcessServer.userClient.createCanvasUser(CreateCanvasUserRequest.getDefaultInstance())

    @Before
    fun setUp() {
        val request = EnrollUserRequest.newBuilder()
                .setCourseId(course.id)
                .setUserId(teacher.id)
                .setEnrollmentType(TEACHER_ENROLLMENT)
                .build()
        InProcessServer.enrollmentClient.enrollUserInCourse(request)
    }

    @Test
    fun createDiscussion() {
        val request = CreateDiscussionRequest.newBuilder()
                .setCourseId(course.id)
                .setToken(teacher.token)
                .build()
        val discussion = InProcessServer.discussionClient.createDiscussion(request)
        assertThat(discussion, instanceOf(Discussion::class.java))
        assertTrue(discussion.id >= 1)
        assertTrue(discussion.title.isNotEmpty())
        assertTrue(discussion.message.isNotEmpty())
        assertFalse(discussion.isAnnouncement)
    }

    @Test
    fun createAnnouncement() {
        val request = CreateAnnouncementRequest.newBuilder()
                .setCourseId(course.id)
                .setToken(teacher.token)
                .build()
        val announcement = InProcessServer.discussionClient.createAnnouncement(request)
        assertThat(announcement, instanceOf(Discussion::class.java))
        assertTrue(announcement.title.isNotEmpty())
        assertTrue(announcement.message.isNotEmpty())
        assertTrue(announcement.isAnnouncement)
    }
}
