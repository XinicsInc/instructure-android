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
package com.instructure.dataseeding.api

import com.instructure.dataseeding.model.*
import com.instructure.dataseeding.util.CanvasRestAdapter
import com.instructure.dataseeding.util.Randomizer
import com.instructure.soseedy.SubmissionType
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

object AssignmentsApi {
    interface AssignmentsService {
        @POST("courses/{courseId}/assignments")
        fun createAssignment(@Path("courseId") courseId: Long, @Body createAssignment: CreateAssignmentWrapper): Call<AssignmentApiModel>

        @POST("courses/{courseId}/assignments/{assignmentId}/overrides")
        fun createAssignmentOverride(@Path("courseId") courseId: Long, @Path("assignmentId") assignmentId: Long, @Body createAssignmentOverride: CreateAssignmentOverrideForStudentsWrapper): Call<AssignmentOverrideApiModel>
    }

    private fun assignmentsService(token: String): AssignmentsService
            = CanvasRestAdapter.retrofitWithToken(token).create(AssignmentsService::class.java)

    fun createAssignment(
            courseId: Long,
            withDescription: Boolean,
            lockAt: String,
            unlockAt: String,
            dueAt: String,
            submissionTypes: List<SubmissionType>,
            teacherToken: String,
            groupCategoryId: Long?): AssignmentApiModel {
        val assignment = CreateAssignmentWrapper(Randomizer.randomAssignment(
                withDescription,
                lockAt,
                unlockAt,
                dueAt,
                submissionTypes,
                groupCategoryId))

        return assignmentsService(teacherToken).createAssignment(courseId, assignment).execute().body()!!
    }

    fun createAssignmentOverride(
            courseId: Long,
            assignmentId: Long,
            token: String,
            studentIds: List<Long>?,
            groupId: Long?,
            courseSectionId: Long?,
            dueAt: String?,
            unlockAt: String?,
            lockAt: String?): AssignmentOverrideApiModel {
        val assignmentOverride = CreateAssignmentOverrideForStudentsWrapper(
                CreateAssignmentOverrideForStudents(
                        Randomizer.randomAssignmentOverrideTitle(),
                        studentIds,
                        groupId,
                        courseSectionId,
                        dueAt,
                        unlockAt,
                        lockAt))
        return assignmentsService(token).createAssignmentOverride(courseId, assignmentId, assignmentOverride).execute().body()!!
    }
}
