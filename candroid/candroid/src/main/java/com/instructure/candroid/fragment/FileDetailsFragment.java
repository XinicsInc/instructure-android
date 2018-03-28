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

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.instructure.candroid.R;
import com.instructure.interactions.FragmentInteractions;
import com.instructure.candroid.util.DownloadMedia;
import com.instructure.candroid.util.StringUtilities;
import com.instructure.candroid.view.ViewUtils;
import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.managers.FileFolderManager;
import com.instructure.canvasapi2.managers.ModuleManager;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.FileFolder;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.DateHelper;
import com.instructure.canvasapi2.utils.LinkHeaders;
import com.instructure.canvasapi2.utils.pageview.BeforePageView;
import com.instructure.canvasapi2.utils.pageview.PageView;
import com.instructure.canvasapi2.utils.pageview.PageViewUrlParam;
import com.instructure.canvasapi2.utils.pageview.PageViewUrlQuery;
import com.instructure.pandautils.utils.Const;
import com.instructure.pandautils.utils.FragmentExtensionsKt;
import com.instructure.pandautils.utils.PermissionUtils;
import com.instructure.pandautils.utils.ViewStyler;
import com.squareup.picasso.Picasso;

import java.util.Date;

import okhttp3.ResponseBody;
import retrofit2.Response;

@PageView(url = "{canvasContext}/files/{fileId}")
public class FileDetailsFragment extends ParentFragment {

    private Button openButton;
    private Button downloadButton;
    private TextView fileNameTextView;
    private TextView fileTypeTextView;
    private ImageView icon;
    private Toolbar toolbar;

    private long moduleId;
    private long itemId;

    private FileFolder file;
    private String fileUrl = "";

    private StatusCallback<FileFolder> fileFolderCanvasCallback;
    private StatusCallback<ResponseBody> markReadCanvasCallback;

    @PageViewUrlParam(name = "fileId")
    private long getFileId() {
        return file.getId();
    }

    @PageViewUrlQuery(name = "module_item_id")
    private Long getModuleItemId() {
        return FragmentExtensionsKt.getModuleItemId(this);
    }

    @BeforePageView
    private void setPageViewReady() {}

    @Override
    @NonNull
    public FragmentInteractions.Placement getFragmentPlacement() {
        return FragmentInteractions.Placement.DETAIL;
    }

    @Override
    @NonNull
    public String title() {
        return file != null && file.getLockInfo() == null ? file.getDisplayName() : getString(R.string.file);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_file_details, container, false);
        setupViews(rootView);
        return rootView;
    }

    private void setupViews(View rootView) {
        fileNameTextView = rootView.findViewById(R.id.fileName);
        fileTypeTextView = rootView.findViewById(R.id.fileType);
        toolbar = rootView.findViewById(R.id.toolbar);
        icon = rootView.findViewById(R.id.fileIcon);

        openButton = rootView.findViewById(R.id.openButton);
        downloadButton = rootView.findViewById(R.id.downloadButton);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //we need to get the file info based on the URL that we received.
        setUpCallback();
        FileFolderManager.getFileFolderFromURL(fileUrl, fileFolderCanvasCallback);
    }

    @Override
    public void applyTheme() {
        setupToolbarMenu(toolbar);
        ViewStyler.themeToolbar(getActivity(), toolbar, getCanvasContext());
    }

    public void setupTextViews() {
        fileNameTextView.setText(file.getDisplayName());
        fileTypeTextView.setText(file.getContentType());
    }

    public void setupClickListeners() {
        openButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                openMedia(file.getContentType(), file.getUrl(), file.getDisplayName());

                //Mark the module as read
                ModuleManager.markModuleItemAsRead(getCanvasContext(), moduleId, itemId, markReadCanvasCallback);
            }
        });

        downloadButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (PermissionUtils.hasPermissions(getActivity(), PermissionUtils.WRITE_EXTERNAL_STORAGE)) {
                    downloadFile();
                } else {
                    requestPermissions(PermissionUtils.makeArray(PermissionUtils.WRITE_EXTERNAL_STORAGE), PermissionUtils.WRITE_FILE_PERMISSION_REQUEST_CODE);
                }
            }
        });
    }

    private void downloadFile() {
        DownloadMedia.downloadMedia(getActivity(), file.getUrl(), file.getDisplayName(), file.getName());

        //Mark the module as read
        ModuleManager.markModuleItemAsRead(getCanvasContext(), moduleId, itemId, markReadCanvasCallback);
    }

    public void setUpCallback() {
        fileFolderCanvasCallback = new StatusCallback<FileFolder>() {
            @Override
            public void onResponse(@NonNull Response<FileFolder> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                if (!apiCheck()) {
                    return;
                }

                //set up everything else now, we should have a file
                file = response.body();

                if (file != null) {
                    if (file.getLockInfo() != null) {
                        //file is locked
                        icon.setImageResource(R.drawable.vd_lock);
                        openButton.setVisibility(View.GONE);
                        downloadButton.setVisibility(View.GONE);
                        fileTypeTextView.setVisibility(View.INVISIBLE);
                        String lockedMessage = "";

                        if (file.getLockInfo().getLockedModuleName() != null) {
                            lockedMessage = "<p>" + String.format(getActivity().getString(R.string.lockedFileDesc), "<b>" + file.getLockInfo().getLockedModuleName() + "</b>") + "</p>";
                        }
                        if (file.getLockInfo().getModulePrerequisiteNames().size() > 0) {
                            //we only want to add this text if there are module completion requirements
                            lockedMessage += getActivity().getString(R.string.mustComplete) + "<br>";
                            //textViews can't display <ul> and <li> tags, so we need to use "&#8226; " instead
                            for (int i = 0; i < file.getLockInfo().getModulePrerequisiteNames().size(); i++) {
                                lockedMessage += "&#8226; " + file.getLockInfo().getModulePrerequisiteNames().get(i);  //"&#8226; "
                            }
                            lockedMessage += "<br><br>";
                        }

                        //check to see if there is an unlocked date
                        if (file.getLockInfo().getUnlockAt() != null && file.getLockInfo().getUnlockAt().after(new Date())) {
                            lockedMessage += DateHelper.createPrefixedDateTimeString(getContext(), getActivity().getString(R.string.unlockedAt) + "<br>&#8226; ", file.getLockInfo().getUnlockAt());
                        }
                        fileNameTextView.setText(StringUtilities.simplifyHTML(Html.fromHtml(lockedMessage)));
                    } else {
                        setupTextViews();
                        setupClickListeners();
                        // if the file has a thumbnail then show it. Make it a little bigger since the thumbnail size is pretty small
                        if(!TextUtils.isEmpty(file.getThumbnailUrl())) {
                            int dp = (int)ViewUtils.convertDipsToPixels(150, getActivity());
                            Picasso.with(getActivity()).load(file.getThumbnailUrl()).resize(dp,dp).centerInside().into(icon);
                        }
                    }
                    setPageViewReady();
                }
                toolbar.setTitle(title());
            }
        };

        markReadCanvasCallback = new StatusCallback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Response<ResponseBody> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {}
        };
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

        fileUrl = extras.getString(Const.FILE_URL);
        moduleId = extras.getLong(Const.MODULE_ID);
        itemId = extras.getLong(Const.ITEM_ID);
    }


    public static Bundle createBundle(CanvasContext canvasContext, String fileUrl) {
        Bundle extras = createBundle(canvasContext);
        extras.putString(Const.FILE_URL, fileUrl);
        return extras;
    }

    public static Bundle createBundle(CanvasContext canvasContext, long moduleId, long itemId, String fileUrl) {
        Bundle extras = createBundle(canvasContext);
        extras.putString(Const.FILE_URL, fileUrl);
        extras.putLong(Const.MODULE_ID, moduleId);
        extras.putLong(Const.ITEM_ID, itemId);
        return extras;
    }

    @Override
    public boolean allowBookmarking() {
        return false;
    }
}
