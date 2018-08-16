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
import com.instructure.soseedy.*
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert.*
import org.junit.Test

class EnrollmentsTest {
    private val course: Course = InProcessServer.courseClient.createCourse(CreateCourseRequest.newBuilder().build())
    private val section: Section = InProcessServer.sectionClient.createSection(CreateSectionRequest.newBuilder().setCourseId(course.id).build())
    private val user: CanvasUser = InProcessServer.userClient.createCanvasUser(CreateCanvasUserRequest.getDefaultInstance())

    @Test
    fun enrollUserInCourse() {
        val request = EnrollUserRequest.newBuilder()
                .setCourseId(course.id)
                .setUserId(user.id).setEnrollmentType(STUDENT_ENROLLMENT)
                .build()
        val enrollment = InProcessServer.enrollmentClient.enrollUserInCourse(request)
        assertThat(enrollment, instanceOf(Enrollment::class.java))
        assertEquals(course.id, enrollment.courseId)
        assertTrue(enrollment.sectionId >= 1)
        assertEquals(user.id, enrollment.userId)
        assertEquals(STUDENT_ENROLLMENT, enrollment.type)
        assertEquals(STUDENT_ENROLLMENT, enrollment.role)
    }

    @Test
    fun enrollUserInSection() {
        val request = EnrollUserInSectionRequest.newBuilder()
                .setSectionId(section.id)
                .setUserId(user.id).setEnrollmentType(STUDENT_ENROLLMENT)
                .build()
        val enrollment = InProcessServer.enrollmentClient.enrollUserInSection(request)
        assertThat(enrollment, instanceOf(Enrollment::class.java))
        assertEquals(course.id, enrollment.courseId)
        assertEquals(section.id, enrollment.sectionId)
        assertEquals(user.id, enrollment.userId)
        assertEquals(STUDENT_ENROLLMENT, enrollment.type)
        assertEquals(STUDENT_ENROLLMENT, enrollment.role)
    }
}
