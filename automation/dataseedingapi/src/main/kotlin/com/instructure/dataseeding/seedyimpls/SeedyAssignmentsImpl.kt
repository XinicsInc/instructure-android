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
import com.instructure.dataseeding.api.AssignmentsApi
import com.instructure.dataseeding.api.SubmissionsApi
import com.instructure.soseedy.*
import com.instructure.soseedy.SeedyAssignmentsGrpc.SeedyAssignmentsImplBase
import io.grpc.stub.StreamObserver
import java.lang.Thread.sleep

class SeedyAssignmentsImpl : SeedyAssignmentsImplBase(), Reaper by SeedyReaper {
    //region API Calls
    private fun createAssignment(courseId: Long, withDescription: Boolean, locked: String, unlocked: String, dueAt: String, submissionTypes: List<SubmissionType>, teacherToken: String, groupCategoryId: Long?) =
            AssignmentsApi.createAssignment(courseId, withDescription, locked, unlocked, dueAt, submissionTypes, teacherToken, groupCategoryId)

    private fun createAssignmentOverride(courseId: Long, assignmentId: Long, token: String, studentIds: List<Long>?, groupId: Long?, courseSectionId: Long?, dueAt: String?, unlockAt: String?, lockAt:String?) =
            AssignmentsApi.createAssignmentOverride(courseId, assignmentId, token, studentIds, groupId, courseSectionId, dueAt, unlockAt, lockAt)

    private fun createCourseAssignmentComment(studentToken: String, courseId: Long, assignmentId: Long, fileIds: MutableList<Long>) =
            SubmissionsApi.commentOnSubmission(studentToken, courseId, assignmentId, fileIds)

    private fun createCourseSubmission(submissionType: SubmissionType, courseId: Long, assignmentId: Long, fileIds: MutableList<Long>, studentToken: String) =
            SubmissionsApi.submitCourseAssignment(submissionType, courseId, assignmentId, fileIds, studentToken)

    //endregion

    override fun createAssignment(request: CreateAssignmentRequest, responseObserver: StreamObserver<Assignment>) {
        try {
            val assignment = createAssignment(
                    request.courseId,
                    request.withDescription,
                    request.lockAt,
                    request.unlockAt,
                    request.dueAt,
                    request.submissionTypesList,
                    request.teacherToken,
                    if (request.groupCategoryId != 0L) request.groupCategoryId else null
            )

            val reply = Assignment.newBuilder()
                    .setId(assignment.id)
                    .setName(assignment.name)
                    .setPublished(assignment.published)
                    .build()

            onSuccess(responseObserver, reply)
        } catch (e: Exception) {
            onError(responseObserver, e)
        }
    }

    override fun createAssignmentOverride(request: CreateAssignmentOverrideRequest, responseObserver: StreamObserver<AssignmentOverride>) {
        try {
            val assignmentOverride = createAssignmentOverride(
                    request.courseId,
                    request.assignmentId,
                    request.token,
                    if (request.studentIdsCount != 0) request.studentIdsList else null,
                    if (request.groupId != 0L) request.groupId else null,
                    if (request.courseSectionId != 0L) request.courseSectionId else null,
                    if (request.dueAt.isNotEmpty()) request.dueAt else null,
                    if (request.unlockAt.isNotEmpty()) request.unlockAt else null,
                    if (request.lockAt.isNotEmpty()) request.lockAt else null
            )

            val reply = AssignmentOverride.newBuilder()
                    .setId(assignmentOverride.id)
                    .setAssignmentId(assignmentOverride.assignmentId)
                    .setTitle(assignmentOverride.title)

            if (assignmentOverride.studentIds != null) reply.addAllStudentIds(assignmentOverride.studentIds)
            if (assignmentOverride.groupId != null) reply.groupId = assignmentOverride.groupId
            if (assignmentOverride.courseSectionId != null) reply.courseSectionId = assignmentOverride.courseSectionId
            if (assignmentOverride.dueAt != null) reply.dueAt = assignmentOverride.dueAt
            if (assignmentOverride.unlockAt != null) reply.unlockAt = assignmentOverride.unlockAt
            if (assignmentOverride.lockAt != null) reply.lockAt = assignmentOverride.lockAt

            onSuccess(responseObserver, reply.build())
        } catch (e: Exception) {
            onError(responseObserver, e)
        }
    }

    override fun submitCourseAssignment(request: SubmitCourseAssignmentRequest, responseObserver: StreamObserver<CourseAssignmentSubmission>) {
        try {
            val submission = createCourseSubmission(
                    request.submissionType,
                    request.courseId,
                    request.assignmentId,
                    request.fileIdsList,
                    request.studentToken
            )

            val builder = CourseAssignmentSubmission.newBuilder()
                    .setId(submission.id)
                    .addAllSubmissionComments(submission.submissionComments.map {
                        Comment.newBuilder()
                                .setAuthorName(it.authorName)
                                .setComment(it.comment)
                                .build()
                    })

            submission.url?.let {
                builder.url = it
            }

            submission.body?.let {
                builder.body = it
            }

            val reply = builder.build()
            onSuccess(responseObserver, reply)
        } catch (e: Exception) {
            onError(responseObserver, e)
        }
    }

    override fun createCourseAssignmentSubmissionComment(request: CreateCourseAssignmentCommentRequest, responseObserver: StreamObserver<CourseAssignmentSubmission>) {
        try {
            val comment = createCourseAssignmentComment(
                    request.studentToken,
                    request.courseId,
                    request.assignmentId,
                    request.fileIdsList
            )

            val reply = CourseAssignmentSubmission.newBuilder()
                    .setId(comment.id)
                    .addAllSubmissionComments(comment.submissionComments?.map {
                        Comment.newBuilder()
                                .setAuthorName(it.authorName)
                                .setComment(it.comment)
                                .addAllAttachments(it.attachments?.map {
                                    Attachment.newBuilder()
                                            .setDisplayName(it.displayName)
                                            .setFileName(it.fileName)
                                            .setId(it.id)
                                            .build()
                                } ?: emptyList()) // No attachments, use empty list
                                .build()
                    } ?: emptyList()) // No comments, use empty list
                    .build()
            onSuccess(responseObserver, reply)
        } catch (e: Exception) {
            onError(responseObserver, e)
        }
    }

    override fun seedAssignments(request: SeedAssignmentRequest, responseObserver: StreamObserver<Assignments>) {
        try {
            val seededAssignments = Assignments.newBuilder()

            seededAssignments.addAllAssignments(
                    (0 until request.assignments).map {
                        val assignmentRequest = with(request) {
                            CreateAssignmentRequest.newBuilder()
                                    .setCourseId(courseId)
                                    .setWithDescription(withDescription)
                                    .setLockAt(lockAt)
                                    .setUnlockAt(unlockAt)
                                    .setDueAt(dueAt)
                                    .addAllSubmissionTypes(submissionTypesList)
                                    .setTeacherToken(teacherToken)
                                    .build()
                        }

                        InProcessServer.assignmentClient.createAssignment(assignmentRequest)
                    })

            onSuccess(responseObserver, seededAssignments.build())
        } catch (e: Exception) {
            onError(responseObserver, e)
        }
    }

    override fun seedAssignmentSubmission(request: SeedAssignmentSubmissionRequest, responseObserver: StreamObserver<SeededCourseAssignmentSubmissions>) {
        try {
            val submissionsList: MutableList<CourseAssignmentSubmission> = mutableListOf()
            with(request) {
                for (seed in submissionSeedsList) {
                    for (t in 0 until seed.amount) {

                        // Submit an assignment
                        val submissionRequest = SubmitCourseAssignmentRequest.newBuilder()
                                .setAssignmentId(assignmentId)
                                .setCourseId(courseId)
                                .setStudentToken(studentToken)
                                .setSubmissionType(seed.submissionType)
                                .addAllFileIds(seed.attachmentsList.map { it.id })
                                .build()

                        // Canvas will only record submissions with unique "submitted_at" values.
                        // Sleep for 1 second to ensure submissions are recorded!!!
                        //
                        // https://github.com/instructure/mobile_qa/blob/7f985a08161f457e9b5d60987bd6278d21e2557e/SoSeedy/lib/so_seedy/canvas_models/account_admin.rb#L357-L359
                        sleep(1000)
                        var submission = InProcessServer.assignmentClient.submitCourseAssignment(submissionRequest)

                        // Create comments on the submitted assignment
                        submission = commentSeedsList
                                .map {
                                    // Create comments with any assigned upload file types
                                    val commentRequest = CreateCourseAssignmentCommentRequest.newBuilder()
                                            .setAssignmentId(assignmentId)
                                            .setCourseId(courseId)
                                            .setStudentToken(studentToken)

                                    // Grab all valid ids and put them in the list
                                    commentRequest.addAllFileIds(
                                            it.attachmentsList
                                                    .filter { it.id != -1L }
                                                    .map { it.id }
                                    )

                                    InProcessServer.assignmentClient.createCourseAssignmentSubmissionComment(commentRequest.build())
                                }
                                .lastOrNull() ?: submission // Last one (if it exists) will have all the comments loaded up on it

                        // Add file uploads as Attachments to the submission object so we have their data
                        submission = CourseAssignmentSubmission.newBuilder(submission)
                                .addAllAttachments(seed.attachmentsList)
                                .build()

                        // Add submission to our collection
                        submissionsList.add(submission)
                    }
                }
            }

            // Add all submissions to our return object
            val seededAssignments = SeededCourseAssignmentSubmissions.newBuilder().addAllSubmissions(submissionsList)

            onSuccess(responseObserver, seededAssignments.build())
        } catch (e: Exception) {
            onError(responseObserver, e)
        }
    }
}
