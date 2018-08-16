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

import com.instructure.soseedy.CanvasUser
import com.instructure.soseedy.SeededCourseAssignmentSubmissions
import com.instructure.soseedy.SubmissionSeed
import com.instructure.soseedy.SubmissionType
import com.instructure.soseedy.SubmissionType.*
import com.instructure.teacher.R
import com.instructure.teacher.ui.utils.*
import org.junit.Test

class SpeedGraderPageTest : TeacherTest() {

    @Test
    override fun displaysPageObjects() {
        goToSpeedGraderPage()
        speedGraderPage.assertPageObjects()
    }

    @Test
    fun displaysSubmissionDropDown() {
        goToSpeedGraderPage(submissionType = ONLINE_TEXT_ENTRY, students = 1, submissions = listOf(2))
        speedGraderPage.assertHasSubmissionDropDown()
    }

    @Test
    fun displaySubmissionPickerDialog() {
        goToSpeedGraderPage(submissionType = ONLINE_TEXT_ENTRY, students = 1, submissions = listOf(2))
        speedGraderPage.openSubmissionsDialog()
        speedGraderPage.assertSubmissionDialogDisplayed()
    }

    @Test
    fun opensToCorrectSubmission() {
        val data = goToSpeedGraderPage(students = 4, submissionType = ONLINE_TEXT_ENTRY)
        val students = data.students
        speedGraderPage.clickBackButton()
        for (i in 0 until students.size) {
            val student = students[i]
            assignmentSubmissionListPage.clickSubmission(student)
            speedGraderPage.assertGradingStudent(student)
            speedGraderPage.clickBackButton()
        }
    }

    @Test
    fun hasCorrectPageCount() {
        goToSpeedGraderPage(students = 4)
        speedGraderPage.assertPageCount(4)
    }

    /* TODO: Uncomment and implement if we come up with a way to create/modify submissions dates
    @Test
    fun displaysSelectedSubmissionInDropDown() {
        goToSpeedGraderPage()
        speedGraderPage.openSubmissionsDialog()
        getNextSubmission()
        val submission = getNextSubmission()
        speedGraderPage.selectSubmissionFromDialog(submission)
        speedGraderPage.assertSubmissionSelected(submission)
    }
    */

    @Test
    fun displaysTextSubmission() {
        goToSpeedGraderPage(submissionType = ONLINE_TEXT_ENTRY, submissions = listOf(1))
        speedGraderPage.assertDisplaysTextSubmissionView()
    }

    @Test
    fun displaysUnsubmittedEmptyState() {
        goToSpeedGraderPage(submissionType = ONLINE_TEXT_ENTRY)
        speedGraderPage.assertDisplaysEmptyState(R.string.speedgrader_student_no_submissions)
    }

    @Test
    fun displaysNoSubmissionsAllowedEmptyState() {
        goToSpeedGraderPage(submissionType = NO_TYPE)
        speedGraderPage.assertDisplaysEmptyState(R.string.speedGraderNoneMessage)
    }

    @Test
    fun displaysOnPaperEmptyState() {
        goToSpeedGraderPage(submissionType = ON_PAPER)
        speedGraderPage.assertDisplaysEmptyState(R.string.speedGraderOnPaperMessage)
    }

    @Test
    fun displaysExternalToolEmptyState() {
        goToSpeedGraderPage(submissionType = EXTERNAL_TOOL)
        speedGraderPage.assertDisplaysEmptyState(R.string.speedgrader_student_no_submissions)
    }

    @Test
    fun displaysUrlSubmission() {
        val submissions = goToSpeedGraderPage(submissionType = ONLINE_URL, submissions = listOf(1)).submissions[0]
        speedGraderPage.assertDisplaysUrlSubmissionLink(submissions.getSubmissions(0))
        speedGraderPage.assertDisplaysUrlWebView()
    }

    private fun goToSpeedGraderPage(
            students: Int = 1,
            submissionType: SubmissionType = SubmissionType.NO_TYPE,
            submissions: List<Int> = listOf(0),
            selectStudent: Int = 0
    ): SpeedGraderPageData {
        val data = seedData(teachers = 1, favoriteCourses = 1, students = students)
        val teacher = data.teachersList[0]
        val course = data.coursesList[0]
        val assignment = seedAssignments(
                assignments = 1,
                courseId = course.id,
                submissionTypes = listOf(submissionType),
                teacherToken = teacher.token).assignmentsList[0]

        val assignmentSubmissions: MutableList<SeededCourseAssignmentSubmissions> =
                (0 until submissions.size).map {
                    seedAssignmentSubmission(
                            submissionSeeds = listOf(
                                    SubmissionSeed.newBuilder()
                                            .setSubmissionType(submissionType)
                                            .setAmount(submissions[it]).build()),
                            assignmentId = assignment.id,
                            courseId = course.id,
                            studentToken = data.studentsList[it].token
                    )
                }.toMutableList()

        tokenLogin(teacher)
        coursesListPage.openCourse(course)
        courseBrowserPage.openAssignmentsTab()
        assignmentListPage.clickAssignment(assignment)
        assignmentDetailsPage.openSubmissionsPage()
        assignmentSubmissionListPage.clickSubmission(data.studentsList[selectStudent])

        return SpeedGraderPageData(
                submissions = assignmentSubmissions,
                students = data.studentsList
        )
    }
}

data class SpeedGraderPageData(
        val submissions: List<SeededCourseAssignmentSubmissions>,
        val students: List<CanvasUser>
)
