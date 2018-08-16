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

import com.instructure.dataseeding.util.ago
import com.instructure.dataseeding.util.days
import com.instructure.dataseeding.util.fromNow
import com.instructure.dataseeding.util.iso8601
import com.instructure.soseedy.Assignment
import com.instructure.soseedy.SubmissionSeed
import com.instructure.soseedy.SubmissionType.*
import com.instructure.soseedy.*
import com.instructure.teacher.ui.utils.*
import org.junit.Test

class AssignmentDetailsPageTest : TeacherTest() {

    @Test
    @TestRail(ID = "C3109579")
    override fun displaysPageObjects() {
        getToAssignmentDetailsPage(
                submissionTypes = listOf(ONLINE_TEXT_ENTRY),
                students = 1,
                submissions = listOf(
                        SubmissionSeed.newBuilder()
                                .setSubmissionType(ONLINE_TEXT_ENTRY)
                                .setAmount(1)
                                .build()
                        ))
        assignmentDetailsPage.assertPageObjects()
    }

    @Test
    @TestRail(ID = "C3109579")
    fun displaysCorrectDetails() {
        val assignment = getToAssignmentDetailsPage()
        assignmentDetailsPage.assertAssignmentDetails(assignment)
    }

    @Test
    @TestRail(ID = "C3109579")
    fun displaysInstructions() {
        getToAssignmentDetailsPage(withDescription = true)
        assignmentDetailsPage.assertDisplaysInstructions()
    }

    @Test
    @TestRail(ID = "C3134480")
    fun displaysNoInstructionsMessage() {
        getToAssignmentDetailsPage()
        assignmentDetailsPage.assertDisplaysNoInstructionsView()
    }

    @Test
    @TestRail(ID = "C3134481")
    fun displaysClosedAvailability() {
        getToAssignmentDetailsPage(lockAt = 7.days.ago.iso8601)
        assignmentDetailsPage.assertAssignmentClosed()
    }

    @Test
    @TestRail(ID = "C3134482")
    fun displaysNoFromDate() {
        getToAssignmentDetailsPage(lockAt = 7.days.fromNow.iso8601)
        assignmentDetailsPage.assertToFilledAndFromEmpty()
    }

    @Test
    @TestRail(ID = "C3134483")
    fun displaysNoToDate() {
        getToAssignmentDetailsPage(unlockAt = 7.days.ago.iso8601)
        assignmentDetailsPage.assertFromFilledAndToEmpty()
    }

    @Test
    fun displaysSubmissionTypeNone() {
        getToAssignmentDetailsPage(submissionTypes = listOf(NO_TYPE))
        assignmentDetailsPage.assertSubmissionTypeNone()
    }

    @Test
    fun displaysSubmissionTypeOnPaper() {
        getToAssignmentDetailsPage(submissionTypes = listOf(ON_PAPER))
        assignmentDetailsPage.assertSubmissionTypeOnPaper()
    }

    @Test
    fun displaysSubmissionTypeOnlineTextEntry() {
        getToAssignmentDetailsPage(submissionTypes = listOf(ONLINE_TEXT_ENTRY))
        assignmentDetailsPage.assertSubmissionTypeOnlineTextEntry()
    }

    @Test
    fun displaysSubmissionTypeOnlineUrl() {
        getToAssignmentDetailsPage(submissionTypes = listOf(ONLINE_URL))
        assignmentDetailsPage.assertSubmissionTypeOnlineUrl()
    }

    @Test
    fun displaysSubmissionTypeOnlineUpload() {
        getToAssignmentDetailsPage(submissionTypes = listOf(ONLINE_UPLOAD))
        assignmentDetailsPage.assertSubmissionTypeOnlineUpload()
    }

    @Test
    fun displaysSubmittedDonut() {
        getToAssignmentDetailsPage(
                submissionTypes = listOf(ONLINE_TEXT_ENTRY),
                students = 1,
                submissions = listOf(
                        SubmissionSeed.newBuilder()
                                .setSubmissionType(ONLINE_TEXT_ENTRY)
                                .setAmount(1)
                                .build()
                ))
        assignmentDetailsPage.assertHasSubmitted()
    }

    @Test
    fun displaysNotSubmittedDonut() {
        getToAssignmentDetailsPage(students = 1)
        assignmentDetailsPage.assertNotSubmitted()
    }

    private fun getToAssignmentDetailsPage(
            assignments: Int = 1,
            withDescription: Boolean = false,
            lockAt: String = "",
            unlockAt: String = "",
            submissionTypes: List<SubmissionType> = emptyList(),
            students: Int = 0,
            submissions: List<SubmissionSeed> = emptyList()): Assignment {

        val data = seedData(teachers = 1, favoriteCourses = 1, students = students)
        val teacher = data.teachersList[0]
        val course = data.coursesList[0]
        val assignment = seedAssignments(
                assignments = assignments,
                courseId = course.id,
                withDescription = withDescription,
                lockAt = lockAt,
                unlockAt = unlockAt,
                submissionTypes = submissionTypes,
                teacherToken = teacher.token)

        if (!submissions.isEmpty()) {
            seedAssignmentSubmission(
                    submissionSeeds = submissions,
                    assignmentId = assignment.assignmentsList[0].id,
                    courseId = course.id,
                    studentToken = if (data.studentsList.isEmpty()) "" else data.studentsList[0].token
            )
        }

        tokenLogin(teacher)

        coursesListPage.openCourse(course)
        courseBrowserPage.openAssignmentsTab()
        assignmentListPage.clickAssignment(assignment.assignmentsList[0])
        return assignment.assignmentsList[0]
    }
}
