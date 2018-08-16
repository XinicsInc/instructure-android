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

package com.instructure.interactions;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.View;

public interface Navigation {

    <F extends Fragment & FragmentInteractions> void addFragment(@NonNull F fragment);
    <F extends Fragment & FragmentInteractions> void addFragment(@NonNull F fragment, boolean ignoreDebounce);
    <F extends Fragment & FragmentInteractions> void addFragment(@NonNull F fragment, int inAnimation, int outAnimation);
    <F extends Fragment & FragmentInteractions> void addFragment(@NonNull F fragment, int transitionId, View sharedElement);

    @Nullable Fragment getTopFragment();
    @Nullable Fragment getPeekingFragment();
    @Nullable Fragment getCurrentFragment();

    void popCurrentFragment();
    void updateCalendarStartDay();
    void addBookmark();

    <F extends Fragment & FragmentInteractions> void attachNavigationDrawer(@NonNull F fragment, @NonNull Toolbar toolbar);
}
