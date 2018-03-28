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

import com.instructure.soseedy.*
import com.instructure.teacher.ui.utils.*
import org.junit.Test
import com.instructure.soseedy.SubmissionType.*

class SpeedGraderFilesPageTest : TeacherTest() {

    @Test
    override fun displaysPageObjects() {
        goToSpeedGraderFilesPage(
                submissions = listOf(
                        SubmissionSeed.newBuilder()
                                .setSubmissionType(ONLINE_UPLOAD)
                                .setAmount(1)
                                .setFileType(FileType.TEXT).build())
        )
        speedGraderFilesPage.assertPageObjects()
    }

    @Test
    fun displaysEmptyFilesView() {
        goToSpeedGraderFilesPage()
        speedGraderFilesPage.assertDisplaysEmptyView()
    }

    @Test
    fun displaysFilesList() {
        val submissions = goToSpeedGraderFilesPage(
                submissions = listOf(
                        SubmissionSeed.newBuilder()
                                .setSubmissionType(ONLINE_UPLOAD)
                                .setAmount(1)
                                .setFileType(FileType.TEXT).build())
        )
        speedGraderFilesPage.assertHasFiles(submissions.getSubmissions(0).attachmentsList)
    }

    @Test
    fun displaysSelectedFile() {
        goToSpeedGraderFilesPage(
                submissions = listOf(
                        SubmissionSeed.newBuilder()
                                .setSubmissionType(ONLINE_UPLOAD)
                                .setAmount(1)
                                .setFileType(FileType.TEXT).build())
        )
        val position = 0

        speedGraderFilesPage.selectFile(position)
        speedGraderFilesPage.assertFileSelected(position)
    }

    private fun goToSpeedGraderFilesPage(assignments: Int = 1,
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

        speedGraderPage.selectFilesTab()
        return submissionList
    }
}