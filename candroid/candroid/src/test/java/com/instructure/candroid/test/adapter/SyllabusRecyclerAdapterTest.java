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
import com.instructure.candroid.adapter.SyllabusRecyclerAdapter;
import com.instructure.canvasapi2.models.ScheduleItem;
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
public class SyllabusRecyclerAdapterTest extends InstrumentationTestCase{
    private SyllabusRecyclerAdapter mAdapter;

    /**
     * Make it so the protected constructor can be called
     */
    public static class SyllabusRecyclerAdapterWrapper extends SyllabusRecyclerAdapter {
        protected SyllabusRecyclerAdapterWrapper(Context context) {
            super(context);
        }
    }

    @Before
    public void setup(){
        mAdapter = new SyllabusRecyclerAdapterWrapper(RuntimeEnvironment.application.getApplicationContext());
    }

    @Test
    public void areContentsTheSame_NotNullSameDate(){
        ScheduleItem item = new ScheduleItem();
        item.setTitle("item");
        item.setStartAt(new Date());

        assertTrue(mAdapter.createItemCallback().areContentsTheSame(item, item));
    }

    @Test
    public void areContentsTheSame_NotNullDifferentDate(){
        ScheduleItem item = new ScheduleItem();
        item.setTitle("item");
        item.setStartAt(new Date(Calendar.getInstance().getTimeInMillis() + 1000));

        ScheduleItem item1 = new ScheduleItem();
        item1.setTitle("item");
        item1.setStartAt(new Date(Calendar.getInstance().getTimeInMillis() - 1000));

        assertFalse(mAdapter.createItemCallback().areContentsTheSame(item, item1));
    }

    @Test
    public void areContentsTheSame_NullDate(){
        ScheduleItem item = new ScheduleItem();
        item.setTitle("item");
        item.setStartAt(new Date());

        ScheduleItem item1 = new ScheduleItem();
        item1.setTitle("item");
        item1.setStartAt(null);

        assertFalse(mAdapter.createItemCallback().areContentsTheSame(item, item1));
    }

}
