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
import android.text.TextUtils;

import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.managers.CourseManager;
import com.instructure.canvasapi2.models.Course;
import com.instructure.canvasapi2.models.Student;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.LinkHeaders;
import com.instructure.parentapp.viewinterface.CourseListView;

import java.util.List;

import instructure.androidblueprint.SyncPresenter;
import retrofit2.Response;

/**
 * Copyright (c) 2016 Instructure. All rights reserved.
 */
public class CourseListPresenter extends SyncPresenter<Course, CourseListView> {

    private Student mStudent;

    public CourseListPresenter(Student student) {
        super(Course.class);
        mStudent = student;
    }

    @Override
    public void loadData(boolean forceNetwork) {
        if(getViewCallback() != null) {
            CourseManager.getCoursesForUserAirwolf(
                    getViewCallback().airwolfDomain(),
                    getViewCallback().parentId(),
                    mStudent.getStudentId(),
                    forceNetwork,
                    mCoursesCallback
            );
        }
    }

    @Override
    public void refresh(boolean forceNetwork) {
        onRefreshStarted();
        mCoursesCallback.reset();
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

    private StatusCallback<List<Course>> mCoursesCallback = new StatusCallback<List<Course>>(){
        @Override
        public void onResponse(@NonNull Response<List<Course>> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
            for(Course course : response.body()) {
                if(!TextUtils.isEmpty(course.getName())) {
                    getData().addOrUpdate(course);
                }
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

    @Override
    public int compare(Course o1, Course o2) {
        return o1.compareTo(o2);
    }

    @Override
    public boolean areContentsTheSame(Course oldItem, Course newItem) {
        return false;
    }

    @Override
    public boolean areItemsTheSame(Course item1, Course item2) {
        return item1.getContextId().hashCode() == item2.getContextId().hashCode();
    }
}
