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

package com.instructure.candroid.adapter;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;

import com.instructure.candroid.R;
import com.instructure.candroid.binders.EmptyBinder;
import com.instructure.candroid.binders.ModuleBinder;
import com.instructure.candroid.binders.ModuleHeaderBinder;
import com.instructure.candroid.holders.ModuleEmptyViewHolder;
import com.instructure.candroid.holders.ModuleHeaderViewHolder;
import com.instructure.candroid.holders.ModuleSubHeaderViewHolder;
import com.instructure.candroid.holders.ModuleViewHolder;
import com.instructure.candroid.interfaces.ModuleAdapterToFragmentCallback;
import com.instructure.candroid.util.ModuleUtility;
import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.managers.ModuleManager;
import com.instructure.canvasapi2.models.AssignmentSet;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.Course;
import com.instructure.canvasapi2.models.ModuleItem;
import com.instructure.canvasapi2.models.ModuleObject;
import com.instructure.canvasapi2.utils.APIHelper;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.DateHelper;
import com.instructure.canvasapi2.utils.LinkHeaders;
import com.instructure.pandarecycler.interfaces.ViewHolderHeaderClicked;
import com.instructure.pandarecycler.util.GroupSortedList;
import com.instructure.pandarecycler.util.Types;
import com.instructure.pandautils.utils.ColorKeeper;
import com.instructure.pandautils.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Response;

public class ModuleListRecyclerAdapter extends ExpandableRecyclerAdapter<ModuleObject, ModuleItem, RecyclerView.ViewHolder> {

    private Course mCourse;
    private CanvasContext mCanvasContext;
    private HashMap<Long, ModuleItemCallback> mModuleItemCallbacks = new HashMap<>();
    private ModuleAdapterToFragmentCallback mCallback;
    private StatusCallback<List<ModuleObject>> mModuleObjectCallback;
    private long mDefaultExpandedModuleId;

    /* For testing purposes only */
    protected ModuleListRecyclerAdapter(Context context){
        super(context, ModuleObject.class, ModuleItem.class);
    }

    public ModuleListRecyclerAdapter(Course course, long defaultExpandedModuleId, Context context1, CanvasContext canvasContext, ModuleAdapterToFragmentCallback adapterToFragmentCallback) {
        super(context1, ModuleObject.class, ModuleItem.class);
        mCourse = course;
        mCanvasContext = canvasContext;
        mCallback = adapterToFragmentCallback;
        mDefaultExpandedModuleId = defaultExpandedModuleId;
        setViewHolderHeaderClicked(new ViewHolderHeaderClicked<ModuleObject>() {
            @Override
            public void viewClicked(View view, ModuleObject moduleObject) {
                ModuleItemCallback moduleItemsCallback = getModuleItemsCallback(moduleObject, false);
                if (!moduleItemsCallback.isFromNetwork() && !isGroupExpanded(moduleObject)) {
                    ModuleManager.getFirstPageModuleItems(mCanvasContext, moduleObject.getId(), getModuleItemsCallback(moduleObject, false), true);
                } else {
                    expandCollapseGroup(moduleObject);
                }
            }
        });
        setExpandedByDefault(false);
        setDisplayEmptyCell(true);
        loadData();
    }

    @Override
    public RecyclerView.ViewHolder createViewHolder(View v, int viewType) {
        if (viewType == Types.TYPE_HEADER) {
            return new ModuleHeaderViewHolder(v);
        } else if (viewType == Types.TYPE_SUB_HEADER) {
            return new ModuleSubHeaderViewHolder(v);
        } else if (viewType == Types.TYPE_EMPTY_CELL) {
            return new ModuleEmptyViewHolder(v);
        } else {
            return new ModuleViewHolder(v);
        }
    }

    @Override
    public void onBindChildHolder(RecyclerView.ViewHolder holder, ModuleObject moduleObject, ModuleItem moduleItem) {
        if(holder instanceof ModuleSubHeaderViewHolder) {
            final int groupItemCount = getGroupItemCount(moduleObject);
            final int itemPosition = storedIndexOfItem(moduleObject, moduleItem);

            ModuleBinder.bindSubHeader((ModuleSubHeaderViewHolder)holder, moduleObject, moduleItem,
                    itemPosition == 0, itemPosition == groupItemCount - 1);
        } else {
            final int courseColor = ColorKeeper.getOrGenerateColor(mCanvasContext);
            final int groupItemCount = getGroupItemCount(moduleObject);
            final int itemPosition = storedIndexOfItem(moduleObject, moduleItem);

            ModuleBinder.bind((ModuleViewHolder) holder, moduleObject, moduleItem, getContext(), mCallback, isSequentiallyEnabled(moduleObject, moduleItem), courseColor,
                    itemPosition == 0, itemPosition == groupItemCount - 1);
        }
    }

    @Override
    public void onBindHeaderHolder(RecyclerView.ViewHolder holder, ModuleObject moduleObject, boolean isExpanded) {
        ModuleHeaderBinder.bind((ModuleHeaderViewHolder) holder, moduleObject, getContext(), mCanvasContext, getGroupItemCount(moduleObject), getViewHolderHeaderClicked(), isExpanded);
    }

    @Override
    public void onBindEmptyHolder(RecyclerView.ViewHolder holder, ModuleObject moduleObject) {
        ModuleEmptyViewHolder moduleEmptyViewHolder = (ModuleEmptyViewHolder) holder;
        // Keep displaying No connection as long as the result is not from network
        // Doing so will cause the user to toggle the expand to refresh the list, if they had expanded a module while offline
        if (mModuleItemCallbacks.containsKey(moduleObject.getId()) && mModuleItemCallbacks.get(moduleObject.getId()).isFromNetwork()) {
            EmptyBinder.bind(moduleEmptyViewHolder, getPrerequisiteString(moduleObject));
        } else {
            EmptyBinder.bind(moduleEmptyViewHolder, getContext().getString(R.string.noConnection));
        }
    }

    @Override
    public int itemLayoutResId(int viewType) {
        if (viewType == Types.TYPE_HEADER) {
            return R.layout.viewholder_header_module;
        } else if (viewType == Types.TYPE_SUB_HEADER) {
            return R.layout.viewholder_sub_header_module;
        } else if (viewType == Types.TYPE_EMPTY_CELL) {
            return ModuleEmptyViewHolder.holderResId();
        } else {
            return R.layout.viewholder_module;
        }
    }

    @Override
    public void refresh() {
        mModuleItemCallbacks.clear();
        collapseAll();
        super.refresh();
    }

    @Override
    public void contextReady() {

    }

    // region Expandable Callbacks
    @Override
    public GroupSortedList.GroupComparatorCallback<ModuleObject> createGroupCallback() {
        return new GroupSortedList.GroupComparatorCallback<ModuleObject>() {
            @Override
            public int compare(ModuleObject o1, ModuleObject o2) {
                return o1.getPosition() - o2.getPosition();
            }

            @Override
            public boolean areContentsTheSame(ModuleObject oldGroup, ModuleObject newGroup) {
                boolean isNewLocked = ModuleUtility.isGroupLocked(newGroup);
                boolean isOldLocked = ModuleUtility.isGroupLocked(oldGroup);
                return oldGroup.getName().equals(newGroup.getName()) && isNewLocked == isOldLocked;
            }

            @Override
            public boolean areItemsTheSame(ModuleObject group1, ModuleObject group2) {
                return group1.getId() == group2.getId();
            }

            @Override
            public int getGroupType(ModuleObject group) {
                return Types.TYPE_HEADER;
            }

            @Override
            public long getUniqueGroupId(ModuleObject group) {
                return group.getId();
            }
        };
    }

    @Override
    public GroupSortedList.ItemComparatorCallback<ModuleObject, ModuleItem> createItemCallback() {
        return new GroupSortedList.ItemComparatorCallback<ModuleObject, ModuleItem>() {
            @Override
            public int compare(ModuleObject group, ModuleItem o1, ModuleItem o2) {
                return o1.getPosition() - o2.getPosition();
            }

            @Override
            public boolean areContentsTheSame(ModuleItem oldItem, ModuleItem newItem) {
                return oldItem.getTitle().equals(newItem.getTitle());
            }

            @Override
            public boolean areItemsTheSame(ModuleItem item1, ModuleItem item2) {
                return item1.getId() == item2.getId();
            }

            @Override
            public int getChildType(ModuleObject group, ModuleItem item) {
                if(item.getType().equals(ModuleItem.TYPE.SubHeader.toString())) {
                    return Types.TYPE_SUB_HEADER;
                }
                return Types.TYPE_ITEM;
            }

            @Override
            public long getUniqueItemId(ModuleItem item) {
                return item.getId();
            }
        };
    }
    // endregion


    public ProgressDialog createProgressDialog(Context context) {
        ProgressDialog dialog = new ProgressDialog(context);
        try {
            dialog.show();
        } catch (WindowManager.BadTokenException e) {}

        dialog.setCancelable(false);

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setContentView(R.layout.progress_dialog);
        final int currentColor = ColorKeeper.getOrGenerateColor(mCanvasContext);

        ((ProgressBar)dialog.findViewById(R.id.progressBar)).getIndeterminateDrawable().setColorFilter(currentColor, PorterDuff.Mode.SRC_ATOP);
        return dialog;
    }

    public void updateMasteryPathItems() {

        final ProgressDialog dialog = createProgressDialog(getContext());
        dialog.show();
        //show for 3 seconds and then refresh the list
        //This 3 seconds is to allow the Canvas database to update so we can pull the module info down
        new CountDownTimer(3000,1000){

            @Override
            public void onTick(long millisUntilFinished) {}

            @Override
            public void onFinish() {
                dialog.cancel();
                refresh();
            }
        }.start();


    }

    private ModuleItemCallback getModuleItemsCallback(final ModuleObject moduleObject, final boolean isNotifyGroupChange) {
        if (mModuleItemCallbacks.containsKey(moduleObject.getId())) {
            return mModuleItemCallbacks.get(moduleObject.getId());
        } else {
            ModuleItemCallback moduleItemCallback = new ModuleItemCallback(moduleObject) {

                private int checkMasteryPaths(int position, ModuleItem item) {
                    if(item.getMasteryPaths() != null && item.getMasteryPaths().isLocked()) {
                        //add another module item that says it's locked
                        ModuleItem masteryPathsLocked = new ModuleItem();
                        //set an id so that if there is more than one path we'll display all of them. otherwise addOrUpdateItem will overwrite it
                        masteryPathsLocked.setId(UUID.randomUUID().getLeastSignificantBits());
                        masteryPathsLocked.setTitle(String.format(Locale.getDefault(), getContext().getString(R.string.locked_mastery_paths), item.getTitle()));
                        masteryPathsLocked.setType(ModuleItem.TYPE.Locked.toString());
                        masteryPathsLocked.setCompletionRequirement(null);
                        masteryPathsLocked.setPosition(position++);
                        addOrUpdateItem(this.getModuleObject(), masteryPathsLocked);
                    } else if (item.getMasteryPaths() != null && !item.getMasteryPaths().isLocked() && item.getMasteryPaths().getSelectedSetId() == 0) {
                        //add another module item that says select to choose assignment group
                        //We only want to do this when we have a mastery paths object, it's unlocked, and the user hasn't already selected a set
                        ModuleItem masteryPathsSelect = new ModuleItem();
                        //set an id so that if there is more than one path we'll display all of them. otherwise addOrUpdateItem will overwrite it
                        masteryPathsSelect.setId(UUID.randomUUID().getLeastSignificantBits());
                        masteryPathsSelect.setTitle(getContext().getString(R.string.choose_assignment_group));
                        masteryPathsSelect.setType(ModuleItem.TYPE.ChooseAssignmentGroup.toString());
                        masteryPathsSelect.setCompletionRequirement(null);
                        masteryPathsSelect.setPosition(position++);
                        //sort the mastery paths by position
                        ArrayList<AssignmentSet> assignmentSets = new ArrayList<>();
                        assignmentSets.addAll(Arrays.asList(item.getMasteryPaths().getAssignmentSets()));
                        Collections.sort(assignmentSets, new Comparator<AssignmentSet>() {
                            @Override
                            public int compare(AssignmentSet lh, AssignmentSet rh) {
                                if(lh != null && rh != null) {
                                    if(lh.getPosition() < rh.getPosition()) {
                                        return -1;
                                    } else if(lh.getPosition() > rh.getPosition()) {
                                        return 1;
                                    }
                                }
                                return 0;
                            }
                        });
                        AssignmentSet[] set = new AssignmentSet[assignmentSets.size()];
                        assignmentSets.toArray(set);
                        item.getMasteryPaths().setAssignmentSets(set);
                        masteryPathsSelect.setMasteryPathsItemId(item.getId());
                        masteryPathsSelect.setMasteryPaths(item.getMasteryPaths());
                        addOrUpdateItem(this.getModuleObject(), masteryPathsSelect);
                    }
                    return position;
                }

                @Override
                public void onResponse(@NonNull Response<List<ModuleItem>> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                    List<ModuleItem> moduleItems = response.body();
                    if(type == ApiType.API) {
                       int position = (moduleItems.size() > 0 && moduleItems.get(0) != null) ? moduleItems.get(0).getPosition() - 1 : 0;
                       for (ModuleItem item : moduleItems) {
                           item.setPosition(position++);
                           addOrUpdateItem(this.getModuleObject(), item);
                           position = checkMasteryPaths(position, item);
                       }

                       String nextItemsURL = linkHeaders.nextUrl;
                       if (nextItemsURL != null) {
                           ModuleManager.getNextPageModuleItems(nextItemsURL, this, true);
                       }

                       this.setIsFromNetwork(true);
                       expandGroup(this.getModuleObject(), isNotifyGroupChange);
                   } else if (type == ApiType.CACHE) {
                       int position = (moduleItems.size() > 0 && moduleItems.get(0) != null) ? moduleItems.get(0).getPosition() - 1 : 0;
                       for(ModuleItem item : moduleItems) {
                           item.setPosition(position++);
                           addOrUpdateItem(this.getModuleObject(), item);
                       }

                       String nextItemsURL = linkHeaders.nextUrl;
                       if(nextItemsURL != null){
                           ModuleManager.getNextPageModuleItems(nextItemsURL, this, true);
                       }

                       // Wait for the network to expand when there are no items
                       if (moduleItems.size() > 0) {
                           expandGroup(this.getModuleObject(), isNotifyGroupChange);
                       }
                   }
                }

                @Override
                public void onFail(@Nullable Call<List<ModuleItem>> call, @NonNull Throwable error, @Nullable Response response) {

                    // Only expand if there was no cache result and no network. No connection empty cell will be displayed
                    if (response != null
                            && response.code() == 504
                            && APIHelper.isCachedResponse(response)
                            && getContext() != null
                            && !Utils.isNetworkAvailable(getContext())) {
                        expandGroup(this.getModuleObject(), isNotifyGroupChange);
                    }
                }
            };

            mModuleItemCallbacks.put(moduleObject.getId(), moduleItemCallback);
            return moduleItemCallback;
        }
    }

    // region Pagination
    @Override
    public boolean isPaginated() {
        return true;
    }

    @Override
    public void setupCallbacks() {
        mModuleObjectCallback = new StatusCallback<List<ModuleObject>>() {

            @Override
            public void onResponse(@NonNull Response<List<ModuleObject>> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                List<ModuleObject> moduleObjects = response.body();
                setNextUrl(linkHeaders.nextUrl);

                addOrUpdateAllGroups(moduleObjects.toArray(new ModuleObject[moduleObjects.size()]));

                if (mDefaultExpandedModuleId == -1 && moduleObjects.size() > 0) {
                    mDefaultExpandedModuleId = moduleObjects.get(0).getId();
                }

                if (mDefaultExpandedModuleId != -1) {
                    ModuleObject defaultExpandedModuleObject = getGroup(mDefaultExpandedModuleId);
                    if (defaultExpandedModuleObject != null) {
                        // In order for the arrow to be the correct direction when expanded, set isNotifyGroupChange to true
                        // It may be a race condition, but if we don't put the ! before the call in progress check then it will show an empty cell that says no network connection
                        if (!getModuleItemsCallback(defaultExpandedModuleObject, true).isCallInProgress()) {
                            ModuleManager.getFirstPageModuleItems(mCanvasContext, defaultExpandedModuleObject.getId(),getModuleItemsCallback(defaultExpandedModuleObject, true), true );
                        } else {
                            expandGroup(defaultExpandedModuleObject, true);
                        }
                    }
                }

                mCallback.onRefreshFinished();
            }

            @Override
            public void onFinished(ApiType type) {
                ModuleListRecyclerAdapter.this.onCallbackFinished(type);
            }
        };



    }

    @Override
    public void loadFirstPage() {
        ModuleManager.getFirstPageModuleObjects(mCourse, mModuleObjectCallback, true);
    }

    @Override
    public void loadNextPage(String nextURL) {
        ModuleManager.getNextPageModuleObjects(nextURL, mModuleObjectCallback, true);
    }

    // endregion

    // region Module binder Helpers
    private boolean isSequentiallyEnabled(ModuleObject moduleObject, ModuleItem moduleItem) {
        //if it's sequential progress and the group is unlocked, the first incomplete one can be viewed
        //if this moduleItem is locked, it should be greyed out unless it is the first one (position == 1 -> it is 1 based, not
        //0 based) or the previous item is unlocked
        if (mCanvasContext instanceof Course && (((Course) mCanvasContext).isTeacher() || ((Course) mCanvasContext).isTA())) {
            return true;
        }

        if (moduleObject.isSequential_progress() &&
            (moduleObject.getState() != null &&
            (moduleObject.getState().equals(ModuleObject.STATE.unlocked.toString()) || moduleObject.getState().equals(ModuleObject.STATE.started.toString())))) {

            //group is sequential, need to figure out which ones to grey out
            int indexOfCurrentModuleItem = storedIndexOfItem(moduleObject, moduleItem);
            if (indexOfCurrentModuleItem != -1) {
                // getItem performs invalid index checks
                ModuleItem previousModuleItem = getItem(moduleObject, indexOfCurrentModuleItem - 1);
                ModuleItem nextModuleItem = getItem(moduleObject, indexOfCurrentModuleItem + 1);

                if (isComplete(moduleItem)) {
                    return true;
                } else if (previousModuleItem == null) { // Its the first one in the sequence
                    return true;
                } else if (!isComplete(previousModuleItem)) { // previous item is not complete
                    return false;
                } else if (isComplete(previousModuleItem) && !isComplete(moduleItem)) { // previous complete, so show current as next in sequence
                    return true;
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isComplete(ModuleItem moduleItem) {
        return moduleItem != null && moduleItem.getCompletionRequirement() != null && moduleItem.getCompletionRequirement().isCompleted();
    }

    // never actually shows prereqs because grayed out module items show instead.
    private String getPrerequisiteString(ModuleObject moduleObject) {
        String prereqString = getContext().getString(R.string.noItemsToDisplayShort);

        if(ModuleUtility.isGroupLocked(moduleObject)){
            prereqString = getContext().getString(R.string.locked);
        }

        if(moduleObject.getState() != null &&
           moduleObject.getState().equals(ModuleObject.STATE.locked.toString()) &&
           getGroupItemCount(moduleObject) > 0 &&
           getItem(moduleObject, 0).getType().equals(ModuleObject.STATE.unlock_requirements.toString())) {

           StringBuilder reqs = new StringBuilder();
            long[] ids = moduleObject.getPrerequisite_ids();
            //check to see if they need to finish other modules first
            if(ids != null) {
                for(int i = 0; i < ids.length; i++) {
                    ModuleObject prereqModuleObject = getGroup(ids[i]);
                    if(prereqModuleObject != null) {
                        if(i == 0) { //if it's the first one, add the "Prerequisite:" label
                            reqs.append(getContext().getString(R.string.prerequisites) + " " + prereqModuleObject.getName());
                        } else {
                            reqs.append(", " + prereqModuleObject.getName());
                        }
                    }
                }
            }

            if(moduleObject.getUnlock_at() != null) {
                //only want a newline if there are prerequisite ids
                if(ids.length > 0 && ids[0] != 0) {
                    reqs.append("\n");
                }
                reqs.append(DateHelper.createPrefixedDateTimeString(getContext(), R.string.unlocked, moduleObject.getUnlock_at()));
            }

            prereqString = reqs.toString();
        }
        return prereqString;
    }
    // endregion

    private static abstract class ModuleItemCallback extends StatusCallback<List<ModuleItem>> {
        private ModuleObject moduleObject;
        private boolean isFromNetwork = false; // When true, there is no need to fetch objects from the network again.
        public ModuleItemCallback(ModuleObject moduleObject) {
            this.moduleObject = moduleObject;
        }

        public ModuleObject getModuleObject() {
            return moduleObject;
        }

        public boolean isFromNetwork() {
            return isFromNetwork;
        }

        public void setIsFromNetwork(boolean isFromNetwork) {
            this.isFromNetwork = isFromNetwork;
        }
    }
}
