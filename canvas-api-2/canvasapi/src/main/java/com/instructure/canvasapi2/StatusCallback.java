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
package com.instructure.canvasapi2;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.instructure.canvasapi2.models.CanvasErrorCode;
import com.instructure.canvasapi2.utils.APIHelper;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.LinkHeaders;
import com.instructure.canvasapi2.utils.Logger;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public abstract class StatusCallback<DATA> implements Callback<DATA> {

    private boolean mIsApiCallInProgress = false;
    private LinkHeaders mLinkHeaders = null;
    private ArrayList<Call<DATA>> mCalls = new ArrayList<>();
    private boolean mIsCanceled = false;

    public StatusCallback() {}

    @Override
    final public void onResponse(@NonNull final Call<DATA> data, @NonNull final Response<DATA> response) {
        mIsApiCallInProgress = true;
        if (response.isSuccessful()) {
            publishHeaderResponseResults(response, response.raw(), APIHelper.parseLinkHeaderResponse(response.headers()));
        } else if (response.code() == 504) {
            // Cached response does not exist.
            Logger.e("StatusCallback: GOT A 504");
            // No response
            onCallbackFinished(ApiType.CACHE);
        } else {
            onFail(data, new Throwable("StatusCallback: 40X Error"), response);
            try {
                EventBus.getDefault().post(new CanvasErrorCode(response.code(), response.errorBody().string()));
            } catch (IOException ignored) {}
            // No response or no data
            onCallbackFinished(ApiType.API);
        }
    }

    @Override
    final public void onFailure(@NonNull Call<DATA> data, @NonNull Throwable t) {
        mIsApiCallInProgress = false;
        if (data.isCanceled() || "Canceled".equals(t.getMessage())) {
            Logger.d("StatusCallback: callback(s) were cancelled");
            onCancelled();
        } else {
            Logger.e("StatusCallback: Failure: " + t.getMessage());
            onFail(data, t, null);
        }
    }

    final public void onCallbackStarted() {
        mIsApiCallInProgress = true;
        onStarted();
    }

    final public void onCallbackFinished(ApiType type) {
        mIsApiCallInProgress = false;
        onFinished(type);
    }

    final synchronized public boolean isCallInProgress() {
        return mIsApiCallInProgress;
    }

    /**
     * Where all responses will report. Api or Cache.
     * @param response The data of the response
     * @param linkHeaders The link headers for the response, used for pagination
     * @param type The type of response, Cache or Api
     */
    public void onResponse(@NonNull Response<DATA> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type){}

    /**
     * The result of a failed call
     * @param call The original call
     * @param error The error
     * @param response The data of the response, can be null
     */
    public void onFail(@Nullable Call<DATA> call, @NonNull Throwable error, @Nullable Response response) {}
    public void onCancelled(){}
    public void onStarted(){}
    public void onFinished(ApiType type){}

    private void publishHeaderResponseResults(@NonNull Response<DATA> response, @NonNull okhttp3.Response okResponse, @NonNull LinkHeaders linkHeaders) {
        setLinkHeaders(linkHeaders);
        final boolean isCacheResponse = APIHelper.isCachedResponse(okResponse);
        Logger.d("Is Cache Response? " + (isCacheResponse ? "YES" : "NO"));
        if (isCacheResponse) {
            onResponse(response, linkHeaders, ApiType.CACHE);
            onCallbackFinished(ApiType.CACHE);
        } else {
            onResponse(response, linkHeaders, ApiType.API);
            onCallbackFinished(ApiType.API);
        }
    }

    public boolean moreCallsExist() {
        return moreCallsExist(getLinkHeaders());
    }

    public boolean isFirstPage() {
        return isFirstPage(getLinkHeaders());
    }

    public static boolean moreCallsExist(@Nullable LinkHeaders...headers) {
        return (headers != null && headers.length > 0 && headers[0] != null && headers[0].nextUrl != null);
    }

    public static boolean isFirstPage(@Nullable LinkHeaders...headers) {
        return (headers == null || headers.length == 0 || headers[0] == null);
    }

    public void setLinkHeaders(@Nullable LinkHeaders linkHeaders) {
        mLinkHeaders = linkHeaders;
    }

    public @Nullable LinkHeaders getLinkHeaders() {
        return mLinkHeaders;
    }

    private void clearLinkHeaders() {
        mLinkHeaders = null;
    }

    /**
     * Used to reset a callback to it's former glory
     * Clears the LinkHeaders
     * Cancels any ongoing Calls
     * Clears all calls from the ArrayList of Calls
     */
    public void reset() {
        clearLinkHeaders();
        cancel();
        clearCalls();
        mIsCanceled = false;
    }

    public void clearCalls() {
        mCalls.clear();
    }

    public Call<DATA> addCall(Call<DATA> call) {
        if (!call.isCanceled()) mIsCanceled = true;
        mCalls.add(call);
        return call;
    }

    public void cancel() {
        mIsCanceled = true;
        for(Call<DATA> call : mCalls) {
            call.cancel();
        }
    }

    public boolean isCanceled() {
        return mIsCanceled;
    }

    public static void cancelAllCalls() {
        CanvasRestAdapter.cancelAllCalls();
    }
}
