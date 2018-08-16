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

package com.instructure.canvasapi2.apis;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.builders.RestBuilder;
import com.instructure.canvasapi2.builders.RestParams;
import com.instructure.canvasapi2.models.Conversation;
import com.instructure.canvasapi2.utils.ApiType;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;


public class InboxApi {

    public static final String CONVERSATION_MARK_UNREAD = "mark_as_unread";

    public enum Scope { ALL, UNREAD, ARCHIVED, STARRED, SENT }

    public static String conversationScopeToString(Scope scope) {
        if (scope == Scope.UNREAD) {
            return "unread";
        } else if (scope == Scope.STARRED) {
            return "starred";
        } else if (scope == Scope.ARCHIVED) {
            return "archived";
        } else if (scope == Scope.SENT) {
            return "sent";
        }
        return "";
    }

    interface InboxInterface {

        @GET("conversations/?interleave_submissions=1&include[]=participant_avatars")
        Call<List<Conversation>> getConversations(@Query("scope") String scope);

        @GET("conversations/?interleave_submissions=1&include[]=participant_avatars")
        Call<List<Conversation>> getConversationsFiltered(@Query("scope") String scope, @Query("filter") String canvasContextFilter);

        @GET
        Call<List<Conversation>> getNextPage(@Url String nextURL);

        @POST("conversations?group_conversation=true")
        Call<List<Conversation>> createConversation(@Query("recipients[]") List<String> recipients, @Query("body") String message, @Query("subject") String subject, @Query("context_code") String contextCode, @Query("attachment_ids[]") long[] attachmentIds, @Query("bulk_message") int isBulk);

        @GET("conversations/{conversationId}?include[]=participant_avatars")
        Call<Conversation> getConversation(@Path("conversationId") long conversationId);

        @PUT("conversations/{conversationId}")
        Call<Conversation> updateConversation(@Path("conversationId") long conversationId, @Query("conversation[workflow_state]") String workflowState, @Query("conversation[starred]") Boolean isStarred);

        @DELETE("conversations/{conversationId}")
        Call<Conversation> deleteConversation(@Path("conversationId") long conversationId);

        @POST("conversations/{conversationId}/remove_messages")
        Call<Conversation> deleteMessages(@Path("conversationId") long conversationId, @Query("remove[]") List<Long> messageIds);

        @POST("conversations/{conversationId}/add_message?group_conversation=true")
        Call<Conversation> addMessage(@Path("conversationId") long conversationId, @Query("recipients[]") List<String> recipientIds, @Query("body") String body, @Query("included_messages[]") long[] includedMessageIds, @Query("attachment_ids[]") long[] attachmentIds);

        @PUT("conversations")
        Call<Void> markConversationAsUnread(@Query("conversation_ids[]") long conversationId, @Query("event") String conversationEvent);

        @GET("conversations/{conversationId}?interleave_submissions=1&include[]=participant_avatars")
        Conversation getConversationSynchronous(@Path("conversationId") long conversationId);

        @POST("conversations/{id}/add_message")
        Call<Conversation> addMessageToConversationSynchronous(@Path("id")long conversationId, @Query("body")String message, @Query("attachment_ids[]") List<Long> attachments);

        @POST("conversations?group_conversation=true")
        Call<List<Conversation>> createConversationWithAttachmentSynchronous(@Query("recipients[]") List<String> recipients, @Query("body") String message, @Query("subject") String subject, @Query("context_code") String contextCode, @Query("bulk_message") int isGroup, @Query("attachment_ids[]") List<Long> attachments);

    }

    public static void getConversation(@NonNull RestBuilder adapter, @NonNull StatusCallback<Conversation> callback, @NonNull RestParams params, long conversationId) {
        callback.addCall(adapter.build(InboxInterface.class, params).getConversation(conversationId)).enqueue(callback);
    }

    public static void getConversations(@NonNull Scope scope, @NonNull RestBuilder adapter, @NonNull StatusCallback<List<Conversation>> callback, @NonNull RestParams params) {
        if (StatusCallback.isFirstPage(callback.getLinkHeaders())) {
            adapter.build(InboxInterface.class, params).getConversations(conversationScopeToString(scope)).enqueue(callback);
        } else if (StatusCallback.moreCallsExist(callback.getLinkHeaders()) && callback.getLinkHeaders() != null) {
            adapter.build(InboxInterface.class, params).getNextPage(callback.getLinkHeaders().nextUrl).enqueue(callback);
        } else {
            callback.onCallbackFinished(ApiType.API);
        }
    }

    public static void getConversationsFiltered(@NonNull Scope scope, @NonNull String canvasContextFilter, @NonNull RestBuilder adapter, @NonNull StatusCallback<List<Conversation>> callback, @NonNull RestParams params) {
        if (StatusCallback.isFirstPage(callback.getLinkHeaders())) {
            adapter.build(InboxInterface.class, params).getConversationsFiltered(conversationScopeToString(scope), canvasContextFilter).enqueue(callback);
        } else if (StatusCallback.moreCallsExist(callback.getLinkHeaders()) && callback.getLinkHeaders() != null) {
            adapter.build(InboxInterface.class, params).getNextPage(callback.getLinkHeaders().nextUrl).enqueue(callback);
        } else {
            callback.onCallbackFinished(ApiType.API);
        }
    }

    public static void createConversation(@NonNull RestBuilder adapter, @NonNull RestParams params, List<String> userIDs, String message, String subject, String contextId, long[] attachmentIds, boolean isBulk, StatusCallback<List<Conversation>> callback) {
        //The message has to be sent to somebody.
        if(userIDs.size() == 0){ return; }
        // "true" has to be hardcoded for group_conversations, see the base url above at the interface.
        // isBulk is what we use to differentiate between sent individually vs as a group
        callback.addCall(adapter.build(InboxInterface.class, params).createConversation(userIDs, message, subject, contextId, attachmentIds, isBulk ? 1 : 0)).enqueue(callback);
    }

    public static void updateConversation(@NonNull RestBuilder adapter, @NonNull StatusCallback<Conversation> callback, @NonNull RestParams params, long conversationId, @Nullable Conversation.WorkflowState workflowState, @Nullable Boolean starred) {
        callback.addCall(adapter.build(InboxInterface.class, params).updateConversation(conversationId, Conversation.getWorkflowStateAPIString(workflowState), starred)).enqueue(callback);
    }

    public static void deleteConversation(@NonNull RestBuilder adapter, @NonNull StatusCallback<Conversation> callback, @NonNull RestParams params, long conversationId) {
        callback.addCall(adapter.build(InboxInterface.class, params).deleteConversation(conversationId)).enqueue(callback);
    }

    public static void deleteMessages(@NonNull RestBuilder adapter, @NonNull StatusCallback<Conversation> callback, @NonNull RestParams params, long conversationId, List<Long> messageIds) {
        callback.addCall(adapter.build(InboxInterface.class, params).deleteMessages(conversationId, messageIds)).enqueue(callback);
    }

    public static void addMessage(@NonNull RestBuilder adapter, @NonNull StatusCallback<Conversation> callback, @NonNull RestParams params, long conversationId, List<String> recipientIds, String message, long[] includedMessageIds, long[] attachmentIds) {
        callback.addCall(adapter.build(InboxInterface.class, params).addMessage(conversationId, recipientIds, message, includedMessageIds, attachmentIds)).enqueue(callback);
    }

    public static void markConversationAsUnread(@NonNull RestBuilder adapter, @NonNull StatusCallback<Void> callback, @NonNull RestParams params, long conversationId, String conversationEvent) {
        callback.addCall(adapter.build(InboxInterface.class, params).markConversationAsUnread(conversationId, conversationEvent)).enqueue(callback);
    }

    public static Response<Conversation> getConversationSynchronous(@NonNull RestBuilder adapter, @NonNull RestParams params, long conversationId) throws IOException {
        return adapter.build(InboxInterface.class, params).getConversation(conversationId).execute();
    }

    public static Response<Conversation> addMessageToConversationSynchronous(@NonNull RestBuilder adapter, @NonNull RestParams params, long conversationId, String messageBody, List<Long> attachmentIds) throws IOException {

        return adapter.build(InboxInterface.class, params).addMessageToConversationSynchronous(conversationId, messageBody, attachmentIds).execute();
    }

    public static @Nullable List<Conversation> createConversationWithAttachmentSynchronous(@NonNull RestBuilder adapter, @NonNull RestParams params, List<String> userIDs, String message, String subject, String contextId, boolean isGroup, List<Long> attachmentIds) throws IOException {
        //The message has to be sent to somebody.
        if(userIDs.isEmpty()) return null;

        try {
            return adapter.build(InboxInterface.class, params).createConversationWithAttachmentSynchronous(userIDs, message, subject, contextId, isGroup ? 0 : 1, attachmentIds).execute().body();
        } catch (Exception e) {
            return null;
        }
    }
}
