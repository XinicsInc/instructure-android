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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.apis.DiscussionAPI;
import com.instructure.canvasapi2.builders.RestBuilder;
import com.instructure.canvasapi2.builders.RestParams;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.DiscussionEntry;
import com.instructure.canvasapi2.models.DiscussionTopic;
import com.instructure.canvasapi2.models.DiscussionTopicHeader;
import com.instructure.canvasapi2.models.post_models.DiscussionEntryPostBody;
import com.instructure.canvasapi2.models.post_models.DiscussionTopicPostBody;
import com.instructure.canvasapi2.utils.ExhaustiveListCallback;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

import okhttp3.MultipartBody;
import retrofit2.Response;


public class DiscussionManager extends BaseManager {

    private static boolean mTesting = false;

    public static void getDiscussions(final boolean forceNetwork, CanvasContext canvasContext, StatusCallback<List<DiscussionTopicHeader>> callback) {
        if (isTesting() || mTesting) {
            //TODO:
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .withPerPageQueryParam(true)
                    .build();

            DiscussionAPI.getDiscussions(canvasContext, adapter, callback, params);
        }
    }

    public static boolean markReplyAsReadSynchronously(CanvasContext canvasContext, long topicId) {
        if (isTesting() || mTesting) {
            // TODO
            return true;
        } else {
            RestBuilder adapter = new RestBuilder();
            RestParams params = new RestParams.Builder().build();
            return DiscussionAPI.markReplyAsReadSynchronous(canvasContext, topicId, adapter, params);
        }
    }

    public static void createDiscussion(CanvasContext canvasContext, DiscussionTopicHeader newDiscussionHeader, @Nullable MultipartBody.Part attachment, StatusCallback<DiscussionTopicHeader> callback) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .build();

            DiscussionAPI.createDiscussion(adapter, params, canvasContext, newDiscussionHeader, attachment, callback);
        }
    }

    public static void createStudentDiscussion(CanvasContext canvasContext, DiscussionTopicHeader newDiscussionHeader, @Nullable MultipartBody.Part attachment, StatusCallback<DiscussionTopicHeader> callback) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .build();

            DiscussionAPI.createStudentDiscussion(adapter, params, canvasContext, newDiscussionHeader, attachment, callback);
        }
    }

    public static void editDiscussionTopic(CanvasContext canvasContext, long discussionHeaderId, DiscussionTopicPostBody discussionTopicPostBody, StatusCallback<DiscussionTopicHeader> callback) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .build();

            DiscussionAPI.editDiscussionTopic(canvasContext, discussionHeaderId, discussionTopicPostBody, adapter, callback, params);
        }
    }

    public static void getAllDiscussionTopicHeaders(@NonNull CanvasContext canvasContext, final boolean forceNetwork, StatusCallback<List<DiscussionTopicHeader>> callback) {
        if(isTesting() || mTesting) {
            //TODO:
        } else {
            final RestBuilder adapter = new RestBuilder(callback);
            final RestParams params = new RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .withPerPageQueryParam(true)
                    .build();

            StatusCallback<List<DiscussionTopicHeader>> depaginatedCallback = new ExhaustiveListCallback<DiscussionTopicHeader>(callback) {
                @Override
                public void getNextPage(@NotNull StatusCallback<List<DiscussionTopicHeader>> callback, @NotNull String nextUrl, boolean isCached) {
                    DiscussionAPI.getNextPage(nextUrl, adapter, callback, params);
                }
            };
            adapter.setStatusCallback(depaginatedCallback);
            DiscussionAPI.getFirstPageDiscussionTopicHeaders(canvasContext, adapter, depaginatedCallback, params);
        }
    }

    public static void getFullDiscussionTopic(CanvasContext canvasContext, long topicId, boolean forceNetwork, StatusCallback<DiscussionTopic> callback) {

        if(isTesting() || mTesting) {
            //TODO:
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .build();
            DiscussionAPI.getFullDiscussionTopic(adapter, canvasContext, topicId, callback, params);
        }
    }

    public static void getDetailedDiscussion(CanvasContext canvasContext, long topicId, StatusCallback<DiscussionTopicHeader> callback) {

        if(isTesting() || mTesting) {
            //TODO:
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            DiscussionAPI.getDetailedDiscussion(adapter, canvasContext, topicId, callback, new RestParams.Builder().build());
        }
    }

    public static void rateDiscussionEntry(CanvasContext canvasContext, long topicId, long entryId, int rating, StatusCallback<Void> callback) {
        if(isTesting() || mTesting) {
            //TODO:
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            DiscussionAPI.rateDiscussionEntry(adapter, canvasContext, topicId, entryId, rating, callback, new RestParams.Builder().build());
        }
    }

    @Nullable
    public static Response<Void> rateDiscussionEntrySynchronously(CanvasContext canvasContext, long topicId, long entryId, int rating) {
        if(isTesting() || mTesting) {
            //TODO:
        } else {
            RestBuilder adapter = new RestBuilder();
            return DiscussionAPI.rateDiscussionEntrySynchronously(adapter, canvasContext, topicId, entryId, rating, new RestParams.Builder().build());
        }
        return null;
    }

    public static void markDiscussionTopicEntryRead(CanvasContext canvasContext, long topicId, long entryId, StatusCallback<Void> callback) {
        if(isTesting() || mTesting) {
            //TODO:
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            DiscussionAPI.markDiscussionTopicEntryRead(adapter, canvasContext, topicId, entryId, callback, new RestParams.Builder().build());
        }
    }

    @Nullable
    public static boolean markDiscussionTopicEntryReadSynchronously(CanvasContext canvasContext, long topicId, long entryId) {
        if(isTesting() || mTesting) {
            //TODO:
            return true;
        } else {
            RestBuilder adapter = new RestBuilder();
            return DiscussionAPI.markDiscussionTopicEntryReadSynchronously(adapter, canvasContext, topicId, entryId, new RestParams.Builder().build());
        }
    }

    public static void markAllDiscussionTopicEntriesRead(CanvasContext canvasContext, long topicId, StatusCallback<Void> callback) {
        if(isTesting() || mTesting) {
            //TODO:
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            DiscussionAPI.markAllDiscussionTopicEntriesRead(adapter, canvasContext, topicId, callback, new RestParams.Builder().build());
        }
    }

    @Nullable
    public static Response<Void> markAllDiscussionTopicEntriesReadSynchronously(CanvasContext canvasContext, long topicId) {
        if(isTesting() || mTesting) {
            //TODO:
        } else {
            RestBuilder adapter = new RestBuilder();
            return DiscussionAPI.markAllDiscussionTopicEntriesReadSynchronously(adapter, canvasContext, topicId, new RestParams.Builder().build());
        }
        return null;
    }

    public static void getDiscussionEntries(CanvasContext canvasContext, long topicId, boolean forceNetwork, StatusCallback<List<DiscussionEntry>> callback) {
        if(isTesting() || mTesting) {
            //TODO:
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(true)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();

            DiscussionAPI.getDiscussionEntries(adapter, canvasContext, topicId, callback, params);
        }
    }

    public static void replyToDiscussionEntry(CanvasContext canvasContext, long topicId, long entryId, String message, StatusCallback<DiscussionEntry> callback) {
        if(isTesting() || mTesting) {
            //TODO:
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder().build();

            DiscussionAPI.replyToDiscussionEntry(adapter, canvasContext, topicId, entryId, message, callback, params);
        }
    }

    public static void replyToDiscussionEntry(CanvasContext canvasContext, long topicId, long entryId, String message, File attachment, String mimeType, StatusCallback<DiscussionEntry> callback) {
        if(isTesting() || mTesting) {
            //TODO:
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder().build();

            DiscussionAPI.replyToDiscussionEntryWithAttachment(adapter, canvasContext, topicId, entryId, message, attachment, mimeType, callback, params);
        }
    }

    public static void updateDiscussionEntry(CanvasContext canvasContext, long topicId, long entryId, DiscussionEntryPostBody updatedEntry, StatusCallback<DiscussionEntry> callback) {
        if(isTesting() || mTesting) {
            //TODO:
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder().build();

            DiscussionAPI.updateDiscussionEntry(adapter, canvasContext, topicId, entryId, updatedEntry, callback, params);
        }
    }

    public static void postToDiscussionTopic(CanvasContext canvasContext, long topicId, String message, StatusCallback<DiscussionEntry> callback) {
        if(isTesting() || mTesting) {
            //TODO:
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder().build();

            DiscussionAPI.postToDiscussionTopic(adapter, canvasContext, topicId, message, callback, params);
        }
    }

    public static void postToDiscussionTopic(CanvasContext canvasContext, long topicId, String message, File attachment, String mimeType, StatusCallback<DiscussionEntry> callback) {
        if(isTesting() || mTesting) {
            //TODO:
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder().build();

            DiscussionAPI.postToDiscussionTopicWithAttachment(adapter, canvasContext, topicId, message, attachment, mimeType, callback, params);
        }
    }

    public static void pinDiscussionTopicHeader(@NonNull CanvasContext canvasContext, long topicId, StatusCallback<DiscussionTopicHeader> callback) {
        if(isTesting() || mTesting) {
            //TODO:
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .build();

            DiscussionAPI.pinDiscussion(adapter, canvasContext, topicId, callback, params);
        }
    }

    public static void unpinDiscussionTopicHeader(@NonNull CanvasContext canvasContext, long topicId, StatusCallback<DiscussionTopicHeader> callback) {
        if(isTesting() || mTesting) {
            //TODO:
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .build();

            DiscussionAPI.unpinDiscussion(adapter, canvasContext, topicId, callback, params);
        }
    }

    public static void lockDiscussionTopicHeader(@NonNull CanvasContext canvasContext, long topicId, StatusCallback<DiscussionTopicHeader> callback) {
        if(isTesting() || mTesting) {
            //TODO:
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .build();

            DiscussionAPI.lockDiscussion(adapter, canvasContext, topicId, callback, params);
        }
    }

    public static void unlockDiscussionTopicHeader(@NonNull CanvasContext canvasContext, long topicId, StatusCallback<DiscussionTopicHeader> callback) {
        if(isTesting() || mTesting) {
            //TODO:
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .build();

            DiscussionAPI.unlockDiscussion(adapter, canvasContext, topicId, callback, params);
        }
    }

    public static void deleteDiscussionTopicHeader(@NonNull CanvasContext canvasContext, long topicId, StatusCallback<Void> callback) {
        if(isTesting() || mTesting) {
            //TODO:
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .build();

            DiscussionAPI.deleteDiscussionTopicHeader(adapter, canvasContext, topicId, callback, params);
        }
    }

    public static void deleteDiscussionEntry(@NonNull CanvasContext canvasContext, long topicId, long entryId, StatusCallback<Void> callback) {
        if(isTesting() || mTesting) {
            //TODO:
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .build();

            DiscussionAPI.deleteDiscussionEntry(adapter, canvasContext, topicId, entryId, callback, params);
        }
    }

    public static void getDetailedDiscussionAirwolf(String airwolfDomain, String parentId, String studentId, String courseId, String discussionTopicId, StatusCallback<DiscussionTopicHeader> callback) {

        if(isTesting() || mTesting) {
            //TODO:
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withShouldIgnoreToken(false)
                    .withPerPageQueryParam(false)
                    .withDomain(airwolfDomain)
                    .withAPIVersion("")
                    .build();

            DiscussionAPI.getDetailedDiscussionAirwolf(adapter, parentId, studentId, courseId, discussionTopicId, callback, params);
        }
    }

    public static void getFirstPagePinnedDiscussions(@NonNull CanvasContext canvasContext, final boolean forceNetwork, StatusCallback<List<DiscussionTopicHeader>> callback) {
        if(isTesting() || mTesting) {
            //TODO:
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .withPerPageQueryParam(true)
                    .build();

            DiscussionAPI.getFirstPagePinnedDiscussions(canvasContext, adapter, callback, params);
        }
    }

    public static void getAllPinnedDiscussions(@NonNull CanvasContext canvasContext, final boolean forceNetwork, StatusCallback<List<DiscussionTopicHeader>> callback) {
        if(isTesting() || mTesting) {
            //TODO:
        } else {
            final RestBuilder adapter = new RestBuilder(callback);
            final RestParams params = new RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .withPerPageQueryParam(false)
                    .build();

            StatusCallback<List<DiscussionTopicHeader>> depaginatedCallback = new ExhaustiveListCallback<DiscussionTopicHeader>(callback) {
                @Override
                public void getNextPage(@NotNull StatusCallback<List<DiscussionTopicHeader>> callback, @NotNull String nextUrl, boolean isCached) {
                    DiscussionAPI.getNextPage(nextUrl, adapter, callback, params);
                }
            };
            adapter.setStatusCallback(depaginatedCallback);
            DiscussionAPI.getFirstPagePinnedDiscussions(canvasContext, adapter, depaginatedCallback, params);
        }
    }

    public static void getStudentGroupDiscussionTopicHeaderExhaustive(@NonNull CanvasContext canvasContext, final long rootTopicId, final boolean forceNetwork, StatusCallback<List<DiscussionTopicHeader>> callback) {
        if(isTesting() || mTesting) {
            //TODO:
        } else {
            final RestBuilder adapter = new RestBuilder(callback);
            final RestParams params = new RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .withPerPageQueryParam(true)
                    .build();

            StatusCallback<List<DiscussionTopicHeader>> depaginatedCallback = new ExhaustiveListCallback<DiscussionTopicHeader>(callback) {
                @Override
                public void getNextPage(@NotNull StatusCallback<List<DiscussionTopicHeader>> callback, @NotNull String nextUrl, boolean isCached) {
                    DiscussionAPI.getNextPage(nextUrl, adapter, callback, params);
                }
            };
            adapter.setStatusCallback(depaginatedCallback);
            DiscussionAPI.getFirstPageStudentGroupDiscussionTopicHeader(canvasContext, rootTopicId, depaginatedCallback, adapter, params);
        }
    }

    public static void getFilteredDiscussionTopic(final boolean forceNetwork, @NonNull CanvasContext canvasContext, @NonNull String searchTerm, StatusCallback<List<DiscussionTopicHeader>> callback) {
        if(isTesting() || mTesting) {
            //TODO:
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .withPerPageQueryParam(true)
                    .build();

            DiscussionAPI.getFilteredDiscussionTopic(canvasContext, searchTerm, callback, adapter, params);
        }
    }

    public static void updateDiscussionTopic(CanvasContext canvasContext, long topicId, @NonNull String title, @NonNull String message, boolean threaded, boolean isPublished, StatusCallback<DiscussionTopicHeader> callback) {
        if(isTesting() || mTesting) {
            //TODO:
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .build();

            DiscussionAPI.updateDiscussionTopic(adapter, canvasContext, topicId, title, message, threaded, isPublished, callback, params);
        }
    }

    public static void createDiscussion(@NonNull CanvasContext canvasContext, @NonNull String title, @NonNull String message, boolean isThreaded, boolean isAnnouncement, boolean isPublished, StatusCallback<DiscussionTopicHeader> callback) {
        if(isTesting() || mTesting) {
            //TODO:
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .build();

            DiscussionAPI.createDiscussion(adapter, params, canvasContext, title, message, isThreaded, isAnnouncement, isPublished, callback);
        }
    }
}
