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

import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.widget.Toast;

import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.managers.AccountNotificationManager;
import com.instructure.canvasapi2.managers.AssignmentManager;
import com.instructure.canvasapi2.managers.CalendarEventManager;
import com.instructure.canvasapi2.managers.CourseManager;
import com.instructure.canvasapi2.managers.DiscussionManager;
import com.instructure.canvasapi2.managers.FileFolderManager;
import com.instructure.canvasapi2.models.AccountNotification;
import com.instructure.canvasapi2.models.Assignment;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.Course;
import com.instructure.canvasapi2.models.DiscussionTopicHeader;
import com.instructure.canvasapi2.models.FileFolder;
import com.instructure.canvasapi2.models.ScheduleItem;
import com.instructure.canvasapi2.models.Student;
import com.instructure.canvasapi2.utils.ApiPrefs;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.LinkHeaders;
import com.instructure.canvasapi2.utils.Logger;
import com.instructure.pandautils.loaders.OpenMediaAsyncTaskLoader;
import com.instructure.pandautils.utils.Const;
import com.instructure.parentapp.R;
import com.instructure.parentapp.fragments.AnnouncementFragment;
import com.instructure.parentapp.fragments.AssignmentFragment;
import com.instructure.parentapp.fragments.CourseSyllabusFragment;
import com.instructure.parentapp.fragments.CourseWeekFragment;
import com.instructure.parentapp.fragments.EventFragment;
import com.instructure.parentapp.fragments.InternalWebviewFragment;
import com.instructure.parentapp.util.ApplicationManager;
import com.instructure.parentapp.util.Param;
import com.instructure.parentapp.util.RouterUtils;
import com.instructure.parentapp.util.StringUtilities;

import retrofit2.Call;
import retrofit2.Response;

public abstract class BaseRouterActivity extends BaseParentActivity {

    // region OpenMediaAsyncTaskLoader
    private Bundle openMediaBundle;
    private LoaderManager.LoaderCallbacks<OpenMediaAsyncTaskLoader.LoadedMedia> openMediaCallbacks;
    private ProgressDialog progressDialog;
    // endregion

    /**
     * Handles the Route based
     * Use RouterUtils.canRouteInternally()
     * @param route
     */
    public void handleRoute(RouterUtils.Route route, @Nullable Student student) {
        try {
            //currently all of our routes contain the course
            if (route.getParamsHash().containsKey(Param.COURSE_ID)) {
                long courseId = Long.parseLong(route.getParamsHash().get(Param.COURSE_ID));


                if (route.getContextType() == CanvasContext.Type.COURSE) {
                    getCourseForRouting(courseId, route, student);
                }
                return; // do not remove return
            } else {
                if(route.getRouteType() == RouterUtils.ROUTE_TYPE.FILE_DOWNLOAD) {
                    //Work around for attachments in announcements, as they can have no
                    //associated course id
                    Toast.makeText(BaseRouterActivity.this, getString(R.string.cannot_view_file), Toast.LENGTH_SHORT).show();
                    return;
                } else if(route.getRouteType() == RouterUtils.ROUTE_TYPE.ACCOUNT_NOTIFICATION) {
                    routeToAccountNotification(route, student);
                }
            }
        } catch (Exception e) {
            Logger.e("Could not parse and route url in BaseRouterActivity");
        }
    }

    /**
     * The intent will have information about the url to open (usually from clicking on a link in an email)
     * @param intent
     */
    private void parse(Intent intent) {
        if(intent == null) {
            return;
        }
        if(intent.getExtras() == null) {
            return;
        }

        final Bundle extras = intent.getExtras();
        Logger.logBundle(extras);

        if(extras.containsKey(Const.PARSE)) {
            final String url = extras.getString(Const.URL);
            RouterUtils.routeUrl(this, url, null, "", false);
        } else if(extras.containsKey(Const.BOOKMARK)) {
            final String url = extras.getString(Const.URL);
            RouterUtils.routeUrl(this, url, null, "", false);
        }
    }

    private void routeToFragment(final CanvasContext canvasContext, final Student student, final RouterUtils.Route route) {
        if(student == null) {
            //Error here, warn user, don't route
            Toast.makeText(this, getString(R.string.errorOccurred), Toast.LENGTH_SHORT).show();
            return;
        }

        if(route.getRouteType() == RouterUtils.ROUTE_TYPE.FILE_DOWNLOAD) {
            //not sure which of these options we'll use for file downloading/viewing. We'll have to see when the API side is
            //finished on Airwolf. So for now, we're going to comment these out and give the user a message
            Toast.makeText(BaseRouterActivity.this, getString(R.string.cannot_view_file), Toast.LENGTH_SHORT).show();
            return;
        }

        //check if it's the assignment route
        if(route.getFragCls().equals(AssignmentFragment.class)) {
            //the params has should have the assignment id, and we have the course id at this point
            long assignmentId = Long.parseLong(route.getParamsHash().get("assignment_id"));

            AssignmentManager.getAssignmentAirwolf(
                    ApiPrefs.getAirwolfDomain(),
                    student.getParentId(),
                    student.getStudentId(),
                    Long.toString(canvasContext.getId()),
                    Long.toString(assignmentId),
                    new StatusCallback<Assignment>() {
                        @Override
                        public void onResponse(@NonNull Response<Assignment> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                            assignmentHelper(response.body());
                        }

                        private void assignmentHelper(Assignment assignment) {
                            if (assignment != null) {
                                startActivity(DetailViewActivity.createIntent(BaseRouterActivity.this, DetailViewActivity.DETAIL_FRAGMENT.ASSIGNMENT, assignment, canvasContext.getName(), student));
                                overridePendingTransition(R.anim.slide_from_bottom, android.R.anim.fade_out);
                            }
                        }

                        @Override
                        public void onFail(@Nullable Call<Assignment> call, @NonNull Throwable error, @Nullable Response response) {
                            if (response != null && response.code() == 404) {
                                Toast.makeText(BaseRouterActivity.this, R.string.could_not_route_assignment, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
            );

        } else if(route.getFragCls().equals(AnnouncementFragment.class)) {

            long announcementId = Long.parseLong(route.getParamsHash().get("announcement_id"));

            DiscussionManager.getDetailedDiscussionAirwolf(
                    ApiPrefs.getAirwolfDomain(),
                    student.getParentId(),
                    student.getStudentId(),
                    Long.toString(canvasContext.getId()),
                    Long.toString(announcementId),
                    new StatusCallback<DiscussionTopicHeader>() {
                        @Override
                        public void onResponse(@NonNull Response<DiscussionTopicHeader> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                            startActivity(DetailViewActivity.createIntent(BaseRouterActivity.this, DetailViewActivity.DETAIL_FRAGMENT.ANNOUNCEMENT, response.body(), canvasContext.getName(), student));
                            overridePendingTransition(R.anim.slide_from_bottom, android.R.anim.fade_out);
                        }

                        @Override
                        public void onFail(@Nullable Call<DiscussionTopicHeader> call, @NonNull Throwable error, @Nullable Response response) {
                            Toast.makeText(BaseRouterActivity.this, R.string.errorOccurred, Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        } else if(route.getFragCls().equals(EventFragment.class)) {

            long eventId = Long.parseLong(route.getParamsHash().get("event_id"));

            CalendarEventManager.getCalendarEventAirwolf(
                    ApiPrefs.getAirwolfDomain(),
                    student.getParentId(),
                    student.getStudentId(),
                    Long.toString(eventId),
                    new StatusCallback<ScheduleItem>() {
                        @Override
                        public void onResponse(@NonNull Response<ScheduleItem> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                            startActivity(DetailViewActivity.createIntent(BaseRouterActivity.this, DetailViewActivity.DETAIL_FRAGMENT.EVENT, response.body(), student));
                            overridePendingTransition(R.anim.slide_from_bottom, android.R.anim.fade_out);
                        }

                        @Override
                        public void onFail(@Nullable Call<ScheduleItem> call, @NonNull Throwable error, @Nullable Response response) {
                            Toast.makeText(BaseRouterActivity.this, R.string.errorOccurred, Toast.LENGTH_SHORT).show();
                        }
                    }
            );

        } else if(route.getFragCls().equals(CourseWeekFragment.class)) {
            startActivity(DetailViewActivity.createIntent(BaseRouterActivity.this, DetailViewActivity.DETAIL_FRAGMENT.WEEK, student, (Course)canvasContext));
        } else if (route.getFragCls().equals(CourseSyllabusFragment.class)) {
            //we have the course and the student, just go to the syllabus fragment
            if(this instanceof DetailViewActivity) {
                ((DetailViewActivity)this).addFragment(CourseSyllabusFragment.newInstance((Course)canvasContext,student), false);

            } else {
                startActivity(DetailViewActivity.createIntent(BaseRouterActivity.this, DetailViewActivity.DETAIL_FRAGMENT.SYLLABUS, student, (Course)canvasContext));
                overridePendingTransition(R.anim.slide_from_bottom, android.R.anim.fade_out);
            }
        }
    }


    private void routeToAccountNotification(RouterUtils.Route route, final Student student) {
        String notificationId = route.getParamsHash().get("notification_id");

        AccountNotificationManager.getAccountNotificationForStudentAirwolf(
                ApiPrefs.getAirwolfDomain(),
                ApplicationManager.getParentId(BaseRouterActivity.this),
                student.getStudentId(),
                notificationId,
                true,
                new StatusCallback<AccountNotification>() {

                    @Override
                    public void onResponse(@NonNull Response<AccountNotification> accountNotificationResponse, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                        if (accountNotificationResponse.body() != null) {
                            startActivity(DetailViewActivity.createIntent(BaseRouterActivity.this, DetailViewActivity.DETAIL_FRAGMENT.ACCOUNT_NOTIFICATION, accountNotificationResponse.body(), student));
                            overridePendingTransition(R.anim.slide_from_bottom, android.R.anim.fade_out);
                        }
                    }
                });
    }

    private void getCourseForRouting(long id, final RouterUtils.Route route, final Student student) {
        CourseManager.getCourseWithGradeAirwolf(
                ApiPrefs.getAirwolfDomain(),
                ApplicationManager.getParentId(BaseRouterActivity.this),
                student.getStudentId(),
                id,
                new StatusCallback<Course>() {
                    @Override
                    public void onResponse(@NonNull Response<Course> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                        if (response.body() == null) {
                            Toast.makeText(BaseRouterActivity.this, getString(R.string.could_not_route_course), Toast.LENGTH_SHORT).show();
                        } else {
                            routeToFragment(response.body(), student, route);
                        }
                    }
                }
        );
    }

    private LoaderManager.LoaderCallbacks<OpenMediaAsyncTaskLoader.LoadedMedia> getLoaderCallbacks() {
        if (openMediaCallbacks == null) {
            openMediaCallbacks = new LoaderManager.LoaderCallbacks<OpenMediaAsyncTaskLoader.LoadedMedia>() {
                @Override
                public Loader<OpenMediaAsyncTaskLoader.LoadedMedia> onCreateLoader(int id, Bundle args) {
                    showProgressDialog();
                    return new OpenMediaAsyncTaskLoader(BaseRouterActivity.this, args);
                }

                @Override
                public void onLoadFinished(Loader<OpenMediaAsyncTaskLoader.LoadedMedia> loader, OpenMediaAsyncTaskLoader.LoadedMedia loadedMedia) {
                    dismissProgressDialog();

                    try {
                        if (loadedMedia.isError()) {
                            Toast.makeText(BaseRouterActivity.this, getString(loadedMedia.getErrorMessage()), Toast.LENGTH_LONG).show();
                        } else if (loadedMedia.isHtmlFile()) {
                            //create and add the InternalWebviewFragment to deal with the link they clicked
                            InternalWebviewFragment internalWebviewFragment = new InternalWebviewFragment();
                            internalWebviewFragment.setArguments(loadedMedia.getBundle());

                            FragmentTransaction ft = BaseRouterActivity.this.getSupportFragmentManager().beginTransaction();
                            ft.setCustomAnimations(R.anim.slide_from_bottom, android.R.anim.fade_out, R.anim.none, R.anim.slide_to_bottom);
                            ft.add(R.id.fullscreen, internalWebviewFragment, internalWebviewFragment.getClass().getName());
                            ft.addToBackStack(internalWebviewFragment.getClass().getName());
                            ft.commitAllowingStateLoss();
                        } else if (loadedMedia.getIntent() != null) {
                            BaseRouterActivity.this.startActivity(loadedMedia.getIntent());
                        }
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(BaseRouterActivity.this, R.string.noApps, Toast.LENGTH_LONG).show();
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

    public static void downloadMedia(Context context, String url, String filenameForDownload, String downloadDescription) {

        //if downloadDescription is empty we can set the description as filename
        if(StringUtilities.isEmpty(url, filenameForDownload)){
            //let the user know something went wrong
            Toast.makeText(context, R.string.unexpectedErrorDownloadingFile, Toast.LENGTH_SHORT).show();
            return;
        }
        DownloadManager.Request request;

        //Some older phones don't support https downloading... 3.1 and older do I believe...
        try {
            request = new DownloadManager.Request(Uri.parse(url));
        } catch(Exception e) {
            // certain urls are crashing here. So temporarily we have this extra try/catch to log more
            // information so we can fix it
            try {
                request = new DownloadManager.Request(Uri.parse(url.replaceFirst("https://", "http://")));
            } catch (Exception e2) {
                Toast.makeText(context, R.string.unexpectedErrorDownloadingFile, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if(!TextUtils.isEmpty(downloadDescription)) {
            request.setDescription(downloadDescription);
        }
        else {
            request.setDescription(filenameForDownload);

        }
        request.setTitle(filenameForDownload);
        // in order for this if to run, you must use the android 3.2 to compile your app
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        }
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,filenameForDownload);

        // get download service and enqueue file
        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(request);
    }

    // ProgressDialog
    private void initProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(true);
        progressDialog.setMessage(getString(R.string.opening));
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                dismissProgressDialog();
                openMediaBundle = null; // set to null, otherwise the progressDialog will appear again
                BaseRouterActivity.this.getSupportLoaderManager().destroyLoader(R.id.openMediaLoaderID);
            }
        });
        progressDialog.setCanceledOnTouchOutside(true);
    }

    public void showProgressDialog() {
        if (progressDialog == null) {
            initProgressDialog();
        }
        progressDialog.show();
    }

    public void dismissProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    private void handleSpecificFile(long courseId, String fileID) {
        final CanvasContext canvasContext = CanvasContext.getGenericContext(CanvasContext.Type.COURSE, courseId, "");
        Logger.d("handleSpecificFile()");

        FileFolderManager.getFileFolderFromURLAirwolf(
                ApiPrefs.getAirwolfDomain(),
                "files/" + fileID,
                new StatusCallback<FileFolder>() {
                    @Override
                    public void onResponse(@NonNull Response<FileFolder> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                        if (type == ApiType.API) {
                            FileFolder fileFolder = response.body();
                            if (fileFolder == null || response.code() == 404) {
                                Toast.makeText(BaseRouterActivity.this, R.string.fileNoLongerExists, Toast.LENGTH_LONG).show();
                            } else {
                                if (fileFolder.isLocked() || fileFolder.isLockedForUser()) {
                                    Toast.makeText(BaseRouterActivity.this, String.format(getString(R.string.fileLocked), (fileFolder.getDisplayName() == null) ? getString(R.string.file) : fileFolder.getDisplayName()), Toast.LENGTH_LONG).show();
                                } else {
                                    downloadMedia(BaseRouterActivity.this, fileFolder.getContentType(), fileFolder.getUrl(), fileFolder.getDisplayName());
                                }
                            }
                        }
                    }

                    @Override
                    public void onFail(@Nullable Call<FileFolder> call, @NonNull Throwable error, @Nullable Response response) {
                        if (response != null && response.code() == 404) {
                            Toast.makeText(BaseRouterActivity.this, R.string.fileNoLongerExists, Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );
    }

    @Override
    public void unBundle(@NonNull Bundle extras) {

    }

    @Override
    public boolean applyThemeAutomagically() {
        return false;
    }
}
