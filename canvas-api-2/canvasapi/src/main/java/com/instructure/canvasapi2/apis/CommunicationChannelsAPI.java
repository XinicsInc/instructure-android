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
import com.instructure.canvasapi2.models.CommunicationChannel;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public class CommunicationChannelsAPI {

    interface CommunicationChannelInterface {
        @GET("users/{user_id}/communication_channels")
        Call<List<CommunicationChannel>> getCommunicationChannels(@Path("user_id") long userId);

        @POST("users/self/communication_channels?communication_channel[type]=push")
        Call<ResponseBody> addPushCommunicationChannel(@Query("communication_channel[token]") String registrationId);
    }

    public static void getCommunicationChannels(
            long userId,
            @NonNull RestBuilder adapter,
            @NonNull RestParams params,
            @NonNull StatusCallback<List<CommunicationChannel>> callback) {
        callback.addCall(adapter.build(CommunicationChannelInterface.class, params).getCommunicationChannels(userId)).enqueue(callback);
    }

    public static Response<ResponseBody> addNewPushCommunicationChannelSynchronous(String registrationId, @NonNull RestBuilder adapter, @NonNull RestParams params) {
        try {
            return adapter.build(CommunicationChannelInterface.class, params).addPushCommunicationChannel(registrationId).execute();
        } catch (Exception e) {
            return null;
        }
    }

    public static void addNewPushCommunicationChannel(
            @NonNull String registrationId,
            @NonNull StatusCallback<ResponseBody> callback,
            @NonNull RestBuilder adapter,
            @NonNull RestParams params) {
        callback.addCall(adapter.build(CommunicationChannelInterface.class, params).addPushCommunicationChannel(registrationId)).enqueue(callback);
    }
}
