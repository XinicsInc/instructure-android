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

import com.instructure.candroid.adapter.FileListRecyclerAdapter;
import com.instructure.canvasapi2.models.FileFolder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@Config(sdk = 19)
@RunWith(RobolectricTestRunner.class)
public class FileListRecyclerAdapterTest extends InstrumentationTestCase {
    private FileListRecyclerAdapter mAdapter;

    public static class FileListRecyclerAdapterWrapper extends FileListRecyclerAdapter {
        protected FileListRecyclerAdapterWrapper(Context context) { super(context, null, false, new FileFolder(), null, true); }
    }

    @Before
    public void setup(){
        mAdapter = new FileListRecyclerAdapterWrapper(RuntimeEnvironment.application);
    }

    @Test
    public void testAreContentsTheSame_SameObjects(){
        FileFolder fileFolder = new FileFolder();
        fileFolder.setDisplayName("fileFolder");
        fileFolder.setSize(0);
        assertTrue(mAdapter.getItemCallback().areContentsTheSame(fileFolder, fileFolder));
    }

    @Test
    public void testAreContentsTheSame_DifferentObjectNames(){
        FileFolder fileFolder1 = new FileFolder();
        fileFolder1.setDisplayName("fileFolder1");
        fileFolder1.setSize(100);

        FileFolder fileFolder2 = new FileFolder();
        fileFolder2.setDisplayName("fileFolder2");
        fileFolder2.setSize(100);

        assertFalse(mAdapter.getItemCallback().areContentsTheSame(fileFolder1, fileFolder2));
    }

    @Test
    public void testAreContentsTheSame_DifferentObjectSizes(){
        FileFolder fileFolder1 = new FileFolder();
        fileFolder1.setDisplayName("fileFolder");
        fileFolder1.setSize(10);

        FileFolder fileFolder2 = new FileFolder();
        fileFolder2.setDisplayName("fileFolder");
        fileFolder2.setSize(100);

        assertFalse(mAdapter.getItemCallback().areContentsTheSame(fileFolder1, fileFolder2));
    }

    @Test
    public void testAreContentsTheSame_SameFolders(){
        FileFolder fileFolder = new FileFolder();
        fileFolder.setName("fileFolder");
        fileFolder.setSize(0);
        assertTrue(mAdapter.getItemCallback().areContentsTheSame(fileFolder, fileFolder));
    }

    @Test
    public void testAreContentsTheSame_DifferentFolderNames(){
        FileFolder fileFolder1 = new FileFolder();
        fileFolder1.setName("fileFolder1");
        fileFolder1.setSize(100);

        FileFolder fileFolder2 = new FileFolder();
        fileFolder2.setName("fileFolder2");
        fileFolder2.setSize(100);

        assertFalse(mAdapter.getItemCallback().areContentsTheSame(fileFolder1, fileFolder2));
    }

    @Test
    public void testAreContentsTheSame_DifferentFolderSizes(){
        FileFolder fileFolder1 = new FileFolder();
        fileFolder1.setName("fileFolder");
        fileFolder1.setSize(10);

        FileFolder fileFolder2 = new FileFolder();
        fileFolder2.setName("fileFolder");
        fileFolder2.setSize(100);

        assertFalse(mAdapter.getItemCallback().areContentsTheSame(fileFolder1, fileFolder2));
    }

}
