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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.managers.AlertThresholdManager;
import com.instructure.canvasapi2.models.Alert;
import com.instructure.canvasapi2.models.AlertThreshold;
import com.instructure.canvasapi2.models.Student;
import com.instructure.canvasapi2.utils.APIHelper;
import com.instructure.canvasapi2.utils.ApiPrefs;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.LinkHeaders;
import com.instructure.pandautils.utils.Const;
import com.instructure.parentapp.BuildConfig;
import com.instructure.parentapp.R;
import com.instructure.parentapp.dialogs.StudentThresholdDialog;
import com.instructure.parentapp.util.AnalyticUtils;
import com.instructure.parentapp.util.StringUtilities;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public class StudentDetailsActivity extends BaseParentActivity implements StudentThresholdDialog.StudentThresholdChanged {

    private static final int GRADE_ABOVE = 1;
    private static final int GRADE_BELOW = 2;
    private static final int ASSIGNMENT_GRADE_ABOVE = 3;
    private static final int ASSIGNMENT_GRADE_BELOW = 4;

    private TextView mGradeAbove;
    private TextView mGradeBelow;
    private TextView mAssignmentGradeAbove;
    private TextView mAssignmentGradeBelow;
    private SwitchCompat mAssignmentMissingSwitch;
    private SwitchCompat mTeacherAnnouncementsSwitch;
    private SwitchCompat mInstitutionAnnouncementsSwitch;

    private StatusCallback<List<AlertThreshold>> mAlertThresholdCallback;
    private ArrayList<AlertThreshold> mAlertThresholds = new ArrayList<>();

    private SwitchCompat.OnCheckedChangeListener mAssignmentMissingCheckedChangeListener;
    private SwitchCompat.OnCheckedChangeListener mTeacherAnnouncementsCheckedChangeListener;
    private SwitchCompat.OnCheckedChangeListener mInstitutionAnnouncementsCheckedChangeListener;

    private boolean mHasGradeAboveThreshold = false;
    private boolean mHasGradeBelowThreshold = false;
    private boolean mHasAssignmentMissingThreshold = false;
    private boolean mHasAssignmentGradeAboveThreshold = false;
    private boolean mHasAssignmentGradeBelowThreshold = false;
    private boolean mHasTeacherAnnouncementsThreshold = false;
    private boolean mHasInstitutionAnnouncementsThreshold = false;

    @Nullable
    private Student mStudent;

    public static Intent createIntent(Context context, Student student) {
        Intent intent = new Intent(context, StudentDetailsActivity.class);
        Bundle extras = new Bundle();
        extras.putParcelable(Const.STUDENT, student);
        intent.putExtras(extras);
        return intent;
    }

    private Student getStudent() {
        if(mStudent == null) {
            mStudent = getIntent().getExtras().getParcelable(Const.STUDENT);
        }
        return mStudent;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(Activity.RESULT_CANCELED);
        setContentView(R.layout.activity_student_details);
        setupCallbacks();
        setupViews();

        AlertThresholdManager.getAlertThresholdsForStudent(
                ApiPrefs.getAirwolfDomain(),
                getStudent().getParentId(),
                getStudent().getStudentId(),
                mAlertThresholdCallback
        );
    }

    private void setupViews() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_close_white);
        toolbar.setNavigationContentDescription(R.string.close);
        toolbar.setTitle(R.string.action_settings);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        toolbar.inflateMenu(R.menu.menu_student_details);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(item.getItemId() == R.id.remove_student) {
                    if(!BuildConfig.IS_ROBO_TEST) {
                        Intent intent = new Intent(Intent.ACTION_DELETE);
                        Bundle extras = new Bundle();
                        extras.putParcelable(Const.STUDENT, getStudent());
                        intent.putExtras(extras);
                        setResult(RESULT_OK, intent);
                        finish();
                        return true;
                    }
                }
                return false;
            }
        });

        CircleImageView avatar = (CircleImageView) findViewById(R.id.avatar);
        TextView studentName = (TextView) findViewById(R.id.studentName);

        Picasso.with(StudentDetailsActivity.this)
                .load(getStudent().getAvatarUrl())
                .placeholder(R.drawable.ic_cv_user)
                .error(R.drawable.ic_cv_user)
                .fit()
                .into(avatar);

        studentName.setText(getStudent().getStudentName());

        mGradeAbove = (TextView) findViewById(R.id.gradeAboveValue);
        mGradeBelow = (TextView) findViewById(R.id.gradeBelowValue);
        mAssignmentGradeAbove = (TextView) findViewById(R.id.assignmentGradeAboveValue);
        mAssignmentGradeBelow = (TextView) findViewById(R.id.assignmentGradeBelowValue);

        configureSwitches();
        configureListeners();
    }

    //region Threshold

    private String getThresholdIdByAlertType(Alert.ALERT_TYPE alertType) {
        for(AlertThreshold threshold : mAlertThresholds) {
            if(threshold.getAlertType().equals(Alert.alertTypeToAPIString(alertType))) {
                return threshold.getStringId();
            }
        }
        return "-1";
    }

    private void removeThresholdByAlertType(Alert.ALERT_TYPE alertType) {
        for(int i = 0; i < mAlertThresholds.size(); i++) {
            if(mAlertThresholds.get(i).getAlertType().equals(Alert.alertTypeToAPIString(alertType))) {
                mAlertThresholds.remove(i);
            }
        }
    }

    //endregion

    //region Configure

    private void configureListeners() {
        mGradeAbove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAlertThresholdDialog(GRADE_ABOVE,
                        getResources().getString(R.string.gradeAbove), mGradeAbove.getText().toString());
            }
        });

        mGradeBelow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAlertThresholdDialog(GRADE_BELOW,
                        getResources().getString(R.string.gradeBelow), mGradeBelow.getText().toString());
            }
        });

        mAssignmentGradeAbove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAlertThresholdDialog(ASSIGNMENT_GRADE_ABOVE,
                        getResources().getString(R.string.assignmentGradeAbove),
                        mAssignmentGradeAbove.getText().toString());
            }
        });

        mAssignmentGradeBelow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAlertThresholdDialog(ASSIGNMENT_GRADE_BELOW,
                        getResources().getString(R.string.assignmentGradeBelow),
                        mAssignmentGradeBelow.getText().toString());
            }
        });
    }

    private void configureSwitches() {
        mAssignmentMissingSwitch = (SwitchCompat) findViewById(R.id.assignmentMissingSwitch);
        mAssignmentMissingCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                //disable it to prevent users from hitting it multiple times
                mAssignmentMissingSwitch.setEnabled(false);

                //make the API call
                if(isChecked) {
                    //if it already exists, just update
                    if(mHasAssignmentMissingThreshold) {
                        updateAlertThreshold(Alert.ALERT_TYPE.ASSIGNMENT_MISSING);
                    } else {
                        //otherwise, create it
                        createThreshold(Alert.ALERT_TYPE.ASSIGNMENT_MISSING);
                    }
                } else {
                    //delete the threshold
                    deleteThreshold(Alert.ALERT_TYPE.ASSIGNMENT_MISSING);
                }
            }
        };

        mTeacherAnnouncementsSwitch = (SwitchCompat) findViewById(R.id.teacherAnnouncementsSwitch);
        mTeacherAnnouncementsCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                //disable it to prevent users from hitting it multiple times
                mTeacherAnnouncementsSwitch.setEnabled(false);
                //make the API call
                if(isChecked) {
                    //if it already exists, just update
                    if(mHasTeacherAnnouncementsThreshold) {
                        updateAlertThreshold(Alert.ALERT_TYPE.COURSE_ANNOUNCEMENT);
                    } else {
                        //otherwise, create it
                        createThreshold(Alert.ALERT_TYPE.COURSE_ANNOUNCEMENT);
                    }
                } else {
                    //delete the threshold
                    deleteThreshold(Alert.ALERT_TYPE.COURSE_ANNOUNCEMENT);
                }
            }
        };

        mInstitutionAnnouncementsSwitch = (SwitchCompat) findViewById(R.id.institutionAnnouncementsSwitch);
        mInstitutionAnnouncementsCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                //disable it to prevent users from hitting it multiple times
                mInstitutionAnnouncementsSwitch.setEnabled(false);

                //make the API call
                if(isChecked) {
                    //if it already exists, just update
                    if(mHasInstitutionAnnouncementsThreshold) {
                        updateAlertThreshold(Alert.ALERT_TYPE.INSTITUTION_ANNOUNCEMENT);
                    } else {
                        //otherwise, create it
                        createThreshold(Alert.ALERT_TYPE.INSTITUTION_ANNOUNCEMENT);
                    }
                } else {
                    //delete the threshold
                    deleteThreshold(Alert.ALERT_TYPE.INSTITUTION_ANNOUNCEMENT);
                }
            }
        };
    }

    //endregion

    private void showAlertThresholdDialog(final int thresholdType, String title, final String currentThreshold){
        //cleanup the threshold string
        final String threshold = getCleanThresholdValue(currentThreshold);

        StudentThresholdDialog.Companion.newInstance(title, threshold, thresholdType)
                .show(getSupportFragmentManager(), StudentThresholdDialog.class.getSimpleName());
    }

    private boolean isThresholdValid(String threshold, String comparedThreshold) {
        return (StringUtilities.isStringNumeric(threshold) && StringUtilities.isStringNumeric(getCleanThresholdValue(comparedThreshold)));
    }

    /**
     * Make sure that the lower grade threshold isn't above the higher threshold and vice versa
     * @return
     */
    private boolean isGradeThresholdValid(int thresholdType, String thresholdValue) {
        if(thresholdType == GRADE_ABOVE && mHasGradeBelowThreshold) {
            //make sure the threshold isn't less than the below threshold

            //first, make sure the thresholds are numeric, then we'll compare them
            if(isThresholdValid(thresholdValue, mGradeBelow.getText().toString())) {
                int currentThreshold = Integer.parseInt(thresholdValue);
                int belowThreshold = Integer.parseInt(getCleanThresholdValue(mGradeBelow.getText().toString()));

                if(currentThreshold <= belowThreshold) {
                    return false;
                } else {
                    return true;
                }
            }
            return false;
        } else if(thresholdType == GRADE_BELOW && mHasGradeAboveThreshold) {
            //first, make sure the thresholds are numeric, then we'll compare them
            if(isThresholdValid(thresholdValue, mGradeAbove.getText().toString())) {
                int currentThreshold = Integer.parseInt(thresholdValue);
                int aboveThreshold = Integer.parseInt(getCleanThresholdValue(mGradeAbove.getText().toString()));

                if(currentThreshold >= aboveThreshold) {
                    return false;
                } else {
                    return true;
                }
            }
            return false;
        }

        return true;
    }

    /**
     * Make sure that the lower assignment threshold isn't above the higher threshold and vice versa
     * @return
     */
    private boolean isAssignmentThresholdValid(int thresholdType, String thresholdValue) {
        if(thresholdType == ASSIGNMENT_GRADE_ABOVE && mHasAssignmentGradeBelowThreshold) {
            //make sure the threshold isn't less than the below threshold

            //first, make sure the thresholds are numeric, then we'll compare them
            if(isThresholdValid(thresholdValue, mAssignmentGradeBelow.getText().toString())) {
                int currentThreshold = Integer.parseInt(thresholdValue);
                int belowThreshold = Integer.parseInt(getCleanThresholdValue(mAssignmentGradeBelow.getText().toString()));

                if(currentThreshold <= belowThreshold) {
                    return false;
                } else {
                    return true;
                }
            }
            return false;
        } else if(thresholdType == ASSIGNMENT_GRADE_BELOW && mHasAssignmentGradeAboveThreshold) {
            //first, make sure the thresholds are numeric, then we'll compare them
            if(isThresholdValid(thresholdValue, mAssignmentGradeAbove.getText().toString())) {
                int currentThreshold = Integer.parseInt(thresholdValue);
                int aboveThreshold = Integer.parseInt(getCleanThresholdValue(mAssignmentGradeAbove.getText().toString()));

                if(currentThreshold >= aboveThreshold) {
                    return false;
                } else {
                    return true;
                }
            }
            return false;
        }

        return true;
    }

    @Override
    public void handlePositiveThreshold(int thresholdType, String threshold){
        switch(thresholdType) {
            case GRADE_ABOVE:
                if(!isGradeThresholdValid(thresholdType, threshold)){
                    Toast.makeText(StudentDetailsActivity.this, getString(R.string.course_threshold_grade_above_invalid), Toast.LENGTH_SHORT).show();
                    return;
                }
                if(mHasGradeAboveThreshold){
                    updateAlertThreshold(threshold, Alert.ALERT_TYPE.COURSE_GRADE_HIGH);
                } else {
                    createThreshold(threshold, Alert.ALERT_TYPE.COURSE_GRADE_HIGH);
                    mHasGradeAboveThreshold = true;
                }
                mGradeAbove.setText(threshold + "%");
                break;
            case GRADE_BELOW:
                if(!isGradeThresholdValid(thresholdType, threshold)){
                    Toast.makeText(StudentDetailsActivity.this, getString(R.string.course_threshold_grade_below_invalid), Toast.LENGTH_SHORT).show();
                    return;
                }
                if(mHasGradeBelowThreshold){
                    updateAlertThreshold(threshold, Alert.ALERT_TYPE.COURSE_GRADE_LOW);
                } else {
                    createThreshold(threshold, Alert.ALERT_TYPE.COURSE_GRADE_LOW);
                    mHasGradeBelowThreshold = true;
                }
                mGradeBelow.setText(threshold + "%");
                break;
            case ASSIGNMENT_GRADE_ABOVE:
                if(!isAssignmentThresholdValid(thresholdType, threshold)){
                    Toast.makeText(StudentDetailsActivity.this, getString(R.string.assignment_threshold_grade_above_invalid), Toast.LENGTH_SHORT).show();
                    return;
                }
                if(mHasAssignmentGradeAboveThreshold){
                    updateAlertThreshold(threshold, Alert.ALERT_TYPE.ASSIGNMENT_GRADE_HIGH);
                } else {
                    createThreshold(threshold, Alert.ALERT_TYPE.ASSIGNMENT_GRADE_HIGH);
                    mHasAssignmentGradeAboveThreshold = true;
                }
                mAssignmentGradeAbove.setText(threshold + "%");
                break;
            case ASSIGNMENT_GRADE_BELOW:
                if(!isAssignmentThresholdValid(thresholdType, threshold)){
                    Toast.makeText(StudentDetailsActivity.this, getString(R.string.assignment_threshold_grade_below_invalid), Toast.LENGTH_SHORT).show();
                    return;
                }
                if(mHasAssignmentGradeBelowThreshold){
                    updateAlertThreshold(threshold, Alert.ALERT_TYPE.ASSIGNMENT_GRADE_LOW);
                } else {
                    createThreshold(threshold, Alert.ALERT_TYPE.ASSIGNMENT_GRADE_LOW);
                    mHasAssignmentGradeBelowThreshold = true;
                }
                mAssignmentGradeBelow.setText(threshold + "%");
                break;
        }

    }

    @Override
    public void handleNeutralThreshold(int thresholdType){
        //delete the threshold
        switch(thresholdType) {
            case GRADE_ABOVE:
                mGradeAbove.setText(getResources().getString(R.string.never));
                deleteThreshold(Alert.ALERT_TYPE.COURSE_GRADE_HIGH);
                break;
            case GRADE_BELOW:
                mGradeBelow.setText(getResources().getString(R.string.never));
                deleteThreshold(Alert.ALERT_TYPE.COURSE_GRADE_LOW);
                break;
            case ASSIGNMENT_GRADE_ABOVE:
                mAssignmentGradeAbove.setText(getResources().getString(R.string.never));
                deleteThreshold(Alert.ALERT_TYPE.ASSIGNMENT_GRADE_HIGH);
                break;
            case ASSIGNMENT_GRADE_BELOW:
                mAssignmentGradeBelow.setText(getResources().getString(R.string.never));
                deleteThreshold(Alert.ALERT_TYPE.ASSIGNMENT_GRADE_LOW);
                break;
        }
    }

    private void deleteThreshold(final Alert.ALERT_TYPE alertType) {
        AnalyticUtils.trackButtonPressed(AnalyticUtils.MODIFY_THRESHOLD);

        String thresholdId = getThresholdIdByAlertType(alertType);
        if(!thresholdId.equals("-1")) {
            AlertThresholdManager.deleteAlertThreshold(
                    ApiPrefs.getAirwolfDomain(),
                    getStudent().getParentId(),
                    thresholdId,
                    new StatusCallback<ResponseBody>() {
                        @Override
                        public void onResponse(@NonNull Response<ResponseBody> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                            if (!APIHelper.isCachedResponse(response)) {
                                switch (alertType) {
                                    case COURSE_GRADE_LOW:
                                        mHasGradeBelowThreshold = false;
                                        break;
                                    case COURSE_GRADE_HIGH:
                                        mHasGradeAboveThreshold = false;
                                        break;
                                    case ASSIGNMENT_MISSING:
                                        mHasAssignmentMissingThreshold = false;
                                        break;
                                    case COURSE_ANNOUNCEMENT:
                                        mHasTeacherAnnouncementsThreshold = false;
                                        break;
                                    case INSTITUTION_ANNOUNCEMENT:
                                        mHasInstitutionAnnouncementsThreshold = false;
                                        break;
                                    case ASSIGNMENT_GRADE_HIGH:
                                        mHasAssignmentGradeAboveThreshold = false;
                                        break;
                                    case ASSIGNMENT_GRADE_LOW:
                                        mHasAssignmentGradeBelowThreshold = false;
                                        break;
                                }
                                enableSwitches(alertType);
                            }
                            removeThresholdByAlertType(alertType);
                        }

                        @Override
                        public void onFail(@Nullable Call<ResponseBody> call, @NonNull Throwable error, @Nullable Response response) {
                            enableSwitches(alertType);
                            Toast.makeText(StudentDetailsActivity.this, getString(R.string.errorOccurred), Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        }
    }

    private String getCleanThresholdValue(String currentThreshold) {
        if(!TextUtils.isEmpty(currentThreshold) && currentThreshold.contains("%")
                && currentThreshold.length() >= 2
                && currentThreshold.indexOf('%') == currentThreshold.length() - 1) {
            String cleanThreshold = currentThreshold.substring(0, currentThreshold.length() - 1);
            if(StringUtilities.isStringNumeric(cleanThreshold)){
                return cleanThreshold;
            } else {
                return "";
            }
        } else {
            return "";
        }
    }

    private void updateAlertThreshold(String newThreshold, Alert.ALERT_TYPE alertType) {
        AnalyticUtils.trackButtonPressed(AnalyticUtils.MODIFY_THRESHOLD);

        String thresholdId = getThresholdIdByAlertType(alertType);
        if(!thresholdId.equals("-1")) {
            AlertThresholdManager.updateAlertThreshold(
                    ApiPrefs.getAirwolfDomain(),
                    getStudent().getParentId(),
                    thresholdId,
                    Alert.alertTypeToAPIString(alertType),
                    newThreshold,
                    new StatusCallback<AlertThreshold>() {
                    });
        }
    }

    private void updateAlertThreshold(final Alert.ALERT_TYPE alertType) {
        AnalyticUtils.trackButtonPressed(AnalyticUtils.MODIFY_THRESHOLD);

        String thresholdId = getThresholdIdByAlertType(alertType);
        if(!thresholdId.equals("-1")) {
            AlertThresholdManager.updateAlertThreshold(
                    ApiPrefs.getAirwolfDomain(),
                    getStudent().getParentId(),
                    thresholdId,
                    Alert.alertTypeToAPIString(alertType),
                    new StatusCallback<AlertThreshold>() {
                        @Override
                        public void onResponse(@NonNull Response<AlertThreshold> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                            if (!APIHelper.isCachedResponse(response)) {
                                enableSwitches(alertType);
                            }
                        }

                        @Override
                        public void onFail(@Nullable Call<AlertThreshold> call, @NonNull Throwable error, @Nullable Response response) {
                            enableSwitches(alertType);
                            Toast.makeText(StudentDetailsActivity.this, getString(R.string.errorOccurred), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }



    private void createThreshold(final Alert.ALERT_TYPE alertType) {
        AnalyticUtils.trackButtonPressed(AnalyticUtils.MODIFY_THRESHOLD);

        AlertThresholdManager.createAlertThreshold(
                ApiPrefs.getAirwolfDomain(),
                getStudent().getParentId(),
                getStudent().getStudentId(),
                Alert.alertTypeToAPIString(alertType),
                new StatusCallback<AlertThreshold>() {
                    @Override
                    public void onResponse(@NonNull Response<AlertThreshold> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                        if (!APIHelper.isCachedResponse(response)) {
                            mAlertThresholds.add(response.body());
                            enableSwitches(alertType);
                        }
                    }

                    @Override
                    public void onFail(@Nullable Call<AlertThreshold> call, @NonNull Throwable error, @Nullable Response response) {
                        enableSwitches(alertType);
                        Toast.makeText(StudentDetailsActivity.this, getString(R.string.errorOccurred), Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void createThreshold(String threshold, Alert.ALERT_TYPE alertType) {
        AnalyticUtils.trackButtonPressed(AnalyticUtils.MODIFY_THRESHOLD);


        AlertThresholdManager.createAlertThreshold(
                ApiPrefs.getAirwolfDomain(),
                getStudent().getParentId(),
                getStudent().getStudentId(),
                Alert.alertTypeToAPIString(alertType),
                threshold,
                new StatusCallback<AlertThreshold>() {
                    @Override
                    public void onResponse(@NonNull Response<AlertThreshold> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                        mAlertThresholds.add(response.body());
                    }
                }
        );
    }

    /**
     * Switched get disabled when the user interacts with them until the api call is finished. This
     * prevents weird race condition issues and prevents them from spamming our servers
     *
     * @param alertType
     */
    public void enableSwitches(Alert.ALERT_TYPE alertType) {
        if(alertType == Alert.ALERT_TYPE.ASSIGNMENT_MISSING) {
            mAssignmentMissingSwitch.setEnabled(true);
        } else if(alertType == Alert.ALERT_TYPE.COURSE_ANNOUNCEMENT) {
            mTeacherAnnouncementsSwitch.setEnabled(true);
        } else if(alertType == Alert.ALERT_TYPE.INSTITUTION_ANNOUNCEMENT) {
            mInstitutionAnnouncementsSwitch.setEnabled(true);
        }
    }

    private void setupCallbacks() {
        mAlertThresholdCallback = new StatusCallback<List<AlertThreshold>>(){
            @Override
            public void onResponse(@NonNull Response<List<AlertThreshold>> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                mAlertThresholds.clear();
                mAlertThresholds.addAll(response.body());

                //turn off all the switches
                //set the listeners to be null to not trigger an API call
                mTeacherAnnouncementsSwitch.setOnCheckedChangeListener(null);
                mAssignmentMissingSwitch.setOnCheckedChangeListener(null);
                mInstitutionAnnouncementsSwitch.setOnCheckedChangeListener(null);


                mTeacherAnnouncementsSwitch.setChecked(false);
                mAssignmentMissingSwitch.setChecked(false);
                mInstitutionAnnouncementsSwitch.setChecked(false);

                //clear the edit texts so we don't put incorrect data in there
                mGradeAbove.setText(getString(R.string.never));
                mGradeBelow.setText(getString(R.string.never));
                mAssignmentGradeAbove.setText(getString(R.string.never));
                mAssignmentGradeBelow.setText(getString(R.string.never));

                //change the switches based on which thresholds have been set
                for(AlertThreshold threshold : response.body()) {
                    if(!TextUtils.isEmpty(threshold.getAlertType()) && Alert.getAlertTypeFromString(threshold.getAlertType()) != null) {
                        switch (Alert.getAlertTypeFromString(threshold.getAlertType())) {
                            case COURSE_GRADE_HIGH:
                                if (!TextUtils.isEmpty(threshold.getThreshold())) {
                                    mGradeAbove.setText(threshold.getThreshold() + "%");

                                    mHasGradeAboveThreshold = true;
                                }
                                break;
                            case COURSE_GRADE_LOW:
                                if (!TextUtils.isEmpty(threshold.getThreshold())) {
                                    mGradeBelow.setText(threshold.getThreshold() + "%");

                                    mHasGradeBelowThreshold = true;
                                }
                                break;
                            case COURSE_ANNOUNCEMENT:
                                mTeacherAnnouncementsSwitch.setChecked(true);

                                mHasTeacherAnnouncementsThreshold = true;
                                break;
                            case ASSIGNMENT_MISSING:
                                mAssignmentMissingSwitch.setChecked(true);

                                mHasAssignmentMissingThreshold = true;
                                break;
                            case ASSIGNMENT_GRADE_HIGH:
                                if (!TextUtils.isEmpty(threshold.getThreshold())) {
                                    mAssignmentGradeAbove.setText(threshold.getThreshold() + "%");

                                    mHasAssignmentGradeAboveThreshold = true;
                                }
                                break;
                            case ASSIGNMENT_GRADE_LOW:
                                if (!TextUtils.isEmpty(threshold.getThreshold())) {
                                    mAssignmentGradeBelow.setText(threshold.getThreshold() + "%");

                                    mHasAssignmentGradeBelowThreshold = true;
                                }
                                break;
                            case INSTITUTION_ANNOUNCEMENT:
                                mInstitutionAnnouncementsSwitch.setChecked(true);

                                mHasInstitutionAnnouncementsThreshold = true;
                                break;
                        }
                    }
                }

                //reset the listeners
                mTeacherAnnouncementsSwitch.setOnCheckedChangeListener(mTeacherAnnouncementsCheckedChangeListener);
                mAssignmentMissingSwitch.setOnCheckedChangeListener(mAssignmentMissingCheckedChangeListener);
                mInstitutionAnnouncementsSwitch.setOnCheckedChangeListener(mInstitutionAnnouncementsCheckedChangeListener);
            }
        };
    }

    @Override
    public void unBundle(@NonNull Bundle extras) {

    }

    @Override
    public boolean applyThemeAutomagically() {
        return false;
    }
}
