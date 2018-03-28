/*
 * Copyright (C) 2016 - present  Instructure, Inc.
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

package com.instructure.parentapp.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import com.instructure.canvasapi2.RequestInterceptor;
import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.managers.CourseManager;
import com.instructure.canvasapi2.models.Course;
import com.instructure.canvasapi2.models.ScheduleItem;
import com.instructure.canvasapi2.models.Student;
import com.instructure.canvasapi2.utils.ApiPrefs;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.LinkHeaders;
import com.instructure.pandautils.utils.Const;
import com.instructure.parentapp.R;
import com.instructure.parentapp.util.ApplicationManager;
import com.instructure.parentapp.util.RouterUtils;
import com.instructure.parentapp.video.ActivityContentVideoViewClient;
import com.instructure.parentapp.view.CanvasWebView;

import retrofit2.Response;

public class CourseSyllabusFragment extends ParentFragment {

    // view variables
    private CanvasWebView mDetailsWebView;

    // model variables
    private ScheduleItem mSyllabus;

    // callbacks
    StatusCallback<Course> mSyllabusCallback;
    private TextView mEmptySyllabusTextView;

    private Course mCourse;
    private Student mStudent;

    public static CourseSyllabusFragment newInstance(Course course, Student student){
        Bundle args = new Bundle();
        args.putParcelable(Const.COURSE, course);
        args.putParcelable(Const.USER, student);
        CourseSyllabusFragment fragment = new CourseSyllabusFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int getRootLayout() {
        return R.layout.fragment_syllabus;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCourse = getArguments().getParcelable(Const.COURSE);
        mStudent = getArguments().getParcelable(Const.USER);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View rootView = inflater.inflate(getRootLayout(), container, false);
        mEmptySyllabusTextView = (TextView)rootView.findViewById(R.id.emptyTextView);
        mDetailsWebView = (CanvasWebView) rootView.findViewById(R.id.description);
        mDetailsWebView.setClient(new ActivityContentVideoViewClient(getActivity()));
        mDetailsWebView.setCanvasEmbeddedWebViewCallback(new CanvasWebView.CanvasEmbeddedWebViewCallback() {
            @Override
            public boolean shouldLaunchInternalWebViewFragment(String url) {
                return true;
            }

            @Override
            public void launchInternalWebViewFragment(String url) {
                //create and add the InternalWebviewFragment to deal with the link they clicked
                InternalWebviewFragment internalWebviewFragment = new InternalWebviewFragment();
                internalWebviewFragment.setArguments(InternalWebviewFragment.createBundle(url + RequestInterceptor.getSessionLocaleString(), "", null, mStudent));

                FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
                ft.setCustomAnimations(R.anim.slide_from_bottom, android.R.anim.fade_out, R.anim.none, R.anim.slide_to_bottom);
                ft.add(R.id.fullscreen, internalWebviewFragment, internalWebviewFragment.getClass().getName());
                ft.addToBackStack(internalWebviewFragment.getClass().getName());
                ft.commitAllowingStateLoss();
            }
        });

        mDetailsWebView.setCanvasWebViewClientCallback(new CanvasWebView.CanvasWebViewClientCallback() {
            @Override
            public void openMediaFromWebView(String mime, String url, String filename) {

            }

            @Override
            public void onPageStartedCallback(WebView webView, String url) {

            }

            @Override
            public void onPageFinishedCallback(WebView webView, String url) {

            }

            @Override
            public boolean canRouteInternallyDelegate(String url) {
                Uri uri = Uri.parse(url);
                return RouterUtils.canRouteInternally(null, url, mStudent, uri.getHost(), false);
            }

            @Override
            public void routeInternallyCallback(String url) {
                Uri uri = Uri.parse(url);
                RouterUtils.canRouteInternally(getActivity(), url, mStudent, uri.getHost(), true);
            }

            @Override
            public String studentDomainReferrer() {
                return mStudent.getStudentDomain();
            }
        });

        setupDialogToolbar(rootView);

        return rootView;
    }

    @Override
    protected void setupDialogToolbar(View rootView) {
        super.setupDialogToolbar(rootView);

        TextView toolbarTitle = (TextView)rootView.findViewById(R.id.toolbar_title);
        toolbarTitle.setText(R.string.syllabus);
    }

    void populateViews() {
        if (getActivity() == null){
            return;
        }
        if (mSyllabus == null || mSyllabus.getItemType() != ScheduleItem.Type.TYPE_SYLLABUS) {
            //course has no syllabus
            mEmptySyllabusTextView.setVisibility(View.VISIBLE);
            return;
        }
        mDetailsWebView.formatHTML(mSyllabus.getDescription(), mSyllabus.getTitle());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupCallbacks();

        if (mSyllabus == null || mSyllabus.getDescription() == null) {
            CourseManager.getCourseWithSyllabusAirwolf(
                    ApiPrefs.getAirwolfDomain(),
                    ApplicationManager.getParentId(getContext()),
                    mStudent.getStudentId(),
                    mCourse.getId(),
                    mSyllabusCallback
            );
        } else {
            populateViews();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mDetailsWebView != null) {
            mDetailsWebView.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mDetailsWebView != null) {
            mDetailsWebView.onResume();
        }
    }


    private void setupCallbacks() {
        mSyllabusCallback = new StatusCallback<Course>(){
            @Override
            public void onResponse(@NonNull Response<Course> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                Course course = response.body();
                if (course.getSyllabusBody() != null) {
                    mSyllabus = new ScheduleItem();
                    mSyllabus.setItemType(ScheduleItem.Type.TYPE_SYLLABUS);
                    mSyllabus.setTitle(course.getName());
                    mSyllabus.setDescription(course.getSyllabusBody());
                    populateViews();
                } else {
                    //course has no syllabus?
                    populateViews();
                }
            }
        };
    }
}
