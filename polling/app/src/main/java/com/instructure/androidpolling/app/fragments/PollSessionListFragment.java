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
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.Time;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.devspark.appmsg.AppMsg;
import com.instructure.androidpolling.app.R;
import com.instructure.androidpolling.app.activities.FragmentManagerActivity;
import com.instructure.androidpolling.app.activities.PublishPollActivity;
import com.instructure.androidpolling.app.rowfactories.PollSessionRowFactory;
import com.instructure.androidpolling.app.util.ApplicationManager;
import com.instructure.androidpolling.app.util.Constants;
import com.instructure.androidpolling.app.util.SwipeDismissListViewTouchListener;
import com.instructure.androidpolling.app.util.Utils;
import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.managers.PollsManager;
import com.instructure.canvasapi2.managers.SectionManager;
import com.instructure.canvasapi2.models.Course;
import com.instructure.canvasapi2.models.Poll;
import com.instructure.canvasapi2.models.PollChoice;
import com.instructure.canvasapi2.models.PollChoiceResponse;
import com.instructure.canvasapi2.models.PollSession;
import com.instructure.canvasapi2.models.PollSessionResponse;
import com.instructure.canvasapi2.models.PollSubmission;
import com.instructure.canvasapi2.models.Section;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.LinkHeaders;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public class PollSessionListFragment extends PaginatedListFragment<PollSession> {

    private StatusCallback<PollSessionResponse> pollSessionCallback;
    private StatusCallback<Section> sectionCallback;
    private StatusCallback<PollChoiceResponse> pollChoiceCallback;
    private StatusCallback<ResponseBody> responseCanvasCallback;

    private Poll poll;

    @BindView(R.id.question) TextView question;
    @BindView(R.id.publishPoll) Button publishPoll;

    private SwipeDismissListViewTouchListener touchListener;

    private Map<Long, Course> courseMap;
    private Map<Long, Section> sectionMap = new HashMap<>();
    private ArrayList<PollChoice> pollChoiceArrayList = new ArrayList<>();
    private Map<Long, PollChoice> pollChoiceMap = new HashMap<>();

    private String sessionNextUrl;
    private boolean fromGenerateCSV = false;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);

        touchListener = new SwipeDismissListViewTouchListener(
                        getListView(),
                        new SwipeDismissListViewTouchListener.DismissCallbacks() {
                            @Override
                            public boolean canDismiss(int position) {
                                return true;
                            }

                            @Override
                            public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {
                                    //set the poll that we want to remove after the api call returns successfully
                                    PollSession pollSession = getItem(position);

                                    //remove the item from the list
                                    removeItem(pollSession);

                                    //delete the poll from canvas
                                    PollsManager.deletePollSession(poll.getId(), pollSession.getId(), responseCanvasCallback, true);
                                }
                            }
                        });
        getListView().setOnTouchListener(touchListener);
        getListView().setOnScrollListener(touchListener.makeScrollListener());

        //set an animation for adding list items
        LayoutAnimationController controller
                = AnimationUtils.loadLayoutAnimation(
                getActivity(), R.anim.list_layout_controller);

        getListView().setLayoutAnimation(controller);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == Constants.PUBLISH_POLL_SUCCESS || resultCode == Constants.PUBLISH_POLL_SUCCESS_MULTIPLE) {

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
            reloadData();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_upload_csv:
                fromGenerateCSV = true;
                PollsManager.getFirstPagePollChoices(poll.getId(), pollChoiceCallback, true);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupClickListeners() {
        publishPoll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(PublishPollActivity.createIntent(getActivity(), poll.getId()), Constants.PUBLISH_POLL_REQUEST);
            }
        });
    }

    private void generateCSV() {
        String csv = "";
        csv += "Poll Title, Poll Session, Course Name, Section Name, User Name, Answer, Date\n";
        for(int i = 0; i < getItemCount(); i++) {
            PollSession pollSession = getItem(i);
            if(pollSession.getPoll_submissions() != null) {
                for (PollSubmission pollSubmission : pollSession.getPoll_submissions()) {
                    //now add all the necessary stuff to the csv string
                    csv += poll.getQuestion() + ",";
                    csv += pollSession.getId() + ",";
                    csv += courseMap.get(pollSession.getCourse_id()).getName() + ",";
                    csv += sectionMap.get(pollSession.getCourse_section_id()).getName() + ",";
                    csv += pollSubmission.getUser_id() + ",";
                    //make sure we have the poll choice information so we can include the poll choice text instead
                    //of just an id
                    if(pollChoiceMap != null && pollChoiceMap.containsKey(pollSubmission.getPoll_choice_id())) {
                        csv += pollChoiceMap.get(pollSubmission.getPoll_choice_id()).getText() + ",";
                    }
                    else {
                        csv += pollSubmission.getPoll_choice_id() + ",";
                    }
                    csv += pollSubmission.getCreated_at() + "\n";
                }
            }
            else {
                csv += poll.getQuestion() + ",";
                csv += pollSession.getId() + ",";
                csv += courseMap.get(pollSession.getCourse_id()).getName() + ",";
                csv += sectionMap.get(pollSession.getCourse_section_id()).getName() + ",";
                csv += "" + ",";
                csv += "" + ",";
                csv += pollSession.getCreated_at() + "\n";
            }

        }

        //check to make sure there is external storage
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            //it's not there, so the reset of this won't work. Let the user know.
            AppMsg.makeText(getActivity(), getString(R.string.errorGeneratingCSV), AppMsg.STYLE_ERROR).show();
            return;
        }

        File path = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), getString(R.string.generatedCSVFolderName));
        // Make sure the directory exists.
        boolean success = path.mkdirs();

        if(!success) {
            //didn't actually create the path, so now check if it's a directory (if it's already created mkdirs will
            // return false)
            if(!path.isDirectory()) {
                // it's not a directory and wasn't created, so we need to return with an error
                AppMsg.makeText(getActivity(), getString(R.string.errorGeneratingCSV), AppMsg.STYLE_ERROR).show();
                return;
            }
        }
        Time now = new Time();
        now.setToNow();
        File file = new File(path, "csv_" + now.format3339(false) + ".csv");
        try {


            //write the string to a file
            FileWriter out = new FileWriter(file);
            out.write(csv);
            out.close();


        } catch (IOException e) {
            // Unable to create file
            AppMsg.makeText(getActivity(), getString(R.string.errorGeneratingCSV), AppMsg.STYLE_ERROR).show();
        }

        //file is generated, not share it
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        shareIntent.setType("text/csv");
        startActivity(Intent.createChooser(shareIntent, getString(R.string.shareCSV)));
    }

    @Override
    public int getEmptyViewLayoutCode() {
        return R.layout.empty_view_poll_sessions;
    }

    @Override
    public int getRootLayoutCode() {
        return R.layout.fragment_poll_session_list;
    }

    @Override
    public void configureViews(View rootView) {
        super.configureViews(rootView);
        ButterKnife.bind(this, rootView);

        if(getArguments() != null) {
            poll = getArguments().getParcelable(Constants.POLL_DATA);
            if(poll != null) {
                question.setText(poll.getQuestion());
            }
        }

        setupClickListeners();
    }

    @Override
    public View getRowViewForItem(PollSession item, View convertView, int position) {
        courseMap = Utils.createCourseMap(ApplicationManager.getCourseList(getActivity()));
        String courseName = courseMap.get(item.getCourse_id()).getName();

        String sectionName = "";
        if(sectionMap.containsKey(item.getCourse_section_id())) {
            sectionName = sectionMap.get(item.getCourse_section_id()).getName();
        }

        return PollSessionRowFactory.buildRowView(layoutInflater(), getActivity(), courseName, sectionName, item.is_published(), convertView);
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
    public boolean onRowClick(PollSession item, int position) {
        PollResultsFragment pollResultsFragment = new PollResultsFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(Constants.POLL_DATA, poll);
        bundle.putParcelable(Constants.POLL_SESSION, item);
        pollResultsFragment.setArguments(bundle);
        ((FragmentManagerActivity)getActivity()).swapFragments(pollResultsFragment, PollResultsFragment.class.getSimpleName());
        return true;
    }

    @Override
    public boolean areItemsSorted() {
        return true;
    }

    @Override
    public void loadFirstPage() {
        PollsManager.getFirstPagePollSessions(poll.getId(), pollSessionCallback, true);
    }

    @Override
    public void loadNextPage(String nextURL) {
        PollsManager.getNextPagePollSessions(nextURL, pollSessionCallback, true);
    }

    @Override
    public String getNextURL() {
        return sessionNextUrl;
    }

    @Override
    public void setNextURLNull() {
        sessionNextUrl = null;
    }

    @Override
    public void resetData() {}

    @Override
    public void setupCallbacks() {

        pollSessionCallback = new StatusCallback<PollSessionResponse>() {
            @Override
            public void onResponse(@NonNull Response<PollSessionResponse> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                if(getActivity() == null || type.isCache()) return;

                List<PollSession> pollSessions = response.body().getPollSessions();
                sessionNextUrl = linkHeaders.nextUrl;
                if(pollSessions != null) {
                    for (PollSession pollSession : pollSessions) {
                        addItem(pollSession);
                        SectionManager.getSection(pollSession.getCourse_id(), pollSession.getCourse_section_id(), sectionCallback, true);
                    }
                }
            }

            @Override
            public void onFinished(ApiType type) {
                if(swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        };

        sectionCallback = new StatusCallback<Section>() {
            @Override
            public void onResponse(@NonNull Response<Section> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                if(getActivity() == null || type.isCache()) return;
                sectionMap.put(response.body().getId(), response.body());
                notifyDataSetChanged();
            }
        };

        pollChoiceCallback = new StatusCallback<PollChoiceResponse>() {
            @Override
            public void onResponse(@NonNull Response<PollChoiceResponse> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                if(getActivity() == null || type.isCache()) return;

                List<PollChoice> pollChoices = response.body().getPollChoices();
                if(pollChoices != null) {
                    pollChoiceArrayList.addAll(pollChoices);
                }

                //if linkHeaders.nextURL is null it means we have all the choices, so we can go to the edit poll page now
                //or generate the CSV, depending on which action they selected
                if(!StatusCallback.moreCallsExist(linkHeaders)) {
                    if(fromGenerateCSV) {
                        //generate a map from the array list of poll choices
                        for(PollChoice choice : pollChoiceArrayList) {
                            pollChoiceMap.put(choice.getId(), choice);
                        }
                        generateCSV();
                    }
                    else {
                        AddQuestionFragment addQuestionFragment = new AddQuestionFragment();
                        //populate the current data with the bundle
                        Bundle bundle = new Bundle();
                        bundle.putParcelable(Constants.POLL_BUNDLE, poll);
                        bundle.putParcelableArrayList(Constants.POLL_CHOICES, pollChoiceArrayList);
                        addQuestionFragment.setArguments(bundle);
                        ((FragmentManagerActivity) getActivity()).swapFragments(addQuestionFragment, AddQuestionFragment.class.getSimpleName(), R.anim.slide_in_from_bottom, 0, 0, R.anim.slide_out_to_bottom);
                    }
                } else {
                    //otherwise, get the next group of poll choices.
                    PollsManager.getNextPagePollChoices(linkHeaders.nextUrl, pollChoiceCallback, false);
                }
            }

            @Override
            public void onFinished(ApiType type) {
                if(swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        };

        responseCanvasCallback = new StatusCallback<ResponseBody>() {
            @Override
            public void onFail(@Nullable Call<ResponseBody> call, @NonNull Throwable error, @Nullable Response response) {
                AppMsg.makeText(getActivity(), getString(R.string.errorDeletingPollSession), AppMsg.STYLE_ERROR).show();
                //we didn't actually delete anything, but we removed the item from the list to make the animation smoother, so now
                //lets get the poll sessions again
                reloadData();
            }
        };
    }
}
