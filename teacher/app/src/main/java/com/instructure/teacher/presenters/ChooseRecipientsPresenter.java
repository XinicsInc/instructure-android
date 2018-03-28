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

package com.instructure.teacher.presenters;

import android.support.annotation.NonNull;
import android.support.v7.util.SortedList;

import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.managers.RecipientManager;
import com.instructure.canvasapi2.models.Recipient;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.LinkHeaders;
import com.instructure.teacher.viewinterface.ChooseRecipientsView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import instructure.androidblueprint.SyncPresenter;
import retrofit2.Response;


public class ChooseRecipientsPresenter extends SyncPresenter<Recipient, ChooseRecipientsView> {

    private HashSet<Recipient> selectedRecipients = new HashSet<>();

    private Stack<StackEntry> mBackStack = new Stack<>();

    public void clearBackStack() {
        mBackStack.clear();
    }

    public boolean popBackStack() {
        if (mBackStack.size() > 1) {
            mBackStack.pop();
            refresh(false);
            return true;
        }
        return false;
    }

    private static class StackEntry {
        public Recipient recipientGroup;

        public StackEntry(Recipient recipient) {
            this.recipientGroup = recipient;
        }
    }

    public ChooseRecipientsPresenter(String rootContextId) {
        super(Recipient.class);

        // Create root recipient group, add to back stack
        Recipient rootContextRecipient = new Recipient();
        rootContextRecipient.setStringId(rootContextId);
        mBackStack.add(new StackEntry(rootContextRecipient));
    }

    @Override
    public void loadData(boolean forceNetwork) {
        if (getData().size() == 0) {
            RecipientManager.searchAllRecipients(forceNetwork, null, getCanvasContextId(), mRecipientCallback);
        }
    }

    @Override
    public void refresh(boolean forceNetwork) {
        onRefreshStarted();
        mRecipientCallback.reset();
        clearData();
        loadData(forceNetwork);
    }

    private StatusCallback<List<Recipient>> mRecipientCallback = new StatusCallback<List<Recipient>>() {
        @Override
        public void onResponse(@NonNull Response<List<Recipient>> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
            int idx;
            getData().beginBatchedUpdates();
            for (Recipient recipient : response.body()) {
                idx = getData().indexOf(recipient);
                if (idx == SortedList.INVALID_POSITION) {
                    getData().add(recipient);
                }
            }
            getData().endBatchedUpdates();
        }

        @Override
        public void onFinished(ApiType type) {
            if (getViewCallback() != null) {
                getViewCallback().onRefreshFinished();
                getViewCallback().checkIfEmpty();
            }
        }
    };

    private String getCanvasContextId() {
        return mBackStack.peek().recipientGroup.getStringId();
    }

    public void setContextRecipient(Recipient recipient) {
        mBackStack.add(new StackEntry(recipient));
        refresh(false);
    }

    public boolean isRecipientSelected(Recipient recipient) {
        return selectedRecipients.contains(recipient);
    }

    public void addAlreadySelectedRecipients(ArrayList<Recipient> recipients) {
        selectedRecipients.addAll(recipients);
    }

    public void addOrRemoveRecipient(Recipient recipient, int position) {
        if (!selectedRecipients.add(recipient)) {
            selectedRecipients.remove(recipient);
        }
        getData().updateItemAt(position, recipient);
    }

    public ArrayList<Recipient> getRecipients() {
        return new ArrayList<>(selectedRecipients);
    }

    @Override
    public int compare(Recipient o1, Recipient o2) {
        // Compare types, should sort by group > metagroup > person
        int result = o1.getRecipientType().ordinal() - o2.getRecipientType().ordinal();

        // Compare by name
        if (result == 0) {
            result = o1.getName().compareToIgnoreCase(o2.getName());
        }

        // Compare by id
        if (result == 0) {
            result = o1.getStringId().compareTo(o2.getStringId());
        }

        return result;
    }

    @Override
    public boolean areItemsTheSame(Recipient item1, Recipient item2) {
        return item1.getStringId().equals(item2.getStringId());
    }
}
