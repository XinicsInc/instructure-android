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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.instructure.candroid.R;
import com.instructure.interactions.Navigation;
import com.instructure.interactions.FragmentInteractions;
import com.instructure.candroid.util.AppManager;
import com.instructure.candroid.util.DownloadMedia;
import com.instructure.candroid.util.LoggingUtility;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.Submission;
import com.instructure.pandautils.utils.Const;
import com.instructure.pandautils.utils.PandaViewUtils;
import com.instructure.pandautils.utils.PermissionUtils;
import com.instructure.pandautils.utils.ViewStyler;

import java.io.InputStream;
import java.net.URL;

public class SubmissionViewOnlineURLFragment extends ParentFragment {

    //logic variables
    private Submission submission;
    
    //view variables
    private Toolbar toolbar;
    private ImageView previewImage;
    private Button urlButton;
    private View loadingView;
    private TextView submittedImage;

    @Override
    @NonNull
    public FragmentInteractions.Placement getFragmentPlacement() {return FragmentInteractions.Placement.DETAIL; }

    @Override
    @NonNull
    public String title() {
        return getString(R.string.urlSubmission);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = getLayoutInflater().inflate(R.layout.fragment_submission_view_online_url, container, false);

        setupViews(rootView);      
        
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        populateViews();

        if(submission.getAttachments().size() > 0) {
            //get the image from the server and display it
            DownloadImageTask downloadImage = new DownloadImageTask(previewImage);
            downloadImage.execute(submission.getAttachments().get(0).getUrl());
        } else {
            // the image snapshot isn't there (yet, or possibly ever), change the description to match the web
            submittedImage.setText(R.string.urlSubmissionNoPreview);
        }
    }

    @Override
    public void applyTheme() {
        setupToolbarMenu(toolbar);
        toolbar.setTitle(title());
        PandaViewUtils.setupToolbarBackButton(toolbar, this);
        ViewStyler.themeToolbar(getActivity(), toolbar, getCanvasContext());
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        menu.add(getResources().getString(R.string.open));

        if(AppManager.isDownloadManagerAvailable(getActivity()))
            menu.add(getResources().getString(R.string.download));

        //I'm not sure why, but on tablets onContextItemSelected wasn't getting called. It looks like it's basically the
        //same as the files fragment, which works. The main difference is that this is registering the context menu on
        //an image instead of a listview. This is kinda hacky, but it works.
        MenuItem.OnMenuItemClickListener listener = new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                onContextItemSelected(item);
                return true;
            }
        };

        for (int i = 0, n = menu.size(); i < n; i++)
            menu.getItem(i).setOnMenuItemClickListener(listener);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        if (item.getTitle().equals(getResources().getString(R.string.open))) {
            //Open media
            openMedia(submission.getAttachments().get(0).getContentType(), submission.getAttachments().get(0).getUrl(), submission.getAttachments().get(0).getDisplayName());
        } else if (item.getTitle().equals(getResources().getString(R.string.download))) {
            if (PermissionUtils.hasPermissions(getActivity(), PermissionUtils.WRITE_EXTERNAL_STORAGE)) {
                downloadFile();
            } else {
                requestPermissions(PermissionUtils.makeArray(PermissionUtils.WRITE_EXTERNAL_STORAGE), PermissionUtils.WRITE_FILE_PERMISSION_REQUEST_CODE);
            }
        }
        return true;
    }

    private void downloadFile() {
        //Download media
        DownloadMedia.downloadMedia(getActivity(), submission.getAttachments().get(0).getUrl(), submission.getAttachments().get(0).getFilename(), submission.getAttachments().get(0).getDisplayName());
    }

    private void setupViews(View rootView) {
        toolbar = rootView.findViewById(R.id.toolbar);
        previewImage = (ImageView)rootView.findViewById(R.id.previewImage);
        urlButton = (Button) rootView.findViewById(R.id.urlButton);
        loadingView = rootView.findViewById(R.id.loadingLayout);
        loadingView.setVisibility(View.GONE);
        submittedImage = rootView.findViewById(R.id.submittedImage);

        //allow long presses to show context menu
        registerForContextMenu(previewImage);
    }
    private void populateViews() {
        urlButton.setText(getString(R.string.visitPage) + " " + submission.getUrl());
        setupListeners();
    }

    private void setupListeners() {
        urlButton.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                InternalWebviewFragment.Companion.loadInternalWebView(getActivity(), ((Navigation)getActivity()), InternalWebviewFragment.Companion.createBundle(getCanvasContext(), submission.getUrl(), false));
            }
        });

    }

    ///////////////////////////////////////////////////////////////////////////
    // Helpers
    ///////////////////////////////////////////////////////////////////////////
    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        return inSampleSize;
    }


    /////////////////////////////////////////////////////////////////////////// 
    // AsyncTasks 
    ///////////////////////////////////////////////////////////////////////////

    //this will download the screen capture of the website that was submitted and set it to 
    //be the image view that is passed into the constructor 
    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //start indeterminate progress circle
            Handler mHandler = new Handler();
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    loadingView.setVisibility(View.VISIBLE);
                }
            });
        }
        @Override
        protected Bitmap doInBackground(String... urls) {
            //get the url that we need to get
            String urlDisplay = urls[0];
            Bitmap bitmap = null;
            try {
                URL bitmapURL = new URL(urlDisplay);

                //if the OS version is newer than 10 we can use the BitmapRegionDecoder, otherwise
                //we'll have to downsample the image so that we don't get out of memory exceptions
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
                    //don't decode the image, but get the information about it
                    InputStream in = (InputStream)bitmapURL.getContent();
                    final BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeStream(in, null, options);

                    //just decode the top 700 pixels, but use the actual width
                    BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance((InputStream)bitmapURL.getContent(), false);
                    bitmap = decoder.decodeRegion(new Rect(0, 0, options.outWidth, 700), null);
                }
                else {
                    InputStream in = (InputStream)bitmapURL.getContent();
                    final BitmapFactory.Options options2 = new BitmapFactory.Options();
                    options2.inJustDecodeBounds = true;
                    BitmapFactory.decodeStream(in, null, options2);
                    //subsample original image
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 4;

                    in = (InputStream)bitmapURL.getContent();
                    bitmap = BitmapFactory.decodeStream(in, null, options);

                    in.close();
                }
            } catch (Exception e) {
                LoggingUtility.LogException(getActivity(), e);
            }
   
            return bitmap;
        }
        @Override
        protected void onPostExecute(Bitmap result) {
            //stop indeterminate progress indicator
            Handler mHandler = new Handler();
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    loadingView.setVisibility(View.GONE);
                }
            });
            //set the imageview
            bmImage.setImageBitmap(result);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == PermissionUtils.WRITE_FILE_PERMISSION_REQUEST_CODE) {
            if(PermissionUtils.permissionGranted(permissions, grantResults, PermissionUtils.WRITE_EXTERNAL_STORAGE)) {
                downloadFile();
            }
        }
    }

    @Override
    public void handleIntentExtras(Bundle extras) {
        super.handleIntentExtras(extras);
        submission = extras.getParcelable(Const.SUBMISSION_GRADE);
    }

    public static Bundle createBundle(CanvasContext canvasContext, Submission submission) {
        Bundle extras = createBundle(canvasContext);
        extras.putParcelable(Const.SUBMISSION_GRADE, submission);
        return extras;
    }

    @Override
    public boolean allowBookmarking() {
        return true;
    }
}
