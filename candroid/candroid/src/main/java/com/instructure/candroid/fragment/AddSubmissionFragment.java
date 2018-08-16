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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.instructure.candroid.R;
import com.instructure.candroid.util.Analytics;
import com.instructure.candroid.util.VisibilityAnimator;
import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.managers.ExternalToolManager;
import com.instructure.canvasapi2.managers.SubmissionManager;
import com.instructure.canvasapi2.models.Assignment;
import com.instructure.canvasapi2.models.Attachment;
import com.instructure.canvasapi2.models.Course;
import com.instructure.canvasapi2.models.LTITool;
import com.instructure.canvasapi2.models.Submission;
import com.instructure.canvasapi2.utils.ApiPrefs;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.LinkHeaders;
import com.instructure.interactions.FragmentInteractions;
import com.instructure.interactions.Navigation;
import com.instructure.pandautils.activities.NotoriousMediaUploadPicker;
import com.instructure.pandautils.dialogs.UploadFilesDialog;
import com.instructure.pandautils.utils.Const;
import com.instructure.pandautils.utils.FileUploadEvent;
import com.instructure.pandautils.utils.FileUploadNotification;
import com.instructure.pandautils.utils.PandaViewUtils;
import com.instructure.pandautils.utils.RequestCodes;
import com.instructure.pandautils.utils.ThemePrefs;
import com.instructure.pandautils.utils.ViewStyler;
import com.instructure.pandautils.views.CanvasWebView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import retrofit2.Call;
import retrofit2.Response;

public class AddSubmissionFragment extends ParentFragment {

	//View
	private ScrollView scrollView;
    private Toolbar toolbar;
	private LinearLayout textEntryContainer;
	private LinearLayout urlEntryContainer;
    private LinearLayout mArcEntryContainer;

	private CanvasWebView webView;

	private TextView textEntry;
	private AppCompatEditText textSubmission;
	private Button submitTextEntry;

	private TextView urlEntry;
	private AppCompatEditText urlSubmission;
	private Button submitURLEntry;

	private TextView fileEntry;

    private TextView mediaUpload;

    private TextView mArcUpload;
    private AppCompatEditText mArcSubmission;
    private Button mSubmitArcEntry;

	//Passed In Assignment and course
	private Assignment assignment;
	private Course course;

	
	//Assignment Permissions
	private boolean isOnlineTextAllowed;
	private boolean isUrlEntryAllowed;
	private boolean isFileEntryAllowed;
    private boolean isMediaRecordingAllowed;

	//Timer
	private Timer timer;
	private Timer isPageDone;

	//Handler
	private Handler mHandler;

    private boolean isFileUploadCanceled = false;

    private StatusCallback<Submission> canvasCallbackSubmission;
    private StatusCallback<List<LTITool>> mLTIToolCallback;

    private LTITool mArcLTITool;

    @Override
    @NonNull
    public FragmentInteractions.Placement getFragmentPlacement() {return FragmentInteractions.Placement.DETAIL; }

    @Override
    @NonNull
    public String title() {
        return getString(R.string.assignmentTabSubmission);
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		mHandler = new Handler();

		View rootView = getLayoutInflater().inflate(R.layout.fragment_add_submission, container, false);
        toolbar = rootView.findViewById(R.id.toolbar);
		scrollView = rootView.findViewById(R.id.scrollView);

		textEntryContainer = rootView.findViewById(R.id.textEntryContainer);
		urlEntryContainer = rootView.findViewById(R.id.urlEntryContainer);

		webView = rootView.findViewById(R.id.webView);
		webView.getSettings().setJavaScriptEnabled(true);

		textEntry = rootView.findViewById(R.id.textEntryHeader);
		textSubmission = rootView.findViewById(R.id.textEntry);
		submitTextEntry = rootView.findViewById(R.id.submitTextEntry);

		urlEntry = rootView.findViewById(R.id.onlineURLHeader);
		urlSubmission = rootView.findViewById(R.id.onlineURL);
		submitURLEntry = rootView.findViewById(R.id.submitURLEntry);

		fileEntry = rootView.findViewById(R.id.fileUpload);

        mediaUpload = rootView.findViewById(R.id.mediaSubmission);

        mArcUpload = rootView.findViewById(R.id.arcSubmission);
        mArcSubmission = rootView.findViewById(R.id.arcEntry);
        mSubmitArcEntry = rootView.findViewById(R.id.submitArcEntry);
        mArcEntryContainer = rootView.findViewById(R.id.arcEntryContainer);

		timer = new Timer();
		isPageDone = new Timer();

		return rootView;
	}

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ViewStyler.themeButton(submitTextEntry);
        ViewStyler.themeButton(submitURLEntry);
        ViewStyler.themeButton(mSubmitArcEntry);

        ViewStyler.themeEditText(getContext(), textSubmission, ThemePrefs.getBrandColor());
        ViewStyler.themeEditText(getContext(), urlSubmission, ThemePrefs.getBrandColor());
        ViewStyler.themeEditText(getContext(), mArcSubmission, ThemePrefs.getBrandColor());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setUpCallback();
        setupViews();
        setupListeners();

        //if file entry is allowed check to see if the account has arc installed
        if(isFileEntryAllowed) {
            ExternalToolManager.getExternalToolsForCanvasContext(getCanvasContext(), mLTIToolCallback, true);
        }

        // If only file is allowed, move to the next page (or go back if we are coming from file uploads).
        if(!isOnlineTextAllowed && !isUrlEntryAllowed) {
            if(isMediaRecordingAllowed && !isFileEntryAllowed){
                Intent intent = NotoriousMediaUploadPicker.createIntentForAssignmentSubmission(getContext(), assignment);
                startActivityForResult(intent, RequestCodes.NOTORIOUS_REQUEST);
            }
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == RequestCodes.NOTORIOUS_REQUEST) {
            // When its a Notorious request, just dismiss the fragment, and the user can look at the notification to see progress.
            getActivity().onBackPressed();
        } else if (resultCode == Activity.RESULT_CANCELED && requestCode == RequestCodes.NOTORIOUS_REQUEST) {
            isFileUploadCanceled = true;
        }
    }

	//  This gets called by the activity in onBackPressed().
    //  Call super so that we can check if there is unsaved data.
	@Override
	public boolean handleBackPressed() {
		if(urlEntryContainer.getVisibility() == View.VISIBLE && webView.canGoBack()) {
			webView.goBack();
			return true;
		} else {
            return super.handleBackPressed();
        }
	}

    @Override
    public void onPause() {
        dataLossPause(textSubmission, Const.DATA_LOSS_ADD_SUBMISSION);
        dataLossPause(urlSubmission, Const.DATA_LOSS_ADD_SUBMISSION_URL);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(dataLossResume(textSubmission, Const.DATA_LOSS_ADD_SUBMISSION)) {
            if(isOnlineTextAllowed) {
                textEntry.performClick();
            }
        }

        if(dataLossResume(urlSubmission, Const.DATA_LOSS_ADD_SUBMISSION_URL)) {
            if(isUrlEntryAllowed) {
                urlEntry.performClick();
            }
        }
        dataLossAddTextWatcher(textSubmission, Const.DATA_LOSS_ADD_SUBMISSION);
        dataLossAddTextWatcher(urlSubmission, Const.DATA_LOSS_ADD_SUBMISSION_URL);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mArcSubmissionReceiver, new IntentFilter(Const.ARC_SUBMISSION));
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mArcSubmissionReceiver);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(final FileUploadEvent event) {
        event.once(FileUploadNotification.class.getSimpleName(), new Function1<FileUploadNotification, Unit>() {
            @Override
            public Unit invoke(FileUploadNotification fileUploadNotification) {
                EventBus.getDefault().post(event); //Repost for SubmissionDetailsFragment
                if(getNavigation() != null) getNavigation().popCurrentFragment();
                return null;
            }
        });
    }

	/*
	 * Useful for 2 reasons.
	 * 1.) The WebView won't load without a scheme
	 * 2.) The API automatically puts http or https at the front if it's not there anyways.
	 */
	public String getHttpURLSubmission() {
		String url = urlSubmission.getText().toString();
		if(!url.startsWith("http://") && !url.startsWith("https://")) return "http://"+url;
        else return url;
	}

    private void setupViews() {
        //hide arc until we can figure out if arc is installed for the course
        mArcUpload.setVisibility(View.GONE);

        //Hide text if it's not allowed.
        if(!isOnlineTextAllowed) {
            textEntry.setVisibility(View.GONE);
        }

        //Hide url if it's not allowed.
        if(!isUrlEntryAllowed) {
            urlEntry.setVisibility(View.GONE);
        }

        //Hide file if it's not allowed.
        if(!isFileEntryAllowed) {
            fileEntry.setVisibility(View.GONE);
        }

        if(!isMediaRecordingAllowed) {
            mediaUpload.setVisibility(View.GONE);
        }
        //If only text is allowed, open the tab.
        if(isOnlineTextAllowed && !isUrlEntryAllowed && !isFileEntryAllowed) {
            VisibilityAnimator.animateVisible(AnimationUtils.loadAnimation(getActivity(),
                    R.anim.slow_push_left_in), textEntryContainer);
        }

        //If only url is allowed, open the tab.
        if(!isOnlineTextAllowed && isUrlEntryAllowed && !isFileEntryAllowed) {
            VisibilityAnimator.animateVisible(AnimationUtils.loadAnimation(getActivity(),
                    R.anim.slow_push_left_in), urlEntryContainer);
        }

        setupWebview();
    }

    private void setupWebview() {
        //Give it a default.
        webView.loadUrl("");

        //Start off by hiding webview box.
        webView.setVisibility(View.GONE);

        //Fit to width.
        // Configure the webview
        WebSettings settings = webView.getSettings();
        settings.setBuiltInZoomControls(true);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        //Open all urls with our webview.
        webView.setWebViewClient(new WebViewClient()
                //Once a page has loaded, stop the spinner.
        {
            @Override
            public void onPageFinished(WebView view, final String finishedURL)
            {
                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        String url = finishedURL;
                        scrollView.scrollTo(0, urlEntryContainer.getTop());

                        //In Kit-Kat, an empty url auto-redirects to "about:blank".
                        //This handles that case.
                        if (url.equals("about:blank")) {
                            return;
                        }
                        //Do my best to not interrupt their typing.
                        else if (urlSubmission.getText().toString().endsWith("/")) {
                            if (!url.endsWith("/")) {
                                url = url + "/";
                            }
                        } else {
                            if(url.endsWith("/")) {
                                url = url.substring(0, url.length()-1);
                            }
                        }

                        //we only want to set the text to the url if it's a valid url. If you put an invalid url (www.goog for example)
                        //the webview redirects and eventually returns some html that then is put into the urlSubmission editText
                        if(Patterns.WEB_URL.matcher(url).matches()) {
                            urlSubmission.setText(url);
                            urlSubmission.setSelection(urlSubmission.getText().length());
                        }
                    }
                });
            }
        });
    }

    private void showFileUploadDialog() {
        if(!isMediaRecordingAllowed && isFileEntryAllowed && mArcLTITool == null){
            if (isFileUploadCanceled) {
                getActivity().onBackPressed();
            } else {
                isFileUploadCanceled = true;
                UploadFilesDialog.show(getFragmentManager(), UploadFilesDialog.createAssignmentBundle(null, course, assignment), new Function1<Integer, Unit>() {
                    @Override
                    public Unit invoke(Integer event) {
                        if(event == UploadFilesDialog.EVENT_ON_UPLOAD_BEGIN) {
                            if(getNavigation() != null) getNavigation().popCurrentFragment();
                        }
                        return null;
                    }
                });
            }
        }
    }

    private BroadcastReceiver mArcSubmissionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent != null) {
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    String url = extras.getString(Const.URL);
                    mArcEntryContainer.setVisibility(View.VISIBLE);
                    mArcSubmission.setText(url);
                }
            }
        }
    };

    private void setupListeners() {
        fileEntry.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                UploadFilesDialog.show(getFragmentManager(), UploadFilesDialog.createAssignmentBundle(null, course, assignment), new Function1<Integer, Unit>() {
                    @Override
                    public Unit invoke(Integer integer) {
                        return null;
                    }
                });
            }
        });

        urlEntry.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if(urlEntryContainer.getVisibility() == View.VISIBLE)
                {
                    VisibilityAnimator.animateGone(AnimationUtils.loadAnimation(getActivity(),
                            R.anim.slow_push_right_out), urlEntryContainer);
                }
                else
                {
                    VisibilityAnimator.animateVisible(AnimationUtils.loadAnimation(getActivity(),
                            R.anim.slow_push_left_in), urlEntryContainer);
                }
            }
        });

        textEntry.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if(textEntryContainer.getVisibility() == View.VISIBLE)
                {
                    VisibilityAnimator.animateGone(AnimationUtils.loadAnimation(getActivity(),
                            R.anim.slow_push_right_out), textEntryContainer);
                }
                else
                {
                    VisibilityAnimator.animateVisible(AnimationUtils.loadAnimation(getActivity(),
                            R.anim.slow_push_left_in), textEntryContainer);
                }
            }
        });

        submitTextEntry.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                tryToSubmitText();
            }
        });

        //Because it's not single line, we have to handle the enter button.
        textSubmission.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    tryToSubmitText();

                    //Hide keyboard
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                    return true;
                }
                return false;
            }
        });

        submitURLEntry.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                tryToSubmitURL();
            }
        });

        //Because it's not single line, we have to handle the enter button.
        urlSubmission.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    tryToSubmitURL();

                    //Hide keyboard
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                    return true;
                }
                return false;
            }
        });


        urlSubmission.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {}

            @Override
            public void afterTextChanged(final Editable string) {
                timer.cancel();
                timer = new Timer();
                timer.schedule(new TimerTask() {

                    @Override
                    public void run() {
                        //Start it again just in case another page finished loading
                        //in the one second after the text changed and the webview starts loading.
                        mHandler.post(new Runnable() {

                            @Override
                            public void run() {
                                String originalURL = webView.getUrl();

                                //Null pointer check
                                if(originalURL == null)
                                {
                                    originalURL = "";
                                }

                                //Strip off ending / characters
                                if(originalURL.endsWith("/"))
                                {
                                    originalURL = originalURL.substring(0, originalURL.length()-1);
                                }

                                String currentURL = getHttpURLSubmission();
                                //Null pointer check
                                if(currentURL == null)
                                {
                                    currentURL = "";
                                }
                                if(currentURL.endsWith("/"))
                                {
                                    currentURL = currentURL.substring(0, currentURL.length()-1);
                                }

                                //If it's empty clear the view.
                                if(string.toString().trim().length() == 0)
                                {
                                    webView.setVisibility(View.GONE);
                                    webView.loadUrl("");
                                }
                                else if (!originalURL.equals(currentURL))	//if it's already loaded, don't do it.
                                {
                                    webView.setVisibility(View.VISIBLE);
                                    webView.loadUrl(getHttpURLSubmission());
                                }
                            }
                        });
                    }
                }, 2000);
            }
        });

        mediaUpload.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = NotoriousMediaUploadPicker.createIntentForAssignmentSubmission(getContext(), assignment);
                startActivityForResult(intent, RequestCodes.NOTORIOUS_REQUEST);
            }
        });

        mArcUpload.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = String.format(Locale.getDefault(), "%s/%s/external_tools/%d/resource_selection?launch_type=homework_submission&assignment_id=%d", ApiPrefs.getFullDomain(), getCanvasContext().toAPIString(), mArcLTITool.getId(), assignment.getId());
                ArcWebviewFragment.loadInternalWebView(getActivity(), ((Navigation)getActivity()), InternalWebviewFragment.Companion.createBundle(getCanvasContext(), url, mArcLTITool.getName(), true));
            }
        });

        mSubmitArcEntry.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                tryToSubmitArc();
            }
        });
    }

    ///////////////////////////////////////////////////////////////////////////
    // Submit Submission Data
    ///////////////////////////////////////////////////////////////////////////

    public void tryToSubmitText(){
        //get the text, replace all line breaks with <br/> tags so they are preserved when displayed in a webview.
        String textToSubmit = textSubmission.getText().toString().replaceAll("\\n", "<br/>");
        if(textToSubmit.trim().length() == 0) { 	//It's an empty submission
            Toast.makeText(getActivity(), R.string.blankSubmission, Toast.LENGTH_LONG).show();
        }
        else {
            //Log to GA
            Analytics.trackButtonPressed(getActivity(), "Submit Text Assignment", null);

            SubmissionManager.postTextSubmission(course, assignment.getId(), textToSubmit, canvasCallbackSubmission);
            dataLossDeleteStoredData(Const.DATA_LOSS_ADD_SUBMISSION);
        }
    }

    public void tryToSubmitURL(){
        String urlToSubmit = urlSubmission.getText().toString();
        if(urlToSubmit.trim().length() == 0) { 	//It's an empty submission
            Toast.makeText(getActivity(), R.string.blankSubmission, Toast.LENGTH_LONG).show();
        }
        else {
            //Log to GA
            Analytics.trackButtonPressed(getActivity(), "Submit URL Assignment", null);

            SubmissionManager.postUrlSubmission(course, assignment.getId(), urlToSubmit, false, canvasCallbackSubmission);
            dataLossDeleteStoredData(Const.DATA_LOSS_ADD_SUBMISSION_URL);
        }
    }

    public void tryToSubmitArc(){
        String urlToSubmit = mArcSubmission.getText().toString();
        if(urlToSubmit.trim().length() == 0) { 	//It's an empty submission
            Toast.makeText(getActivity(), R.string.blankSubmission, Toast.LENGTH_LONG).show();
        }
        else {
            //Log to GA
            Analytics.trackButtonPressed(getActivity(), "Submit Arc Assignment", null);

            SubmissionManager.postUrlSubmission(course, assignment.getId(), urlToSubmit, true, canvasCallbackSubmission);
            dataLossDeleteStoredData(Const.DATA_LOSS_ADD_SUBMISSION_URL);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // CallBack
    ///////////////////////////////////////////////////////////////////////////

    public void setUpCallback() {

        canvasCallbackSubmission = new StatusCallback<Submission>() {
            @Override
            public void onResponse(@NonNull Response<Submission> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                if(!apiCheck()){
                    return;
                }

                Submission result = response.body();
                if(result.getBody() != null || result.getUrl() != null) {
                    Toast.makeText(getActivity(), R.string.successPostingSubmission, Toast.LENGTH_LONG).show();
                    // clear text fields because they are saved
                    textSubmission.setText("");
                    urlSubmission.setText("");
                    // Send broadcast so list is updated.
                    EventBus.getDefault().post(new FileUploadEvent(new FileUploadNotification(null, new ArrayList<Attachment>())));
                    Navigation navigation = getNavigation();
                    if(navigation != null) navigation.popCurrentFragment();
                } else {
                    Toast.makeText(getActivity(), R.string.errorPostingSubmission, Toast.LENGTH_LONG).show();
                }
            }
        };

        mLTIToolCallback = new StatusCallback<List<LTITool>>() {

            @Override
            public void onResponse(@NonNull Response<List<LTITool>> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                for (LTITool ltiTool : response.body()) {
                    final String url = ltiTool.getUrl();
                    if (url != null && url.contains("instructuremedia.com/lti/launch")) {
                        mArcUpload.setVisibility(View.VISIBLE);
                        mArcLTITool = ltiTool;
                        break;
                    }
                }
                //check to see if we should automatically show the file upload dialog
                showFileUploadDialog();
            }

            @Override
            public void onFail(@Nullable Call<List<LTITool>> call, @NonNull Throwable error, @Nullable Response response) {
                //check to see if we should automatically show the file upload dialog
                //we don't want to show it if this failed due to there being no cache
                if (response != null && response.code() != 504) {
                    showFileUploadDialog();
                }
            }
        };                                  
    }

	///////////////////////////////////////////////////////////////////////////
	// Intent
	///////////////////////////////////////////////////////////////////////////

	@Override
	public void handleIntentExtras(Bundle extras) {
		super.handleIntentExtras(extras);
		// do stuff with bundle here
        course = (Course) getCanvasContext();

		assignment = extras.getParcelable(Const.ASSIGNMENT);
        isOnlineTextAllowed = extras.getBoolean(Const.TEXT_ALLOWED);
		isUrlEntryAllowed = extras.getBoolean(Const.URL_ALLOWED);
		isFileEntryAllowed = extras.getBoolean(Const.FILE_ALLOWED);
        isMediaRecordingAllowed = extras.getBoolean(Const.MEDIA_UPLOAD_ALLOWED);
	}

	public static Bundle createBundle(Course course, Assignment assignment, boolean textEntryAllowed, boolean urlEntryAllowed, boolean fileEntryAllowed, boolean mediaUploadAllowed) {
		Bundle extras = createBundle(course);
		extras.putParcelable(Const.ASSIGNMENT, assignment);
		extras.putBoolean(Const.TEXT_ALLOWED, textEntryAllowed);
		extras.putBoolean(Const.URL_ALLOWED, urlEntryAllowed);
		extras.putBoolean(Const.FILE_ALLOWED, fileEntryAllowed);
        extras.putBoolean(Const.MEDIA_UPLOAD_ALLOWED, mediaUploadAllowed);

		return extras;
	}

    @Override
    public boolean allowBookmarking() {
        return true;
    }
}
