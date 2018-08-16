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

import com.instructure.soseedy.Discussion
import com.instructure.teacher.R
import com.instructure.teacher.ui.utils.OnViewWithId
import com.instructure.teacher.ui.utils.assertDisplayed
import com.instructure.teacher.ui.utils.click
import com.instructure.teacher.ui.utils.pageAssert.PageAssert
import com.instructure.teacher.ui.utils.pageAssert.SimplePageAssert
import com.instructure.teacher.ui.utils.waitForViewWithText

class AnnouncementsListPage : BasePage(), PageAssert by SimplePageAssert() {

    private val announcementListToolbar by OnViewWithId(R.id.discussionListToolbar)
    private val announcementsFAB by OnViewWithId(R.id.createNewDiscussion)
    private val announcementsRecyclerView by OnViewWithId(R.id.discussionRecyclerView)

    fun clickDiscussion(discussion: Discussion) {
        waitForViewWithText(discussion.title).click()
    }

    fun assertHasAnnouncement(discussion: Discussion) {
        waitForViewWithText(discussion.title).assertDisplayed()
    }

    fun assertFAB() {
        announcementsFAB.assertDisplayed()
    }
}