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

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import com.instructure.candroid.R;
import com.instructure.candroid.binders.ExpandableHeaderBinder;
import com.instructure.candroid.binders.TodoBinder;
import com.instructure.candroid.holders.ExpandableViewHolder;
import com.instructure.candroid.holders.TodoViewHolder;
import com.instructure.candroid.interfaces.NotificationAdapterToFragmentCallback;
import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.managers.CalendarEventManager;
import com.instructure.canvasapi2.managers.CourseManager;
import com.instructure.canvasapi2.managers.GroupManager;
import com.instructure.canvasapi2.managers.ToDoManager;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.Course;
import com.instructure.canvasapi2.models.Group;
import com.instructure.canvasapi2.models.ScheduleItem;
import com.instructure.canvasapi2.models.ToDo;
import com.instructure.canvasapi2.utils.APIHelper;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.DateHelper;
import com.instructure.canvasapi2.utils.LinkHeaders;
import com.instructure.canvasapi2.utils.ModelExtensionsKt;
import com.instructure.pandarecycler.util.GroupSortedList;
import com.instructure.pandarecycler.util.Types;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

public class TodoListRecyclerAdapter extends ExpandableRecyclerAdapter<Date, ToDo, RecyclerView.ViewHolder> {

    private NotificationAdapterToFragmentCallback<ToDo> mAdapterToFragmentCallback;
    private TodoCheckboxCallback mTodoCheckboxCallback;

    private Map<Long, Course> mCourseMap;
    private Map<Long, Group> mGroupMap;
    private List<ToDo> mTodoList;

    private StatusCallback<List<ToDo>> mTodoCallback;
    private StatusCallback<List<Course>> mCoursesCallback;
    private StatusCallback<List<Group>> mGroupsCallback;
    private CanvasContext mCanvasContext;

    private HashSet<ToDo> mCheckedTodos = new HashSet<>();
    private HashSet<ToDo> mDeletedTodos = new HashSet<>();

    private boolean mIsEditMode;
    private boolean mIsNoNetwork; // With multiple callbacks, some could fail while others don't. This manages when to display no connection when offline

    // region Interfaces
    public interface TodoCheckboxCallback {
        void onCheckChanged(ToDo todo, boolean isChecked, int position);
        boolean isEditMode();
    }

    // endregion

    /* For testing purposes only */
    protected TodoListRecyclerAdapter(Context context){
        super(context, Date.class, ToDo.class);
    }

    public TodoListRecyclerAdapter(Context context, CanvasContext canvasContext, NotificationAdapterToFragmentCallback<ToDo> adapterToFragmentCallback) {
        super(context, Date.class, ToDo.class);
        mCanvasContext = canvasContext;
        mAdapterToFragmentCallback = adapterToFragmentCallback;
        mIsEditMode = false;
        setExpandedByDefault(true);
        loadData();
    }

    @Override
    public RecyclerView.ViewHolder createViewHolder(View v, int viewType) {
        if (viewType == Types.TYPE_HEADER) {
            return new ExpandableViewHolder(v);
        } else {
            return new TodoViewHolder(v);
        }
    }

    @Override
    public int itemLayoutResId(int viewType) {
        if (viewType == Types.TYPE_HEADER) {
            return ExpandableViewHolder.holderResId();
        } else {
            return TodoViewHolder.holderResId();
        }
    }

    @Override
    public void onBindChildHolder(RecyclerView.ViewHolder holder, Date date, ToDo todo) {
        TodoBinder.bind(getContext(), (TodoViewHolder) holder, todo, mAdapterToFragmentCallback, mTodoCheckboxCallback);
    }

    @Override
    public void onBindHeaderHolder(RecyclerView.ViewHolder holder, Date date, boolean isExpanded) {
        // If the to do doesn't have a due date (like if you're a teacher grading an assignment) the date that is set for the
        // header is a cleaned up version of a new date with the value of Long.MAX_VALUE
        Date defaultDate = DateHelper.getCleanDate(new Date(Long.MAX_VALUE).getTime());

        String displayDate;

        // If the date hasn't been set to something besides the default it means the to do doesn't have a due date
        if(date.equals(defaultDate)) {
            displayDate = getContext().getString(R.string.toDoNoDueDate);
        } else {
            displayDate = DateHelper.getFormattedDate(getContext(), date);
        }

        ExpandableHeaderBinder.bind(getContext(), mCanvasContext, (ExpandableViewHolder) holder, date, displayDate, isExpanded, getViewHolderHeaderClicked());
    }

    @Override
    public void loadData() {
        CourseManager.getCourses(true, mCoursesCallback);
        GroupManager.getAllGroups(mGroupsCallback, true);
        ToDoManager.getTodos(mCanvasContext, mTodoCallback, isRefresh());
}

    @Override
    public void onCallbackFinished(ApiType type) {
        // Workaround for the multiple callbacks, some will succeed while others don't
        setLoadedFirstPage(true);
        shouldShowLoadingFooter();
        AdapterToRecyclerViewCallback adapterToRecyclerViewCallback = getAdapterToRecyclerViewCallback();
        if(adapterToRecyclerViewCallback != null){
            if (!mIsNoNetwork) { // Double negative, only happens when there is network
                adapterToRecyclerViewCallback.setDisplayNoConnection(false);
                getAdapterToRecyclerViewCallback().setIsEmpty(isAllPagesLoaded() && size() == 0);
            }
        }
    }

    @Override
    public void onNoNetwork() {
        super.onNoNetwork();
        mIsNoNetwork = true;
    }

    @Override
    public void refresh() {
        mIsNoNetwork = false;
        getAdapterToRecyclerViewCallback().setDisplayNoConnection(false);
        super.refresh();
    }

    private void populateAdapter() {
        if(mTodoList == null || mCourseMap == null || mGroupMap == null) {
            return;
        }

        List<ToDo> todos = ToDoManager.mergeToDoUpcoming(mTodoList, null);

        // Now populate the todoList and upcomingList with the course information
        for(ToDo toDo : todos) {
            ToDo.setContextInfo(toDo, mCourseMap, mGroupMap);
            if(toDo.getCanvasContext() != null) addOrUpdateItem(DateHelper.getCleanDate(toDo.getComparisonDate().getTime()), toDo);
        }
        mAdapterToFragmentCallback.onRefreshFinished();
        setAllPagesLoaded(true);

        // Reset the lists
        mTodoList = null;
        mCourseMap = null;
        mGroupMap = null;

        onCallbackFinished(ApiType.API);
    }

    @Override
    public void setupCallbacks() {
        mTodoCheckboxCallback = new TodoCheckboxCallback() {
            @Override
            public void onCheckChanged(ToDo todo, boolean isChecked, int position) {
                // We don't want to let them try to delete things while offline because they
                // won't actually delete them from the server
                if(!APIHelper.hasNetworkConnection()) {
                    Toast.makeText(getContext(), getContext().getString(R.string.notAvailableOffline), Toast.LENGTH_SHORT).show();
                    return;
                }

                todo.setChecked(isChecked);
                if (isChecked && !mDeletedTodos.contains(todo)) {
                    mCheckedTodos.add(todo);
                } else {
                    mCheckedTodos.remove(todo);
                }

                // If we aren't in the edit mode, enable edit mode for future clicks

                if(!mIsEditMode){
                    mIsEditMode = true;
                } else if (mCheckedTodos.size() == 0){ // If this was the last item, cancel
                    mIsEditMode = false;
                }

                mAdapterToFragmentCallback.onShowEditView(mCheckedTodos.size() > 0);
                notifyItemChanged(position);
            }

            @Override
            public boolean isEditMode() {
                return mIsEditMode;
            }
        };

        mCoursesCallback = new StatusCallback<List<Course>>() {

            @Override
            public void onResponse(@NonNull Response<List<Course>> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                if(response.body() == null) return;
                ArrayList<Course> filteredCourses = new ArrayList<>();
                for (Course course: response.body()) {
                    if(course.isFavorite() && !course.isAccessRestrictedByDate() && !ModelExtensionsKt.isInvited(course)) {
                        filteredCourses.add(course);
                    }
                }
                mCourseMap = CourseManager.createCourseMap(filteredCourses);
                populateAdapter();
            }
        };

        mGroupsCallback = new StatusCallback<List<Group>>() {

            @Override
            public void onResponse(@NonNull Response<List<Group>> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                mGroupMap = GroupManager.createGroupMap(response.body());
                populateAdapter();
            }
        };

        mTodoCallback = new StatusCallback<List<ToDo>>() {
            @Override
            public void onResponse(@NonNull Response<List<ToDo>> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                mTodoList = response.body();
                populateAdapter();

                // Remove the todos that have been deleted
                for (ToDo toDo : mDeletedTodos) {
                    removeItem(toDo);
                }
            }
        };

    }

    // endregion

    // region Data

    public void confirmButtonClicked() {
        for (ToDo todo : mCheckedTodos) {
            hideTodoItem(todo);
            mDeletedTodos.add(todo);
        }
        mIsEditMode = false;
        clearMarked();
    }

    public void cancelButtonClicked() {
        for (ToDo todo : mCheckedTodos) {
            todo.setChecked(false);
        }
        mIsEditMode = false;
        clearMarked();
        notifyDataSetChanged();
    }

    public void clearMarked() {
        mCheckedTodos.clear();
        mAdapterToFragmentCallback.onShowEditView(mCheckedTodos.size() > 0);
    }

    private void hideTodoItem(final ToDo todo) {
        if (todo.getIgnore() == null) {
            // No Url to hit to remove this to-do so it's probably dead and gone by now
            removeItem(todo);
        } else {
            ToDoManager.dismissTodo(todo, new StatusCallback<Void>() {
                @Override
                public void onResponse(@NonNull Response<Void> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                    removeItem(todo);
                    getAdapterToRecyclerViewCallback().setIsEmpty(size() == 0);
                }

                @Override
                public void onFail(@Nullable Call<Void> call, @NonNull Throwable error, @Nullable Response response) {
                    mDeletedTodos.remove(todo);
                }
            });
        }
    }

    // endregion

    // region Expandable Callbacks

    @Override
    public GroupSortedList.GroupComparatorCallback<Date> createGroupCallback() {
        return new GroupSortedList.GroupComparatorCallback<Date>() {
            @Override
            public int compare(Date o1, Date o2) {
                return o1.compareTo(o2);
            }

            @Override
            public boolean areContentsTheSame(Date oldGroup, Date newGroup) {
                return oldGroup.equals(newGroup);
            }

            @Override
            public boolean areItemsTheSame(Date group1, Date group2) {
                return group1.getTime() == group2.getTime();
            }

            @Override
            public long getUniqueGroupId(Date group) {
                return group.getTime();
            }

            @Override
            public int getGroupType(Date group) {
                return Types.TYPE_HEADER;
            }
        };
    }

    @Override
    public GroupSortedList.ItemComparatorCallback<Date, ToDo> createItemCallback() {
        return new GroupSortedList.ItemComparatorCallback<Date, ToDo>() {
            @Override
            public int compare(Date group, ToDo o1, ToDo o2) {
                return o1.compareTo(o2);
            }

            @Override
            public boolean areContentsTheSame(ToDo oldItem, ToDo newItem) {
                return oldItem.getTitle().equals(newItem.getTitle()); // cache is not used
            }

            @Override
            public boolean areItemsTheSame(ToDo item1, ToDo item2) {
                return item1.getId() == item2.getId();
            }

            @Override
            public long getUniqueItemId(ToDo item) {
                return item.getId();
            }

            @Override
            public int getChildType(Date group, ToDo item) {
                return Types.TYPE_ITEM;
            }
        };
    }

    // endregion
}
