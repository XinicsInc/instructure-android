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
import com.instructure.canvasapi2.models.Favorite;
import com.instructure.canvasapi2.models.Group;
import com.instructure.canvasapi2.utils.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;

public class GroupAPI {

    interface GroupInterface {

        @GET("users/self/favorites/groups")
        Call<List<Group>> getFirstPageFavoriteGroups();

        @GET
        Call<List<Group>> getNextPageGroups(@Url String nextUrl);

        @GET("users/self/groups?include[]=favorites")
        Call<List<Group>> getFirstPageGroups();

        @GET("groups/{groupId}?include[]=permissions&include[]=favorites")
        Call<Group> getDetailedGroup(@Path("groupId") long groupId);

        @POST("users/self/favorites/groups/{groupId}")
        Call<Favorite> addGroupToFavorites(@Path("groupId") long groupId);

        @DELETE("users/self/favorites/groups/{groupId}")
        Call<Favorite> removeGroupFromFavorites(@Path("groupId") long groupId);

        @GET("users/self/groups")
        Call<List<Group>> getGroupsSynchronous(@Query("page") int page);

        @GET("courses/{courseId}/groups")
        Call<List<Group>> getFirstPageCourseGroups(@Path("courseId") long courseId);
    }

    public static void getFirstPageGroups(@NonNull RestBuilder adapter, @NonNull StatusCallback<List<Group>> callback, @NonNull RestParams params) {
        callback.addCall(adapter.build(GroupInterface.class, params).getFirstPageGroups()).enqueue(callback);
    }

    public static void getFavoriteGroups(@NonNull RestBuilder adapter, @NonNull StatusCallback<List<Group>> callback, @NonNull RestParams params) {
        callback.addCall(adapter.build(GroupInterface.class, params).getFirstPageFavoriteGroups()).enqueue(callback);
    }

    public static Response<List<Group>> getFavoriteGroupsSynchronously(@NonNull RestBuilder adapter, @NonNull RestParams params) throws IOException {
        return (adapter.build(GroupInterface.class, params).getFirstPageFavoriteGroups()).execute();
    }

    public static Response<List<Group>> getGroupsSynchronously(@NonNull RestBuilder adapter, @NonNull RestParams params) throws IOException {
        return (adapter.build(GroupInterface.class, params).getGroupsSynchronous(1)).execute();
    }

    public static Response<List<Group>> getNextPageGroupsSynchronously(@NonNull String url, @NonNull RestBuilder adapter, @NonNull RestParams params) throws IOException {
        return (adapter.build(GroupInterface.class, params).getNextPageGroups(url)).execute();
    }

    public static void getNextPageGroups(String nextUrl, RestBuilder adapter, StatusCallback<List<Group>> callback, RestParams params) {
        callback.addCall(adapter.build(GroupInterface.class, params).getNextPageGroups(nextUrl)).enqueue(callback);
    }

    public static void getDetailedGroup(@NonNull RestBuilder adapter, @NonNull StatusCallback<Group> callback, @NonNull RestParams params, long groupId) {
        callback.addCall(adapter.build(GroupInterface.class, params).getDetailedGroup(groupId)).enqueue(callback);
    }

    public static void addGroupToFavorites(@NonNull RestBuilder adapter, @NonNull StatusCallback<Favorite> callback, @NonNull RestParams params, long groupId) {
        callback.addCall(adapter.build(GroupInterface.class, params).addGroupToFavorites(groupId)).enqueue(callback);
    }

    public static void removeGroupFromFavorites(@NonNull RestBuilder adapter, @NonNull StatusCallback<Favorite> callback, @NonNull RestParams params, long groupId) {
        callback.addCall(adapter.build(GroupInterface.class, params).removeGroupFromFavorites(groupId)).enqueue(callback);
    }

    public static void getGroupsForCourse(@NonNull RestBuilder adapter, @NonNull StatusCallback<List<Group>> callback, @NonNull RestParams params, long courseId) {
        callback.addCall(adapter.build(GroupInterface.class, params).getFirstPageCourseGroups(courseId)).enqueue(callback);
    }


    /////////////////////////////////////////////////////////////////////////////
    // Synchronous
    //
    // If Retrofit is unable to parse (no network for example) Synchronous calls
    // will throw a nullPointer exception. All synchronous calls need to be in a
    // try catch block.
    /////////////////////////////////////////////////////////////////////////////

    public static List<Group> getAllGroupsSynchronous(
            @NonNull RestBuilder adapter,
            @NonNull RestParams params) {
        try {
            ArrayList<Group> allGroups = new ArrayList<Group>();
            int page = 1;
            long firstItemId = -1;

            //for(ever) loop. break once we've run outta stuff;
            for(;;){
                List<Group> groups = adapter.build(GroupInterface.class, params).getGroupsSynchronous(page).execute().body();
                page++;

                //This is all or nothing. We don't want partial data.
                if(groups == null){
                    return null;
                } else if(groups.size() == 0){
                    break;
                } else if(groups.get(0).getId() == firstItemId){
                    break;
                } else{
                    firstItemId = groups.get(0).getId();
                    allGroups.addAll(groups);
                }
            }

            return allGroups;

        } catch (Exception E) {
            return null;
        }
    }
}
