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
import com.instructure.dataseeding.api.GroupsApi
import com.instructure.soseedy.*
import com.instructure.soseedy.SeedyGroupsGrpc.SeedyGroupsImplBase
import io.grpc.stub.StreamObserver

class SeedyGroupsImpl : SeedyGroupsImplBase(), Reaper by SeedyReaper {

    //region API Calls
    private fun createCourseGroupCategory(courseId: Long, teacherToken: String) = GroupsApi.createCourseGroupCategory(courseId, teacherToken)

    private fun createGroup(groupCategoryId: Long, teacherToken: String) = GroupsApi.createGroup(groupCategoryId, teacherToken)
    private fun createGroupMembership(groupId: Long, userId: Long, teacherToken: String) = GroupsApi.createGroupMembership(groupId, userId, teacherToken)
    //endregion

    override fun createGroup(request: CreateGroupRequest, responseObserver: StreamObserver<Group>) {
        try {
            val response = createGroup(request.groupCategoryId, request.teacherToken)

            val reply = Group.newBuilder()
                    .setId(response.id)
                    .setName(response.name)
                    .setDescription(response.description)
                    .setContextType(response.contextType)
                    .setCourseId(response.courseId)
                    .setGroupCategoryId(response.groupCategoryId)
                    .build()

            onSuccess(responseObserver, reply)
        } catch (e: Exception) {
            onError(responseObserver, e)
        }
    }

    override fun createGroupMembership(request: CreateGroupMembershipRequest, responseObserver: StreamObserver<GroupMembership>) {
        try {
            val response = createGroupMembership(request.groupId, request.userId, request.teacherToken)

            val reply = GroupMembership.newBuilder()
                    .setId(response.id)
                    .setGroupId(response.groupId)
                    .setUserId(response.userId)
                    .setWorkflowState(response.workflowState)
                    .build()

            onSuccess(responseObserver, reply)
        } catch (e: Exception) {
            onError(responseObserver, e)
        }
    }


    override fun createCourseGroupCategory(request: CreateCourseGroupCategoryRequest, responseObserver: StreamObserver<GroupCategory>) {
        try {
            val response = createCourseGroupCategory(request.courseId, request.teacherToken)

            val reply = GroupCategory.newBuilder()
                    .setId(response.id)
                    .setName(response.name)
                    .setContextType(response.contextType)
                    .build()

            onSuccess(responseObserver, reply)
        } catch (e: Exception) {
            onError(responseObserver, e)
        }
    }


}
