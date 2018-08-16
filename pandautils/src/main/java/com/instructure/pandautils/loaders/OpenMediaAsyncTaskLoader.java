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

package com.instructure.pandautils.loaders;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;

import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.utils.HttpHelper;
import com.instructure.pandautils.R;
import com.instructure.pandautils.utils.Const;
import com.instructure.pandautils.utils.FileUploadUtils;
import com.instructure.pandautils.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenMediaAsyncTaskLoader extends AsyncTaskLoader<OpenMediaAsyncTaskLoader.LoadedMedia> {
    public enum ERROR_TYPE { NO_APPS, UNKNOWN }
    public class LoadedMedia {
        //try to open a MediaComment or attachment
        private ERROR_TYPE errorType = ERROR_TYPE.UNKNOWN;
        private int errorMessage;
        private boolean isError;
        private boolean isHtmlFile;
        private boolean isUseOutsideApps;

        // Used to identify when we don't want to show annotations/etc for pspdfkit
        private boolean isSubmission;

        private Intent intent;
        private Bundle bundle; // Used for html files

        public LoadedMedia() {

        }

        // region Getter & setter
        public boolean isHtmlFile() {
            return isHtmlFile;
        }

        public Bundle getBundle() {
            return bundle;
        }

        public void setBundle(Bundle bundle) {
            isHtmlFile = true;
            this.bundle = bundle;
        }

        public void setHtmlFile(boolean htmlFile) {
            isHtmlFile = htmlFile;
        }

        public Intent getIntent() {
            return intent;
        }

        public void setIntent(Intent intent) {
            this.intent = intent;
        }

        public boolean isUseOutsideApps() {return isUseOutsideApps;}

        public void setUseOutsideApps(boolean isUseOutsideApps) {
            this.isUseOutsideApps = isUseOutsideApps;
        }

        public boolean isError() {
            return isError;
        }

        public int getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(int errorMessage) {
            this.errorMessage = errorMessage;
            this.isError = true;
        }

        public boolean isSubmission() { return isSubmission; }

        public void setIsSubmission(boolean isSubmission) {
            this.isSubmission = isSubmission;
        }

        public ERROR_TYPE getErrorType() {
            return errorType;
        }
        // endregion

    }

    private String mimeType;
    private String url;
    private String filename;
    private boolean isSubmission;
    private CanvasContext canvasContext;
    private boolean isUseOutsideApps;

    private Context applicationContext;
    private PackageManager packageManager;


    private Boolean isHtmlFile;

    public OpenMediaAsyncTaskLoader(Context context, Bundle args) {
        super(context);
        if (args != null) {
            url = args.getString(Const.URL);
            isUseOutsideApps = args.getBoolean(Const.OPEN_OUTSIDE);
            if (args.containsKey(Const.MIME) && args.containsKey(Const.FILE_URL)) {
                mimeType = args.getString(Const.MIME);
                filename = args.getString(Const.FILE_URL);
                filename = makeFilenameUnique(filename, url);
            }
            if(args.containsKey(Const.IS_SUBMISSION)) {
                isSubmission = args.getBoolean(Const.IS_SUBMISSION);
            }
            canvasContext = args.getParcelable(Const.CANVAS_CONTEXT);
        }

        applicationContext = context.getApplicationContext();
        packageManager = applicationContext.getPackageManager();
    }

    @Override
    public LoadedMedia loadInBackground() {
        LoadedMedia loadedMedia = new LoadedMedia();
        if(this.isUseOutsideApps){
            loadedMedia.setUseOutsideApps(true);
        }

        if(this.isSubmission) {
            loadedMedia.setIsSubmission(true);
        }

        final Context context = getContext();
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.putExtra(Const.IS_MEDIA_TYPE, true);
            if (isHtmlFile() && canvasContext != null) {
                File file = downloadFile(context, url, filename);
                Bundle bundle = FileUploadUtils.createTaskLoaderBundle(canvasContext, FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + Const.FILE_PROVIDER_AUTHORITY, file).toString(), filename, false);
                loadedMedia.setBundle(bundle);
            } else if(isHtmlFile() && canvasContext == null) {
                //when the canvasContext is null we're routing from the teacher app, which just needs the url and title to get the html file
                Bundle bundle = new Bundle();
                bundle.putString(Const.INTERNAL_URL, url);
                bundle.putString(Const.ACTION_BAR_TITLE, filename);
                loadedMedia.setBundle(bundle);
            } else if (Utils.isAmazonDevice()) {
                attemptDownloadFile(context, intent, loadedMedia, url, filename);
            } else {
                loadedMedia.setHtmlFile(isHtmlFile());
                Uri uri = attemptConnection(url);
                if (uri != null) {
                    intent.setDataAndType(uri, mimeType);
                    loadedMedia.setIntent(intent);
                    Log.d(Const.OPEN_MEDIA_ASYNC_TASK_LOADER_LOG, "Intent can be handled: " + isIntentHandledByActivity(intent));
                    attemptDownloadFile(context, intent, loadedMedia, url, filename);
                } else {
                    loadedMedia.setErrorMessage(R.string.noDataConnection);
                }
            }
        } catch (MalformedURLException e) {
            Log.e(Const.OPEN_MEDIA_ASYNC_TASK_LOADER_LOG, "MalformedURLException: " + e.toString());
        } catch (IOException e) {
            Log.e(Const.OPEN_MEDIA_ASYNC_TASK_LOADER_LOG, "IOException: " + e.toString());
            loadedMedia.setErrorMessage(R.string.unexpectedErrorOpeningFile);
        } catch (ActivityNotFoundException e) {
            Log.e(Const.OPEN_MEDIA_ASYNC_TASK_LOADER_LOG, "ActivityNotFoundException: " + e.toString());
            loadedMedia.setErrorMessage(R.string.noApps);
        } catch (Exception e) {
            Log.e(Const.OPEN_MEDIA_ASYNC_TASK_LOADER_LOG, "Exception: " + e.toString());
            loadedMedia.setErrorMessage(R.string.unexpectedErrorOpeningFile);
        }
        return loadedMedia;
    }

    boolean isHtmlFile() {
        isHtmlFile = filename != null && (filename.toLowerCase().endsWith(".htm") || filename.toLowerCase().endsWith(".html"));

        return isHtmlFile;
    }

    public String getUrl() {
        return url;
    }

    public String getFilename() {
        return filename;
    }

    public static String parseFilename(String headerField) {
        String filename = headerField;
        Matcher matcher = Pattern.compile("filename=\"(.*)\"").matcher(headerField);
        if (matcher.find()) {
            filename = matcher.group(1);
        }
        return filename;
    }

    public static String makeFilenameUnique(String filename, String url) {
        Matcher matcher = Pattern.compile("(.*)\\.(.*)").matcher(filename);
        if (matcher.find()) {
            String actualFilename = matcher.group(1);
            String fileType = matcher.group(2);
            filename = String.format("%s_%s.%s", actualFilename, url.hashCode(), fileType);

        } else {
            filename = url.hashCode() + filename;
        }

        return filename;
    }

    /**
     *
     * @return uri if theres a connection, returns null otherwise
     * @throws Exception
     */
    private Uri attemptConnection(String url) throws IOException {
        Uri uri = null;
        final HttpURLConnection hc = (HttpURLConnection) new URL(url).openConnection();
        // alpha에서 정상적으로 파일을 다운받기 위해 헤더에 Referer를 추가해준다.
        hc.setRequestProperty("Referer", Utils.getRefererString(getContext()));
        final HttpURLConnection connection = HttpHelper.redirectURL(hc);
        // alpha에서 정상적으로 파일을 다운받기 위해 헤더에 Referer를 추가해준다.
        connection.setRequestProperty("Referer", Utils.getRefererString(getContext()));

        if(connection != null) {
            final String connectedUrl = connection.getURL().toString();
            // When only the url is specified in the bundle arguments, mimeType and filename are null or empty.
            if (TextUtils.isEmpty(mimeType)) {
                mimeType = connection.getContentType();
                if (mimeType == null) {
                    throw new IOException();
                }
            }
            Log.d(Const.OPEN_MEDIA_ASYNC_TASK_LOADER_LOG, "mimeType: " + mimeType);
            if (TextUtils.isEmpty(filename)) {
                // parse filename from Content-Disposition header which is a response field that is normally used to set the file name
                String headerField = connection.getHeaderField("Content-Disposition");
                if (headerField != null) {
                    filename = parseFilename(headerField);
                    filename = makeFilenameUnique(filename, url);
                } else {
                    filename = "" + url.hashCode();
                }
            }
            
            if(!TextUtils.isEmpty(connectedUrl)) {
                uri = Uri.parse(connectedUrl);
                if (mimeType.toLowerCase().equals("binary/octet-stream") || mimeType.toLowerCase().equals("*/*")) {
                    String guessedMimeType = URLConnection.guessContentTypeFromName(uri.getPath());
                    Log.d(Const.OPEN_MEDIA_ASYNC_TASK_LOADER_LOG, "guess mimeType: " + guessedMimeType);
                    if (!TextUtils.isEmpty(guessedMimeType)) {
                        mimeType = guessedMimeType;
                    }
                }
            }
        }
        return uri;
    }

    private File downloadFile(Context context, String url, String filename) throws Exception {
        //They have to download the content first... gross.
        //Download it if the file doesn't exist in the external cache....
        Log.d(Const.OPEN_MEDIA_ASYNC_TASK_LOADER_LOG, "downloadFile URL: " + url);
        File attachmentFile = new File(Utils.getAttachmentsDirectory(context), filename);
        Log.d(Const.OPEN_MEDIA_ASYNC_TASK_LOADER_LOG, "File: " + attachmentFile);
        if (!attachmentFile.exists()) {
            //Download the content from the url
            if (writeAttachmentsDirectoryFromURL(url, attachmentFile)) {
                Log.d(Const.OPEN_MEDIA_ASYNC_TASK_LOADER_LOG, "file not cached");
                return attachmentFile;
            }
        }
        return attachmentFile;
    }

    private boolean isIntentHandledByActivity(Intent intent) {
        ComponentName cn = intent.resolveActivity(packageManager);
        return cn != null;
    }

    private void attemptDownloadFile(Context context, Intent intent, LoadedMedia loadedMedia, String url, String filename) throws Exception {
        File file = downloadFile(context, url, filename);
        ContentResolver contentResolver = context.getContentResolver();
        Uri fileUri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + Const.FILE_PROVIDER_AUTHORITY, file);
        intent.setDataAndType(fileUri, contentResolver.getType(fileUri));

        if (!isIntentHandledByActivity(intent)) {
            loadedMedia.setErrorMessage(R.string.noApps);
            loadedMedia.errorType = ERROR_TYPE.NO_APPS;
        } else {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            loadedMedia.setIntent(intent);
        }
    }

    private boolean writeAttachmentsDirectoryFromURL(String url2, File toWriteTo) throws Exception {
        //create the new connection
        URL url = new URL(url2);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        // alpha에서 정상적으로 파일을 다운받기 위해 헤더에 Referer를 추가해준다.
        urlConnection.setRequestProperty("Referer", Utils.getRefererString(getContext()));
        //set up some things on the connection
        urlConnection.setRequestMethod("GET");

        //and connect!
        urlConnection.connect();
        HttpURLConnection connection = HttpHelper.redirectURL(urlConnection);
        // alpha에서 정상적으로 파일을 다운받기 위해 헤더에 Referer를 추가해준다.
        connection.setRequestProperty("Referer", Utils.getRefererString(getContext()));

        //this will be used to write the downloaded uri into the file we created
        String name = toWriteTo.getName();
        toWriteTo.getParentFile().mkdirs();
        FileOutputStream fileOutput = null;
        //if there is an external cache, we want to write to that
        if (applicationContext.getExternalCacheDir() != null) {
            fileOutput = new FileOutputStream(toWriteTo);
        }  else {            //otherwise, use internal cache.
            fileOutput = applicationContext.openFileOutput(name,
                    Activity.MODE_WORLD_READABLE | Activity.MODE_WORLD_WRITEABLE);
        }
        //this will be used in reading the uri from the internet
        InputStream inputStream = connection.getInputStream();

        //this is the total size of the file
        final int totalSize = connection.getContentLength();

        //create a buffer...
        byte[] buffer = new byte[1024];
        int bufferLength = 0; //used to store a temporary size of the buffer

        //now, read through the input buffer and write the contents to the file
        while ((bufferLength = inputStream.read(buffer)) != -1) {
            fileOutput.write(buffer, 0, bufferLength);
        }

        fileOutput.flush();
        fileOutput.close();
        inputStream.close(); // close after fileOuput.flush() or else the fileOutput won't actually close leading to open failed: EBUSY (Device or resource busy)

        return true;
    }

    public static Bundle createBundle(CanvasContext canvasContext, String mime, String url, String filename) {
        Bundle openMediaBundle = new Bundle();
        openMediaBundle.putString(Const.MIME, mime);
        openMediaBundle.putString(Const.URL, url);
        openMediaBundle.putString(Const.FILE_URL, filename);
        openMediaBundle.putParcelable(Const.CANVAS_CONTEXT, canvasContext);
        return openMediaBundle;
    }

    public static Bundle createBundle(CanvasContext canvasContext, String mime, String url, String filename, boolean useOutsideApps) {
        Bundle openMediaBundle = createBundle(canvasContext, mime, url, filename);
        openMediaBundle.putBoolean(Const.OPEN_OUTSIDE, useOutsideApps);
        return openMediaBundle;
    }

    public static Bundle createBundle(CanvasContext canvasContext, String url) {
        Bundle openMediaBundle = new Bundle();
        openMediaBundle.putString(Const.URL, url);
        openMediaBundle.putParcelable(Const.CANVAS_CONTEXT, canvasContext);
        return openMediaBundle;
    }

    public static Bundle createBundle(String url) {
        Bundle openMediaBundle = new Bundle();
        openMediaBundle.putString(Const.URL, url);
        return openMediaBundle;
    }

    public static Bundle createBundle(String mime, String url, String filename) {
        Bundle openMediaBundle = new Bundle();
        openMediaBundle.putString(Const.MIME, mime);
        openMediaBundle.putString(Const.URL, url);
        openMediaBundle.putString(Const.FILE_URL, filename);
        return openMediaBundle;
    }

    public static Bundle createBundle(CanvasContext canvasContext, boolean isSubmission, String mime, String url, String filename) {
        Bundle openMediaBundle = new Bundle();
        openMediaBundle.putString(Const.MIME, mime);
        openMediaBundle.putString(Const.URL, url);
        openMediaBundle.putString(Const.FILE_URL, filename);
        openMediaBundle.putParcelable(Const.CANVAS_CONTEXT, canvasContext);
        openMediaBundle.putBoolean(Const.IS_SUBMISSION, isSubmission);
        return openMediaBundle;
    }
}
