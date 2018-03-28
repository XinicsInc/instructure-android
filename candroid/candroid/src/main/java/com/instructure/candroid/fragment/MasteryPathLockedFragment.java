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

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.instructure.candroid.R;
import com.instructure.interactions.FragmentInteractions;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.pandautils.utils.Const;

public class MasteryPathLockedFragment extends ParentFragment {
    //region views
    private TextView mModuleItemNameTextView;
    //endregion

    private String mModuleItemName;


    @Override
    @NonNull
    public FragmentInteractions.Placement getFragmentPlacement() {
        return FragmentInteractions.Placement.DETAIL;
    }

    @Override
    @NonNull
    public String title() {
        return getString(R.string.locked);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View rootView = inflater.inflate(R.layout.fragment_master_paths_locked, container, false);
        setupViews(rootView);


        return rootView;
    }

    private void setupViews(View rootView) {
        mModuleItemNameTextView = (TextView) rootView.findViewById(R.id.lockedDescription);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupTextViews();
    }

    @Override
    public void applyTheme() {

    }

    public void setupTextViews() {
        mModuleItemNameTextView.setText(mModuleItemName);
    }

    //region Intent

    @Override
    public void handleIntentExtras(Bundle extras) {
        super.handleIntentExtras(extras);

        mModuleItemName = extras.getString(Const.MODULE_ITEM);
    }


    public static Bundle createBundle(CanvasContext canvasContext, String moduleItemName) {
        Bundle extras = createBundle(canvasContext);
        extras.putString(Const.MODULE_ITEM, moduleItemName);
        return extras;
    }

    //endregion
    @Override
    public boolean allowBookmarking() {
        return false;
    }
}
