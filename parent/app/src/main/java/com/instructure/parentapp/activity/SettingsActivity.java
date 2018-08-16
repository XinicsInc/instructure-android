/*
 * Copyright (C) 2017 - present  Instructure, Inc.
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

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.managers.UserManager;
import com.instructure.canvasapi2.models.Student;
import com.instructure.canvasapi2.utils.ApiPrefs;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.LinkHeaders;
import com.instructure.pandarecycler.decorations.SpacesItemDecoration;
import com.instructure.pandautils.utils.Const;
import com.instructure.parentapp.R;
import com.instructure.parentapp.adapter.SettingsRecyclerAdapter;
import com.instructure.parentapp.asynctask.LogoutAsyncTask;
import com.instructure.parentapp.factorys.SettingsPresenterFactory;
import com.instructure.parentapp.holders.SettingsViewHolder;
import com.instructure.parentapp.interfaces.AdapterToFragmentCallback;
import com.instructure.parentapp.presenters.SettingsPresenter;
import com.instructure.parentapp.util.AnalyticUtils;
import com.instructure.parentapp.util.ApplicationManager;
import com.instructure.parentapp.util.RecyclerViewUtils;
import com.instructure.parentapp.util.ViewUtils;
import com.instructure.parentapp.view.EmptyPandaView;
import com.instructure.parentapp.viewinterface.SettingsView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import instructure.androidblueprint.PresenterFactory;
import instructure.androidblueprint.SyncActivity;
import okhttp3.ResponseBody;
import retrofit2.Response;

public class SettingsActivity extends SyncActivity<Student, SettingsPresenter, SettingsView, SettingsViewHolder, SettingsRecyclerAdapter>
        implements SettingsView {

    private SettingsRecyclerAdapter mRecyclerAdapter;

    //region Binding

    @BindView(R.id.swipeRefreshLayout) SwipeRefreshLayout mSwipeRefreshLayout;
    @BindView(R.id.emptyPandaView) EmptyPandaView mEmptyPandaView;
    @BindView(R.id.recyclerView) RecyclerView mRecyclerView;

    //endregion

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setResult(RESULT_CANCELED);
        //make the status bar dark blue
        ViewUtils.setStatusBarColor(this, ContextCompat.getColor(SettingsActivity.this, R.color.colorPrimary));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);
        setupViews();
    }

    private void setupViews() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_close_white);
        toolbar.setNavigationContentDescription(R.string.close);
        toolbar.setTitle(R.string.action_settings);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        toolbar.inflateMenu(R.menu.menu_settings);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.add_students:
                        AnalyticUtils.trackButtonPressed(AnalyticUtils.ADD_STUDENT);
                        Intent intent = FindSchoolActivity.Companion.createIntent(SettingsActivity.this, true, true);
                        startActivityForResult(intent, com.instructure.parentapp.util.Const.DOMAIN_PICKER_REQUEST_CODE);
                        return true;
                    case R.id.help:
                        AnalyticUtils.trackButtonPressed(AnalyticUtils.HELP);
                        startActivity(HelpActivity.createIntent(SettingsActivity.this));
                        return true;
                    case R.id.log_out:
                        logoutWarning();
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    public SettingsRecyclerAdapter getAdapter() {
        if(mRecyclerAdapter == null) {
            mRecyclerAdapter = new SettingsRecyclerAdapter(SettingsActivity.this, getPresenter(), new AdapterToFragmentCallback<Student>() {
                @Override
                public void onRowClicked(Student student, int position, boolean isOpenDetail) {
                    startActivityForResult(StudentDetailsActivity.createIntent(SettingsActivity.this, student), com.instructure.parentapp.util.Const.STUDENT_DETAILS_REQUEST_CODE);
                }
            });
        }
        return mRecyclerAdapter;
    }

    @Override
    public void onBackPressed() {
        //make the status bar dark blue
        ViewUtils.setStatusBarColor(this, getResources().getColor(R.color.colorPrimary));
        super.onBackPressed();
    }

    private void logoutWarning() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.logout_warning);
        builder.setPositiveButton(R.string.logout_yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                AnalyticUtils.trackButtonPressed(AnalyticUtils.LOG_OUT);
                new LogoutAsyncTask(SettingsActivity.this, "").execute();
                dialog.dismiss();
            }
        });

        builder.setNegativeButton(R.string.logout_no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        final AlertDialog logoutWarningDialog = builder.create();
        logoutWarningDialog.show();
    }

    public void removeStudent(final Student student) {
        AnalyticUtils.trackButtonPressed(AnalyticUtils.REMOVE_STUDENT);

        setResult(RESULT_OK);

        final ProgressDialog dialog = ProgressDialog.show(SettingsActivity.this, getString(R.string.removingStudent), "", true, false);
        dialog.show();

        //this api call removes student data from the db (like alerts info).
        UserManager.removeStudentAirwolf(
                ApiPrefs.getAirwolfDomain(),
                student.getParentId(),
                student.getStudentId(),
                new StatusCallback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Response<ResponseBody> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                        dialog.dismiss();
                        Toast.makeText(SettingsActivity.this, getString(R.string.studentRemoved), Toast.LENGTH_SHORT).show();

                        int adapterSizeBeforeRemove = getAdapter().size();
                        boolean removed = getAdapter().removeItem(student);
                        if(removed && adapterSizeBeforeRemove == 1) {
                            //Catches case for removing last student
                            finish();
                        }
                    }
                });
    }

    public void addStudent(final ArrayList<Student> students) {
        if (students != null && !students.isEmpty()) {
            setResult(RESULT_OK);
            getAdapter().addAll(students);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == com.instructure.parentapp.util.Const.DOMAIN_PICKER_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<Student> students = data.getParcelableArrayListExtra(Const.STUDENT);
            addStudent(students);
        } else if (requestCode == com.instructure.parentapp.util.Const.STUDENT_DETAILS_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            if(Intent.ACTION_DELETE.equals(data.getAction())) {
                Student student = data.getParcelableExtra(Const.STUDENT);
                removeStudent(student);
            }
        } else if (requestCode == com.instructure.parentapp.util.Const.STUDENT_LOGIN_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<Student> students = data.getParcelableArrayListExtra(Const.STUDENT);
            addStudent(students);
        }
    }

    public static Intent createIntent(Context context, String userName) {
        Intent intent = new Intent(context, SettingsActivity.class);
        intent.putExtra(Const.NAME, userName);
        return intent;
    }

    @NonNull
    @Override
    public String airwolfDomain() {
        return ApiPrefs.getAirwolfDomain();
    }

    @NonNull
    @Override
    public String parentId() {
        return ApplicationManager.getParentId(SettingsActivity.this);
    }

    @Override
    public void hasStudent(boolean hasStudent) {
        if(!hasStudent) {
            setResult(RESULT_OK);
            finish();
        }
    }

    //Sync

    @Override
    protected void onReadySetGo(SettingsPresenter presenter) {
        mRecyclerView.setAdapter(getAdapter());
        getPresenter().loadData(false);
    }

    @Override
    protected PresenterFactory<SettingsPresenter> getPresenterFactory() {
        return new SettingsPresenterFactory();
    }

    @Override
    protected void onPresenterPrepared(SettingsPresenter presenter) {
        mRecyclerView = RecyclerViewUtils.buildRecyclerView(SettingsActivity.this, getAdapter(),
                presenter, mSwipeRefreshLayout, mRecyclerView, mEmptyPandaView, getString(R.string.noCourses));
        mRecyclerView.addItemDecoration(new SpacesItemDecoration(SettingsActivity.this, R.dimen.med_padding));
        addSwipeToRefresh(mSwipeRefreshLayout);
        addPagination();
    }

    @NonNull
    @Override
    protected RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    @Override
    protected int perPageCount() {
        return ApiPrefs.getPerPageCount();
    }

    @Override
    public void onRefreshStarted() {
        mEmptyPandaView.setLoading();
    }

    @Override
    public void onRefreshFinished() {
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void checkIfEmpty() {
        RecyclerViewUtils.checkIfEmpty(mEmptyPandaView, mRecyclerView, mSwipeRefreshLayout, getAdapter(), getPresenter().isEmpty());
    }
}
