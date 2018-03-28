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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import com.instructure.candroid.R;
import com.instructure.interactions.Navigation;
import com.instructure.interactions.FragmentInteractions;
import com.instructure.candroid.util.FragUtils;
import com.instructure.candroid.util.RouterUtils;
import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.managers.QuizManager;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.Quiz;
import com.instructure.canvasapi2.utils.ApiPrefs;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.DateHelper;
import com.instructure.canvasapi2.utils.LinkHeaders;
import com.instructure.pandautils.utils.Const;
import com.instructure.pandautils.views.CanvasWebView;

import retrofit2.Response;

public class ModuleQuizDecider extends ParentFragment {

    private String baseURL;
    private String apiURL;
    private Quiz mQuiz;

    private TextView quizTitle;
    private CanvasWebView quizDetails;
    private TextView quizDueDateDetails;
    private Button goToQuizBtn;

    private StatusCallback<Quiz> quizCanvasCallback;
    private CanvasWebView.CanvasWebViewClientCallback webViewClientCallback;
    private CanvasWebView.CanvasEmbeddedWebViewCallback embeddedWebViewCallback;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View rootView = inflater.inflate(R.layout.module_quiz_decider, container, false);
        setupViews(rootView);
        setupCallbacks();

        return rootView;
    }

    @Override
    public void applyTheme() {

    }

    private void setupViews(View rootView) {
        quizTitle = (TextView) rootView.findViewById(R.id.quiz_title);
        quizDetails = (CanvasWebView) rootView.findViewById(R.id.quiz_details);
        quizDueDateDetails = (TextView) rootView.findViewById(R.id.quiz_due_details);
        goToQuizBtn = (Button) rootView.findViewById(R.id.goToQuiz);

        goToQuizBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mQuiz != null) {
                    if(!QuizListFragment.isNativeQuiz(getCanvasContext(), mQuiz)) {
                        Navigation navigation = getNavigation();
                        if (navigation != null) {
                            Bundle bundle = BasicQuizViewFragment.Companion.createBundle(getCanvasContext(), baseURL, mQuiz);
                            navigation.addFragment(FragUtils.getFrag(BasicQuizViewFragment.class, bundle), true);
                        }
                    } else {
                        //take them to the quiz start fragment instead, let them take it natively
                        Navigation navigation = getNavigation();
                        if (navigation != null) {
                            Bundle bundle = QuizStartFragment.createBundle(getCanvasContext(), mQuiz);
                            navigation.addFragment(FragUtils.getFrag(QuizStartFragment.class, bundle), true);
                        }
                    }

                }
            }
        });
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

                InternalWebviewFragment.Companion.loadInternalWebView(getActivity(), getNavigation(), InternalWebviewFragment.Companion.createBundle(getCanvasContext(), url, false));
            }

            @Override
            public boolean shouldLaunchInternalWebViewFragment(String url) {
                return true;
            }
        };

        quizCanvasCallback = new StatusCallback<Quiz>() {
            @Override
            public void onResponse(@NonNull Response<Quiz> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                if(!apiCheck()) { return; }

                mQuiz = response.body();
                quizTitle.setText(mQuiz.getTitle());
                if (mQuiz.getDueAt() != null) {
                    quizDueDateDetails.setText(DateHelper.getDateTimeString(getActivity(), mQuiz.getDueAt()));
                } else {
                    quizDueDateDetails.setText(getString(R.string.toDoNoDueDate));
                }
                quizDetails.formatHTML(mQuiz.getDescription(), "");
                quizDetails.setBackgroundColor(getResources().getColor(R.color.transparent));
                //set some callbacks in case there is a link in the quiz description. We want it to open up in a new
                //InternalWebViewFragment
                quizDetails.setCanvasEmbeddedWebViewCallback(embeddedWebViewCallback);

                quizDetails.setCanvasWebViewClientCallback(webViewClientCallback);
            }
        };
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        QuizManager.getDetailedQuizByUrl(apiURL, true, quizCanvasCallback);
    }

    @Override
    @NonNull
    public String title() {
        return "";
    }

    @Override
    @NonNull
    public FragmentInteractions.Placement getFragmentPlacement() {
        return FragmentInteractions.Placement.DETAIL;
    }

    @Override
    public boolean allowBookmarking() {
        return false;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Intent
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void handleIntentExtras(Bundle extras) {
        super.handleIntentExtras(extras);

        baseURL = extras.getString(Const.URL);
        apiURL = extras.getString(Const.API_URL);
    }

    //for module progression we need the api url too
    public static Bundle createBundle(CanvasContext canvasContext, String url, String apiURL) {
        Bundle extras = createBundle(canvasContext);
        extras.putString(Const.URL, url);
        extras.putString(Const.API_URL, apiURL);
        return extras;
    }
}
