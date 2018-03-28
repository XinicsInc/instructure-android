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

import com.instructure.dataseeding.util.ago
import com.instructure.dataseeding.util.days
import com.instructure.dataseeding.util.iso8601
import com.instructure.soseedy.Quiz
import com.instructure.teacher.ui.utils.TeacherTest
import com.instructure.teacher.ui.utils.seedData
import com.instructure.teacher.ui.utils.seedQuizzes
import com.instructure.teacher.ui.utils.tokenLogin
import org.junit.Test

class QuizListPageTest : TeacherTest() {

    @Test
    override fun displaysPageObjects() {
        getToQuizzesPage()
        quizListPage.assertPageObjects()
    }

    @Test
    fun displaysNoQuizzesView() {
        getToQuizzesPage(makeQuiz = false)
        quizListPage.assertDisplaysNoQuizzesView()
    }

    @Test
    fun displaysQuiz() {
        val quiz = getToQuizzesPage()
        if (quiz != null) {
            quizListPage.assertHasQuiz(quiz)
        }
    }

    private fun getToQuizzesPage(makeQuiz: Boolean = true): Quiz? {
        val data = seedData(teachers = 1, favoriteCourses = 1, students = 1)
        val teacher = data.teachersList[0]
        val course = data.coursesList[0]
        var quiz: Quiz? = null

        if (makeQuiz) {
            quiz = seedQuizzes(
                    courseId = course.id,
                    quizzes = 1,
                    withDescription = false,
                    lockAt = 1.days.ago.iso8601,
                    unlockAt = 2.days.ago.iso8601,
                    teacherToken = teacher.token).quizzesList[0]
        }

        tokenLogin(teacher)
        coursesListPage.openCourse(course)
        courseBrowserPage.openQuizzesTab()

        return quiz
    }
}
