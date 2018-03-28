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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.devspark.appmsg.AppMsg;
import com.instructure.androidpolling.app.R;
import com.instructure.androidpolling.app.activities.BaseActivity;
import com.instructure.androidpolling.app.activities.FragmentManagerActivity;
import com.instructure.androidpolling.app.rowfactories.QuestionRowFactory;
import com.instructure.androidpolling.app.util.Constants;
import com.instructure.androidpolling.app.util.SwipeDismissListViewTouchListener;
import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.managers.PollsManager;
import com.instructure.canvasapi2.models.Course;
import com.instructure.canvasapi2.models.Poll;
import com.instructure.canvasapi2.models.PollChoice;
import com.instructure.canvasapi2.models.PollChoiceResponse;
import com.instructure.canvasapi2.models.PollResponse;
import com.instructure.canvasapi2.models.PollSession;
import com.instructure.canvasapi2.models.PollSessionResponse;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.LinkHeaders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public class QuestionListFragment extends PaginatedExpandableListFragment<String, Poll> {

    private StatusCallback<PollResponse> pollCallback;
    private StatusCallback<ResponseBody> responseCanvasCallback;
    private StatusCallback<PollSessionResponse> pollSessionCallback;
    private StatusCallback<PollChoiceResponse> pollChoiceCallback;

    @BindView(R.id.empty_state) RelativeLayout emptyState;
    @BindView(R.id.expandableListView) ExpandableListView expandableListView;
    @BindView(R.id.addQuestion) FloatingActionButton addQuestion;

    private boolean hasTeacherEnrollment;
    private SwipeDismissListViewTouchListener touchListener;

    private Map<Long, PollSession> openSessions = new HashMap<>();
    private Map<Long, PollSession> closedSessions = new HashMap<>();

    private ArrayList<Poll> pollList = new ArrayList<>();
    private ArrayList<PollChoice> pollChoiceArrayList = new ArrayList<>();

    private Poll pollToDelete;
    private Poll selectedPoll;
    private String nextUrl;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        //clear the list so we don't get duplicates
        pollList.clear();
        openSessions.clear();
        closedSessions.clear();

        setupClickListeners();
        touchListener = new SwipeDismissListViewTouchListener(
                        expandableListView,
                        new SwipeDismissListViewTouchListener.DismissCallbacks() {
                            @Override
                            public boolean canDismiss(int position) {
                                return true;
                            }

                            @Override
                            public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {
                                    //set the poll that we want to remove after the api call returns successfully
                                    pollToDelete = (Poll)expandableListView.getItemAtPosition(position);
                                    confirmDelete();
                                }
                            }
                        });
        expandableListView.setOnTouchListener(touchListener);
        expandableListView.setOnScrollListener(touchListener.makeScrollListener());

        //set an animation for adding list items
        LayoutAnimationController controller
                = AnimationUtils.loadLayoutAnimation(
                getActivity(), R.anim.list_layout_controller);

        expandableListView.setLayoutAnimation(controller);

        ((BaseActivity)getActivity()).setActionBarTitle(getString(R.string.pollQuestions));
    }

    private void setupClickListeners() {
        addQuestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //open the add question fragment
                AddQuestionFragment addQuestionFragment = new AddQuestionFragment();
                ((FragmentManagerActivity)getActivity()).swapFragments(addQuestionFragment,
                        AddQuestionFragment.class.getSimpleName(),
                        R.anim.slide_in_from_bottom, 0, 0, R.anim.slide_out_to_bottom);
            }
        });
    }
    //we need to know if the user is a teacher in any course
    private void checkEnrollments(List<Course> courses) {
        for(Course course: courses) {
            if(course.isTeacher()) {
                hasTeacherEnrollment = true;
                //update the actionbar so the icon shows if we need it
                getActivity().invalidateOptionsMenu();
                return;
            }
        }
        hasTeacherEnrollment = false;
        //update the actionbar so the icon shows if we need it
        getActivity().invalidateOptionsMenu();
    }
    private void displayEmptyState() {
        emptyState.setVisibility(View.VISIBLE);
    }

    //make the teacher confirm that they want to delete the poll
    private void confirmDelete() {
        AlertDialog confirmDeleteDialog =new AlertDialog.Builder(getActivity())
                //set message, title, and icon
                .setTitle(getString(R.string.delete))
                .setMessage(getString(R.string.confirmDelete))
                .setIcon(R.drawable.ic_cv_delete)

                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        //remove the item from the list
                        removeItem(pollToDelete);

                        //delete the poll from canvas
                        PollsManager.deletePoll(pollToDelete.getId(), responseCanvasCallback, true);
                        dialog.dismiss();

                        //if there are any empty groups we want to remove them
                        removeEmptyGroups();
                        //check if all the items are gone
                        if(getGroupCount() == 0) {
                            //show the empty state again
                            displayEmptyState();
                        }
                    }

                })

                .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        pollToDelete = null;
                        dialog.dismiss();
                    }
                })
                .create();

        confirmDeleteDialog.show();
    }

    @Override
    public void updatePoll(Poll poll) {
        //add the poll to the top of the list.
        //after we have apis here we may want to just pull to refresh to get the latest data
        reloadData();
    }

    @Override
    public void configureViews(View rootView) {
        ButterKnife.bind(this, rootView);
    }

    @Override
    public int getRootLayoutCode() {
        return R.layout.fragment_question_list;
    }

    @Override
    public View getRowViewForItem(Poll item, View convertView, int groupPosition, int childPosition, boolean isLastRowInGroup, boolean isLastRow) {
        boolean hasActiveSession = false;
        if(openSessions.containsKey(item.getId())) {
            hasActiveSession = true;
        }
        return QuestionRowFactory.buildRowView(layoutInflater(), getActivity(), item.getQuestion(), hasActiveSession, convertView);
    }

    @Override
    public View getGroupViewForItem(String groupItem, View convertView, int groupPosition, boolean isExpanded) {
        return QuestionRowFactory.buildGroupView(layoutInflater(), groupItem, convertView);
    }

    @Override
    public boolean areGroupsSorted() {
        return true;
    }

    @Override
    public boolean areGroupsReverseSorted() {
        return false;
    }

    @Override
    protected boolean areGroupsCollapsible() {
        return true;
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
    public boolean onRowClick(Poll item) {
        //if the poll is in the draft section, we want to take the user to the edit poll screen
        if(!openSessions.containsKey(item.getId()) && !closedSessions.containsKey(item.getId())) {
            selectedPoll = item;
            pollChoiceArrayList.clear();
            PollsManager.getFirstPagePollChoices(selectedPoll.getId(), pollChoiceCallback, true);
            return true;
        }

        //send the poll data to the results screen
        PollSessionListFragment pollSessionListFragment = new PollSessionListFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(Constants.POLL_DATA, item);
        pollSessionListFragment.setArguments(bundle);
        ((FragmentManagerActivity)getActivity()).swapFragments(pollSessionListFragment, PollSessionListFragment.class.getSimpleName());

        return true;
    }

    @Override
    public boolean areItemsSorted() {
        return true;
    }

    @Override
    public void loadFirstPage() {
        PollsManager.getFirstPagePolls(pollCallback, true);
    }

    @Override
    public void loadNextPage(String nextURL) {
        PollsManager.getNextPagePolls(nextURL, pollCallback, true);
    }

    @Override
    public String getNextURL() {
        return nextUrl;
    }

    @Override
    public void setNextURLNull() {
        nextUrl = null;
    }

    @Override
    public void resetData() {
        pollSessionCallback.cancel();
        pollCallback.cancel();
        openSessions.clear();
        pollList.clear();
    }

    @Override
    public void setupCallbacks() {

        pollCallback = new StatusCallback<PollResponse>() {
            @Override
            public void onResponse(@NonNull Response<PollResponse> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                if(getActivity() == null || type.isCache()) return;

                nextUrl = linkHeaders.nextUrl;
                if(response.body().getPolls().size() == 0) {
                    displayEmptyState();
                }
                else {
                    List<Poll> polls = response.body().getPolls();
                    for(Poll poll: polls) {
                        //add all the polls to a list. we'll use the list later to populate the
                        //different groups after we get some session information about each poll
                        pollList.add(poll);
                        PollsManager.getFirstPagePollSessions(poll.getId(), pollSessionCallback, true);
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

        responseCanvasCallback = new StatusCallback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Response<ResponseBody> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                if(response.code() == 204) {
                    if(pollToDelete != null) {
                        //reset it so we don't try to remove it from the list again
                        pollToDelete = null;
                    }
                }
            }

            @Override
            public void onFail(@Nullable Call<ResponseBody> call, @NonNull Throwable error, @Nullable Response response) {
                AppMsg.makeText(getActivity(), getString(R.string.errorDeletingPoll), AppMsg.STYLE_ERROR).show();
                //we didn't actually delete anything, but we removed the item from the list to make the animation smoother, so now
                //lets get the polls again
                reloadData();
            }
        };

        pollSessionCallback = new StatusCallback<PollSessionResponse>() {
            @Override
            public void onResponse(@NonNull Response<PollSessionResponse> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                if(getActivity() == null || type.isCache()) return;
                List<PollSession> pollSessions = response.body().getPollSessions();
                for(PollSession session : pollSessions) {
                    if(session.is_published()) {
                        openSessions.put(session.getPoll_id(), session);
                        //we only care about there being one active poll session
                        break;
                    }
                    else {
                        closedSessions.put(session.getPoll_id(), session);
                    }
                }
                //if the poll has an active session, remove it from the list (from the "inactive" group)
                //and add it to the "active" group
                for(Poll poll : pollList) {
                    if(openSessions.containsKey(poll.getId())) {
                        removeItem(poll);
                        addItem(getString(R.string.active), poll);
                    }
                    //if the poll doesn't have an open session or any closed sessions, it is still in the draft state
                    else if(!closedSessions.containsKey(poll.getId())) {
                        removeItem(poll);
                        addItem(getString(R.string.draft), poll);
                    }
                    else {
                        removeItem(poll);
                        addItem(getString(R.string.inactive), poll);
                    }
                }
                expandAllGroups();
                if(linkHeaders.nextUrl != null) {
                    PollsManager.getNextPagePollSessions(linkHeaders.nextUrl, pollSessionCallback, true);
                }
                notifyDataSetChanged();
            }

            @Override
            public void onFinished(ApiType type) {
                if(swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        };

        pollChoiceCallback = new StatusCallback<PollChoiceResponse>() {
            @Override
            public void onResponse(@NonNull Response<PollChoiceResponse> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                if(getActivity() == null || type.isCache()) return;

                List<PollChoice> pollChoices = response.body().getPollChoices();
                if (pollChoices != null) {
                    pollChoiceArrayList.addAll(pollChoices);
                }

                //if linkHeaders.nextURL is null it means we have all the choices, so we can go to the edit poll page now
                //or generate the CSV, depending on which action they selected
                if (!StatusCallback.moreCallsExist(linkHeaders)) {

                    AddQuestionFragment addQuestionFragment = new AddQuestionFragment();
                    //populate the current data with the bundle
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(Constants.POLL_BUNDLE, selectedPoll);
                    bundle.putParcelableArrayList(Constants.POLL_CHOICES, pollChoiceArrayList);
                    addQuestionFragment.setArguments(bundle);
                    ((FragmentManagerActivity) getActivity()).swapFragments(addQuestionFragment, AddQuestionFragment.class.getSimpleName());

                } else {
                    //otherwise, get the next group of poll choices.
                    PollsManager.getNextPagePollChoices(linkHeaders.nextUrl, pollChoiceCallback, true);
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
}
