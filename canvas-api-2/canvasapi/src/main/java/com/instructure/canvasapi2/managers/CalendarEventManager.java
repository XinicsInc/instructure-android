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
import com.instructure.canvasapi2.apis.CalendarEventAPI;
import com.instructure.canvasapi2.builders.RestBuilder;
import com.instructure.canvasapi2.builders.RestParams;
import com.instructure.canvasapi2.models.ScheduleItem;
import com.instructure.canvasapi2.tests.CalendarEventManager_Test;
import com.instructure.canvasapi2.utils.ExhaustiveListCallback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Response;

import static android.R.attr.type;


public class CalendarEventManager extends BaseManager {

    private static boolean mTesting = false;

    public static void getCalendarEvents(
            CalendarEventAPI.CalendarEventType type,
            RestParams params,
            String startDate,
            String endDate,
            List<String> canvasContexts,
            StatusCallback<List<ScheduleItem>> callback) {

        if (isTesting() || mTesting) {
            CalendarEventManager_Test.getCalendarEvents(callback);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            CalendarEventAPI.getCalendarEvents(true, type, startDate, endDate, canvasContexts, adapter, callback, params);
        }
    }

    public static void getCalendarEventsExhaustive(
            final boolean allEvents,
            @NonNull final CalendarEventAPI.CalendarEventType type,
            @Nullable final String startDate,
            @Nullable final String endDate,
            @NonNull final List<String> canvasContexts,
            @NonNull StatusCallback<List<ScheduleItem>> callback,
            boolean forceNetwork) {

        if (isTesting() || mTesting) {
            CalendarEventManager_Test.getCalendarEvents(callback);
        } else {
            final RestBuilder adapter = new RestBuilder(callback);
            final RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(true)
                    .withForceReadFromNetwork(forceNetwork)
                    .build();

            StatusCallback<List<ScheduleItem>> exhaustiveCallback = new ExhaustiveListCallback<ScheduleItem>(callback) {
                @Override
                public void getNextPage(@NonNull StatusCallback<List<ScheduleItem>> callback, @NonNull String nextUrl, boolean isCached) {
                    CalendarEventAPI.getCalendarEvents(allEvents, type, startDate, endDate, canvasContexts, adapter, callback, params);
                }
            };

            adapter.setStatusCallback(exhaustiveCallback);
            CalendarEventAPI.getCalendarEvents(allEvents, type, startDate, endDate, canvasContexts, adapter, exhaustiveCallback, params);
        }
    }

    public static void getUpcomingEvents(StatusCallback<List<ScheduleItem>> callback, boolean forceNetwork) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            final RestBuilder adapter = new RestBuilder(callback);
            final RestParams params = new RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .build();
            CalendarEventAPI.getUpcomingEvents(adapter, params, callback);
        }
    }

    @NonNull
    public static List<ScheduleItem> getUpcomingEventsSynchronous(boolean forceNetwork) throws IOException {
        if (isTesting() || mTesting) {
            // TODO
            return new ArrayList();
        } else {
            final RestBuilder adapter = new RestBuilder();
            final RestParams params = new RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .withPerPageQueryParam(true)
                    .build();

            Response<List<ScheduleItem>> response = CalendarEventAPI.getUpcomingEventsSynchronous(adapter, params);
            return (response != null && response.isSuccessful() && response.body() != null) ? response.body() : new ArrayList();
        }
    }

    public static void getCalendarEvent(long eventId, StatusCallback<ScheduleItem> callback, boolean forceNetwork) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            final RestBuilder adapter = new RestBuilder(callback);
            final RestParams params = new RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .build();
            CalendarEventAPI.getCalendarEvent(eventId, adapter, params, callback);
        }
    }

    public static void deleteCalendarEvent(long eventId, String cancelReason, StatusCallback<ScheduleItem> callback) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder().build();
            CalendarEventAPI.deleteCalendarEvent(eventId, cancelReason, adapter, params, callback);
        }
    }

    public static void createCalendarEvent(
            @NonNull String contextCode,
            @NonNull String title,
            @NonNull String description,
            @NonNull String startDate,
            @NonNull String endDate,
            @NonNull String location,
            @NonNull StatusCallback<ScheduleItem> callback) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder().build();
            CalendarEventAPI.createCalendarEvent(contextCode, title, description, startDate, endDate, location, adapter, params, callback);
        }
    }

    public static void getAllCalendarEventsWithSubmissionsAirwolf(
            String airwolfDomain,
            String parentId,
            String studentId,
            String startDate,
            String endDate,
            ArrayList<String> canvasContexts,
            boolean forceNetwork,
            StatusCallback<List<ScheduleItem>> callback) {

        if (isTesting() || mTesting) {
            //TODO:
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withPerPageQueryParam(true)
                    .withDomain(airwolfDomain)
                    .withAPIVersion("")
                    .withForceReadFromNetwork(forceNetwork)
                    .build();

            CalendarEventAPI.getAllCalendarEventsWithSubmissionAirwolf(
                    parentId, studentId, startDate, endDate, canvasContexts, adapter, callback, params);
        }
    }

    public static void getCalendarEventAirwolf(
            String airwolfDomain,
            String parentId,
            String studentId,
            String eventId,
            StatusCallback<ScheduleItem> callback) {

        RestBuilder adapter = new RestBuilder(callback);
        RestParams params = new RestParams.Builder()
                .withShouldIgnoreToken(false)
                .withPerPageQueryParam(false)
                .withDomain(airwolfDomain)
                .withAPIVersion("")
                .build();

        CalendarEventAPI.getCalendarEventAirwolf(parentId, studentId, eventId, adapter, callback, params);
    }

    private static boolean isEventTypeValid(int type) {
        return (type == CalendarEventAPI.ASSIGNMENT_TYPE || type == CalendarEventAPI.EVENT_TYPE);
    }
}
