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
import com.instructure.dataseeding.api.FeatureFlagsApi
import com.instructure.soseedy.FeatureFlag
import com.instructure.soseedy.SeedyFeatureFlagsGrpc.SeedyFeatureFlagsImplBase
import com.instructure.soseedy.SetAccountFeatureFlagRequest
import io.grpc.stub.StreamObserver

class SeedyFeatureFlagsImpl : SeedyFeatureFlagsImplBase(), Reaper by SeedyReaper {
    // region API Calls
    private fun setAccountFeatureFlag(feature: String, state: String) =
            FeatureFlagsApi.setAccountFeatureFlag(feature, state)

    // endregion

    override fun setAccountFeatureFlag(request: SetAccountFeatureFlagRequest, responseObserver: StreamObserver<FeatureFlag>) {
        try {
            val featureFlag = setAccountFeatureFlag(request.feature, request.state)
            val reply = FeatureFlag.newBuilder()
                    .setFeature(featureFlag.feature)
                    .setState(featureFlag.state)
                    .setContextType(featureFlag.contextType)
                    .build()

            onSuccess(responseObserver, reply)

        } catch (e: Exception) {
            onError(responseObserver, e)
        }
    }
}
