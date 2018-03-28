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
import com.instructure.dataseeding.api.EnrollmentTermsApi
import com.instructure.dataseeding.api.EnrollmentsApi
import com.instructure.dataseeding.model.EnrollmentTypes
import com.instructure.dataseeding.util.DataSeedingException
import com.instructure.soseedy.*
import com.instructure.soseedy.SeedyEnrollmentsGrpc.SeedyEnrollmentsImplBase
import io.grpc.stub.StreamObserver

class SeedyEnrollmentsImpl : SeedyEnrollmentsImplBase(), Reaper by SeedyReaper {

    //region API Calls
    private fun createEnrollmentTerm() = EnrollmentTermsApi.createEnrollmentTerm()

    private fun enrollUser(courseId: Long, userId: Long, enrollmentType: String) =
            when (enrollmentType) {
                EnrollmentTypes.STUDENT_ENROLLMENT -> {
                    EnrollmentsApi.enrollUserAsStudent(courseId, userId)
                }
                EnrollmentTypes.TEACHER_ENROLLMENT -> {
                    EnrollmentsApi.enrollUserAsTeacher(courseId, userId)
                }
                EnrollmentTypes.TA_ENROLLMENT -> {
                    EnrollmentsApi.enrollUserAsTA(courseId, userId)
                }
                EnrollmentTypes.DESIGNER_ENROLLMENT -> {
                    EnrollmentsApi.enrollUserAsDesigner(courseId, userId)
                }
                EnrollmentTypes.OBSERVER_ENROLLMENT -> {
                    EnrollmentsApi.enrollUserAsObserver(courseId, userId)
                }
                else -> {
                    throw DataSeedingException("Unknown Enrollment Type: " + enrollmentType)
                }
            }

    private fun enrollUserInSection(sectionId: Long, userId: Long, enrollmentType: String) =
            when (enrollmentType) {
                EnrollmentTypes.STUDENT_ENROLLMENT -> {
                    EnrollmentsApi.enrollUserInSectionAsStudent(sectionId, userId)
                }
                EnrollmentTypes.TEACHER_ENROLLMENT -> {
                    EnrollmentsApi.enrollUserInSectionAsTeacher(sectionId, userId)
                }
                EnrollmentTypes.TA_ENROLLMENT -> {
                    EnrollmentsApi.enrollUserInSectionAsTA(sectionId, userId)
                }
                EnrollmentTypes.DESIGNER_ENROLLMENT -> {
                    EnrollmentsApi.enrollUserInSectionAsDesigner(sectionId, userId)
                }
                EnrollmentTypes.OBSERVER_ENROLLMENT -> {
                    EnrollmentsApi.enrollUserInSectionAsObserver(sectionId, userId)
                }
                else -> {
                    throw DataSeedingException("Unknown Enrollment Type: " + enrollmentType)
                }
            }
    //endregion

    override fun createEnrollmentTerm(request: CreateEnrollmentTermRequest, responseObserver: StreamObserver<EnrollmentTerm>) {
        try {
            val enrollmentTerm = createEnrollmentTerm()

            val reply = EnrollmentTerm.newBuilder()
                    .setId(enrollmentTerm.id)
                    .build()

            onSuccess(responseObserver, reply)
        } catch (e: Exception) {
            onError(responseObserver, e)
        }
    }

    override fun enrollUserInCourse(request: EnrollUserRequest, responseObserver: StreamObserver<Enrollment>) {
        try {
            val enrollment = enrollUser(request.courseId, request.userId, request.enrollmentType)

            val reply = Enrollment.newBuilder()
                    .setCourseId(enrollment.courseId)
                    .setSectionId(enrollment.sectionId)
                    .setUserId(enrollment.userId)
                    .setRole(enrollment.role)
                    .setType(enrollment.type)
                    .build()

            onSuccess(responseObserver, reply)
        } catch (e: Exception) {
            onError(responseObserver, e)
        }
    }

    override fun enrollUserInSection(request: EnrollUserInSectionRequest, responseObserver: StreamObserver<Enrollment>) {
        try {
            val enrollment = enrollUserInSection(request.sectionId, request.userId, request.enrollmentType)

            val reply = Enrollment.newBuilder()
                    .setCourseId(enrollment.courseId)
                    .setSectionId(enrollment.sectionId)
                    .setUserId(enrollment.userId)
                    .setRole(enrollment.role)
                    .setType(enrollment.type)
                    .build()

            onSuccess(responseObserver, reply)
        } catch (e: Exception) {
            onError(responseObserver, e)
        }
    }

    companion object {
        fun seedEnrollment(courseId: Long, userId: Long, enrollmentType: String): Enrollment {
            return InProcessServer.enrollmentClient.enrollUserInCourse(EnrollUserRequest.newBuilder()
                    .setCourseId(courseId)
                    .setUserId(userId)
                    .setEnrollmentType(enrollmentType)
                    .build())
        }
    }
}
