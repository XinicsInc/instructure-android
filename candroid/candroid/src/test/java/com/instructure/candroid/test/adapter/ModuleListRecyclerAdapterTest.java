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
import com.instructure.candroid.adapter.ModuleListRecyclerAdapter;
import com.instructure.canvasapi2.models.ModuleItem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@Config(sdk = 19)
@RunWith(RobolectricTestRunner.class)
public class ModuleListRecyclerAdapterTest  extends InstrumentationTestCase {
    private ModuleListRecyclerAdapter mAdapter;

    /**
     * Make it so the protected constructor can be called
     */
    public static class ModuleListRecyclerAdapterWrapper extends ModuleListRecyclerAdapter {
        protected ModuleListRecyclerAdapterWrapper(Context context) {
            super(context);
        }
    }

    @Before
    public void setup(){
        mAdapter = new ModuleListRecyclerAdapterWrapper(RuntimeEnvironment.application.getApplicationContext());
    }

    @Test
    public void testAreContentsTheSame_SameModule(){
        ModuleItem item = new ModuleItem();
        item.setTitle("item");

        assertTrue(mAdapter.createItemCallback().areContentsTheSame(item, item));
    }

    @Test
    public void testAreContentsTheSame_DiffModule(){
        ModuleItem item = new ModuleItem();
        item.setTitle("item");

        ModuleItem item1 = new ModuleItem();
        item1.setTitle("item1");
        assertFalse(mAdapter.createItemCallback().areContentsTheSame(item, item1));
    }
}
