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
import com.instructure.dataseeding.api.PagesApi
import com.instructure.soseedy.CreateCoursePageRequest
import com.instructure.soseedy.Page
import com.instructure.soseedy.SeedyPagesGrpc.SeedyPagesImplBase
import io.grpc.stub.StreamObserver

class SeedyPagesImpl : SeedyPagesImplBase(), Reaper by SeedyReaper {

    //region API Calls
    private fun createCoursePage(courseId: Long, published: Boolean, frontPage: Boolean, token: String)
            = PagesApi.createCoursePage(courseId, published, frontPage, token)
    //endregion

    override fun createCoursePage(request: CreateCoursePageRequest, responseObserver: StreamObserver<Page>) {
        try {
            val response = createCoursePage(request.courseId, request.published, request.frontPage, request.token)
            val reply = Page.newBuilder()
                    .setUrl(response.url)
                    .setTitle((response.title))
                    .setBody(response.body)
                    .setPublished(response.published)
                    .setFrontPage(response.frontPage)
                    .build()

            onSuccess(responseObserver, reply)
        } catch (e: Exception) {
            onError(responseObserver, e)
        }
    }
}
