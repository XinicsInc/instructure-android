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
import com.instructure.dataseeding.api.CoursesApi
import com.instructure.soseedy.*
import com.instructure.soseedy.SeedyCoursesGrpc.SeedyCoursesImplBase
import io.grpc.stub.StreamObserver

class SeedyCoursesImpl : SeedyCoursesImplBase(), Reaper by SeedyReaper {

    //region API Calls
    private fun createCourse(enrollmentTermId: Long?) = CoursesApi.createCourse(enrollmentTermId)

    private fun addFavoriteCourse(courseId: Long, token: String) = CoursesApi.addCourseToFavorites(courseId, token)
    //endregion

    override fun createCourse(request: CreateCourseRequest, responseObserver: StreamObserver<Course>) {
        try {
            val course = createCourse(if (request.termId != 0L) request.termId else null)

            val reply = Course.newBuilder()
                    .setId(course.id)
                    .setName(course.name)
                    .setCourseCode(course.courseCode)
                    .build()

            onSuccess(responseObserver, reply)
        } catch (e: Exception) {
            onError(responseObserver, e)
        }
    }

    override fun addFavoriteCourse(request: AddFavoriteCourseRequest, responseObserver: StreamObserver<Favorite>) {
        try {
            val favorite = addFavoriteCourse(request.courseId, request.token)

            val reply = Favorite.newBuilder()
                    .setContextId(favorite.contextId)
                    .build()

            onSuccess(responseObserver, reply)
        } catch (e: Exception) {
            onError(responseObserver, e)
        }
    }

    companion object {
        fun seedCourse(gradingPeriods: Boolean): Course {
            // Create a new course
            return if (gradingPeriods) {
                // Create enrollment term
                val enrollmentRequest = CreateEnrollmentTermRequest.getDefaultInstance()
                val enrollmentTerm = InProcessServer.enrollmentClient.createEnrollmentTerm(enrollmentRequest)
                // Create grading period set
                val createGradingPeriodSetRequest = CreateGradingPeriodSetRequest.newBuilder().setEnrollmentId(enrollmentTerm.id).build()
                val gradingPeriodSet = InProcessServer.gradingClient.createGradingPeriodSet(createGradingPeriodSetRequest)
                // Create course, assign it to previously created enrollment term
                val createCourseRequest = CreateCourseRequest.newBuilder().setTermId(enrollmentTerm.id).build()
                val courseWithTerm = InProcessServer.courseClient.createCourse(createCourseRequest)
                // Create grading period using enrollment term
                val createGradingPeriodRequest = CreateGradingPeriodRequest.newBuilder().setGradingPeriodSetId(gradingPeriodSet.id).build()
                val gradingPeriod = InProcessServer.gradingClient.createGradingPeriod(createGradingPeriodRequest)

                courseWithTerm
            } else {
                InProcessServer.courseClient.createCourse(CreateCourseRequest.getDefaultInstance())
            }
        }
    }
}
