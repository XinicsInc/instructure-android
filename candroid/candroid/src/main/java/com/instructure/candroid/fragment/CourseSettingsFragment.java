/*
 * Copyright (C) 2017 - present Instructure, Inc.
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.instructure.candroid.R;
import com.instructure.interactions.FragmentInteractions;
import com.instructure.canvasapi2.models.Course;
import com.instructure.canvasapi2.utils.DateHelper;
import com.instructure.pandautils.utils.ColorKeeper;
import com.instructure.pandautils.utils.Const;
import com.instructure.pandautils.utils.PandaViewUtils;
import com.instructure.pandautils.utils.ViewStyler;

public class CourseSettingsFragment extends ParentFragment implements DatePickerFragment.DatePickerCancelListener {

    @Override
    public void onCancel() {}
    private View rootView;
    private Toolbar toolbar;
    private Course course;

    private ViewFlipper viewFlipper;


    //View the Settings
    private TextView courseName;
    private TextView courseCode;

    private LinearLayout startAtLayout;
    private TextView startAt;

    private LinearLayout endAtLayout;
    private TextView endAt;

    private TextView license;
    private TextView visibility;

    private LinearLayout editStartAtLayout;
    private TextView editStartDate;
    private ImageView editStartAt;

    private LinearLayout editEndAtLayout;
    private TextView editEndDate;
    private ImageView editEndAt;

    @Override
    @NonNull
    public FragmentInteractions.Placement getFragmentPlacement() {return FragmentInteractions.Placement.MASTER; }

    @Override
    @NonNull
    public String title() {
        return getString(R.string.settings);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_course_settings, container, false);
        toolbar = rootView.findViewById(R.id.toolbar);
        viewFlipper = rootView.findViewById(R.id.view_flipper);

        //View the Settings
        courseName = rootView.findViewById(R.id.course_name);
        courseCode = rootView.findViewById(R.id.course_code);
        startAt = rootView.findViewById(R.id.start_date);
        startAtLayout = rootView.findViewById(R.id.starts_layout);

        endAt = rootView.findViewById(R.id.end_date);
        endAtLayout = rootView.findViewById(R.id.ends_layout);

        license = rootView.findViewById(R.id.license_string);
        visibility = rootView.findViewById(R.id.visibility_string);


        editStartAt = rootView.findViewById(R.id.edit_start_date);
        editStartAt.setImageDrawable(ColorKeeper.getColoredDrawable(getContext(), R.drawable.vd_calendar, course));

        editStartDate = rootView.findViewById(R.id.tvStartDate);

        if(course.getStartDate() != null) {
            editStartDate.setText(DateHelper.getShortDate(getActivity(), course.getStartDate()));
        } else {
            editStartDate.setText(getString(R.string.noDate));
        }
        editEndAt = rootView.findViewById(R.id.edit_end_date);
        editEndAt.setImageDrawable(ColorKeeper.getColoredDrawable(getContext(), R.drawable.vd_calendar, course));

        editEndDate = rootView.findViewById(R.id.tvEndDate);

        if(course.getEndDate() != null) {
            editEndDate.setText(DateHelper.getShortDate(getActivity(), course.getEndDate()));
        } else {
            editEndDate.setText(getString(R.string.noDate));
        }
        editEndAtLayout = rootView.findViewById(R.id.edit_ends_layout);
        editStartAtLayout = rootView.findViewById(R.id.edit_starts_layout);


        viewFlipper.setInAnimation(getContext(), R.anim.fade_in_quick);
        viewFlipper.setOutAnimation(getContext(), R.anim.fade_out_quick);

        resetCourseData();

        return rootView;
    }

    @Override
    public void applyTheme() {
        toolbar.setTitle(title());
        PandaViewUtils.setupToolbarBackButton(toolbar, this);
        ViewStyler.themeToolbar(getActivity(), toolbar, getCanvasContext());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if(getArguments() != null) {
            getArguments().putBoolean(Const.IN_EDIT_MODE, isEditMode());
        }
        super.onConfigurationChanged(newConfig);
    }

    public boolean isEditMode() {
        return viewFlipper.getDisplayedChild() != 0;
    }

    public void resetCourseData() {
        courseName.setText(course.getName());

        courseCode.setText(course.getCourseCode());

        if (course.getStartDate() != null) {
            startAt.setText(DateHelper.dateToDayMonthYearString(getContext(), course.getStartDate()));
        } else {
            startAtLayout.setVisibility(View.GONE);
            editStartAtLayout.setVisibility(View.GONE);
        }

        if (course.getEndDate() != null) {
            endAt.setText(DateHelper.dateToDayMonthYearString(getContext(), course.getEndDate()));

        } else {
            endAtLayout.setVisibility(View.GONE);
            editEndAtLayout.setVisibility(View.GONE);
        }

        license.setText(course.getLicensePrettyPrint());

        if (course.isPublic()) {
            visibility.setText(getString(R.string.publiclyAvailable));
        } else {
            visibility.setText(getString(R.string.privatelyAvailable));
        }

    }
    @Override
    public void handleIntentExtras(Bundle bundle) {
        super.handleIntentExtras(bundle);

        if(getCanvasContext() instanceof Course) {
            course = (Course) getCanvasContext();
        }
    }

    @Override
    public boolean allowBookmarking() {
        return false;
    }
}
