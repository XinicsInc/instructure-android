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

import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.managers.AlertManager;
import com.instructure.canvasapi2.managers.CourseManager;
import com.instructure.canvasapi2.models.Alert;
import com.instructure.canvasapi2.models.Course;
import com.instructure.canvasapi2.models.Student;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.LinkHeaders;
import com.instructure.parentapp.viewinterface.AlertView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import instructure.androidblueprint.SyncPresenter;
import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Copyright (c) 2016 Instructure. All rights reserved.
 */
public class AlertPresenter extends SyncPresenter<Alert, AlertView> {

    private Student mStudent;
    private Map<String, Course> mCourseMap = new HashMap<>();

    public AlertPresenter(Student student) {
        super(Alert.class);
        mStudent = student;
    }

    @Override
    public void loadData(boolean forceNetwork) {
        if(getViewCallback() != null) {
            AlertManager.getAlertsAirwolf(
                    getViewCallback().airwolfDomain(),
                    getViewCallback().parentId(),
                    mStudent.getStudentId(),
                    forceNetwork,
                    mAlertsCallback
            );
        }
    }

    @Override
    public void refresh(boolean forceNetwork) {
        onRefreshStarted();
        mAlertsCallback.reset();
        clearData();
        loadData(forceNetwork);
    }

    public Student getStudent() {
        return mStudent;
    }

    public void setStudent(Student student, boolean refresh) {
        mStudent = student;
        if(refresh) {
            refresh(false);
        }
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

    public void markAlertAsRead(String alertId, final int position) {
        if(getViewCallback() != null) {
            AlertManager.markAlertAsRead(
                    getViewCallback().airwolfDomain(),
                    mStudent.getParentId(),
                    alertId,
                    new StatusCallback<ResponseBody>(){
                        @Override
                        public void onResponse(@NonNull Response<ResponseBody> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                            if(getViewCallback() != null) {
                                getViewCallback().markPositionAsRead(position);
                            }
                        }
                    }
            );
        }
    }

    public void markAlertAsDismissed(String alertId) {
        if(getViewCallback() != null) {
            AlertManager.markAlertAsDismissed(
                    getViewCallback().airwolfDomain(),
                    mStudent.getParentId(),
                    alertId,
                    new StatusCallback<ResponseBody>(){
                        @Override
                        public void onResponse(@NonNull Response<ResponseBody> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {}
                    }
            );
        }

    }
    private void getAlerts(boolean forceNetwork) {
        if(getViewCallback() != null) {
            AlertManager.getAlertsAirwolf(
                    getViewCallback().airwolfDomain(),
                    getViewCallback().parentId(),
                    mStudent.getStudentId(),
                    forceNetwork,
                    mAlertsCallback
            );
        }
    }

    private StatusCallback<List<Alert>> mAlertsCallback = new StatusCallback<List<Alert>>() {
        @Override
        public void onResponse(@NonNull Response<List<Alert>> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
            List<Alert> alerts = new ArrayList<>();
            for(Alert alert : response.body()) {
                //Not very efficient, adds a course to alerts for use in the Binder
                if(!alert.isDismissed()) {
                    if (mCourseMap.containsKey(alert.getCourseId())) {
                        alert.setCourse(mCourseMap.get(alert.getCourseId()));
                    }
                    alerts.add(alert);
                }
            }
            getData().addOrUpdate(alerts);
            if(getViewCallback() != null) {
                getViewCallback().updateUnreadCount();
            }
        }

        @Override
        public void onFinished(ApiType type) {
            if(getViewCallback() != null) {
                getViewCallback().onRefreshFinished();
                getViewCallback().checkIfEmpty();
            }
        }
    };

    private StatusCallback<List<Course>> mCoursesCallback = new StatusCallback<List<Course>>(){
        @Override
        public void onResponse(@NonNull Response<List<Course>> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
            addToMap(response.body());
            getAlerts(false);
        }
    };

    private void addToMap(List<Course> courses) {
        for(Course course : courses) {
            mCourseMap.put(Long.toString(course.getId()), course);
        }
    }

    @Override
    public int compare(Alert item1, Alert item2) {
        return sortAlerts(item1, item2);
    }

    @Override
    public boolean areContentsTheSame(Alert item1, Alert item2) {
        return compareAlerts(item1, item2);
    }

    private boolean compareAlerts(Alert oldAlert, Alert newAlert){
        if(oldAlert.getTitle() != null && newAlert.getTitle() != null){
            boolean sameTitle = oldAlert.getTitle().equals(newAlert.getTitle());
            boolean sameState = oldAlert.isMarkedRead() == newAlert.isMarkedRead();
            return sameState && sameTitle;
        }
        return false;
    }

    private int sortAlerts(Alert o1, Alert o2){
        //First compare the read status of the alerts
        int firstCompare = (o1.isMarkedRead() == o2.isMarkedRead() ? 0 : (o2.isMarkedRead() ? -1 : 1));
        if(firstCompare != 0) {
            //If they read status doesn't match, use that
            return firstCompare;
        } else {
            //otherwise, check if the date is null
            if(o1.getActionDate() == null && o2.getActionDate() == null) {
                return 0;
            } else if(o1.getActionDate() == null && o2.getActionDate() != null) {
                return -1;
            } else if(o1.getActionDate() != null && o2.getActionDate() == null) {
                return 1;
            } else {
                //If the read status is the same, and the dates aren't null, compare them
                return o2.getActionDate().compareTo(o1.getActionDate());
            }
        }
    }
}
