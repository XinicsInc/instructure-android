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
import com.instructure.candroid.adapter.TodoListRecyclerAdapter;
import com.instructure.canvasapi2.models.Assignment;
import com.instructure.canvasapi2.models.ScheduleItem;
import com.instructure.canvasapi2.models.ToDo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@Config(sdk = 19)
@RunWith(RobolectricTestRunner.class)
public class TodoListRecyclerAdapterTest extends InstrumentationTestCase {
    private TodoListRecyclerAdapter mAdapter;

    /**
     * Make it so the protected constructor can be called
     */
    public static class TodoListRecyclerAdapterWrapper extends TodoListRecyclerAdapter {
        protected TodoListRecyclerAdapterWrapper(Context context) {
            super(context);
        }
    }

    @Before
    public void setup(){
        mAdapter = new TodoListRecyclerAdapterWrapper(RuntimeEnvironment.application.getApplicationContext());
    }

    @Test
    public void testAreContentsTheSame_SameTitleFromAssignment(){
        ToDo item = new ToDo();
        Assignment assignment = new Assignment();
        assignment.setName("item");
        item.setAssignment(assignment);

        assertTrue(mAdapter.createItemCallback().areContentsTheSame(item, item));
    }

    @Test
    public void testAreContentsTheSame_SameTitleFromSchedule(){
        ToDo item = new ToDo();
        ScheduleItem scheduleItem = new ScheduleItem();
        scheduleItem.setTitle("item");
        item.setScheduleItem(scheduleItem);

        assertTrue(mAdapter.createItemCallback().areContentsTheSame(item, item));
    }

    @Test
    public void testAreContentsTheSame_DifferentTitleFromAssignment(){
        ToDo item = new ToDo();
        Assignment assignment = new Assignment();
        assignment.setName("item");
        item.setAssignment(assignment);
        ToDo item1 = new ToDo();
        Assignment assignment1 = new Assignment();
        assignment1.setName("item1");
        item1.setAssignment(assignment1);

        assertFalse(mAdapter.createItemCallback().areContentsTheSame(item, item1));
    }

    @Test
    public void testAreContentsTheSame_DifferentTitleFromSchedule(){
        ToDo item = new ToDo();
        ScheduleItem scheduleItem = new ScheduleItem();
        scheduleItem.setTitle("item");
        item.setScheduleItem(scheduleItem);
        ToDo item1 = new ToDo();
        ScheduleItem scheduleItem1 = new ScheduleItem();
        scheduleItem1.setTitle("item1");
        item1.setScheduleItem(scheduleItem1);

        assertFalse(mAdapter.createItemCallback().areContentsTheSame(item, item1));
    }
}

