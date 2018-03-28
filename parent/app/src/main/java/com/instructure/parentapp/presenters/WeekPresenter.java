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

package com.instructure.parentapp.presenters;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.managers.CalendarEventManager;
import com.instructure.canvasapi2.managers.CourseManager;
import com.instructure.canvasapi2.models.Course;
import com.instructure.canvasapi2.models.ScheduleItem;
import com.instructure.canvasapi2.models.Student;
import com.instructure.canvasapi2.utils.APIHelper;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.LinkHeaders;
import com.instructure.canvasapi2.utils.Logger;
import com.instructure.parentapp.models.WeekHeaderItem;
import com.instructure.parentapp.util.AnalyticUtils;
import com.instructure.parentapp.viewinterface.WeekView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import instructure.androidblueprint.SyncExpandablePresenter;
import retrofit2.Response;

/**
 * Copyright (c) 2016 Instructure. All rights reserved.
 */
public class WeekPresenter extends SyncExpandablePresenter<WeekHeaderItem, ScheduleItem, WeekView> {

    private Student mStudent;
    private Course mCourse;

    private GregorianCalendar mStartDate = new GregorianCalendar();
    private GregorianCalendar mEndDate = new GregorianCalendar();

    private Map<Long, Course> mCourseMap = new HashMap<>();
    private Map<Integer, WeekHeaderItem> mHeaders = new HashMap<>(7);

    private static final int TIME_SPAN = Calendar.WEEK_OF_MONTH;

    public WeekPresenter(Student student, @Nullable Course course) {
        super(WeekHeaderItem.class, ScheduleItem.class);
        mStudent = student;
        mCourse = course;

        initCalendars();
        initHeaders();
    }

    @Override
    public int compare(WeekHeaderItem group1, WeekHeaderItem group2) {
        if(group1.getComparisonDate() != null && group2.getComparisonDate() != null) {
            return group1.getComparisonDate().compareTo(group2.getComparisonDate());
        }
        return super.compare(group1, group2);
    }

    @Override
    public void loadData(boolean forceNetwork) {
        setWeekText();
        if(mCourseMap == null || mCourseMap.size() == 0) {
            getCourses(forceNetwork);
        } else {
            getCalendarEvents(forceNetwork);
        }
    }

    @Override
    public void refresh(boolean forceNetwork) {
        onRefreshStarted();
        mCoursesCallback.reset();
        mCalendarEventsCallback.reset();
        clearData();
        //Additionally, courses need to be cleared, as they could be leftover from a previously selected student
        mCourseMap.clear();
        loadData(forceNetwork);
    }

    private void getCourses(boolean forceNetwork) {
        if(getViewCallback() != null) {
            CourseManager.getCoursesForUserAirwolf(
                    getViewCallback().airwolfDomain(),
                    mStudent.getParentId(),
                    mStudent.getStudentId(),
                    forceNetwork,
                    mCoursesCallback
            );
        }
    }

    private void getCalendarEvents(boolean forceNetwork) {
        if(getViewCallback() != null) {
            CalendarEventManager.getAllCalendarEventsWithSubmissionsAirwolf(
                    getViewCallback().airwolfDomain(),
                    mStudent.getParentId(),
                    mStudent.getStudentId(),
                    APIHelper.dateToString(mStartDate),
                    APIHelper.dateToString(mEndDate),
                    getContextCodes(),
                    forceNetwork,
                    mCalendarEventsCallback
            );
        }
    }

    private @NonNull
    ArrayList<String> getContextCodes(){
        ArrayList<String> contextCodes =  new ArrayList<>();
        for(Course course : getCourses()) {
            contextCodes.add(course.getContextId());
        }
        return contextCodes;
    }

    public Student getStudent() {
        return mStudent;
    }

    @Nullable
    public Course getCourse() {
        return mCourse;
    }

    public void setStudent(Student student, boolean refresh) {
        mStudent = student;
        if(refresh) {
            refresh(false);
        }
    }

    private StatusCallback<List<ScheduleItem>> mCalendarEventsCallback = new StatusCallback<List<ScheduleItem>>(){
        @Override
        public void onResponse(@NonNull Response<List<ScheduleItem>> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
            publishScheduleItem(response.body());
        }

        @Override
        public void onFinished(ApiType type) {
            if(getViewCallback() != null) {
                getViewCallback().onRefreshFinished();
                getViewCallback().checkIfEmpty();
            }
        }

        private void publishScheduleItem(List<ScheduleItem> items) {
            for(ScheduleItem item : items) {
                if (item.hasAssignmentOverrides() && item.getAssignmentOverrides().get(0).dueAt != null) {
                    Calendar date = dateToCalendar(item.getAssignmentOverrides().get(0).dueAt);
                    amendDateAndUpdate(item, date);
                } else if (item.isAllDay() && item.getAllDayDate() != null) {
                    Calendar date = dateToCalendar(item.getAllDayDate());
                    amendDateAndUpdate(item, date);
                } else if (item.getStartAt() != null) {
                    Calendar date = dateToCalendar(item.getStartAt());
                    amendDateAndUpdate(item, date);
                } else {
                    Logger.e("Could not parse schedule item, invalid date: " + item.getId());
                }
            }
        }

        private void amendDateAndUpdate(ScheduleItem item, Calendar date) {
            final int dayOfWeek = date.get(Calendar.DAY_OF_WEEK);
            WeekHeaderItem group = mHeaders.get(dayOfWeek);
            group.setDate(date);
            getData().addOrUpdateItem(group, item);
        }
    };

    private StatusCallback<List<Course>> mCoursesCallback = new StatusCallback<List<Course>>(){
        @Override
        public void onResponse(@NonNull Response<List<Course>> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
            addToMap(response.body());
            getCalendarEvents(false);
        }
    };

    //region Calendar Management

    private GregorianCalendar dateToCalendar(@NonNull Date date) {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(date.getTime());
        return calendar;
    }

    //endregion

    //region Next, Prev, Date Text

    private void addToMap(List<Course> courses) {
        for(Course course : courses) {
            //we won't be able to get the name if the course hasn't started yet or if the user doesn't have
            //access to the course. So we don't want to add the course to the list
            if(!TextUtils.isEmpty(course.getName())) {
                mCourseMap.put(course.getId(), course);
            }
        }
    }

    public ArrayList<Course> getCourses() {
        if(mCourse != null) {
            ArrayList<Course> courses = new ArrayList<>();
            courses.add(mCourse);
            return courses;
        } else {
            return new ArrayList<>(mCourseMap.values());
        }
    }

    public Map<Long, Course> getCoursesMap() {
        if(mCourse != null) {
            Map<Long, Course> map = new HashMap<>();
            map.put(mCourse.getId(), mCourse);
            return map;
        } else {
            return mCourseMap;
        }
    }

    public void nextWeekClicked() {
        if(mCourse == null) {
            AnalyticUtils.trackButtonPressed(AnalyticUtils.WEEK_NAV_NEXT);
        } else {
            AnalyticUtils.trackFlow(AnalyticUtils.COURSE_FLOW, AnalyticUtils.WEEK_NAV_NEXT);
        }

        mStartDate.add(TIME_SPAN, 1);
        adjustEndTime();
        setWeekText();
        refresh(false);
    }

    public void prevWeekClicked() {
        if(mCourse == null) {
            AnalyticUtils.trackButtonPressed(AnalyticUtils.WEEK_NAV_PREVIOUS);
        } else {
            AnalyticUtils.trackFlow(AnalyticUtils.COURSE_FLOW, AnalyticUtils.WEEK_NAV_PREVIOUS);
        }

        mStartDate.add(TIME_SPAN, -1);
        adjustEndTime();
        setWeekText();
        refresh(false);
    }

    private void adjustEndTime() {
        mEndDate.setTimeInMillis(mStartDate.getTimeInMillis());
        mEndDate.add(TIME_SPAN, 1);
    }

    private void setWeekText() {
        ArrayList<GregorianCalendar> dates = new ArrayList<>(2);
        dates.add(0, mStartDate);
        mEndDate.add(Calendar.SECOND, -1);
        dates.add(1, mEndDate);
        if(getViewCallback() != null) {
            getViewCallback().updateWeekText(dates);
        }
        mEndDate.add(Calendar.SECOND,  1);
    }

    public void onNewDatePicked(GregorianCalendar datePicked) {
        mStartDate = datePicked;
        initCalendars();
        refresh(false);
    }

    //endregion

    //region Setup

    private void initCalendars() {
        cleanCalendar(mStartDate);
        adjustEndTime();
    }

    private void initHeaders() {
        mHeaders.put(Calendar.SUNDAY, new WeekHeaderItem(Calendar.SUNDAY));
        mHeaders.put(Calendar.MONDAY, new WeekHeaderItem(Calendar.MONDAY));
        mHeaders.put(Calendar.TUESDAY, new WeekHeaderItem(Calendar.TUESDAY));
        mHeaders.put(Calendar.WEDNESDAY, new WeekHeaderItem(Calendar.WEDNESDAY));
        mHeaders.put(Calendar.THURSDAY, new WeekHeaderItem(Calendar.THURSDAY));
        mHeaders.put(Calendar.FRIDAY, new WeekHeaderItem(Calendar.FRIDAY));
        mHeaders.put(Calendar.SATURDAY, new WeekHeaderItem(Calendar.SATURDAY));
    }

    private void cleanCalendar(GregorianCalendar calendar) {
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    //endregion
}
