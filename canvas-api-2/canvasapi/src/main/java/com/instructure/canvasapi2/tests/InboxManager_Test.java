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
 *
 */

package com.instructure.canvasapi2.tests;

import android.support.annotation.NonNull;

import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.apis.InboxApi;
import com.instructure.canvasapi2.builders.RestBuilder;
import com.instructure.canvasapi2.builders.RestParams;
import com.instructure.canvasapi2.models.Conversation;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.LinkHeaders;

import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;


public class InboxManager_Test {

    public static void getConversations(
            @NonNull InboxApi.Scope scope,
            @NonNull RestBuilder adapter,
            @NonNull StatusCallback<List<Conversation>> callback,
            @NonNull RestParams params) {

        //TODO:
        Response response = new Response.Builder()
                .code(200)
                .message("todo")
                .protocol(Protocol.HTTP_1_0)
                .body(ResponseBody.create(MediaType.parse("application/json"), "todo".getBytes()))
                .addHeader("content-type", "application/json")
                .build();

        List<Conversation> conversations = new ArrayList<>();

        retrofit2.Response<List<Conversation>> response1 = retrofit2.Response.success(conversations, response);
        callback.onResponse(response1, new LinkHeaders(), ApiType.CACHE);
    }

    public static void getConversationsFiltered(
            @NonNull InboxApi.Scope scope,
            @NonNull String canvasContextFilter,
            @NonNull RestBuilder adapter,
            @NonNull StatusCallback<List<Conversation>> callback,
            @NonNull RestParams params) {

        //TODO:
        Response response = new Response.Builder()
                .code(200)
                .message("todo")
                .protocol(Protocol.HTTP_1_0)
                .body(ResponseBody.create(MediaType.parse("application/json"), "todo".getBytes()))
                .addHeader("content-type", "application/json")
                .build();

        List<Conversation> conversations = new ArrayList<>();

        retrofit2.Response<List<Conversation>> response1 = retrofit2.Response.success(conversations, response);
        callback.onResponse(response1, new LinkHeaders(), ApiType.CACHE);
    }

    public static void getConversation(RestBuilder adapter, StatusCallback<Conversation> callback, RestParams params, long conversationId) {
        //TODO:
        Response response = new Response.Builder()
                .code(200)
                .message("todo")
                .protocol(Protocol.HTTP_1_0)
                .body(ResponseBody.create(MediaType.parse("application/json"), "todo".getBytes()))
                .addHeader("content-type", "application/json")
                .build();

        Conversation conversation = new Conversation();

        retrofit2.Response<Conversation> retrofitResponse = retrofit2.Response.success(conversation, response);
        callback.onResponse(retrofitResponse, new LinkHeaders(), ApiType.CACHE);
    }

    public static void updateConversation(long conversationId, Conversation.WorkflowState workflowState, Boolean starred, StatusCallback<Conversation> callback) {
        // TODO:
        Response rawResponse = new Response.Builder()
                .code(200)
                .message("todo")
                .protocol(Protocol.HTTP_1_0)
                .body(ResponseBody.create(MediaType.parse("application/json"), "todo".getBytes()))
                .addHeader("content-type", "application/json")
                .build();

        Conversation conversation = new Conversation();

        retrofit2.Response<Conversation> response = retrofit2.Response.success(conversation, rawResponse);
        callback.onResponse(response, new LinkHeaders(), ApiType.CACHE);
    }



    public static void deleteConversation(long conversationId, StatusCallback<Conversation> callback) {
        // TODO:
        Response rawResponse = new Response.Builder()
                .code(200)
                .message("todo")
                .protocol(Protocol.HTTP_1_0)
                .body(ResponseBody.create(MediaType.parse("application/json"), "todo".getBytes()))
                .addHeader("content-type", "application/json")
                .build();

        Conversation conversation = new Conversation();

        retrofit2.Response<Conversation> response = retrofit2.Response.success(conversation, rawResponse);
        callback.onResponse(response, new LinkHeaders(), ApiType.CACHE);
    }

    public static void deleteMessages(long conversationId, List<Long> messageIds, StatusCallback<Conversation> callback) {
        // TODO:
        Response rawResponse = new Response.Builder()
                .code(200)
                .message("todo")
                .protocol(Protocol.HTTP_1_0)
                .body(ResponseBody.create(MediaType.parse("application/json"), "todo".getBytes()))
                .addHeader("content-type", "application/json")
                .build();

        Conversation conversation = new Conversation();

        retrofit2.Response<Conversation> response = retrofit2.Response.success(conversation, rawResponse);
        callback.onResponse(response, new LinkHeaders(), ApiType.CACHE);
    }

    public static void createConversation(RestBuilder adapter, RestParams params, List<String> userIDs, String message, String subject, String contextId, boolean isBulk, StatusCallback<List<Conversation>> callback) {
        Response rawResponse = new Response.Builder()
                .code(200)
                .message("todo")
                .protocol(Protocol.HTTP_1_0)
                .body(ResponseBody.create(MediaType.parse("application/json"), "todo".getBytes()))
                .addHeader("content-type", "application/json")
                .build();

        List<Conversation> conversation = new ArrayList<>();

        retrofit2.Response<List<Conversation>> response = retrofit2.Response.success(conversation, rawResponse);
        callback.onResponse(response, new LinkHeaders(), ApiType.CACHE);
    }

    public static void addMessage(RestBuilder adapter, StatusCallback<Conversation> callback, RestParams params, long conversationId, List<String> recipientIds, String message, long[] includedMessageIds, long[] attachmentIds) {
        // TODO:
        Response rawResponse = new Response.Builder()
                .code(200)
                .message("todo")
                .protocol(Protocol.HTTP_1_0)
                .body(ResponseBody.create(MediaType.parse("application/json"), "todo".getBytes()))
                .addHeader("content-type", "application/json")
                .build();

        Conversation conversation = new Conversation();

        retrofit2.Response<Conversation> response = retrofit2.Response.success(conversation, rawResponse);
        callback.onResponse(response, new LinkHeaders(), ApiType.CACHE);
    }

    public static void markConversationAsRead(long conversationId, String conversationEvent, StatusCallback<Void> callback) {
        Response rawResponse = new Response.Builder()
                .code(200)
                .message("todo")
                .protocol(Protocol.HTTP_1_0)
                .body(ResponseBody.create(MediaType.parse("application/json"), "todo".getBytes()))
                .addHeader("content-type", "application/json")
                .build();

        // We don't care about the response
        retrofit2.Response<Void> response = retrofit2.Response.success(null, rawResponse);
        callback.onResponse(response, new LinkHeaders(), ApiType.CACHE);
    }
}
