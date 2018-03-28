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
import com.instructure.canvasapi2.apis.SubmissionAPI;
import com.instructure.canvasapi2.builders.RestBuilder;
import com.instructure.canvasapi2.builders.RestParams;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.LTITool;
import com.instructure.canvasapi2.models.RubricCriterionAssessment;
import com.instructure.canvasapi2.models.Submission;
import com.instructure.canvasapi2.models.SubmissionSummary;
import com.instructure.canvasapi2.tests.SubmissionManager_Test;
import com.instructure.canvasapi2.utils.ExhaustiveListCallback;

import java.util.List;
import java.util.Map;

public class SubmissionManager extends BaseManager {

    private static boolean mTesting = false;

    public static void getSingleSubmission(long courseId, long assignmentId, long studentId, @NonNull StatusCallback<Submission> callback, boolean forceNetwork) {
        if (isTesting() | mTesting) {
            // TODO
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .withPerPageQueryParam(false)
                    .build();

            SubmissionAPI.getSingleSubmission(courseId, assignmentId, studentId, adapter, callback, params);
        }
    }

    public static void getAllStudentSubmissionsForCourse(final long studentId, final long courseId, @NonNull StatusCallback<List<Submission>> callback, boolean forceNetwork) {
        if (isTesting() | mTesting) {
            SubmissionManager_Test.getStudentSubmissionsForCourse(studentId, courseId, callback);
        } else {
            final RestBuilder adapter = new RestBuilder(callback);
            final RestParams params = new RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .withPerPageQueryParam(true)
                    .withShouldIgnoreToken(false)
                    .build();

            StatusCallback<List<Submission>> depaginatedCallback = new ExhaustiveListCallback<Submission>(callback) {
                @Override
                public void getNextPage(@NonNull StatusCallback<List<Submission>> callback, @NonNull String nextUrl, boolean isCached) {
                    SubmissionAPI.getStudentSubmissionsForCourse(courseId, studentId, adapter, callback, params);
                }
            };

            adapter.setStatusCallback(depaginatedCallback);
            SubmissionAPI.getStudentSubmissionsForCourse(courseId, studentId, adapter, callback, params);
        }
    }

    public static void updateRubricAssessment(long courseId, long assignmentId, long studentId, Map<String, RubricCriterionAssessment> assessmentMap, @NonNull StatusCallback<Submission> callback) {
        if (isTesting() || mTesting) {
            SubmissionManager_Test.updateRubricAssessment(courseId, assignmentId, studentId, assessmentMap, callback);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder().build();
            SubmissionAPI.updateRubricAssessment(courseId, assignmentId, studentId, assessmentMap, adapter, callback, params);
        }
    }

    public static void postSubmissionComment(long courseId, long assignmentId, long userId, String commentText, boolean isGroupMessage, List<Long> attachments, @NonNull StatusCallback<Submission> callback) {
        if (isTesting() || mTesting) {
            SubmissionManager_Test.postSubmissionComment(courseId, assignmentId, userId, commentText, isGroupMessage, callback);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder().build();
            SubmissionAPI.postSubmissionComment(courseId, assignmentId, userId, commentText, isGroupMessage, attachments, adapter, callback, params);
        }
    }

    public static void postSubmissionGrade(long courseId, long assignmentId, long userId, String score, boolean isExcused, boolean forceNetwork, @NonNull StatusCallback<Submission> callback) {
        if (isTesting() | mTesting) {
//            SubmissionManager_Test.postSubmissionGrade(courseId, assignmentId, userId, score, callback);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .build();

            SubmissionAPI.postSubmissionGrade(courseId, assignmentId, userId, score, isExcused, adapter, callback, params);
        }
    }

    public static void postSubmissionExcusedStatus(long courseId, long assignmentId, long userId, boolean isExcused, boolean forceNetwork, @NonNull StatusCallback<Submission> callback) {
        if (isTesting() | mTesting) {
//            SubmissionManager_Test.postSubmissionGrade(courseId, assignmentId, userId, score, callback);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .build();

            SubmissionAPI.postSubmissionExcusedStatus(courseId, assignmentId, userId, isExcused, adapter, callback, params);
        }
    }

    public static void getSubmissionSummary(long courseId, long assignmentId, boolean forceNetwork, @NonNull StatusCallback<SubmissionSummary> callback) {
        if (isTesting() | mTesting) {
//            SubmissionManager_Test.postSubmissionGrade(courseId, assignmentId, userId, score, callback);
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .withPerPageQueryParam(false)
                    .withShouldIgnoreToken(false)
                    .build();

            SubmissionAPI.getSubmissionSummary(courseId, assignmentId, adapter, params, callback);
        }
    }

    public static void postTextSubmission(CanvasContext canvasContext, long assignmentId, String text, StatusCallback<Submission> callback) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withCanvasContext(canvasContext)
                    .build();
            SubmissionAPI.postTextSubmission(canvasContext.getId(), assignmentId, text, adapter, params, callback);
        }
    }

    public static void postUrlSubmission(CanvasContext canvasContext, long assignmentId, String url, boolean isLti, StatusCallback<Submission> callback) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withCanvasContext(canvasContext)
                    .build();
            String type = isLti ? "basic_lti_launch" : "online_url";
            SubmissionAPI.postUrlSubmission(canvasContext.getId(), assignmentId, type, url, adapter, params, callback);
        }
    }

    public static void getLtiFromAuthenticationUrl(String url, StatusCallback<LTITool> callback, boolean forceNetwork) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withForceReadFromNetwork(forceNetwork)
                    .build();
            SubmissionAPI.getLtiFromAuthenticationUrl(url, adapter, params, callback);
        }
    }

    public static void postMediaSubmissionComment(CanvasContext canvasContext, long assignmentId, long studentId, String mediaId, String mediaType, boolean isGroupComment, StatusCallback<Submission> callback) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withCanvasContext(canvasContext)
                    .build();
            SubmissionAPI.postMediaSubmissionComment(canvasContext.getId(), assignmentId, studentId, mediaId, mediaType, isGroupComment, adapter, params, callback);
        }
    }

    public static void postMediaSubmission(CanvasContext canvasContext, long assignmentId, String submissionType, String mediaId, String mediaType, StatusCallback<Submission> callback) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            RestBuilder adapter = new RestBuilder(callback);
            RestParams params = new RestParams.Builder()
                    .withCanvasContext(canvasContext)
                    .build();
            SubmissionAPI.postMediaSubmission(canvasContext.getId(), assignmentId, submissionType, mediaId, mediaType, adapter, params, callback);
        }
    }


    public static @Nullable Submission postSubmissionAttachmentsSynchronous(long courseId, long assignmentId, List<Long> attachmentsIds) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            RestBuilder adapter = new RestBuilder();
            RestParams params = new RestParams.Builder().build();
            return SubmissionAPI.postSubmissionAttachmentsSynchronous(courseId, assignmentId, attachmentsIds, adapter, params);
        }
        return null;
    }
}
