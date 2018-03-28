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
package com.instructure.teacher.ui.pages

import android.support.test.espresso.Espresso
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.contrib.RecyclerViewActions.*
import android.support.test.espresso.matcher.BoundedMatcher
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.v7.widget.RecyclerView
import com.instructure.teacher.R
import com.instructure.teacher.holders.AssigneeItemViewHolder
import com.instructure.teacher.holders.CourseBrowserViewHolder
import com.instructure.teacher.ui.utils.*
import com.instructure.teacher.ui.utils.pageAssert.PageAssert
import com.instructure.teacher.ui.utils.pageAssert.SimplePageAssert
import com.instructure.teacher.viewinterface.CourseBrowserView
import com.pspdfkit.framework.it
import org.hamcrest.Description
import org.hamcrest.Matcher

class CourseBrowserPage : BasePage(), PageAssert by SimplePageAssert() {

    // TODO: Add recycler view scrolling to support small screen size devices.
    private val courseBrowserRecyclerView by WaitForViewWithId(R.id.courseBrowserRecyclerView)
    private val courseImage by OnViewWithId(R.id.courseImage)
    private val courseTitle by OnViewWithId(R.id.courseBrowserTitle)
    private val courseSubtitle by OnViewWithId(R.id.courseBrowserSubtitle)
    private val courseSettingsMenuButton by OnViewWithId(R.id.menu_course_browser_settings)

    fun openAssignmentsTab() {
        waitForViewWithText("Assignments").click()
    }

    fun openQuizzesTab() {
        /* The course browser RecyclerView is inside a CoordinatorLayout and is therefore only partially
        visible, causing some clicks to fail. We need to perform a swipe up first to make it fully visible. */
        Espresso.onView(ViewMatchers.withId(android.R.id.content)).perform(ViewActions.swipeUp())
        Espresso.onView(ViewMatchers.withId(R.id.courseBrowserRecyclerView))
                .perform(scrollToPosition<CourseBrowserViewHolder>(10))
        waitForViewWithText(R.string.tab_quizzes).click()
    }

    fun openDiscussionsTab() {
        onView(withId(R.id.courseBrowserRecyclerView)).perform(actionOnItemAtPosition<CourseBrowserViewHolder>(2, click()))
    }

    fun openAnnouncementsTab() {
        waitForViewWithText("Announcements").click()
    }

    fun clickSettingsButton() {
        courseSettingsMenuButton.click()
    }

    /**
     * Taken from https://stackoverflow.com/questions/37736616/espresso-how-to-find-a-specific-item-in-a-recycler-view-order-is-random
     *
     * This allows us to match a specific view with specific text in a specific RecyclerView.Holder
     */
    fun withTitle(title: String): Matcher<RecyclerView.ViewHolder> =
            object: BoundedMatcher<RecyclerView.ViewHolder, CourseBrowserViewHolder>(CourseBrowserViewHolder::class.java) {
                override fun matchesSafely(item: CourseBrowserViewHolder?): Boolean {
                    return item?.let {
                        it.labelText.text.toString().equals(title, true)
                    } ?: false
                }

                override fun describeTo(description: Description?) {
                    description?.appendText("view holder with title: " + title)
                }
            }
}
