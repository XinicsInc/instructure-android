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

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.instructure.candroid.R;
import com.instructure.candroid.adapter.QuizListRecyclerAdapter;
import com.instructure.interactions.Navigation;
import com.instructure.candroid.interfaces.AdapterToFragmentCallback;
import com.instructure.interactions.FragmentInteractions;
import com.instructure.candroid.util.FragUtils;
import com.instructure.candroid.util.Param;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.Course;
import com.instructure.canvasapi2.models.Quiz;
import com.instructure.canvasapi2.models.QuizQuestion;
import com.instructure.canvasapi2.models.Tab;
import com.instructure.canvasapi2.utils.pageview.PageView;
import com.instructure.pandautils.utils.PandaViewUtils;
import com.instructure.pandautils.utils.ViewStyler;

import java.util.ArrayList;

@PageView(url = "{canvasContext}/quizzes")
public class QuizListFragment extends ParentFragment {

    private View mRootView;
    private Toolbar mToolbar;
    private QuizListRecyclerAdapter mRecyclerAdapter;
    private AdapterToFragmentCallback<Quiz> mAdapterToFragmentCallback;

    @Override
    @NonNull
    public FragmentInteractions.Placement getFragmentPlacement() {return FragmentInteractions.Placement.MASTER; }

    public String getTabId() {
        return Tab.QUIZZES_ID;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapterToFragmentCallback = new AdapterToFragmentCallback<Quiz>() {
            @Override
            public void onRowClicked(Quiz quiz, int position, boolean isOpenDetail) {
                rowClick(quiz, isOpenDetail);
            }

            @Override
            public void onRefreshFinished() {
                setRefreshing(false);
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mRootView = getLayoutInflater().inflate(R.layout.quiz_list_layout, container, false);
        mToolbar = mRootView.findViewById(R.id.toolbar);
        mRecyclerAdapter = new QuizListRecyclerAdapter(getContext(), getCanvasContext(), mAdapterToFragmentCallback);
        configureRecyclerView(mRootView, getContext(), mRecyclerAdapter, R.id.swipeRefreshLayout, R.id.emptyPandaView, R.id.listView);

        return mRootView;
    }

    @Override
    public void applyTheme() {
        setupToolbarMenu(mToolbar);
        mToolbar.setTitle(title());
        PandaViewUtils.setupToolbarBackButton(mToolbar, this);
        ViewStyler.themeToolbar(getActivity(), mToolbar, getCanvasContext());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        configureRecyclerView(mRootView, getContext(), mRecyclerAdapter, R.id.swipeRefreshLayout, R.id.emptyPandaView, R.id.listView);
    }

    @Override
    @NonNull
    public String title() {
        return getString(R.string.quizzes);
    }

    @Override
    protected String getSelectedParamName() {
        return Param.QUIZ_ID;
    }

    private void rowClick(Quiz quiz, boolean closeSlidingPane){
        Navigation navigation = getNavigation();
        if(navigation != null){
            //determine if we support the quiz question types. If not, just show the questions in a webview.
            //if the quiz has an access code we don't currently support that natively on the app, so send them
            //to a webview. Also, we currently don't support one quiz question at a time quizzes.
            if(!isNativeQuiz(getCanvasContext(), quiz)) {
                //Log to GA, track if they're a teacher (because teachers currently always get the non native quiz)
                Bundle bundle = BasicQuizViewFragment.Companion.createBundle(getCanvasContext(), quiz.getUrl(), quiz);
                navigation.addFragment(
                        FragUtils.getFrag(BasicQuizViewFragment.class, bundle));
            } else {

                Bundle bundle = QuizStartFragment.createBundle(getCanvasContext(), quiz);
                navigation.addFragment(
                        FragUtils.getFrag(QuizStartFragment.class, bundle));
            }
        }
    }

    public static boolean isNativeQuiz(CanvasContext canvasContext, Quiz quiz) {
        return !(containsUnsupportedQuestionType(quiz) || quiz.isHasAccessCode() || quiz.getOneQuestionAtATime() || (canvasContext instanceof Course && ((Course) canvasContext).isTeacher()));
    }

    //currently support TRUE_FALSE, ESSAY, SHORT_ANSWER, MULTI_CHOICE
    private static boolean containsUnsupportedQuestionType(Quiz quiz) {
        ArrayList<QuizQuestion.QUESTION_TYPE> questionTypes = quiz.getParsedQuestionTypes();
        if(questionTypes == null || questionTypes.size() == 0) {
            return true;
        }

        //loop through all the quiz question types. If there is one we don't support, return true
        for(QuizQuestion.QUESTION_TYPE questionType : questionTypes) {
            switch (questionType) {
                case CALCULATED:
                case FILL_IN_MULTIPLE_BLANKS:
                case UNKNOWN:
                    return true;
            }
        }

        return false;
    }

    @Override
    public void handleIntentExtras(Bundle extras) {
        super.handleIntentExtras(extras);
    }

    @Override
    public boolean allowBookmarking() {
        return true;
    }
}
