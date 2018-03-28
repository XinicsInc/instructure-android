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
package com.instructure.teacher.ui

import android.support.test.espresso.Espresso
import com.instructure.soseedy.CommentSeed
import com.instructure.soseedy.CourseAssignmentSubmission
import com.instructure.soseedy.FileType.*
import com.instructure.soseedy.SeededCourseAssignmentSubmissions
import com.instructure.soseedy.SubmissionSeed
import com.instructure.soseedy.SubmissionType.*
import com.instructure.teacher.ui.utils.*
import org.junit.Test

class SpeedGraderCommentsPageTest : TeacherTest() {

    @Test
    override fun displaysPageObjects() {
        goToSpeedGraderCommentsPage(
                submissions = listOf(
                        SubmissionSeed.newBuilder()
                                .setSubmissionType(ONLINE_TEXT_ENTRY)
                                .setAmount(1).build())
        )

        speedGraderCommentsPage.assertPageObjects()
    }

    @Test
    fun displaysAuthorName() {
        val submissions = goToSpeedGraderCommentsPage(
                submissions = listOf(
                        SubmissionSeed.newBuilder()
                                .setSubmissionType(ONLINE_TEXT_ENTRY)
                                .setAmount(1).build()),
                submissionComments = listOf(CommentSeed.newBuilder().build())
        )

        val authorName = submissions.getSubmissions(0).getSubmissionComments(0).authorName
        speedGraderCommentsPage.assertDisplaysAuthorName(authorName)
    }

    @Test
    fun displaysCommentText() {
        val submissions = goToSpeedGraderCommentsPage(
                submissions = listOf(
                        SubmissionSeed.newBuilder()
                                .setSubmissionType(ONLINE_TEXT_ENTRY)
                                .setAmount(1).build()),
                submissionComments = listOf(CommentSeed.newBuilder().build())
        )

        val commentText = submissions.getSubmissions(0).getSubmissionComments(0).comment
        speedGraderCommentsPage.assertDisplaysCommentText(commentText)
    }

    @Test
    fun displaysCommentAttachment() {
        val submissions = goToSpeedGraderCommentsPage(
                submissions = listOf(
                        SubmissionSeed.newBuilder()
                                .setSubmissionType(ONLINE_TEXT_ENTRY)
                                .setAmount(1).build()),
                submissionComments = listOf(CommentSeed.newBuilder().setFileType(TEXT).build())
        )

        val attachment = submissions.getSubmissions(0).getSubmissionComments(0).getAttachments(0)
        speedGraderCommentsPage.assertDisplaysCommentAttachment(attachment)
    }

    @Test
    fun displaysSubmissionHistory() {
        goToSpeedGraderCommentsPage(
                submissions = listOf(
                        SubmissionSeed.newBuilder()
                                .setSubmissionType(ONLINE_TEXT_ENTRY)
                                .setAmount(1).build())
        )

        speedGraderCommentsPage.assertDisplaysSubmission()
    }

    @Test
    fun displaysSubmissionFile() {
        val submissions = goToSpeedGraderCommentsPage(
                submissions = listOf(
                        SubmissionSeed.newBuilder()
                                .setSubmissionType(ONLINE_UPLOAD)
                                .setAmount(1)
                                .setFileType(TEXT).build())
        )

        val fileAttachments = submissions.getSubmissions(0).getAttachments(0)
        speedGraderCommentsPage.assertDisplaysSubmissionFile(fileAttachments)
    }

    @Test
    fun addsNewTextComment() {
        goToSpeedGraderCommentsPage(
                submissions = listOf(
                        SubmissionSeed.newBuilder()
                                .setSubmissionType(ONLINE_TEXT_ENTRY)
                                .setAmount(1).build())
        )

        val newComment = randomString(32)
        speedGraderCommentsPage.addComment(newComment)
        speedGraderCommentsPage.assertDisplaysCommentText(newComment)
    }

    @Test
    fun showsNoCommentsMessage() {
        goToSpeedGraderCommentsPage(
                submissions = listOf(
                        SubmissionSeed.newBuilder()
                                .setSubmissionType(ON_PAPER)
                                .setAmount(0).build())
        )

        speedGraderCommentsPage.assertDisplaysEmptyState()
    }

    private fun goToSpeedGraderCommentsPage(
            assignments: Int = 1,
            withDescription: Boolean = false,
            lockAt: String = "",
            unlockAt: String = "",
            submissions: List<SubmissionSeed> = emptyList(),
            submissionComments: List<CommentSeed> = emptyList()): SeededCourseAssignmentSubmissions {

        val data = seedData(teachers = 1, favoriteCourses = 1, students = 1)
        val teacher = data.teachersList[0]
        val course = data.coursesList[0]
        val student = data.studentsList[0]
        val assignment = seedAssignments(
                assignments = assignments,
                courseId = course.id,
                withDescription = withDescription,
                lockAt = lockAt,
                unlockAt = unlockAt,
                submissionTypes = submissions.map { it.submissionType },
                teacherToken = teacher.token)

        val submissionList = seedAssignmentSubmission(
                submissionSeeds = submissions,
                assignmentId = assignment.assignmentsList[0].id,
                courseId = course.id,
                studentToken = if (data.studentsList.isEmpty()) "" else data.studentsList[0].token,
                commentSeeds = submissionComments
        )

        tokenLogin(teacher)

        coursesListPage.openCourse(course)
        courseBrowserPage.openAssignmentsTab()
        assignmentListPage.clickAssignment(assignment.assignmentsList[0])
        assignmentDetailsPage.openSubmissionsPage()
        assignmentSubmissionListPage.clickSubmission(student)
        speedGraderPage.selectCommentsTab()
        return submissionList
    }
}