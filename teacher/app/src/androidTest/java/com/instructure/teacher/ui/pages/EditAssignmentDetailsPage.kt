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
package com.instructure.teacher.ui.pages

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.contrib.PickerActions
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.widget.DatePicker
import android.widget.TimePicker
import com.instructure.canvasapi2.utils.DateHelper
import com.instructure.teacher.R
import com.instructure.teacher.ui.utils.*
import com.instructure.teacher.ui.utils.pageAssert.PageAssert
import com.instructure.teacher.ui.utils.pageAssert.SimplePageAssert
import com.instructure.teacher.view.AssignmentOverrideView
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matchers
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*


class EditAssignmentDetailsPage : BasePage(), PageAssert by SimplePageAssert() {

    private val assignmentNameEditText by OnViewWithId(R.id.editAssignmentName)
    private val pointsPossibleEditText by WaitForViewWithId(R.id.editGradePoints)
    private val publishSwitch by WaitForViewWithId(R.id.publishSwitch)
    private val saveButton by OnViewWithId(R.id.menuSave)
    private val descriptionWebView by OnViewWithId(R.id.descriptionWebView, autoAssert = false)
    private val noDescriptionTextView by OnViewWithId(R.id.noDescriptionTextView, autoAssert = false)
    private val overlayContainer by OnViewWithId(R.id.overrideContainer, autoAssert = false)

    fun saveAssignment() {
        saveButton.click()
    }

    fun clickAssignmentNameEditText() {
        assignmentNameEditText.click()
    }

    fun clickPointsPossibleEditText() {
        pointsPossibleEditText.scrollTo().click()
    }

    fun editAssignmentName() : String {
        val newName = randomString()
        assignmentNameEditText.replaceText(newName)
        saveAssignment()
        return newName
    }

    fun editAssignmentPoints() : Double {
        val newPoints = randomDouble()
        val df = DecimalFormat("#")

        pointsPossibleEditText.replaceText(df.format(newPoints))
        saveAssignment()
        return newPoints
    }

    fun editAssignees() = waitForViewWithId(R.id.assignTo).scrollTo().click()
    fun clickEditDueDate() = waitForViewWithId(R.id.dueDate).scrollTo().click()
    fun clickEditDueTime() = waitForViewWithId(R.id.dueTime).scrollTo().click()
    fun clickEditUnlockDate() = waitForViewWithId(R.id.fromDate).scrollTo().click()
    fun clickEditUnlockTime() = waitForViewWithId(R.id.fromTime).scrollTo().click()
    fun clickEditLockDate() = waitForViewWithId(R.id.toDate).scrollTo().click()
    fun clickEditLockTime() = waitForViewWithId(R.id.toTime).scrollTo().click()

    fun clickAddOverride() = onView(allOf(withId(R.id.addOverride), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))).scrollTo().click()
    fun removeFirstOverride() {
        waitForViewWithContentDescription("remove_override_button_0").scrollTo().click()
        waitForViewWithText(R.string.remove).click()
    }

    fun editDate(year: Int, month: Int, dayOfMonth: Int) {
        waitForViewWithClassName(Matchers.equalTo<String>(DatePicker::class.java.name))
                .perform(PickerActions.setDate(year, month, dayOfMonth))
        onViewWithId(android.R.id.button1).click()
    }

    fun editTime(hour: Int, min: Int) {
        waitForViewWithClassName(Matchers.equalTo<String>(TimePicker::class.java.name))
                .perform(PickerActions.setTime(hour, min))
        onViewWithId(android.R.id.button1).click()
    }

    fun assertDateChanged(year: Int, month: Int, dayOfMonth: Int, id: Int) {
        val cal = Calendar.getInstance().apply {set(year, month, dayOfMonth)}
        waitForViewWithId(id).assertHasText(DateHelper.getFullMonthNoLeadingZeroDateFormat().format(cal.time))
    }

    fun assertTimeChanged(hour: Int, min: Int, id: Int) {
        val cal = Calendar.getInstance().apply {set(0, 0, 0, hour, min)}
        val sdh = SimpleDateFormat("H:mm a", Locale.US)
        waitForViewWithId(id).assertHasText(sdh.format(cal.time))
    }

    fun assertNewOverrideCreated() {
        waitForViewWithId(R.id.overrideContainer).check(has(2, Matchers.instanceOf(AssignmentOverrideView::class.java)))
    }

    fun assertOverrideRemoved() {
        waitForViewWithId(R.id.overrideContainer).check(has(1, Matchers.instanceOf(AssignmentOverrideView::class.java)))
    }

    fun assertDueDateBeforeUnlockDateErrorShown() {
        waitForViewWithId(R.id.fromDateTextInput).check(matches(hasTextInputLayoutErrorText(R.string.unlock_after_due_date_error)))
    }

    fun assertDueDateAfterLockDateErrorShown() {
        waitForViewWithId(R.id.toDateTextInput).check(matches(hasTextInputLayoutErrorText(R.string.lock_before_due_date_error)))
    }

    fun assertLockDateAfterUnlockDateErrorShown() {
        waitForViewWithId(R.id.toDateTextInput).check(matches(hasTextInputLayoutErrorText(R.string.lock_after_unlock_error)))
    }

    fun assertNoAssigneesErrorShown() {
        onView(withIndex(withId(R.id.assignToTextInput), 1)).check(matches(hasTextInputLayoutErrorText(R.string.assignee_blank_error)))
    }
}
