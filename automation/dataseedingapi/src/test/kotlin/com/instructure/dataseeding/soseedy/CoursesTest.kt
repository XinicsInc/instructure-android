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
import org.junit.Before
import org.junit.Test

class CoursesTest {
    private val course: Course = InProcessServer.courseClient.createCourse(CreateCourseRequest.newBuilder().build())
    private val student: CanvasUser = InProcessServer.userClient.createCanvasUser(CreateCanvasUserRequest.getDefaultInstance())

    @Before
    fun setUp() {
        InProcessServer.enrollmentClient.enrollUserInCourse(EnrollUserRequest.newBuilder()
                .setCourseId(course.id)
                .setUserId(student.id)
                .setEnrollmentType(STUDENT_ENROLLMENT)
                .build())
    }

    @Test
    fun createCourse() {
        val request = CreateCourseRequest.newBuilder().build()
        val course = InProcessServer.courseClient.createCourse(request)
        assertThat(course, instanceOf(Course::class.java))
        assertTrue(course.id >= 1)
        assertTrue(course.name.isNotEmpty())
        assertFalse(course.favorite)
        assertTrue(course.courseCode.isNotEmpty())
    }

    @Test
    fun addFavoriteCourse() {
        val request = AddFavoriteCourseRequest.newBuilder()
                .setCourseId(course.id)
                .setToken(student.token)
                .build()
        val favorite = InProcessServer.courseClient.addFavoriteCourse(request)
        assertThat(favorite, instanceOf(Favorite::class.java))
        assertEquals(course.id, favorite.contextId)
    }
}
