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
import com.instructure.dataseeding.model.ContextTypes.COURSE
import com.instructure.dataseeding.model.EnrollmentTypes.STUDENT_ENROLLMENT
import com.instructure.dataseeding.model.EnrollmentTypes.TEACHER_ENROLLMENT
import com.instructure.dataseeding.model.WorkflowStates.ACCEPTED
import com.instructure.soseedy.*
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GroupsTest {
    private val course: Course = InProcessServer.courseClient.createCourse(CreateCourseRequest.newBuilder().build())
    private val teacher: CanvasUser = InProcessServer.userClient.createCanvasUser(CreateCanvasUserRequest.getDefaultInstance())
    private val student: CanvasUser = InProcessServer.userClient.createCanvasUser(CreateCanvasUserRequest.getDefaultInstance())

    @Before
    fun setUp() {
        InProcessServer.enrollmentClient.enrollUserInCourse(EnrollUserRequest.newBuilder()
                .setCourseId(course.id)
                .setUserId(teacher.id)
                .setEnrollmentType(TEACHER_ENROLLMENT)
                .build())
        InProcessServer.enrollmentClient.enrollUserInCourse(EnrollUserRequest.newBuilder()
                .setCourseId(course.id)
                .setUserId(student.id)
                .setEnrollmentType(STUDENT_ENROLLMENT)
                .build())
    }

    @Test
    fun createCourseGroupCategory() {
        val request = CreateCourseGroupCategoryRequest.newBuilder()
                .setCourseId(course.id)
                .setTeacherToken(teacher.token)
                .build()
        val category = InProcessServer.groupClient.createCourseGroupCategory(request)
        assertThat(category, instanceOf(GroupCategory::class.java))
        assertTrue(category.id >= 1)
        assertTrue(category.name.isNotEmpty())
        assertEquals(COURSE, category.contextType)
    }

    @Test
    fun createGroup() {
        val category = InProcessServer.groupClient.createCourseGroupCategory(CreateCourseGroupCategoryRequest.newBuilder()
                .setCourseId(course.id)
                .setTeacherToken(teacher.token)
                .build())
        val request = CreateGroupRequest.newBuilder()
                .setGroupCategoryId(category.id)
                .setTeacherToken(teacher.token)
                .build()
        val group = InProcessServer.groupClient.createGroup(request)
        assertThat(group, instanceOf(Group::class.java))
        assertTrue(group.id >= 1)
        assertTrue(group.name.isNotEmpty())
        assertTrue(group.description.isNotEmpty())
        assertEquals(COURSE, group.contextType)
        assertEquals(course.id, group.courseId)
        assertEquals(category.id, group.groupCategoryId)
    }

    @Test
    fun createGroupMembership() {
        val category = InProcessServer.groupClient.createCourseGroupCategory(CreateCourseGroupCategoryRequest.newBuilder()
                .setCourseId(course.id)
                .setTeacherToken(teacher.token)
                .build())
        val group = InProcessServer.groupClient.createGroup(CreateGroupRequest.newBuilder()
                .setGroupCategoryId(category.id)
                .setTeacherToken(teacher.token)
                .build())
        val request = CreateGroupMembershipRequest.newBuilder()
                .setGroupId(group.id)
                .setUserId(student.id)
                .setTeacherToken(teacher.token)
                .build()
        val membership = InProcessServer.groupClient.createGroupMembership(request)
        assertThat(membership, instanceOf(GroupMembership::class.java))
        assertTrue(membership.id >= 1)
        assertEquals(group.id, membership.groupId)
        assertEquals(student.id, membership.userId)
        assertEquals(ACCEPTED, membership.workflowState)
    }
}
