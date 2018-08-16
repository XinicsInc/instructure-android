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

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.instructure.candroid.R;
import com.instructure.candroid.activity.VideoViewActivity;
import com.instructure.candroid.adapter.ExpandableRecyclerAdapter;
import com.instructure.candroid.decorations.DividerItemDecoration;
import com.instructure.candroid.decorations.ExpandableGridSpacingDecorator;
import com.instructure.candroid.decorations.GridSpacingDecorator;
import com.instructure.interactions.Navigation;
import com.instructure.candroid.interfaces.ConfigureRecyclerView;
import com.instructure.interactions.FragmentInteractions;
import com.instructure.candroid.util.LoggingUtility;
import com.instructure.candroid.util.Param;
import com.instructure.candroid.util.StudentPrefs;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.Course;
import com.instructure.canvasapi2.models.Group;
import com.instructure.canvasapi2.models.Tab;
import com.instructure.canvasapi2.models.User;
import com.instructure.canvasapi2.utils.APIHelper;
import com.instructure.canvasapi2.utils.Logger;
import com.instructure.canvasapi2.utils.NetworkUtils;
import com.instructure.pandarecycler.BaseRecyclerAdapter;
import com.instructure.pandarecycler.PaginatedRecyclerAdapter;
import com.instructure.pandarecycler.PandaRecyclerView;
import com.instructure.pandarecycler.interfaces.EmptyViewInterface;
import com.instructure.pandarecycler.util.Types;
import com.instructure.pandautils.loaders.OpenMediaAsyncTaskLoader;
import com.instructure.pandautils.utils.Const;
import com.instructure.pandautils.utils.LoaderUtils;
import com.instructure.pandautils.utils.PermissionUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;

abstract public class ParentFragment extends DialogFragment implements ConfigureRecyclerView, FragmentInteractions {

    private CanvasContext canvasContext;

    private HashMap<String, String> mUrlParams;

    private Bundle openMediaBundle;
    private LoaderManager.LoaderCallbacks<OpenMediaAsyncTaskLoader.LoadedMedia> openMediaCallbacks;
    private ProgressDialog progressDialog;

    protected long mDefaultSelectedId = -1;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    @Nullable private Tab mTab;
    private boolean mShouldUpdateTitle = true;
    private OpenMediaAsyncTaskLoader.LoadedMedia mLoadedMedia;
    private RecyclerView.ItemDecoration mSpacingDecoration;

    @Override
    @NonNull
    public Placement getFragmentPlacement() {
        return Placement.MASTER;
    }

    @Deprecated //Use CanvasContext.isCourseContext instead (Kotlin Extension)
    public boolean navigationContextIsCourse() {
        if(getCanvasContext().getType() == CanvasContext.Type.USER) {
            return false;
        } else {
            return true;
        }
    }

    @Nullable
    public Tab getTab() {
        return mTab;
    }

    @NonNull
    public HashMap<String, String> getParamForBookmark() {
        return getCanvasContextParams();
    }

    @NonNull
    public HashMap<String, String> getQueryParamForBookmark() {
        return new HashMap<>();
    }

    private HashMap<String, String> getCanvasContextParams() {
        HashMap<String, String> map = new HashMap<>();
        if(canvasContext instanceof Course || canvasContext instanceof Group) {
            map.put(Param.COURSE_ID, Long.toString(canvasContext.getId()));
        } else if(canvasContext instanceof User) {
            map.put(Param.USER_ID, Long.toString(canvasContext.getId()));
        }
        return map;
    }

    @NonNull
    public CanvasContext getCanvasContext() {
        if(canvasContext == null) {
            handleIntentExtras(getArguments());
        }
        return canvasContext;
    }

    public void setCanvasContext(@NonNull CanvasContext canvasContext) {
        this.canvasContext = canvasContext;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        //first saving my state, so the bundle wont be empty.
        dismissProgressDialog(); // Always close.
        LoaderUtils.saveLoaderBundle(outState, openMediaBundle, Const.OPEN_MEDIA_LOADER_BUNDLE);

        outState.putString("WORKAROUND_FOR_BUG_19917_KEY", "WORKAROUND_FOR_BUG_19917_VALUE");
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle args = getArguments();
        if(args != null) {
            handleIntentExtras(args);
        }
        LoggingUtility.Log(getActivity(), Log.DEBUG, Logger.getFragmentName(this) + " --> On Create");
    }

    public void setRetainInstance(ParentFragment fragment, boolean retain) {
        if(fragment != null) {
            try{
                fragment.setRetainInstance(retain);
            }catch(IllegalStateException e){
                Logger.d("failed to setRetainInstance on fragment: " + e);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LoggingUtility.Log(getActivity(), Log.DEBUG, Logger.getFragmentName(this) + " --> On Create View");
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        LoggingUtility.Log(getActivity(), Log.DEBUG, Logger.getFragmentName(this) + " --> On Activity Created");

        LoaderUtils.restoreLoaderFromBundle(getActivity().getSupportLoaderManager(), savedInstanceState, getLoaderCallbacks(), R.id.openMediaLoaderID, Const.OPEN_MEDIA_LOADER_BUNDLE);
        if (savedInstanceState != null && savedInstanceState.getBundle(Const.OPEN_MEDIA_LOADER_BUNDLE) != null) {
            showProgressDialog();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        setHasOptionsMenu(true);
    }

    @Override
    public void onDetach() {

        //This could go wrong, but we don't want to crash the app since we are just
        //dismissing the soft keyboard
        try{
            closeKeyboard();
        } catch (Exception e){
            LoggingUtility.Log(getActivity(), Log.DEBUG,
                    "An exception was thrown while trying to dismiss the keyboard: "
                            + e.getMessage());
        }

        //Very important fix for the support library and child fragments.
        try {
            Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
            childFragmentManager.setAccessible(true);
            childFragmentManager.set(this, null);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        super.onDetach();
    }

    @Override
    public void onStart() {
        super.onStart();
        LoggingUtility.Log(getActivity(), Log.DEBUG, Logger.getFragmentName(this) + " --> On Start");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        return dialog;
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        LoggingUtility.Log(getActivity(), Log.DEBUG, Logger.getFragmentName(this) + " --> On Resume");
    }

    @Override
    public void onPause() {
        super.onPause();
        LoggingUtility.Log(getActivity(), Log.DEBUG, Logger.getFragmentName(this) + " --> On Pause.");
    }

    public boolean handleBackPressed() {
        return false;
    }

    //region Toolbar & Menus

    /**
     * General setup method for toolbar menu items
     * All menu item selections are returned to the onToolbarMenuItemClick() function
     * @param toolbar a toolbar
     */
    public void setupToolbarMenu(@NonNull Toolbar toolbar) {
        addBookmarkMenuIfAllowed(toolbar);
        addOnMenuItemClickListener(toolbar);
    }

    /**
     * General setup method for toolbar menu items
     * All menu item selections are returned to the onToolbarMenuItemClick() function
     * @param toolbar a toolbar
     * @param menu xml menu resource id, R.menu.matthew_rice_is_great
     */
    public void setupToolbarMenu(@NonNull Toolbar toolbar, @MenuRes int menu) {
        toolbar.getMenu().clear();
        addBookmarkMenuIfAllowed(toolbar);
        toolbar.inflateMenu(menu);
        addOnMenuItemClickListener(toolbar);
    }

    private void addBookmarkMenuIfAllowed(@NonNull Toolbar toolbar) {
        if (allowBookmarking() && toolbar.getMenu().findItem(R.id.bookmark) == null) {
            toolbar.inflateMenu(R.menu.bookmark_menu);
        }
    }

    private void addOnMenuItemClickListener(@NonNull Toolbar toolbar) {
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return onOptionsItemSelected(item);
            }
        });
    }

    /**
     * Override to handle toolbar menu item clicks
     * Super() should be called most if not all of the time.
     * @param item a menu item
     * @return true if the menu item click was handled
     */
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.bookmark) {
            if(APIHelper.hasNetworkConnection()) {
                Navigation navigation = getNavigation();
                if(navigation != null) navigation.addBookmark();
            } else {
                Toast.makeText(getContext(), getContext().getString(R.string.notAvailableOffline), Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return false;
    }

    //endregion

    /**
     * Factory method for constructing a fragment of the specified type.
     *
     * Make sure to use the generic parameters of this method
     * in order to avoid casting.
     *
     *
     * @param fragmentClass The class of fragment to be created.
     * @param params The bundle of extras to be passed to the
     *               fragment's handleIntentExtras() method. This
     *               method is called immediately after the fragment is constructed.
     * @param <Type> The type of fragment that this method will return, in order to
     *               avoid casting.
     * @return The fragment that was constructed.
     */
    public static <Type extends ParentFragment> Type createFragment(@NonNull Class<Type> fragmentClass, @Nullable Bundle params) {
        Type fragment = null;
        try {
            fragment = fragmentClass.newInstance();
            fragment.setArguments(params);
        } catch (java.lang.InstantiationException e) {
            LoggingUtility.LogException(null, e);
        } catch (IllegalAccessException e) {
            LoggingUtility.LogException(null, e);
        }
        return fragment;
    }


    public static ParentFragment createParentFragment(@NonNull Class<? extends FragmentInteractions> fragmentClass, @Nullable Bundle params) {
        ParentFragment fragment = null;
        try {
            fragment = (ParentFragment)fragmentClass.newInstance();
            fragment.setArguments(params);
        } catch (java.lang.InstantiationException e) {
            LoggingUtility.LogException(null, e);
        } catch (IllegalAccessException e) {
            LoggingUtility.LogException(null, e);
        }
        return fragment;
    }

    public void loadData() {}

    public void reloadData() {}

    //Fragment-ception fix:
    //Some fragments (currently our AssigmentFragment) have children fragments.
    //In the module progression view pager these child fragments don't get
    //destroyed when the root fragment gets destroyed. Override this function
    //in the appropriate activity to remove child fragments.  For example, in
    //the module progression class we call this function when onDestroyItem
    //is called and it is implemented in the AssignmentFragment class.
    public void removeChildFragments() {}

    protected <I> I getModelObject() { return null; }

    @Override
    public void startActivity(Intent intent) {
        if(getContext() == null){return;}
        super.startActivity(intent);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        if(getContext() == null) {return;}
        super.startActivityForResult(intent, requestCode);
    }

    public HashMap<String, String> getUrlParams() {
        return mUrlParams;
    }

    public long parseLong(String l, long defaultValue) {
        long value;
        try {
            value = Long.parseLong(l);
        } catch (NumberFormatException e) {
            value = defaultValue;
        }
        return value;
    }

    public void handleIntentExtras(@Nullable Bundle extras) {

        if(extras == null) {
            setArguments(new Bundle());
            Logger.d("handleIntentExtras extras was null");
            return;
        }
        Serializable serializable =  extras.getSerializable(Const.URL_PARAMS);
        if (serializable instanceof HashMap) {
            mUrlParams = (HashMap<String, String>) extras.getSerializable(Const.URL_PARAMS);
        }

        if (getUrlParams() != null) {
            mDefaultSelectedId = parseLong(getUrlParams().get(getSelectedParamName()), -1);
        } else if (extras.containsKey(Const.ITEM_ID)) {
            mDefaultSelectedId = extras.getLong(Const.ITEM_ID, -1);
        }

        if(extras.containsKey(Const.TAB)) {
            mTab = extras.getParcelable(Const.TAB);
        }

        LoggingUtility.LogBundle(getActivity(), extras);
        setCanvasContext((CanvasContext) extras.getParcelable(Const.CANVAS_CONTEXT));
    }

    public static Bundle createBundle(@NonNull CanvasContext canvasContext) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(Const.CANVAS_CONTEXT, canvasContext);

        return bundle;
    }

    public static Bundle createBundle(@NonNull CanvasContext canvasContext, @Nullable Tab tab) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(Const.CANVAS_CONTEXT, canvasContext);
        if(tab != null) bundle.putParcelable(Const.TAB, tab);
        return bundle;
    }

    public static Bundle createBundle(@NonNull CanvasContext canvasContext, HashMap<String, String> params, HashMap<String, String> queryParams, String url, @Nullable Tab tab) {
        Bundle bundle = createBundle(canvasContext, tab);
        bundle.putSerializable(Const.URL_PARAMS, params);
        bundle.putSerializable(Const.URL_QUERY_PARAMS, queryParams);
        if(tab != null) bundle.putSerializable(Const.TAB_ID, tab.getTabId());
        bundle.putSerializable(Const.URL, url);
        return bundle;
    }

    public static Bundle createBundle(CanvasContext canvasContext, long itemId) {
        Bundle bundle = createBundle(canvasContext);
        bundle.putLong(Const.ITEM_ID, itemId);
        return bundle;
    }

    public Navigation getNavigation() {
        if(getActivity() instanceof Navigation) {
            return (Navigation) getActivity();
        }
        return null;
    }

    public boolean isTablet() {
        return getResources().getBoolean(R.bool.is_device_tablet);
    }

    @Deprecated
    public boolean isTablet(Context context) {
        return context != null && context.getResources().getBoolean(R.bool.is_device_tablet);
    }

    public boolean isLandscape(Context context) {
        return context != null && context.getResources().getBoolean(R.bool.isLandscape);
    }

    public boolean apiCheck(){
        return isAdded();
    }

    /**
     * Will try to save data if some exits
     * Intended to be used with @dataLossResume()
     * @param editText
     * @param preferenceConstant the constant the fragment is using to store data
     */
    final public void dataLossPause(final EditText editText, final String preferenceConstant) {
        if(editText != null && !TextUtils.isEmpty(editText.getText())) {
            //Data exists in message editText so we want to save it.
            StudentPrefs.INSTANCE.putString(preferenceConstant, editText.getText().toString());
        }
    }

    /**
     * Restores data that may have been lost by navigating
     * @param editText
     * @param preferenceConstant the constant the fragment is using to store data
     * @return if the data was restored
     */
    final public boolean dataLossResume(final EditText editText, final String preferenceConstant) {
        //If we have no text in our editText
        if(editText != null && TextUtils.isEmpty(editText.getText())) {
            //and we have text stored, we can restore that text
            String messageText = StudentPrefs.INSTANCE.getString(preferenceConstant, "");
            if (!TextUtils.isEmpty(messageText)) {
                editText.setText(messageText);
                return true;
            }
        }
        return false;
    }

    /**
     * Will remove any data for a given constant
     * @param preferenceConstant
     */
    final public void dataLossDeleteStoredData(final String preferenceConstant) {
        StudentPrefs.INSTANCE.remove(preferenceConstant);
    }

    /**
     * A text watcher that will remove any data stored when the user has removed all text
     * @param editText
     * @param preferenceConstant the constant the fragment is using to store data
     */
    final public void dataLossAddTextWatcher(final EditText editText, final String preferenceConstant) {
        if (editText != null) {
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (TextUtils.isEmpty(s.toString())) {
                        dataLossDeleteStoredData(preferenceConstant);
                    }
                }
            });
        }
    }

    public void closeKeyboard() {
        // Check if no view has focus:
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    // region RecyclerView Methods

    // The paramName is used to specify which param should be selected when the list loads for the first time
    protected String getSelectedParamName() {
        return "";
    }

    protected long getDefaultSelectedId() {
        return mDefaultSelectedId;
    }

    protected void setDefaultSelectedId(long id) {
        mDefaultSelectedId = id;
    }

    public void setRefreshing(boolean isRefreshing) {
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setRefreshing(isRefreshing);
        }
    }

    public void setRefreshingEnabled(boolean isEnabled) {
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setEnabled(isEnabled);
        }
    }

    @Override
    public PandaRecyclerView configureRecyclerView(
            View rootView,
            Context context,
            final BaseRecyclerAdapter baseRecyclerAdapter,
            int swipeRefreshLayoutResId,
            int emptyViewResId,
            int recyclerViewResId) {
        return configureRecyclerView(rootView, context, baseRecyclerAdapter, swipeRefreshLayoutResId, emptyViewResId, recyclerViewResId, getResources().getString(R.string.noItemsToDisplayShort), false);
    }

    @Override
    public PandaRecyclerView configureRecyclerView(
            View rootView,
            Context context,
            final BaseRecyclerAdapter baseRecyclerAdapter,
            int swipeRefreshLayoutResId,
            int emptyViewResId,
            int recyclerViewResId,
            int emptyViewStringResId) {
        return configureRecyclerView(rootView, context, baseRecyclerAdapter, swipeRefreshLayoutResId, emptyViewResId, recyclerViewResId, getResources().getString(emptyViewStringResId), false);
    }

    @Override
    public PandaRecyclerView configureRecyclerView(
            View rootView,
            Context context,
            final BaseRecyclerAdapter baseRecyclerAdapter,
            int swipeRefreshLayoutResId,
            int emptyViewResId,
            int recyclerViewResId,
            String emptyViewString) {
        return configureRecyclerView(rootView, context, baseRecyclerAdapter, swipeRefreshLayoutResId, emptyViewResId, recyclerViewResId, emptyViewString, false);
    }

    @Override
    public PandaRecyclerView configureRecyclerView(
            View rootView,
            Context context,
            final BaseRecyclerAdapter baseRecyclerAdapter,
            int swipeRefreshLayoutResId,
            int emptyViewResId,
            int recyclerViewResId,
            boolean withDividers) {
        return configureRecyclerView(rootView, context, baseRecyclerAdapter, swipeRefreshLayoutResId, emptyViewResId, recyclerViewResId, getResources().getString(R.string.noItemsToDisplayShort), withDividers);
    }

    @Override
    public PandaRecyclerView configureRecyclerView(
            View rootView,
            Context context,
            final BaseRecyclerAdapter baseRecyclerAdapter,
            int swipeRefreshLayoutResId,
            int emptyViewResId,
            int recyclerViewResId,
            String emptyViewString,
            boolean withDivider) {
        EmptyViewInterface emptyViewInterface = rootView.findViewById(emptyViewResId);
        PandaRecyclerView recyclerView = rootView.findViewById(recyclerViewResId);

        baseRecyclerAdapter.setSelectedItemId(getDefaultSelectedId());
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setEmptyView(emptyViewInterface);
        emptyViewInterface.emptyViewText(emptyViewString);
        emptyViewInterface.setNoConnectionText(getString(R.string.noConnection));
        recyclerView.setSelectionEnabled(true);
        recyclerView.setAdapter(baseRecyclerAdapter);
        if(withDivider) {
            recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL_LIST));
        }

        mSwipeRefreshLayout = rootView.findViewById(swipeRefreshLayoutResId);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (!NetworkUtils.isNetworkAvailable()) {
                    mSwipeRefreshLayout.setRefreshing(false);
                } else {
                    baseRecyclerAdapter.refresh();
                }
            }
        });

        return recyclerView;
    }

    @Override
    public void configureRecyclerViewAsGrid(
            View rootView,
            final BaseRecyclerAdapter baseRecyclerAdapter,
            int swipeRefreshLayoutResId,
            int emptyViewResId,
            int recyclerViewResId) {
        configureRecyclerViewAsGrid(rootView, baseRecyclerAdapter, swipeRefreshLayoutResId, emptyViewResId, recyclerViewResId, R.string.noItemsToDisplayShort);
    }

    @Override
    public void configureRecyclerViewAsGrid(View rootView, BaseRecyclerAdapter baseRecyclerAdapter, int swipeRefreshLayoutResId, int emptyViewResId, int recyclerViewResId, int emptyViewStringResId, int span) {
        configureRecyclerViewAsGrid(rootView, baseRecyclerAdapter, swipeRefreshLayoutResId, emptyViewResId, recyclerViewResId, emptyViewStringResId, span, null);
    }

    @Override
    public void configureRecyclerViewAsGrid(
            View rootView,
            final BaseRecyclerAdapter baseRecyclerAdapter,
            int swipeRefreshLayoutResId,
            int emptyViewResId,
            int recyclerViewResId,
            int emptyViewStringResId,
            Drawable...emptyImage) {
        configureRecyclerViewAsGrid(rootView, baseRecyclerAdapter, swipeRefreshLayoutResId, emptyViewResId, recyclerViewResId, emptyViewStringResId, null, emptyImage);
    }

    @Override
    public void configureRecyclerViewAsGrid(View rootView, BaseRecyclerAdapter baseRecyclerAdapter, int swipeRefreshLayoutResId, int emptyViewResId, int recyclerViewResId, int emptyViewStringResId, View.OnClickListener emptyImageClickListener, Drawable... emptyImage) {
        final int minCardWidth = getResources().getDimensionPixelOffset(R.dimen.course_card_min_width);
        final Display display = getActivity().getWindowManager().getDefaultDisplay();
        final Point size = new Point();
        display.getSize(size);
        final int width = size.x;
        final int cardPadding = getResources().getDimensionPixelOffset(R.dimen.card_outer_margin);

        //Sets a dynamic span size based on the min card width we need to display the color chooser.
        final int span;
        if(width != 0) {
            span = width / (minCardWidth + cardPadding);
        } else {
            span = 1;
        }

        configureRecyclerViewAsGrid(rootView, baseRecyclerAdapter, swipeRefreshLayoutResId, emptyViewResId, recyclerViewResId, emptyViewStringResId, span < 1 ? 1 : span, emptyImageClickListener, emptyImage);
    }

    @Override
    public void configureRecyclerViewAsGrid(
            View rootView,
            final BaseRecyclerAdapter baseRecyclerAdapter,
            int swipeRefreshLayoutResId,
            int emptyViewResId,
            int recyclerViewResId,
            int emptyViewStringResId,
            final int span,
            View.OnClickListener emptyImageListener,
            Drawable...emptyImage) {

        final int cardPadding = getResources().getDimensionPixelOffset(R.dimen.card_outer_margin);
        EmptyViewInterface emptyViewInterface = rootView.findViewById(emptyViewResId);
        final PandaRecyclerView recyclerView = rootView.findViewById(recyclerViewResId);
        baseRecyclerAdapter.setSelectedItemId(getDefaultSelectedId());
        emptyViewInterface.emptyViewText(emptyViewStringResId);
        emptyViewInterface.setNoConnectionText(getString(R.string.noConnection));

        if(emptyImage != null && emptyImage.length > 0) {
            emptyViewInterface.emptyViewImage(emptyImage[0]);
            if(emptyImageListener != null && emptyViewInterface.getEmptyViewImage() != null) {
                emptyViewInterface.getEmptyViewImage().setOnClickListener(emptyImageListener);
            }
        }

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), span, GridLayoutManager.VERTICAL, false);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if(position < recyclerView.getAdapter().getItemCount()) {
                    int viewType = recyclerView.getAdapter().getItemViewType(position);
                    if (Types.TYPE_HEADER == viewType || PaginatedRecyclerAdapter.LOADING_FOOTER_TYPE == viewType) {
                        return span;
                    }
                } else {
                    //if something goes wrong it will take up the entire space, but at least it won't crash
                    return span;
                }
                return 1;
            }
        });

        if(mSpacingDecoration != null) {
            recyclerView.removeItemDecoration(mSpacingDecoration);
        }

        if(baseRecyclerAdapter instanceof ExpandableRecyclerAdapter) {
            mSpacingDecoration = new ExpandableGridSpacingDecorator(cardPadding);
        } else {
            mSpacingDecoration = new GridSpacingDecorator(cardPadding);
        }
        recyclerView.addItemDecoration(mSpacingDecoration);


        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setEmptyView(emptyViewInterface);
        recyclerView.setAdapter(baseRecyclerAdapter);
        mSwipeRefreshLayout = rootView.findViewById(swipeRefreshLayoutResId);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (!com.instructure.pandautils.utils.Utils.isNetworkAvailable(getContext())) {
                    mSwipeRefreshLayout.setRefreshing(false);
                } else {
                    baseRecyclerAdapter.refresh();
                }
            }
        });

    }
    // endregion

    // region OpenMediaAsyncTaskLoader

    private LoaderManager.LoaderCallbacks<OpenMediaAsyncTaskLoader.LoadedMedia> getLoaderCallbacks() {
        if (openMediaCallbacks == null) {
            openMediaCallbacks = new LoaderManager.LoaderCallbacks<OpenMediaAsyncTaskLoader.LoadedMedia>() {
                @Override
                public Loader<OpenMediaAsyncTaskLoader.LoadedMedia> onCreateLoader(int id, Bundle args) {
                    showProgressDialog();
                    return new OpenMediaAsyncTaskLoader(getContext(), args);
                }

                @Override
                public void onLoadFinished(Loader<OpenMediaAsyncTaskLoader.LoadedMedia> loader, OpenMediaAsyncTaskLoader.LoadedMedia loadedMedia) {
                    dismissProgressDialog();

                    try {
                        if (loadedMedia.isError()) {
                            if(loadedMedia.getErrorType() == OpenMediaAsyncTaskLoader.ERROR_TYPE.NO_APPS) {
                                mLoadedMedia = loadedMedia;
                                Snackbar.make(getView(), getString(R.string.noAppsShort), Snackbar.LENGTH_LONG)
                                        .setAction(getString(R.string.download), snackbarClickListener)
                                        .setActionTextColor(Color.WHITE)
                                        .show();
                            } else {
                                Toast.makeText(getActivity(), getActivity().getResources().getString(loadedMedia.getErrorMessage()), Toast.LENGTH_LONG).show();
                            }
                        } else if (loadedMedia.isHtmlFile()) {
                            InternalWebviewFragment.Companion.loadInternalWebView(getActivity(), (Navigation) getActivity(), loadedMedia.getBundle());
                        } else if (loadedMedia.getIntent() != null) {
                            if (loadedMedia.getIntent().getType().equals("video/mp4")){
                                getActivity().startActivity(VideoViewActivity.createIntent(getContext(), loadedMedia.getIntent().getDataString()));
                            } else {
                                // 파일 > pdf 선택 시
                                // 학생이 제출한 과제 볼 때에
                                getActivity().startActivity(loadedMedia.getIntent());
                            }
                        }
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(getActivity(), R.string.noApps, Toast.LENGTH_LONG).show();
                    }
                    openMediaBundle = null; // set to null, otherwise the progressDialog will appear again
                }

                @Override
                public void onLoaderReset(Loader<OpenMediaAsyncTaskLoader.LoadedMedia> loader) {
                }
            };
        }
        return openMediaCallbacks;
    }

    public View.OnClickListener snackbarClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            try {
                downloadFileToDownloadDir(getContext(), mLoadedMedia.getIntent().getData().getPath(), mLoadedMedia.getIntent().getData().getLastPathSegment());
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getActivity(), R.string.errorOccurred, Toast.LENGTH_LONG).show();
            }
        }
    };

    public void openMedia(String mime, String url, String filename) {
        if(getActivity() != null) {
            openMediaBundle = OpenMediaAsyncTaskLoader.createBundle(getCanvasContext(), mime, url, filename);
            LoaderUtils.restartLoaderWithBundle(getActivity().getSupportLoaderManager(), openMediaBundle, getLoaderCallbacks(), R.id.openMediaLoaderID);
        }
    }

    public void openMedia(boolean isSubmission, String mime, String url, String filename) {
        if(getActivity() != null) {
            openMediaBundle = OpenMediaAsyncTaskLoader.createBundle(getCanvasContext(), isSubmission, mime, url, filename);
            LoaderUtils.restartLoaderWithBundle(getActivity().getSupportLoaderManager(), openMediaBundle, getLoaderCallbacks(), R.id.openMediaLoaderID);
        }
    }

    public void openMedia(CanvasContext canvasContext, String url) {
        if(getActivity() != null) {
            openMediaBundle = OpenMediaAsyncTaskLoader.createBundle(canvasContext, url);
            LoaderUtils.restartLoaderWithBundle(getActivity().getSupportLoaderManager(), openMediaBundle, getLoaderCallbacks(), R.id.openMediaLoaderID);
        }
    }

    public void openMedia(String mime, String url, String filename, boolean useOutsideApps) {
        if(getActivity() != null) {
            openMediaBundle = OpenMediaAsyncTaskLoader.createBundle(getCanvasContext(), mime, url, filename, useOutsideApps);
            LoaderUtils.restartLoaderWithBundle(getActivity().getSupportLoaderManager(), openMediaBundle, getLoaderCallbacks(), R.id.openMediaLoaderID);
        }
    }

    private File downloadFileToDownloadDir(Context context, String url, String filename) throws Exception {
        // We should have the file cached locally at this point; We'll just move it to the user's Downloads folder

        if (!PermissionUtils.hasPermissions(context, PermissionUtils.WRITE_EXTERNAL_STORAGE)) {
            requestPermissions(PermissionUtils.makeArray(PermissionUtils.WRITE_EXTERNAL_STORAGE), PermissionUtils.WRITE_FILE_PERMISSION_REQUEST_CODE);
            return null;
        }

        Log.d(Const.OPEN_MEDIA_ASYNC_TASK_LOADER_LOG, "downloadFile URL: " + url);
        File attachmentFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), mLoadedMedia.getIntent().getData().getLastPathSegment());

        if (!attachmentFile.exists()) {
            // We've downloaded and cached this file already, so we'll just move it to the download directory
            InputStream src = context.getContentResolver().openInputStream(mLoadedMedia.getIntent().getData());
            OutputStream dst = new FileOutputStream(attachmentFile);

            byte[] buffer = new byte[1024];
            int len;
            while ((len = src.read(buffer)) > 0) {
                dst.write(buffer, 0, len);
            }
        }
        return attachmentFile;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionUtils.WRITE_FILE_PERMISSION_REQUEST_CODE) {
            if (PermissionUtils.permissionGranted(permissions, grantResults, PermissionUtils.WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(getContext(), R.string.filePermissionGranted, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getContext(), R.string.filePermissionDenied, Toast.LENGTH_LONG).show();
            }
        }
    }

    // ProgressDialog
    private void initProgressDialog() {
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setCancelable(true);
        progressDialog.setMessage(getString(R.string.opening));
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                dismissProgressDialog();
                openMediaBundle = null; // set to null, otherwise the progressDialog will appear again
                if (getActivity() == null) { return; }
                getActivity().getSupportLoaderManager().destroyLoader(R.id.openMediaLoaderID);
            }
        });
        progressDialog.setCanceledOnTouchOutside(true);
    }

    public void showProgressDialog() {
        if (getActivity() != null && progressDialog == null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    initProgressDialog();
                }
            });
        }

        if (getActivity() != null && progressDialog != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.show();
                }
            });
        }
    }

    public void dismissProgressDialog() {
        if (getActivity() != null && progressDialog != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.dismiss();
                }
            });
        }
    }

    // endregion


    public void showToast(int stringResId) {
        if(isAdded()) {
            Toast.makeText(getActivity(), stringResId, Toast.LENGTH_SHORT).show();
        }
    }

    public void showToast(String message) {
        if(TextUtils.isEmpty(message)) {
            return;
        }
        if(isAdded()) {
            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
        }
    }

    public void showToast(int stringResId, int length) {
        if(isAdded()) {
            Toast.makeText(getActivity(), stringResId, length).show();
        }
    }

    public void showToast(String message, int length) {
        if(TextUtils.isEmpty(message)) {
            return;
        }
        if(isAdded()) {
            Toast.makeText(getActivity(), message, length).show();
        }
    }
}
