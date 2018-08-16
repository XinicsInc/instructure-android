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
import com.instructure.candroid.adapter.PageListRecyclerAdapter;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.Page;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@Config(sdk = 19)
@RunWith(RobolectricTestRunner.class)
public class PageListRecycerAdapterTest extends InstrumentationTestCase {
    private PageListRecyclerAdapter mAdapter;

    public static class PageListRecyclerAdapterWrapper extends PageListRecyclerAdapter {
        protected PageListRecyclerAdapterWrapper(Context context) { super(context, CanvasContext.emptyCourseContext(), null, "", false);}
    }

    @Before
    public void setup() {
        mAdapter = new PageListRecyclerAdapterWrapper(RuntimeEnvironment.application);
    }

    @Test
    public void testAreContentsTheSame_titleSame() {
        Page page = new Page();
        page.setTitle("HI");
        assertTrue(mAdapter.getItemCallback().areContentsTheSame(page, page));
    }

    @Test
    public void testAreContentsTheSame_titleDifferent() {
        Page page1 = new Page();
        page1.setTitle("HI");
        Page page2 = new Page();
        page1.setTitle("HI I AM SUPER DIFFERENT");
        assertFalse(mAdapter.getItemCallback().areContentsTheSame(page1, page2));
    }

}
