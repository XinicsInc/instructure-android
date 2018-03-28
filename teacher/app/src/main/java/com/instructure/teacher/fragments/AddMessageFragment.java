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
 */

package com.instructure.teacher.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.util.Rfc822Tokenizer;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.ex.chips.RecipientEditTextView;
import com.android.ex.chips.RecipientEntry;
import com.instructure.canvasapi2.models.Attachment;
import com.instructure.canvasapi2.models.BasicUser;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.Conversation;
import com.instructure.canvasapi2.models.Course;
import com.instructure.canvasapi2.models.Group;
import com.instructure.canvasapi2.models.Message;
import com.instructure.canvasapi2.models.Recipient;
import com.instructure.canvasapi2.models.RemoteFile;
import com.instructure.canvasapi2.utils.APIHelper;
import com.instructure.canvasapi2.utils.ApiPrefs;
import com.instructure.pandautils.dialogs.UnsavedChangesExitDialog;
import com.instructure.pandautils.dialogs.UploadFilesDialog;
import com.instructure.pandautils.fragments.BasePresenterFragment;
import com.instructure.pandautils.models.FileSubmitObject;
import com.instructure.pandautils.utils.ColorUtils;
import com.instructure.pandautils.utils.Const;
import com.instructure.pandautils.utils.FileUploadEvent;
import com.instructure.pandautils.utils.FileUploadNotification;
import com.instructure.pandautils.utils.FilesSelected;
import com.instructure.pandautils.utils.ThemePrefs;
import com.instructure.pandautils.utils.ViewStyler;
import com.instructure.pandautils.views.AttachmentLayout;
import com.instructure.pandautils.views.AttachmentView;
import com.instructure.teacher.R;
import com.instructure.teacher.adapters.CanvasContextSpinnerAdapter;
import com.instructure.teacher.adapters.NothingSelectedSpinnerAdapter;
import com.instructure.teacher.adapters.RecipientAdapter;
import com.instructure.teacher.events.ChooseMessageEvent;
import com.instructure.teacher.events.MessageAddedEvent;
import com.instructure.teacher.factory.AddMessagePresenterFactory;
import com.instructure.teacher.presenters.AddMessagePresenter;
import com.instructure.interactions.router.Route;
import com.instructure.teacher.router.RouteMatcher;
import com.instructure.teacher.utils.ViewUtils;
import com.instructure.teacher.viewinterface.AddMessageView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import instructure.androidblueprint.PresenterFactory;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

import static com.instructure.teacher.R.id.recipient;

public class AddMessageFragment extends BasePresenterFragment<AddMessagePresenter, AddMessageView> implements AddMessageView {

    /* Bundle key for boolean indicating whether the user is replying or forwarding */
    private static final String KEY_IS_REPLY = "is_reply";
    private static final String SELECTED_COURSE = "selected_course";

    /* Bundle key for list of participants */
    private static final String KEY_PARTICIPANTS = "participants";
    private static final String MESSAGE_STUDENTS_WHO = "message_students_who";
    private static final String MESSAGE_STUDENTS_WHO_SUBJECT = "message_students_who_subject";
    private static final String MESSAGE_STUDENTS_WHO_CONTEXT_ID = "message_students_context_id";
    private static final String MESSAGE_STUDENTS_WHO_CONTEXT_IS_PERSONAL = "message_students_is_personal";

    private Message mCurrentMessage;
    private CanvasContext mSelectedCourse;
    private RecipientAdapter mChipsAdapter;
    private boolean isNewMessage = false;
    private boolean mSendIndividually = false;
    private boolean mIsMessageStudentsWho = false;
    private boolean mIsPersonalMessage = false;
    private boolean mShouldAllowExit = false;

    @BindView(R.id.scrollView) ScrollView mScrollView;
    /* Toolbar and title views */
    @BindView(R.id.toolbar) Toolbar mToolbar;
    @BindView(R.id.subject) TextView mSubject;
    @BindView(R.id.editSubject) EditText mEditSubject;
    @BindView(R.id.savingProgressBar) ProgressBar mSavingProgressBar;

    /* Recipient views */
    @BindView(recipient) RecipientEditTextView mChipsTextView;
    @BindView(R.id.contacts_image_button) ImageView mContactsButton;
    @BindView(R.id.recipientWrapper) RelativeLayout mRecipientWrapper;

    /* Compose message views */
    @BindView(R.id.message) EditText mMessage;
    @BindView(R.id.attachments) AttachmentLayout mAttachmentLayout;
    @BindView(R.id.spinnerWrapper) RelativeLayout mSpinnerWrapper;
    @BindView(R.id.courseSpinner) Spinner mCourseSpinner;
    @BindView(R.id.sendIndividualSwitch) SwitchCompat mSendIndividualSwitch;
    @BindView(R.id.sendIndividualMessageWrapper) RelativeLayout mSendIndividualWrapper;

    @Override
    public int layoutResId() {
        return R.layout.fragment_add_message;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_message, container, false);
        ButterKnife.bind(this, view);
        isNewMessage = getArguments().getBoolean(Const.COMPOSE_FRAGMENT);
        mIsPersonalMessage = getArguments().getBoolean(MESSAGE_STUDENTS_WHO_CONTEXT_IS_PERSONAL);
        mIsMessageStudentsWho = getArguments().getBoolean(MESSAGE_STUDENTS_WHO);

        if (savedInstanceState != null && !isNewMessage) {

        } else if(isNewMessage) {
            // composing a new message
            mSpinnerWrapper.setVisibility(View.VISIBLE);
            mRecipientWrapper.setVisibility(View.GONE);
            mSubject.setVisibility(View.GONE);
            mEditSubject.setVisibility(View.VISIBLE);
            mSendIndividualWrapper.setVisibility(View.VISIBLE);
            ViewStyler.themeSwitch(getContext(), mSendIndividualSwitch, ThemePrefs.getBrandColor());
            mSendIndividualSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                    mSendIndividually = isChecked;
                }
            });

            if(savedInstanceState != null && savedInstanceState.containsKey(SELECTED_COURSE)) {
                mSelectedCourse = savedInstanceState.getParcelable(SELECTED_COURSE);
            }

        } else {
            mCurrentMessage = getArguments().getParcelable(Const.MESSAGE_TO_USER);

            ViewTreeObserver vto = mChipsTextView.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

                @Override
                public void onGlobalLayout() {
                    if (getPresenter().isReply()) {
                        if(mCurrentMessage != null) {
                            addInitialRecipients(mCurrentMessage.getParticipatingUserIds());
                        } else {
                            addInitialRecipients(getPresenter().getConversation().getAudience());
                        }
                    } else if(mIsMessageStudentsWho) {
                        ArrayList<BasicUser> participants = getArguments().getParcelableArrayList(KEY_PARTICIPANTS);
                        List<Long> ids = new ArrayList<>();
                        if(participants != null) {
                            for (BasicUser user : participants) {
                                ids.add(user.getId());
                            }
                        }
                        addInitialRecipients(ids);
                    }
                    ViewTreeObserver obs = mChipsTextView.getViewTreeObserver();
                    obs.removeOnGlobalLayoutListener(this);
                }
            });
        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(mSelectedCourse != null) {
            outState.putParcelable(SELECTED_COURSE, mSelectedCourse);
        }
    }

    @Override
    protected void onPresenterPrepared(AddMessagePresenter presenter) {

    }

    @Override
    protected PresenterFactory<AddMessagePresenter> getPresenterFactory() {
        Conversation conversation = getArguments().getParcelable(Const.CONVERSATION);
        ArrayList<BasicUser> participants = getArguments().getParcelableArrayList(KEY_PARTICIPANTS);
        ArrayList<Message> messages = getArguments().getParcelableArrayList(Const.MESSAGE);
        boolean isReply = getArguments().getBoolean(KEY_IS_REPLY, false);
        return new AddMessagePresenterFactory(conversation, participants, messages, isReply);
    }

    @Override
    protected void onReadySetGo(AddMessagePresenter presenter) {

        setupToolbar();

        // Set conversation subject
        if(!isNewMessage && !mIsMessageStudentsWho) {
            mSubject.setText(presenter.getConversation().getSubject());
        } else if(mIsMessageStudentsWho) {
            if (mIsPersonalMessage) {
                mSubject.setVisibility(View.GONE);
                mEditSubject.setVisibility(View.VISIBLE);
                mEditSubject.setText(getArguments().getString(MESSAGE_STUDENTS_WHO_SUBJECT));
            } else {
                mSubject.setText(getArguments().getString(MESSAGE_STUDENTS_WHO_SUBJECT));
            }
        }

        // Set up recipients view
        mChipsTextView.setTokenizer(new Rfc822Tokenizer());
        if(mChipsAdapter == null) {
            mChipsAdapter = new RecipientAdapter(getContext());
        }
        if(mChipsTextView.getAdapter() == null) {
            mChipsTextView.setAdapter(mChipsAdapter);
        }
        if(getPresenter().getCourse().getId() != 0) {
            mChipsAdapter.getCanvasRecipientManager().setCanvasContext(getPresenter().getCourse());
        } else if(mSelectedCourse != null) {
            courseWasSelected();
            mChipsAdapter.getCanvasRecipientManager().setCanvasContext(mSelectedCourse);
        }
        ColorUtils.colorIt(ThemePrefs.getButtonColor(), mContactsButton);


        //don't show the contacts button if there is no selected course and there is no context_code from the conversation (shouldn't happen, but it does)
        if(mSelectedCourse == null && getPresenter().getCourse() != null && getPresenter().getCourse().getId() == 0) {
            mContactsButton.setVisibility(View.INVISIBLE);
        }
        mContactsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CanvasContext canvasContext;
                if(getPresenter().getCourse() != null && getPresenter().getCourse().getId() == 0) {
                    //presenter doesn't know what the course is, use the mSelectedCourse instead
                    canvasContext = mSelectedCourse;
                } else {
                    canvasContext = getPresenter().getCourse();
                }

                RouteMatcher.route(getContext(), new Route(ChooseRecipientsFragment.class, canvasContext, ChooseRecipientsFragment.createBundle(canvasContext, getRecipientsFromRecipientEntries())));

            }
        });

        // Ensure attachments are up to date
        refreshAttachments();

        //get courses and groups if this is a new compose message
        if(isNewMessage) {
            getPresenter().getAllCoursesAndGroups(true);
        }

    }

    @Override
    public void addCoursesAndGroups(ArrayList<Course> courses, ArrayList<Group> groups) {
        final CanvasContextSpinnerAdapter adapter = CanvasContextSpinnerAdapter.newAdapterInstance(getContext(), courses, groups);
        mCourseSpinner.setAdapter(new NothingSelectedSpinnerAdapter(adapter, R.layout.spinner_item_nothing_selected, getContext()));
        if (mSelectedCourse != null) {
            mCourseSpinner.setOnItemSelectedListener(null); // prevent listener from firing the when selection is placed
            mCourseSpinner.setSelection(adapter.getPosition(mSelectedCourse) + 1, false); //  + 1 is for the nothingSelected position
            courseWasSelected();
        }
        mCourseSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != 0) { // position zero is nothingSelected prompt
                    CanvasContext canvasContext = adapter.getItem(position - 1); // -1 to account for nothingSelected item
                    if (mSelectedCourse == null || mSelectedCourse.getId() != canvasContext.getId()) {
                        mChipsTextView.removeAllRecipientEntry();
                        mSelectedCourse = canvasContext;
                        courseWasSelected();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void courseWasSelected() {
        mRecipientWrapper.setVisibility(View.VISIBLE);
        mContactsButton.setVisibility(View.VISIBLE);
        getActivity().invalidateOptionsMenu();
        mChipsAdapter.getCanvasRecipientManager().setCanvasContext(mSelectedCourse);

    }

    private void setupToolbar() {
        if(isNewMessage || (mIsMessageStudentsWho && mIsPersonalMessage)) {
            mToolbar.setTitle(R.string.newMessage);
        } else if(mIsMessageStudentsWho) {
            mToolbar.setTitle(R.string.messageStudentsWho);
        } else {
            mToolbar.setTitle(getPresenter().isReply() ? R.string.reply_to_message : R.string.forward_message);
        }

        if (mToolbar.getMenu().size() == 0)
            mToolbar.inflateMenu(R.menu.menu_compose_message_activity);
        mToolbar.getMenu().findItem(R.id.menu_attachment).setVisible(true);
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                if (item.getItemId() == R.id.menu_send) {
                    sendMessage();
                    return true;
                } else if (item.getItemId() == R.id.menu_attachment) {
                    Bundle bundle = UploadFilesDialog.createAttachmentsBundle(new ArrayList<FileSubmitObject>());
                    UploadFilesDialog.show(getFragmentManager(), bundle, new Function1<Integer, Unit>() {
                        @Override
                        public Unit invoke(Integer integer) {
                            return null;
                        }
                    });
                    return true;
                } else {
                    return false;
                }
            }
        });

        ViewStyler.themeToolbarBottomSheet(getActivity(), getResources().getBoolean(R.bool.is_device_tablet), mToolbar, Color.BLACK, false);
        ViewUtils.setupToolbarCloseButton(mToolbar, new Function0<Unit>() {
            @Override
            public Unit invoke() {
                return handleExit();

            }
        });
    }

    @Nullable
    public Unit handleExit() {
        //check to see if the user has made any changes
        if(mSelectedCourse != null || !TextUtils.isEmpty(mEditSubject.getText()) || !TextUtils.isEmpty(mMessage.getText()) || getPresenter().getAttachments().size() > 0) {
            mShouldAllowExit = false;
            UnsavedChangesExitDialog.Companion.show(getActivity().getSupportFragmentManager(), new Function0<Unit>() {
                @Override
                public Unit invoke() {
                    mShouldAllowExit = true;
                    getActivity().onBackPressed();
                    return null;
                }
            });
        } else {
            mShouldAllowExit = true;
            getActivity().onBackPressed();
        }

        return null;
    }

    public boolean shouldAllowExit() {
        return mShouldAllowExit;
    }

    @Override
    public boolean onHandleBackPressed() {
        //see if they have unsent changes
        if(!shouldAllowExit()) {
            handleExit();
            return true;
        }
        return super.onHandleBackPressed();
    }

    @Override
    public void messageSuccess() {
        Toast.makeText(getContext(), R.string.message_sent_successfully, Toast.LENGTH_SHORT).show();
        mShouldAllowExit = true;
        //post a unique skip id in case they come back to this message and send another message
        EventBus.getDefault().postSticky(new MessageAddedEvent(true, null));
        getActivity().onBackPressed();
    }

    @Override
    public void messageFailure() {
        mToolbar.getMenu().findItem(R.id.menu_send).setVisible(true);
        mToolbar.getMenu().findItem(R.id.menu_attachment).setVisible(true);
        mSavingProgressBar.setVisibility(View.GONE);
        Toast.makeText(getContext(), R.string.error_sending_message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void refreshAttachments() {
        mAttachmentLayout.setRemoteFileAttachments(getPresenter().getAttachments(), new Function2<AttachmentView.AttachmentAction, RemoteFile, Unit>() {
            @Override
            public Unit invoke(AttachmentView.AttachmentAction attachmentAction, RemoteFile remoteFile) {
                if (attachmentAction == AttachmentView.AttachmentAction.REMOVE) {
                    getPresenter().removeAttachment(remoteFile);
                }
                return null;
            }
        });
    }

    private boolean isValidNewMessage() {
        if(isNewMessage) {
            if(mSelectedCourse == null) {
                Toast.makeText(getContext(), R.string.no_course_selected, Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        if (mChipsTextView.getSelectedRecipients().size() == 0) {
            Toast.makeText(getContext(), R.string.message_has_no_recipients, Toast.LENGTH_SHORT).show();
            return false;
        } else if (TextUtils.getTrimmedLength(mMessage.getText()) == 0) {
            Toast.makeText(getContext(), R.string.empty_message, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    void sendMessage() {
        // Validate inputs
        if (!isValidNewMessage()) return;

        // Ensure network is available
        if (!APIHelper.hasNetworkConnection()) {
            Toast.makeText(getContext(), AddMessageFragment.this.getString(R.string.not_available_offline), Toast.LENGTH_SHORT).show();
            return;
        }

        // Can't send a message to yourself. Canvas throws a 400, but canvas lets you select yourself as part of a list of people
        if(mChipsTextView.getSelectedRecipients().size() == 1 && mChipsTextView.getSelectedRecipients().get(0).getId() == ApiPrefs.getUser().getId()) {
            Toast.makeText(getContext(), R.string.addMorePeopleToMessage, Toast.LENGTH_SHORT).show();
            return;
        }

        // Make the progress bar visible and the other buttons not there so they can't try to re-send the message multiple times
        mToolbar.getMenu().findItem(R.id.menu_send).setVisible(false);
        mToolbar.getMenu().findItem(R.id.menu_attachment).setVisible(false);
        ViewStyler.themeProgressBar(mSavingProgressBar, Color.BLACK);
        mSavingProgressBar.announceForAccessibility(getString(R.string.sendingSimple));
        mSavingProgressBar.setVisibility(View.VISIBLE);

        // Send message
        if(isNewMessage || mIsMessageStudentsWho) {
            boolean isBulk = false;

            //we need to make sure that the switch is checked AND they have more than one recipient
            if (mChipsTextView.getSelectedRecipients().size() > 1 && mSendIndividually){
                isBulk = true;
            } else {
                for (RecipientEntry entry: mChipsTextView.getSelectedRecipients()){
                    if (entry.getUserCount() > 1 && mSendIndividually) {
                        isBulk = true;
                        break;
                    }
                }
            }
            String contextId;
            String subject;
            if(mIsMessageStudentsWho) {
                mSendIndividually = false;
                contextId = getArguments().getString(MESSAGE_STUDENTS_WHO_CONTEXT_ID, "");
                subject = mIsPersonalMessage ? mEditSubject.getText().toString() : mSubject.getText().toString();
            } else {
                contextId = mSelectedCourse.getContextId();
                subject = mEditSubject.getText().toString();
            }
            //isBulk controls the group vs individual messages, so group message flag is hardcoded to true at the api call
            getPresenter().sendNewMessage(mChipsTextView.getSelectedRecipients(), mMessage.getText().toString(), subject, contextId, isBulk);
        } else {
            getPresenter().sendMessage(mChipsTextView.getSelectedRecipients(), mMessage.getText().toString());
        }
    }


    private void addInitialRecipients(List<Long> initialRecipientIds) {
        List<RecipientEntry> selectedRecipients = mChipsTextView.getSelectedRecipients();

        addRecipient:
        for (Long recipientId : initialRecipientIds) {

            BasicUser recipient = getPresenter().getParticipantById(recipientId);

            if (recipient == null) continue;
            //skip if the user is the current logged in user
            if (ApiPrefs.getUser() != null && recipientId == ApiPrefs.getUser().getId()) continue;
            // Skip if this recipient is already added
            for (RecipientEntry entry : selectedRecipients) {
                if (entry.getDestination().equals(Long.toString(recipient.getId()))) {
                    continue addRecipient;
                }
            }

            // Create RecipientEntry from Recipient and add to list
            RecipientEntry recipientEntry = new RecipientEntry(recipient.getId(), recipient.getName(), Long.toString(recipient.getId()), "", recipient.getAvatarUrl(), 0, 0, true, null, null);
            mChipsTextView.appendRecipientEntry(recipientEntry);

        }
    }

    private void addRecipients(ArrayList<Recipient> newRecipients) {
        List<RecipientEntry> selectedRecipients = mChipsTextView.getSelectedRecipients();
        mChipsTextView.setTokenizer(new Rfc822Tokenizer());
        if(mChipsAdapter == null) {
            mChipsAdapter = new RecipientAdapter(getContext());
        }
        if(mChipsTextView.getAdapter() == null) {
            mChipsTextView.setAdapter(mChipsAdapter);
        }
        addRecipient:
        for (Recipient recipient : newRecipients) {

            // Skip if this recipient is already added
            for (RecipientEntry entry : selectedRecipients) {
                if (entry.getDestination().equals(recipient.getStringId())) {
                    continue addRecipient;
                }
            }

            // Create RecipientEntry from Recipient and add to list
            RecipientEntry recipientEntry = new RecipientEntry(recipient.getIdAsLong(), recipient.getName(), recipient.getStringId(), "", recipient.getAvatarURL(), 0, 0, true,
                    recipient.getCommonCourses() != null ? recipient.getCommonCourses().keySet() : null,
                    recipient.getCommonGroups() != null ? recipient.getCommonGroups().keySet() : null);
            mChipsTextView.appendRecipientEntry(recipientEntry);

        }
    }

    private ArrayList<Recipient> getRecipientsFromRecipientEntries() {
        List<RecipientEntry> selectedRecipients = mChipsTextView.getSelectedRecipients();

        ArrayList<Recipient> recipients = new ArrayList<>();

        for (RecipientEntry entry : selectedRecipients) {
            Recipient recipient = new Recipient();
            recipient.setAvatarURL(entry.getAvatarUrl());
            recipient.setName(entry.getName());
            recipient.setStringId(entry.getDestination());

            recipients.add(recipient);
        }

        return recipients;
    }

    @Override
    public void onRefreshFinished() {}

    @Override
    public void onRefreshStarted() {}

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onRecipientsUpdated(ChooseMessageEvent event) {
        event.once(getClass().getSimpleName(), new Function1<ArrayList<Recipient>, Unit>() {
            @Override
            public Unit invoke(final ArrayList<Recipient> recipients) {
                //need to have the textview laid out first so that the chips view will have a width
                mChipsTextView.post(new Runnable() {

                    @Override
                    public void run() {
                        addRecipients(recipients);
                    }
                });

                return null;
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFileUploadStarted(final FilesSelected event) {
        //update the adapter item
        event.get(new Function1<List<? extends FileSubmitObject>, Unit>() {
            @Override
            public Unit invoke(List<? extends FileSubmitObject> fileSubmitObjects) {
                EventBus.getDefault().removeStickyEvent(event);
                ArrayList<Attachment> attachments = new ArrayList<>(fileSubmitObjects.size());
                for (FileSubmitObject item : fileSubmitObjects) {
                    attachments.add(item.toAttachment());
                }
                getPresenter().addAttachments(attachments);
                refreshAttachments();
                return null;
            }
        });
    }

    public static Bundle createBundle(boolean isReply, Conversation conversation, ArrayList<BasicUser> participants, ArrayList<Message> messages, Message currentMessage) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(KEY_IS_REPLY, isReply);
        bundle.putParcelable(Const.CONVERSATION, conversation);
        bundle.putParcelableArrayList(KEY_PARTICIPANTS, participants);
        bundle.putParcelableArrayList(Const.MESSAGE, messages);
        bundle.putParcelable(Const.MESSAGE_TO_USER, currentMessage);
        return bundle;
    }

    public static Bundle createBundle() {
        Bundle bundle = new Bundle();
        bundle.putBoolean(Const.COMPOSE_FRAGMENT, true);
        return bundle;
    }

    public static Bundle createBundle(ArrayList<BasicUser> users, String subject, String contextId, boolean isPersonal) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(MESSAGE_STUDENTS_WHO_CONTEXT_IS_PERSONAL, isPersonal);
        bundle.putBoolean(MESSAGE_STUDENTS_WHO, true);
        bundle.putParcelableArrayList(KEY_PARTICIPANTS, users);
        bundle.putString(MESSAGE_STUDENTS_WHO_SUBJECT, subject);
        bundle.putString(MESSAGE_STUDENTS_WHO_CONTEXT_ID, contextId);
        return bundle;
    }

    public static AddMessageFragment newInstance(Bundle bundle) {
        AddMessageFragment addMessageActivity = new AddMessageFragment();
        addMessageActivity.setArguments(bundle);
        return addMessageActivity;
    }
}
