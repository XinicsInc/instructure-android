/*
 * Copyright (C) 2016 - present  Instructure, Inc.
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

package com.instructure.parentapp.activity;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.content.Intent;
import android.graphics.LinearGradient;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.PaintDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.managers.UserManager;
import com.instructure.canvasapi2.models.Student;
import com.instructure.canvasapi2.utils.APIHelper;
import com.instructure.canvasapi2.utils.ApiPrefs;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.LinkHeaders;
import com.instructure.pandautils.dialogs.RatingDialog;
import com.instructure.pandautils.utils.AppType;
import com.instructure.pandautils.utils.Const;
import com.instructure.pandautils.utils.Prefs;
import com.instructure.parentapp.BuildConfig;
import com.instructure.parentapp.R;
import com.instructure.parentapp.adapter.CarouselPagerAdapter;
import com.instructure.parentapp.adapter.StudentActivityFragmentPagerAdapter;
import com.instructure.parentapp.util.AnalyticUtils;
import com.instructure.parentapp.util.ApplicationManager;
import com.instructure.parentapp.util.CarouselTransformer;
import com.instructure.parentapp.util.ViewUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Response;

public class StudentViewActivity extends BaseRouterActivity implements ViewPager.OnPageChangeListener{

    private ImageButton mSettingsButton;
    private TextView mStudentName;
    private ViewPager mCarouselViewPager;
    private CarouselPagerAdapter mCarouselAdapter;
    private TabLayout mTabLayout;

    private Integer[] mTopColors = new Integer[4];
    private Integer[] mBottomColors = new Integer[4];
    private Integer[] mBottomColorsNoAlpha = new Integer[4];

    private StudentActivityFragmentPagerAdapter mPagerAdapter;
    private int statusBarHeightId;
    private boolean mIsSettling = false;
    private boolean mIsDragging = false;
    private int mUnreadAlertCount;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Lock phones into portrait, no change for tablets
        ViewUtils.setScreen(this);
        setContentView(R.layout.activity_student_view);
        if(savedInstanceState != null) {
            mUnreadAlertCount = savedInstanceState.getInt(Const.UNREAD, 0);
        }

        mTopColors[0] = ContextCompat.getColor(StudentViewActivity.this, R.color.colorGradientYellow);
        mTopColors[1] = ContextCompat.getColor(StudentViewActivity.this, R.color.colorGradientCyan);
        mTopColors[2] = ContextCompat.getColor(StudentViewActivity.this, R.color.colorGradientMagenta);
        mTopColors[3] = ContextCompat.getColor(StudentViewActivity.this, R.color.colorGradientLime);

        mBottomColors[0] = ContextCompat.getColor(StudentViewActivity.this, R.color.colorGradientRed);
        mBottomColors[1] = ContextCompat.getColor(StudentViewActivity.this, R.color.colorGradientBlue);
        mBottomColors[2] = ContextCompat.getColor(StudentViewActivity.this, R.color.colorGradientPurple);
        mBottomColors[3] = ContextCompat.getColor(StudentViewActivity.this, R.color.colorGradientGreen);

        mBottomColorsNoAlpha[0] = ContextCompat.getColor(StudentViewActivity.this, R.color.colorGradientRedNoAlpha);
        mBottomColorsNoAlpha[1] = ContextCompat.getColor(StudentViewActivity.this, R.color.colorGradientBlueNoAlpha);
        mBottomColorsNoAlpha[2] = ContextCompat.getColor(StudentViewActivity.this, R.color.colorGradientPurpleNoAlpha);
        mBottomColorsNoAlpha[3] = ContextCompat.getColor(StudentViewActivity.this, R.color.colorGradientGreeneNoAlpha);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //Retrieve status bar height from non-public resource
            statusBarHeightId = getResources().getIdentifier("status_bar_height", "dimen", "android");

            //make the status bar not so translucent
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

            getWindow().setStatusBarColor(ContextCompat.getColor(StudentViewActivity.this, R.color.semi_transparent));
        }

        setupViews();
        setupListeners();

        if(!BuildConfig.IS_TESTING) {
            RatingDialog.showRatingDialog(StudentViewActivity.this, AppType.PARENT);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //save the position so when the parent comes back to this page it will load the student they were on last
        Prefs prefs = new Prefs(this, com.instructure.parentapp.util.Const.CANVAS_PARENT_SP);
        prefs.save(Const.POSITION, mCarouselViewPager.getCurrentItem());
        prefs.save(Const.TAB, mTabLayout.getSelectedTabPosition());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(mPagerAdapter != null) {
            outState.putInt(Const.UNREAD, mPagerAdapter.getAlertFragmentUnreadCount());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && requestCode == com.instructure.parentapp.util.Const.SETTINGS_ACTIVITY_REQUEST_CODE) {
            //make the api call to get all the students
            UserManager.getStudentsForParentAirwolf(
                    ApiPrefs.getAirwolfDomain(),
                    ApplicationManager.getParentId(StudentViewActivity.this),
                    new StatusCallback<List<Student>>() {
                        @Override
                        public void onResponse(@NonNull Response<List<Student>> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                            //Only non-cache data
                            if (!APIHelper.isCachedResponse(response)) {
                                if (response.body() != null && !response.body().isEmpty()) {
                                    //replace the data that the carousel will try to use
                                    if (getIntent().getExtras().getParcelableArrayList(Const.USER) != null) {
                                        Intent intent = getIntent().putParcelableArrayListExtra(Const.USER, new ArrayList<Parcelable>(response.body()));
                                        getIntent().replaceExtras(intent);
                                    }

                                    setupViews();
                                    setupListeners();
                                } else {
                                    //we have no students, finish and start the main activity, which will force them to add a student
                                    Intent intent = new Intent(StudentViewActivity.this, SplashActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    finish();
                                }
                            }
                        }
                    }
            );
        }
    }

    private void setupViews() {
        if (statusBarHeightId > 0) {
            // else it uses the default dimens in XML
            //Configure views to slide under status bar, will only effect > API 21
            int statusBarHeight = getResources().getDimensionPixelSize(statusBarHeightId);
            RelativeLayout root = (RelativeLayout) findViewById(R.id.rootView);
            RelativeLayout navigationWrapper = (RelativeLayout) findViewById(R.id.navigationWrapper);
            FrameLayout.LayoutParams rootParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
            rootParams.setMargins(0, -statusBarHeight, 0, 0);
            root.setLayoutParams(rootParams);
            navigationWrapper.setPadding(0, statusBarHeight, 0, 0);
        } else {
            RelativeLayout root = (RelativeLayout) findViewById(R.id.rootView);
            RelativeLayout navigationWrapper = (RelativeLayout) findViewById(R.id.navigationWrapper);

            FrameLayout.LayoutParams rootParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
            rootParams.setMargins(0, 0, 0, 0);
            root.setLayoutParams(rootParams);
            navigationWrapper.setPadding(0, 0, 0, 0);
        }


        mSettingsButton = (ImageButton) findViewById(R.id.settings);
        mStudentName = (TextView) findViewById(R.id.studentName);

        // Get the ViewPager and set it's PagerAdapter so that it can display items
        final ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        if(mPagerAdapter == null) {
            mPagerAdapter = new StudentActivityFragmentPagerAdapter(getSupportFragmentManager(),
                    StudentViewActivity.this);
        }
        viewPager.setAdapter(mPagerAdapter);

        //When loading course page, alert page will need to be updated so we need to be sure to
        //keep all fragments in memory
        viewPager.setOffscreenPageLimit(2);

        // Give the TabLayout the ViewPager
        mTabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        mTabLayout.setupWithViewPager(viewPager);
        mTabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getCustomView() != null) {
                    tab.getCustomView().setAlpha(1f);
                }
                viewPager.setCurrentItem(tab.getPosition());
                //set the tab content description to the title of the tab, for a11y/testing
                tab.setContentDescription(tab.getText());
                onPageScrolled(mCarouselViewPager.getCurrentItem(), 0, 0);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                if (tab.getCustomView() != null) {
                    tab.getCustomView().setAlpha(.30f);
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        // Iterate over all tabs and set the custom view
        for (int i = 0; i < mTabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = mTabLayout.getTabAt(i);
            tab.setCustomView(mPagerAdapter.getTabView(i));
            if (i != viewPager.getCurrentItem()) {
                tab.getCustomView().setAlpha(.30f);
            }
        }

        configureUserCarousel();

        Prefs prefs = new Prefs(StudentViewActivity.this, com.instructure.parentapp.util.Const.CANVAS_PARENT_SP);
        int pos = prefs.load(Const.TAB, 0);
        if(pos != 0) {
            viewPager.setCurrentItem(pos);
        }

        //update unread count
        mPagerAdapter.setAlertFragmentUnreadCount(mUnreadAlertCount);

    }

    public void updateAlertUnreadCount(int unreadCount) {
        if(mPagerAdapter != null) {
            mPagerAdapter.setAlertFragmentUnreadCount(unreadCount);
        }
    }

    private void configureUserCarousel(){
        mCarouselViewPager = (ViewPager) findViewById(R.id.carouselPager);
        mCarouselAdapter = new CarouselPagerAdapter(this);
        mCarouselViewPager.addOnPageChangeListener(this);
        mCarouselViewPager.setPageTransformer(true, new CarouselTransformer());
        if(getIntent().getExtras().getParcelableArrayList(Const.USER) != null) {
            ArrayList<Student> students = getIntent().getExtras().getParcelableArrayList(Const.USER);
            if (students != null && students.size() > 0) {
                mCarouselAdapter.clear();
                mCarouselViewPager.setOffscreenPageLimit(students.size());
                mCarouselAdapter.addAll(students);
            }
        }


        mCarouselViewPager.post(new Runnable() {
            @Override
            public void run() {
                mCarouselViewPager.setAdapter(mCarouselAdapter);
                //Use this to set the offset for the viewpager
                int childWidth = (int) getResources().getDimension(R.dimen.carousel_avatar_size);
                double factor = 1.30;
                if(getResources().getBoolean(R.bool.isTablet)) {
                    //adjust for tablets so the student's icons are closer together
                    factor = 1.15;
                }
                double width = (mCarouselViewPager.getWidth() / factor) + (childWidth / 2);
                int truncWidth = (int) width;
                mCarouselViewPager.setPageMargin(-truncWidth);
                if (mCarouselAdapter.getCount() > 1) {
                    Prefs prefs = new Prefs(StudentViewActivity.this, com.instructure.parentapp.util.Const.CANVAS_PARENT_SP);
                    int pos = prefs.load(Const.POSITION, mCarouselAdapter.getCount() / 2);
                    mCarouselViewPager.setCurrentItem(pos);
                    onPageSelected(pos);
                } else if(mCarouselAdapter.getCount() == 1){
                    //need to call onPageSelected so that it shows the user's name
                    onPageSelected(0);
                }
            }
        });

    }

    private void setupListeners() {
        mSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //use start activity for result because we need to be aware if they remove a student
                startActivityForResult(SettingsActivity.createIntent(StudentViewActivity.this, ""), com.instructure.parentapp.util.Const.SETTINGS_ACTIVITY_REQUEST_CODE);
                overridePendingTransition(R.anim.slide_from_bottom, android.R.anim.fade_out);
            }
        });
    }

    private void setSwipeRefreshColor(int color) {
        if(mPagerAdapter.getWeekFragment() != null) {
            mPagerAdapter.getWeekFragment().getSwipeRefreshLayout().setColorSchemeColors(color, color, color, color);
            if(mPagerAdapter.getWeekFragment().getProgressBar() != null && mPagerAdapter.getWeekFragment().getProgressBar().getIndeterminateDrawable() != null) {
                mPagerAdapter.getWeekFragment().getProgressBar().getIndeterminateDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
            }
        }

        if(mPagerAdapter.getCourseListFragment() != null) {
            mPagerAdapter.getCourseListFragment().getSwipeRefreshLayout().setColorSchemeColors(color, color, color, color);
            if(mPagerAdapter.getCourseListFragment().getProgressBar() != null && mPagerAdapter.getCourseListFragment().getProgressBar().getIndeterminateDrawable() != null) {
                mPagerAdapter.getCourseListFragment().getProgressBar().getIndeterminateDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
            }
        }

        if(mPagerAdapter.getAlertFragment() != null) {
            mPagerAdapter.getAlertFragment().getSwipeRefreshLayout().setColorSchemeColors(color, color, color, color);
            if(mPagerAdapter.getAlertFragment().getProgressBar() != null && mPagerAdapter.getAlertFragment().getProgressBar().getIndeterminateDrawable() != null) {
                mPagerAdapter.getAlertFragment().getProgressBar().getIndeterminateDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
            }
        }
    }
    ///////////////////////////////////////////////////////////////////////////
    // Intents
    ///////////////////////////////////////////////////////////////////////////

    public static Intent createIntent(Context context, List<Student> students) {
        Intent intent = new Intent(context, StudentViewActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putParcelableArrayListExtra(Const.USER, new ArrayList<Parcelable>(students));
        return intent;
    }

    @Override
    public void onPageSelected(int position) {
        if(mCarouselViewPager.getCurrentItem() == position) {

            // We only want to track selecting the student if they have more than one.
            // This will be called even if there is only one student
            if(mCarouselViewPager.getAdapter().getCount() > 1) {
                AnalyticUtils.trackButtonPressed(AnalyticUtils.SWIPE_STUDENT);
            }

            //Set name
            String studentName = getUserFirstName(mCarouselAdapter.getItem(position).getStudentName());
            //a11y for the student name is covered by the avatars a11y
            mStudentName.setText(ViewUtils.applyKerning(studentName, .5f));

            if(mPagerAdapter.getCourseListFragment() != null) {
                if(mPagerAdapter.getCourseListFragment().getStudent().getStudentId().equals(mCarouselAdapter.getItem(position).getStudentId())) {
                    mPagerAdapter.getCourseListFragment().refreshWithStudent(mCarouselAdapter.getItem(position), false);
                } else {
                    mPagerAdapter.getCourseListFragment().refreshWithStudent(mCarouselAdapter.getItem(position), true);
                }
            }
            if(mPagerAdapter.getWeekFragment() != null) {
                if(mPagerAdapter.getWeekFragment().getStudent().getStudentId().equals(mCarouselAdapter.getItem(position).getStudentId())) {
                    mPagerAdapter.getWeekFragment().refreshWithStudent(mCarouselAdapter.getItem(position), false);
                } else {
                    mPagerAdapter.getWeekFragment().refreshWithStudent(mCarouselAdapter.getItem(position), true);
                }
            }
            if(mPagerAdapter.getAlertFragment() != null) {
                if(mPagerAdapter.getAlertFragment().getStudent().getStudentId().equals(mCarouselAdapter.getItem(position).getStudentId())) {
                    mPagerAdapter.getAlertFragment().refreshWithStudent(mCarouselAdapter.getItem(position), false);
                } else {
                    mPagerAdapter.getAlertFragment().refreshWithStudent(mCarouselAdapter.getItem(position), true);
                }
            }
        } else {
            mCarouselViewPager.setCurrentItem(position);
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if(state == ViewPager.SCROLL_STATE_SETTLING) {
            mIsSettling = true;
        } else if (state == ViewPager.SCROLL_STATE_DRAGGING) {
            mIsDragging = true;
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        ArgbEvaluator evaluator = new ArgbEvaluator();
        final View rootView = findViewById(R.id.navigationWrapper);
        int colorPosition = position % 4;
        int currentColor;

        if(positionOffset < 0) return;

        if(colorPosition < 3) {
            //This will use colors 0 - 3
            final int topColor = (Integer) evaluator.evaluate(positionOffset, mTopColors[colorPosition], mTopColors[colorPosition + 1]);
            final int bottomColor = (Integer) evaluator.evaluate(positionOffset, mBottomColors[colorPosition], mBottomColors[colorPosition + 1]);
            LayerDrawable layerDrawable = createBackgroundLayers(rootView, topColor, bottomColor);
            rootView.setBackgroundDrawable(layerDrawable);
            setTabBackgroundColor(positionOffset, evaluator, colorPosition, colorPosition + 1);

            currentColor = (Integer) evaluator.evaluate(positionOffset, mBottomColorsNoAlpha[colorPosition], mBottomColorsNoAlpha[colorPosition + 1]);
            if(mPagerAdapter.getWeekFragment() != null) {
                mPagerAdapter.getWeekFragment().setWeekViewBackground(currentColor);
            }

        } else {
            //This will use colors 3 - 0, to wrap it around
            final int topColor = (Integer) evaluator.evaluate(positionOffset, mTopColors[colorPosition], mTopColors[0]);
            final int bottomColor = (Integer) evaluator.evaluate(positionOffset, mBottomColors[colorPosition], mBottomColors[0]);
            LayerDrawable layerDrawable = createBackgroundLayers(rootView, topColor, bottomColor);
            rootView.setBackgroundDrawable(layerDrawable);
            currentColor = (Integer) evaluator.evaluate(positionOffset, mBottomColorsNoAlpha[colorPosition], mBottomColorsNoAlpha[0]);
            if(mPagerAdapter.getWeekFragment() != null) {
                mPagerAdapter.getWeekFragment().setWeekViewBackground(currentColor);
            }
            setTabBackgroundColor(positionOffset, evaluator, colorPosition, 0);
        }

        //if the user has let go of the user carousel mIsSettling will be set to true, that's when we want to check
        //the current position and move it to the closest item in the view pager
        if(mIsSettling && mIsDragging) {
            if (positionOffset < 0.5) {
                // we're already on this position, we don't need to set anything
            } else if (positionOffset >= 0.5) {
                // If we're already on that position we don't want to go to it again
                if(mCarouselViewPager.getCurrentItem() != position + 1) {
                    onPageSelected(position + 1);
                }
            }

            mIsSettling = false;
            mIsDragging = false;
        }

        //save the current color
        Prefs prefs = new Prefs(this, com.instructure.parentapp.util.Const.CANVAS_PARENT_SP);
        prefs.save(Const.NEW_COLOR, currentColor);
    }

    @NonNull
    private LayerDrawable createBackgroundLayers(final View rootView, final int topColor, final int bottomColor) {
        ShapeDrawable.ShaderFactory sf = new ShapeDrawable.ShaderFactory() {
            @Override
            public Shader resize(int width, int height) {
                LinearGradient lg = new LinearGradient(0, 0, 0, rootView.getHeight(),
                        new int[] {
                                topColor,
                                bottomColor},
                        new float[] {
                                0, 1.0f },
                        Shader.TileMode.REPEAT);
                return lg;
            }
        };
        PaintDrawable p = new PaintDrawable();
        p.setShape(new RectShape());
        p.setShaderFactory(sf);

        Drawable layers[] = new Drawable[2];
        layers[0] = getResources().getDrawable(R.drawable.triangle_background);
        layers[1] = p;
        return new LayerDrawable(layers);
    }

    private void setTabBackgroundColor(float positionOffset, ArgbEvaluator evaluator, int firstColorPosition, int secondColorPosition) {
        try {
            Field field = TabLayout.class.getDeclaredField("mTabStrip");
            field.setAccessible(true);
            Object ob = field.get(mTabLayout);

            View tabView = ((LinearLayout)ob).getChildAt(mTabLayout.getSelectedTabPosition());
            GradientDrawable gd = new GradientDrawable(
                    GradientDrawable.Orientation.BOTTOM_TOP,
                    new int[] { (Integer) evaluator.evaluate(positionOffset, mBottomColorsNoAlpha[firstColorPosition], mBottomColorsNoAlpha[secondColorPosition]), 0x00000000});
            gd.setCornerRadius(0f);

            //set the current color for the loading view and pull to refresh views
            setSwipeRefreshColor((Integer) evaluator.evaluate(positionOffset, mBottomColorsNoAlpha[firstColorPosition], mBottomColorsNoAlpha[secondColorPosition]));

            //now we have the gradient drawable, we need to set the background of the stateListDrawable
            StateListDrawable states = new StateListDrawable();
            states.addState(new int[] {android.R.attr.state_selected},
                    gd);

            states.addState(new int[]{},
                    null);
            tabView.setBackgroundDrawable(states);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }  catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public Student getCurrentStudent(){
        if(mCarouselAdapter != null && mCarouselAdapter.getCount() > 0) {
            return mCarouselAdapter.getItem(mCarouselViewPager.getCurrentItem());
        }
        return new Student();
    }

    private String getUserFirstName(String name){
        //This will probably work... Could have some issues with weird names though.
        //keeping this here just in case we want to use something similar later
        //String[] nameArray = name.split("\\s+");
        //return nameArray[0];
        return name;
    }
}
