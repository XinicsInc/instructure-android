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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.instructure.candroid.R;
import com.instructure.interactions.Navigation;
import com.instructure.interactions.FragmentInteractions;
import com.instructure.candroid.util.Const;
import com.instructure.candroid.util.ModuleUtility;
import com.instructure.candroid.util.Param;
import com.instructure.candroid.view.ViewPagerNonSwipeable;
import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.managers.ModuleManager;
import com.instructure.canvasapi2.models.Course;
import com.instructure.canvasapi2.models.ModuleItem;
import com.instructure.canvasapi2.models.ModuleObject;
import com.instructure.canvasapi2.models.Tab;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.LinkHeaders;
import com.instructure.pandautils.utils.ColorKeeper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Response;

public class CourseModuleProgressionFragment extends ParentFragment {
    //View variables
    private TextView moduleName;
    private ImageView moduleNameIcon;
    private Button prev;
    private Button next;
    private RelativeLayout rootView;
    private View markDoneWrapper, markDoneButton;
    private CheckBox markDoneCheckBox;
    //default number will get reset
    private int NUM_ITEMS = 3;

    private CourseModuleProgressionAdapter adapter;
    private ViewPagerNonSwipeable viewPager;

    private Course course;
    private int groupPos;
    private int childPos;

    //there's a case where we try to get the previous module and the previous module has a paginated list
    //of items.  A task will get those items and populate them in the background, but it throws off the
    //indexes because it adds the items to (possibly) the middle of the arrayList that backs the adapter.
    //The same case will happen if we don't have any information about the previous module.
    //This will keep track of where we need to be.
    private int currentPos = 0;
    private ArrayList<ModuleObject> modules = new ArrayList<>();
    private ArrayList<ArrayList<ModuleItem>> items = new ArrayList<>();

    //retrofit callbacks
    private StatusCallback<List<ModuleItem>> moduleItemsCallback;

    @Override
    @NonNull
    public FragmentInteractions.Placement getFragmentPlacement() {return FragmentInteractions.Placement.DETAIL; }

    @Override
    @NonNull
    public String title() {
        return getString(R.string.modules);
    }

    public String getTabId() {
        return Tab.MODULES_ID;
    }

    @Override
    protected String getSelectedParamName() {
        return Param.MODULE_ID;
    }

    @Override
    public boolean allowBookmarking() {
        return false;
    }

    @Override
    @NonNull
    public HashMap<String, String> getParamForBookmark() {
        ModuleItem item = getCurrentModuleItem(currentPos);
        Uri uri = Uri.parse(item.getUrl());

        if(uri != null) {
            List<String> params = uri.getPathSegments();
            //get the last 2 segments for the type and type_id
            if(params.size() > 2) {
                String itemType = params.get(params.size() - 2);
                String itemTypeId = params.get(params.size() - 1);
                HashMap<String, String> map = super.getParamForBookmark();

                map.put(Param.MODULE_TYPE_SLASH_ID, itemType + "/" + itemTypeId);
                map.put(Param.MODULE_ITEM_ID, Long.toString(item.getId()));

                return map;
            }
        }

        return super.getParamForBookmark();
    }

    @Override
    @NonNull
    public HashMap<String, String> getQueryParamForBookmark() {
        ModuleItem item = getCurrentModuleItem(currentPos);
        if(item != null) {
            HashMap<String, String> map = new HashMap<>();
            map.put(Param.MODULE_ITEM_ID, Long.toString(item.getId()));
            return map;
        }
        return super.getQueryParamForBookmark();
    }

    ///////////////////////////////////////////////////////////////////////////
    // LifeCycle
    ///////////////////////////////////////////////////////////////////////////


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(this, false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        rootView = (RelativeLayout) getLayoutInflater().inflate(R.layout.course_module_progression, container, false);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initViews(rootView);

        setupCallback();

        //setup view info/state
        setViewInfo(savedInstanceState);

        //set the onClick listeners for the buttons
        setButtonListeners();

    }

    @Override
    public void applyTheme() {

    }

    //this function is mostly for the internal web view fragments so we can go back in the webview
    @Override
    public boolean handleBackPressed() {
        if (viewPager != null && viewPager.getCurrentItem() != -1) {
            ParentFragment pFrag = (ParentFragment) adapter.instantiateItem(viewPager, viewPager.getCurrentItem());
            if (pFrag != null && pFrag.handleBackPressed()) {
                return true;
            }
        }
        return super.handleBackPressed();
    }

    @Override
    protected ModuleItem getModelObject() {
        return getCurrentModuleItem(currentPos);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //save the position that we're currently on
        outState.putInt(Const.MODULE_POSITION, currentPos);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Helper Functions
    ///////////////////////////////////////////////////////////////////////////

    private void initViews(View rootView) {
        viewPager = rootView.findViewById(R.id.pager);
        markDoneWrapper = rootView.findViewById(R.id.markDoneWrapper);
        markDoneButton =  rootView.findViewById(R.id.markDoneButton);
        markDoneCheckBox = rootView.findViewById(R.id.markDoneCheckbox);

        //module name that will appear between the prev and next buttons
        moduleName = rootView.findViewById(R.id.moduleName);
        moduleNameIcon = rootView.findViewById(R.id.moduleNameIcon);

        // Watch for button clicks.
        prev = rootView.findViewById(R.id.prev_item);

        prev.setBackground(ColorKeeper.getColoredDrawable(getActivity(), R.drawable.left_arrow, getCanvasContext()));

        next = rootView.findViewById(R.id.next_item);
        next.setBackground(ColorKeeper.getColoredDrawable(getActivity(), R.drawable.right_arrow, getCanvasContext()));
    }

    private void setModuleName(String name) {
        //set the label at the bottom
        moduleName.setText(name);
    }

    private void setViewInfo(Bundle bundle) {
        if(bundle != null && bundle.containsKey(Const.MODULE_POSITION)) {
            currentPos = bundle.getInt(Const.MODULE_POSITION);
        }
        else {
            //figure out which position in the overall adapter based on group and child position
            currentPos = getCurrentModuleItemPos(groupPos, childPos);
        }
        //setup adapter
        adapter = new CourseModuleProgressionAdapter(getChildFragmentManager());

        if(viewPager != null) {
            viewPager.setAdapter(adapter);
            //set the item number in the adapter to be the overall position

            viewPager.setCurrentItem(currentPos);
        }
        //if we're on the first item, make the prev button disappear
        if(currentPos == 0) {
            prev.setVisibility(View.INVISIBLE);
        }
        //if we're on the last item, make the next button invisible
        if(currentPos >= NUM_ITEMS - 1) {
            next.setVisibility(View.INVISIBLE);
        }


        //check the previous and next modules when we first get into the module progression
        //check if there are previous module items that we can view
        setupPreviousModule(getModuleItemGroup(currentPos));
        //check if there are any next module items that we can view
        setupNextModule(getModuleItemGroup(currentPos));

        //set the label at the bottom
        try {
            setModuleName(modules.get(groupPos).getName());
        } catch (IndexOutOfBoundsException e) {
            setModuleName("");
        }
        //add the locked icon if needed
        addLockedIconIfNeeded(modules, items, groupPos, childPos);

        updateModuleMarkDoneView(getCurrentModuleItem(currentPos));
    }

    //the bottom navigation bar has prev and next arrows that sometimes show up and sometimes don't depending
    //on where the user is (on the first module item there isn't a prev button).  We also may need to update these
    //dynamically when we use an async task to get the next group of module items
    private void updateBottomNavBarButtons() {
        //make them visible by default
        prev.setVisibility(View.VISIBLE);
        next.setVisibility(View.VISIBLE);

        //don't want to see the previous button if we're on the first item
        if(currentPos == 0) {
            prev.setVisibility(View.INVISIBLE);
        }
        //don't show the next button if we're on the last item
        if(currentPos + 1 >= NUM_ITEMS) {
            next.setVisibility(View.INVISIBLE);
        }


        //get the fragment and update the title
        Fragment fragment = adapter.getItem(currentPos);

        ModuleItem.CompletionRequirement completionRequirement = getCurrentModuleItem(currentPos).getCompletionRequirement();
        if (completionRequirement != null && ModuleItem.MUST_VIEW.equals(completionRequirement.getType())) {
            //reload the module object to update the items. By viewing this item subsequent items may now be unlocked
            adapter.notifyDataSetChanged();
            addLockedIconIfNeeded(modules, items, groupPos, childPos);

        }

        ModuleItem moduleItem = getCurrentModuleItem(currentPos);

        updateModuleMarkDoneView(moduleItem);
    }

    private void updateModuleMarkDoneView(ModuleItem item) {
        //sets up if the "mark done" view should be visible
        if(item == null) {
            markDoneWrapper.setVisibility(View.GONE);
        } else {
            ModuleItem.CompletionRequirement completionRequirement = item.getCompletionRequirement();
            if (completionRequirement != null && ModuleItem.MUST_MARK_DONE.equals(completionRequirement.getType())) {
                markDoneWrapper.setVisibility(View.VISIBLE);
                markDoneCheckBox.setChecked(completionRequirement.isCompleted());
            } else {
                markDoneWrapper.setVisibility(View.GONE);
            }
        }
    }

    private void setButtonListeners() {
        prev.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                setupPrevModuleName(currentPos);
                setupPreviousModule(getModuleItemGroup(currentPos));
                if (currentPos >= 1) {
                    viewPager.setCurrentItem(--currentPos);
                }
                updateBottomNavBarButtons();
            }
        });

        next.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                setupNextModuleName(currentPos);
                setupNextModule(getModuleItemGroup(currentPos));
                if (currentPos < (NUM_ITEMS - 1)) {
                    viewPager.setCurrentItem(++currentPos);
                }

                updateBottomNavBarButtons();
            }
        });

        markDoneButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getModelObject() != null && getModelObject().getCompletionRequirement() != null) {
                    if (getModelObject().getCompletionRequirement().isCompleted()) {
                        ModuleManager.markAsNotDone(getCanvasContext(), getModelObject().getModuleId(), getModelObject().getId(),
                                new StatusCallback<ResponseBody>() {
                                    @Override
                                    public void onResponse(@NonNull Response<ResponseBody> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                                        markDoneCheckBox.setChecked(false);
                                        getModelObject().getCompletionRequirement().setCompleted(false);
                                        notifyOfItemChanged(getModelObject());
                                    }
                                });
                    } else {
                        ModuleManager.markAsDone(getCanvasContext(), getModelObject().getModuleId(), getModelObject().getId(),
                                new StatusCallback<ResponseBody>() {
                                    @Override
                                    public void onResponse(@NonNull Response<ResponseBody> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                                        markDoneCheckBox.setChecked(true);
                                        getModelObject().getCompletionRequirement().setCompleted(true);
                                        notifyOfItemChanged(getModelObject());
                                    }
                                });
                    }
                }
            }
        });
    }

    private void notifyOfItemChanged(ModuleItem item) {
        Navigation navigation = getNavigation();
        if(navigation != null) {
            Fragment fragment = navigation.getPeekingFragment();
            if(fragment instanceof ModuleListFragment) {
                ((ModuleListFragment)fragment).notifyOfItemChanged(modules.get(groupPos), item);
            }
        }
    }

    private void getModuleItemData(long moduleId) {
        ModuleManager.getAllModuleItems(course, moduleId, moduleItemsCallback, true);
    }
    /**
     * Items could have a lot of modules with no data because we haven't retrieved it yet. So 
     * we need to use the group position to get it
     *
     * @param groupPosition
     * @return
     */
    private void setupNextModule(int groupPosition) {
        int nextUnlocked = groupPosition + 1;
        //check if the next module exists
        if(items.size() > nextUnlocked && items.get(nextUnlocked) != null && items.get(nextUnlocked).isEmpty() && moduleItemsCallback != null) {
            //get the module items for the next module
            getModuleItemData(modules.get(nextUnlocked).getId());
        }
    }

    /**
     * Items could have a lot of modules with no data because we haven't retrieved it yet. So 
     * we need to use the group position to get it
     *
     * @param groupPosition
     * @return
     */
    private void setupPreviousModule(int groupPosition) {
        int prevUnlocked = groupPosition - 1;
        //check if the prev module exists
        if(prevUnlocked >= 0 && items.get(prevUnlocked) != null && items.get(prevUnlocked).isEmpty() && moduleItemsCallback != null) {
            //get the module items for the previous module. The user could select the third module without expanding the second module, so we wouldn't
            //know what is in the second module.
            getModuleItemData(modules.get(prevUnlocked).getId());
        }
    }

    /**
     * Setup the module name while progressing through the modules using the next button
     *
     * @param position
     * @return
     */
    private void setupNextModuleName(int position) {
        int modulePos = 0;
        int i;
        for(i = 0; i < items.size(); i++) {
            if(position + 1< items.get(i).size() + modulePos) {
                //set the label at the bottom
                setModuleName(modules.get(i).getName());

                break;
            }
            modulePos += items.get(i).size();
        }
        //+1 because we're going to the next module item
        addLockedIconIfNeeded(modules, items, i, position-modulePos+1);
    }

    /**
     * Setup the module name while progressing through the modules using the previous button
     *
     * @param position
     * @return
     */
    private void setupPrevModuleName(int position) {
        int modulePos = 0;
        int i = 0;
        for(i = 0; i < items.size(); i++) {
            if(position - 1 < items.get(i).size() + modulePos) {
                //set the label at the bottom
                setModuleName(modules.get(i).getName());

                break;
            }
            modulePos += items.get(i).size();
        }
        //-1 from position because we're going to the previous module item
        addLockedIconIfNeeded(modules, items, i, position-modulePos-1);
    }

    /**
     * Iterate through the items to find the module item at the given position.  The module items are
     * an arrayList of arrayLists, so there isn't a built in way to get the moduleItem that we need.
     *
     * We sometimes want the overall position of the module item, but the data structure that we have 
     * is an arrayList of arrayLists, so we don't know which module item is the 9th module item without
     * going through the arrayLists and counting.
     * @param position
     * @return
     */
    private ModuleItem getCurrentModuleItem(int position) {
        ModuleItem moduleItem = null;
        int modulePos = 0;
        for(int i = 0; i < items.size(); i++) {
            if(position < items.get(i).size() + modulePos) {
                moduleItem = items.get(i).get(position - modulePos);
                if(moduleItem == null) {
                    continue;
                }

                break;
            }
            modulePos += items.get(i).size();
        }
        return moduleItem;
    }

    /**
     * Get the group position based on the overall position. When getting the next or previous group,
     * sometimes we just have the overall position in the adapter but we need the group number. This
     * helper function does that
     *
     * @param overallPos
     * @return
     */
    private int getModuleItemGroup(int overallPos) {
        int modulePos = 0;
        for(int i = 0; i < items.size(); i++) {
            if(overallPos < items.get(i).size() + modulePos) {
                //overallPos is the contained in the current group (i)
                return i;
            }
            modulePos += items.get(i).size();
        }
        return 0;
    }

    /**
     * Iterate through the items to find the overall position at the given group and child position.  
     * We are given the group and child info, but we need to know which position that is in the 
     * items arrayList that is used by the adapter.
     *
     * @param groupPosition
     * @return
     */
    private int getCurrentModuleItemPos(int groupPosition, int childPosition) {
        int modulePos = 0;
        for(int i = 0; i < groupPosition; i++) {
            modulePos += items.get(i).size();
        }
        return modulePos + childPosition;
    }


    //we don't want to add subheaders or external tools into the list. subheaders don't do anything and we
    //don't support external tools.
    public static boolean shouldAddModuleItem(Context context, ModuleItem moduleItem) {
        if(moduleItem.getType().equals("unlock_requirements")) {
            return false;
        }

        if(moduleItem.getType().equals("SubHeader")) {
            return false;
        }

        if(moduleItem.getTitle().equalsIgnoreCase(context.getString(R.string.loading))) {
            return false;
        }

        return true;
    }

    /**
     * We add a locked icon on the bottom bar if the item is locked.  The item could be locked because of
     * a module that has sequential progression or because the module object is locked.
     *
     * @param mObjects
     * @param mItems
     * @param groupPosition
     * @param childPosition
     * @return true if icon is added, false otherwise
     */
    private boolean addLockedIconIfNeeded(ArrayList<ModuleObject> mObjects, ArrayList<ArrayList<ModuleItem>> mItems, int groupPosition, int childPosition) {
        if(mObjects == null || mObjects.size() <= groupPosition || mObjects.get(groupPosition) == null) {
            setLockedIcon();
            return true;
        }
        //if the group is locked, add locked icon
        if(ModuleUtility.isGroupLocked(mObjects.get(groupPosition))) {
            setLockedIcon();
            return true;
        }

        //check if it's sequential progress
        //if the module has a sequential progress and the module itself is either unlocked or started, we 
        //need to see which items we add a locked icon to

        //make sure the module is sequential progress
        if(mObjects.get(groupPosition).isSequential_progress()
                //the state isn't always set, so we need a null check
                && (mObjects.get(groupPosition).getState() != null
                //modules can be locked by date or prerequisites. If we're unlocked then we will get just the first item, if the module is
                //"started" then the user has done 0 or more items, we need to find out which ones are available to display.
                && (mObjects.get(groupPosition).getState().equals("unlocked") || mObjects.get(groupPosition).getState().equals("started")))) {

            //group is sequential, need to figure out which ones to display and not display. We don't want to display any locked items
            int index = 0;
            for(int i = 0; i < mItems.get(groupPosition).size(); i++) {
                if(mItems.get(groupPosition).get(i).getCompletionRequirement() != null && !mItems.get(groupPosition).get(i).getCompletionRequirement().isCompleted()) {
                    //i is the first non completed item, so we don't include this item to show in the view pager.
                    index = i;
                    break;
                }
            }
            //index is now the index of the first non completed item. We show the first non completed item, but not the next one, which is why this is a 
            //greater than check instead of >=
            if(childPosition > index) {
                setLockedIcon();
                return true;
            }
        }

        moduleNameIcon.setVisibility(View.GONE);
        return false;
    }

    private void setLockedIcon() {
        moduleNameIcon.setVisibility(View.VISIBLE);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Adapter
    ///////////////////////////////////////////////////////////////////////////////////////////
    public class CourseModuleProgressionAdapter extends FragmentStatePagerAdapter {

        private boolean expectingUpdate;

        @Override
        public void finishUpdate(ViewGroup container) {
            super.finishUpdate(container);
            if (!expectingUpdate) return;
            expectingUpdate = false;
            List<Fragment> fragments = getChildFragmentManager().getFragments();
            for (Fragment fragment : fragments) {
                if (fragment instanceof FragmentInteractions) {
                    ((FragmentInteractions) fragment).applyTheme();
                }
            }
        }

        public CourseModuleProgressionAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        @Override
        public Fragment getItem(int position) {
            expectingUpdate = true;

            //position is the overall position, and we could have multiple modules with their individual positions (if 2 modules have 3 items each, the last
            //item in the second module is position 5, not 2 (zero based)),
            //so we need to find the correct one overall
            ModuleItem moduleItem = getCurrentModuleItem(position);

            // Add module item ID to bundle for PageView tracking
            Fragment fragment = ModuleUtility.getFragment(moduleItem, course, modules.get(groupPos));
            Bundle args = fragment.getArguments();
            if (args == null) {
                args = new Bundle();
                fragment.setArguments(args);
            }
            args.putLong(com.instructure.pandautils.utils.Const.MODULE_ITEM_ID, moduleItem.getId());

            return fragment;
            //don't update the actionbar title here, we'll do it later. When we update it here the actionbar title sometimes
            //gets updated to the next fragment's title
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            // For PageView tracking
            if (object != null) ((Fragment) object).setUserVisibleHint(true);
        }

        @Override
        public void destroyItem (ViewGroup container, int position, Object object) {
            //need to remove all the child fragments so they don't
            //hang around and get attached to other activities
            ParentFragment fragment = (ParentFragment) object;
            fragment.removeChildFragments();
            super.destroyItem(container, position, object);
        }

    }

    ///////////////////////////////////////////////////////////////////////////
    // Callbacks
    ///////////////////////////////////////////////////////////////////////////

    private void setupCallback() {
        moduleItemsCallback = new StatusCallback<List<ModuleItem>>() {

            @Override
            public void onResponse(@NonNull Response<List<ModuleItem>> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                List<ModuleItem> moduleItems = response.body();
                if(!apiCheck() || moduleItems == null || moduleItems.size() == 0) {
                    return;
                }

                // Update ui here with results
                ArrayList<ModuleItem> result = new ArrayList<>();

                result.addAll(moduleItems);

                // Holds the position of the module the current module item belongs to
                int index = 0;

                for(int i = 0; i < modules.size(); i++) {
                    // Loop through the modules for the course

                    if(modules.get(i).getId() == result.get(0).getModuleId()) {
                        // We've found the module these items belong to, keep track of it's index
                        index = i;
                        break;
                    }
                }

                int itemsAdded = 0;
                //add the results in the correct place.  We need to get the index especially for pagination so we
                //add the items where they're supposed to be
                for(int i = 0; i < result.size(); i++) {
                    //check if we should add the moduleItem. Also, we don't want to add it if the view pager
                    //already contains it. This could happen if they complete an item and pull to refresh
                    //or if the teacher adds an item
                    if(shouldAddModuleItem(getContext(), result.get(i)) &&
                            !items.get(index).contains(result.get(i))) {
                        items.get(index).add(result.get(i));
                        itemsAdded++;
                    }
                }
                NUM_ITEMS += itemsAdded;

                //only add to currentPos if we're adding to the module that is the previous module
                //Without this check it will modify the index of the array while we are progressing through
                //the a module which will cause it to jump around a lot because the index is changing.
                if(index < getModuleItemGroup(currentPos + itemsAdded)) {
                    currentPos += itemsAdded;
                }

                adapter.notifyDataSetChanged();

                // When we tap on a module item it will try to load the previous and next modules, this can throw off the module item that was already loaded,
                // so load it to the current position
                viewPager.setCurrentItem(currentPos);

                //prev/next buttons may now need to be visible (if we were on a module item that was the last in its group but
                //now we have info about the next module, we want the user to be able to navigate there)
                updateBottomNavBarButtons();
            };
        };
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Intents
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void handleIntentExtras(Bundle extras) {
        super.handleIntentExtras(extras);
        if(extras == null) {return;}

        //get the extras from the intent   
        modules = extras.getParcelableArrayList(Const.MODULE_OBJECTS);
        items = (ArrayList<ArrayList<ModuleItem>>) extras.getSerializable(Const.MODULE_ITEM);
        course = extras.getParcelable(Const.COURSE);

        if(items == null) {
            items = new ArrayList<>();
        }

        if(modules == null) {
            modules = new ArrayList<>();
        }

        // Figure out the total size so the adapter knows how many items it will have
        int size = 0;
        for(int i = 0; i < items.size(); i++) {
            size += items.get(i).size();
        }
        NUM_ITEMS = size;

        groupPos = extras.getInt(Const.GROUP_POSITION, 0);
        childPos = extras.getInt(Const.CHILD_POSITION, 0);

    }

    public static Bundle createBundle(ArrayList<ModuleObject> moduleObjects, ArrayList<ArrayList<ModuleItem>> itemList, Course course, int groupPos, int childPos){
        Bundle bundle = createBundle(course);
        bundle.putParcelableArrayList(Const.MODULE_OBJECTS, moduleObjects);
        bundle.putSerializable(Const.MODULE_ITEM, itemList);
        bundle.putParcelable(Const.COURSE, course);

        bundle.putInt(Const.GROUP_POSITION, groupPos);
        bundle.putInt(Const.CHILD_POSITION, childPos);
        return bundle;
    }

    public static Bundle createBundle(ArrayList<ModuleObject> moduleObjects, ArrayList<ArrayList<ModuleItem>> itemList, Course course, long itemId, long moduleItemId){
        Bundle bundle = createBundle(course);
        bundle.putParcelableArrayList(Const.MODULE_OBJECTS, moduleObjects);
        bundle.putSerializable(Const.MODULE_ITEM, itemList);
        bundle.putParcelable(Const.COURSE, course);
        bundle.putLong(Const.ITEM_ID, itemId);
        bundle.putLong(Const.MODULE_ID, moduleItemId);
        return bundle;
    }
}
