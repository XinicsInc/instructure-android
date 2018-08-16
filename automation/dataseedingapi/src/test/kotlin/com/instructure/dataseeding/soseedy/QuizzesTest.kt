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

package com.instructure.dataseeding.soseedy

import com.instructure.dataseeding.InProcessServer
import com.instructure.dataseeding.model.EnrollmentTypes.STUDENT_ENROLLMENT
import com.instructure.dataseeding.model.EnrollmentTypes.TEACHER_ENROLLMENT
import com.instructure.soseedy.*
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class QuizzesTest {
    private val course: Course = InProcessServer.courseClient.createCourse(CreateCourseRequest.newBuilder().build())
    private val teacher: CanvasUser = InProcessServer.userClient.createCanvasUser(CreateCanvasUserRequest.getDefaultInstance())
    private val student: CanvasUser = InProcessServer.userClient.createCanvasUser(CreateCanvasUserRequest.getDefaultInstance())

    @Before
    fun setUp() {
        InProcessServer.enrollmentClient.enrollUserInCourse(EnrollUserRequest.newBuilder()
                .setCourseId(course.id)
                .setUserId(teacher.id)
                .setEnrollmentType(TEACHER_ENROLLMENT)
                .build())
        InProcessServer.enrollmentClient.enrollUserInCourse(EnrollUserRequest.newBuilder()
                .setCourseId(course.id)
                .setUserId(student.id)
                .setEnrollmentType(STUDENT_ENROLLMENT)
                .build())
    }

    @Test
    fun createQuiz() {
        val request = CreateQuizRequest.newBuilder()
                .setCourseId(course.id)
                .setWithDescription(true)
                .setLockAt("")
                .setUnlockAt("")
                .setDueAt("")
                .setPublished(false)
                .setToken(teacher.token)
                .build()
        val quiz = InProcessServer.quizClient.createQuiz(request)
        assertThat(quiz, instanceOf(Quiz::class.java))
        assertTrue(quiz.id >= 1)
        assertTrue(quiz.title.isNotEmpty())
        assertFalse(quiz.published)
    }

    @Test
    fun createQuizQuestion() {
        val quiz = InProcessServer.quizClient.createQuiz(CreateQuizRequest.newBuilder()
                .setCourseId(course.id)
                .setWithDescription(true)
                .setLockAt("")
                .setUnlockAt("")
                .setDueAt("")
                .setPublished(false)
                .setToken(teacher.token)
                .build())
        val request = CreateQuizQuestionRequest.newBuilder()
                .setCourseId(course.id)
                .setQuizId(quiz.id)
                .setTeacherToken(teacher.token)
                .build()
        val question = InProcessServer.quizClient.createQuizQuestion(request)
        assertThat(question, instanceOf(CreateQuizQuestionResponse::class.java))
        assertTrue(question.id >= 1)
    }

    @Test
    fun publishQuiz() {
        var quiz = InProcessServer.quizClient.createQuiz(CreateQuizRequest.newBuilder()
                .setCourseId(course.id)
                .setWithDescription(true)
                .setLockAt("")
                .setUnlockAt("")
                .setDueAt("")
                .setPublished(false)
                .setToken(teacher.token)
                .build())
        val request = PublishQuizRequest.newBuilder()
                .setCourseId(course.id)
                .setQuizId(quiz.id)
                .setPublished(true)
                .setTeacherToken(teacher.token)
                .build()
        quiz = InProcessServer.quizClient.publishQuiz(request)
        assertThat(quiz, instanceOf(Quiz::class.java))
        assertTrue(quiz.id >= 1)
        assertTrue(quiz.title.isNotEmpty())
        assertTrue(quiz.published)
    }

    @Test
    fun createQuizSubmission() {
        val quiz = InProcessServer.quizClient.createQuiz(CreateQuizRequest.newBuilder()
                .setCourseId(course.id)
                .setWithDescription(true)
                .setLockAt("")
                .setUnlockAt("")
                .setDueAt("")
                .setPublished(true)
                .setToken(teacher.token)
                .build())
        val request = CreateQuizSubmissionRequest.newBuilder()
                .setCourseId(course.id)
                .setQuizId(quiz.id)
                .setStudentToken(student.token)
                .build()
        val submission = InProcessServer.quizClient.createQuizSubmission(request)
        assertThat(submission, instanceOf(QuizSubmission::class.java))
        assertTrue(submission.id >= 1)
        assertEquals(1, submission.attempt)
        assertTrue(submission.validationToken.isNotEmpty())
    }

    @Test
    fun completeQuizSubmission() {
        val quiz = InProcessServer.quizClient.createQuiz(CreateQuizRequest.newBuilder()
                .setCourseId(course.id)
                .setWithDescription(true)
                .setLockAt("")
                .setUnlockAt("")
                .setDueAt("")
                .setPublished(true)
                .setToken(teacher.token)
                .build())
        var submission = InProcessServer.quizClient.createQuizSubmission(CreateQuizSubmissionRequest.newBuilder()
                .setCourseId(course.id)
                .setQuizId(quiz.id)
                .setStudentToken(student.token)
                .build())
        val request = CompleteQuizSubmissionRequest.newBuilder()
                .setCourseId(course.id)
                .setQuizId(quiz.id)
                .setSubmissionId(submission.id)
                .setAttempt(submission.attempt)
                .setValidationToken(submission.validationToken)
                .setStudentToken(student.token)
                .build()
        submission = InProcessServer.quizClient.completeQuizSubmission(request)
        assertThat(submission, instanceOf(QuizSubmission::class.java))
        assertEquals(1, submission.attempt)
        assertTrue(submission.validationToken.isNotEmpty())
    }

    @Test
    fun seedQuizzes() {
        for (quizCount in 0..2) {
            val request = SeedQuizzesRequest.newBuilder()
                    .setCourseId(course.id)
                    .setQuizzes(quizCount)
                    .setToken(teacher.token)
                    .build()
            val quizzes = InProcessServer.quizClient.seedQuizzes(request)
            assertEquals(quizCount, quizzes.quizzesCount)
        }
    }
}
