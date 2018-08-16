/*
 * Copyright (C) 2017 - present Instructure, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.instructure.candroid.test.adapter;

import android.content.Context;
import android.test.InstrumentationTestCase;

import com.instructure.candroid.adapter.CalendarListRecyclerAdapter;
import com.instructure.canvasapi2.models.Assignment;
import com.instructure.canvasapi2.models.ScheduleItem;
import com.instructure.canvasapi2.utils.APIHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Calendar;
import java.util.Date;

@Config(sdk = 19)
@RunWith(RobolectricTestRunner.class)
public class CalendarListRecyclerAdapterTest extends InstrumentationTestCase {
    private CalendarListRecyclerAdapter mAdapter;

    /**
     * Make it so the protected constructor can be called
     */
    public static class CalendarListRecyclerAdapterWrapper extends CalendarListRecyclerAdapter {
        protected CalendarListRecyclerAdapterWrapper(Context context) {
            super(context);
        }
    }

    @Before
    public void setup(){
        mAdapter = new CalendarListRecyclerAdapterWrapper(RuntimeEnvironment.application.getApplicationContext());
    }

    @Test
    public void testAreContentsTheSame_noAssignmentSame(){
        ScheduleItem scheduleItem1 = new ScheduleItem();
        scheduleItem1.setTitle("ScheduleItem1");
        scheduleItem1.setStartAt(new Date());
        assertTrue(mAdapter.createItemCallback().areContentsTheSame(scheduleItem1, scheduleItem1));
    }

    @Test
    public void testAreContentsTheSame_noAssignmentDifferentName(){
        ScheduleItem scheduleItem1 = new ScheduleItem();
        scheduleItem1.setTitle("ScheduleItem1a");
        Date date = new Date();
        scheduleItem1.setStartAt(date);
        ScheduleItem scheduleItem2 = new ScheduleItem();
        scheduleItem2.setTitle("ScheduleItem1b");
        scheduleItem2.setStartAt(date);
        assertFalse(mAdapter.createItemCallback().areContentsTheSame(scheduleItem1, scheduleItem2));
    }

    @Test
    public void testAreContentsTheSame_noAssignmentDifferentDate(){
        ScheduleItem scheduleItem1 = new ScheduleItem();
        scheduleItem1.setTitle("ScheduleItem1a");
        scheduleItem1.setStartAt(new Date(Calendar.getInstance().getTimeInMillis() - 1000));
        ScheduleItem scheduleItem2 = new ScheduleItem();
        scheduleItem2.setTitle("ScheduleItem1a");
        scheduleItem2.setStartAt(new Date(Calendar.getInstance().getTimeInMillis() + 1000));
        assertFalse(mAdapter.createItemCallback().areContentsTheSame(scheduleItem1, scheduleItem2));
    }

    @Test
    public void testAreContentsTheSame_sameAssignment(){
        ScheduleItem scheduleItem1 = new ScheduleItem();
        scheduleItem1.setTitle("ScheduleItem1");
        scheduleItem1.setStartAt(new Date());
        Assignment assignment1 = new Assignment();
        assignment1.setDueAt(APIHelper.dateToString(new Date()));
        scheduleItem1.setAssignment(assignment1);
        assertTrue(mAdapter.createItemCallback().areContentsTheSame(scheduleItem1, scheduleItem1));
    }

    @Test
    public void testAreContentsTheSame_differentAssignment(){
        ScheduleItem scheduleItem1 = new ScheduleItem();
        scheduleItem1.setTitle("ScheduleItem1");
        Date date = new Date();
        scheduleItem1.setStartAt(date);
        Assignment assignment1 = new Assignment();
        assignment1.setDueAt(APIHelper.dateToString(new Date(Calendar.getInstance().getTimeInMillis() - 1000)));
        scheduleItem1.setAssignment(assignment1);

        ScheduleItem scheduleItem2 = new ScheduleItem();
        scheduleItem2.setTitle("ScheduleItem1");
        scheduleItem2.setStartAt(date);
        Assignment assignment2 = new Assignment();
        assignment2.setDueAt(APIHelper.dateToString(new Date(Calendar.getInstance().getTimeInMillis() + 1000)));
        scheduleItem2.setAssignment(assignment2);

        assertFalse(mAdapter.createItemCallback().areContentsTheSame(scheduleItem1, scheduleItem2));
    }

    @Test
    public void testAreContentsTheSame_nullAssignment() {
        ScheduleItem scheduleItem1 = new ScheduleItem();
        scheduleItem1.setTitle("ScheduleItem1");
        Date date = new Date();
        scheduleItem1.setStartAt(date);
        Assignment assignment1 = new Assignment();
        assignment1.setDueAt(APIHelper.dateToString(date));
        scheduleItem1.setAssignment(assignment1);

        ScheduleItem scheduleItem2 = new ScheduleItem();
        scheduleItem2.setTitle("ScheduleItem1");
        scheduleItem2.setStartAt(date);
        Assignment assignment2 = null;
        scheduleItem2.setAssignment(assignment2);

        assertFalse(mAdapter.createItemCallback().areContentsTheSame(scheduleItem1, scheduleItem2));
    }
}
