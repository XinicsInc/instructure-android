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

package com.instructure.canvasapi2.managers;

import android.support.annotation.Nullable;

import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.apis.InboxApi;
import com.instructure.canvasapi2.builders.RestBuilder;
import com.instructure.canvasapi2.builders.RestParams;
import com.instructure.canvasapi2.models.Conversation;
import com.instructure.canvasapi2.tests.InboxManager_Test;

import java.io.IOException;
import java.util.List;

import retrofit2.Response;


public class InboxManager extends BaseManager {

    private static boolean mTesting = false;

    public static void createConversation(List<String> userIDs, String message, String subject, String contextId, long[] attachmentIds, boolean isBulk, StatusCallback<List<Conversation>> callback) {
        if(isTesting() || mTesting) {
            //todo
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .build();

            InboxManager_Test.createConversation(adapter, params, userIDs, message, subject, contextId, isBulk, callback);

        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .build();

            InboxApi.createConversation(adapter, params, userIDs, message, subject, contextId, attachmentIds, isBulk, callback);
        }
    }

    public static void getConversations(InboxApi.Scope scope, boolean forceNetwork, StatusCallback<List<Conversation>> callback) {

        if(isTesting() || mTesting) {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(true)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();

            InboxManager_Test.getConversations(scope, adapter, callback, params);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(true)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();

            InboxApi.getConversations(scope, adapter, callback, params);
        }
    }

    public static void getConversationsFiltered(InboxApi.Scope scope, String canvasContext, boolean forceNetwork, StatusCallback<List<Conversation>> callback) {

        if(isTesting() || mTesting) {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(true)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();

            InboxManager_Test.getConversationsFiltered(scope, canvasContext, adapter, callback, params);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(true)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();

            InboxApi.getConversationsFiltered(scope, canvasContext, adapter, callback, params);
        }
    }

    public static void getConversation(long conversationId, boolean forceNetwork, StatusCallback<Conversation> callback) {
        if (isTesting() || mTesting) {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();
            InboxManager_Test.getConversation(adapter, callback, params, conversationId);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();
            InboxApi.getConversation(adapter, callback, params, conversationId);
        }
    }

    public static void starConversation(long conversationId, boolean starred, Conversation.WorkflowState workFlowState, StatusCallback<Conversation> callback) {
        if (isTesting() || mTesting) {
            InboxManager_Test.updateConversation(conversationId, null, starred, callback);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withShouldIgnoreToken(false)
                    .withPerPageQueryParam(false)
                    .build();
            InboxApi.updateConversation(adapter, callback, params, conversationId, workFlowState, starred);
        }
    }

    public static void archiveConversation(long conversationId, boolean archive, StatusCallback<Conversation> callback) {
        if (isTesting() || mTesting) {
            InboxManager_Test.updateConversation(conversationId, archive ? Conversation.WorkflowState.ARCHIVED : Conversation.WorkflowState.READ, null, callback);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withShouldIgnoreToken(false)
                    .withPerPageQueryParam(false)
                    .build();
            InboxApi.updateConversation(adapter, callback, params, conversationId, archive ? Conversation.WorkflowState.ARCHIVED : Conversation.WorkflowState.READ, null);
        }
    }

    public static void deleteConversation(long conversationId, StatusCallback<Conversation> callback) {
        if (isTesting() || mTesting) {
            InboxManager_Test.deleteConversation(conversationId, callback);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withShouldIgnoreToken(false)
                    .withPerPageQueryParam(false)
                    .build();
            InboxApi.deleteConversation(adapter, callback, params, conversationId);
        }
    }

    public static void deleteMessages(long conversationId, List<Long> messageIds, StatusCallback<Conversation> callback) {
        if (isTesting() || mTesting) {
            InboxManager_Test.deleteMessages(conversationId, messageIds, callback);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withShouldIgnoreToken(false)
                    .withPerPageQueryParam(false)
                    .build();
            InboxApi.deleteMessages(adapter, callback, params, conversationId, messageIds);
        }
    }

    public static void addMessage(long conversationId, String message, List<String> recipientIds, long[] includedMessageIds, long[] attachmentIds, StatusCallback<Conversation> callback) {
        if (isTesting() || mTesting) {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withShouldIgnoreToken(false)
                    .withPerPageQueryParam(false)
                    .build();
            InboxManager_Test.addMessage(adapter, callback, params, conversationId, recipientIds, message, includedMessageIds, attachmentIds);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withShouldIgnoreToken(false)
                    .withPerPageQueryParam(false)
                    .build();
            InboxApi.addMessage(adapter, callback, params, conversationId, recipientIds, message, includedMessageIds, attachmentIds);
        }
    }

    public static void markConversationAsUnread(long conversationId, String conversationEvent, StatusCallback<Void> callback) {
        if (isTesting() || mTesting) {
            InboxManager_Test.markConversationAsRead(conversationId, conversationEvent, callback);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withShouldIgnoreToken(false)
                    .withPerPageQueryParam(false)
                    .build();
            InboxApi.markConversationAsUnread(adapter, callback, params, conversationId, conversationEvent);
        }
    }

    public static Conversation getConversationSynchronous(long conversationId, boolean forceNetwork) {
        if (isTesting() || mTesting) {
            // TODO
            return new Conversation();
        } else {
            RestBuilder adapter = new RestBuilder();
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();
            try {
                Response<Conversation> response = InboxApi.getConversationSynchronous(adapter, params, conversationId);
                return response.isSuccessful() ? response.body() : null;
            } catch (IOException e) {
                return null;
            }
        }
    }

    public static @Nullable List<Conversation> createConversationWithAttachmentSynchronous(String messageText, String subject, List<String> userIds, String contextId, boolean isGroup, List<Long> attachmentsIds) {
        if (isTesting() || mTesting) {
            // TODO
            return null;
        } else {
            RestBuilder adapter = new RestBuilder();
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .build();
            try {
                return InboxApi.createConversationWithAttachmentSynchronous(adapter, params, userIds, messageText, subject, contextId, isGroup, attachmentsIds);
            } catch (IOException e) {
                return null;
            }
        }
    }

    public static @Nullable Conversation addMessageToConversationSynchronous(long conversationId, String messageText, List<Long> attachmentIds) {
        if (isTesting() || mTesting) {
            // TODO
            return new Conversation();
        } else {
            RestBuilder adapter = new RestBuilder();
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .build();

            try {
                Response<Conversation> response = InboxApi.addMessageToConversationSynchronous(adapter, params, conversationId, messageText, attachmentIds);
                return response.isSuccessful() ? response.body() : null;
            } catch (IOException e) {
                return null;
            }
        }
    }
}
