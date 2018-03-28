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
import com.instructure.dataseeding.api.GradingPeriodsApi
import com.instructure.soseedy.CreateGradingPeriodRequest
import com.instructure.soseedy.CreateGradingPeriodSetRequest
import com.instructure.soseedy.GradingPeriod
import com.instructure.soseedy.GradingPeriodSet
import com.instructure.soseedy.SeedyGradingPeriodsGrpc.SeedyGradingPeriodsImplBase
import io.grpc.stub.StreamObserver

class SeedyGradingPeriodsImpl : SeedyGradingPeriodsImplBase(), Reaper by SeedyReaper {

    //region API Calls
    private fun createGradingPeriodSet(enrollmentTermId: Long) =
            GradingPeriodsApi.createGradingPeriodSet(enrollmentTermId)

    private fun createGradingPeriod(gradingPeriodSetId: Long) =
            GradingPeriodsApi.createGradingPeriod(gradingPeriodSetId)
    //endregion

    override fun createGradingPeriodSet(request: CreateGradingPeriodSetRequest, responseObserver: StreamObserver<GradingPeriodSet>) {
        try {
            val gradingPeriodSet = createGradingPeriodSet(request.enrollmentId)

            val reply = GradingPeriodSet.newBuilder()
                    .setId(gradingPeriodSet.gradingPeriodSet.id)
                    .build()

            onSuccess(responseObserver, reply)
        } catch (e: Exception) {
            onError(responseObserver, e)
        }
    }

    override fun createGradingPeriod(request: CreateGradingPeriodRequest, responseObserver: StreamObserver<GradingPeriod>) {
        try {
            val gradingPeriod = createGradingPeriod(request.gradingPeriodSetId)

            val reply = GradingPeriod.newBuilder()
                    .setId(gradingPeriod.gradingPeriods[0].id)
                    .build()

            onSuccess(responseObserver, reply)
        } catch (e: Exception) {
            onError(responseObserver, e)
        }
    }
}
