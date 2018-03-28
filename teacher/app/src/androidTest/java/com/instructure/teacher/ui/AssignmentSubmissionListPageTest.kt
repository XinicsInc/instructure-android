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

import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.matcher.ViewMatchers.isRoot
import com.instructure.dataseeding.util.ago
import com.instructure.dataseeding.util.days
import com.instructure.dataseeding.util.iso8601
import com.instructure.soseedy.SeededData
import com.instructure.soseedy.SubmissionSeed
import com.instructure.soseedy.SubmissionType.*
import com.instructure.teacher.R
import com.instructure.teacher.ui.utils.*
import com.instructure.teacher.utils.TeacherPrefs
import org.junit.Test

class AssignmentSubmissionListPageTest : TeacherTest() {

    @Test
    override fun displaysPageObjects() {
        goToAssignmentSubmissionListPage()
        assignmentSubmissionListPage.assertPageObjects()
    }

    @Test
    fun displaysNoSubmissionsView() {
        goToAssignmentSubmissionListPage(
                students = 0,
                submissions = 0
        )
        assignmentSubmissionListPage.assertDisplaysNoSubmissionsView()
    }

    @Test
    fun filterLateSubmissions() {
        goToAssignmentSubmissionListPage(
                dueAt = 7.days.ago.iso8601
        )
        assignmentSubmissionListPage.clickFilterButton()
        assignmentSubmissionListPage.clickFilterSubmissions()
        assignmentSubmissionListPage.clickFilterSubmittedLate()
        assignmentSubmissionListPage.clickFilterDialogOk()
        assignmentSubmissionListPage.assertDisplaysClearFilter()
        assignmentSubmissionListPage.assertFilterLabelText(R.string.submitted_late)
        assignmentSubmissionListPage.assertHasSubmission()
    }

    @Test
    fun filterUngradedSubmissions() {
        goToAssignmentSubmissionListPage()
        assignmentSubmissionListPage.clickFilterButton()
        assignmentSubmissionListPage.clickFilterSubmissions()
        assignmentSubmissionListPage.clickFilterUngraded()
        assignmentSubmissionListPage.clickFilterDialogOk()
        assignmentSubmissionListPage.assertDisplaysClearFilter()
        assignmentSubmissionListPage.assertFilterLabelText(R.string.havent_been_graded)
        assignmentSubmissionListPage.assertHasSubmission()
    }

    @Test
    fun displaysAssignmentStatusSubmitted() {
        goToAssignmentSubmissionListPage()
        assignmentSubmissionListPage.assertSubmissionStatusSubmitted()
    }

    @Test
    fun displaysAssignmentStatusMissing() {
        goToAssignmentSubmissionListPage(
                students = 1,
                submissions = 0,
                dueAt = 1.days.ago.iso8601
        )
        assignmentSubmissionListPage.assertSubmissionStatusMissing()
    }

    @Test
    fun displaysAssignmentStatusNotSubmitted() {
        goToAssignmentSubmissionListPage(
                students = 1,
                submissions = 0
        )
        assignmentSubmissionListPage.assertSubmissionStatusNotSubmitted()
    }

    @Test
    fun displaysAssignmentStatusLate() {
        goToAssignmentSubmissionListPage(
                dueAt = 7.days.ago.iso8601
        )
        assignmentSubmissionListPage.assertSubmissionStatusLate()
    }

    @Test
    fun messageStudentsWho() {
        val data = goToAssignmentSubmissionListPage(
                students = 1
        )
        val student = data.studentsList[0]
        assignmentSubmissionListPage.clickAddMessage()
        addMessagePage.assertPageObjects()
        addMessagePage.assertHasStudentRecipient(student)
    }

    @Test
    fun togglesMute() {
        goToAssignmentSubmissionListPage()
        openOverflowMenu()
        assignmentSubmissionListPage.assertDisplaysMuteOption()
        assignmentSubmissionListPage.clickMuteOption()
        assignmentSubmissionListPage.assertDisplaysMutedStatus()
        openOverflowMenu()
        assignmentSubmissionListPage.assertDisplaysUnmuteOption()
    }

    @Test
    fun toggleAnonymousGrading() {
        goToAssignmentSubmissionListPage()
        openOverflowMenu()
        assignmentSubmissionListPage.assertDisplaysEnableAnonymousOption()
        assignmentSubmissionListPage.clickAnonymousOption()
        assignmentSubmissionListPage.assertDisplaysAnonymousGradingStatus()
        assignmentSubmissionListPage.assertDisplaysAnonymousName()
        openOverflowMenu()
        assignmentSubmissionListPage.assertDisplaysDisableAnonymousOption()
    }

    private fun goToAssignmentSubmissionListPage(
            students: Int = 1,
            submissions: Int = 1,
            dueAt: String = ""
    ): SeededData {
        val data = seedData(teachers = 1, favoriteCourses = 1, students = students)
        val course = data.coursesList[0]
        val teacher = data.teachersList[0]
        val assignment = seedAssignments(
                courseId = course.id,
                assignments = 1,
                submissionTypes = listOf(ONLINE_TEXT_ENTRY),
                dueAt = dueAt,
                teacherToken = teacher.token).assignmentsList[0]

        for (s in 0 until submissions) {
            seedAssignmentSubmission(
                    listOf(
                            SubmissionSeed.newBuilder()
                                    .setSubmissionType(ONLINE_TEXT_ENTRY)
                                    .setAmount(1).build()),
                    assignment.id,
                    course.id,
                    data.studentsList[s].token)
        }

        tokenLogin(teacher)
        coursesListPage.openCourse(course)
        courseBrowserPage.openAssignmentsTab()
        assignmentListPage.clickAssignment(assignment)
        assignmentDetailsPage.openSubmissionsPage()

        return data
    }
}