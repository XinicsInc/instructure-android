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
import com.instructure.canvasapi2.models.Quiz;
import com.instructure.canvasapi2.models.QuizQuestion;
import com.instructure.canvasapi2.models.QuizSubmission;
import com.instructure.canvasapi2.models.QuizSubmissionQuestionResponse;
import com.instructure.canvasapi2.models.QuizSubmissionResponse;
import com.instructure.canvasapi2.models.QuizSubmissionTime;
import com.instructure.canvasapi2.models.post_models.QuizPostBodyWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;

public class QuizAPI {

    interface QuizInterface {
        @GET("{contextType}/{contextId}/quizzes")
        Call<List<Quiz>> getFirstPageQuizzesList(@Path("contextType") String contextType, @Path("contextId") long contextId);

        @GET
        Call<List<Quiz>> getNextPageQuizzesList(@Url String nextURL);

        @GET("{contextType}/{contextId}/quizzes/{quizId}")
        Call<Quiz> getDetailedQuiz(@Path("contextType") String contextType, @Path("contextId") long contextId, @Path("quizId") long quizId);

        @GET
        Call<Quiz> getDetailedQuizByUrl(@Url String quizUrl);

        @GET("courses/{courseId}/quizzes/{quizId}/questions")
        Call<List<QuizQuestion>> getFirstPageQuizQuestions(@Path("courseId") long contextId, @Path("quizId") long quizId);

        @GET
        Call<List<QuizQuestion>> getNextPageQuizQuestions(@Url String nextUrl);

        @POST("courses/{courseId}/quizzes/{quizId}/submissions?preview=1")
        Call<QuizSubmissionResponse> startQuizPreview(@Path("courseId") long contextId, @Path("quizId") long quizId);

        @GET("quiz_submissions/{quizSubmissionId}/questions")
        Call<QuizSubmissionQuestionResponse> getFirstPageSubmissionQuestions(@Path("quizSubmissionId") long quizSubmissionId);

        @GET
        Call<QuizSubmissionQuestionResponse> getNextPageSubmissionQuestions(@Url String nextURL);

        @GET("courses/{courseId}/quizzes")
        Call<List<Quiz>> getFirstPageQuizzes(@Path("courseId") long contextId);

        @GET
        Call<List<Quiz>> getNextPageQuizzes(@Url String nextUrl);

        @GET("courses/{courseId}/quizzes/{quizId}")
        Call<Quiz> getQuiz(@Path("courseId") long courseId, @Path("quizId") long quizId);

        @PUT("courses/{courseId}/quizzes/{quizId}")
        Call<Quiz> editQuiz(
                @Path("courseId") long courseId,
                @Path("quizId") long quizId,
                @Body QuizPostBodyWrapper body);

        //FIXME: MIGRATION can be removed and replaced with other getFirstPageQuizSubmissions()
        @GET("courses/{courseId}/quizzes/{quizId}/submissions")
        Call<QuizSubmissionResponse> getFirstPageQuizSubmissions(@Path("courseId") Long courseId, @Path("quizId") Long quizId);

        @GET
        Call<QuizSubmissionResponse> getNextPageQuizSubmissions(@Url String nextUrl);

        @POST("{contextType}/{contextId}/quizzes/{quizId}/submissions/{submissionId}/complete")
        Call<QuizSubmissionResponse> submitQuiz(
                @Path("contextType") String contextType,
                @Path("contextId") long contextId,
                @Path("quizId") long quizId,
                @Path("submissionId") long submissionId,
                @Query("attempt") int attempt,
                @Query("validation_token") String token);

        @GET("{contextType}/{contextId}/quizzes/{quizId}/submissions")
        Call<QuizSubmissionResponse> getFirstPageQuizSubmissions(@Path("contextType") String contextType, @Path("contextId") long contextId, @Path("quizId") long quizId);

        @POST("{contextType}/{contextId}/quizzes/{quizId}/submissions/{submissionId}/events")
        Call<ResponseBody> postQuizStartedEvent(
                @Path("contextType") String contextType,
                @Path("contextId") long contextId,
                @Path("quizId") long quizId,
                @Path("submissionId") long submissionId,
                @Query("quiz_submission_events[][event_type]") String sessionStartedString,
                @Query("quiz_submission_events[][event_data][user_agent]") String userAgentString);

        @POST("{contextType}/{contextId}/quizzes/{quizId}/submissions")
        Call<QuizSubmissionResponse> startQuiz(
                @Path("contextType") String contextType,
                @Path("contextId") long contextId,
                @Path("quizId") long quizId);

        @GET("{contextType}/{contextId}/quizzes/{quizId}/submissions/{submissionId}/time")
        Call<QuizSubmissionTime> getQuizSubmissionTime(
                @Path("contextType") String contextType,
                @Path("contextId") long contextId,
                @Path("quizId") long quizId,
                @Path("submissionId") long submissionId);

        @POST("{contextType}/{contextId}/quizzes/{quizId}/submissions/{submissionId}/complete")
        Call<QuizSubmissionResponse> postQuizSubmit(
                @Path("contextType") String contextType,
                @Path("contextId") long contextId,
                @Path("quizId") long quizId,
                @Path("submissionId") long submissionId,
                @Query("attempt") int attempt,
                @Query("validation_token") String token);

        @POST("quiz_submissions/{quizSubmissionId}/questions")
        Call<QuizSubmissionQuestionResponse> postQuizQuestionFileUpload(
                @Path("quizSubmissionId") long quizSubmissionId,
                @Query("attempt") int attempt,
                @Query("validation_token") String token,
                @Query("quiz_questions[][id]") long questionId,
                @Query("quiz_questions[][answer]") String answer);

        @POST
        Call<QuizSubmissionQuestionResponse> postQuizQuestionUrl(@Url String url);

        @POST("quiz_submissions/{quizSubmissionId}/questions")
        Call<QuizSubmissionQuestionResponse> postQuizQuestionEssay(
                @Path("quizSubmissionId") long quizSubmissionId,
                @Query("attempt") int attempt,
                @Query("validation_token") String token,
                @Query("quiz_questions[][id]") long questionId,
                @Query("quiz_questions[][answer]") String answer);

        @POST("quiz_submissions/{quizSubmissionId}/questions")
        Call<QuizSubmissionQuestionResponse> postQuizQuestionMultiChoice(
                @Path("quizSubmissionId") long quizSubmissionId,
                @Query("attempt") int attempt,
                @Query("validation_token") String token,
                @Query("quiz_questions[][id]") long questionId,
                @Query("quiz_questions[][answer]") long answer);

        @PUT("quiz_submissions/{quizSubmissionId}/questions/{questionId}/{flag}")
        Call<ResponseBody> putFlagQuizQuestion(
                @Path("quizSubmissionId") long quizSubmissionId,
                @Path("questionId") long questionId,
                @Path("flag") String flag,
                @Query("attempt") int attempt,
                @Query("validation_token") String token);
    }

    public static void getQuizQuestions(long contextId, long quizId, @NonNull RestBuilder adapter, @NonNull StatusCallback<List<QuizQuestion>> callback, @NonNull RestParams params) {
        if (StatusCallback.isFirstPage(callback.getLinkHeaders())) {
            callback.addCall(adapter.build(QuizInterface.class, params).getFirstPageQuizQuestions(contextId, quizId)).enqueue(callback);
        } else if (StatusCallback.moreCallsExist(callback.getLinkHeaders()) && callback.getLinkHeaders() != null) {
            callback.addCall(adapter.build(QuizInterface.class, params).getNextPageQuizQuestions(callback.getLinkHeaders().nextUrl)).enqueue(callback);
        }
    }

    public static void getQuiz(long courseId, long quizId, @NonNull RestBuilder adapter, @NonNull StatusCallback<Quiz> callback, @NonNull RestParams params) {
        callback.addCall(adapter.build(QuizInterface.class, params).getQuiz(courseId, quizId)).enqueue(callback);
    }

    /**
     * Start the quiz in preview mode. For teachers only.
     *
     * @param contextId
     * @param quizId
     * @param adapter
     * @param callback
     * @param params
     */
    public static void startQuizPreview(long contextId, long quizId, @NonNull RestBuilder adapter, @NonNull StatusCallback<QuizSubmissionResponse> callback, @NonNull RestParams params) {
        callback.addCall(adapter.build(QuizInterface.class, params).startQuizPreview(contextId, quizId)).enqueue(callback);
    }

    public static void getQuizSubmissionQuestions(long quizSubmissionId, @NonNull RestBuilder adapter, @NonNull StatusCallback<QuizSubmissionQuestionResponse> callback, @NonNull RestParams params) {
        if (StatusCallback.isFirstPage(callback.getLinkHeaders())) {
            callback.addCall(adapter.build(QuizInterface.class, params).getFirstPageSubmissionQuestions(quizSubmissionId)).enqueue(callback);
        } else if (StatusCallback.moreCallsExist(callback.getLinkHeaders()) && callback.getLinkHeaders() != null) {
            callback.addCall(adapter.build(QuizInterface.class, params).getNextPageSubmissionQuestions(callback.getLinkHeaders().nextUrl)).enqueue(callback);
        }
    }

    public static void getFirstPageSubmissionQuestions(long quizSubmissionId, @NonNull RestBuilder adapter, @NonNull RestParams params, @NonNull StatusCallback<QuizSubmissionQuestionResponse> callback) {
        callback.addCall(adapter.build(QuizInterface.class, params).getFirstPageSubmissionQuestions(quizSubmissionId)).enqueue(callback);
    }

    public static void getNextPageSubmissionQuestions(String nextUrl, @NonNull RestBuilder adapter, @NonNull RestParams params, @NonNull StatusCallback<QuizSubmissionQuestionResponse> callback) {
        callback.addCall(adapter.build(QuizInterface.class, params).getNextPageSubmissionQuestions(nextUrl)).enqueue(callback);
    }

    public static void getQuizzes(long contextId, @NonNull RestBuilder adapter, @NonNull StatusCallback<List<Quiz>> callback, @NonNull RestParams params) {
        if (StatusCallback.isFirstPage(callback.getLinkHeaders())) {
            callback.addCall(adapter.build(QuizInterface.class, params).getFirstPageQuizzes(contextId)).enqueue(callback);
        } else if (StatusCallback.moreCallsExist(callback.getLinkHeaders()) && callback.getLinkHeaders() != null) {
            callback.addCall(adapter.build(QuizInterface.class, params).getNextPageQuizzes(callback.getLinkHeaders().nextUrl)).enqueue(callback);
        }
    }

    public static void getFirstPageQuizzes(long contextId, boolean forceNetwork, @NonNull RestBuilder adapter, @NonNull StatusCallback<List<Quiz>> callback) {
        RestParams params = new RestParams.Builder()
                .withShouldIgnoreToken(false)
                .withPerPageQueryParam(true)
                .withForceReadFromNetwork(forceNetwork)
                .build();

        callback.addCall(adapter.build(QuizInterface.class, params).getFirstPageQuizzes(contextId)).enqueue(callback);
    }

    public static void getNextPageQuizzes(boolean forceNetwork, String nextUrl, RestBuilder adapter, @NonNull StatusCallback<List<Quiz>> callback) {
        RestParams params = new RestParams.Builder()
                .withShouldIgnoreToken(false)
                .withPerPageQueryParam(true)
                .withForceReadFromNetwork(forceNetwork)
                .build();

        callback.addCall(adapter.build(QuizInterface.class, params).getNextPageQuizzes(nextUrl)).enqueue(callback);
    }

    public static void editQuiz(long courseId, long assignmentId, QuizPostBodyWrapper body, RestBuilder adapter, final StatusCallback<Quiz> callback, RestParams params) {
        callback.addCall(adapter.buildSerializeNulls(QuizInterface.class, params).editQuiz(courseId, assignmentId, body)).enqueue(callback);
    }

    public static void getQuizSubmissions(CanvasContext canvasContext, long quizId, @NonNull RestBuilder adapter, @NonNull StatusCallback<QuizSubmissionResponse> callback, @NonNull RestParams params) {
        if (StatusCallback.isFirstPage(callback.getLinkHeaders())) {
            callback.addCall(adapter.build(QuizInterface.class, params).getFirstPageQuizSubmissions(canvasContext.getId(), quizId)).enqueue(callback);
        } else if (StatusCallback.moreCallsExist(callback.getLinkHeaders()) && callback.getLinkHeaders() != null) {
            callback.addCall(adapter.build(QuizInterface.class, params).getNextPageQuizSubmissions(callback.getLinkHeaders().nextUrl)).enqueue(callback);
        }
    }

    public static void getFirstPageQuizList(CanvasContext canvasContext, @NonNull RestBuilder adapter,@NonNull RestParams params, @NonNull StatusCallback<List<Quiz>> callback) {
        callback.addCall(adapter.build(QuizInterface.class, params).getFirstPageQuizzesList(canvasContext.apiContext(), canvasContext.getId())).enqueue(callback);
    }

    public static void getNextPageQuizList(@NonNull String nextUrl, @NonNull RestBuilder adapter,@NonNull RestParams params, @NonNull StatusCallback<List<Quiz>> callback) {
        callback.addCall(adapter.build(QuizInterface.class, params).getNextPageQuizzesList(nextUrl)).enqueue(callback);
    }

    public static void getDetailedQuiz(@NonNull CanvasContext canvasContext, long quizId, @NonNull RestBuilder adapter,@NonNull RestParams params, @NonNull StatusCallback<Quiz> callback) {
        callback.addCall(adapter.build(QuizInterface.class, params).getDetailedQuiz(canvasContext.apiContext(), canvasContext.getId(), quizId)).enqueue(callback);
    }

    public static void getDetailedQuizByUrl(@NonNull String quizUrl, @NonNull RestBuilder adapter, @NonNull RestParams params, @NonNull StatusCallback<Quiz> callback) {
        callback.addCall(adapter.build(QuizInterface.class, params).getDetailedQuizByUrl(quizUrl)).enqueue(callback);
    }

    public static void submitQuiz(@NonNull CanvasContext canvasContext,
                                  @NonNull QuizSubmission quizSubmission,
                                  @NonNull RestBuilder adapter,
                                  @NonNull RestParams params,
                                  @NonNull StatusCallback<QuizSubmissionResponse> callback) {
        callback.addCall(adapter.build(QuizInterface.class, params).submitQuiz(
                canvasContext.apiContext(),
                canvasContext.getId(),
                quizSubmission.getQuizId(),
                quizSubmission.getId(),
                quizSubmission.getAttempt(),
                quizSubmission.getValidationToken())).enqueue(callback);
    }

    public static void getFirstPageQuizSubmissions(CanvasContext canvasContext, long quizId, @NonNull RestBuilder adapter, @NonNull RestParams params, @NonNull StatusCallback<QuizSubmissionResponse> callback) {
        callback.addCall(adapter.build(QuizInterface.class, params).getFirstPageQuizSubmissions(canvasContext.apiContext(), canvasContext.getId(), quizId)).enqueue(callback);
    }

    public static void postQuizStartedEvent(
            @NonNull CanvasContext canvasContext,
            long quizId,
            long submissionId,
            @NonNull String userAgent,
            @NonNull RestBuilder adapter,
            @NonNull RestParams params,
            @NonNull StatusCallback<ResponseBody> callback) {

        callback.addCall(adapter.build(QuizInterface.class, params)
                .postQuizStartedEvent(canvasContext.apiContext(), canvasContext.getId(), quizId, submissionId, "android_session_started", userAgent)).enqueue(callback);
    }

    public static void startQuiz(
            @NonNull CanvasContext canvasContext,
            long quizId,
            @NonNull RestBuilder adapter,
            @NonNull RestParams params,
            @NonNull StatusCallback<QuizSubmissionResponse> callback) {

        callback.addCall(adapter.build(QuizInterface.class, params).startQuiz(canvasContext.apiContext(), canvasContext.getId(), quizId)).enqueue(callback);
    }

    public static void getQuizSubmissionTime(
            @NonNull CanvasContext canvasContext,
            long quizId,
            long submissionId,
            @NonNull RestBuilder adapter,
            @NonNull RestParams params,
            @NonNull StatusCallback<QuizSubmissionTime> callback) {

        callback.addCall(adapter.build(QuizInterface.class, params).getQuizSubmissionTime(canvasContext.apiContext(), canvasContext.getId(), quizId, submissionId)).enqueue(callback);
    }

    public static void postQuizSubmit(
            @NonNull CanvasContext canvasContext,
            QuizSubmission quizSubmission,
            @NonNull RestBuilder adapter,
            @NonNull RestParams params,
            @NonNull StatusCallback<QuizSubmissionResponse> callback) {

        callback.addCall(adapter.build(QuizInterface.class, params).postQuizSubmit(
                canvasContext.apiContext(), canvasContext.getId(), quizSubmission.getQuizId(), quizSubmission.getId(), quizSubmission.getAttempt(), quizSubmission.getValidationToken())).enqueue(callback);
    }

    public static void postQuizQuestionFileUpload(
            @NonNull CanvasContext canvasContext,
            QuizSubmission quizSubmission,
            long answer,
            long questionId,
            @NonNull RestBuilder adapter,
            @NonNull RestParams params,
            @NonNull StatusCallback<QuizSubmissionQuestionResponse> callback) {

        callback.addCall(adapter.build(QuizInterface.class, params).postQuizQuestionFileUpload(
                quizSubmission.getId(),
                quizSubmission.getAttempt(),
                quizSubmission.getValidationToken(),
                questionId,
                (answer == -1) ? "" : Long.toString(answer)
        )).enqueue(callback);
    }


    public static void postQuizQuestionMatching(
            QuizSubmission quizSubmission,
            long questionId,
            HashMap<Long, Integer> answers,
            @NonNull RestBuilder adapter,
            @NonNull RestParams params,
            @NonNull StatusCallback<QuizSubmissionQuestionResponse> callback) {

        callback.addCall(adapter.build(QuizInterface.class, params).postQuizQuestionUrl(
                buildMatchingUrl(quizSubmission.getId(), quizSubmission.getAttempt(), quizSubmission.getValidationToken(), questionId, answers))).enqueue(callback);
    }

    public static void postQuizQuestionMultipleDropdown(
            QuizSubmission quizSubmission,
            long questionId,
            HashMap<String, Long> answers,
            @NonNull RestBuilder adapter,
            @NonNull RestParams params,
            @NonNull StatusCallback<QuizSubmissionQuestionResponse> callback) {

        callback.addCall(adapter.build(QuizInterface.class, params).postQuizQuestionUrl(
                buildMultipleDropdownUrl(quizSubmission.getId(), quizSubmission.getAttempt(), quizSubmission.getValidationToken(), questionId, answers))).enqueue(callback);
    }

    public static void postQuizQuestionMultiAnswers(
            QuizSubmission quizSubmission,
            long questionId,
            ArrayList<Long> answers,
            @NonNull RestBuilder adapter,
            @NonNull RestParams params,
            @NonNull StatusCallback<QuizSubmissionQuestionResponse> callback) {

        callback.addCall(adapter.build(QuizInterface.class, params).postQuizQuestionUrl(
                buildMultiAnswerUrl(quizSubmission.getId(), quizSubmission.getAttempt(), quizSubmission.getValidationToken(), questionId, answers))).enqueue(callback);
    }

    public static void postQuizQuestionEssay(
            QuizSubmission quizSubmission,
            long questionId,
            String answer,
            @NonNull RestBuilder adapter,
            @NonNull RestParams params,
            @NonNull StatusCallback<QuizSubmissionQuestionResponse> callback) {

        callback.addCall(adapter.build(QuizInterface.class, params).postQuizQuestionEssay(
                quizSubmission.getId(),
                quizSubmission.getAttempt(),
                quizSubmission.getValidationToken(),
                questionId,
                answer)).enqueue(callback);
    }

    public static void postQuizQuestionMultiChoice(
            long submissionId,
            int attempts,
            long questionId,
            long answer,
            @NonNull String token,
            @NonNull RestBuilder adapter,
            @NonNull RestParams params,
            @NonNull StatusCallback<QuizSubmissionQuestionResponse> callback) {

        callback.addCall(adapter.build(QuizInterface.class, params).postQuizQuestionMultiChoice(
                submissionId,
                attempts,
                token,
                questionId,
                answer)).enqueue(callback);
    }

    public static void putFlagQuizQuestion(
            QuizSubmission quizSubmission,
            long questionId,
            boolean flagQuestion,
            @NonNull RestBuilder adapter,
            @NonNull RestParams params,
            @NonNull StatusCallback<ResponseBody> callback) {

        callback.addCall(adapter.build(QuizInterface.class, params).putFlagQuizQuestion(
                quizSubmission.getId(),
                questionId,
                flagQuestion ? "flag" : "unflag",
                quizSubmission.getAttempt(),
                quizSubmission.getValidationToken())).enqueue(callback);
    }

    private static String buildMultiAnswerUrl(long quizSubmissionId, int attempt, String validationToken, long questionId, ArrayList<Long> answers) {
        // Build the entire relative URL because Retrofit 2 forcfeully escapes @Path params. It will end up looking like:
        // quiz_submissions/{submission_id}/questions?attempt={attempt}&validation_token={validation_token}&quiz_questions[][id]={question_id}&quiz_questions[][answer][]={answer_id}...
        StringBuilder builder = new StringBuilder();
        builder.append("quiz_submissions/");
        builder.append(Long.toString(quizSubmissionId));
        builder.append("/questions");
        builder.append("?");
        builder.append("attempt=");
        builder.append(Integer.toString(attempt));
        builder.append("&");
        builder.append("validation_token=");
        builder.append(validationToken);
        builder.append("&");
        builder.append("quiz_questions[][id]=");
        builder.append(Long.toString(questionId));
        builder.append("&");
        for(Long answer : answers) {
            builder.append("quiz_questions[][answer][]");
            builder.append("=");
            builder.append(Long.toString(answer));
            builder.append("&");
        }

        String answerString = builder.toString();
        if(answerString.endsWith("&")) {
            answerString = answerString.substring(0, answerString.length() - 1);
        }
        return answerString;
    }

    private static String buildMatchingUrl(long quizSubmissionId, int attempt, String validationToken, long questionId, HashMap<Long, Integer> answers) {
        // Build the entire relative URL because Retrofit 2 forcfeully escapes @Path params. It will end up looking like:
        // quiz_submissions/{submission_id}/questions?attempt={attempt}&validation_token={validation_token}&quiz_questions[][id]={question_id}&quiz_questions[][answer][][answer_id]={answer_id}&quiz_questions[][answer][][match_id]={match_id}...
        StringBuilder builder = new StringBuilder();
        builder.append("quiz_submissions/");
        builder.append(Long.toString(quizSubmissionId));
        builder.append("/questions");
        builder.append("?");
        builder.append("attempt=");
        builder.append(Integer.toString(attempt));
        builder.append("&");
        builder.append("validation_token=");
        builder.append(validationToken);
        builder.append("&");
        builder.append("quiz_questions[][id]=");
        builder.append(Long.toString(questionId));
        builder.append("&");
        //loop through the HashMap that contains the list of answers and their matches that the user selected
        for(Map.Entry<Long, Integer> answer : answers.entrySet()) {
            builder.append("quiz_questions[][answer][][answer_id]");
            builder.append("=");
            builder.append(Long.toString(answer.getKey()));
            builder.append("&");
            builder.append("quiz_questions[][answer][][match_id]");
            builder.append("=");
            builder.append(Integer.toString(answer.getValue()));
            builder.append("&");

        }

        String answerString = builder.toString();
        if(answerString.endsWith("&")) {
            answerString = answerString.substring(0, answerString.length() - 1);
        }
        return answerString;
    }

    private static String buildMultipleDropdownUrl(long quizSubmissionId, int attempt, String validationToken, long questionId, HashMap<String, Long> answers) {
        // Build the entire relative URL because Retrofit 2 forcfeully escapes @Path params. It will end up looking like:
        // quiz_submissions/{submission_id}/questions?attempt={attempt}&validation_token={validation_token}&quiz_questions[][id]={question_id}&quiz_questions[][answer][{answerKey}]={answerValue}...
        StringBuilder builder = new StringBuilder();
        builder.append("quiz_submissions/");
        builder.append(Long.toString(quizSubmissionId));
        builder.append("/questions");
        builder.append("?");
        builder.append("attempt=");
        builder.append(Integer.toString(attempt));
        builder.append("&");
        builder.append("validation_token=");
        builder.append(validationToken);
        builder.append("&");
        builder.append("quiz_questions[][id]=");
        builder.append(Long.toString(questionId));
        builder.append("&");
        //loop through the HashMap that contains the list of answers and their matches that the user selected
        for(Map.Entry<String, Long> answer : answers.entrySet()) {
            builder.append("quiz_questions[][answer][");
            builder.append(answer.getKey());
            builder.append("]");
            builder.append("=");
            builder.append(Long.toString(answer.getValue()));
            builder.append("&");
        }

        String answerString = builder.toString();
        if(answerString.endsWith("&")) {
            answerString = answerString.substring(0, answerString.length() - 1);
        }
        return answerString;
    }
}
