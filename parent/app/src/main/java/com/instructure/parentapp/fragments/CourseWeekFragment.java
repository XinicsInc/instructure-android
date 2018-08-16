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

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.instructure.canvasapi2.models.Course;
import com.instructure.canvasapi2.models.Student;
import com.instructure.pandautils.utils.Const;
import com.instructure.pandautils.utils.Prefs;
import com.instructure.parentapp.R;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Copyright (c) 2016 Instructure. All rights reserved.
 */
public class CourseWeekFragment extends WeekFragment {

    @BindView(R.id.toolbar_title) TextView mToolbarTitle;
    @BindView(R.id.toolbar_right_icon) ImageView mToolbarIcon;
    //TODO
    private Course mCourse;
    private Student mStudent;

    public static CourseWeekFragment newInstance(Student user, Course course) {
        Bundle args = new Bundle();
        args.putParcelable(Const.STUDENT, user);
        args.putParcelable(Const.COURSE, course);
        CourseWeekFragment fragment = new CourseWeekFragment();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public int layoutResId() {
        return R.layout.fragment_course_week;
    }

    @Override
    public void onCreateView(View view) {
        super.onCreateView(view);
        ButterKnife.bind(this, view);

        //set the color of the weekBackground
        Prefs prefs = new Prefs(getActivity(), getActivity().getString(R.string.app_name_parent));
        int color = prefs.load(Const.NEW_COLOR, -1);
        if(color != -1) {
            mWeekBackground.setBackgroundColor(color);
        }

        mToolbarTitle.setText(mCourse.getName());
        if(mCourse.getSyllabusBody() == null) {
            mToolbarIcon.setVisibility(View.GONE);
        }
        mToolbarIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CourseSyllabusFragment fragment = CourseSyllabusFragment.newInstance(mCourse, mStudent);

                FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
                ft.setCustomAnimations(R.anim.slide_from_bottom, android.R.anim.fade_out, R.anim.none, R.anim.slide_to_bottom);
                ft.add(R.id.fullscreen, fragment, fragment.getClass().getName());
                ft.addToBackStack(fragment.getClass().getName());
                ft.commitAllowingStateLoss();
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCourse = getArguments().getParcelable(Const.COURSE);
        mStudent = getArguments().getParcelable(Const.STUDENT);
    }
}
