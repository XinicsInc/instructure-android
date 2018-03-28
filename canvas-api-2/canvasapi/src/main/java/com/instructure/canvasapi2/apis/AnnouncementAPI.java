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

import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.builders.RestBuilder;
import com.instructure.canvasapi2.builders.RestParams;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.DiscussionTopicHeader;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;


public class AnnouncementAPI {

    interface AnnouncementInterface {
        @GET("{contextType}/{contextId}/discussion_topics?only_announcements=1")
        Call<List<DiscussionTopicHeader>> getFirstPageAnnouncementsList(@Path("contextType") String contextType, @Path("contextId") long contextId);

        @GET
        Call<List<DiscussionTopicHeader>>  getNextPageAnnouncementsList(@Url String nextUrl);

        @GET("announcements")
        Call<List<DiscussionTopicHeader>> getAnnouncements(@Query("context_codes[]") List<String> contextCodes, @Query("start_date") String startDate, @Query("end_date") String endDate);
    }

    public static void getAnnouncements(@NonNull CanvasContext canvasContext, @NonNull RestBuilder adapter, @NonNull StatusCallback<List<DiscussionTopicHeader>> callback, @NonNull RestParams params) {
        final String contextType = CanvasContext.getApiContext(canvasContext);
        if (StatusCallback.isFirstPage(callback.getLinkHeaders())) {
            callback.addCall(adapter.build(AnnouncementInterface.class, params).getFirstPageAnnouncementsList(contextType, canvasContext.getId())).enqueue(callback);
        } else if (StatusCallback.moreCallsExist(callback.getLinkHeaders()) && callback.getLinkHeaders() != null) {
            callback.addCall(adapter.build(AnnouncementInterface.class, params).getNextPageAnnouncementsList(callback.getLinkHeaders().nextUrl)).enqueue(callback);
        }
    }

    public static void getAnnouncements(@NonNull List<String> contextCodes, String startDate, String endDate, @NonNull RestBuilder adapter, @NonNull StatusCallback<List<DiscussionTopicHeader>> callback, @NonNull RestParams params) {
        if (StatusCallback.isFirstPage(callback.getLinkHeaders())) {
            callback.addCall(adapter.build(AnnouncementInterface.class, params).getAnnouncements(contextCodes, startDate, endDate)).enqueue(callback);
        } else if (StatusCallback.moreCallsExist(callback.getLinkHeaders()) && callback.getLinkHeaders() != null) {
            callback.addCall(adapter.build(AnnouncementInterface.class, params).getNextPageAnnouncementsList(callback.getLinkHeaders().nextUrl)).enqueue(callback);
        }
    }

    public static void getFirstPageAnnouncements(@NonNull CanvasContext canvasContext, @NonNull RestBuilder adapter, @NonNull StatusCallback<List<DiscussionTopicHeader>> callback, @NonNull RestParams params) {
        final String contextType = CanvasContext.getApiContext(canvasContext);
        callback.addCall(adapter.build(AnnouncementInterface.class, params).getFirstPageAnnouncementsList(contextType, canvasContext.getId())).enqueue(callback);
    }

    public static void getNextPage(String nextUrl, @NonNull RestBuilder adapter, @NonNull StatusCallback<List<DiscussionTopicHeader>> callback, @NonNull RestParams params) {
        callback.addCall(adapter.build(AnnouncementInterface.class, params).getNextPageAnnouncementsList(nextUrl)).enqueue(callback);
    }
}
