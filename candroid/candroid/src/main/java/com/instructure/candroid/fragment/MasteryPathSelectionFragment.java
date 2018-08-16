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
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.instructure.candroid.R;
import com.instructure.interactions.FragmentInteractions;
import com.instructure.canvasapi2.models.Course;
import com.instructure.canvasapi2.models.MasteryPath;
import com.instructure.pandautils.utils.ColorKeeper;
import com.instructure.pandautils.utils.Const;

import java.lang.ref.WeakReference;
import java.util.Locale;


public class MasteryPathSelectionFragment extends ParentFragment {

    private int currentTab = 0;

    private TabLayout mTabLayout;

    // views
    private ViewPager mViewPager;

    // fragments
    private FragmentPagerDetailAdapter mFragmentPagerAdapter;

    // model variables
    private MasteryPath mMasteryPath;
    private Course mCourse;

    private long mModuleObjectId;
    private long mModuleItemId;

    @Override
    @NonNull
    public FragmentInteractions.Placement getFragmentPlacement() {return FragmentInteractions.Placement.DETAIL; }

    @Override
    @NonNull
    public String title() {
        return getString(R.string.choose_assignment_group);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // DO NOT USE setRetainInstance. It breaks the FragmentStatePagerAdapter.
        // The error is "Attempt to invoke virtual method 'int java.util.ArrayList.size()' on a null object reference"
        // setRetainInstance(this, true);
    }

     @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_assignment, container, false);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mTabLayout.setTabMode((!isTablet(getContext()) && !isLandscape(getContext()) ? TabLayout.MODE_SCROLLABLE : TabLayout.MODE_FIXED));
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mTabLayout = (TabLayout) view.findViewById(R.id.tabLayout);
        mViewPager = (ViewPager) view.findViewById(R.id.pager);
        mViewPager.setOffscreenPageLimit(2);
        mViewPager.setSaveFromParentEnabled(false); // Prevents a crash with FragmentStatePagerAdapter
        mFragmentPagerAdapter = new FragmentPagerDetailAdapter(getChildFragmentManager());
    }

    private TabLayout.OnTabSelectedListener tabSelectedListener = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            mViewPager.setCurrentItem(tab.getPosition(), true);
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {}

        @Override
        public void onTabReselected(TabLayout.Tab tab) {}
    };

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState){
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putParcelable(Const.CANVAS_CONTEXT, mCourse);
        savedInstanceState.putParcelable(Const.MASTERY_PATH, mMasteryPath);
        if (mViewPager != null) {
            savedInstanceState.putInt(Const.TAB_ID, mViewPager.getCurrentItem());
            currentTab = mViewPager.getCurrentItem();
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mViewPager.setAdapter(mFragmentPagerAdapter);
        setupTabLayoutColors();
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabLayout));
        mTabLayout.setTabMode((!isTablet(getContext()) && !isLandscape(getContext()) ? TabLayout.MODE_SCROLLABLE : TabLayout.MODE_FIXED));
        mTabLayout.setupWithViewPager(mViewPager);
        mTabLayout.addOnTabSelectedListener(tabSelectedListener);

        if (savedInstanceState != null) {
            currentTab = savedInstanceState.getInt(Const.TAB_ID, 0);
        }
        // currentTab can either be save on orientation change or handleIntentExtras (when someone opens a link from an email)
        mViewPager.setCurrentItem(currentTab);

    }

    private void setupTabLayoutColors() {
        int color = ColorKeeper.getOrGenerateColor(getCanvasContext());
        mTabLayout.setBackgroundColor(color);
        mTabLayout.setTabTextColors(getResources().getColor(R.color.glassWhite), Color.WHITE);
    }

    @Override
    public void applyTheme() {

    }

    //region Adapter

    class FragmentPagerDetailAdapter extends FragmentStatePagerAdapter {
        // http://stackoverflow.com/questions/8785221/retrieve-a-fragment-from-a-viewpager
        SparseArray<WeakReference<Fragment>> registeredFragments;

        public FragmentPagerDetailAdapter(FragmentManager fm) {
            super(fm);
            registeredFragments = new SparseArray<>();
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            registeredFragments.put(position, new WeakReference<>(fragment));
            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            registeredFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        public Fragment getRegisteredFragment(int position) {
            WeakReference<Fragment> weakReference = registeredFragments.get(position);
            if (weakReference != null) {
                return weakReference.get();
            }
            return null;
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment;
            Bundle bundle;

            bundle = MasteryPathOptionsFragment.createBundle(getCanvasContext(), mMasteryPath.getAssignmentSets()[position].getAssignments(), mMasteryPath.getAssignmentSets()[position], mModuleObjectId, mModuleItemId);
            fragment = ParentFragment.createFragment(MasteryPathOptionsFragment.class, bundle);

            return fragment;
        }

        @Override
        public int getCount() {
            return mMasteryPath.getAssignmentSets().length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return String.format(Locale.getDefault(), getString(R.string.choice_position), (position+1));
        }
    }



    //endregion


    @Override
    public void handleIntentExtras(Bundle extras) {
        super.handleIntentExtras(extras);

        if(extras == null){return;}

        mCourse = (Course)getCanvasContext();

        if (extras.containsKey(Const.MASTERY_PATH)) {
            mMasteryPath =  extras.getParcelable(Const.MASTERY_PATH);
        }

        mModuleObjectId = extras.getLong(Const.MODULE_ID);
        mModuleItemId = extras.getLong(Const.MODULE_ITEM);
    }

    public static Bundle createBundle(Course course, MasteryPath masteryPath, long moduleObjectId, long moduleItemId) {
        Bundle extras = createBundle(course);
        extras.putParcelable(Const.MASTERY_PATH, masteryPath);
        extras.putLong(Const.MODULE_ID, moduleObjectId);
        extras.putLong(Const.MODULE_ITEM, moduleItemId);
        return extras;
    }

    @Override
    public boolean allowBookmarking() {
       return false;
    }
}
