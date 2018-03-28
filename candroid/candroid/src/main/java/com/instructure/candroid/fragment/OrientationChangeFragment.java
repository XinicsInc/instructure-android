/*
 * Copyright (C) 2016 - present Instructure, Inc.
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

package com.instructure.candroid.fragment;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.instructure.candroid.R;

/**
 * Used for when a fragment has mutiple layouts (portrait and landscape) Deals with configuration change events.
 */
public abstract class OrientationChangeFragment extends ParentFragment {

    private FrameLayout mRootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = (FrameLayout)inflater.inflate(R.layout.fragment_orientation, container, false);
        mRootView.addView(populateView(inflater, mRootView));
        return mRootView;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(mRootView == null) return;

        mRootView.removeAllViews();
        mRootView.addView(populateView(getLayoutInflater(), mRootView));
    }

    public abstract View populateView(LayoutInflater inflater, ViewGroup container);
}
