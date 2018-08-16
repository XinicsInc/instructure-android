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

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.View;

import com.instructure.candroid.R;
import com.instructure.candroid.binders.ExpandableHeaderBinder;
import com.instructure.candroid.binders.QuizBinder;
import com.instructure.candroid.holders.ExpandableViewHolder;
import com.instructure.candroid.holders.QuizViewHolder;
import com.instructure.candroid.interfaces.AdapterToFragmentCallback;
import com.instructure.candroid.util.StringUtilities;
import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.managers.QuizManager;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.Quiz;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.DateHelper;
import com.instructure.canvasapi2.utils.LinkHeaders;
import com.instructure.pandarecycler.interfaces.ViewHolderHeaderClicked;
import com.instructure.pandarecycler.util.GroupSortedList;
import com.instructure.pandarecycler.util.Types;
import com.instructure.pandautils.utils.ColorKeeper;

import java.util.List;

import retrofit2.Response;

public class QuizListRecyclerAdapter extends ExpandableRecyclerAdapter<String, Quiz, RecyclerView.ViewHolder>{

    private List<Quiz> mQuizzes;
    private CanvasContext mCanvasContext;

    private StatusCallback<List<Quiz>> mQuizzesCallback;
    private AdapterToFragmentCallback<Quiz> mAdapterToFragmentCallback;
    private int mCourseColor;


    public QuizListRecyclerAdapter(Context context, CanvasContext canvasContext, AdapterToFragmentCallback<Quiz> adapterToFragmentCallback) {
        super(context, String.class, Quiz.class);

        mCanvasContext = canvasContext;
        mAdapterToFragmentCallback = adapterToFragmentCallback;
        mCourseColor = ColorKeeper.getOrGenerateColor(canvasContext);
        setExpandedByDefault(true);
        setViewHolderHeaderClicked(new ViewHolderHeaderClicked<String>() {
            @Override
            public void viewClicked(View view, String groupName) {
                expandCollapseGroup(groupName);

                if(!isGroupExpanded(groupName)) {
                    //if this group is collapsed we want to try to load the data to avoid having
                    //a progress bar spin forever.
                    loadData();
                }
            }
        });
        loadData();
    }

    @Override
    public void setupCallbacks() {
        mQuizzesCallback = new StatusCallback<List<Quiz>>() {

            @Override
            public void onResponse(@NonNull Response<List<Quiz>> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                setNextUrl(linkHeaders.nextUrl);
                mQuizzes = response.body();
                populateAdapter();
            }

            @Override
            public void onFinished(ApiType type) {
                if (mAdapterToFragmentCallback != null) mAdapterToFragmentCallback.onRefreshFinished();
            }
        };
    }

    @Override
    public void resetData() {
        mQuizzes = null;
        super.resetData();
    }

    private void populateAdapter() {
        String assignmentQuizzes = getContext().getString(R.string.assignmentQuizzes);
        String surveys = getContext().getString(R.string.surveys);
        String gradedSurveys = getContext().getString(R.string.gradedSurveys);
        String practiceQuizzes = getContext().getString(R.string.practiceQuizzes);

        for (Quiz quiz : mQuizzes) {
            if (quiz.getQuizType().equals(Quiz.TYPE_ASSIGNMENT)) {
                addOrUpdateItem(assignmentQuizzes, quiz);
            } else if (quiz.getQuizType().equals(Quiz.TYPE_SURVEY)) {
                addOrUpdateItem(surveys, quiz);
            } else if (quiz.getQuizType().equals(Quiz.TYPE_GRADED_SURVEY)) {
                addOrUpdateItem(gradedSurveys, quiz);
            } else if (quiz.getQuizType().equals(Quiz.TYPE_PRACTICE)) {
                addOrUpdateItem(practiceQuizzes, quiz);
            }

            mQuizzes = null;
        }

        mAdapterToFragmentCallback.onRefreshFinished();
        notifyDataSetChanged();
    }

    @Override
    public boolean isPaginated() {
        return true;
    }

    @Override
    public void loadFirstPage() {
        QuizManager.getFirstPageQuizList(mCanvasContext, false, mQuizzesCallback);
    }

    @Override
    public void loadNextPage(String nextURL) {
        QuizManager.getNextPageQuizList(nextURL, false, mQuizzesCallback);
    }

    @Override
    public RecyclerView.ViewHolder createViewHolder(View v, int viewType) {
        if(viewType == Types.TYPE_HEADER){
            return new ExpandableViewHolder(v);
        } else {
            return new QuizViewHolder(v);
        }
    }

    @Override
    public int itemLayoutResId(int viewType) {
        if(viewType == Types.TYPE_HEADER){
            return ExpandableViewHolder.holderResId();
        } else {
            return QuizViewHolder.holderResId();
        }
    }

    @Override
    public void onBindChildHolder(RecyclerView.ViewHolder holder, String s, Quiz quiz) {
        QuizBinder.bind((QuizViewHolder)holder, quiz, mAdapterToFragmentCallback, getContext(), mCourseColor);
    }

    @Override
    public void onBindHeaderHolder(RecyclerView.ViewHolder holder, String s, boolean isExpanded) {
        ExpandableHeaderBinder.bind(getContext(), mCanvasContext, (ExpandableViewHolder)holder, s, s, isExpanded, getViewHolderHeaderClicked());
    }

    @Override
    public GroupSortedList.GroupComparatorCallback<String> createGroupCallback() {
        return new GroupSortedList.GroupComparatorCallback<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }

            @Override
            public boolean areContentsTheSame(String oldGroup, String newGroup) {
                return oldGroup.equals(newGroup);
            }

            @Override
            public boolean areItemsTheSame(String group1, String group2) {
                return group1.equals(group2);
            }

            @Override
            public long getUniqueGroupId(String group) {
                return group.hashCode();
            }

            @Override
            public int getGroupType(String group) {
                return Types.TYPE_HEADER;
            }
        };
    }

    @Override
    public GroupSortedList.ItemComparatorCallback<String, Quiz> createItemCallback() {
        return new GroupSortedList.ItemComparatorCallback<String, Quiz>() {
            @Override
            public int compare(String group, Quiz o1, Quiz o2) {
                return compareQuiz(o1, o2);
            }

            @Override
            public boolean areContentsTheSame(Quiz oldItem, Quiz newItem) {
                boolean sameTitle = false;
                boolean sameDate = false;
                boolean sameDescription = false;
                if(oldItem.getTitle().equals(newItem.getTitle())){
                    sameTitle = true;
                }
                if(StringUtilities.simplifyHTML(Html.fromHtml(oldItem.getDescription())).equals(StringUtilities.simplifyHTML(Html.fromHtml(newItem.getDescription())))){
                    sameDescription = true;
                }
                String newDateString = getDateString(newItem);
                String oldDateString = getDateString(oldItem);

                if(newDateString != null && oldDateString != null){
                    sameDate = newDateString.equals(oldDateString);
                } else if(isNullableChanged(oldDateString, newDateString)){
                    sameDate = false;
                }

                return sameTitle && sameDate && sameDescription;
            }

            @Override
            public boolean areItemsTheSame(Quiz item1, Quiz item2) {
                return item1.getId() == item2.getId();
            }

            @Override
            public long getUniqueItemId(Quiz item) {
                return item.getId();
            }

            @Override
            public int getChildType(String group, Quiz item) {
                return Types.TYPE_ITEM;
            }
        };
    }

    private String getDateString(Quiz quiz){
        String dateString;
        if (quiz.getDueAt() != null) {
            dateString = DateHelper.createPrefixedDateTimeString(getContext(), R.string.dueAt, quiz.getDueAt());
        } else {
            dateString = null;
        }
        return dateString;
    }

    private boolean isNullableChanged(Object o1, Object o2) {
        return (o1 == null && o2 != null) || (o1 !=null && o2 == null);
    }

    private int compareQuiz(Quiz q1, Quiz q2){
        if (q1.getAssignmentId() > 0 && q1.getDueAt() != null && q2.getAssignmentId() > 0 && q2.getDueAt() != null) {
            return q1.getDueAt().compareTo(q2.getDueAt());
        } else {
            return q1.getTitle().toLowerCase().compareTo(q2.getTitle().toLowerCase());
        }
    }
}
