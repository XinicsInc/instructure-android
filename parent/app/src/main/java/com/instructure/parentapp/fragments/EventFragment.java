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
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.instructure.canvasapi2.models.ScheduleItem;
import com.instructure.canvasapi2.models.Student;
import com.instructure.canvasapi2.utils.DateHelper;
import com.instructure.pandautils.utils.Const;
import com.instructure.parentapp.R;
import com.instructure.parentapp.database.DatabaseHandler;
import com.instructure.parentapp.receivers.AlarmReceiver;
import com.instructure.parentapp.util.AnalyticUtils;
import com.instructure.parentapp.util.RouterUtils;
import com.instructure.parentapp.video.ActivityContentVideoViewClient;
import com.instructure.parentapp.view.CanvasWebView;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Copyright (c) 2014 Instructure. All rights reserved.
 */
public class EventFragment extends ParentFragment implements
        TimePickerFragment.TimePickerFragmentListener,
        TimePickerFragment.TimePickerCancelListener,
        DatePickerFragment.DatePickerFragmentListener,
        DatePickerFragment.DatePickerCancelListener {

    private static final String DATE_PICKER_EVENT_TAG = "datePickerEvent";
    private static final String TIME_PICKER_EVENT_TAG = "timePickerEvent";

    // view variables
    private CanvasWebView mCanvasWebView;
    private View mCalendarView;

    private TextView mDate1;
    private TextView mDate2;
    private TextView mAddress1;
    private TextView mAddress2;
    private TextView mAlarmDetails;
    private SwitchCompat mAlarmSwitch;
    private DatabaseHandler mDatabaseHandler;
    private int mAlarmId = -1;
    private SwitchCompat.OnCheckedChangeListener mCheckedChangeListener;
    private TimePickerFragment mTimePicker;
    private DatePickerFragment mDatePickerDialog;
    private Calendar mSetDate;

    // model variables
    private ScheduleItem mScheduleItem;
    private Student mStudent;
    private String mCourseTitle;
    private long mScheduleItemId;

    public static EventFragment newInstance(ScheduleItem scheduleItem, Student student){
        Bundle args = new Bundle();
        args.putParcelable(Const.SCHEDULE_ITEM, scheduleItem);
        args.putParcelable(Const.STUDENT, student);
        EventFragment fragment = new EventFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int getRootLayout() {
        return R.layout.fragment_event;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(getRootLayout(), container, false);
        mScheduleItem = getArguments().getParcelable(Const.SCHEDULE_ITEM);
        mStudent = getArguments().getParcelable(Const.STUDENT);
        setupDialogToolbar(rootView);
        initViews(rootView);
        populateViews();
        setupListeners();
        setupAlarmInfo();

        return rootView;
    }

    @Override
    protected void setupDialogToolbar(View rootView) {
        super.setupDialogToolbar(rootView);

        TextView toolbarTitle = (TextView)rootView.findViewById(R.id.toolbar_title);
        toolbarTitle.setText(mScheduleItem.getTitle());
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mCanvasWebView != null) {
            mCanvasWebView.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mCanvasWebView != null) {
            mCanvasWebView.onResume();
        }
    }

    private void setupListeners() {
        mCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked) {
                    mAlarmDetails.setVisibility(View.VISIBLE);
                    mSetDate = Calendar.getInstance();
                    Calendar cal = Calendar.getInstance();
                    if (mScheduleItem.getStartAt() != null) {
                        cal.setTime(mScheduleItem.getStartAt());
                    }

                    mDatePickerDialog = DatePickerFragment.newInstance(EventFragment.this, EventFragment.this);

                    mDatePickerDialog.show(getActivity().getFragmentManager(), DATE_PICKER_EVENT_TAG);

                } else {
                    mAlarmDetails.setVisibility(View.INVISIBLE);
                    mAlarmDetails.setText("");
                    cancelAlarm();
                }
            }
        };
    }

    private void setupAlarmInfo() {
        mDatabaseHandler = new DatabaseHandler(getActivity());
        try {
            mDatabaseHandler.open();
            Calendar alarm = mDatabaseHandler.getAlarmByAssignmentId(mScheduleItem.getId());
            if(alarm != null) {
                mAlarmId = mDatabaseHandler.getRowIdByAssignmentId(mScheduleItem.getId());

                mAlarmDetails.setVisibility(View.VISIBLE);
                mAlarmDetails.setText(DateHelper.getShortDateTimeStringUniversal(getContext(), alarm.getTime()));
                //set the listener to null so we don't trigger the onCheckChangedListener when we set the value
                mAlarmSwitch.setOnCheckedChangeListener(null);
                mAlarmSwitch.setChecked(true);
                mAlarmSwitch.setOnCheckedChangeListener(mCheckedChangeListener);
            } else {
                mAlarmSwitch.setChecked(false);
                mAlarmSwitch.setOnCheckedChangeListener(mCheckedChangeListener);
            }
            mDatabaseHandler.close();
        } catch (SQLException e) {
            //couldn't find the alarm in the database, so don't show that there is an alarm
            mAlarmSwitch.setChecked(false);
            mAlarmSwitch.setOnCheckedChangeListener(mCheckedChangeListener);
        }
    }

    private void cancelAlarm() {
        //cancel the alarm
        AlarmReceiver alarmReceiver = new AlarmReceiver();
        String subTitle = "";
        if(mScheduleItem.getStartAt() != null) {
            subTitle = DateHelper.getDateTimeString(getContext(), mScheduleItem.getStartAt());
        }
        alarmReceiver.cancelAlarm(getContext(), mScheduleItem.getId(), mScheduleItem.getTitle(), subTitle);

        //remove it from the database
        if(mDatabaseHandler == null) {
            mDatabaseHandler = new DatabaseHandler(getActivity());
        }
        try {
            mDatabaseHandler.open();
            int id = mDatabaseHandler.getRowIdByAssignmentId(mScheduleItem.getId());
            int result = mDatabaseHandler.deleteAlarm(id);
            mDatabaseHandler.close();
        } catch (SQLException e) {
            //couldn't delete the alarm, so it will remain in the database. But the actual
            //alarm should have been canceled above.
        }
    }

    void initViews(View rootView) {

        mCalendarView = rootView.findViewById(R.id.calendarView);

        mDate1 = (TextView) rootView.findViewById(R.id.date1);
        mDate2 = (TextView) rootView.findViewById(R.id.date2);
        mAddress1 = (TextView) rootView.findViewById(R.id.address1);
        mAddress2 = (TextView) rootView.findViewById(R.id.address2);
        mAlarmDetails = (TextView) rootView.findViewById(R.id.alarmDetails);

        mAlarmSwitch = (SwitchCompat) rootView.findViewById(R.id.alarmSwitch);
        mAlarmSwitch.setOnCheckedChangeListener(mCheckedChangeListener);

        mCanvasWebView = (CanvasWebView) rootView.findViewById(R.id.description);
        mCanvasWebView.setClient(new ActivityContentVideoViewClient(getActivity()));
        mCanvasWebView.setCanvasEmbeddedWebViewCallback(new CanvasWebView.CanvasEmbeddedWebViewCallback() {
            @Override
            public void launchInternalWebViewFragment(String url) {
                //create and add the InternalWebviewFragment to deal with the link they clicked
                InternalWebviewFragment internalWebviewFragment = new InternalWebviewFragment();
                internalWebviewFragment.setArguments(InternalWebviewFragment.createBundle(url, "", null, mStudent));

                FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
                ft.setCustomAnimations(R.anim.slide_from_bottom, android.R.anim.fade_out, R.anim.none, R.anim.slide_to_bottom);
                ft.add(R.id.fullscreen, internalWebviewFragment, internalWebviewFragment.getClass().getName());
                ft.addToBackStack(internalWebviewFragment.getClass().getName());
                ft.commitAllowingStateLoss();
            }

            @Override
            public boolean shouldLaunchInternalWebViewFragment(String url) {
                return true;
            }
        });

        mCanvasWebView.setCanvasWebViewClientCallback(new CanvasWebView.CanvasWebViewClientCallback() {
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
                Uri uri = Uri.parse(mScheduleItem.getHtmlUrl());
                return RouterUtils.canRouteInternally(null, url, mStudent, uri.getHost(), false);
            }

            @Override
            public void routeInternallyCallback(String url) {
                Uri uri = Uri.parse(mScheduleItem.getHtmlUrl());
                RouterUtils.canRouteInternally(getActivity(), url, mStudent, uri.getHost(), true);
            }

            @Override
            public String studentDomainReferrer() {
                return mStudent.getStudentDomain();
            }
        });
    }

    void populateViews() {
        String content = mScheduleItem.getDescription();

        mCanvasWebView.setVisibility(View.GONE);

        if(mScheduleItem.isAllDay()) {
            mDate1.setText(getString(R.string.allDayEvent));
            mDate2.setText(getFullDateString(mScheduleItem.getEndAt()));
        } else {
            //Setup the calendar event start/end times
            if(mScheduleItem.getStartAt() != null && mScheduleItem.getEndAt() != null && mScheduleItem.getStartAt().getTime() != mScheduleItem.getEndAt().getTime()) {
                //Our date times are different so we display two strings
                mDate1.setText(getFullDateString(mScheduleItem.getEndAt()));
                String startTime = DateHelper.getFormattedTime(getContext(), mScheduleItem.getStartAt());
                String endTime = DateHelper.getFormattedTime(getContext(), mScheduleItem.getEndAt());
                mDate2.setText(startTime + " - " + endTime);
            } else {
                mDate1.setText(getFullDateString(mScheduleItem.getStartAt()));
                mDate2.setVisibility(View.INVISIBLE);
            }
        }

        boolean noLocationTitle = TextUtils.isEmpty(mScheduleItem.getLocationName());
        boolean noLocation = TextUtils.isEmpty(mScheduleItem.getLocationAddress());

        if(noLocation && noLocationTitle) {
            mAddress1.setText(getString(R.string.noLocation));
            mAddress2.setVisibility(View.INVISIBLE);
        } else {
            if(noLocationTitle) {
                mAddress1.setText(mScheduleItem.getLocationAddress());
            } else {
                mAddress1.setText(mScheduleItem.getLocationName());
                mAddress2.setText(mScheduleItem.getLocationAddress());
            }
        }

        if(!TextUtils.isEmpty(content)){
            mCanvasWebView.setVisibility(View.VISIBLE);
            if(getResources().getBoolean(R.bool.isTablet)) {
                mCanvasWebView.setBackgroundColor(getResources().getColor(R.color.white));
            } else {
                mCanvasWebView.setBackgroundColor(getResources().getColor(R.color.canvasBackgroundLight));
            }
            mCanvasWebView.formatHTML(content, mScheduleItem.getTitle());
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // CallBack
    ///////////////////////////////////////////////////////////////////////////

    public String getFullDateString(Date date) {
        if(mScheduleItem == null || date == null) {
            return "";
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE,");
        String dayOfWeek = dateFormat.format(date);
        String dateString = DateHelper.getFormattedDate(getContext(), date);

        return dayOfWeek + " " + dateString;
    }

    @Override
    public void onCancel() {
        mAlarmSwitch.setChecked(false);
    }


    @Override
    public void onDateSet(int year, int month, int day) {
        mSetDate.set(Calendar.YEAR, year);
        mSetDate.set(Calendar.MONTH, month);
        mSetDate.set(Calendar.DAY_OF_MONTH, day);

        mDatePickerDialog.dismiss();
        mTimePicker = TimePickerFragment.newInstance(this, this);

        mTimePicker.setCancelable(false);

        mTimePicker.show(getActivity().getSupportFragmentManager(), TIME_PICKER_EVENT_TAG);
    }


    @Override
    public void onTimeSet(int hourOfDay, int minute) {

        mSetDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
        mSetDate.set(Calendar.MINUTE, minute);

        mAlarmDetails.setText(DateHelper.getShortDateTimeStringUniversal(getContext(), mSetDate.getTime()));
        //save/update the alarm information
        try {
            mDatabaseHandler.open();

            String subTitle;
            if(mScheduleItem.getStartAt() != null) {
                subTitle = DateHelper.getShortDateTimeStringUniversal(getContext(), mScheduleItem.getStartAt());
            } else {
                subTitle = "";
            }

            mDatabaseHandler.createAlarm(mSetDate.get(Calendar.YEAR), mSetDate.get(Calendar.MONTH), mSetDate.get(Calendar.DAY_OF_MONTH), mSetDate.get(Calendar.HOUR_OF_DAY), mSetDate.get(Calendar.MINUTE), mScheduleItem.getId(), mScheduleItem.getTitle(), subTitle);

            mDatabaseHandler.close();

        } catch (SQLException e) {
            //couldn't save the alarm in the database, so stop here and don't actually create one. If the database
            //doesn't have the alarm in it, the user will think that it didn't save
            Toast.makeText(getContext(), getContext().getString(R.string.alarmNotSet), Toast.LENGTH_SHORT).show();
            mTimePicker.dismiss();
            return;
        }
        AlarmReceiver alarmReceiver = new AlarmReceiver();
        String subTitle;

        if(mScheduleItem.getStartAt() != null) {
            subTitle = DateHelper.getShortDateTimeStringUniversal(getContext(), mScheduleItem.getStartAt());
        } else {
            subTitle = "";
        }

        alarmReceiver.setAlarm(getContext(), mSetDate, mScheduleItem.getId(), mScheduleItem.getTitle(), subTitle);

        AnalyticUtils.trackButtonPressed(AnalyticUtils.REMINDER_EVENT);

        mTimePicker.dismiss();
    }

}
