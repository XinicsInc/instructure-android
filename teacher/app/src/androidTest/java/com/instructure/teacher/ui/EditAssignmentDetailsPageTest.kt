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
 *
 */
package com.instructure.teacher.ui

import com.instructure.canvasapi2.utils.NumberHelper
import com.instructure.soseedy.Assignment
import com.instructure.soseedy.SubmissionType
import com.instructure.teacher.R.id.*
import com.instructure.teacher.ui.utils.*
import org.junit.Test

class EditAssignmentDetailsPageTest : TeacherTest() {

    @Test
    @TestRail(ID = "C3109580")
    override fun displaysPageObjects() {
        getToEditAssignmentDetailsPage()
        editAssignmentDetailsPage.assertPageObjects()
    }

    @Test
    @TestRail(ID = "C3134126")
    fun editAssignmentName() {
        getToEditAssignmentDetailsPage()
        editAssignmentDetailsPage.clickAssignmentNameEditText()
        val newAssignmentName: String = editAssignmentDetailsPage.editAssignmentName()
        assignmentDetailsPage.assertAssignmentNameChanged(newAssignmentName)
    }

    @Test
    @TestRail(ID = "C3134126")
    fun editAssignmentPoints() {
        getToEditAssignmentDetailsPage()
        editAssignmentDetailsPage.clickPointsPossibleEditText()
        val newPoints: Double = editAssignmentDetailsPage.editAssignmentPoints()
        val stringPoints = NumberHelper.formatDecimal(newPoints, 1, true)
        assignmentDetailsPage.assertAssignmentPointsChanged(stringPoints)
    }

    @Test
    fun editDueDate() {
        getToEditAssignmentDetailsPage()
        editAssignmentDetailsPage.clickEditDueDate()
        editAssignmentDetailsPage.editDate(2017, 1, 1)
        editAssignmentDetailsPage.assertDateChanged(2017, 0, 1, dueDate)
    }

    @Test
    fun editDueTime() {
        getToEditAssignmentDetailsPage()
        editAssignmentDetailsPage.clickEditDueTime()
        editAssignmentDetailsPage.editTime(1, 30)
        editAssignmentDetailsPage.assertTimeChanged(1, 30, dueTime)
    }

    @Test
    fun editUnlockDate() {
        getToEditAssignmentDetailsPage()
        editAssignmentDetailsPage.clickEditUnlockDate()
        editAssignmentDetailsPage.editDate(2017, 1, 1)
        editAssignmentDetailsPage.assertDateChanged(2017, 0, 1, fromDate)
    }

    @Test
    fun editUnlockTime() {
        getToEditAssignmentDetailsPage()
        editAssignmentDetailsPage.clickEditUnlockTime()
        editAssignmentDetailsPage.editTime(1, 30)
        editAssignmentDetailsPage.assertTimeChanged(1, 30, fromTime)
    }

    @Test
    fun editLockDate() {
        getToEditAssignmentDetailsPage()
        editAssignmentDetailsPage.clickEditLockDate()
        editAssignmentDetailsPage.editDate(2017, 1, 1)
        editAssignmentDetailsPage.assertDateChanged(2017, 0, 1, toDate)
    }

    @Test
    fun editLockTime() {
        getToEditAssignmentDetailsPage()
        editAssignmentDetailsPage.clickEditLockTime()
        editAssignmentDetailsPage.editTime(1, 30)
        editAssignmentDetailsPage.assertTimeChanged(1, 30, toTime)
    }

    @Test
    fun addOverride() {
        getToEditAssignmentDetailsPage()
        editAssignmentDetailsPage.clickAddOverride()
        assigneeListPage.saveAndClose()
        editAssignmentDetailsPage.assertNewOverrideCreated()
    }

    @Test
    fun removeOverride() {
        getToEditAssignmentDetailsPage()
        editAssignmentDetailsPage.clickAddOverride()
        assigneeListPage.saveAndClose()
        editAssignmentDetailsPage.assertNewOverrideCreated()
        editAssignmentDetailsPage.removeFirstOverride()
        editAssignmentDetailsPage.assertOverrideRemoved()
    }

    @Test
    fun dueDateBeforeUnlockDateError() {
        getToEditAssignmentDetailsPage()
        editAssignmentDetailsPage.clickEditDueDate()
        editAssignmentDetailsPage.editDate(1987, 8, 10)
        editAssignmentDetailsPage.clickEditUnlockDate()
        editAssignmentDetailsPage.editDate(2015, 2, 10)
        editAssignmentDetailsPage.saveAssignment()
        editAssignmentDetailsPage.assertDueDateBeforeUnlockDateErrorShown()
    }

    @Test
    fun dueDateAfterLockDateError() {
        getToEditAssignmentDetailsPage()
        editAssignmentDetailsPage.clickEditDueDate()
        editAssignmentDetailsPage.editDate(2015, 2, 10)
        editAssignmentDetailsPage.clickEditLockDate()
        editAssignmentDetailsPage.editDate(1987, 8, 10)
        editAssignmentDetailsPage.saveAssignment()
        editAssignmentDetailsPage.assertDueDateAfterLockDateErrorShown()
    }

    @Test
    fun unlockDateAfterLockDateError() {
        getToEditAssignmentDetailsPage()
        editAssignmentDetailsPage.clickEditUnlockDate()
        editAssignmentDetailsPage.editDate(2015, 2, 10)
        editAssignmentDetailsPage.clickEditLockDate()
        editAssignmentDetailsPage.editDate(1987, 8, 10)
        editAssignmentDetailsPage.saveAssignment()
        editAssignmentDetailsPage.assertLockDateAfterUnlockDateErrorShown()
    }

    @Test
    fun noAssigneesError() {
        getToEditAssignmentDetailsPage()
        editAssignmentDetailsPage.clickAddOverride()
        assigneeListPage.saveAndClose()
        editAssignmentDetailsPage.assertNewOverrideCreated()
        editAssignmentDetailsPage.saveAssignment()
        editAssignmentDetailsPage.assertNoAssigneesErrorShown()
    }

    private fun getToEditAssignmentDetailsPage(
            assignments: Int = 1,
            withDescription: Boolean = false,
            lockAt: String = "",
            unlockAt: String = "",
            submissionTypes: List<SubmissionType> = emptyList()): Assignment {

        val data = seedData(teachers = 1, favoriteCourses = 1)
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

        tokenLogin(teacher)

        coursesListPage.openCourse(course)
        courseBrowserPage.openAssignmentsTab()
        assignmentListPage.clickAssignment(assignment.assignmentsList[0])

        assignmentDetailsPage.openEditPage()
        return assignment.assignmentsList[0]
    }
}
