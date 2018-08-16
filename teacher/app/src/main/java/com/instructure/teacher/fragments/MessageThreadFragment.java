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

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.instructure.canvasapi2.apis.InboxApi;
import com.instructure.canvasapi2.models.Attachment;
import com.instructure.canvasapi2.models.BasicUser;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.Conversation;
import com.instructure.canvasapi2.models.Course;
import com.instructure.canvasapi2.models.Message;
import com.instructure.canvasapi2.utils.ApiPrefs;
import com.instructure.pandautils.fragments.BaseSyncFragment;
import com.instructure.pandautils.utils.ColorUtils;
import com.instructure.pandautils.utils.Const;
import com.instructure.pandautils.utils.PermissionUtils;
import com.instructure.pandautils.utils.RequestCodes;
import com.instructure.pandautils.utils.ThemePrefs;
import com.instructure.pandautils.utils.ToolbarColorizeHelper;
import com.instructure.pandautils.utils.ViewStyler;
import com.instructure.pandautils.views.AttachmentView;
import com.instructure.teacher.R;
import com.instructure.teacher.activities.InitActivity;
import com.instructure.teacher.adapters.MessageAdapter;
import com.instructure.teacher.adapters.StudentContextFragment;
import com.instructure.teacher.events.ConversationDeletedEvent;
import com.instructure.teacher.events.ConversationUpdatedEvent;
import com.instructure.teacher.events.ConversationUpdatedEventTablet;
import com.instructure.teacher.events.MessageAddedEvent;
import com.instructure.teacher.factory.MessageThreadPresenterFactory;
import com.instructure.teacher.holders.MessageHolder;
import com.instructure.interactions.Identity;
import com.instructure.teacher.interfaces.MessageAdapterCallback;
import com.instructure.teacher.presenters.MessageThreadPresenter;
import com.instructure.interactions.router.Route;
import com.instructure.teacher.router.RouteMatcher;
import com.instructure.teacher.utils.MediaDownloader;
import com.instructure.teacher.utils.ModelExtensions;
import com.instructure.teacher.utils.RecyclerViewUtils;
import com.instructure.teacher.utils.ViewUtils;
import com.instructure.teacher.view.EmptyPandaView;
import com.instructure.teacher.viewinterface.MessageThreadView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.Nullable;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import instructure.androidblueprint.PresenterFactory;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import static android.app.Activity.RESULT_OK;
import static com.instructure.teacher.R.id.recyclerView;

public class MessageThreadFragment extends BaseSyncFragment<Message, MessageThreadPresenter, MessageThreadView, MessageHolder, MessageAdapter> implements MessageThreadView, Identity {

    @BindView(recyclerView) RecyclerView mRecyclerView;
    @BindView(R.id.emptyPandaView) EmptyPandaView mEmptyPandaView;
    @BindView(R.id.swipeRefreshLayout) SwipeRefreshLayout mSwipeRefreshLayout;
    @BindView(R.id.toolbar) Toolbar mToolbar;
    @BindView(R.id.subject) TextView mSubject;
    @BindView(R.id.starred) ImageView mStarred;

    private String conversationScope;

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
    public int layoutResId() {
        return R.layout.fragment_message_thread;
    }

    @Nullable
    @Override
    public Long getIdentity() {
        if(getArguments().containsKey(Const.CONVERSATION)) {
            Conversation conversation = getArguments().getParcelable(Const.CONVERSATION);
            if(conversation != null) {
                return conversation.getId();
            }
        }
        return 0L;
    }

    @Override
    public boolean getSkipCheck() {
        return false;
    }

    @Override
    public void onCreateView(View view) {
        ButterKnife.bind(this, view);
    }

    @Override
    protected void onReadySetGo(MessageThreadPresenter presenter) {
        if(mRecyclerView.getAdapter() == null && getConversation() != null) {
            mRecyclerView.setAdapter(getAdapter());
        }

        //set to true so we actually mark the conversation as read
        presenter.loadData(true);
        mEmptyPandaView.setLoading();
    }

    @Override
    protected PresenterFactory<MessageThreadPresenter> getPresenterFactory() {
        if (getArguments() != null && getArguments().containsKey(Const.CONVERSATION_ID)) {
            // We are coming from a push notification
            return new MessageThreadPresenterFactory(getArguments().getLong(Const.CONVERSATION_ID, 0)); // No way to know from the push notification where in the conversation we should be; position = 0
        }

        return new MessageThreadPresenterFactory((Conversation) getArguments().getParcelable(Const.CONVERSATION), getArguments().getInt(Const.POSITION));
    }

    @Override
    protected void onPresenterPrepared(MessageThreadPresenter presenter) {
        conversationScope = getArguments().getString(Const.SCOPE);

        initToolbar();

        // We may not have the conversation yet (we don't when coming from a push notification)
        if (getConversation() != null) {
            setupConversationDetails();
            setupRecyclerView();
        }
    }

    private void setupRecyclerView() {
        mRecyclerView = RecyclerViewUtils.buildRecyclerView(getActivity().getWindow().getDecorView().getRootView(), getContext(), getAdapter(),
                getPresenter(), R.id.swipeRefreshLayout, recyclerView, R.id.emptyPandaView, getString(R.string.no_items_to_display_short));
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(linearLayoutManager);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(),
                ((LinearLayoutManager) mRecyclerView.getLayoutManager()).getOrientation());
        dividerItemDecoration.setDrawable(ContextCompat.getDrawable(getContext(),
                R.drawable.item_decorator_gray));
        mRecyclerView.addItemDecoration(dividerItemDecoration);
        addSwipeToRefresh(mSwipeRefreshLayout);
    }

    private void initToolbar() {
        mToolbar.setTitle(R.string.message);
        ViewStyler.themeToolbar(getActivity(), mToolbar, ThemePrefs.getPrimaryColor(), ThemePrefs.getPrimaryTextColor());
        if(getActivity() instanceof InitActivity && getResources().getBoolean(R.bool.is_device_tablet)) {
            //don't have an arrow because going back will close the app
        } else {
            ViewUtils.setupToolbarBackButton(mToolbar, this);
        }
        mToolbar.inflateMenu(R.menu.message_thread);

        if(conversationScope != null && conversationScope.equals("sent")) {
            // we can't archive sent conversations
            MenuItem archive = mToolbar.getMenu().findItem(R.id.archive);
            if(archive != null) {
                archive.setVisible(false);
            }
            MenuItem unarchive = mToolbar.getMenu().findItem(R.id.unarchive);
            if(unarchive != null) {
                unarchive.setVisible(false);
            }
        }

        mToolbar.setOnMenuItemClickListener(mMenuListener);
    }

    @Override
    public void setupConversationDetails() {
        Conversation conversation = getConversation();

        if(mRecyclerView.getAdapter() == null) {
            // If we didn't setup the adapter initially (we didn't start off with the conversation), then do it now
            mRecyclerView.setAdapter(getAdapter());
        }

        setupRecyclerView();

        if (conversation.getSubject() == null || conversation.getSubject().trim().isEmpty()) {
            mSubject.setText(R.string.no_subject);
        } else {
            mSubject.setText(conversation.getSubject());
        }

        mStarred.setImageResource(conversation.isStarred() ? R.drawable.vd_star_filled : R.drawable.vd_star);
        ColorUtils.colorIt(ThemePrefs.getBrandColor(), mStarred.getDrawable());

        Menu menu = mToolbar.getMenu();
        // we don't want the archive option when it is in the sent folder, we've already toggled the visibility of those in initToolbar
        boolean isArchived = conversation.getWorkflowState() == Conversation.WorkflowState.ARCHIVED;
        if(conversationScope == null || !conversationScope.equals("sent")) {
            menu.findItem(R.id.archive).setVisible(!isArchived);
            menu.findItem(R.id.unarchive).setVisible(isArchived);
        }

        // Set theme after menu changes, otherwise menu icons may retain original tint
        final int textColor = ThemePrefs.getPrimaryTextColor();
        ToolbarColorizeHelper.colorizeToolbar(mToolbar, textColor, getActivity());
    }

    @OnClick(R.id.starred)
    void toggleStarred(View v) {
        getPresenter().toggleStarred();
    }

    private Toolbar.OnMenuItemClickListener mMenuListener = new Toolbar.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.archive:
                case R.id.unarchive:
                    getPresenter().toggleArchived();
                    return true;

                case R.id.delete:
                    final AlertDialog dialog = new AlertDialog.Builder(getContext())
                            .setView(R.layout.dialog_delete_conversation)
                            .setNegativeButton(R.string.teacher_cancel, new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            })
                            .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    getPresenter().deleteConversation();
                                }
                            })
                            .create();

                    dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface dialogInterface) {
                            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(ThemePrefs.getButtonColor());
                            dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(ThemePrefs.getButtonColor());
                        }
                    });

                    dialog.show();
                    return true;

                case R.id.reply:
                    Message topMessage = getPresenter().getData().get(0);
                    addMessage(topMessage, true);
                    return true;
                case R.id.replyAll:
                    replyAllMessage();
                    return true;
                case R.id.markAsUnread:
                    getPresenter().markConversationUnread();
                    return true;
                case R.id.forward:
                    Message forwardMessage = getPresenter().getData().get(getPresenter().getData().size()-1);
                    addMessage(forwardMessage, false);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected MessageAdapter getAdapter() {
        if (mAdapter == null) {
            mAdapter = new MessageAdapter(getContext(), getPresenter(), getConversation(), mAdapterCallback);
        }

        return mAdapter;
    }

    private MessageAdapterCallback mAdapterCallback = new MessageAdapterCallback() {
        @Override
        public void onAvatarClicked(BasicUser user) {
            CanvasContext canvasContext = CanvasContext.fromContextCode(getConversation().getContextCode());
            if (canvasContext != null && canvasContext instanceof Course) {
                Bundle bundle = StudentContextFragment.makeBundle(user.getId(), canvasContext.getId(), false);
                RouteMatcher.route(getContext(), new Route(StudentContextFragment.class, null, bundle));
            }
        }

        @Override
        public void onAttachmentClicked(AttachmentView.AttachmentAction action, Attachment attachment) {
            if (action == AttachmentView.AttachmentAction.PREVIEW) {
                ModelExtensions.viewAttachment(attachment, getContext());
            } else if (action == AttachmentView.AttachmentAction.DOWNLOAD) {
                if (PermissionUtils.hasPermissions(getActivity(), PermissionUtils.WRITE_EXTERNAL_STORAGE)) {
                    // Download media
                    MediaDownloader.download(getContext(), attachment.getUrl(), attachment.getFilename(), attachment.getFilename());
                } else {
                    requestPermissions(PermissionUtils.makeArray(PermissionUtils.WRITE_EXTERNAL_STORAGE), PermissionUtils.WRITE_FILE_PERMISSION_REQUEST_CODE);
                }
            }
        }

        @Override
        public void onMessageAction(MessageClickAction action, final Message message) {
            switch (action) {
                case REPLY:
                    addMessage(message, true);
                    break;

                case FORWARD:
                    addMessage(message, false);
                    break;

                case DELETE:
                    final AlertDialog dialog = new AlertDialog.Builder(getContext())
                            .setView(R.layout.dialog_delete_message)
                            .setNegativeButton(R.string.teacher_cancel, new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            })
                            .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    getPresenter().deleteMessage(message);
                                }
                            })
                            .create();

                    dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface dialogInterface) {
                            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(ThemePrefs.getButtonColor());
                            dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(ThemePrefs.getButtonColor());
                        }
                    });

                    dialog.show();
                    break;
            }
        }

        @Override
        public BasicUser getParticipantById(long id) {
            return getPresenter().getParticipantById(id);
        }
    };

    private void replyAllMessage() {
        Bundle args = AddMessageFragment.createBundle(
                true,
                getConversation(),
                getPresenter().getParticipants(),
                getPresenter().getMessageChainForMessage(null),
                null);
        RouteMatcher.route(getContext(), new Route(AddMessageFragment.class, null, args));
    }

    private void addMessage(Message message, boolean isReply) {
        Bundle args = AddMessageFragment.createBundle(
                isReply,
                getConversation(),
                getPresenter().getParticipants(),
                getPresenter().getMessageChainForMessage(message),
                message);
        RouteMatcher.route(getContext(), new Route(AddMessageFragment.class, null, args));
    }

    @NonNull
    @Override
    protected RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    private Conversation getConversation() {
        return getPresenter().getConversation();
    }

    @Override
    public void onRefreshFinished() {
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onRefreshStarted() {
        mEmptyPandaView.setLoading();
    }

    @Override
    public void checkIfEmpty() {
        RecyclerViewUtils.checkIfEmpty(mEmptyPandaView, mRecyclerView, mSwipeRefreshLayout, getAdapter(), getPresenter().isEmpty());
    }

    @Override
    public void refreshConversationData() {
        setupConversationDetails();
    }

    @Override
    public void onConversationDeleted(int position) {
        if(!getResources().getBoolean(R.bool.is_device_tablet)) {
            EventBus.getDefault().postSticky(new ConversationDeletedEvent(position, InboxFragment.class.getSimpleName() + ".onPost()"));
        } else {
            EventBus.getDefault().postSticky(new ConversationDeletedEvent(position, InboxFragment.class.getSimpleName() + ".onResume()"));
        }

        //only go back a screen on phones
        if(!getResources().getBoolean(R.bool.is_device_tablet)) {
            getActivity().onBackPressed();
        }
    }

    @Override
    public void onConversationMarkedAsUnread(int position) {
        if(!getResources().getBoolean(R.bool.is_device_tablet)) {
            EventBus.getDefault().postSticky(new ConversationUpdatedEvent(getPresenter().getConversation(), InboxApi.Scope.UNREAD, null));
        } else {
            EventBus.getDefault().postSticky(new ConversationUpdatedEventTablet(position, InboxApi.Scope.UNREAD, null));
        }

        //only go back a screen on phones
        if(!getResources().getBoolean(R.bool.is_device_tablet)) {
            getActivity().onBackPressed();
        }
    }

    @Override
    public void onConversationRead(int position) {
        if(!getResources().getBoolean(R.bool.is_device_tablet)) {
            EventBus.getDefault().postSticky(new ConversationUpdatedEvent(getPresenter().getConversation(), InboxApi.Scope.UNREAD, null));
        } else {
            EventBus.getDefault().postSticky(new ConversationUpdatedEventTablet(position, InboxApi.Scope.UNREAD, null));
        }
    }

    @Override
    public void onMessageDeleted() {
        //update the thread so the reply button is at the top thread
        getPresenter().refresh(true);
    }

    @Override
    public void onConversationArchived(int position) {
        if(!getResources().getBoolean(R.bool.is_device_tablet)) {
            EventBus.getDefault().postSticky(new ConversationUpdatedEvent(getPresenter().getConversation(), InboxApi.Scope.ARCHIVED, null));
        } else {
            EventBus.getDefault().postSticky(new ConversationUpdatedEventTablet(position, InboxApi.Scope.ARCHIVED, null));
        }

        //only go back a screen on phones
        if(!getResources().getBoolean(R.bool.is_device_tablet)) {
            getActivity().onBackPressed();
        }
    }

    @Override
    public void onConversationStarred(int position) {
        if(!getResources().getBoolean(R.bool.is_device_tablet)) {
            EventBus.getDefault().postSticky(new ConversationUpdatedEvent(getPresenter().getConversation(), InboxApi.Scope.STARRED, null));
        } else {
            EventBus.getDefault().postSticky(new ConversationUpdatedEventTablet(position, InboxApi.Scope.STARRED, null));
        }
    }

    @Override
    public void showUserMessage(int userMessageResId) {
        Toast.makeText(getContext(), userMessageResId, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RequestCodes.COMPOSE_MESSAGE && resultCode == RESULT_OK) {
            if (mSwipeRefreshLayout != null && getPresenter() != null) {
                mSwipeRefreshLayout.setRefreshing(true);
                getPresenter().refresh(true);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onMessageEdited(MessageAddedEvent event) {
        event.once(getClass().getSimpleName() + getPresenter().getConversation().getId() + "_" + getPresenter().getConversation().getMessageCount(), new Function1<Boolean, Unit>() {
            @Override
            public Unit invoke(Boolean aBoolean) {
                if(aBoolean) {
                    if (mSwipeRefreshLayout != null && getPresenter() != null) {
                        mSwipeRefreshLayout.setRefreshing(true);
                        getPresenter().refresh(true);
                    }
                }
                return null;
            }
        });
    }

    @Override
    protected int perPageCount() {
        return ApiPrefs.getPerPageCount();
    }

    public static Bundle createBundle(Conversation conversation, int position, String scope) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(Const.CONVERSATION, conversation);
        bundle.putInt(Const.POSITION, position);
        bundle.putString(Const.SCOPE, scope);
        return bundle;
    }

    public static Bundle createBundle(long conversationId) {
        // For when we get a push notification for a new message in a conversation
        Bundle bundle = new Bundle();
        bundle.putLong(Const.CONVERSATION_ID, conversationId);
        return bundle;
    }

    public static MessageThreadFragment newInstance(Bundle bundle) {
        MessageThreadFragment messageThreadActivity = new MessageThreadFragment();
        messageThreadActivity.setArguments(bundle);
        return messageThreadActivity;
    }
}
