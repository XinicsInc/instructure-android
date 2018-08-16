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

package com.instructure.candroid.adapter;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import com.google.gson.internal.LinkedTreeMap;
import com.instructure.candroid.R;
import com.instructure.candroid.activity.NavigationActivity;
import com.instructure.candroid.binders.QuizEssayBinder;
import com.instructure.candroid.binders.QuizFileUploadBinder;
import com.instructure.candroid.binders.QuizMatchingBinder;
import com.instructure.candroid.binders.QuizMultiAnswerBinder;
import com.instructure.candroid.binders.QuizMultiChoiceBinder;
import com.instructure.candroid.binders.QuizMultipleDropdownBinder;
import com.instructure.candroid.binders.QuizNumericalBinder;
import com.instructure.candroid.binders.QuizTextOnlyBinder;
import com.instructure.candroid.binders.SubmitButtonBinder;
import com.instructure.interactions.Navigation;
import com.instructure.candroid.fragment.InternalWebviewFragment;
import com.instructure.candroid.fragment.QuizStartFragment;
import com.instructure.candroid.holders.QuizEssayViewHolder;
import com.instructure.candroid.holders.QuizFileUploadViewHolder;
import com.instructure.candroid.holders.QuizMatchingViewHolder;
import com.instructure.candroid.holders.QuizMultiChoiceViewHolder;
import com.instructure.candroid.holders.QuizMultipleDropdownViewHolder;
import com.instructure.candroid.holders.QuizNumericalViewHolder;
import com.instructure.candroid.holders.QuizTextOnlyViewHolder;
import com.instructure.candroid.holders.SubmitButtonViewHolder;
import com.instructure.candroid.interfaces.QuizFileRemovedListener;
import com.instructure.candroid.interfaces.QuizFileUploadListener;
import com.instructure.candroid.interfaces.QuizPostEssay;
import com.instructure.candroid.interfaces.QuizPostMatching;
import com.instructure.candroid.interfaces.QuizPostMultiAnswers;
import com.instructure.candroid.interfaces.QuizPostMultiChoice;
import com.instructure.candroid.interfaces.QuizPostMultipleDropdown;
import com.instructure.candroid.interfaces.QuizPostNumerical;
import com.instructure.candroid.interfaces.QuizSubmit;
import com.instructure.candroid.interfaces.QuizToggleFlagState;
import com.instructure.candroid.util.RouterUtils;
import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.managers.QuizManager;
import com.instructure.canvasapi2.models.Attachment;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.QuizQuestion;
import com.instructure.canvasapi2.models.QuizSubmission;
import com.instructure.canvasapi2.models.QuizSubmissionQuestion;
import com.instructure.canvasapi2.models.QuizSubmissionQuestionResponse;
import com.instructure.canvasapi2.models.QuizSubmissionResponse;
import com.instructure.canvasapi2.utils.ApiPrefs;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.LinkHeaders;
import com.instructure.pandautils.utils.ColorKeeper;
import com.instructure.pandautils.utils.Const;
import com.instructure.pandautils.views.CanvasWebView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public class QuizSubmissionQuestionListRecyclerAdapter extends BaseListRecyclerAdapter<QuizSubmissionQuestion, RecyclerView.ViewHolder> {

    private static final int ESSAY = 0;
    private static final int MULTI_CHOICE = 1;
    private static final int TEXT_ONLY = 2;
    private static final int SUBMIT_BUTTON = 13370;
    private static final int MULTI_ANSWER = 3;
    private static final int MATCHING = 4;
    private static final int NUMERICAL = 5;
    private static final int FILE_UPLOAD = 6;
    private static final int MULTIPLE_DROPDOWNS = 7;

    private boolean shouldLetAnswer;
    //need this for the token so we can answer questions
    private QuizSubmission quizSubmission;

    // region Order Work-around
    // Since BaseListRecyclerAdapter uses a sorted list to store the list items, there has to be something to order them by.
    // When adding Quiz questions to our adapter, we maintain a hashmap with the position they were inserted into our Adapter.
    // We can then override the item comparator to use the default ordering provided by the API.
    private HashMap<Long, Integer> mInsertedOrderHash = new HashMap<>();

    private CanvasContext canvasContext;
    private CanvasWebView.CanvasEmbeddedWebViewCallback embeddedWebViewCallback;
    private CanvasWebView.CanvasWebViewClientCallback webViewClientCallback;
    private QuizToggleFlagState flagStateCallback;
    private TreeSet<Long> answeredQuestions = new TreeSet<>();
    private QuizFileUploadListener quizFileUploadListener;
    //questionId -> attachment
    private Map<Long, Attachment> fileUploadMap = new HashMap<>();
    private Map<Long, Boolean> isLoadingMap = new HashMap<>();

    //Question State <QuestionId, List<AnswerId>> - Multi Answer Type
    private Map<Long, ArrayList<Long>> multiAnswerQuestionMap = new HashMap<>();

    public QuizSubmissionQuestionListRecyclerAdapter(final Activity context, List<QuizSubmissionQuestion> items, final CanvasContext canvasContext, boolean shouldLetAnswer, final QuizSubmission quizSubmission, QuizFileUploadListener quizFileUploadListener) {

        super(context, QuizSubmissionQuestion.class);
        this.canvasContext = canvasContext;
        this.shouldLetAnswer = shouldLetAnswer;
        this.quizSubmission = quizSubmission;
        this.quizFileUploadListener = quizFileUploadListener;

        embeddedWebViewCallback = new CanvasWebView.CanvasEmbeddedWebViewCallback() {
            @Override
            public void launchInternalWebViewFragment(String url) {
                InternalWebviewFragment.Companion.loadInternalWebView((FragmentActivity)context, ((Navigation) context), InternalWebviewFragment.Companion.createBundle(canvasContext, url, false));
            }

            @Override
            public boolean shouldLaunchInternalWebViewFragment(String url) {
                return true;
            }
        };

        answeredQuestions.clear();
        multiAnswerQuestionMap.clear();

        flagStateCallback = new QuizToggleFlagState() {
            @Override
            public void toggleFlagged(boolean flagQuestion, long questionId) {
                QuizManager.putFlagQuizQuestion(quizSubmission, questionId, flagQuestion, true, new StatusCallback<ResponseBody>() {});
            }
        };

        webViewClientCallback = new CanvasWebView.CanvasWebViewClientCallback() {
            @Override
            public void openMediaFromWebView(String mime, String url, String filename) {
                RouterUtils.canRouteInternally(context, url, ApiPrefs.getDomain(), true);
            }

            @Override
            public void onPageStartedCallback(WebView webView, String url) {

            }

            @Override
            public void onPageFinishedCallback(WebView webView, String url) {

            }

            @Override
            public void routeInternallyCallback(String url) {
                RouterUtils.canRouteInternally(context, url, ApiPrefs.getDomain(), true);
            }

            @Override
            public boolean canRouteInternallyDelegate(String url) {
                return RouterUtils.canRouteInternally(null, url, ApiPrefs.getDomain(), false);
            }
        };

        setItemCallback(new ItemComparableCallback<QuizSubmissionQuestion>() {
            @Override
            public int compare(QuizSubmissionQuestion o1, QuizSubmissionQuestion o2) {
                return mInsertedOrderHash.get(o1.getId()) - mInsertedOrderHash.get(o2.getId());
            }

            @Override
            public boolean areContentsTheSame(QuizSubmissionQuestion oldItem, QuizSubmissionQuestion newItem) {
                return super.areContentsTheSame(oldItem, newItem);
            }

            @Override
            public boolean areItemsTheSame(QuizSubmissionQuestion item1, QuizSubmissionQuestion item2) {
                return super.areItemsTheSame(item1, item2);
            }

            @Override
            public long getUniqueItemId(QuizSubmissionQuestion quizSubmissionQuestion) {
                return super.getUniqueItemId(quizSubmissionQuestion);
            }
        });
        // Add Quiz Questions
        int insertCount = -1;
        for(QuizSubmissionQuestion quizQuestion : items) {
            mInsertedOrderHash.put(quizQuestion.getId(), ++insertCount);
            add(quizQuestion);
        }
    }


    @Override
    public int getItemCount() {
        //if they can't answer we don't want to show them the submit button
        if(shouldLetAnswer) {
            return super.getItemCount() + 1;
        } else {
            return super.getItemCount();
        }
    }

    @Override
    public RecyclerView.ViewHolder createViewHolder(View v, int viewType) {

        switch (viewType) {
            case ESSAY:
                return new QuizEssayViewHolder(v);
            case MULTI_CHOICE:
                return new QuizMultiChoiceViewHolder(v);
            case SUBMIT_BUTTON:
                return new SubmitButtonViewHolder(v);
            case TEXT_ONLY:
                return new QuizTextOnlyViewHolder(v);
            case MULTI_ANSWER:
                return new QuizMultiChoiceViewHolder(v);
            case MATCHING:
                return new QuizMatchingViewHolder(v);
            case FILE_UPLOAD:
                return new QuizFileUploadViewHolder(v);
            case NUMERICAL:
                return new QuizNumericalViewHolder(v);
            case MULTIPLE_DROPDOWNS:
                return new QuizMultipleDropdownViewHolder(v);
        }

        return null;
    }

    public int getAnswerableQuestionCount() {
        int count = 0;
        if(shouldLetAnswer) {
            //the item count includes the submit button, which isn't a question type, so we don't want to include that one
            for (int i = 0; i < getItemCount() - 1; i++) {
                if (getItemAtPosition(i).getQuestionType() != QuizQuestion.QUESTION_TYPE.TEXT_ONLY) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Helper method to let the fragment know which question ids have been answered
     *
     * @return List of ids of questions that have been answered
     */
    public ArrayList<Long> getAnsweredQuestions() {
        return new ArrayList<>(answeredQuestions);
    }

    /**
     * Adds the question id to the list of ids so we have a complete list of answered questions. It is
     * possible that the user answered a question and then came back later to the quiz. This function
     * will help track those cases.
     *
     * @param quizSubmissionQuestion id of the question to add to the array list
     */
    private void addAnsweredQuestion(QuizSubmissionQuestion quizSubmissionQuestion) {
        if(quizSubmissionQuestion.getAnswer() != null && !quizSubmissionQuestion.getAnswer().equals("null")) {
            answeredQuestions.add(quizSubmissionQuestion.getId());
        }
    }

    private void addAnsweredQuestion(long questionId) {
        answeredQuestions.add(questionId);
    }

    private void removeAnsweredQuestion(long questionId) {
        answeredQuestions.remove(questionId);
    }

    @Override
    public void bindHolder(QuizSubmissionQuestion baseItem, RecyclerView.ViewHolder holder, int position) {

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if(position == size()) {
            bindTheHolder(null, holder, position);
        } else {
            QuizSubmissionQuestion baseItem = getItemAtPosition(position);
            bindTheHolder(baseItem, holder, position);
        }
    }

    public void bindTheHolder(final QuizSubmissionQuestion baseItem, RecyclerView.ViewHolder holder, int position) {
        int courseColor = ColorKeeper.getOrGenerateColor(canvasContext);

        if(position == super.getItemCount()) {
            //submit button

            SubmitButtonBinder.bind((SubmitButtonViewHolder)holder, getContext(), canvasContext, QuizSubmissionQuestionListRecyclerAdapter.this, new QuizSubmit() {
                @Override
                public void submitQuiz() {

                    QuizManager.postQuizSubmit(canvasContext, quizSubmission, true, new StatusCallback<QuizSubmissionResponse>() {

                        @Override
                        public void onResponse(@NonNull Response<QuizSubmissionResponse> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                            if(type == ApiType.CACHE) return;
                            // Submitted!
                            Toast.makeText(getContext(), R.string.quizSubmittedSuccessfully, Toast.LENGTH_SHORT).show();

                            // Go back to the startQuizFragment
                            ((Activity)getContext()).onBackPressed();
                            Fragment fragment = ((NavigationActivity)getContext()).getTopFragment();
                            if(fragment instanceof QuizStartFragment) {
                                ((QuizStartFragment)fragment).updateQuizInfo();
                            }
                        }

                        @Override
                        public void onFail(@Nullable Call<QuizSubmissionResponse> call, @NonNull Throwable error, @Nullable Response response) {
                            Toast.makeText(getContext(), R.string.quizSubmittedFailure, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
            return;
        }

        switch(baseItem.getQuestionType()) {
            case ESSAY:
            case SHORT_ANSWER:
                addEssayQuestion(baseItem, (QuizEssayViewHolder) holder, position, courseColor);
                break;
            case MUTIPLE_CHOICE:
            case TRUE_FALSE:
                addMultipleChoiceQuestion(baseItem, (QuizMultiChoiceViewHolder) holder, position, courseColor);
                break;
            case TEXT_ONLY:
                QuizTextOnlyBinder.bind((QuizTextOnlyViewHolder) holder, baseItem);
                break;
            case MULTIPLE_ANSWERS:
                addMultipleAnswerQuestion((QuizMultiChoiceViewHolder) holder, position, courseColor);
                break;
            case MATCHING:
                addMatchingQuestion(baseItem, (QuizMatchingViewHolder) holder, position, courseColor);
                break;
            case FILE_UPLOAD:
                addFileUploadQuestion(baseItem, (QuizFileUploadViewHolder) holder, position, courseColor);
                break;
            case NUMERICAL:
                addNumericalQuestion(baseItem, (QuizNumericalViewHolder) holder, position, courseColor);
                break;
            case MULTIPLE_DROPDOWNS:
                addMultipleDropdown(baseItem, (QuizMultipleDropdownViewHolder) holder, position, courseColor);
                break;
        }
    }

    private void addEssayQuestion(QuizSubmissionQuestion baseItem, QuizEssayViewHolder holder, int position, int courseColor) {
        addAnsweredQuestion(baseItem);
        QuizEssayBinder.bind(holder, baseItem, courseColor, position, shouldLetAnswer, getContext(), flagStateCallback, embeddedWebViewCallback, webViewClientCallback, new QuizPostEssay() {
            @Override
            public void postEssay(long questionId, String answer) {
                addAnsweredQuestion(questionId);

                QuizManager.postQuizQuestionEssay(quizSubmission, answer, questionId, true, new StatusCallback<QuizSubmissionQuestionResponse>() {});
            }
        });
    }

    private void addMultipleChoiceQuestion(QuizSubmissionQuestion baseItem, QuizMultiChoiceViewHolder holder, int position, int courseColor) {
        addAnsweredQuestion(baseItem);
        QuizMultiChoiceBinder.bind(holder, baseItem, courseColor, position, shouldLetAnswer, getContext(), embeddedWebViewCallback, webViewClientCallback, new QuizPostMultiChoice() {
            @Override
            public void postAnswer(final long questionId, long answerId) {
                addAnsweredQuestion(questionId);
                QuizManager.postQuizQuestionMultiChoice(quizSubmission, answerId, questionId, true, new StatusCallback<QuizSubmissionQuestionResponse>() {});
            }
        }, flagStateCallback);
    }

    private void addMultipleAnswerQuestion(QuizMultiChoiceViewHolder holder, int position, int courseColor) {
        addAnsweredQuestion(getItemAtPosition(position));
        QuizMultiAnswerBinder.bind(holder, getItemAtPosition(position), courseColor, position, shouldLetAnswer, getContext(), new QuizPostMultiAnswers() {

            @Override
            public void answerSelected(long questionId, long answerId) {
                addAnsweredQuestion(questionId);
                ArrayList<Long> answers = multiAnswerQuestionMap.get(questionId);
                if(answers == null) {
                    answers = new ArrayList<>();
                }
                answers.add(answerId);
                multiAnswerQuestionMap.put(questionId, answers);
                postQuizQuestionMultiAnswer(questionId, answers);
            }

            @Override
            public void answerUnselected(long questionId, long answerId) {
                final ArrayList<Long> answers = multiAnswerQuestionMap.get(questionId);
                if(answers != null) {
                    if (answers.size() > 0) {
                        answers.remove(answerId);
                        //If an item was removed reset it to the map to hold state
                        multiAnswerQuestionMap.put(questionId, answers);
                    } else if (answers.size() == 0) {
                        removeAnsweredQuestion(questionId);
                    }
                    postQuizQuestionMultiAnswer(questionId, answers);
                }
            }
        }, flagStateCallback);
    }

    private void addNumericalQuestion(QuizSubmissionQuestion baseItem, QuizNumericalViewHolder holder, int position, int courseColor) {
        addAnsweredQuestion(baseItem);
        QuizNumericalBinder.bind(holder, baseItem, courseColor, position, shouldLetAnswer, getContext(), flagStateCallback, embeddedWebViewCallback, webViewClientCallback, new QuizPostNumerical() {
            @Override
            public void postNumerical(long questionId, String answer) {
                addAnsweredQuestion(questionId);

                //note: this is the same as the essay question on purpose. Numerical is just text.
                QuizManager.postQuizQuestionEssay(quizSubmission, answer, questionId, true, new StatusCallback<QuizSubmissionQuestionResponse>() {});
            }
        });
    }

    private void addMultipleDropdown(final QuizSubmissionQuestion baseItem, QuizMultipleDropdownViewHolder holder, int position, int courseColor) {
        addAnsweredQuestion(baseItem);
        QuizMultipleDropdownBinder.bind(holder, baseItem, courseColor, position, shouldLetAnswer, getContext(), embeddedWebViewCallback, webViewClientCallback, new QuizPostMultipleDropdown() {
            @Override
            public void postMultipleDropdown(final long questionId, HashMap<String, Long> answers) {

                QuizManager.postQuizQuestionMultipleDropdown(quizSubmission, questionId, answers, true, new StatusCallback<QuizSubmissionQuestionResponse>() {

                    @Override
                    public void onResponse(@NonNull Response<QuizSubmissionQuestionResponse> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                        if(type == ApiType.CACHE) return;

                        QuizSubmissionQuestionResponse quizSubmissionQuestionResponse = response.body();

                        if (quizSubmissionQuestionResponse.getQuizSubmissionQuestions() != null) {
                            for (QuizSubmissionQuestion question : quizSubmissionQuestionResponse.getQuizSubmissionQuestions()) {
                                if (baseItem.getId() == question.getId()) {
                                    baseItem.setAnswer(question.getAnswer());
                                }
                            }
                            //make sure each answer has a match
                            int numAnswers = 0;
                            // API returns a LinkedTreeMap
                            for (String map : ((LinkedTreeMap<String, String>) baseItem.getAnswer()).values()) {

                                if (map != null && !map.equals("null")) {
                                    numAnswers++;
                                }

                            }
                            if (numAnswers == ((LinkedTreeMap<String, String>) baseItem.getAnswer()).size()) {
                                addAnsweredQuestion(questionId);
                            } else {
                                removeAnsweredQuestion(questionId);
                            }
                        }
                    }
                });
            }
        }, flagStateCallback);

    }

    private void postQuizQuestionMultiAnswer(final long questionId, final ArrayList<Long> answers) {
        QuizManager.postQuizQuestionMultiAnswer(quizSubmission, questionId, answers, true, new StatusCallback<QuizSubmissionQuestionResponse>() {});
    }

    private void addMatchingQuestion(final QuizSubmissionQuestion baseItem, QuizMatchingViewHolder holder, int position, int courseColor) {
        addAnsweredQuestion(baseItem);
        QuizMatchingBinder.bind(holder, baseItem, courseColor, position, shouldLetAnswer, getContext(), embeddedWebViewCallback, webViewClientCallback, new QuizPostMatching() {

            @Override
            public void postMatching(final long questionId, HashMap<Long, Integer> answers) {

                QuizManager.postQuizQuestionMatching(quizSubmission, questionId, answers, true, new StatusCallback<QuizSubmissionQuestionResponse>(){
                    @Override
                    public void onResponse(@NonNull Response<QuizSubmissionQuestionResponse> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                        if(type == ApiType.CACHE) return;

                        final QuizSubmissionQuestionResponse quizSubmissionQuestionResponse = response.body();

                        if (quizSubmissionQuestionResponse.getQuizSubmissionQuestions() != null) {
                            for (QuizSubmissionQuestion question : quizSubmissionQuestionResponse.getQuizSubmissionQuestions()) {
                                if (baseItem.getId() == question.getId()) {
                                    baseItem.setAnswer(question.getAnswer());
                                }
                            }
                            //make sure each answer has a match
                            int numAnswers = 0;
                            // API returns an ArrayList of LinkedTreeMaps
                            for (LinkedTreeMap<String, String> map : ((ArrayList<LinkedTreeMap<String, String>>) baseItem.getAnswer())) {

                                if (map.get(Const.QUIZ_MATCH_ID) != null && !map.get(Const.QUIZ_MATCH_ID).equals("null")) {
                                    numAnswers++;
                                }

                            }
                            if (numAnswers == baseItem.getAnswers().length) {
                                addAnsweredQuestion(questionId);
                            } else {
                                removeAnsweredQuestion(questionId);
                            }
                        }
                    }
                });
            }

        }, flagStateCallback);
    }

    private void addFileUploadQuestion(final QuizSubmissionQuestion baseItem, QuizFileUploadViewHolder holder, int position, int courseColor) {
        addAnsweredQuestion(baseItem);
        boolean isLoading = false;
        if(isLoadingMap.size() != 0 && isLoadingMap.get(baseItem.getId()) != null){
            isLoading = isLoadingMap.get(baseItem.getId());
        }
        QuizFileUploadBinder.bind(holder, getContext(), webViewClientCallback,
                embeddedWebViewCallback, baseItem, courseColor, flagStateCallback, canvasContext,
                position, quizFileUploadListener, shouldLetAnswer,
                fileUploadMap.get(baseItem.getId()),
                isLoading,
                new QuizFileRemovedListener() {
                    @Override
                    public void quizFileUploadRemoved(long quizQuestionId, int position) {
                        //Remove from the UI
                        fileUploadMap.remove(quizQuestionId);
                        isLoadingMap.put(quizQuestionId, false);
                        getItemAtPosition(position).setAnswer(null);
                        notifyDataSetChanged();

                        //Remove from answered Questions
                        removeAnsweredQuestion(quizQuestionId);

                        //Update API, recognizes -1 was a clear
                        postQuizQuestionFileUpload(quizQuestionId, -1);
                    }
                });
    }

    private void postQuizQuestionFileUpload(final long questionId, final long attachmentId){
        QuizManager.postQuizQuestionFileUpload(canvasContext, quizSubmission, attachmentId, questionId, true, new StatusCallback<QuizSubmissionQuestionResponse>() {
            @Override
            public void onResponse(@NonNull Response<QuizSubmissionQuestionResponse> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                if(type.isAPI()) addAnsweredQuestion(questionId);
            }
        });
    }

    @Override
    public int itemLayoutResId(int viewType) {
        switch (viewType) {
            case ESSAY:
                return QuizEssayViewHolder.adapterResId();
            case MULTI_CHOICE:
                return QuizMultiChoiceViewHolder.adapterResId();
            case SUBMIT_BUTTON:
                return SubmitButtonViewHolder.adapterResId();
            case TEXT_ONLY:
                return QuizTextOnlyViewHolder.adapterResId();
            case MULTI_ANSWER:
                //multiple choice and multiple answer the same, so use the same view holder
                return QuizMultiChoiceViewHolder.adapterResId();
            case MATCHING:
                return QuizMatchingViewHolder.adapterResId();
            case FILE_UPLOAD:
                return QuizFileUploadViewHolder.adapterResId();
            case NUMERICAL:
                return QuizNumericalViewHolder.adapterResId();
            case MULTIPLE_DROPDOWNS:
                return QuizMultipleDropdownViewHolder.adapterResId();
        }
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        if(position == super.getItemCount()) {
            //submit button
            return SUBMIT_BUTTON;
        }
        switch((getItemAtPosition(position)).getQuestionType()) {
            case ESSAY:
            case SHORT_ANSWER:
                return ESSAY;
            case MUTIPLE_CHOICE:
            case TRUE_FALSE:
                return MULTI_CHOICE;
            case TEXT_ONLY:
                return TEXT_ONLY;
            case MULTIPLE_ANSWERS:
                return MULTI_ANSWER;
            case MATCHING:
                return MATCHING;
            case FILE_UPLOAD:
                return FILE_UPLOAD;
            case NUMERICAL:
                return NUMERICAL;
            case MULTIPLE_DROPDOWNS:
                return MULTIPLE_DROPDOWNS;

        }

        return 0;
    }

    public void setFileUploadForQuiz(long quizQuestionId, Attachment attachment, int position) {
        //set data set
        fileUploadMap.put(quizQuestionId, attachment);
        isLoadingMap.put(quizQuestionId, false);

        //attempt to accurately update list
        if(position == -1) {
            notifyDataSetChanged();
        } else {
            notifyItemChanged(position);
        }
        //update API
        postQuizQuestionFileUpload(quizQuestionId, attachment.getId());
    }

    public void setIsLoading(long quizQuestionId, boolean isLoading, int position) {
        isLoadingMap.put(quizQuestionId, isLoading);
        notifyItemChanged(position);
    }


    @Override
    public void setupCallbacks() {
    }

    @Override
    public void loadFirstPage() {

    }

    @Override
    public void loadNextPage(String nextURL) {

    }
}
