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
import android.text.TextUtils;

import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.builders.RestBuilder;
import com.instructure.canvasapi2.builders.RestParams;
import com.instructure.canvasapi2.models.LTITool;
import com.instructure.canvasapi2.models.RubricCriterionAssessment;
import com.instructure.canvasapi2.models.Submission;
import com.instructure.canvasapi2.models.SubmissionSummary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;
import retrofit2.http.Url;


public class SubmissionAPI {

    interface SubmissionInterface {

        @GET("courses/{courseId}/assignments/{assignmentId}/submissions/{studentId}?include[]=rubric_assessment&include[]=submission_history&include[]=submission_comments&include[]=group")
        Call<Submission> getSingleSubmission(
                @Path("courseId") long courseId,
                @Path("assignmentId") long assignmentId,
                @Path("studentId") long studentId);

        @GET("courses/{courseId}/students/submissions?include[]=assignment&include[]=rubric_assessment&include[]=submission_history&include[]=submission_comments&include[]=group")
        Call<List<Submission>> getStudentSubmissionsForCourse(@Path("courseId") long courseId, @Query("student_ids[]") long studentId);

        @GET
        Call<List<Submission>> getNextPageSubmissions(@Url String nextUrl);

        @PUT("courses/{courseId}/assignments/{assignmentId}/submissions/{userId}")
        Call<Submission> postSubmissionRubricAssessmentMap(
                @Path("courseId") long courseId,
                @Path("assignmentId") long assignmentId,
                @Path("userId") long userId,
                @QueryMap Map<String, String> rubricAssessment
        );

        @PUT("courses/{courseId}/assignments/{assignmentId}/submissions/{userId}")
        Call<Submission> postSubmissionComment(
                @Path("courseId") long courseId,
                @Path("assignmentId") long assignmentId,
                @Path("userId") long userId,
                @Query("comment[text_comment]") String comment,
                @Query("comment[group_comment]") boolean isGroupComment,
                @Query("comment[file_ids][]") List<Long> attachments
        );

        @POST("{contextId}/assignments/{assignmentId}/submissions")
        Call<Submission> postTextSubmission(
                @Path("contextId") long contextId,
                @Path("assignmentId") long assignmentId,
                @Query("submission[submission_type]") String submissionType,
                @Query("submission[body]") String text);

        @POST("{contextId}/assignments/{assignmentId}/submissions")
        Call<Submission> postUrlSubmission(
                @Path("contextId") long contextId,
                @Path("assignmentId") long assignmentId,
                @Query("submission[submission_type]") String submissionType,
                @Query("submission[url]") String url);

        @PUT("{contextId}/assignments/{assignmentId}/submissions/{userId}")
        Call<Submission> postMediaSubmissionComment(
                @Path("contextId") long contextId,
                @Path("assignmentId") long assignmentId,
                @Path("userId") long userId,
                @Query("comment[media_comment_id]") String mediaId,
                @Query("comment[media_comment_type]") String commentType,
                @Query("comment[group_comment]") boolean isGroupComment);

        @POST("{contextId}/assignments/{assignmentId}/submissions")
        Call<Submission> postMediaSubmission(
                @Path("contextId") long contextId,
                @Path("assignmentId") long assignmentId,
                @Query("submission[submission_type]") String submissionType,
                @Query("submission[media_comment_id]") String notoriousId,
                @Query("submission[media_comment_type]") String mediaType);

        @POST("courses/{courseId}/assignments/{assignmentId}/submissions")
        Call<Submission> postSubmissionAttachments(
                @Path("courseId") long courseId,
                @Path("assignmentId") long assignmentId,
                @Query("submission[submission_type]") String submissionType,
                @Query("submission[file_ids][]") List<Long> attachments);

        @GET
        Call<LTITool> getLtiFromAuthenticationUrl(@Url String url);


        @PUT("courses/{contextId}/assignments/{assignmentId}/submissions/{userId}")
        Call<Submission> postSubmissionGrade(@Path("contextId") long contextId,
                                             @Path("assignmentId") long assignmentId, @Path("userId") long userId,
                                             @Query("submission[posted_grade]") String assignmentScore,
                                             @Query("submission[excuse]") boolean isExcused);

        @PUT("courses/{contextId}/assignments/{assignmentId}/submissions/{userId}")
        Call<Submission> postSubmissionExcusedStatus(@Path("contextId") long contextId,
                                             @Path("assignmentId") long assignmentId, @Path("userId") long userId,
                                             @Query("submission[excuse]") boolean isExcused);

        @GET("courses/{courseId}/assignments/{assignmentId}/submission_summary")
        Call<SubmissionSummary> getSubmissionSummary(@Path("courseId") long courseId,
                                                     @Path("assignmentId") long assignmentId);
    }

    public static void getSingleSubmission(long courseId, long assignmentId, long studentId,  @NonNull RestBuilder adapter, @NonNull StatusCallback<Submission> callback, @NonNull RestParams params) {
        callback.addCall(adapter.build(SubmissionInterface.class, params).getSingleSubmission(courseId, assignmentId, studentId)).enqueue(callback);
    }

    public static void getStudentSubmissionsForCourse(long courseId, long studentId, @NonNull RestBuilder adapter, @NonNull StatusCallback<List<Submission>> callback, @NonNull RestParams params) {
        if (StatusCallback.isFirstPage(callback.getLinkHeaders())) {
            callback.addCall(adapter.build(SubmissionInterface.class, params).getStudentSubmissionsForCourse(courseId, studentId)).enqueue(callback);
        } else if (callback.getLinkHeaders() != null && StatusCallback.moreCallsExist(callback.getLinkHeaders())) {
            callback.addCall(adapter.build(SubmissionInterface.class, params).getNextPageSubmissions(callback.getLinkHeaders().nextUrl)).enqueue(callback);
        }
    }

    public static void postTextSubmission(long contextId, long assignmentId, @NonNull String text, @NonNull RestBuilder adapter, @NonNull RestParams params, @NonNull StatusCallback<Submission> callback) {
        callback.addCall(adapter.build(SubmissionInterface.class, params).postTextSubmission(contextId, assignmentId, "online_text_entry", text)).enqueue(callback);
    }

    public static void postUrlSubmission(long contextId, long assignmentId, @NonNull String submissionType, @NonNull String url, @NonNull RestBuilder adapter, @NonNull RestParams params, @NonNull StatusCallback<Submission> callback) {
        callback.addCall(adapter.build(SubmissionInterface.class, params).postUrlSubmission(contextId, assignmentId, submissionType, url)).enqueue(callback);
    }

    public static void getLtiFromAuthenticationUrl(@NonNull String url, @NonNull RestBuilder adapter, @NonNull RestParams params, @NonNull StatusCallback<LTITool> callback) {
        callback.addCall(adapter.build(SubmissionInterface.class, params).getLtiFromAuthenticationUrl(url)).enqueue(callback);
    }

    public static void postSubmissionGrade(long courseId, long assignmentId, long userId, String assignmentScore, boolean isExcused, @NonNull RestBuilder adapter, @NonNull StatusCallback<Submission> callback, @NonNull RestParams params) {
        callback.addCall(adapter.build(SubmissionInterface.class, params).postSubmissionGrade(courseId, assignmentId, userId, assignmentScore, isExcused)).enqueue(callback);
    }

    public static void postSubmissionComment(long courseId, long assignmentID, long userID, String comment, boolean isGroupMessage, List<Long> attachmentsIds, @NonNull RestBuilder adapter, @NonNull StatusCallback<Submission> callback, @NonNull RestParams params) {
        callback.addCall(adapter.build(SubmissionInterface.class, params).postSubmissionComment(courseId, assignmentID, userID, comment, isGroupMessage, attachmentsIds)).enqueue(callback);
    }

    public static void postMediaSubmissionComment(long canvasContextId, long assignmentId, long studentId, String mediaId, String mediaType, boolean isGroupComment, RestBuilder adapter, RestParams params, StatusCallback<Submission> callback) {
        callback.addCall(adapter.build(SubmissionInterface.class, params).postMediaSubmissionComment(canvasContextId, assignmentId, studentId, mediaId, mediaType, isGroupComment)).enqueue(callback);
    }

    public static void postMediaSubmission(long canvasContextId, long assignmentId, String submissionType, String mediaId, String mediaType, RestBuilder adapter, RestParams params, StatusCallback<Submission> callback) {
        callback.addCall(adapter.build(SubmissionInterface.class, params).postMediaSubmission(canvasContextId, assignmentId, submissionType, mediaId, mediaType)).enqueue(callback);
    }

    public static void postSubmissionExcusedStatus(long courseId, long assignmentId, long userId, boolean isExcused, @NonNull RestBuilder adapter, @NonNull StatusCallback<Submission> callback, @NonNull RestParams params) {
        callback.addCall(adapter.build(SubmissionInterface.class, params).postSubmissionExcusedStatus(courseId, assignmentId, userId, isExcused)).enqueue(callback);
    }

    public static void updateRubricAssessment(long courseId, long assignmentId, long userId, Map<String, RubricCriterionAssessment> rubricAssessment, @NonNull RestBuilder adapter, @NonNull StatusCallback<Submission> callback, @NonNull RestParams params) {
        Map<String, String> assessmentParamMap = generateRubricAssessmentQueryMap(rubricAssessment);
        callback.addCall(adapter.build(SubmissionInterface.class, params).postSubmissionRubricAssessmentMap(courseId, assignmentId, userId, assessmentParamMap)).enqueue(callback);
    }

    public static void getSubmissionSummary(long courseId, long assignmentId, @NonNull RestBuilder adapter, @NonNull RestParams params, @NonNull StatusCallback<SubmissionSummary> callback) {
        callback.addCall(adapter.build(SubmissionInterface.class, params).getSubmissionSummary(courseId, assignmentId)).enqueue(callback);
    }

    public static @Nullable Submission postSubmissionAttachmentsSynchronous(long courseId, long assignmentId, List<Long> attachmentsIds, RestBuilder adapter, RestParams params) {
        try {
            return adapter.build(SubmissionInterface.class, params).postSubmissionAttachments(courseId, assignmentId, "online_upload", attachmentsIds).execute().body();
        } catch (Exception e) {
            return null;
        }
    }

    private static final String assessmentPrefix = "rubric_assessment[";
    private static final String pointsPostFix = "][points]";
    private static final String commentsPostFix = "][comments]";

    private static Map<String, String> generateRubricAssessmentQueryMap(Map<String, RubricCriterionAssessment> rubricAssessment) {
        Map<String, String> map = new HashMap<>();
        for (Map.Entry<String, RubricCriterionAssessment> entry : rubricAssessment.entrySet()) {
            RubricCriterionAssessment rating = entry.getValue();
            String criterionId = entry.getKey();
            if (rating.getPoints() != null) {
                map.put(assessmentPrefix + criterionId + pointsPostFix, String.valueOf(rating.getPoints()));
            }
            map.put(assessmentPrefix + criterionId + commentsPostFix, TextUtils.isEmpty(rating.getComments()) ? "" :rating.getComments());
        }
        return map;
    }
}
