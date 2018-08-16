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
package com.instructure.loginapi.login.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.instructure.canvasapi2.utils.APIHelper;
import com.instructure.canvasapi2.utils.ApiPrefs;
import com.instructure.loginapi.login.R;
import com.instructure.loginapi.login.adapter.PreviousUsersAdapter;
import com.instructure.loginapi.login.adapter.SnickerDoodleAdapter;
import com.instructure.loginapi.login.dialog.NoInternetConnectionDialog;
import com.instructure.loginapi.login.model.SignedInUser;
import com.instructure.loginapi.login.snicker.SnickerDoodle;
import com.instructure.loginapi.login.util.Const;
import com.instructure.loginapi.login.util.PreviousUsersUtils;
import com.instructure.pandautils.utils.ColorUtils;
import com.instructure.pandautils.utils.ViewStyler;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Calendar;

import static com.instructure.loginapi.login.util.Const.CANVAS_LOGIN_FLOW;
import static com.instructure.loginapi.login.util.Const.MASQUERADE_FLOW;
import static com.instructure.loginapi.login.util.Const.MOBILE_VERIFY_FLOW;
import static com.instructure.loginapi.login.util.Const.NORMAL_FLOW;
import static com.instructure.loginapi.login.util.Const.SNICKER_DOODLES;

public abstract class BaseLoginLandingPageActivity extends AppCompatActivity {

    static { AppCompatDelegate.setCompatVectorFromResourcesEnabled(true); }

    private View mPreviousLoginWrapper;
    private RecyclerView mPreviousLoginRecyclerView;
    private Button mFindMySchoolButton;
    private @Nullable TextView mAppDescriptionType;
    private ImageView mCanvasLogo;
    private GestureDetector mGesture;
    private DrawerLayout mDrawerLayout;
    private RecyclerView mDrawerRecyclerView;

    private boolean mGestureFirstFree = true;
    private long mGestureFirst = 0;
    private long mGestureSecond = 0;
    private int mCanvasLogin = NORMAL_FLOW;

    protected abstract Intent beginFindSchoolFlow();
    protected abstract Intent signInActivityIntent(@NonNull SnickerDoodle snickerDoodle);
    protected abstract @ColorInt int themeColor();
    protected abstract @StringRes int appTypeName();
    protected abstract Intent launchApplicationMainActivityIntent();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_landing_page);
        bindViews();
        applyTheme();
        loadPreviousUsers();
        setupGesture();
        setupSnickerDoodles();
    }

    private void bindViews() {
        mPreviousLoginWrapper = findViewById(R.id.previousLoginWrapper);
        mPreviousLoginRecyclerView = (RecyclerView) findViewById(R.id.previousLoginRecyclerView);
        mFindMySchoolButton = (Button) findViewById(R.id.findMySchool);
        mAppDescriptionType = (TextView) findViewById(R.id.appDescriptionType);
        mCanvasLogo = (ImageView) findViewById(R.id.canvasLogo);

        mFindMySchoolButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(APIHelper.hasNetworkConnection()) {
                    Intent intent = beginFindSchoolFlow();
                    intent.putExtra(Const.CANVAS_LOGIN, mCanvasLogin);
                    startActivity(intent);
                } else {
                    NoInternetConnectionDialog.show(getSupportFragmentManager());
                }
            }
        });
    }

    private void loadPreviousUsers() {
        ArrayList<SignedInUser> previousUsers = PreviousUsersUtils.get(this);
        resizePreviousUsersRecyclerView(previousUsers);

        mPreviousLoginRecyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        mPreviousLoginRecyclerView.setAdapter(new PreviousUsersAdapter(previousUsers, new PreviousUsersAdapter.PreviousUsersEvents() {
            @Override
            public void onPreviousUserClick(SignedInUser user) {
                ApiPrefs.setProtocol(user.protocol);
                ApiPrefs.setUser(user.user);
                ApiPrefs.setDomain(user.domain);
                ApiPrefs.setToken(user.token);

                Intent intent = launchApplicationMainActivityIntent();
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onRemovePreviousUserClick(SignedInUser user, int position) {
                PreviousUsersUtils.remove(BaseLoginLandingPageActivity.this, user);
            }

            @Override
            public void onNowEmpty() {
                ObjectAnimator fade = ObjectAnimator.ofFloat(mPreviousLoginWrapper, View.ALPHA, 1F, 0F);
                ObjectAnimator move = ObjectAnimator.ofFloat(mPreviousLoginWrapper, View.TRANSLATION_Y, 0, mPreviousLoginWrapper.getTop());

                AnimatorSet set = new AnimatorSet();
                set.playTogether(fade, move);
                set.setDuration(430);
                set.start();
            }
        }));
        mPreviousLoginWrapper.setVisibility((previousUsers.size() > 0) ? View.VISIBLE : View.GONE);
    }

    private void resizePreviousUsersRecyclerView(ArrayList<SignedInUser> previousUsers) {
        final int maxUsersToShow = getResources().getInteger(R.integer.login_previousMaxVisible);
        if(previousUsers.size() == 1 && maxUsersToShow > 1) {
            //Resize the view to only show one previous user
            ViewGroup.LayoutParams params = mPreviousLoginRecyclerView.getLayoutParams();
            params.height = getResources().getDimensionPixelOffset(R.dimen.login_previousLoginHeight_1x);
            mPreviousLoginRecyclerView.setLayoutParams(params);
        }
    }

    private void applyTheme() {
        //Colors
        final int color = themeColor();
        final int buttonColor = ContextCompat.getColor(this, R.color.login_loginFlowBlue);

        //Button
        Drawable wrapDrawable = DrawableCompat.wrap(mFindMySchoolButton.getBackground());
        DrawableCompat.setTint(wrapDrawable, buttonColor);
        mFindMySchoolButton.setBackground(DrawableCompat.unwrap(wrapDrawable));

        if(getApplicationName().toLowerCase().contains("teacher")) {
            mCanvasLogo.setImageResource(R.drawable.vd_find_school_teacher);
        }

        if(mAppDescriptionType != null) {
            //App Name/Type
            mAppDescriptionType.setTextColor(color);
            mAppDescriptionType.setText(appTypeName());
        }

        ViewStyler.setStatusBarLight(this);
    }

    private String getApplicationName() {
        Context context = getApplicationContext();
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo applicationInfo = getApplicationInfo();
        return (String) (applicationInfo != null ? packageManager.getApplicationLabel(applicationInfo) : "Unknown");
    }

    private void setupGesture() {
        mGesture = new GestureDetector(getApplicationContext(), new GestureDetector.SimpleOnGestureListener() {
            public boolean onDown(MotionEvent event) {
                return true;
            }
        });

        findViewById(R.id.rootView).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mGesture.onTouchEvent(event);
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            int action = event.getAction() & MotionEvent.ACTION_MASK;
            //capture the event when the user lifts their fingers, not on the down press
            //to make sure they're not long pressing
            if (action == MotionEvent.ACTION_POINTER_UP) {
                //timer to get difference between clicks
                Calendar now = Calendar.getInstance();

                //detect number of fingers, change to 1 for a single-finger double-click, 3 for a triple-finger double-click!
                if (event.getPointerCount() == 2) {
                    mGestureFirstFree = !mGestureFirstFree;

                    if (mGestureFirstFree) {
                        //if this is the first click, then there hasn't been a second
                        //click yet, also record the time
                        mGestureFirst = now.getTimeInMillis();
                    } else {
                        //if this is the second click, record its time
                        mGestureSecond = now.getTimeInMillis();
                    }

                    //if the difference between the 2 clicks is less than 500 ms (1/2 second)
                    //Math.abs() is used because you need to be able to detect any sequence of clicks, rather than just in pairs of two
                    //(e.g. click1 could be registered as a second click if the difference between click1 and click2 > 500 but
                    //click2 and the next click1 is < 500)

                    if (Math.abs(mGestureSecond - mGestureFirst) < 500) {
                        mCanvasLogin++;

                        /* Cycle between 0, 1, 2, and 3
                         *
                         * 0 == no special login
                         * 1 == canvas login
                         * 2 == site admin
                         * 3 == No mobile verify check
                         */
                        if (mCanvasLogin > MOBILE_VERIFY_FLOW) {
                            mCanvasLogin = NORMAL_FLOW;
                        }

                        if (mCanvasLogin == NORMAL_FLOW) {
                            Toast.makeText(BaseLoginLandingPageActivity.this, R.string.canvasLoginOff, Toast.LENGTH_SHORT).show();
                        } else if (mCanvasLogin == CANVAS_LOGIN_FLOW) {
                            Toast.makeText(BaseLoginLandingPageActivity.this, R.string.canvasLoginOn, Toast.LENGTH_SHORT).show();
                        } else if (mCanvasLogin == MASQUERADE_FLOW) {
                            Toast.makeText(BaseLoginLandingPageActivity.this, R.string.siteAdminLogin, Toast.LENGTH_SHORT).show();
                        } else if (mCanvasLogin == MOBILE_VERIFY_FLOW) {
                            Toast.makeText(BaseLoginLandingPageActivity.this, R.string.mobileVerifyOff, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        } catch (Exception e) {
            //Do Nothing
        }
        return true;
    }


    /**
     * Adds a simple login method for devs. To add credentials add your snickers (credentials) to the snickers.json
     * Slide the drawer out from the right to have a handy one click login. FYI: Only works on Debug.
     * Sample Format is:

         [
            {
                 "password":"password",
                 "subtitle":"subtitle",
                 "title":"title",
                 "username":"username",
                 "domain":"about.blank"
             },
              ...
         ]

     */
    private void setupSnickerDoodles() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        mDrawerRecyclerView = (RecyclerView) findViewById(R.id.drawerRecyclerView);
        boolean isDebuggable =  (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
        mDrawerLayout.setDrawerLockMode(isDebuggable ? DrawerLayout.LOCK_MODE_UNLOCKED : DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        if(isDebuggable) {
            Writer writer = new StringWriter();
            try {
                InputStream is = getResources().openRawResource(getResources().getIdentifier("snickers", "raw", getPackageName()));

                char[] buffer = new char[1024];
                Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) { writer.write(buffer, 0, n); }
                is.close();
            } catch (Exception e) {
                //Do Nothing
            }

            String jsonString = writer.toString();
            if(jsonString != null && jsonString.length() > 0) {
                ArrayList<SnickerDoodle> snickerDoodles = new Gson().fromJson(jsonString, new TypeToken<ArrayList<SnickerDoodle>>(){}.getType());

                if(snickerDoodles.size() == 0) {
                    findViewById(R.id.drawerEmptyView).setVisibility(View.VISIBLE);
                    findViewById(R.id.drawerEmptyText).setVisibility(View.VISIBLE);
                    return;
                }

                mDrawerRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true));
                mDrawerRecyclerView.setAdapter(new SnickerDoodleAdapter(snickerDoodles, new SnickerDoodleAdapter.SnickerCallback() {
                    @Override
                    public void onClick(SnickerDoodle snickerDoodle) {
                        mDrawerLayout.closeDrawers();
                        Intent intent = signInActivityIntent(snickerDoodle);
                        intent.putExtra(SNICKER_DOODLES, snickerDoodle);
                        startActivity(intent);
                        finish();
                    }
                }));
            } else {
                findViewById(R.id.drawerEmptyView).setVisibility(View.VISIBLE);
                findViewById(R.id.drawerEmptyText).setVisibility(View.VISIBLE);
            }
        }
    }
}
