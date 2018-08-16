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
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import com.instructure.candroid.R;
import com.instructure.interactions.Navigation;
import com.instructure.interactions.FragmentInteractions;
import com.instructure.candroid.interfaces.OnEventUpdatedCallback;
import com.instructure.candroid.util.Param;
import com.instructure.candroid.util.RouterUtils;
import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.managers.CalendarEventManager;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.ScheduleItem;
import com.instructure.canvasapi2.utils.APIHelper;
import com.instructure.canvasapi2.utils.ApiPrefs;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.DateHelper;
import com.instructure.canvasapi2.utils.LinkHeaders;
import com.instructure.pandautils.utils.Const;
import com.instructure.pandautils.utils.OnBackStackChangedEvent;
import com.instructure.pandautils.utils.PandaViewUtils;
import com.instructure.pandautils.utils.ViewStyler;
import com.instructure.pandautils.views.CanvasWebView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Date;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import retrofit2.Response;

public class CalendarEventFragment extends ParentFragment {
    // view variables
    private CanvasWebView canvasWebView;
    private View calendarView;
    private Toolbar toolbar;
    private TextView date1;
    private TextView date2;
    private TextView address1;
    private TextView address2;

    // model variables
    private ScheduleItem scheduleItem;
    private long scheduleItemId;

    private StatusCallback<ScheduleItem> scheduleItemCallback;
    private StatusCallback<ScheduleItem> mDeleteItemCallback;

    private OnEventUpdatedCallback mOnEventUpdatedCallback;

    @Override
    @NonNull
    public FragmentInteractions.Placement getFragmentPlacement() {return FragmentInteractions.Placement.DETAIL; }

    @Override
    @NonNull
    public String title() {
        return scheduleItem != null ? scheduleItem.getTitle() : getString(R.string.Event);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_calendar_event, container, false);
        initViews(rootView);

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if(context instanceof OnEventUpdatedCallback){
            mOnEventUpdatedCallback = (OnEventUpdatedCallback)context;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setUpCallback();
        if (scheduleItem == null) {
            CalendarEventManager.getCalendarEvent(scheduleItemId, scheduleItemCallback, true);
        } else {
            populateViews();
        }
    }

    @Override
    public void applyTheme() {
        if(scheduleItem != null && scheduleItem.getContextId() == ApiPrefs.getUser().getId()) {
            setupToolbarMenu(toolbar, R.menu.calendar_event_menu);
        }
        PandaViewUtils.setupToolbarBackButtonAsBackPressedOnly(toolbar, this);

        ViewStyler.themeToolbar(getActivity(), toolbar, getCanvasContext());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_delete:
                if(!APIHelper.hasNetworkConnection()) {
                    Toast.makeText(getContext(), getContext().getString(R.string.notAvailableOffline), Toast.LENGTH_SHORT).show();
                    return true;
                }
                deleteEvent();
                return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        Dialog dialog = getDialog();
        if(dialog != null && !isTablet(getActivity())) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (canvasWebView != null) {
            canvasWebView.onPause();
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        if (canvasWebView != null) {
            canvasWebView.onResume();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBackStackChangedEvent(OnBackStackChangedEvent event) {
        event.get(new Function1<Class<?>, Unit>() {
            @Override
            public Unit invoke(Class<?> clazz) {
                if(clazz != null && clazz.isAssignableFrom(CalendarEventFragment.class)) {
                    if (canvasWebView != null) {
                        canvasWebView.onResume();
                    }
                } else {
                    if (canvasWebView != null) {
                        canvasWebView.onPause();
                    }
                }
                return null;
            }
        });
    }

    ///////////////////////////////////////////////////////////////////////////
    // View
    ///////////////////////////////////////////////////////////////////////////

    void initViews(View rootView) {
        toolbar = rootView.findViewById(R.id.toolbar);
        calendarView = rootView.findViewById(R.id.calendarView);

        date1 = rootView.findViewById(R.id.date1);
        date2 = rootView.findViewById(R.id.date2);
        address1 = rootView.findViewById(R.id.address1);
        address2 = rootView.findViewById(R.id.address2);

        canvasWebView = rootView.findViewById(R.id.description);
        canvasWebView.addVideoClient(getActivity());
        canvasWebView.setCanvasEmbeddedWebViewCallback(new CanvasWebView.CanvasEmbeddedWebViewCallback() {
            @Override
            public void launchInternalWebViewFragment(String url) {
                InternalWebviewFragment.Companion.loadInternalWebView(getActivity(), (Navigation) getActivity(), InternalWebviewFragment.Companion.createBundle(getCanvasContext(), url, false));
            }

            @Override
            public boolean shouldLaunchInternalWebViewFragment(String url) {
                return true;
            }
        });
        canvasWebView.setCanvasWebViewClientCallback(new CanvasWebView.CanvasWebViewClientCallback() {
            @Override
            public void openMediaFromWebView(String mime, String url, String filename) {
                openMedia(mime, url, filename);
            }

            @Override
            public void onPageStartedCallback(WebView webView, String url) {

            }

            @Override
            public void onPageFinishedCallback(WebView webView, String url) {

            }

            @Override
            public boolean canRouteInternallyDelegate(String url) {
                return RouterUtils.canRouteInternally(null, url, ApiPrefs.getDomain(), false);
            }

            @Override
            public void routeInternallyCallback(String url) {
                RouterUtils.canRouteInternally(getActivity(), url, ApiPrefs.getDomain(), true);
            }
        });
    }

    void populateViews() {
        toolbar.setTitle(title());
        String content = scheduleItem.getDescription();

        calendarView.setVisibility(View.VISIBLE);
        canvasWebView.setVisibility(View.GONE);

        if(scheduleItem.isAllDay()) {
            date1.setText(getString(R.string.allDayEvent));
            date2.setText(getFullDateString(scheduleItem.getEndAt()));
        } else {
            //Setup the calendar event start/end times
            if(scheduleItem.getStartAt() != null && scheduleItem.getEndAt() != null && scheduleItem.getStartAt().getTime() != scheduleItem.getEndAt().getTime()) {
                //Our date times are different so we display two strings
                date1.setText(getFullDateString(scheduleItem.getEndAt()));
                String startTime = DateHelper.getFormattedTime(getContext(), scheduleItem.getStartAt());
                String endTime = DateHelper.getFormattedTime(getContext(), scheduleItem.getEndAt());
                date2.setText(startTime + " - " + endTime);
            } else {
                date1.setText(getFullDateString(scheduleItem.getStartAt()));
                date2.setVisibility(View.INVISIBLE);
            }
        }

        boolean noLocationTitle = TextUtils.isEmpty(scheduleItem.getLocationName());
        boolean noLocation = TextUtils.isEmpty(scheduleItem.getLocationAddress());

        if(noLocation && noLocationTitle) {
            address1.setText(getString(R.string.noLocation));
            address2.setVisibility(View.INVISIBLE);
        } else {
            if(noLocationTitle) {
                address1.setText(scheduleItem.getLocationAddress());
            } else {
                address1.setText(scheduleItem.getLocationName());
                address2.setText(scheduleItem.getLocationAddress());
            }
        }

        if(!TextUtils.isEmpty(content)){
            canvasWebView.setVisibility(View.VISIBLE);
            canvasWebView.setBackgroundColor(getResources().getColor(R.color.canvasBackgroundLight));
            canvasWebView.formatHTML(content, scheduleItem.getTitle());
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // CallBack
    ///////////////////////////////////////////////////////////////////////////

    public void setUpCallback() {
        scheduleItemCallback = new StatusCallback<ScheduleItem>() {
            @Override
            public void onResponse(@NonNull Response<ScheduleItem> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                if (response.body() != null) {
                    CalendarEventFragment.this.scheduleItem = response.body();
                    populateViews();
                }
            }
        };

        mDeleteItemCallback = new StatusCallback<ScheduleItem>() {
            @Override
            public void onResponse(@NonNull Response<ScheduleItem> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                if (!apiCheck()) {
                    return;
                }
                showToast(R.string.eventSuccessfulDeletion);
                //Refresh Calendar
                if (mOnEventUpdatedCallback != null && response.body() != null) {
                    mOnEventUpdatedCallback.onEventSaved(response.body(), true);
                }
                getActivity().onBackPressed();
            }
        };
    }

    public String getFullDateString(Date date) {
        if(scheduleItem == null || date == null) {
            return "";
        }

        String dayOfWeek = DateHelper.getFullDayFormat().format(date);
        String dateString = DateHelper.getFormattedDate(getContext(), date);

        return dayOfWeek + " " + dateString;
    }

    @Override
    public boolean handleBackPressed() {
        if(canvasWebView != null) {
            return canvasWebView.handleGoBack();
        }
        return super.handleBackPressed();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Intent
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void handleIntentExtras(Bundle extras) {
        super.handleIntentExtras(extras);

        if (extras.containsKey(Const.SCHEDULE_ITEM)) {
            scheduleItem =  extras.getParcelable(Const.SCHEDULE_ITEM);
            scheduleItemId = scheduleItem.getId();
        } else if (getUrlParams() != null) {
            scheduleItemId = parseLong(getUrlParams().get(Param.EVENT_ID), -1);
        } else {
            scheduleItemId = extras.getLong(Const.SCHEDULE_ITEM_ID, -1);
        }
    }

    public static Bundle createBundle(CanvasContext canvasContext, ScheduleItem scheduleItem) {
        Bundle extras = createBundle(canvasContext);
        extras.putParcelable(Const.SCHEDULE_ITEM, scheduleItem);
        return extras;
    }

    public static Bundle createBundle(CanvasContext canvasContext, long scheduleItemId) {
        Bundle extras = createBundle(canvasContext);
        extras.putLong(Const.SCHEDULE_ITEM_ID, scheduleItemId);
        return extras;
    }

    @Override
    public boolean allowBookmarking() {
        return false;
    }

    private void deleteEvent(){
        CalendarEventManager.deleteCalendarEvent(scheduleItem.getId(), "", mDeleteItemCallback);
    }
}
