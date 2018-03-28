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
import com.instructure.dataseeding.api.HealthCheckApi
import com.instructure.dataseeding.model.EnrollmentTypes
import com.instructure.dataseeding.seedyimpls.SeedyCoursesImpl.Companion.seedCourse
import com.instructure.dataseeding.seedyimpls.SeedyEnrollmentsImpl.Companion.seedEnrollment
import com.instructure.dataseeding.seedyimpls.SeedyUsersImpl.Companion.seedUser
import com.instructure.soseedy.*
import com.instructure.soseedy.SeedyGeneralGrpc.SeedyGeneralImplBase
import io.grpc.stub.StreamObserver

class GeneralSeedyImpl : SeedyGeneralImplBase(), Reaper by SeedyReaper {

    //region Canvas API calls
    private fun healthCheck() = HealthCheckApi.healthCheck()
    //endregion

    override fun getHealthCheck(request: HealthCheckRequest, responseObserver: StreamObserver<HealthCheck>) {
        try {
            val healthy = healthCheck()

            val reply = HealthCheck.newBuilder()
                    .setHealthy(healthy)
                    .build()

            onSuccess(responseObserver, reply)
        } catch (e: Exception) {
            onError(responseObserver, e)
        }
    }

    override fun seedData(request: SeedDataRequest, responseObserver: StreamObserver<SeededData>) {
        try {
            val seededData = SeededData.newBuilder()

            with(seededData) {
                for (c in 0 until maxOf(request.courses, request.favoriteCourses)) {
                    // Seed course
                    addCourses(seedCourse(request.gradingPeriods))

                    // Seed users
                    for (t in 0 until request.teachers) {
                        addTeachers(seedUser())
                        addEnrollments(seedEnrollment(coursesList[c].id, teachersList[t].id, EnrollmentTypes.TEACHER_ENROLLMENT))
                    }

                    for (s in 0 until request.students) {
                        addStudents(seedUser())
                        addEnrollments(seedEnrollment(coursesList[c].id, studentsList[s].id, EnrollmentTypes.STUDENT_ENROLLMENT))
                    }
                }

                // Seed favorite courses
                addAllFavorites(
                        (0 until minOf(request.favoriteCourses, coursesList.size))
                                .map {
                                    val favRequest = AddFavoriteCourseRequest.newBuilder()
                                            .setCourseId(coursesList[it].id)
                                            .setToken(teachersList[0].token)
                                            .build()

                                    InProcessServer.courseClient.addFavoriteCourse(favRequest)
                                }
                )

                // Seed discussions
                addAllDiscussions(
                        (0 until request.discussions).map {
                            val discussionRequest = CreateDiscussionRequest.newBuilder()
                                    .setCourseId(coursesList[0].id)
                                    .setToken(teachersList[0].token)
                                    .build()

                            InProcessServer.discussionClient.createDiscussion(discussionRequest)
                        }
                )

                // Seed announcements
                addAllDiscussions(
                        (0 until request.announcements).map {
                            val announcementRequest = CreateAnnouncementRequest.newBuilder()
                                    .setCourseId(coursesList[0].id)
                                    .setToken(teachersList[0].token)
                                    .build()

                            InProcessServer.discussionClient.createAnnouncement(announcementRequest)
                        }
                )
            }
            onSuccess(responseObserver, seededData.build())
        } catch (e: Exception) {
            onError(responseObserver, e)
        }
    }

    val SeededData.announcements: List<Discussion>
        get() =
            this.discussionsList.filter {
                it.isAnnouncement
            }
}
