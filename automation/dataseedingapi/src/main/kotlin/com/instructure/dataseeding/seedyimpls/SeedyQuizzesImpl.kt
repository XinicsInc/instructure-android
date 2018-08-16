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
package com.instructure.dataseeding.seedyimpls

import com.instructure.dataseeding.InProcessServer
import com.instructure.dataseeding.Reaper
import com.instructure.dataseeding.SeedyReaper
import com.instructure.dataseeding.api.QuizzesApi
import com.instructure.soseedy.*
import com.instructure.soseedy.SeedyQuizzesGrpc.SeedyQuizzesImplBase
import io.grpc.stub.StreamObserver

class SeedyQuizzesImpl : SeedyQuizzesImplBase(), Reaper by SeedyReaper {

    //region API Calls
    private fun createQuiz(courseId: Long, withDescription: Boolean, lockAt: String, unlockAt: String, dueAt: String, published: Boolean, token: String) =
            QuizzesApi.createQuiz(courseId, withDescription, lockAt, unlockAt, dueAt, published, token)

    private fun createQuizSubmission(courseId: Long, quizId: Long, studentToken: String) = QuizzesApi.createQuizSubmission(courseId, quizId, studentToken)
    private fun completeQuizSubmission(courseId: Long, quizId: Long, submissionId: Long, attempt: Long, validationToken: String, studentToken: String) =
            QuizzesApi.completeQuizSubmission(courseId, quizId, submissionId, attempt, validationToken, studentToken)

    private fun createQuizQuestion(courseId: Long, quizId: Long, teacherToken: String) = QuizzesApi.createQuizQuestion(courseId, quizId, teacherToken)
    private fun publishQuiz(courseId: Long, quizId: Long, teacherToken: String, published: Boolean) = QuizzesApi.publishQuiz(courseId, quizId, teacherToken, published)
    //endregion

    override fun createQuiz(request: CreateQuizRequest, responseObserver: StreamObserver<Quiz>) {
        try {
            val quiz = createQuiz(
                    request.courseId,
                    request.withDescription,
                    request.lockAt,
                    request.unlockAt,
                    request.dueAt,
                    request.published,
                    request.token
            )

            val reply = Quiz.newBuilder()
                    .setId(quiz.id)
                    .setTitle(quiz.title)
                    .setPublished(quiz.published)
                    .build()

            onSuccess(responseObserver, reply)
        } catch (e: Exception) {
            onError(responseObserver, e)
        }
    }

    override fun createQuizSubmission(request: CreateQuizSubmissionRequest, responseObserver: StreamObserver<QuizSubmission>) {
        try {
            val response = createQuizSubmission(request.courseId, request.quizId, request.studentToken)

            val submission = response.quizSubmissions[0]
            val reply = QuizSubmission.newBuilder()
                    .setId(submission.id)
                    .setAttempt(submission.attempt)
                    .setValidationToken(submission.validationToken)
                    .build()

            onSuccess(responseObserver, reply)
        } catch (e: Exception) {
            onError(responseObserver, e)
        }
    }

    override fun completeQuizSubmission(request: CompleteQuizSubmissionRequest, responseObserver: StreamObserver<QuizSubmission>) {
        try {
            val response = completeQuizSubmission(
                    request.courseId,
                    request.quizId,
                    request.submissionId,
                    request.attempt,
                    request.validationToken,
                    request.studentToken
            )

            val submission = response.quizSubmissions[0]
            val reply = QuizSubmission.newBuilder()
                    .setId(submission.id)
                    .setAttempt(submission.attempt)
                    .setValidationToken(submission.validationToken)
                    .build()

            onSuccess(responseObserver, reply)
        } catch (e: Exception) {
            onError(responseObserver, e)
        }
    }

    override fun createQuizQuestion(request: CreateQuizQuestionRequest, responseObserver: StreamObserver<CreateQuizQuestionResponse>) {
        try {
            val response = createQuizQuestion(request.courseId, request.quizId, request.teacherToken)

            val reply = CreateQuizQuestionResponse.newBuilder()
                    .setId(response.id)
                    .build()

            onSuccess(responseObserver, reply)
        } catch (e: Exception) {
            onError(responseObserver, e)
        }
    }

    override fun publishQuiz(request: PublishQuizRequest, responseObserver: StreamObserver<Quiz>) {
        try {
            val quiz = publishQuiz(request.courseId,
                    request.quizId,
                    request.teacherToken,
                    request.published)

            val reply = Quiz.newBuilder()
                    .setId(quiz.id)
                    .setTitle(quiz.title)
                    .setPublished(quiz.published)
                    .build()

            onSuccess(responseObserver, reply)
        } catch (e: Exception) {
            onError(responseObserver, e)
        }
    }

    override fun seedQuizzes(request: SeedQuizzesRequest, responseObserver: StreamObserver<Quizzes>) {
        try {
            val quizzes = Quizzes.newBuilder()
                    .addAllQuizzes((0 until request.quizzes).map {
                        val createRequest = with(request) {
                            CreateQuizRequest.newBuilder()
                                    .setCourseId(courseId)
                                    .setWithDescription(withDescription)
                                    .setLockAt(lockAt)
                                    .setUnlockAt(unlockAt)
                                    .setDueAt(dueAt)
                                    .setPublished(published)
                                    .setToken(token)
                                    .build()
                        }

                        InProcessServer.quizClient.createQuiz(createRequest)
                    })
                    .build()

            onSuccess(responseObserver, quizzes)
        } catch (e: Exception) {
            onError(responseObserver, e)
        }
    }

    override fun seedQuizSubmission(request: SeedQuizSubmissionRequest, responseObserver: StreamObserver<QuizSubmission>) {
        try {
            // "you are not allowed to participate in this quiz" = make sure the quiz isn't locked
            val quizSubmission = with(request) {
                val createRequest = CreateQuizSubmissionRequest.newBuilder()
                        .setCourseId(courseId)
                        .setQuizId(quizId)
                        .setStudentToken(studentToken)
                        .build()
                val submission = InProcessServer.quizClient.createQuizSubmission(createRequest)

                if (complete) {
                    val completeRequest = CompleteQuizSubmissionRequest.newBuilder()
                            .setCourseId(courseId)
                            .setQuizId(quizId)
                            .setSubmissionId(submission.id)
                            .setAttempt(submission.attempt)
                            .setValidationToken(submission.validationToken)
                            .setStudentToken(studentToken)
                            .build()
                    InProcessServer.quizClient.completeQuizSubmission(completeRequest)
                } else
                    submission
            }

            onSuccess(responseObserver, quizSubmission)
        } catch (e: Exception) {
            onError(responseObserver, e)
        }
    }
}
