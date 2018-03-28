/*
 * Copyright (C) 2017 - present  Instructure, Inc.
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

package com.instructure.androidpolling.app.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;

import com.devspark.appmsg.AppMsg;
import com.instructure.androidpolling.app.R;
import com.instructure.androidpolling.app.activities.BaseActivity;
import com.instructure.androidpolling.app.activities.FragmentManagerActivity;
import com.instructure.androidpolling.app.activities.PublishPollActivity;
import com.instructure.androidpolling.app.model.AnswerValue;
import com.instructure.androidpolling.app.util.Constants;
import com.instructure.androidpolling.app.util.SwipeDismissTouchListener;
import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.models.PollChoice;
import com.instructure.canvasapi2.models.PollChoiceResponse;
import com.instructure.canvasapi2.managers.PollsManager;
import com.instructure.canvasapi2.models.Poll;
import com.instructure.canvasapi2.models.PollResponse;
import com.instructure.canvasapi2.models.PollSession;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.LinkHeaders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public class AddQuestionFragment extends ParentFragment {

    @BindView(R.id.edit_question) EditText editQuestion;
    @BindView(R.id.scrollViewAnswers) ScrollView scrollView;
    @BindView(R.id.answerContainer) LinearLayout answerContainer;
    @BindView(R.id.publishPoll) Button publishPoll;
    @BindView(R.id.savePoll) Button savePoll;

    private Button addAnswerBtn;

    private StatusCallback<PollResponse> pollCallback;
    private StatusCallback<PollChoiceResponse> pollChoiceCallback;

    private SwipeDismissTouchListener touchListener;

    private boolean editPoll = false; //set when we're editing a poll that has already be created
    private boolean shouldPublish = false;
    private Poll poll;

    private HashMap<View, AnswerValue> answerMap = new HashMap<>();
    private ArrayList<Long> idDeleteList = new ArrayList<>();

    //num polls created
    private int pollChoicesCreated = 0;
    private boolean submittingPoll = false;
    private LayoutInflater inflater;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_add_question, container, false);
        ButterKnife.bind(this, rootView);
        this.inflater = inflater;
        setHasOptionsMenu(true);

        setupViews();
        setupClickListeners();

        setupCallbacks();
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);

        //set an animation for adding list items
        LayoutAnimationController controller
                = AnimationUtils.loadLayoutAnimation(
                getActivity(), R.anim.list_layout_controller);

        answerContainer.setLayoutAnimation(controller);
        checkBundle(getArguments());

        ((BaseActivity)getActivity()).setActionBarTitle(getString(R.string.createPoll));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == Constants.PUBLISH_POLL_SUCCESS) {
            //try to go to the poll result
            if(data != null) {
                PollSession session = data.getExtras().getParcelable(Constants.POLL_SESSION);
                PollResultsFragment pollResultsFragment = new PollResultsFragment();
                Bundle bundle = new Bundle();
                bundle.putParcelable(Constants.POLL_DATA, poll);
                bundle.putParcelable(Constants.POLL_SESSION, session);
                pollResultsFragment.setArguments(bundle);
                ((FragmentManagerActivity)getActivity()).removeFragment(this);
                ((FragmentManagerActivity)getActivity()).swapFragments(pollResultsFragment, PollResultsFragment.class.getSimpleName());

                return;
            }
            //if there are multiple poll sessions, go to the list
            getActivity().onBackPressed();
        }
        else if(resultCode == Constants.PUBLISH_POLL_SUCCESS_MULTIPLE) {
            PollSessionListFragment pollSessionListFragment = new PollSessionListFragment();
            Bundle bundle = new Bundle();
            bundle.putParcelable(Constants.POLL_DATA, poll);
            pollSessionListFragment.setArguments(bundle);
            ((FragmentManagerActivity)getActivity()).removeFragment(this);
            ((FragmentManagerActivity)getActivity()).swapFragments(pollSessionListFragment, PollResultsFragment.class.getSimpleName());

        }
    }

    private void updatePollInfo() {
        poll.setQuestion(editQuestion.getText().toString());
        PollsManager.updatePoll(poll.getId(), editQuestion.getText().toString(), pollCallback, true);

        //delete the ids of the removed choices
        for(Long id :idDeleteList) {

            PollsManager.deletePollChoice(poll.getId(), id, new StatusCallback<ResponseBody>() {}, true);
        }
    }
    private void setupViews() {
        addAnswerBtn = new Button(getActivity());
        addAnswerBtn.setText(getString(R.string.addAnswer));
        addAnswerBtn.setBackgroundResource(R.drawable.dashed_button);
        addAnswerBtn.setTextColor(getResources().getColor(R.color.canvaspollingtheme_color));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins((int)getResources().getDimension(R.dimen.smallViewMargin), 0, (int)getResources().getDimension(R.dimen.viewMargin), 0);
        addAnswerBtn.setLayoutParams(layoutParams);

        answerContainer.addView(addAnswerBtn);

    }
    private void setupClickListeners() {
        addAnswerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //add another answer
                addAnswer();
            }
        });

        publishPoll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //make sure they entered a question
                if (editQuestion.getEditableText().toString().trim().length() == 0) {
                    AppMsg.makeText(getActivity(), getString(R.string.msgAddQuestion), AppMsg.STYLE_WARNING).show();
                    return;
                }
                if (checkForAnswers()) {
                    shouldPublish = true;
                    if (editPoll) {
                        updatePollInfo();
                    } else {
                        PollsManager.createPoll(editQuestion.getText().toString(), new StatusCallback<PollResponse>() {
                            @Override
                            public void onResponse(@NonNull retrofit2.Response<PollResponse> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {

                            }
                        }, true);
                    }
                }
            }
        });

        savePoll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //make sure they entered a question
                if (editQuestion.getEditableText().toString().trim().length() == 0) {
                    AppMsg.makeText(getActivity(), getString(R.string.msgAddQuestion), AppMsg.STYLE_WARNING).show();
                    return;
                }

                //make sure there are at least 2 answers
                if (checkForAnswers()) {
                    submittingPoll = true;
                    //disable the button so they can't publish multiples of the same poll
                    savePoll.setEnabled(false);
                    if (editPoll) {
                        updatePollInfo();
                    } else {
                        //API call to create poll
                        PollsManager.createPoll(editQuestion.getText().toString(), pollCallback, true);
                    }
                }
            }
        });
    }

    private boolean checkForAnswers() {
        int validEntries = 0;
        //go through the different items, make sure there is something in at least 2 edit texts
        for (AnswerValue value : answerMap.values()) {

            if(value.getValue().trim().length() > 0) {
                validEntries++;
            }
            //if we have 2, we're good to go
            if(validEntries >= 2) {
                return true;
            }
        }
        AppMsg.makeText(getActivity(), getString(R.string.twoAnswersRequired), AppMsg.STYLE_WARNING).show();
        return false;
    }

    private void addAnswer() {
        addAnswer("", false);
    }

    private void addAnswer(String answer) {
        addAnswer(answer, false);
    }

    private void addAnswer(String answer, boolean isCorrect) {
        addAnswer(answer, isCorrect, 0);
    }

    private void addAnswer(final String answer, boolean isCorrect, long pollChoiceId) {
        final AnswerValue value = new AnswerValue();
        value.setValue(answer);
        value.setSelected(isCorrect);
        value.setPollChoiceId(pollChoiceId);
        value.setPosition(answerMap.size());
        //inflate the view from xml and add it to the scrollview's container
        final View answerView = inflater.inflate(R.layout.add_answer_layout, null, false);
        if(answerView == null) {
            return;
        }

        //setup the touchListener for swipe to dismiss
        touchListener = new SwipeDismissTouchListener(
                answerView,
                null, // Optional token/cookie object
                new SwipeDismissTouchListener.DismissCallbacks() {
                    @Override
                    public boolean canDismiss(Object token) {
                        return true;
                    }

                    public void onDismiss(View view, Object token) {
                        answerContainer.removeView(view);

                        //if the poll has already been created, we need to delete the pollchoice with an api call too. But we don't want
                        //to delete the poll choices until they save the poll, so we'll add them to a list
                        if(editPoll && answerMap.containsKey(view) && answerMap.get(view).getPollChoiceId() != 0) {
                            idDeleteList.add(answerMap.get(view).getPollChoiceId());
                        }
                        answerMap.remove(view);
                        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(
                                Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                });
        answerView.setOnTouchListener(touchListener);
        //have to have an onclick listener for swipetodismiss to work
        answerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        //get the edit text
        EditText answerEditText = (EditText)answerView.findViewById(R.id.edit_answer_added);
        answerEditText.setText(answer);
        answerEditText.setOnTouchListener(touchListener);
        answerEditText.requestFocus();

        //add a text watcher to the edit text so we save the values as the user types
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                value.setValue(charSequence.toString());
                answerMap.put(answerView, value);
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        };

        answerEditText.addTextChangedListener(watcher);

        //since the radio buttons aren't in a radioGroup, we have to manage their state by ourselves. Also, with
        //this we can let the user de-select a radio button
        final RadioButton selected = (RadioButton)answerView.findViewById(R.id.correctAnswerAdded);
        selected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(answerMap.get(view.getParent().getParent()).isSelected()) {
                    selected.setSelected(false);
                    selected.setChecked(false);
                    answerMap.get(view.getParent().getParent()).setSelected(false);
                }
                else {
                    selected.setSelected(true);

                    //uncheck all the other ones
                    for (int i = 0; i < answerMap.size(); i++) {
                        View answer = answerContainer.getChildAt(i);
                        RadioButton selected = (RadioButton)answer.findViewById(R.id.correctAnswerAdded);
                        selected.setChecked(false);
                        answerMap.get(answer).setSelected(false);
                    }
                    selected.setChecked(true);
                    //make sure this one is checked
                    answerMap.get(view.getParent().getParent()).setSelected(true);
                }
            }
        });

        selected.setChecked(value.isSelected());

        //add the view and answerValue to a map so we can remove the value later on swipeToDismiss
        answerMap.put(answerView, value);

        //we want to add the view before the "add answer" button
        int index = answerMap.size();
        if(index > 0) {
            index--;
        }

        //make sure we're not going to get an index out of bounds exception
        if(index > answerContainer.getChildCount()) {
            index = answerContainer.getChildCount();
        }

        answerContainer.addView(answerView, index);
    }

    private void checkBundle(Bundle bundle) {
        if(bundle != null) {
            poll = bundle.getParcelable(Constants.POLL_BUNDLE);
            if(poll != null) {
                editPoll = true;
                //we're editing a poll that has already been created

                scrollView.setVisibility(View.VISIBLE);
                editQuestion.setText(poll.getQuestion());

                ArrayList<PollChoice> pollChoices = bundle.getParcelableArrayList(Constants.POLL_CHOICES);
                if(pollChoices != null) {
                    for (PollChoice pollChoice : pollChoices) {
                        addAnswer(pollChoice.getText(), pollChoice.is_correct(), pollChoice.getId());
                    }
                }

                //update the actionbar
                getActivity().invalidateOptionsMenu();
            }
        }
    }

    //get the number of AnswerValues that actually have a value
    private int getNumValidEntries() {
        int validEntries = 0;
        for(AnswerValue value : answerMap.values()) {
            if(value.getValue().trim().length() > 0) {
                validEntries++;
            }
        }

        return validEntries;
    }

    private void setupCallbacks() {

        pollCallback = new StatusCallback<PollResponse>() {
            @Override
            public void onResponse(@NonNull Response<PollResponse> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                if(getActivity() == null || type.isCache()) return;

                List<Poll> pollList = response.body().getPolls();
                if(pollList == null) {
                    return;
                }
                pollChoicesCreated = 0;
                poll = pollList.get(0);
                //now create the poll choices if we're not in edit mode
                if(!editPoll) {
                    for (AnswerValue answerValue : answerMap.values()) {
                        PollsManager.createPollChoice(pollList.get(0).getId(), answerValue.getValue(), answerValue.isSelected(), answerValue.getPosition(), pollChoiceCallback, true);
                    }
                }
                else {
                    for (AnswerValue answerValue : answerMap.values()) {
                        //if they added some answers we won't have a pollChoiceId
                        if(answerValue.getPollChoiceId() == 0) {
                            PollsManager.createPollChoice(pollList.get(0).getId(), answerValue.getValue(), answerValue.isSelected(), answerValue.getPosition(), pollChoiceCallback, true);
                        } else {
                            PollsManager.updatePollChoice(pollList.get(0).getId(), answerValue.getPollChoiceId(), answerValue.getValue(), answerValue.isSelected(), answerValue.getPosition(), pollChoiceCallback, true);
                        }
                    }
                }
            }

            @Override
            public void onFail(@Nullable Call<PollResponse> call, @NonNull Throwable error, @Nullable Response response) {
                submittingPoll = false;
                getActivity().invalidateOptionsMenu();
            }
        };

        pollChoiceCallback = new StatusCallback<PollChoiceResponse>() {
            @Override
            public void onResponse(@NonNull Response<PollChoiceResponse> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                if(getActivity() == null || type.isCache()) return;

                pollChoicesCreated++;

                //after we've created all the polls that we need
                if(pollChoicesCreated == getNumValidEntries()) {

                    if(editPoll || shouldPublish) {
                        if(shouldPublish) {
                            shouldPublish = false;
                            startActivityForResult(PublishPollActivity.createIntent(getActivity(), poll.getId()), Constants.PUBLISH_POLL_REQUEST);
                        }
                        else {
                            //we just came from the PollSessionsFragment
                            //removing the fragment will make it reload it's data
                            ((BaseActivity) getActivity()).removeFragment(QuestionListFragment.class.getSimpleName());
                        }
                    }
                    else {
                        //now create the poll choices
                        //removing the fragment will make it reload it's data
                        ((BaseActivity) getActivity()).removeFragment(QuestionListFragment.class.getSimpleName());
                    }
                }
            }

            @Override
            public void onFail(@Nullable Call<PollChoiceResponse> call, @NonNull Throwable error, @Nullable Response response) {
                submittingPoll = false;
                getActivity().invalidateOptionsMenu();
            }
        };
    }
}
