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
import com.instructure.dataseeding.api.SectionsApi
import com.instructure.soseedy.CreateSectionRequest
import com.instructure.soseedy.Section
import com.instructure.soseedy.SeedySectionsGrpc.SeedySectionsImplBase
import io.grpc.stub.StreamObserver

class SeedySectionsImpl : SeedySectionsImplBase(), Reaper by SeedyReaper {
    //region API Calls
    private fun createSection(courseId: Long) = SectionsApi.createSection(courseId)
    //endregion

    override fun createSection(request: CreateSectionRequest, responseObserver: StreamObserver<Section>) {
        try {
            val response = createSection(request.courseId)
            val reply = Section.newBuilder()
                    .setId(response.id)
                    .setCourseId(response.courseId)
                    .setName(response.name)
                    .build()

            onSuccess(responseObserver, reply)
        } catch (e: Exception) {
            onError(responseObserver, e)
        }
    }
}
