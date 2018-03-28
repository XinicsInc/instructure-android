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
import com.instructure.dataseeding.api.ConversationsApi
import com.instructure.soseedy.Conversation
import com.instructure.soseedy.CreateConversationRequest
import com.instructure.soseedy.SeedyConversationsGrpc.SeedyConversationsImplBase
import io.grpc.stub.StreamObserver

class SeedyConversationsImpl : SeedyConversationsImplBase(), Reaper by SeedyReaper {
    //region API Calls
    private fun createConversation(token: String, recipients: List<String>) = ConversationsApi.createConversation(token, recipients)
    //endregion

    override fun createConversation(request: CreateConversationRequest, responseObserver: StreamObserver<Conversation>) {
        try {
            val conversations = createConversation(request.token, request.recipientsList)

            val reply = Conversation.newBuilder()
                    .setId(conversations[0].id)
                    .setSubject(conversations[0].subject)
                    .build()

            onSuccess(responseObserver, reply)
        } catch (e: Exception) {
            onError(responseObserver, e)
        }
    }

}
