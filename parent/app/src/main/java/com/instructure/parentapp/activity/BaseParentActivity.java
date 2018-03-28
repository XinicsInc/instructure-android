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

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.managers.UserManager;
import com.instructure.canvasapi2.models.BlockedStudentResponse;
import com.instructure.canvasapi2.models.MismatchedRegionResponse;
import com.instructure.canvasapi2.models.RevokedTokenResponse;
import com.instructure.canvasapi2.models.Student;
import com.instructure.canvasapi2.utils.ApiPrefs;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.LinkHeaders;
import com.instructure.pandautils.activities.BaseActivity;
import com.instructure.pandautils.utils.Const;
import com.instructure.pandautils.utils.Prefs;
import com.instructure.parentapp.R;
import com.instructure.parentapp.asynctask.LogoutAsyncTask;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Response;

public class BaseParentActivity extends BaseActivity {


    public void handleError(int code, String error) {

        if (code == 418) {
            // Parse the message from the response body
            Gson gson = new Gson();
            JsonParser parser = new JsonParser();
            JsonElement mJson = parser.parse(error);

            RevokedTokenResponse revokedTokenResponse = gson.fromJson(mJson, RevokedTokenResponse.class);
            showRevokedTokenDialog(revokedTokenResponse, this);
        }

        if (code == 403) {
            // Parse the message from the response body
            Gson gson = new Gson();
            JsonParser parser = new JsonParser();
            JsonElement mJson = parser.parse(error);

            BlockedStudentResponse blockedStudentResponse = gson.fromJson(mJson, BlockedStudentResponse.class);
            if (blockedStudentResponse.code.equals("studentBlocked")) {
                Prefs prefs = new Prefs(this, getString(R.string.app_name_parent));
                String parentId = prefs.load(Const.ID, "");
                // We want to refresh cache so the main activity can load quickly with accurate information
                UserManager.getStudentsForParentAirwolf(ApiPrefs.getAirwolfDomain(), parentId, new StatusCallback<List<Student>>() {
                    @Override
                    public void onResponse(@NonNull Response<List<Student>> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                        if (response.body() != null && !response.body().isEmpty()) {
                            // They have students that they are observing, take them to that activity
                            startActivity(StudentViewActivity.createIntent(BaseParentActivity.this, response.body()));
                            overridePendingTransition(0, 0);
                            finish();

                        } else {
                            // Take the parent to the add user page.
                            FindSchoolActivity.Companion.createIntent(BaseParentActivity.this, true);
                            finish();
                        }
                    }
                });
            }
        }

        if (code == 451) {
            // Parse the message from the response body
            Gson gson = new Gson();
            JsonParser parser = new JsonParser();
            JsonElement mJson = parser.parse(error);

            MismatchedRegionResponse mismatchedRegionResponse = gson.fromJson(mJson, MismatchedRegionResponse.class);
            showMismatchedRegionDialog(mismatchedRegionResponse.getStudentRegion(), this);
        }
    }

    private void showRevokedTokenDialog(final RevokedTokenResponse response, final Context context) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.revokedTokenErrorTitle)
                .setMessage(getString(R.string.revokedTokenErrorContent, response.shortName))
                .setPositiveButton(R.string.removeStudent, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        removeStudent(ApiPrefs.getAirwolfDomain(), response.parentId, response.studentId, context);
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void showMismatchedRegionDialog(final String regionString, final Context context) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.unauthorizedRegion)
                .setMessage(getString(R.string.mismatchedRegionMessage, getReadableRegion(this, regionString)))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Prefs prefs = new Prefs(BaseParentActivity.this, getString(R.string.app_name_parent));
                        String parentId = prefs.load(Const.ID, "");
                        UserManager.getStudentsForParentAirwolf(ApiPrefs.getAirwolfDomain(), parentId, new StatusCallback<List<Student>>() {
                            @Override
                            public void onResponse(@NonNull Response<List<Student>> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                                if (response.body() != null && !response.body().isEmpty()) {
                                    // They have students that they are observing, take them to that activity
                                    startActivity(StudentViewActivity.createIntent(BaseParentActivity.this, response.body()));
                                    overridePendingTransition(0, 0);
                                    finish();

                                } else {
                                    // Log the user out
                                    new LogoutAsyncTask(BaseParentActivity.this, "").execute();
                                }
                            }
                        });
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void removeStudent(final String airwolfDomain, final String parentId, String studentId, final Context context) {

        UserManager.removeStudentAirwolf(airwolfDomain, parentId, studentId, new StatusCallback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Response<ResponseBody> response, @NonNull com.instructure.canvasapi2.utils.LinkHeaders linkHeaders, @NonNull ApiType type) {
                //Inform the user that the student has been removed
                Toast.makeText(context, context.getResources().getString(R.string.studentRemoved), Toast.LENGTH_SHORT).show();

                //We want to refresh cache so the main activity can load quickly with accurate information
                UserManager.getStudentsForParentAirwolf(airwolfDomain, parentId, new StatusCallback<List<Student>>() {
                    @Override
                    public void onResponse(@NonNull Response<List<Student>> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                        if (response.body() != null && !response.body().isEmpty()) {
                            //they have students that they are observing, take them to that activity
                            startActivity(StudentViewActivity.createIntent(BaseParentActivity.this, response.body()));
                            overridePendingTransition(0, 0);
                            finish();

                        } else {
                            //Take the parent to the add user page.
                            FindSchoolActivity.Companion.createIntent(BaseParentActivity.this, true);
                            finish();
                        }
                    }
                });
            }
        });
    }

    public static String getReadableRegion(Context context, String regionCode) {
        switch(regionCode) {
            case ("ca-central-1"):
                return context.getString(R.string.canada);
            case ("eu-central-1"):
                return context.getString(R.string.ireland);
            case ("eu-west-1"):
                return context.getString(R.string.germany);
            case ("ap-southeast-1"):
                return context.getString(R.string.singapore);
            case ("ap-southeast-2"):
                return context.getString(R.string.australia);
            case ("us-east-1"):
                return context.getString(R.string.theUnitedStates);
            default:
                return context.getString(R.string.theUnitedStates);
        }
    }

    @Override
    public void unBundle(@NonNull Bundle extras) {

    }

    @Override
    public boolean applyThemeAutomagically() {
        return false;
    }

}
