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

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.devspark.appmsg.AppMsg;
import com.instructure.androidpolling.app.R;
import com.instructure.androidpolling.app.activities.PublishPollActivity;
import com.instructure.androidpolling.app.rowfactories.PollResultsRowFactory;
import com.instructure.androidpolling.app.util.Constants;
import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.managers.PollsManager;
import com.instructure.canvasapi2.models.Poll;
import com.instructure.canvasapi2.models.PollChoice;
import com.instructure.canvasapi2.models.PollChoiceResponse;
import com.instructure.canvasapi2.models.PollSession;
import com.instructure.canvasapi2.models.PollSessionResponse;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.LinkHeaders;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.ResponseBody;
import retrofit2.Response;

public class PollResultsFragment extends PaginatedListFragment<PollChoice>  {

    @BindView(R.id.question) TextView question;
    @BindView(R.id.publishPoll) Button publishPollBtn;
    @BindView(R.id.shareResults) Button shareResultsBtn;
    @BindView(R.id.timer) TextView timer;
    @BindView(R.id.sessionStatus) TextView sessionStatus;

    private Poll poll;
    private PollSession pollSession;

    private ArrayList<Poll> pollList;
    private ArrayList<PollChoice> pollChoices;
    private Map<Long, Integer> sessionResults;
    private int totalSubmissions = 0;
    private boolean hasSubmissions = false;

    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;
    private long createdTime;
    private NumberFormat numberFormat;

    private OnUpdatePollListener callback;
    private StatusCallback<PollChoiceResponse> pollChoiceCallback;
    private StatusCallback<ResponseBody> closePollCallback;
    private StatusCallback<PollSessionResponse> updatePollSessionCallback;
    private StatusCallback<PollSessionResponse> pollSessionCallback;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);
        numberFormat = new DecimalFormat("00");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //if we published just one poll session
        if(resultCode == Constants.PUBLISH_POLL_SUCCESS) {
            //success! we just published a poll, refresh this fragment
            if(data != null) {
                pollSession = data.getExtras().getParcelable(Constants.POLL_SESSION);
                poll.setId(data.getExtras().getLong(Constants.POLL_ID));

                //publishing the poll was a success, so mark it as successful. Then update the views
                //so that it shows the correct status of the poll (active)
                pollSession.setIs_published(true);
                updateViews();

                reloadData();
            }
        }
        //if we just published multiple polls from this location, go back to the poll session list
        else if(resultCode == Constants.PUBLISH_POLL_SUCCESS_MULTIPLE) {
            getActivity().onBackPressed();
        }
    }

    @Override
    public void updatePoll(Poll poll) {
        //set the current poll to be the one passed in
        this.poll = poll;
        PollsManager.getFirstPagePollChoices(poll.getId(), pollChoiceCallback, true);
    }

    @Override
    public void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        timerHandler = new Handler();
        //setup the timer to update how long the poll has been running
        timerRunnable = new Runnable() {

            @Override
            public void run() {
                setTime();

                timerHandler.postDelayed(this, 1000);
            }
        };
        if(pollSession != null) {
            timerHandler.postDelayed(timerRunnable, 0);
        }

    }

    private void setupViews(Poll poll, ArrayList<PollChoice> pollChoices) {
        clearAdapter();
        question.setText(poll.getQuestion());
        if(pollChoices != null) {
            for (PollChoice choice : pollChoices) {
                addItem(choice);
            }
        }
    }

    private void updateViews() {
        if(pollSession == null) {
            return;
        }
        //publish button
        if(pollSession.is_published()) {
            publishPollBtn.setText(getString(R.string.closePoll));
            shareResultsBtn.setVisibility(View.GONE);
        }
        else {
            publishPollBtn.setText(getString(R.string.republishPoll));
            shareResultsBtn.setVisibility(View.VISIBLE);
        }

        //share button
        if(pollSession.has_public_results()) {
            shareResultsBtn.setText(getString(R.string.hideResults));
        }
        else {
            shareResultsBtn.setText(getString(R.string.shareResults));
        }

        //figure out how much time has elapsed since the start of the poll
        createdTime = pollSession.getCreated_at().getTime();
        if(pollSession.is_published()) {
            sessionStatus.setText(getString(R.string.pollRunning));
            sessionStatus.setTextColor(getResources().getColor(R.color.polling_aqua));
            timer.setVisibility(View.VISIBLE);
            timer.setTextColor(getResources().getColor(R.color.polling_aqua));
            timerHandler.postDelayed(timerRunnable, 0);
        }
        else {
            sessionStatus.setText(getString(R.string.pollEnded));
            sessionStatus.setTextColor(getResources().getColor(R.color.canvasRed));
            timer.setVisibility(View.INVISIBLE);
        }
    }

    private void setTime() {
        Date now = new Date(System.currentTimeMillis());
        long difference = now.getTime() - createdTime;

        int hours = (int)difference/(60*60*1000);
        int minutes = (int)(difference)/(60*1000) % 60;
        int seconds = (int)difference/1000 % 60;

        timer.setText(numberFormat.format(hours) + ":" + numberFormat.format(minutes) + ":" + numberFormat.format(seconds));

    }

    private void setupClickListeners() {
        publishPollBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(pollSession.is_published()) {
                    //close the poll session
                    PollsManager.closePollSession(poll.getId(), pollSession.getId(), closePollCallback, true);
                }
                else {
                    //go to the publish screen to let the teacher select which courses and sections to use
                    startActivityForResult(PublishPollActivity.createIntent(getActivity(), poll.getId()), Constants.PUBLISH_POLL_REQUEST);
                }
            }
        });

        shareResultsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //if we don't have public results, share them
                if(!pollSession.has_public_results()) {
                    PollsManager.updatePollSession(poll.getId(), pollSession.getId(), pollSession.getCourse_id(), pollSession.getCourse_section_id(), true, updatePollSessionCallback, true);
                } else {
                    //otherwise, turn them off
                    PollsManager.updatePollSession(poll.getId(), pollSession.getId(), pollSession.getCourse_id(), pollSession.getCourse_section_id(), false, updatePollSessionCallback, true);
                }
            }
        });
    }

    @Override
    public int getRootLayoutCode() {
        return R.layout.fragment_poll_results;
    }

    @Override
    public void configureViews(View rootView) {
        ButterKnife.bind(this, rootView);
        setupClickListeners();
        setupCallbacks();

        //get the poll passed in by tapping the poll on the poll list page. But we also get a poll in the updatePoll
        //function and we don't want to overwrite it with the poll from the bundle
        if(getArguments() != null && poll == null) {
            poll = getArguments().getParcelable(Constants.POLL_DATA);
            if(poll != null) {
                PollsManager.getFirstPagePollSessions(poll.getId(), pollSessionCallback, true);
            }
            pollSession = getArguments().getParcelable(Constants.POLL_SESSION);
        }

        updateViews();

    }

    @Override
    public View getRowViewForItem(PollChoice item, View convertView, int position) {
        float result = 0;
        //sessionResults holds the student submissions for this session. Find out how many students have selected this PollChoice
        if(sessionResults != null && sessionResults.size() > 0 && item != null) {
            if(sessionResults.containsKey(item.getId()) && totalSubmissions > 0) {
                result = (float)(sessionResults.get(item.getId())) / totalSubmissions;
            }
        }
        return PollResultsRowFactory.buildRowView(layoutInflater(), getActivity(), item.getText(), (int)(result*100), item.is_correct(), convertView, position);
    }

    @Override
    public int getFooterLayoutCode() {
        return 0;
    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public boolean onRowClick(PollChoice item, int position) {
        return false;
    }

    @Override
    public boolean areItemsSorted() {
        return false;
    }

    @Override
    public void loadFirstPage() {
        PollsManager.getFirstPagePollChoices(poll.getId(), pollChoiceCallback, true);
        PollsManager.getSinglePollSession(poll.getId(), pollSession.getId(), pollSessionCallback, true);
    }

    @Override
    public void loadNextPage(String nextURL) {

    }

    @Override
    public String getNextURL() {
        return null;
    }

    @Override
    public void setNextURLNull() {

    }

    @Override
    public void resetData() {
        pollSessionCallback.cancel();
        totalSubmissions = 0;
    }

    @Override
    public void setupCallbacks() {
        pollChoiceCallback = new StatusCallback<PollChoiceResponse>() {
            @Override
            public void onResponse(@NonNull Response<PollChoiceResponse> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                if(getActivity() == null || type.isCache()) return;

                List<PollChoice> pollChoices = response.body().getPollChoices();
                if(pollChoices != null) {
                    for (PollChoice pollChoice : pollChoices) {
                        addItem(pollChoice);
                    }
                    ArrayList<PollChoice> pollChoiceArrayList = new ArrayList<PollChoice>();
                    pollChoiceArrayList.addAll(pollChoices);
                    setupViews(poll, pollChoiceArrayList);
                    updateViews();
                }
            }

            @Override
            public void onFinished(ApiType type) {
                if(swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        };

        closePollCallback = new StatusCallback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Response<ResponseBody> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                if(getActivity() == null || type.isCache()) return;

                if(response.code() == 200) {
                    AppMsg.makeText(getActivity(), getString(R.string.successfullyClosed), AppMsg.STYLE_SUCCESS).show();
                    pollSession.setIs_published(false);
                    updateViews();
                }
            }

            @Override
            public void onFinished(ApiType type) {
                if(swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        };

        updatePollSessionCallback = new StatusCallback<PollSessionResponse>() {
            @Override
            public void onResponse(@NonNull Response<PollSessionResponse> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                if(getActivity() == null || type.isCache()) return;

                List<PollSession> pollSessions = response.body().getPollSessions();
                if(pollSessions != null) {
                    pollSession = pollSessions.get(0);
                }
                updateViews();
            }
        };

        pollSessionCallback = new StatusCallback<PollSessionResponse>() {
            @Override
            public void onResponse(@NonNull Response<PollSessionResponse> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                if(getActivity() == null || type.isCache()) return;

                List<PollSession> pollSessions = response.body().getPollSessions();
                if(pollSessions != null) {
                    sessionResults = pollSessions.get(0).getResults();
                    pollSession = pollSessions.get(0);
                    for(Integer count : sessionResults.values()) {
                        totalSubmissions += count;
                    }
                    updateViews();
                    notifyDataSetChanged();
                }
            }

            @Override
            public void onFinished(ApiType type) {
                if(swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        };
    }
    @Override
    public int getDividerHeight(){ return 0;}

    @Override
    public int getDividerColor(){ return android.R.color.transparent;}
}
