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
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.instructure.candroid.R;
import com.instructure.candroid.activity.InternalWebViewActivity;
import com.instructure.candroid.util.AppManager;
import com.instructure.candroid.util.DownloadMedia;
import com.instructure.candroid.util.FragUtils;
import com.instructure.candroid.util.RouterUtils;
import com.instructure.candroid.view.AttachmentLayout;
import com.instructure.candroid.view.AttachmentView;
import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.managers.AssignmentManager;
import com.instructure.canvasapi2.managers.NotoriousManager;
import com.instructure.canvasapi2.managers.SubmissionManager;
import com.instructure.canvasapi2.models.Assignment;
import com.instructure.canvasapi2.models.Attachment;
import com.instructure.canvasapi2.models.Author;
import com.instructure.canvasapi2.models.Course;
import com.instructure.canvasapi2.models.DiscussionTopic;
import com.instructure.canvasapi2.models.LTITool;
import com.instructure.canvasapi2.models.MediaComment;
import com.instructure.canvasapi2.models.NotoriousConfig;
import com.instructure.canvasapi2.models.Submission;
import com.instructure.canvasapi2.models.SubmissionComment;
import com.instructure.canvasapi2.utils.APIHelper;
import com.instructure.canvasapi2.utils.ApiPrefs;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.DateHelper;
import com.instructure.canvasapi2.utils.FileUtils;
import com.instructure.canvasapi2.utils.LinkHeaders;
import com.instructure.canvasapi2.utils.pageview.BeforePageView;
import com.instructure.canvasapi2.utils.pageview.PageView;
import com.instructure.canvasapi2.utils.pageview.PageViewUrlParam;
import com.instructure.canvasapi2.utils.pageview.PageViewUrlQuery;
import com.instructure.interactions.FragmentInteractions;
import com.instructure.interactions.Navigation;
import com.instructure.pandautils.activities.NotoriousMediaUploadPicker;
import com.instructure.pandautils.dialogs.UploadFilesDialog;
import com.instructure.pandautils.models.FileSubmitObject;
import com.instructure.pandautils.services.FileUploadService;
import com.instructure.pandautils.utils.ColorKeeper;
import com.instructure.pandautils.utils.Const;
import com.instructure.pandautils.utils.FileUploadEvent;
import com.instructure.pandautils.utils.FileUploadNotification;
import com.instructure.pandautils.utils.FragmentExtensionsKt;
import com.instructure.pandautils.utils.PandaViewUtils;
import com.instructure.pandautils.utils.PermissionUtils;
import com.instructure.pandautils.utils.ProfileUtils;
import com.instructure.pandautils.utils.RequestCodes;
import com.instructure.pandautils.utils.Utils;
import com.instructure.pandautils.utils.ViewStyler;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import retrofit2.Call;
import retrofit2.Response;

@PageView(url = "{canvasContext}/assignments/{assignmentId}/submissions")
public class SubmissionDetailsFragment extends ParentFragment {

    public interface SubmissionDetailsFragmentCallback {
        void updateSubmissionDate(Date date);
    }

    private Toolbar toolbar;

    private Button addSubmission;
    private Button submitComment;
    private Button mediaComment;
    private Button addComment;
    private ProgressBar sendingProgressBar;
    private ListView listView;
    private ImageView addSubmissionInfo;
    private AttachmentLayout attachmentLayout;

    private SubmissionAdapter adapter;
    private boolean hasSubmissions;
    private boolean hasGrade;
    private String grade;
    private Assignment assignment;
    private Course course;
    private int lastPosition = -1;
    private ArrayList<FileSubmitObject> attachmentsList = new ArrayList<>();
    private ArrayList<Long> attachmentIds = new ArrayList<>();
    private ArrayList<Attachment> attachments = new ArrayList<>();

    private BroadcastReceiver errorBroadcastReceiver;
    private BroadcastReceiver allUploadsCompleteBroadcastReceiver;
    private boolean needsUnregister;

    //submission type booleans
    private boolean isOnlineTextAllowed;
    private boolean isUrlEntryAllowed;
    private boolean isFileEntryAllowed;
    private boolean isPaper;
    private boolean isNoSubmission;
    private boolean isQuiz;
    private boolean isDiscussion;
    private boolean isExternalTool;
    private boolean isMediaRecording;

    private long myUserId;


    //submission comment variables
    private String currentMimeType = "";
    private String currentURL = "";
    private String currentFileName;
    private String currentDisplayName;
    private String currentMessage;
    private AppManager am;
    private LayoutInflater inflater;
    //view that contains the comment text box and submit button. Global here so we can
    //make it GONE when the user is a teacher.
    private View composeHeaderView;
    private View headerView;

    //views
    private EditText message;

    // assignment fragment
    private WeakReference<AssignmentFragment> assignmentFragment;


    private SubmissionDetailsFragmentCallback submissionDetailsFragmentCallback;

    // callbacks
    private StatusCallback<Submission> canvasCallbackSubmission;
    private StatusCallback<Submission> canvasCallbackMessage;
    private StatusCallback<LTITool> canvasCallbackLTITool;
    private StatusCallback<Assignment> canvasCallbackAssignment;
    private StatusCallback<NotoriousConfig> notoriousConfigCallback;

    @PageViewUrlParam(name = "assignmentId")
    private long getAssignmentId() {
        return assignment.getId();
    }

    @PageViewUrlQuery(name = "module_item_id")
    private Long getModuleItemId() {
        return FragmentExtensionsKt.getModuleItemId(this);
    }

    private BroadcastReceiver submissionCommentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(assignment != null){
                loadData(assignment.getId(), false, false);
            } else {
                showToast(R.string.errorOccurred);
            }
        }
    };

    @Override
    @NonNull
    public FragmentInteractions.Placement getFragmentPlacement() {return FragmentInteractions.Placement.DETAIL; }

    @Override
    @NonNull
    public String title() {
        return getString(R.string.assignmentTabSubmission);
    }

    public void setSubmissionDetailsFragmentCallback(SubmissionDetailsFragmentCallback submissionDetailsFragmentCallback) {
        this.submissionDetailsFragmentCallback = submissionDetailsFragmentCallback;
    }

    public void setAssignmentFragment(WeakReference<AssignmentFragment> assignmentFragment) {
        this.assignmentFragment = assignmentFragment;
    }

    /**
     * For explanation of isWithinAnotherCallback and isCached refer to comment in {@link com.instructure.candroid.activity.CallbackActivity#}
     */
    @BeforePageView
    public void setAssignment(Assignment assignment, boolean isWithinAnotherCallback, boolean isCached) {
        this.assignment = assignment;
        populateViews(assignment, isWithinAnotherCallback, isCached);
    }

    public static int getTabTitle() {
        return R.string.assignmentTabSubmission;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    public void onStart() {
        super.onStart();
        registerReceivers();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        unregisterReceivers();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = getLayoutInflater().inflate(R.layout.fragment_submission_details, container, false);
        setupViews(rootView);
        return rootView;
    }

    @Override
    public void applyTheme() {
        setupToolbarMenu(toolbar);
        PandaViewUtils.setupToolbarBackButton(toolbar, this);
        ViewStyler.themeToolbar(getActivity(), toolbar, getCanvasContext());
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onFileEvent(final FileUploadEvent event) {
        event.get(new Function1<FileUploadNotification, Unit>() {
            @Override
            public Unit invoke(FileUploadNotification fileUploadNotification) {
                if(assignment != null) loadData(assignment.getId(), false, false);
                return null;
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // helps GC and memory management
        notoriousConfigCallback = null;
        canvasCallbackAssignment = null;
        canvasCallbackLTITool = null;
        canvasCallbackMessage = null;
        adapter = null;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        adapter = new SubmissionAdapter(getActivity(), R.layout.listview_item_row_submission_comments_left, new ArrayList<Submission>());
        listView.setAdapter(adapter);

        inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        myUserId = ApiPrefs.getUser().getId();

        am = ((AppManager)getActivity().getApplication());

        setupListeners();
        hasGrade = false;
        hasSubmissions = false;

        isOnlineTextAllowed = false;
        isUrlEntryAllowed = false;
        isFileEntryAllowed = false;
        isPaper = false;
        isNoSubmission = false;
        isQuiz = false;
        isDiscussion = false;
        isExternalTool = false;
        isMediaRecording = false;

        setupCallbacks();
        if (notoriousConfigCallback != null) {
            NotoriousManager.getConfiguration(notoriousConfigCallback);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        currentMimeType = (String) v.findViewById(R.id.mime).getTag();
        currentURL = (String) v.findViewById(R.id.url).getTag();
        currentFileName = (String) v.findViewById(R.id.file).getTag();
        currentDisplayName = (String) v.findViewById(R.id.display).getTag();

        menu.add(getResources().getString(R.string.open));

        if(AppManager.isDownloadManagerAvailable(getActivity()) && currentFileName != null && currentDisplayName != null)
            menu.add(getResources().getString(R.string.download));
    }


    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        if(item.getTitle().equals(getResources().getString(R.string.open))) {
            //Open media
            openMedia(currentMimeType, currentURL, currentFileName);
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
        DownloadMedia.downloadMedia(getActivity(),currentURL, currentFileName, currentDisplayName);
    }

    private void setupViews(View rootView) {
        toolbar = rootView.findViewById(R.id.toolbar);

        LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        //set up top header (top cell)
        headerView = inflater.inflate(R.layout.submission_details_header, null);
        //buttons, make them gone initially because we don't show them if they're a teacher
        addSubmission = headerView.findViewById(R.id.addSubmission);
        addSubmissionInfo = headerView.findViewById(R.id.addSubmissionInfo);
        addSubmissionInfo.setVisibility(View.GONE);
        addSubmission.setVisibility(View.GONE);

        addComment = headerView.findViewById(R.id.addComment);
        addComment.setVisibility(View.GONE);

        //listviews
        listView = rootView.findViewById(R.id.submissionsList);

        //compose header view
        composeHeaderView = inflater.inflate(R.layout.submission_details_compose_view, null);
        composeHeaderView.setLayoutParams(new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, ListView.LayoutParams.MATCH_PARENT));
        composeHeaderView.findViewById(R.id.composeButton).setBackground(ColorKeeper.getColoredDrawable(getContext(), R.drawable.vd_send, Color.GRAY));
        submitComment = composeHeaderView.findViewById(R.id.composeButton);
        sendingProgressBar = composeHeaderView.findViewById(R.id.sendingProgressBar);
        mediaComment = composeHeaderView.findViewById(R.id.mediaComment);
        attachmentLayout = composeHeaderView.findViewById(R.id.attachmentLayout);
        //editTexts
        message = composeHeaderView.findViewById(R.id.composeMessage);

        composeHeaderView.setVisibility(View.GONE);
        //add the top header (turn in button, label, and divider)
        listView.addHeaderView(headerView, null, false);

        ViewStyler.themeButton(addSubmission);
        ViewStyler.themeButton(submitComment);
        ViewStyler.themeButton(mediaComment);
        ViewStyler.themeButton(addComment);
    }

    private void populateAssignmentDetails(Assignment assignment, Course course) {
        if(course == null || (course != null && course.isStudent())) {
            addSubmission.setVisibility(View.VISIBLE);
            addComment.setVisibility(View.VISIBLE);
            //set the submission button label. It could be "Go to Quiz" or "Go to Discussion" if the assignment
            //is that type
            Date currentDate = new Date();
            if(assignment.getLockAt() != null && currentDate.after(assignment.getLockAt())){
                addSubmission.setEnabled(false);
                addSubmission.setText(getString(R.string.pastDueDate));
                ViewStyler.themeButton(addSubmission, Color.LTGRAY, Color.BLACK);
            } else if (assignment.getDiscussionTopicHeader() != null && assignment.getDiscussionTopicHeader().getId() > 0 && assignment.getCourseId() > 0) { //Allow the user to go to the discussion.
                addSubmission.setText(getString(R.string.goToDiscussion));
            }
            else if (assignment.getQuizId() > 0) {
                addSubmission.setText(getString(R.string.goToQuiz));
            }
            else if (assignment.getTurnInType() == Assignment.TURN_IN_TYPE.EXTERNAL_TOOL){
                addSubmission.setText(getResources().getString(R.string.goToExternalTool));
            }
            //not a quiz or a discussion, check if they already have submitted something
            else if(hasSubmissions) {
                addSubmission.setText(getString(R.string.addAnotherSubmission));
            } else {
                addSubmission.setText(getString(R.string.addSubmission));
            }
        }
    }


    /**
     * For explanation of isWithinAnotherCallback and isCached refer to comment in {@link com.instructure.candroid.activity.CallbackActivity#}
     */
    private void populateViews(Assignment assignment, boolean isWithinAnotherCallback, boolean isCached) {
        if (assignment == null) return;
        populateAssignmentDetails(assignment, course);
        loadData(assignment.getId(), isWithinAnotherCallback, isCached);

        //at this point the button says something based on if there is a submission or not.
        //The assignment may not have a submission or it may be a type that we don't handle,
        //so we'll set the message here if applicable and disable the button
        checkSubmissionTypes(assignment);
    }

    /**
     * For explanation of isWithinAnotherCallback and isCached refer to comment in {@link com.instructure.candroid.activity.CallbackActivity}
     */
    public void loadData(long assignmentId, boolean isWithinAnotherCallback, boolean isCached) {
        SubmissionManager.getSingleSubmission(course.getId(), assignmentId, myUserId, canvasCallbackSubmission, true);
    }

    private void checkSubmissionTypes(Assignment assignment) {
        for(Assignment.SUBMISSION_TYPE submissionType : assignment.getSubmissionTypes()) {

            if(submissionType == Assignment.SUBMISSION_TYPE.ONLINE_TEXT_ENTRY) {
                isOnlineTextAllowed = true;
            } else if(submissionType == Assignment.SUBMISSION_TYPE.ONLINE_URL) {
                isUrlEntryAllowed = true;
            } else if(submissionType == Assignment.SUBMISSION_TYPE.ONLINE_UPLOAD) {
                isFileEntryAllowed = true;
            } else if(submissionType == Assignment.SUBMISSION_TYPE.ON_PAPER) {
                isPaper = true;
                addSubmission.setEnabled(false);
                ViewStyler.themeButton(addSubmission, Color.LTGRAY, Color.BLACK);
                addSubmission.setText(getString(R.string.turnIn));
                addSubmissionInfo.setVisibility(View.VISIBLE);
                addSubmissionInfo.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showToast(R.string.turnInByHand);
                    }
                });
            } else if(submissionType == Assignment.SUBMISSION_TYPE.NONE) {
                isNoSubmission = true;
                addSubmission.setEnabled(false);
                ViewStyler.themeButton(addSubmission, Color.LTGRAY, Color.BLACK);
                addSubmission.setText(getString(R.string.noSubRequired));
            } else if(submissionType == Assignment.SUBMISSION_TYPE.MEDIA_RECORDING){
                isMediaRecording = true;

            }
            //if it's a quiz or discussion we want them to go to the quiz or discussion.
            //Setting these true here will prevent the button from being disabled below
            else if(submissionType == Assignment.SUBMISSION_TYPE.DISCUSSION_TOPIC) {
                isDiscussion = true;
            } else if(submissionType == Assignment.SUBMISSION_TYPE.ONLINE_QUIZ) {
                isQuiz = true;
            } else if (submissionType == Assignment.SUBMISSION_TYPE.EXTERNAL_TOOL){
                isExternalTool = true;
            }
        }

        //if they're all still false it's a submission type that we don't handle. 
        //Display a message and disable the button.
        if(!isOnlineTextAllowed && !isUrlEntryAllowed && !isFileEntryAllowed &&
                !isPaper && !isNoSubmission && !isDiscussion && !isQuiz && !isExternalTool && !isMediaRecording) {
            addSubmission.setEnabled(false);
            ViewStyler.themeButton(addSubmission, Color.LTGRAY, Color.BLACK);
            addSubmission.setText(getString(R.string.noTurnInOnMobile));
        }
    }

    private void setupListeners() {
        if (addSubmission != null) {
            addSubmission.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    // TODO  Open Submission view, "Go To Discussion" or "Go To Quiz"
                    if (assignment.getDiscussionTopicHeader() != null && assignment.getDiscussionTopicHeader().getId() > 0 && assignment.getCourseId() > 0) //Allow the user to go to the discussion.
                    {
                        String url = DiscussionTopic.getDiscussionURL(ApiPrefs.getProtocol(), ApiPrefs.getDomain(), assignment.getCourseId(), assignment.getDiscussionTopicHeader().getId());
                        if(!RouterUtils.canRouteInternally(getActivity(), url, ApiPrefs.getDomain(), true)) {
                            Intent intent = new Intent(getActivity(), InternalWebViewActivity.class);
                            getActivity().startActivity(intent);
                        }
                    } else if (assignment.getQuizId() > 0) {
                        String url = getQuizURL(getActivity(), assignment.getCourseId(), assignment.getQuizId());
                        if(!RouterUtils.canRouteInternally(getActivity(), url, ApiPrefs.getDomain(), true)) {
                            Intent intent = new Intent(getActivity(), InternalWebViewActivity.class);
                            getActivity().startActivity(intent);
                        }
                    } else if (assignment.getTurnInType() == Assignment.TURN_IN_TYPE.EXTERNAL_TOOL) {
                        //TODO stream doesn't pass the LTI url. Grab it now if we need it
                        String authenticationURL = assignment.getUrl();
                        if (authenticationURL == null || authenticationURL.equalsIgnoreCase("null")) {
                            //get the assignment
                            AssignmentManager.getAssignment(assignment.getId(), course.getId(), true, canvasCallbackAssignment);
                            return;
                        } else {
                            SubmissionManager.getLtiFromAuthenticationUrl(assignment.getUrl(), canvasCallbackLTITool, true);
                        }
                    } else {
                        if(!APIHelper.hasNetworkConnection()) {
                            Toast.makeText(getContext(), getContext().getString(R.string.notAvailableOffline), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        //open add submission view
                        //start it for xml so we can update the results if we need to. Need to call it on the parent fragment
                        //and it will call onActivityResult in the current child fragment.
                        Navigation navigation = getNavigation();
                        if (navigation != null) {
                            Bundle bundle = AddSubmissionFragment.createBundle((Course) getCanvasContext(), assignment, isOnlineTextAllowed, isUrlEntryAllowed, isFileEntryAllowed, isMediaRecording);
                            navigation.addFragment(
                                    FragUtils.getFrag(AddSubmissionFragment.class, bundle));
                        }
                    }
                }
            });
        }

        //Handle pressing of the send button.
        if(submitComment != null) {
            submitComment.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    //try to submit the comment
                    if(!APIHelper.hasNetworkConnection()) {
                        Toast.makeText(getContext(), getContext().getString(R.string.notAvailableOffline), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    sendMessage(assignment.getId());
                }});

            mediaComment.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    ListView listView = new ListView(getActivity());
                    AttachmentAdapter adapter = new AttachmentAdapter(getActivity(), android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.discussion_attachments));
                    listView.setAdapter(adapter);
                    listView.setDivider(null);
                    listView.setPadding(
                            (int) Utils.dpToPx(getActivity(), 10),
                            (int) Utils.dpToPx(getActivity(), 10),
                            (int) Utils.dpToPx(getActivity(), 10),
                            (int) Utils.dpToPx(getActivity(), 16));

                    final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.commentUpload)
                            .setView(listView)
                            .create();

                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            switch(position){
                                case 0:
                                    // Use File Upload Dialog
                                    showUploadFileDialog();
                                    dialog.dismiss();
                                    break;
                                case 1:
                                    if (message.getText().toString().trim().length() != 0 || attachmentIds.size() > 0) {
                                        Toast.makeText(getContext(), R.string.unableToUploadMediaComment, Toast.LENGTH_SHORT).show();
                                    } else {
                                        // Use Notorious
                                        Intent intent = NotoriousMediaUploadPicker.createIntentForSubmissionComment(getContext(), assignment);
                                        assignmentFragment.get().startActivityForResult(intent, RequestCodes.NOTORIOUS_REQUEST);
                                        dialog.dismiss();
                                    }
                                    break;
                                default:
                                    break;
                            }
                        }
                    });

                    dialog.show();

                }
            });
        }


        //Handle if they hit the send button in landscape mode.
        message.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                //Try to send the message.
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendMessage(assignment.getId());

                    //Hide keyboard
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                            Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(message.getWindowToken(), 0);

                    return true;
                }
                return false;
            }
        });



        if(listView!= null) {
            listView.setOnItemClickListener(new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> adapter, View view,
                                        int position, long id) {
                    Submission submission = (Submission)adapter.getAdapter().getItem(position);
                    //if there are no comments it means this is a submission history, which means
                    //it has the details that we need
                    if(submission.getSubmissionComments().size() == 0) {
                        if(submission.getSubmissionType().equals("online_upload") || submission.getSubmissionType().equals("media_recording")) {
                            if(submission.getAttachments().size() == 1) {
                                //makes more sense to open the file since they should already have it on their device
                                openMedia(submission.getAttachments().get(0).getContentType(), submission.getAttachments().get(0).getUrl(), submission.getAttachments().get(0).getFilename());
                            } else if (submission.getMediaComment() != null){
                                MediaComment mediaComment = submission.getMediaComment();
                                openMedia(mediaComment.getContentType(), mediaComment.getUrl(), mediaComment.get_fileName());
                            }
                            else {
                                //show a list dialog of the files to download.
                                showFileList(submission.getAttachments());
                            }
                        }
                        else if(submission.getSubmissionType().equals("online_text_entry")) {

                            Navigation navigation = getNavigation();
                            if(navigation != null){
                                Bundle bundle = InternalWebviewFragment.Companion.createBundleHTML(getCanvasContext(), submission.getBody());
                                navigation.addFragment(FragUtils.getFrag(InternalWebviewFragment.class, bundle));
                            }
                        }
                        else if(submission.getSubmissionType().equals("online_url")) {
                            // Take the user to viewing the online url submission. If the image hasn't processed yet they'll see a toast and it
                            // will match what the web does - show the url and say it might be different from when they submitted.
                            Navigation navigation = getNavigation();
                            if(navigation != null){
                                Bundle bundle = SubmissionViewOnlineURLFragment.createBundle(getCanvasContext(), submission);
                                navigation.addFragment(
                                        FragUtils.getFrag(SubmissionViewOnlineURLFragment.class, bundle));
                            }

                            if(submission.getAttachments().size() == 0) {
                                //the server hasn't processed the image for the submitted url yet, show a crouton and reload
                                showToast(R.string.itemStillProcessing);
                                loadData(assignment.getId(), false, false);
                            }

                        }
                    }
                }
            });
        }

        addComment.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!APIHelper.hasNetworkConnection()) {
                    Toast.makeText(getContext(), getContext().getString(R.string.notAvailableOffline), Toast.LENGTH_SHORT).show();
                    return;
                }
                //Only show the header if it hasn't been added prior.
                if(listView.getHeaderViewsCount()  == 1){
                    listView.addHeaderView(composeHeaderView, null, false);
                }

                //add the compose header (the comment box)
                composeHeaderView.setVisibility(View.VISIBLE);
            }
        });

    }

    private void showUploadFileDialog(){
        // We don't want to remember what was previously there
        attachmentsList.clear();
        Bundle bundle = UploadFilesDialog.createSubmissionCommentBundle(course, assignment, attachmentsList);
        UploadFilesDialog.show(getFragmentManager(), bundle, new Function1<Integer, Unit>() {
            @Override
            public Unit invoke(Integer integer) {
                return null;
            }
        });
    }


    private void registerReceivers() {
        errorBroadcastReceiver = getErrorReceiver();
        allUploadsCompleteBroadcastReceiver = getAllUploadsCompleted();

        getActivity().registerReceiver(errorBroadcastReceiver, new IntentFilter(FileUploadService.UPLOAD_ERROR));
        getActivity().registerReceiver(allUploadsCompleteBroadcastReceiver, new IntentFilter(FileUploadService.ALL_UPLOADS_COMPLETED));
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(submissionCommentReceiver,
                new IntentFilter(Const.SUBMISSION_COMMENT_SUBMITTED));
        needsUnregister = true;
    }

    private void unregisterReceivers() {
        if(getActivity() == null || !needsUnregister){return;}

        if(errorBroadcastReceiver != null){
            getActivity().unregisterReceiver(errorBroadcastReceiver);
            errorBroadcastReceiver = null;
        }

        if(allUploadsCompleteBroadcastReceiver != null){
            getActivity().unregisterReceiver(allUploadsCompleteBroadcastReceiver);
            allUploadsCompleteBroadcastReceiver = null;
        }

        if(submissionCommentReceiver != null) {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(submissionCommentReceiver);
        }

        needsUnregister = false;
    }

    private BroadcastReceiver getErrorReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                if(!isAdded()){return;}

                final Bundle bundle = intent.getExtras();
                String errorMessage = bundle.getString(Const.MESSAGE);
                if(null == errorMessage || "".equals(errorMessage)){
                    errorMessage = getString(R.string.errorUploadingFile);
                }
                showToast(errorMessage);
            }
        };
    }

    // Creates new discussion reply with attachments for the user
    private BroadcastReceiver getAllUploadsCompleted() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                if(!isAdded()){return;}
                showToast(R.string.filesUploadedSuccessfully);
                if(intent != null && intent.hasExtra(Const.ATTACHMENTS)) {
                    ArrayList<Attachment> intentAttachments = intent.getExtras().getParcelableArrayList(Const.ATTACHMENTS);
                    if (intentAttachments != null && intentAttachments.size() > 0) {
                        for (int i = 0; i < intentAttachments.size(); i++) {
                            Attachment attachment = intentAttachments.get(i);
                            attachments.add(attachment);
                            attachmentIds.add(attachment.getId());
                        }

                        refreshAttachments();
                    }
                }
            }
        };
    }

    private void refreshAttachments() {
        attachmentLayout.setPendingAttachments(attachments, true, new Function2<AttachmentView.AttachmentAction, Attachment, Unit>() {
            @Override
            public Unit invoke(AttachmentView.AttachmentAction action, Attachment attachment) {
                if (action == AttachmentView.AttachmentAction.REMOVE) {
                    attachments.remove(attachment);
                    attachmentIds.remove(attachment.getId());
                }
                return null;
            }
        });

    }

    public static String getQuizURL(Context context, long courseid, long quizId) {
        //https://mobiledev.instructure.com/api/v1/courses/24219/quizzes/1129998/
        AppManager AM = (AppManager) context.getApplicationContext();
        return ApiPrefs.getProtocol() + "://" + ApiPrefs.getDomain() + "/courses/" + courseid + "/quizzes/" + quizId;
    }

    /**
     * this will display a dialog list of the attachments for the assignment. Tapping on one will open it.
     *
     * @param attachments
     */
    private void showFileList(ArrayList<Attachment> attachments) {
        //create a new dialog
        Dialog dlg = new Dialog(getActivity());
        LayoutInflater li = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //inflate the view
        View v = li.inflate(R.layout.dialog_listview, null, false);
        dlg.setContentView(v);
        ListView lv = (ListView)v.findViewById(R.id.listview);

        //create the adapter
        SubmissionFileAdapter fileAdapter = new SubmissionFileAdapter(getActivity(), R.layout.listview_item_row_attachedfiles, attachments);
        dlg.setTitle(getString(R.string.myFiles));
        lv.setAdapter(fileAdapter);

        //when they tap an item, open it
        lv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position,
                                    long id) {
                Attachment attachment = (Attachment)adapter.getAdapter().getItem(position);
                openMedia(attachment.getContentType(), attachment.getUrl(), attachment.getFilename());
            }
        });
        dlg.show();
    }

    private void sendMessage(long assignmentId) {
        //Only send if submission comments are fetched.
        //No other message is sending.
        //AND the message isn't empty.

        if(message.getText().toString().trim().length() == 0) {
            showToast(R.string.emptyMessage);
        } else {
            currentMessage = message.getText().toString();

            // Show a progress bar while the message sends
            submitComment.setVisibility(View.GONE);
            sendingProgressBar.setVisibility(View.VISIBLE);
            SubmissionManager.postSubmissionComment(course.getId(), assignmentId, ApiPrefs.getUser().getId(), currentMessage, false, attachmentIds, canvasCallbackMessage);
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Populate content helpers
    ///////////////////////////////////////////////////////////////////////////


    private void populateMessage(SubmissionComment comment, Author author, LinearLayout extras,  TextView title, ImageView image, TextView date, TextView message)
    {

        title.setText(author.getDisplayName());


        if(comment.getCreatedAt()!=null)
            date.setText(DateHelper.getDateTimeString(getContext(), comment.getCreatedAt()));

        message.setText(comment.getComment());


        getMessageExtras(extras, comment);
    }

    private void getMessageExtras(final LinearLayout extras, SubmissionComment message)
    {

        //Assume there are no media comments or attachments.
        extras.removeAllViews();

        //If there is a media comment, add it.
        final MediaComment mc = message.getMediaComment();
        if(mc != null)
        {
            populateMediaComments(extras, message, mc);
        }

        //If there are attachments, add them.
        final List<Attachment> atts = message.getAttachments();
        if(atts != null)
        {
            populateAttachments(extras, atts);
        }


        if(extras.getChildCount() > 0)
        {

            populateExtras(extras);
        }

    }

    /**
     * @param extras
     */
    private void populateExtras(final LinearLayout extras) {
        RelativeLayout extraVisibility = (RelativeLayout)(inflater.inflate(R.layout.detailed_conversation_extras, null));

        //the text should be the display name
        final TextView content = (TextView) extraVisibility.findViewById(R.id.content);
        content.setText(getResources().getString(R.string.showExtras));

        //Bold the text view.
        content.setTextColor(getResources().getColor(R.color.darkGray));
        content.setTypeface(null, Typeface.BOLD);

        //Set the image to be the media_comment icon
        final ImageView image = (ImageView)extraVisibility.findViewById(R.id.image);
        image.setImageDrawable(ColorKeeper.getColoredDrawable(getContext(), R.drawable.vd_add, Color.GRAY));

        extraVisibility.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (content.getText().toString().equals(getResources().getString(R.string.showExtras))) {
                    content.setText(getResources().getString(R.string.hideExtras));

                    image.setImageDrawable(ColorKeeper.getColoredDrawable(getContext(), R.drawable.vd_minus, Color.GRAY));

                    for (int i = 1; i < extras.getChildCount(); i++) {
                        extras.getChildAt(i).setVisibility(View.VISIBLE);
                    }
                } else {
                    content.setText(getResources().getString(R.string.showExtras));

                    image.setImageDrawable(ColorKeeper.getColoredDrawable(getContext(), R.drawable.vd_add, Color.GRAY));

                    for (int i = 1; i < extras.getChildCount(); i++) {
                        extras.getChildAt(i).setVisibility(View.GONE);
                    }
                }
            }
        });

        extras.addView(extraVisibility, 0);
    }

    /**
     * @param extras
     * @param message
     * @param mediaComment
     */
    private void populateMediaComments(final LinearLayout extras,
                                       SubmissionComment message, final MediaComment mediaComment) {
        //Inflate the view.
        RelativeLayout extra = (RelativeLayout)(inflater.inflate(R.layout.detailed_conversation_extras, null));

        //the text should be the display name
        TextView content = extra.findViewById(R.id.content);
        String displayName = mediaComment.getDisplayName();
        if (displayName == null || "null".equals(displayName)) {
            displayName = mediaComment.get_fileName();
        }
        content.setText(displayName);

        //Underline the text view.
        content.setPaintFlags(content.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        //Make it blue
        content.setTextColor(getResources().getColor(R.color.real_blue));

        //Set the image to be the media_comment icon
        ImageView image = extra.findViewById(R.id.image);
        if(mediaComment.getMediaType() == MediaComment.MediaType.VIDEO) {
            image.setImageDrawable(ColorKeeper.getColoredDrawable(getContext(), R.drawable.vd_media, getResources().getColor(R.color.defaultTextGray)));
        } else {
            image.setImageDrawable(ColorKeeper.getColoredDrawable(getContext(), R.drawable.vd_audio, getResources().getColor(R.color.defaultTextGray)));
        }

        //Add a divider
        View divider = new View(getActivity());
        divider.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,1));
        divider.setBackgroundColor(getResources().getColor(R.color.gray));

        //Set hidden mimetype url display name. filename. id
        extra.findViewById(R.id.mime).setTag(mediaComment.getContentType());
        extra.findViewById(R.id.url).setTag(mediaComment.getUrl());
        extra.findViewById(R.id.file).setTag(mediaComment.getMediaId() + "." + FileUtils.getFileExtensionFromMimetype(mediaComment.getContentType()));
        extra.findViewById(R.id.display).setTag(displayName);

        //allow long presses to show context menu
        registerForContextMenu(extra);

        extra.setOnClickListener(new OnClickListener(){

            @Override
            public void onClick(View v) {
                openMedia(mediaComment.getContentType(), mediaComment.getUrl(), mediaComment.getMediaId() + "." + FileUtils.getFileExtensionFromMimetype(mediaComment.getContentType()));
            }});

        //Add the view
        divider.setVisibility(View.GONE);
        extra.setVisibility(View.GONE);

        extras.addView(divider);
        extras.addView(extra);
    }

    /**
     * @param extras
     * @param atts
     */
    private void populateAttachments(final LinearLayout extras,
                                     final List<Attachment> atts) {
        for(int i = 0; i < atts.size(); i++)
        {
            //Inflate the view
            RelativeLayout extra = (RelativeLayout)(inflater.inflate(R.layout.detailed_conversation_extras, null));

            //The text should be the display name
            TextView content = (TextView) extra.findViewById(R.id.content);
            content.setText(atts.get(i).getDisplayName());

            //underline it.
            content.setPaintFlags(content.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

            //Make it blue
            content.setTextColor(getResources().getColor(R.color.real_blue));

            //Set the image to be the conversation_attachment icon
            ImageView image = (ImageView)extra.findViewById(R.id.image);
            image.setImageResource(R.drawable.vd_attachment);

            //Set hidden mimetype url display name. filename. id
            extra.findViewById(R.id.mime).setTag(atts.get(i).getDisplayName());
            extra.findViewById(R.id.url).setTag(atts.get(i).getUrl());
            extra.findViewById(R.id.file).setTag(atts.get(i).getFilename());
            extra.findViewById(R.id.display).setTag(atts.get(i).getDisplayName());

            //Add a divider
            View divider = new View(getActivity());
            divider.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,1));
            divider.setBackgroundColor(getResources().getColor(R.color.gray));

            //Add the view
            divider.setVisibility(View.GONE);
            extra.setVisibility(View.GONE);
            extras.addView(divider);
            extras.addView(extra);

            final int j = i;

            extra.setOnClickListener(new OnClickListener(){

                @Override
                public void onClick(View v) {
                    openMedia(atts.get(j).getContentType(),atts.get(j).getUrl(), atts.get(j).getFilename());
                }});

            //allows context menu to appear on long press
            registerForContextMenu(extra);

        }
    }

    private void populateAdapter(Submission result) {
        //update ui here with results
        //no connection
        if (result == null) {
            hasSubmissions = false;

            showToast(R.string.noDataConnection);
            return;
        }

        hasSubmissions = result.hasRealSubmission();


        if(result.getGrade() != null && !result.getGrade().equals("null")) {
            hasGrade = true;
            grade = result.getGrade();
        }

        //update the parent assignment with the new submission
        if (result.getSubmittedAt() != null) {
            //"xml" should hold the latest submission
            if (submissionDetailsFragmentCallback != null) {
                submissionDetailsFragmentCallback.updateSubmissionDate(result.getSubmittedAt());
            }
        }

        adapter.subList.clear();
        //now see if there are any submission history
        //a submission history is just a submission
        for (Submission submission : result.getSubmissionHistory()) {
            //we don't want to include submissions that aren't actually submitted.  For instance, when a comment
            //is made there is a submission created, even though the submission is basically empty

            //also, we don't want to include an assignment if there is no submission date or type.
            //Canvas creates a dummy submission to show the grade, but if there isn't a type or a date, it's pointless to show it in the list as there is nothing to show.
            boolean hasNoSubmission = submission == null || submission.getSubmittedAt() == null || submission.getSubmissionType() == null;

            if (!hasNoSubmission && !"unsubmitted".equals(submission.getWorkflowState())) {
                adapter.subList.add(submission);
            }
        }

        //now all all the comments.  Comments aren't just a submission, but we're going to create a submission
        //and set the comment as its comment. We're using the subList as the list backing the adapter, and
        //we need to sort them by date, and this is a fairly straightforward way of doing it

        for(int i = 0; i < result.getSubmissionComments().size(); i++) {
            Submission newSub = new Submission();
            ArrayList<SubmissionComment> comments = new ArrayList<SubmissionComment>();
            comments.add(result.getSubmissionComments().get(i));
            newSub.setSubmittedAt(result.getSubmissionComments().get(i).getCreatedAt());
            newSub.setSubmissionComments(comments);
            adapter.subList.add(newSub);
        }

        //now sort the list so comments and submissions are interleaved
        Collections.sort(adapter.subList, new Comparator<Submission>() {
            @Override
            public int compare(Submission lhs, Submission rhs) {
                if (lhs.getSubmittedAt() == null && rhs.getSubmittedAt() == null)
                    return 0;
                if (lhs.getSubmittedAt() == null && rhs.getSubmittedAt() != null)
                    return -1;
                if (lhs.getSubmittedAt() != null && rhs.getSubmittedAt() == null)
                    return 1;
                return rhs.getSubmittedAt().compareTo(lhs.getSubmittedAt());
            }
        });
        adapter.notifyDataSetChanged();
    }


    ///////////////////////////////////////////////////////////////////////////
    // Adapter
    ///////////////////////////////////////////////////////////////////////////

    public class AttachmentAdapter extends ArrayAdapter<String> {

        public AttachmentAdapter(@NonNull Context context, int resource, @NonNull String[] objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textView = (TextView) super.getView(position, convertView, parent);

            if (position == 1 && (message.getText().toString().trim().length() != 0 || attachmentIds.size() > 0)) {
                // We want to gray it out if there is a comment already started because it will submit the media comment without text comments and attachments
                textView.setTextColor(getResources().getColor(R.color.defaultTextGray));
            } else {
                textView.setTextColor(getResources().getColor(R.color.defaultTextDark));
            }
            return textView;
        }
    }

    public class SubmissionFileAdapter extends ArrayAdapter<Attachment>{

        private Context context;
        public ArrayList<Attachment> attachmentList;

        public SubmissionFileAdapter(Context context, int layoutResourceId, ArrayList<Attachment> attachmentList) {
            super(context, layoutResourceId, attachmentList);
            this.context = context;
            this.attachmentList = attachmentList;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            final AttachmentHolder holder;

            if(row == null) {
                LayoutInflater inflater = ((Activity) context).getLayoutInflater();

                holder = new AttachmentHolder();
                row = inflater.inflate(R.layout.listview_item_row_attachedfiles, parent, false);
                holder.title = row.findViewById(R.id.fileName);
                holder.trashCan = row.findViewById(R.id.removeFile);
                holder.icon = row.findViewById(R.id.fileIcon);
                row.setTag(holder);
            } else {
                holder = (AttachmentHolder)row.getTag();
            }
            holder.icon.setImageDrawable(ColorKeeper.getColoredDrawable(context, R.drawable.vd_document, course));

            Attachment attachment = attachmentList.get(position);
            if(attachment != null) {

                holder.title.setText(attachment.getDisplayName());
                holder.trashCan.setVisibility(View.GONE);
            }

            return row;
        }


    }
    public class SubmissionAdapter extends ArrayAdapter<Submission>{

        private Context context;
        public ArrayList<Submission> subList;

        public SubmissionAdapter(Context context, int layoutResourceId, ArrayList<Submission> subList) {
            super(context, layoutResourceId, subList);
            this.context = context;
            this.subList = subList;
        }

        @Override
        public int getItemViewType(int position) {
            // determine which layout to use
            //if there are no comments show the submission history view
            if(subList.get(position).getSubmissionComments().size() == 0) {
                return 0;
            }
            else {
                //is the comment author the current user?
                if(subList.get(position).getSubmissionComments().get(0).getAuthorId() == myUserId) {
                    return 2;
                }
                return 1;
            }

        }

        @Override
        public int getViewTypeCount() {
            /*
             * layout 1: submission date and grade (if it is graded)
             * layout 2: submission comments others
             * layout 3: submission comments user
             */
            return 3; // Count of different layouts
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            final SubmissionHolder holder;

            if(row == null) {
                LayoutInflater inflater = ((Activity) context).getLayoutInflater();

                holder = new SubmissionHolder();
                if(getItemViewType(position) == 0) {
                    row = inflater.inflate(R.layout.listview_row_submission_details, parent, false);
                    holder.title = (TextView)row.findViewById(R.id.txtTitle);
                    holder.date = (TextView)row.findViewById(R.id.txtGrade);
                    holder.message = (TextView)row.findViewById(R.id.txtSubmission);
                    holder.image = (ImageView)row.findViewById(R.id.image);
                }
                else if(getItemViewType(position) == 1) {
                    row = inflater.inflate(R.layout.listview_item_row_submission_comments_left, parent, false);
                    holder.extras = (LinearLayout)row.findViewById(R.id.extras);
                    holder.title = (TextView)row.findViewById(R.id.title);
                    holder.date = (TextView)row.findViewById(R.id.date);
                    holder.message = (TextView)row.findViewById(R.id.message);
                    holder.image = (CircleImageView)row.findViewById(R.id.avatar_icon);
                    holder.chatBubble = (LinearLayout)row.findViewById(R.id.chat_bubble);
                }
                else {
                    row = inflater.inflate(R.layout.listview_item_row_submission_comments_right, parent, false);
                    holder.extras = (LinearLayout)row.findViewById(R.id.extras);
                    holder.title = (TextView)row.findViewById(R.id.title);
                    holder.date = (TextView)row.findViewById(R.id.date);
                    holder.message = (TextView)row.findViewById(R.id.message);
                    holder.image = (CircleImageView)row.findViewById(R.id.avatar_icon);
                    holder.chatBubble = (LinearLayout)row.findViewById(R.id.chat_bubble);
                }
                row.setTag(holder);
            }
            else {
                holder = (SubmissionHolder)row.getTag();
            }

            Submission submission = subList.get(position);
            if(submission != null) {
                int color = ColorKeeper.getOrGenerateColor(course);
                int drawable = 0;
                //submission history view
                if(getItemViewType(position) == 0) {
                    if(submission.isGradeMatchesCurrentSubmission() && submission.getGrade() != null && !submission.getGrade().equals("null")) {
                        holder.date.setText(context.getString(R.string.grade) + ": " + submission.getGrade());
                    }
                    //set it to be empty for view recycling purposes
                    else {
                        holder.date.setText("");
                    }
                    if(submission.getSubmissionType() != null) {
                        String subType = submission.getSubmissionType();
                        if(subType.equals("online_text_entry")) {
                            holder.message.setText(context.getString(R.string.subTypeOnlineText));
                            drawable = R.drawable.vd_text_submission;
                        }
                        else if(subType.equals("online_url")) {
                            holder.message.setText(context.getString(R.string.subTypeOnlineURL));
                            drawable = R.drawable.vd_link;
                        }
                        else if(subType.equals("online_upload")) {
                            holder.message.setText(context.getString(R.string.subTypeFileUpload));
                            drawable = R.drawable.vd_document;
                        }
                        else if(subType.equals("media_recording")) {
                            holder.message.setText(context.getString(R.string.subTypeMediaRecording));
                            drawable = R.drawable.vd_attach_media;
                        }
                        else if(subType.equals("online_quiz")) {
                            holder.message.setText(context.getString(R.string.subTypeOnlineQuiz));
                            drawable = R.drawable.vd_quiz;
                        }
                        else if(subType.equals("basic_lti_launch")) {
                            holder.message.setText(context.getString(R.string.sub_type_lti_launch));
                            drawable = R.drawable.vd_lti;
                        }

                        if(drawable != 0) {
                            Drawable d = ColorKeeper.getColoredDrawable(context, drawable, color);
                            holder.image.setImageDrawable(d);
                        }

                    }
                    if(submission.getSubmittedAt() != null) {
                        if(submission.getAttachments().size() > 0) {
                            holder.title.setText(DateHelper.getDateTimeString(context, submission.getSubmittedAt()));
                        }
                        else {
                            holder.title.setText(DateHelper.getDateTimeString(context, submission.getSubmittedAt()));
                        }
                    }
                }
                //submission comment other views
                //view type 1 and 2 use the same data
                else {

                    /*
                     * Clear out everything.
                     */

                    if(holder.extras != null) {
                        holder.extras.removeAllViews();
                    }
                    holder.title.setText("");
                    holder.date.setText("");
                    holder.message.setText("");

                    ProfileUtils.loadAvatarForUser((CircleImageView) holder.image, submission.getSubmissionComments().get(0).getAuthor());

                    //if it's the current user, color the bubble the course color
                    if(getItemViewType(position) == 2) {
                        holder.chatBubble.setBackground(ColorKeeper.getColoredDrawable(context, R.drawable.chat_transparent_right, color));
                    }

                    populateMessage(submission.getSubmissionComments().get(0), submission.getSubmissionComments().get(0).getAuthor(), holder.extras, holder.title, holder.image, holder.date, holder.message);
                }
            }

            Animation animation = AnimationUtils.loadAnimation(getContext(), (position > lastPosition) ? R.anim.up_from_bottom : R.anim.none);
            row.startAnimation(animation);
            if(position > lastPosition) {
                lastPosition = position;
            }

            return row;
        }


    }


    private static class SubmissionHolder {
        LinearLayout extras;
        TextView title;
        ImageView image;
        TextView date;
        TextView message;
        LinearLayout chatBubble;
        int lastPosition;
    }
    private static class AttachmentHolder {
        private TextView title;
        private Button trashCan;
        private ImageView icon;
    }


    ///////////////////////////////////////////////////////////////////////////
    // CallBack
    ///////////////////////////////////////////////////////////////////////////
    public void setupCallbacks() {

        notoriousConfigCallback = new StatusCallback<NotoriousConfig>() {
            @Override
            public void onResponse(@NonNull Response<NotoriousConfig> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                if (!apiCheck()) {
                    return;
                }
                NotoriousConfig notoriousConfig = response.body();
                if (notoriousConfig.isEnabled()) {
                    mediaComment.setEnabled(true);
                }
            }
        };

        // We use a NoNetworkErrorDelegate because sometimes old submissions are deleted.
        // We don't want to display unnecessary croutons.
        canvasCallbackSubmission = new StatusCallback<Submission>() {
            @Override
            public void onResponse(@NonNull Response<Submission> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                if (!apiCheck()) {
                    return;
                }
                if (response.body() != null) {
                    populateAdapter(response.body());
                }
            }
        };

        canvasCallbackMessage = new StatusCallback<Submission>() {

            @Override
            public void onResponse(@NonNull Response<Submission> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                if (!apiCheck()) {
                    return;
                }
                // See if it was successful.
                Submission submission = response.body();
                if (submission != null) {
                    SubmissionComment comment = submission.getSubmissionComments().get(submission.getSubmissionComments().size() - 1);
                    // Our list is a list of Submission, so add the comment to a SubmissionGrade object
                    Submission newSub = new Submission();
                    ArrayList<SubmissionComment> comments = new ArrayList<>();
                    comments.add(comment);
                    newSub.setSubmittedAt(comment.getCreatedAt());
                    newSub.setSubmissionComments(comments);
                    adapter.subList.add(0, newSub);

                    message.setText("");
                } else {
                    showToast(R.string.errorPostingComment);
                }

                adapter.notifyDataSetChanged();

                // Close the keyboard
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(message.getWindowToken(), 0);

                // Enable the send message button again
                submitComment.setVisibility(View.VISIBLE);
                sendingProgressBar.setVisibility(View.GONE);

                // Clear the attachments list
                attachmentsList.clear();
                attachmentIds.clear();
                attachments.clear();
                refreshAttachments();
            }

            @Override
            public void onFail(@Nullable Call<Submission> call, @NonNull Throwable error, @Nullable Response response) {
                // Enable the send message button again if there was an Error
                submitComment.setVisibility(View.VISIBLE);
                sendingProgressBar.setVisibility(View.GONE);
                // Clear the attachments list
                attachmentsList.clear();
                attachmentIds.clear();
                attachments.clear();
                refreshAttachments();
            }
        };


        canvasCallbackLTITool = new StatusCallback<LTITool>() {
            @Override
            public void onResponse(@NonNull Response<LTITool> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                if (!apiCheck()) {
                    return;
                }
                LTITool ltiTool = response.body();
                String url = ltiTool.getUrl();
                // Append platform for quizzes 2 lti tool
                Uri uri = Uri.parse(url).buildUpon()
                        .appendQueryParameter("platform", "android")
                        .build();

                // Do NOT authenticate or the LTI tool won't load.
                // 강의콘텐츠 > 외부 도구로 등록한 유형일 경우에 assignment id도 함께 전달해주기 위하여,
                // InternalWebViewFragment를 생성 할 때에 bundle로 assignment객체도 함께 전달하도록 하였다.
                InternalWebviewFragment.Companion.loadInternalWebView(getActivity(), ((Navigation) getActivity()), InternalWebviewFragment.Companion.createBundle(getCanvasContext(), uri.toString(), ltiTool.getName(), false, false, true, assignment.getUrl(), assignment));
            }

            @Override
            public void onFail(@Nullable Call<LTITool> call, @NonNull Throwable error, @Nullable Response response) {
                // If it wasn't a network Error, then the LTI tool must be expired or invalid.
                if (APIHelper.hasNetworkConnection() && (response == null || (response!= null && response.code() != 504))) {
                    showToast(R.string.invalidExternal);
                }
            }
        };

        canvasCallbackAssignment = new StatusCallback<Assignment>() {

            @Override
            public void onResponse(@NonNull Response<Assignment> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                if(!apiCheck()){
                    return;
                }
                Assignment newAssignment = response.body();
                String authenticationURL;
                if(newAssignment == null) {
                    authenticationURL = null;
                } else {
                    authenticationURL = newAssignment.getUrl();
                }

                // Now get the LTITool
                // This API call handles url being null
                SubmissionManager.getLtiFromAuthenticationUrl(authenticationURL, canvasCallbackLTITool, true);
            }

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
        course = (Course)getCanvasContext();
    }

    @Override
    public boolean allowBookmarking() {
        return false;
    }
}
