/*
 * Copyright (C) 2016 - present Instructure, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.instructure.candroid.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.instructure.candroid.R;
import com.instructure.interactions.Navigation;
import com.instructure.interactions.FragmentInteractions;
import com.instructure.candroid.util.FragUtils;
import com.instructure.candroid.util.Param;
import com.instructure.candroid.util.RouterUtils;
import com.instructure.candroid.view.CanvasLoading;
import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.managers.QuizManager;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.Course;
import com.instructure.canvasapi2.models.Quiz;
import com.instructure.canvasapi2.models.QuizSubmission;
import com.instructure.canvasapi2.models.QuizSubmissionResponse;
import com.instructure.canvasapi2.models.QuizSubmissionTime;
import com.instructure.canvasapi2.models.User;
import com.instructure.canvasapi2.utils.ApiPrefs;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.DateHelper;
import com.instructure.canvasapi2.utils.LinkHeaders;
import com.instructure.canvasapi2.utils.NumberHelper;
import com.instructure.canvasapi2.utils.pageview.PageView;
import com.instructure.canvasapi2.utils.pageview.PageViewUrlParam;
import com.instructure.pandautils.utils.Const;
import com.instructure.pandautils.utils.PandaViewUtils;
import com.instructure.pandautils.utils.ViewStyler;
import com.instructure.pandautils.views.CanvasWebView;

import java.util.ArrayList;
import java.util.HashMap;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

@PageView(url = "{canvasContext}/quizzes/{quizId}")
public class QuizStartFragment extends ParentFragment {

    private Toolbar toolbar;

    private TextView quizTitle;
    private CanvasWebView quizDetails;

    private TextView quizTurnedIn;
    private TextView quizTimeLimit;

    //detail textViews
    private TextView quizPointsPossibleDetails;
    private TextView quizQuestionCountDetails;
    private TextView quizAttemptDetails;
    private TextView quizDueDateDetails;
    private TextView quizTurnedInDetails;
    private TextView quizUnlockedDetails;
    private TextView quizTimeLimitDetails;

    private RelativeLayout quizUnlockedContainer;
    private RelativeLayout quizTimeLimitContainer;
    private RelativeLayout quizTurnedInContainer;

    private Button viewResults;
    private TextView quizUnlocked;
    private CanvasLoading canvasLoading;

    private Button next;

    private Course course;
    private Quiz quiz;

    private QuizSubmission quizSubmission;
    private boolean shouldStartQuiz = false;
    private boolean shouldLetAnswer = true;
    private QuizSubmissionTime quizSubmissionTime;

    private StatusCallback<QuizSubmissionResponse> quizSubmissionResponseCanvasCallback;
    private StatusCallback<QuizSubmissionResponse> quizStartResponseCallback;
    private StatusCallback<ResponseBody> quizStartSessionCallback;
    private StatusCallback<QuizSubmissionTime> quizSubmissionTimeCanvasCallback;

    private CanvasWebView.CanvasWebViewClientCallback webViewClientCallback;
    private CanvasWebView.CanvasEmbeddedWebViewCallback embeddedWebViewCallback;

    @PageViewUrlParam(name = "quizId")
    private Long getQuizId() {
        return quiz != null ? quiz.getId() : 0;
    }

    @Override
    @NonNull
    public String title() {
        return quiz != null ? quiz.getTitle() : getString(R.string.quizzes);
    }

    @Override
    @NonNull
    public FragmentInteractions.Placement getFragmentPlacement() {
        return FragmentInteractions.Placement.DETAIL;
    }

    //Currently there isn't a way to know how to decide if we want to route
    //to this fragment or the BasicQuizViewFragment.
    @Override
    public boolean allowBookmarking() {
        return false;
    }

    @Override
    @NonNull
    public HashMap<String, String> getParamForBookmark() {
        HashMap<String, String> map = super.getParamForBookmark();
        map.put(Param.QUIZ_ID, Long.toString(quiz.getId()));
        return map;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View rootView = inflater.inflate(R.layout.quiz_start, container, false);
        toolbar = rootView.findViewById(R.id.toolbar);
        setupViews(rootView);
        setupCallbacks();

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        canvasLoading.setVisibility(View.VISIBLE);
        if(savedInstanceState != null){
            course = (Course)getCanvasContext();
            quiz = savedInstanceState.getParcelable(Const.QUIZ);
        }
        if(quiz != null) QuizManager.getQuizSubmissions(course, quiz.getId(), true, quizSubmissionResponseCanvasCallback);
    }

    @Override
    public void applyTheme() {
        PandaViewUtils.setupToolbarBackButton(toolbar, this);
        setupToolbarMenu(toolbar);
        ViewStyler.themeToolbar(getActivity(), toolbar, getCanvasContext());
    }

    // Called after submitting a quiz
    public void updateQuizInfo() {
        canvasLoading.setVisibility(View.VISIBLE);
        //don't let them try to start the quiz until the data loads
        next.setEnabled(false);
        quizSubmissionResponseCanvasCallback.reset(); // Reset to clear out any link headers
        QuizManager.getQuizSubmissions(course, quiz.getId(), true, quizSubmissionResponseCanvasCallback);
    }

    private void setupViews(View rootView) {

        quizTitle = rootView.findViewById(R.id.quiz_title);
        quizDetails = rootView.findViewById(R.id.quiz_details);

        quizTurnedIn = rootView.findViewById(R.id.quiz_turned_in);
        quizUnlocked = rootView.findViewById(R.id.quiz_unlocked);
        quizTimeLimit = rootView.findViewById(R.id.quiz_time_limit);

        quizPointsPossibleDetails = rootView.findViewById(R.id.quiz_points_details);
        quizAttemptDetails = rootView.findViewById(R.id.quiz_attempt_details);
        quizQuestionCountDetails = rootView.findViewById(R.id.quiz_question_count_details);
        quizDueDateDetails = rootView.findViewById(R.id.quiz_due_details);
        quizTurnedInDetails = rootView.findViewById(R.id.quiz_turned_in_details);
        quizUnlockedDetails = rootView.findViewById(R.id.quiz_unlocked_details);
        quizTimeLimitDetails = rootView.findViewById(R.id.quiz_time_limit_details);

        quizTurnedInContainer = rootView.findViewById(R.id.quiz_turned_in_container);
        quizUnlockedContainer = rootView.findViewById(R.id.quiz_unlocked_container);
        quizTimeLimitContainer = rootView.findViewById(R.id.quiz_time_limit_container);

        canvasLoading = rootView.findViewById(R.id.loading);


        next = rootView.findViewById(R.id.next);

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(quiz.isLockedForUser()) {
                    if(quiz.getLockExplanation() != null) {
                        showToast(quiz.getLockExplanation());
                    }
                    return;
                }

                if(shouldStartQuiz) {
                    QuizManager.startQuiz(course, quiz.getId(), true, quizStartResponseCallback);
                    // If the user hits the back button, we don't want them to try to start the quiz again
                    shouldStartQuiz = false;
                } else if (quizSubmission != null) {
                    showQuiz();
                } else {
                    getLockedMessage();
                }
            }
        });

        viewResults = rootView.findViewById(R.id.quiz_results);

        viewResults.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle bundle = InternalWebviewFragment.Companion.createBundle(course, quiz.getUrl(), false);

                Navigation navigation = getNavigation();
                if (navigation != null) {
                    InternalWebviewFragment fragment = FragUtils.getFrag(InternalWebviewFragment.class, bundle);
                    //we don't want it to route internally, it will pop open the sliding drawer and route back the to same place
                    fragment.setShouldRouteInternally(false);
                    navigation.addFragment(fragment);
                }
            }
        });
    }

    public void populateQuizInfo() {
        quizTitle.setText(quiz.getTitle());
        toolbar.setTitle(title());

        quizDetails.formatHTML(quiz.getDescription(), "");
        quizDetails.setBackgroundColor(getResources().getColor(R.color.transparent));
        // Set some callbacks in case there is a link in the quiz description. We want it to open up in a new
        // InternalWebViewFragment
        quizDetails.setCanvasEmbeddedWebViewCallback(embeddedWebViewCallback);

        quizDetails.setCanvasWebViewClientCallback(webViewClientCallback);

        quizQuestionCountDetails.setText(NumberHelper.formatInt(quiz.getQuestionCount()));

        quizPointsPossibleDetails.setText(quiz.getPointsPossible());

        if(quiz.getAllowedAttempts() == -1) {
            quizAttemptDetails.setText(getString(R.string.unlimited));
        } else {
            quizAttemptDetails.setText(NumberHelper.formatInt(quiz.getAllowedAttempts()));
        }

        if(quiz.getDueAt() != null) {
            quizDueDateDetails.setText(DateHelper.getDateTimeString(getActivity(), quiz.getDueAt()));
        } else {
            quizDueDateDetails.setText(getString(R.string.toDoNoDueDate));
        }

        if(quiz.getUnlockAt() != null) {
            quizUnlocked.setText(getString(R.string.unlockedAt));
            quizUnlockedDetails.setText(DateHelper.getDateTimeString(getActivity(), quiz.getUnlockAt()));
        } else {
            quizUnlockedContainer.setVisibility(View.GONE);
        }

        if(quiz.getTimeLimit() != 0) {
            quizTimeLimit.setText(getString(R.string.timeLimit));
            quizTimeLimitDetails.setText(NumberHelper.formatInt(quiz.getTimeLimit()));
        } else {
            quizTimeLimitContainer.setVisibility(View.GONE);
        }
    }

    public void onNoNetwork() {
        if (canvasLoading != null) {
            canvasLoading.displayNoConnection(true);
        }
    }

    private void setupCallbacks() {
        webViewClientCallback = new CanvasWebView.CanvasWebViewClientCallback() {
            @Override
            public void openMediaFromWebView(String mime, String url, String filename) {
                openMedia(mime, url, filename);
            }

            @Override
            public void onPageFinishedCallback(WebView webView, String url) {

            }

            @Override
            public void onPageStartedCallback(WebView webView, String url) {


            }

            @Override
            public boolean canRouteInternallyDelegate(String url) {
                return RouterUtils.canRouteInternally(null, url, ApiPrefs.getDomain(), false);
            }

            @Override
            public void routeInternallyCallback(String url) {
                RouterUtils.canRouteInternally(getActivity(), url, ApiPrefs.getDomain(), true);
            }
        };

        embeddedWebViewCallback = new CanvasWebView.CanvasEmbeddedWebViewCallback() {
            @Override
            public void launchInternalWebViewFragment(String url) {

                InternalWebviewFragment.Companion.loadInternalWebView(getActivity(), getNavigation(), InternalWebviewFragment.Companion.createBundle(course, url, false));
            }

            @Override
            public boolean shouldLaunchInternalWebViewFragment(String url) {
                return true;
            }
        };

        quizSubmissionTimeCanvasCallback = new StatusCallback<QuizSubmissionTime>() {

            @Override
            public void onResponse(@NonNull Response<QuizSubmissionTime> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                if(type == ApiType.CACHE) return;
                QuizStartFragment.this.quizSubmissionTime = quizSubmissionTime;
                QuizManager.getQuizSubmissions(course, quiz.getId(), true, quizSubmissionResponseCanvasCallback);
            }
        };
        quizSubmissionResponseCanvasCallback = new StatusCallback<QuizSubmissionResponse>() {

            @Override
            public void onResponse(@NonNull Response<QuizSubmissionResponse> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                if(type == ApiType.CACHE) return;
                final QuizSubmissionResponse quizSubmissionResponse = response.body();

                //since this is a student app, make sure they only have their own submissions (if they're siteadmin it'll be different)
                final ArrayList<QuizSubmission> submissions = new ArrayList<>();
                final User user = ApiPrefs.getUser();
                if(user != null) {
                    for (QuizSubmission submission : quizSubmissionResponse.getQuizSubmissions()) {
                        if (submission.getUserId() == user.getId()){
                            submissions.add(submission);
                        }
                    }
                }

                quizSubmissionResponse.setQuizSubmissions(submissions);
                if (quizSubmissionResponse.getQuizSubmissions() == null || quizSubmissionResponse.getQuizSubmissions().size() == 0) {
                    // No quiz submissions, let the user start the quiz.

                    // They haven't turned it in yet, so don't show the turned-in view
                    quizTurnedInContainer.setVisibility(View.GONE);
                    shouldStartQuiz = true;
                    next.setVisibility(View.VISIBLE);
                    next.setEnabled(true);
                } else {
                    // We should have at least 1 submission
                    quizSubmission = quizSubmissionResponse.getQuizSubmissions().get(quizSubmissionResponse.getQuizSubmissions().size() - 1);

                    next.setEnabled(true);

                    final boolean hasUnlimitedAttempts = quiz.getAllowedAttempts() == -1;
                    final boolean teacherUnlockedQuizAttempts = quizSubmission.isManuallyUnlocked(); // Teacher can manually unlock a quiz for an individual student
                    final boolean hasMoreAttemptsLeft = quizSubmission.getAttemptsLeft() > 0;

                    final boolean canTakeQuizAgain = hasUnlimitedAttempts | teacherUnlockedQuizAttempts | hasMoreAttemptsLeft;

                    if(quiz.getHideResults() == Quiz.HIDE_RESULTS_TYPE.ALWAYS && !canTakeQuizAgain) {
                        // Don't let the user see the questions if they've exceeded their attempts
                        next.setVisibility(View.GONE);
                    } else if(quiz.getHideResults() == Quiz.HIDE_RESULTS_TYPE.AFTER_LAST_ATTEMPT && !canTakeQuizAgain) {
                        // They can only see the results after their last attempt, and that hasn't happened yet
                        next.setVisibility(View.GONE);
                    }

                    // They can -take- the quiz if there's no finished time and they have attempts left, OR the teacher has unlocked the quiz for them

                    // If they've finished the quiz and have no more attempt chances, or the teacher has locked the quiz, then they're done
                    // -1 allowed attempts == unlimited
                    if (quizSubmission.getFinishedAt() != null && !canTakeQuizAgain) {
                        // They've finished the quiz and they can't take it anymore; let them see results
                        next.setVisibility(View.VISIBLE);
                        next.setText(getString(R.string.viewQuestions));
                        shouldLetAnswer = false;
                    } else {
                        // They are allowed to take the quiz...
                        next.setVisibility(View.VISIBLE);

                        if (quizSubmission.getFinishedAt() != null) {
                            shouldStartQuiz = true;
                            next.setText(getString(R.string.takeQuizAgain));
                        } else {
                            // Let the user resume their quiz
                            next.setText(getString(R.string.resumeQuiz));
                        }
                    }

                    if (quizSubmission.getFinishedAt() != null) {
                        quizTurnedIn.setText(getString(R.string.turnedIn));
                        quizTurnedInDetails.setText(DateHelper.getDateTimeString(getActivity(), quizSubmission.getFinishedAt()));
                        // The user has turned in the quiz, let them see the results
                        viewResults.setVisibility(View.VISIBLE);

                    } else {
                        quizTurnedInContainer.setVisibility(View.GONE);
                    }

                    // Weird hack where if the time expires and the user hasn't submitted it doesn't let you start the quiz
                    if(quizSubmission.getWorkflowState() == QuizSubmission.WORKFLOW_STATE.UNTAKEN && (quizSubmission.getEndAt() != null && (quizSubmissionTime != null && quizSubmissionTime.getTimeLeft() > 0))) {
                        next.setEnabled(false);
                        //submit the quiz for them
                        QuizManager.submitQuiz(course, quizSubmission, true, new StatusCallback<QuizSubmissionResponse>() {
                            @Override
                            public void onResponse(@NonNull Response<QuizSubmissionResponse> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                                if (type == ApiType.CACHE) return;
                                //the user has turned in the quiz, let them see the results
                                viewResults.setVisibility(View.VISIBLE);
                                next.setEnabled(true);
                                shouldStartQuiz = true;
                                next.setText(getString(R.string.takeQuizAgain));
                                QuizSubmissionResponse quizResponse = response.body();

                                // Since this is a student app, make sure they only have their own submissions (if they're siteadmin it'll be different)
                                final ArrayList<QuizSubmission> submissions = new ArrayList<>();
                                final User user = ApiPrefs.getUser();
                                if (user != null) {
                                    for (QuizSubmission submission : quizResponse.getQuizSubmissions()) {
                                        if (submission.getUserId() == user.getId()) {
                                            submissions.add(submission);
                                        }
                                    }
                                }

                                quizResponse.setQuizSubmissions(submissions);

                                if (quizResponse.getQuizSubmissions() != null && quizResponse.getQuizSubmissions().size() > 0) {
                                    quizSubmission = quizResponse.getQuizSubmissions().get(quizResponse.getQuizSubmissions().size() - 1);
                                }
                            }
                        });
                    }

                    // If the user can only see results once and they have seen it, don't let them view the questions
                    if (quiz.isOneTimeResults() && quizSubmission.hasSeenResults()) {
                        next.setVisibility(View.GONE);
                    }

                    if (quiz.isLockedForUser()) {
                        shouldStartQuiz = false;
                        next.setText(getString(R.string.assignmentLocked));
                    }
                }

                populateQuizInfo();

                canvasLoading.setVisibility(View.GONE);
            }

            @Override
            public void onFail(@Nullable Call<QuizSubmissionResponse> call, @NonNull Throwable error, @Nullable Response response) {
                canvasLoading.setVisibility(View.GONE);
                //if a quiz is excused we get a 401 error when trying to get the submissions. This is a workaround until we have an excused field
                //on quizzes.
                if (response != null && response.code() == 401) {
                    populateQuizInfo();
                    //there is a not authorized error, so don't let them start the quiz
                    next.setVisibility(View.GONE);
                }
            }
        };

        quizStartResponseCallback = new StatusCallback<QuizSubmissionResponse>() {

            @Override
            public void onResponse(@NonNull Response<QuizSubmissionResponse> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                if(response.code() == 200 && type == ApiType.API) {
                    // We want to show the quiz here, but we need to get the quizSubmissionId first so our
                    // api call for the QuizQuestionsFragment knows which questions to get
                    StatusCallback<QuizSubmissionResponse> quizSubmissionResponseCallback = new StatusCallback<QuizSubmissionResponse>() {

                        @Override
                        public void onResponse(@NonNull Response<QuizSubmissionResponse> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                            QuizSubmissionResponse quizSubmissionResponse = response.body();
                            if(quizSubmissionResponse != null && quizSubmissionResponse.getQuizSubmissions() != null &&
                                    quizSubmissionResponse.getQuizSubmissions().size() > 0) {
                                quizSubmission = quizSubmissionResponse.getQuizSubmissions().get(quizSubmissionResponse.getQuizSubmissions().size() - 1);
                                if(quizSubmission != null) {
                                    showQuiz();
                                } else {
                                    getLockedMessage();
                                }
                            }
                        }
                    };

                    QuizManager.getFirstPageQuizSubmissions(course, quiz.getId(), false, quizSubmissionResponseCallback);
                }
            }

            @Override
            public void onFail(@Nullable Call<QuizSubmissionResponse> call, @NonNull Throwable error, @Nullable Response response) {
                if(response != null && response.code() == 403) {
                    // Forbidden
                    // Check to see if it's because of IP restriction or bad access code or either
                    getLockedMessage();
                }
            }
        };

        quizStartSessionCallback = new StatusCallback<ResponseBody>() {
            // Alerting the user that we couldn't post the start session event doesn't really make sense. If something went wrong the logs will
            // be off on the admin/teacher side
        };
    }


    private void getLockedMessage() {
        // Check to see if it's because of IP restriction or bad access code or either
        if(quiz.getIpFilter() != null && quiz.getAccessCode() == null) {
            showToast(R.string.lockedIPAddress);
        } else if(quiz.getIpFilter() == null && quiz.getAccessCode() != null) {
            showToast(R.string.lockedInvalidAccessCode);
        } else {
            // Something went wrong (no data possibly)
            showToast(R.string.cantStartQuiz);
        }
    }

    private void showQuiz() {
        Navigation navigation = getNavigation();
        if(navigation != null){

            // Post the android session started event
            QuizManager.postQuizStartedEvent(getCanvasContext(), quizSubmission.getQuizId(), quizSubmission.getId(), true, quizStartSessionCallback);
            Bundle bundle = QuizQuestionsFragment.createBundle(getCanvasContext(), quiz, quizSubmission, shouldLetAnswer);

            navigation.addFragment(FragUtils.getFrag(QuizQuestionsFragment.class, bundle));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(Const.QUIZ, quiz);
    }

    @Override
    public void handleIntentExtras(Bundle extras) {
        super.handleIntentExtras(extras);

        if(extras == null){return;}

        course = (Course)getCanvasContext();
        quiz = extras.getParcelable(Const.QUIZ);
    }

    public static Bundle createBundle(CanvasContext canvasContext, Quiz quiz) {
        Bundle extras = createBundle(canvasContext);
        extras.putParcelable(Const.QUIZ, quiz);
        return extras;
    }
}
