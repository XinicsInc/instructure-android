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

import com.instructure.soseedy.Quiz
import com.instructure.teacher.R
import com.instructure.teacher.ui.utils.*
import com.instructure.teacher.ui.utils.pageAssert.PageAssert
import com.instructure.teacher.ui.utils.pageAssert.SimplePageAssert

class QuizListPage : BasePage(), PageAssert by SimplePageAssert() {
    private val quizListToolbar by OnViewWithId(R.id.quizListToolbar)

    private val quizRecyclerView by OnViewWithId(R.id.quizRecyclerView)

    //Only displayed when assignment list is empty
    private val emptyPandaView by WaitForViewWithId(R.id.emptyPandaView)

    fun assertDisplaysNoQuizzesView() {
        emptyPandaView.assertDisplayed()
    }

    fun assertHasQuiz(quiz: Quiz) {
        waitForViewWithText(quiz.title).assertDisplayed()
    }

    fun clickQuiz(quiz: Quiz) {
        waitForViewWithText(quiz.title).click()
    }
}
