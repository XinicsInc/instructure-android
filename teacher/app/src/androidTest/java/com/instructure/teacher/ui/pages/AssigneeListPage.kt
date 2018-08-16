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
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.contrib.RecyclerViewActions.actionOnHolderItem
import android.support.test.espresso.contrib.RecyclerViewActions.scrollToHolder
import android.support.test.espresso.matcher.BoundedMatcher
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.v7.widget.RecyclerView
import com.instructure.teacher.R
import com.instructure.teacher.holders.AssigneeItemViewHolder
import com.instructure.teacher.ui.utils.*
import com.instructure.teacher.ui.utils.pageAssert.PageAssert
import com.instructure.teacher.ui.utils.pageAssert.PageWithIdAssert
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf

@Suppress("unused")
class AssigneeListPage : BasePage(), PageAssert by PageWithIdAssert(R.id.assigneeListPage) {

    private val titleTextView by OnViewWithText(R.string.page_title_add_assignees)
    private val closeButton by OnViewWithContentDescription(R.string.close)
    private val saveButton by OnViewWithId(R.id.menuSave)
    private val recyclerView by WaitForViewWithId(R.id.recyclerView)

    fun assertDisplaysAssigneeOptions(
            sectionNames: List<String> = emptyList(),
            groupNames: List<String> = emptyList(),
            studentNames: List<String> = emptyList()) {
        for (assigneeName in (sectionNames + groupNames + studentNames)) {
            onView(allOf(withText(assigneeName), hasSibling(withId(R.id.assigneeSubtitleView)))).assertDisplayed()
        }
    }

    fun assertAssigneesSelected(assigneeNames: List<String>) {
        val selectedTextView = onViewWithId(R.id.selectedAssigneesTextView)
        for (name in assigneeNames) selectedTextView.assertContainsText(name)
    }

    fun toggleAssignees(assigneeNames: List<String>) {
        assigneeNames
                .map { withTitle(it) }
                .forEach { onView((withId(R.id.recyclerView))).perform(scrollToHolder(it), actionOnHolderItem(it, click())) }
    }

    fun saveAndClose() {
        saveButton.click()
    }
}

/**
 * Taken from https://stackoverflow.com/questions/37736616/espresso-how-to-find-a-specific-item-in-a-recycler-view-order-is-random
 *
 * This allows us to match a specific view with specific text in a specific RecyclerView.Holder
 */
fun withTitle(title: String): Matcher<RecyclerView.ViewHolder> =
        object: BoundedMatcher <RecyclerView.ViewHolder, AssigneeItemViewHolder>(AssigneeItemViewHolder::class.java) {
            override fun matchesSafely(item: AssigneeItemViewHolder?): Boolean {
                return item?.let {
                    it.assigneeTitleViewForTest.text.toString().equals(title, true)
                } ?: false
            }

            override fun describeTo(description: Description?) {
                description?.appendText("view holder with title: " + title)
            }
        }