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
package com.instructure.loginapi.login.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.managers.AccountDomainManager;
import com.instructure.canvasapi2.models.AccountDomain;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.LinkHeaders;
import com.instructure.loginapi.login.R;
import com.instructure.loginapi.login.adapter.DomainAdapter;
import com.instructure.loginapi.login.api.zendesk.utilities.ZendeskDialogStyled;
import com.instructure.loginapi.login.util.Const;
import com.instructure.pandautils.utils.ColorUtils;
import com.instructure.pandautils.utils.ViewStyler;

import java.util.List;

import retrofit2.Response;

public abstract class BaseLoginFindSchoolActivity extends AppCompatActivity implements ZendeskDialogStyled.ZendeskDialogResultListener {

    static { AppCompatDelegate.setCompatVectorFromResourcesEnabled(true); }

    private Toolbar mToolbar;
    private EditText mDomainInput;
    private DomainAdapter mDomainAdapter;
    private TextView mNextActionButton;
    private Handler mDelayFetchAccountHandler = new Handler();
    protected @Nullable TextView mWhatsYourSchoolName;
    private TextView mLoginFlowLogout;

    protected abstract @ColorInt int themeColor();
    protected abstract Intent signInActivityIntent(AccountDomain accountDomain);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_school);
        bindViews();
        applyTheme();
    }

    private void bindViews() {
        mToolbar = findViewById(R.id.toolbar);
        mDomainInput = findViewById(R.id.domainInput);
        mWhatsYourSchoolName = findViewById(R.id.whatsYourSchoolName);
        mLoginFlowLogout = findViewById(R.id.loginFlowLogout);
        mToolbar.setNavigationIcon(ContextCompat.getDrawable(this, R.drawable.ic_action_arrow_back));
        mToolbar.setNavigationContentDescription(R.string.close);
        mToolbar.inflateMenu(R.menu.menu_next);
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(item.getItemId() == R.id.next) {
                    validateDomain(new AccountDomain(mDomainInput.getText().toString()));
                    return true;
                }
                return false;
            }
        });
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        AccessibilityManager a11yManager = ((AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE));
        if (a11yManager != null && (a11yManager.isEnabled() || a11yManager.isTouchExplorationEnabled())) {
            mToolbar.setFocusable(true);
            mToolbar.setFocusableInTouchMode(true);
            mToolbar.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mToolbar.requestFocus();
                    mToolbar.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
                }
            }, 500);
        }

        mNextActionButton = findViewById(R.id.next);
        mNextActionButton.setEnabled(false);
        mNextActionButton.setTextColor(ContextCompat.getColor(BaseLoginFindSchoolActivity.this, R.color.login_grayCanvasLogo));

        mDomainInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                validateDomain(new AccountDomain(mDomainInput.getText().toString()));
                return true;
            }
        });

        mDomainInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (mDomainAdapter != null) {
                    mDomainAdapter.getFilter().filter(s);
                    fetchAccountDomains();
                }

                if(mNextActionButton != null) {
                    if (TextUtils.isEmpty(s.toString())) {
                        mNextActionButton.setEnabled(false);
                        mNextActionButton.setTextColor(ContextCompat.getColor(
                                BaseLoginFindSchoolActivity.this, R.color.login_grayCanvasLogo));
                    } else {
                        mNextActionButton.setEnabled(true);
                        mNextActionButton.setTextColor(ContextCompat.getColor(
                                BaseLoginFindSchoolActivity.this, R.color.login_loginFlowBlue));
                    }
                }
            }
        });

        mDomainAdapter = new DomainAdapter(new DomainAdapter.DomainEvents() {
            @Override
            public void onDomainClick(AccountDomain account) {
                mDomainInput.setText(account.getDomain());
                mDomainInput.setSelection(mDomainInput.getText().length());
                validateDomain(account);
            }

            @Override
            public void onHelpClick() {
                ZendeskDialogStyled dialog = new ZendeskDialogStyled();
                dialog.setArguments(ZendeskDialogStyled.createBundle(true, true));
                dialog.show(getSupportFragmentManager(), ZendeskDialogStyled.TAG);
            }
        });

        RecyclerView recyclerView = findViewById(R.id.findSchoolRecyclerView);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, RecyclerView.VERTICAL));
        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        recyclerView.setAdapter(mDomainAdapter);
    }

    /**
     * Override to handle a logout click event. Use the showLogout() method to enable the buttons visibility
     */
    protected void logout() {}

    /**
     * Shows a logout button. Calls from click return to a logout()
     * @param show a value to indicate if the logout button should be shown.
     */
    protected void showLogout(boolean show) {
        mLoginFlowLogout.setVisibility(show ? View.VISIBLE : View.GONE);
        if(show) {
            mLoginFlowLogout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    logout();
                }
            });
        }
    }

    @Nullable
    protected Toolbar getToolbar() {
        return mToolbar;
    }

    @Nullable
    protected EditText getDomainInput() {
        return mDomainInput;
    }

    private void validateDomain(AccountDomain accountDomain) {
        String url = accountDomain.getDomain().toLowerCase().replace(" ", "");

        //if the user enters nothing, try to connect to canvas.instructure.com
        if (url.trim().length() == 0) {
            url = "canvas.instructure.com";
        }

        //if there are no periods, append .instructure.com
        if (!url.contains(".") || url.endsWith(".beta")) {
            url += ".instructure.com";
        }

        //URIs need to to start with a scheme.
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }

        //Get just the host.
        Uri uri = Uri.parse(url);
        url = uri.getHost();

        //Strip off www. if they typed it.
        if (url.startsWith("www.")) {
            url = url.substring(4);
        }

        accountDomain.setDomain(url);

        Intent intent = signInActivityIntent(accountDomain);
        intent.putExtra(Const.CANVAS_LOGIN, getIntent().getExtras().getInt(Const.CANVAS_LOGIN, 0));
        startActivity(intent);
    }

    protected void applyTheme() {
        final int color = themeColor();

        View view =  LayoutInflater.from(this).inflate(R.layout.login_toolbar_icon, null, false);
        ImageView icon = view.findViewById(R.id.loginLogo);
        icon.setImageDrawable(ColorUtils.colorIt(color, icon.getDrawable()));

        mToolbar.addView(view);

        ViewStyler.setStatusBarLight(this);
    }

    /**
     * Handles fetching account domains. Uses a worker runnable and handler to cancel fetching too often.
     */
    private void fetchAccountDomains() {
        mDelayFetchAccountHandler.removeCallbacks(mFetchAccountsWorker);
        mDelayFetchAccountHandler.postDelayed(mFetchAccountsWorker, 500);
    }

    /**
     * Worker thread for fetching account domains.
     */
    private Runnable mFetchAccountsWorker = new Runnable() {
        @Override
        public void run() {
            if(mDomainInput != null) {
                String query = mDomainInput.getText().toString();
                AccountDomainManager.searchAccounts(query, mAccountDomainCallback);
            }
        }
    };

    private StatusCallback<List<AccountDomain>> mAccountDomainCallback = new StatusCallback<List<AccountDomain>>() {

        @Override
        public void onResponse(@NonNull Response<List<AccountDomain>> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
            if(type.isCache()) return;

            List<AccountDomain> domains = response.body();

            boolean isDebuggable = 0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE);

            if (isDebuggable) {
                //put these domains first
                domains.add(0, createAccountForDebugging("mobiledev.instructure.com"));
                domains.add(1, createAccountForDebugging("mobiledev.beta.instructure.com"));
                domains.add(2, createAccountForDebugging("mobileqa.instructure.com"));
                domains.add(3, createAccountForDebugging("mobileqat.instructure.com"));
                domains.add(4, createAccountForDebugging("clare.instructure.com"));
                domains.add(5, createAccountForDebugging("mobileqa.test.instructure.com"));
            }

            //Debuga모드일 때만 우리 사이트를 찾을 수 있도록 도메인 코드를 추가함
            domains.add(6, createAccountForDebugging("http://xgcanvas2.westus2.cloudapp.azure.com"));

            if (mDomainAdapter != null) {
                mDomainAdapter.setItems(domains);
                mDomainAdapter.getFilter().filter(mDomainInput.getText().toString());
            }
        }
    };

    private AccountDomain createAccountForDebugging(String domain) {
        AccountDomain account = new AccountDomain();
        account.setDomain(domain);
        account.setName("@ " + domain);
        account.setDistance(0);
        return account;
    }

    @Override
    protected void onDestroy() {
        if(mDelayFetchAccountHandler != null && mFetchAccountsWorker != null) {
            mDelayFetchAccountHandler.removeCallbacks(mFetchAccountsWorker);
        }
        super.onDestroy();
    }

    //region Help & Support

    @Override
    public void onTicketPost() {
        Toast.makeText(this, R.string.zendesk_feedbackThankyou, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onTicketError() {}

    //endregion
}
