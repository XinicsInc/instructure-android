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
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.DiscussionEntry;
import com.instructure.canvasapi2.models.DiscussionTopic;
import com.instructure.canvasapi2.models.DiscussionTopicHeader;
import com.instructure.canvasapi2.models.post_models.DiscussionEntryPostBody;
import com.instructure.canvasapi2.models.post_models.DiscussionTopicPostBody;
import com.instructure.canvasapi2.utils.APIHelper;

import java.io.File;
import java.io.IOException;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;

public class DiscussionAPI {

    interface DiscussionInterface {

        @PUT("{contextType}/{contextId}/discussion_topics/{topicId}/read")
        Call<Void> markDiscussionReplyAsRead(
                @Path("contextType") String contextType,
                @Path("contextId") long contextId,
                @Path("topicId") long topicId);

        @Multipart
        @POST("{contextType}/{contextId}/discussion_topics")
        Call<DiscussionTopicHeader> createCourseDiscussion(
                @Path("contextType") String contextType,
                @Path("contextId") long contextId,
                @Part("title") RequestBody title,
                @Part("message") RequestBody message,
                @Part("is_announcement") boolean isAnnouncement,
                @Part("delayed_post_at") RequestBody delayedPostAt,
                @Part("published") boolean isPublished,
                @Part("discussion_type") RequestBody discussionType,
                @Part("require_initial_post") boolean isUsersMustPost,
                @Part("lock_at") RequestBody lockAt,
                @Part MultipartBody.Part attachment);

        @Multipart
        @POST("{contextType}/{contextId}/discussion_topics")
        Call<DiscussionTopicHeader> createStudentCourseDiscussion(
                @Path("contextType") String contextType,
                @Path("contextId") long contextId,
                @Part("title") RequestBody title,
                @Part("message") RequestBody message,
                @Part("is_announcement") boolean isAnnouncement,
                @Part("delayed_post_at") RequestBody delayedPostAt,
                @Part("discussion_type") RequestBody discussionType,
                @Part("require_initial_post") boolean isUsersMustPost,
                @Part("lock_at") RequestBody lockAt,
                @Part MultipartBody.Part attachment);

        @POST("{contextType}/{contextId}/discussion_topics/")
        Call<DiscussionTopicHeader> createNewDiscussion(@Path("contextType") String contextType, @Path("contextId") long courseId, @Query("title") String title, @Query("message")String message, @Query("is_announcement")int announcement, @Query("published") int published, @Query("discussion_type")String discussionType);

        @GET("{contextType}/{contextId}/discussion_topics?override_assignment_dates=true&include[]=all_dates&include[]=overrides")
        Call<List<DiscussionTopicHeader>> getFirstPageDiscussionTopicHeaders(@Path("contextType") String contextType, @Path("contextId") long contextId);

        @GET("{contextType}/{contextId}/discussion_topics?scope=pinned")
        Call<List<DiscussionTopicHeader>> getFirstPagePinnedDiscussions(@Path("contextType") String contextType, @Path("contextId") long courseId);

        @GET("{contextType}/{contextId}/discussion_topics")
        Call<List<DiscussionTopicHeader>> getFirstPageStudentGroupDiscussionTopicHeader(@Path("contextType") String contextType, @Path("contextId") long courseId, @Query("root_topic_id") long rootTopicId);

        @GET("{contextType}/{contextId}/discussion_topics")
        Call<List<DiscussionTopicHeader>> getFilteredDiscussionTopic(@Path("contextType") String contextType, @Path("contextId") long courseId, @Query("search_term") String searchTerm);

        @GET("{contextType}/{contextId}/discussion_topics/{topicId}")
        Call<DiscussionTopicHeader> getDetailedDiscussion(@Path("contextType") String contextType, @Path("contextId") long contextId, @Path("topicId") long topicId);

        @GET("{contextType}/{contextId}/discussion_topics/{topicId}/entries")
        Call<List<DiscussionEntry>> getDiscussionEntries(@Path("contextType") String contextType, @Path("contextId") long contextId, @Path("topicId") long topicId);

        @GET("{contextType}/{contextId}/discussion_topics/{topicId}/view")
        Call<DiscussionTopic> getFullDiscussionTopic(@Path("contextType") String contextType, @Path("contextId") long contextId, @Path("topicId") long topicId, @Query("include_new_entries") int includeNewEntries);

        @POST("{contextType}/{contextId}/discussion_topics/{topicId}/entries/{entryId}/rating")
        Call<Void> rateDiscussionEntry(@Path("contextType") String contextType, @Path("contextId") long contextId, @Path("topicId") long topicId, @Path("entryId") long entryId, @Query("rating") int rating);

        @PUT("{contextType}/{contextId}/discussion_topics/{topicId}/read_all")
        Call<Void> markDiscussionTopicEntriesRead(@Path("contextType") String contextType, @Path("contextId") long contextId, @Path("topicId") long topicId);

        @PUT("{contextType}/{contextId}/discussion_topics/{topicId}/entries/{entryId}/read")
        Call<Void> markDiscussionTopicEntryRead(@Path("contextType") String contextType, @Path("contextId") long contextId, @Path("topicId") long topicId, @Path("entryId") long entryId);

        @PUT("{contextType}/{contextId}/discussion_topics/{topicId}")
        Call<DiscussionTopicHeader> pinDiscussion(@Path("contextType") String contextType, @Path("contextId") long courseId, @Path("topicId") long topicId, @Query("pinned") boolean pinned, @Body String body);

        @PUT("{contextType}/{contextId}/discussion_topics/{topicId}")
        Call<DiscussionTopicHeader> lockDiscussion(@Path("contextType") String contextType, @Path("contextId") long courseId, @Path("topicId") long topicId, @Query("locked") boolean locked, @Body String body);

        @DELETE("{contextType}/{contextId}/discussion_topics/{topicId}")
        Call<Void> deleteDiscussionTopic(@Path("contextType") String contextType, @Path("contextId") long courseId, @Path("topicId") long topicId);

        @DELETE("{contextType}/{contextId}/discussion_topics/{topicId}/entries/{entryId}")
        Call<Void> deleteDiscussionEntry(@Path("contextType") String contextType, @Path("contextId") long courseId, @Path("topicId") long topicId, @Path("entryId") long entryId);

        @Multipart
        @POST("{contextType}/{contextId}/discussion_topics/{topicId}/entries/{entryId}/replies")
        Call<DiscussionEntry> postDiscussionReply(@Path("contextType") String contextType, @Path("contextId") long contextId, @Path("topicId") long topicId, @Path("entryId") long entryId, @Part("message") RequestBody message);

        @Multipart
        @POST("{contextType}/{contextId}/discussion_topics/{topicId}/entries/{entryId}/replies")
        Call<DiscussionEntry> postDiscussionReplyWithAttachment(@Path("contextType") String contextType, @Path("contextId") long contextId,
                                                                @Path("topicId") long topicId, @Path("entryId") long entryId,
                                                                @Part("message") RequestBody message, @Part MultipartBody.Part attachment);

        @Multipart
        @POST("{contextType}/{contextId}/discussion_topics/{topicId}/entries")
        Call<DiscussionEntry> postDiscussionEntry(@Path("contextType") String contextType, @Path("contextId") long contextId, @Path("topicId") long topicId, @Part("message") RequestBody message);

        @Multipart
        @POST("{contextType}/{contextId}/discussion_topics/{topicId}/entries")
        Call<DiscussionEntry> postDiscussionEntryWithAttachment(@Path("contextType") String contextType, @Path("contextId") long contextId,
                                                                @Path("topicId") long topicId, @Part("message") RequestBody message, @Part MultipartBody.Part attachment);

        @GET
        Call<List<DiscussionTopicHeader>> getNextPage(@Url String nextUrl);

        @GET
        Call<List<DiscussionEntry>> getNextPageEntries(@Url String nextUrl);

        @PUT("{contextType}/{contextId}/discussion_topics/{topicId}/entries/{entryId}")
        Call<DiscussionEntry> updateDiscussionEntry(@Path("contextType") String contextType, @Path("contextId") long contextId, @Path("topicId") long topicId, @Path("entryId") long entryId, @Body DiscussionEntryPostBody entry);

        @PUT("{contextType}/{contextId}/discussion_topics/{topicId}")
        Call<DiscussionTopicHeader> updateDiscussionTopic(@Path("contextType") String contextType, @Path("contextId") long contextId, @Path("topicId") long topicId, @Query("title") String title, @Query("message")String message, @Query("published") int isPublished, @Query("discussionType")String discussionType);

        @PUT("{contextType}/{contextId}/discussion_topics/{topicId}")
        Call<DiscussionTopicHeader> editDiscussionTopic(
                @Path("contextType") String contextType,
                @Path("contextId") long contextId,
                @Path("topicId") long topicId,
                @Body DiscussionTopicPostBody body);

        //region Airwolf

        @GET("canvas/{parentId}/{studentId}/courses/{courseId}/discussion_topics/{discussionTopicId}")
        Call<DiscussionTopicHeader> getDetailedDiscussionAirwolf(@Path("parentId") String parentId, @Path("studentId") String studentId, @Path("courseId") String courseId, @Path("discussionTopicId") String discussionTopicId);

        //endregion
    }

    public static boolean markReplyAsReadSynchronous(
            @NonNull CanvasContext canvasContext,
            long topicId,
            @NonNull RestBuilder adapter,
            @NonNull RestParams params) {
        Call<Void> call = adapter
                .build(DiscussionInterface.class, params)
                .markDiscussionReplyAsRead(CanvasContext.getApiContext(canvasContext), canvasContext.getId(), topicId);
        try {
            Response response = call.execute();
            return response.isSuccessful();
        } catch (IOException e) {
            return false;
        }
    }



    public static void createDiscussion(@NonNull RestBuilder adapter,
                                        @NonNull RestParams params,
                                        CanvasContext canvasContext,
                                        DiscussionTopicHeader newDiscussionHeader,
                                        @Nullable MultipartBody.Part attachment,
                                        StatusCallback<DiscussionTopicHeader> callback) {
        callback.addCall(adapter.build(DiscussionInterface.class, params)
                .createCourseDiscussion(
                        CanvasContext.getApiContext(canvasContext),
                        canvasContext.getId(),
                        APIHelper.makeRequestBody(newDiscussionHeader.getTitle()),
                        APIHelper.makeRequestBody(newDiscussionHeader.getMessage()),
                        newDiscussionHeader.isAnnouncement(),
                        newDiscussionHeader.getDelayedPostAt() == null ? null : APIHelper.makeRequestBody(newDiscussionHeader.getDelayedPostAt().toString()),
                        newDiscussionHeader.isPublished(),
                        APIHelper.makeRequestBody(newDiscussionHeader.getDiscussionType()),
                        newDiscussionHeader.isRequireInitialPost(),
                        newDiscussionHeader.getLockAt() == null ? null : APIHelper.makeRequestBody(newDiscussionHeader.getLockAt().toString()),
                        attachment
                )).enqueue(callback);

    }

    public static void createStudentDiscussion(@NonNull RestBuilder adapter,
                                        @NonNull RestParams params,
                                        CanvasContext canvasContext,
                                        DiscussionTopicHeader newDiscussionHeader,
                                        @Nullable MultipartBody.Part attachment,
                                        StatusCallback<DiscussionTopicHeader> callback) {
        callback.addCall(adapter.build(DiscussionInterface.class, params)
                .createStudentCourseDiscussion(
                        CanvasContext.getApiContext(canvasContext),
                        canvasContext.getId(),
                        APIHelper.makeRequestBody(newDiscussionHeader.getTitle()),
                        APIHelper.makeRequestBody(newDiscussionHeader.getMessage()),
                        newDiscussionHeader.isAnnouncement(),
                        newDiscussionHeader.getDelayedPostAt() == null ? null : APIHelper.makeRequestBody(newDiscussionHeader.getDelayedPostAt().toString()),
                        APIHelper.makeRequestBody(newDiscussionHeader.getDiscussionType()),
                        newDiscussionHeader.isRequireInitialPost(),
                        newDiscussionHeader.getLockAt() == null ? null : APIHelper.makeRequestBody(newDiscussionHeader.getLockAt().toString()),
                        attachment
                )).enqueue(callback);

    }

    public static void createDiscussion(@NonNull RestBuilder adapter,
                                           @NonNull RestParams params,
                                           CanvasContext canvasContext,
                                           @NonNull String title,
                                           @NonNull String message,
                                           boolean isThreaded,
                                           boolean isAnnouncement,
                                           boolean isPublished,
                                           StatusCallback<DiscussionTopicHeader> callback) {
        String type = "";
        if (isThreaded)
            type = "threaded";
        else
            type = "side_comment";
        final String contextType = CanvasContext.getApiContext(canvasContext);
        int announcement = APIHelper.booleanToInt(isAnnouncement);
        int publish = APIHelper.booleanToInt(isPublished);
        callback.addCall(adapter.build(DiscussionInterface.class, params).createNewDiscussion(contextType, canvasContext.getId(), title, message, announcement, publish, type)).enqueue(callback);
    }


    public static void getDiscussions(@NonNull CanvasContext canvasContext, @NonNull RestBuilder adapter, @NonNull StatusCallback<List<DiscussionTopicHeader>> callback, @NonNull RestParams params) {
        if (StatusCallback.isFirstPage(callback.getLinkHeaders())) {
            final String contextType = CanvasContext.getApiContext(canvasContext);
            callback.addCall(adapter.build(DiscussionInterface.class, params).getFirstPageDiscussionTopicHeaders(contextType, canvasContext.getId())).enqueue(callback);
        } else if (StatusCallback.moreCallsExist(callback.getLinkHeaders()) && callback.getLinkHeaders() != null) {
            callback.addCall(adapter.build(DiscussionInterface.class, params).getNextPage(callback.getLinkHeaders().nextUrl)).enqueue(callback);
        }
    }

    public static void getFirstPageDiscussionTopicHeaders(@NonNull CanvasContext canvasContext, @NonNull RestBuilder adapter, @NonNull StatusCallback<List<DiscussionTopicHeader>> callback, @NonNull RestParams params) {
        final String contextType = CanvasContext.getApiContext(canvasContext);
        callback.addCall(adapter.build(DiscussionInterface.class, params).getFirstPageDiscussionTopicHeaders(contextType, canvasContext.getId())).enqueue(callback);
    }

    public static void getFirstPagePinnedDiscussions(@NonNull CanvasContext canvasContext, @NonNull RestBuilder adapter, @NonNull StatusCallback<List<DiscussionTopicHeader>> callback, @NonNull RestParams params) {
        final String contextType = CanvasContext.getApiContext(canvasContext);
        callback.addCall(adapter.build(DiscussionInterface.class, params).getFirstPagePinnedDiscussions(contextType, canvasContext.getId())).enqueue(callback);
    }

    public static void getFirstPageStudentGroupDiscussionTopicHeader(@NonNull CanvasContext canvasContext, long rootTopicId, @NonNull StatusCallback<List<DiscussionTopicHeader>> callback, @NonNull RestBuilder adapter, @NonNull RestParams params) {
        final String contextType = CanvasContext.getApiContext(canvasContext);
        callback.addCall(adapter.build(DiscussionInterface.class, params).getFirstPageStudentGroupDiscussionTopicHeader(contextType, canvasContext.getId(), rootTopicId)).enqueue(callback);
    }

    public static void getFilteredDiscussionTopic(@NonNull CanvasContext canvasContext, @NonNull String searchTerm, @NonNull StatusCallback<List<DiscussionTopicHeader>> callback, @NonNull RestBuilder adapter, @NonNull RestParams params) {
        final String contextType = CanvasContext.getApiContext(canvasContext);
        callback.addCall(adapter.build(DiscussionInterface.class, params).getFilteredDiscussionTopic(contextType, canvasContext.getId(), searchTerm)).enqueue(callback);
    }

    public static void getNextPage(String nextUrl, @NonNull RestBuilder adapter, @NonNull StatusCallback<List<DiscussionTopicHeader>> callback, @NonNull RestParams params) {
        callback.addCall(adapter.build(DiscussionInterface.class, params).getNextPage(nextUrl)).enqueue(callback);
    }

    public static void getFullDiscussionTopic(@NonNull RestBuilder adapter, CanvasContext canvasContext, long topicId, StatusCallback<DiscussionTopic> callback, @NonNull RestParams params) {
        final String contextType = CanvasContext.getApiContext(canvasContext);
        callback.addCall(adapter.build(DiscussionInterface.class, params).getFullDiscussionTopic(contextType, canvasContext.getId(), topicId, 1)).enqueue(callback);
    }

    public static void getDetailedDiscussion(@NonNull RestBuilder adapter, CanvasContext canvasContext, long topicId, StatusCallback<DiscussionTopicHeader> callback, @NonNull RestParams params) {
        final String contextType = CanvasContext.getApiContext(canvasContext);
        callback.addCall(adapter.build(DiscussionInterface.class, params).getDetailedDiscussion(contextType, canvasContext.getId(), topicId)).enqueue(callback);
    }

    public static void replyToDiscussionEntry(@NonNull RestBuilder adapter, CanvasContext canvasContext, long topicId, long entryId, String message, StatusCallback<DiscussionEntry> callback, @NonNull RestParams params) {
        final String contextType = CanvasContext.getApiContext(canvasContext);

        RequestBody messagePart = RequestBody.create(MediaType.parse("multipart/form-data"), message);
        callback.addCall(adapter.build(DiscussionInterface.class, params).postDiscussionReply(contextType, canvasContext.getId(), topicId, entryId, messagePart)).enqueue(callback);
    }

    public static void replyToDiscussionEntryWithAttachment(@NonNull RestBuilder adapter, CanvasContext canvasContext, long topicId, long entryId,
                                                            String message, File attachment, String mimeType, StatusCallback<DiscussionEntry> callback, @NonNull RestParams params) {
        final String contextType = CanvasContext.getApiContext(canvasContext);

        RequestBody messagePart = RequestBody.create(MediaType.parse("multipart/form-data"), message);

        RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), attachment);
        MultipartBody.Part attachmentPart = MultipartBody.Part.createFormData("attachment", attachment.getName(), requestFile);

        callback.addCall(adapter.build(DiscussionInterface.class, params).postDiscussionReplyWithAttachment(contextType, canvasContext.getId(), topicId, entryId, messagePart, attachmentPart)).enqueue(callback);
    }

    public static void updateDiscussionEntry(@NonNull RestBuilder adapter, CanvasContext canvasContext, long topicId, long entryId, DiscussionEntryPostBody updatedEntry, StatusCallback<DiscussionEntry> callback, @NonNull RestParams params) {
        final String contextType = CanvasContext.getApiContext(canvasContext);

        callback.addCall(adapter.buildSerializeNulls(DiscussionInterface.class, params).updateDiscussionEntry(contextType, canvasContext.getId(), topicId, entryId, updatedEntry)).enqueue(callback);
    }

    public static void updateDiscussionTopic(@NonNull RestBuilder adapter, CanvasContext canvasContext, long topicId, String title, String message, boolean threaded, boolean isPublished, StatusCallback<DiscussionTopicHeader> callback, @NonNull RestParams params) {
        final String contextType = CanvasContext.getApiContext(canvasContext);

        String type = "";
        if (threaded)
            type = "threaded";
        else
            type = "side_comment";


        callback.addCall(adapter.buildSerializeNulls(DiscussionInterface.class, params).updateDiscussionTopic(contextType, canvasContext.getId(), topicId, title, message, APIHelper.booleanToInt(isPublished), type)).enqueue(callback);
    }

    public static void postToDiscussionTopic(@NonNull RestBuilder adapter, CanvasContext canvasContext, long topicId, String message, StatusCallback<DiscussionEntry> callback, @NonNull RestParams params) {
        final String contextType = CanvasContext.getApiContext(canvasContext);

        RequestBody messagePart = RequestBody.create(MediaType.parse("multipart/form-data"), message);
        callback.addCall(adapter.build(DiscussionInterface.class, params).postDiscussionEntry(contextType, canvasContext.getId(), topicId, messagePart)).enqueue(callback);
    }

    public static void postToDiscussionTopicWithAttachment(@NonNull RestBuilder adapter, CanvasContext canvasContext, long topicId, String message, File attachment, String mimeType, StatusCallback<DiscussionEntry> callback, @NonNull RestParams params) {
        final String contextType = CanvasContext.getApiContext(canvasContext);

        RequestBody messagePart = RequestBody.create(MediaType.parse("multipart/form-data"), message);

        RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), attachment);
        MultipartBody.Part attachmentPart = MultipartBody.Part.createFormData("attachment", attachment.getName(), requestFile);

        callback.addCall(adapter.build(DiscussionInterface.class, params).postDiscussionEntryWithAttachment(contextType, canvasContext.getId(), topicId, messagePart, attachmentPart)).enqueue(callback);
    }

    public static void rateDiscussionEntry(@NonNull RestBuilder adapter, CanvasContext canvasContext, long topicId, long entryId, int rating, StatusCallback<Void> callback, @NonNull RestParams params) {
        final String contextType = CanvasContext.getApiContext(canvasContext);
        callback.addCall(adapter.build(DiscussionInterface.class, params).rateDiscussionEntry(contextType, canvasContext.getId(), topicId, entryId, rating)).enqueue(callback);
    }

    public static Response<Void> rateDiscussionEntrySynchronously(@NonNull RestBuilder adapter, CanvasContext canvasContext, long topicId, long entryId, int rating, @NonNull RestParams params) {
        final String contextType = CanvasContext.getApiContext(canvasContext);
        try {
            return adapter.build(DiscussionInterface.class, params).rateDiscussionEntry(contextType, canvasContext.getId(), topicId, entryId, rating).execute();
        } catch (IOException e) {
            return null;
        }
    }

    public static void markAllDiscussionTopicEntriesRead(@NonNull RestBuilder adapter, @NonNull CanvasContext canvasContext, long topicId, StatusCallback<Void> callback, @NonNull RestParams params) {
        final String contextType = CanvasContext.getApiContext(canvasContext);
        callback.addCall(adapter.build(DiscussionInterface.class, params).markDiscussionTopicEntriesRead(contextType, canvasContext.getId(), topicId)).enqueue(callback);
    }

    @Nullable
    public static Response<Void> markAllDiscussionTopicEntriesReadSynchronously(@NonNull RestBuilder adapter, @NonNull CanvasContext canvasContext, long topicId, @NonNull RestParams params) {
        final String contextType = CanvasContext.getApiContext(canvasContext);
        try {
            return adapter.build(DiscussionInterface.class, params).markDiscussionTopicEntriesRead(contextType, canvasContext.getId(), topicId).execute();
        } catch (IOException e) {
            return null;
        }
    }

    public static void markDiscussionTopicEntryRead(@NonNull RestBuilder adapter, @NonNull CanvasContext canvasContext, long topicId, long entryId, StatusCallback<Void> callback, @NonNull RestParams params) {
        final String contextType = CanvasContext.getApiContext(canvasContext);
        callback.addCall(adapter.build(DiscussionInterface.class, params).markDiscussionTopicEntryRead(contextType, canvasContext.getId(), topicId, entryId)).enqueue(callback);
    }

    @Nullable
    public static boolean markDiscussionTopicEntryReadSynchronously(@NonNull RestBuilder adapter, @NonNull CanvasContext canvasContext, long topicId, long entryId, @NonNull RestParams params) {
        final String contextType = CanvasContext.getApiContext(canvasContext);
        try {
            Response<Void> response = adapter.build(DiscussionInterface.class, params).markDiscussionTopicEntryRead(contextType, canvasContext.getId(), topicId, entryId).execute();
            return response.isSuccessful();
        } catch (IOException e) {
            return false;
        }
    }

    public static void getDiscussionEntries(@NonNull RestBuilder adapter, CanvasContext canvasContext, long topicId, StatusCallback<List<DiscussionEntry>> callback, @NonNull RestParams params) {
        final String contextType = CanvasContext.getApiContext(canvasContext);

        if (StatusCallback.isFirstPage(callback.getLinkHeaders())) {
            callback.addCall(adapter.build(DiscussionInterface.class, params).getDiscussionEntries(contextType, canvasContext.getId(), topicId)).enqueue(callback);
        } else if (StatusCallback.moreCallsExist(callback.getLinkHeaders()) && callback.getLinkHeaders() != null) {
            callback.addCall(adapter.build(DiscussionInterface.class, params).getNextPageEntries(callback.getLinkHeaders().nextUrl)).enqueue(callback);
        }
    }

    public static void getDetailedDiscussionAirwolf(
            @NonNull RestBuilder adapter,
            @NonNull String parentId,
            @NonNull String studentId,
            @NonNull String courseId,
            @NonNull String discussionTopicId,
            @NonNull StatusCallback<DiscussionTopicHeader> callback,
            @NonNull RestParams params) {

        callback.addCall(adapter.build(DiscussionInterface.class, params).getDetailedDiscussionAirwolf(parentId, studentId, courseId, discussionTopicId)).enqueue(callback);
    }

    public static void pinDiscussion(@NonNull RestBuilder adapter, CanvasContext canvasContext, long topicId, StatusCallback<DiscussionTopicHeader> callback, @NonNull RestParams params) {
        callback.addCall(adapter.build(DiscussionInterface.class, params).pinDiscussion(CanvasContext.getApiContext(canvasContext), canvasContext.getId(), topicId, true, "")).enqueue(callback);
    }

    public static void unpinDiscussion(@NonNull RestBuilder adapter, CanvasContext canvasContext, long topicId, StatusCallback<DiscussionTopicHeader> callback, @NonNull RestParams params) {
        callback.addCall(adapter.build(DiscussionInterface.class, params).pinDiscussion(CanvasContext.getApiContext(canvasContext), canvasContext.getId(), topicId, false, "")).enqueue(callback);
    }

    public static void lockDiscussion(@NonNull RestBuilder adapter, CanvasContext canvasContext, long topicId, StatusCallback<DiscussionTopicHeader> callback, @NonNull RestParams params) {
        callback.addCall(adapter.build(DiscussionInterface.class, params).lockDiscussion(CanvasContext.getApiContext(canvasContext), canvasContext.getId(), topicId, true, "")).enqueue(callback);
    }

    public static void unlockDiscussion(@NonNull RestBuilder adapter, CanvasContext canvasContext, long topicId, StatusCallback<DiscussionTopicHeader> callback, @NonNull RestParams params) {
        callback.addCall(adapter.build(DiscussionInterface.class, params).lockDiscussion(CanvasContext.getApiContext(canvasContext), canvasContext.getId(), topicId, false, "")).enqueue(callback);
    }

    public static void deleteDiscussionTopicHeader(@NonNull RestBuilder adapter, CanvasContext canvasContext, long topicId, StatusCallback<Void> callback, @NonNull RestParams params) {
        callback.addCall(adapter.build(DiscussionInterface.class, params).deleteDiscussionTopic(CanvasContext.getApiContext(canvasContext), canvasContext.getId(), topicId)).enqueue(callback);
    }

    public static void editDiscussionTopic(@NonNull CanvasContext canvasContext, long topicId, DiscussionTopicPostBody body, RestBuilder adapter, final StatusCallback<DiscussionTopicHeader> callback, RestParams params) {
        callback.addCall(adapter.build(DiscussionInterface.class, params).editDiscussionTopic(CanvasContext.getApiContext(canvasContext), canvasContext.getId(), topicId, body)).enqueue(callback);
    }

    public static void deleteDiscussionEntry(@NonNull RestBuilder adapter, CanvasContext canvasContext, long topicId, long entryId, StatusCallback<Void> callback, @NonNull RestParams params) {
        callback.addCall(adapter.build(DiscussionInterface.class, params).deleteDiscussionEntry(CanvasContext.getApiContext(canvasContext), canvasContext.getId(), topicId, entryId)).enqueue(callback);
    }
}
