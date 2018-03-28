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
import com.instructure.soseedy.SubmissionType.NO_TYPE
import com.instructure.soseedy.SubmissionType.ONLINE_TEXT_ENTRY
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AssignmentsTest {
    private val course: Course = InProcessServer.courseClient.createCourse(CreateCourseRequest.newBuilder().build())
    private val teacher: CanvasUser = InProcessServer.userClient.createCanvasUser(CreateCanvasUserRequest.getDefaultInstance())
    private val student: CanvasUser = InProcessServer.userClient.createCanvasUser(CreateCanvasUserRequest.getDefaultInstance())
    private val dueAt = "2020-02-01T11:59:59Z"
    private val unlockAt = "2020-01-01T11:59:59Z"
    private val lockAt = "2020-03-01T11:59:59Z"

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
    fun createAssignment() {
        val request = CreateAssignmentRequest.newBuilder()
                .setCourseId(course.id)
                .setWithDescription(false)
                .setLockAt("")
                .setUnlockAt("")
                .setDueAt("")
                .addAllSubmissionTypes(listOf(NO_TYPE))
                .setTeacherToken(teacher.token)
                .build()
        val assignment = InProcessServer.assignmentClient.createAssignment(request)
        assertThat(assignment, instanceOf(Assignment::class.java))
        assertTrue(assignment.id >= 1)
        assertTrue(assignment.name.isNotEmpty())
        assertTrue(assignment.published)
    }

    @Test
    fun submitCourseAssignment() {
        val assignment = InProcessServer.assignmentClient.createAssignment(CreateAssignmentRequest.newBuilder()
                .setCourseId(course.id)
                .setWithDescription(true)
                .setLockAt("")
                .setUnlockAt("")
                .setDueAt("")
                .addAllSubmissionTypes(listOf(ONLINE_TEXT_ENTRY))
                .setTeacherToken(teacher.token)
                .build())
        val request = SubmitCourseAssignmentRequest.newBuilder()
                .setSubmissionType(ONLINE_TEXT_ENTRY)
                .setCourseId(course.id)
                .setAssignmentId(assignment.id)
                .setStudentToken(student.token)
                .build()
        val submission = InProcessServer.assignmentClient.submitCourseAssignment(request)
        assertThat(submission, instanceOf(CourseAssignmentSubmission::class.java))
        assertTrue(submission.id >= 1)
        assertTrue(submission.body.isNotEmpty())
        assertEquals(0, submission.submissionCommentsCount)
        assertEquals(0, submission.attachmentsCount)
    }

    @Test
    fun createCourseAssignmentSubmissionComment() {
        val assignment = InProcessServer.assignmentClient.createAssignment(CreateAssignmentRequest.newBuilder()
                .setCourseId(course.id)
                .setWithDescription(true)
                .setLockAt("")
                .setUnlockAt("")
                .setDueAt("")
                .addAllSubmissionTypes(listOf(ONLINE_TEXT_ENTRY))
                .setTeacherToken(teacher.token)
                .build())
        val request = CreateCourseAssignmentCommentRequest.newBuilder()
                .setCourseId(course.id)
                .setAssignmentId(assignment.id)
                .setStudentToken(student.token)
                .build()
        val submission = InProcessServer.assignmentClient.createCourseAssignmentSubmissionComment(request)
        assertThat(submission, instanceOf(CourseAssignmentSubmission::class.java))
        assertEquals(1, submission.submissionCommentsCount)
        val comment = submission.getSubmissionComments(0)
        assertThat(comment, instanceOf(Comment::class.java))
        assertEquals(student.shortName, comment.authorName)
        assertTrue(comment.comment.isNotEmpty())
        assertEquals(0, comment.attachmentsCount)

    }

    @Test
    fun createAssignmentOverride() {
        val assignment = InProcessServer.assignmentClient.createAssignment(CreateAssignmentRequest.newBuilder()
                .setCourseId(course.id)
                .setWithDescription(true)
                .setLockAt("")
                .setUnlockAt("")
                .setDueAt("")
                .addAllSubmissionTypes(listOf(ONLINE_TEXT_ENTRY))
                .setTeacherToken(teacher.token)
                .build())
        val request = CreateAssignmentOverrideRequest.newBuilder()
                .setCourseId(course.id)
                .setAssignmentId(assignment.id)
                .setToken(teacher.token)
                .addAllStudentIds(listOf(student.id))
                .setDueAt(dueAt)
                .setUnlockAt(unlockAt)
                .setLockAt(lockAt)
                .build()
        val assignmentOverride = InProcessServer.assignmentClient.createAssignmentOverride(request)
        assertThat(assignmentOverride, instanceOf(AssignmentOverride::class.java))
        assertTrue(assignmentOverride.id >= 1)
        assertEquals(assignment.id, assignmentOverride.assignmentId)
        assertTrue(assignmentOverride.title.isNotEmpty())
        assertEquals(1, assignmentOverride.studentIdsCount)
        assertEquals(0, assignmentOverride.groupId)
        assertEquals(0, assignmentOverride.courseSectionId)
        assertEquals(student.id, assignmentOverride.getStudentIds(0))
        assertEquals(dueAt, assignmentOverride.dueAt)
        assertEquals(unlockAt, assignmentOverride.unlockAt)
        assertEquals(lockAt, assignmentOverride.lockAt)
    }

    @Test
    fun createAssignmentOverride_courseGroup() {
        val category = InProcessServer.groupClient.createCourseGroupCategory(CreateCourseGroupCategoryRequest.newBuilder()
                .setCourseId(course.id)
                .setTeacherToken(teacher.token)
                .build())
        val group = InProcessServer.groupClient.createGroup(CreateGroupRequest.newBuilder()
                .setGroupCategoryId(category.id)
                .setTeacherToken(teacher.token)
                .build())
        val assignment = InProcessServer.assignmentClient.createAssignment(CreateAssignmentRequest.newBuilder()
                .setCourseId(course.id)
                .setWithDescription(true)
                .setLockAt("")
                .setUnlockAt("")
                .setDueAt("")
                .addAllSubmissionTypes(listOf(ONLINE_TEXT_ENTRY))
                .setTeacherToken(teacher.token)
                .setGroupCategoryId(category.id)
                .build())
        val request = CreateAssignmentOverrideRequest.newBuilder()
                .setCourseId(course.id)
                .setAssignmentId(assignment.id)
                .setToken(teacher.token)
                .setGroupId(group.id)
                .setDueAt(dueAt)
                .setUnlockAt(unlockAt)
                .setLockAt(lockAt)
                .build()
        val assignmentOverride = InProcessServer.assignmentClient.createAssignmentOverride(request)
        assertThat(assignmentOverride, instanceOf(AssignmentOverride::class.java))
        assertTrue(assignmentOverride.id >= 1)
        assertEquals(assignment.id, assignmentOverride.assignmentId)
        assertTrue(assignmentOverride.title.isNotEmpty())
        assertEquals(0, assignmentOverride.studentIdsCount)
        assertEquals(group.id, assignmentOverride.groupId)
        assertEquals(0, assignmentOverride.courseSectionId)
        assertEquals(dueAt, assignmentOverride.dueAt)
        assertEquals(unlockAt, assignmentOverride.unlockAt)
        assertEquals(lockAt, assignmentOverride.lockAt)
    }

    @Test
    fun createAssignmentOverride_courseSection() {
        val section = InProcessServer.sectionClient.createSection(CreateSectionRequest.newBuilder()
                .setCourseId(course.id)
                .build())
        val assignment = InProcessServer.assignmentClient.createAssignment(CreateAssignmentRequest.newBuilder()
                .setCourseId(course.id)
                .setWithDescription(true)
                .setLockAt("")
                .setUnlockAt("")
                .setDueAt("")
                .addAllSubmissionTypes(listOf(ONLINE_TEXT_ENTRY))
                .setTeacherToken(teacher.token)
                .build())
        val request = CreateAssignmentOverrideRequest.newBuilder()
                .setCourseId(course.id)
                .setAssignmentId(assignment.id)
                .setToken(teacher.token)
                .setCourseSectionId(section.id)
                .setDueAt(dueAt)
                .setUnlockAt(unlockAt)
                .setLockAt(lockAt)
                .build()
        val assignmentOverride = InProcessServer.assignmentClient.createAssignmentOverride(request)
        assertThat(assignmentOverride, instanceOf(AssignmentOverride::class.java))
        assertTrue(assignmentOverride.id >= 1)
        assertEquals(assignment.id, assignmentOverride.assignmentId)
        assertTrue(assignmentOverride.title.isNotEmpty())
        assertEquals(0, assignmentOverride.studentIdsCount)
        assertEquals(0, assignmentOverride.groupId)
        assertEquals(section.id, assignmentOverride.courseSectionId)
        assertEquals(dueAt, assignmentOverride.dueAt)
        assertEquals(unlockAt, assignmentOverride.unlockAt)
        assertEquals(lockAt, assignmentOverride.lockAt)
    }


    @Test
    fun seedAssignments() {
        for (assignmentCount in 0..2) {
            val request = SeedAssignmentRequest.newBuilder()
                    .setCourseId(course.id)
                    .setAssignments(assignmentCount)
                    .addAllSubmissionTypes(listOf())
                    .setTeacherToken(teacher.token)
                    .build()

            val assignments = InProcessServer.assignmentClient.seedAssignments(request)
            assertThat(assignments, instanceOf(Assignments::class.java))
            assertEquals(assignmentCount, assignments.assignmentsCount)
        }
    }

    @Test
    fun seedAssignmentSubmission() {
        for (submissionCount in 0..2) {
            val assignmentCreationRequest = CreateAssignmentRequest.newBuilder()
                    .setCourseId(course.id)
                    .addAllSubmissionTypes(listOf(ONLINE_TEXT_ENTRY))
                    .setTeacherToken(teacher.token)
                    .build()
            val assignment = InProcessServer.assignmentClient.createAssignment(assignmentCreationRequest)
            val attachment = Attachment.newBuilder()
                    .setId(1)
                    .setDisplayName("TestAttachment")
                    .setFileName("TestAttachmentFileName")
                    .build()
            val submissionSeed = SubmissionSeed.newBuilder()
                    .setSubmissionType(ONLINE_TEXT_ENTRY)
                    .setAmount(submissionCount)
                    .setFileType(FileType.TEXT)
                    .addAllAttachments(listOf(attachment))
                    .build()
            val request = SeedAssignmentSubmissionRequest.newBuilder()
                    .setAssignmentId(assignment.id)
                    .setCourseId(course.id)
                    .setStudentToken((student.token))
                    .addAllSubmissionSeeds(listOf(submissionSeed))
                    .build()
            val submissions = InProcessServer.assignmentClient.seedAssignmentSubmission(request)
            assertThat(submissions, instanceOf(SeededCourseAssignmentSubmissions::class.java))
            assertEquals(submissionCount, submissions.submissionsCount)
        }
    }
}
