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

import com.instructure.dataseeding.Reaper
import com.instructure.dataseeding.SeedyReaper
import com.instructure.dataseeding.api.DiscussionTopicsApi
import com.instructure.soseedy.CreateAnnouncementRequest
import com.instructure.soseedy.CreateDiscussionRequest
import com.instructure.soseedy.Discussion
import com.instructure.soseedy.SeedyDiscussionsGrpc.SeedyDiscussionsImplBase
import io.grpc.stub.StreamObserver

class SeedyDiscussionsImpl : SeedyDiscussionsImplBase(), Reaper by SeedyReaper {
    //region API Calls
    private fun createDiscussion(courseId: Long, token: String) = DiscussionTopicsApi.createDiscussion(courseId, token = token)

    private fun createAnnouncement(courseId: Long, token: String) = DiscussionTopicsApi.createAnnouncement(courseId, token)
    //endregion

    override fun createDiscussion(request: CreateDiscussionRequest, responseObserver: StreamObserver<Discussion>) {
        try {
            val discussion = createDiscussion(request.courseId, request.token)

            val reply = Discussion.newBuilder()
                    .setId(discussion.id)
                    .setTitle(discussion.title)
                    .setMessage(discussion.message)
                    .setIsAnnouncement(discussion.isAnnouncement)
                    .build()

            onSuccess(responseObserver, reply)
        } catch (e: Exception) {
            onError(responseObserver, e)
        }
    }

    override fun createAnnouncement(request: CreateAnnouncementRequest, responseObserver: StreamObserver<Discussion>) {
        try {
            val discussion = createAnnouncement(request.courseId, request.token)

            val reply = Discussion.newBuilder()
                    .setId(discussion.id)
                    .setTitle(discussion.title)
                    .setMessage(discussion.message)
                    .setIsAnnouncement(true)
                    .build()

            onSuccess(responseObserver, reply)
        } catch (e: Exception) {
            onError(responseObserver, e)
        }
    }
}
