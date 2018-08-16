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

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.instructure.candroid.R;
import com.instructure.interactions.Navigation;
import com.instructure.interactions.FragmentInteractions;
import com.instructure.candroid.util.FragUtils;
import com.instructure.candroid.util.Param;
import com.instructure.candroid.view.AutoResizeTextView;
import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.managers.UserManager;
import com.instructure.canvasapi2.models.BasicUser;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.Course;
import com.instructure.canvasapi2.models.Enrollment;
import com.instructure.canvasapi2.models.User;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.pageview.BeforePageView;
import com.instructure.canvasapi2.utils.LinkHeaders;
import com.instructure.canvasapi2.utils.pageview.PageView;
import com.instructure.canvasapi2.utils.pageview.PageViewUrlParam;
import com.instructure.pandautils.utils.ColorKeeper;
import com.instructure.pandautils.utils.Const;
import com.instructure.pandautils.utils.ProfileUtils;
import com.instructure.pandautils.utils.ViewStyler;

import java.util.ArrayList;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Response;

@PageView(url = "{canvasContext}/users/{userId}")
public class PeopleDetailsFragment extends ParentFragment {

    private View rootView;
    private AutoResizeTextView name;
    private CircleImageView userAvatar;
    private TextView bioText;
    private TextView userRole;
    private FrameLayout userBackground;
    private FloatingActionButton composeButton;
    private CardView cardView;
    private RelativeLayout clickContainer;

    private User user;

    @PageViewUrlParam(name = "userId")
    private long userId = -1; // used for routing from a url

    private StatusCallback<User> getCourseUserByIdCallback;

    @Override
    @NonNull
    public String title() {
        return "";
    }

    @Override
    @NonNull
    public FragmentInteractions.Placement getFragmentPlacement() {
        return FragmentInteractions.Placement.DETAIL;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(this, true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = getLayoutInflater().inflate(R.layout.fragment_people_details, container, false);
        clickContainer = rootView.findViewById(R.id.clickContainer);
        name = rootView.findViewById(R.id.userName);
        userAvatar = rootView.findViewById(R.id.avatar);
        //bio
        bioText = rootView.findViewById(R.id.bioText);
        userRole = rootView.findViewById(R.id.userRole);
        userBackground = rootView.findViewById(R.id.userBackground);

        composeButton = rootView.findViewById(R.id.compose);

        int color = ColorKeeper.getOrGenerateColor(getCanvasContext());
        composeButton.setColorNormal(color);
        composeButton.setColorPressed(color);

        composeButton.setIconDrawable(ColorKeeper.getColoredDrawable(getContext(), R.drawable.vd_send, Color.WHITE));
        composeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ArrayList<BasicUser> participants = new ArrayList<>();
                participants.add(BasicUser.userToBasicUser(user));
                Navigation navigation = getNavigation();
                if (navigation != null) {
                    Bundle args = InboxComposeMessageFragment.createBundleNewConversation(participants, getCanvasContext());
                    navigation.addFragment(FragUtils.getFrag(InboxComposeMessageFragment.class, args));
                }
            }
        });
        setupCallbacks();
        if (user == null) {
            UserManager.getUserForContextId(getCanvasContext(), userId, getCourseUserByIdCallback, true);
        } else {
            setupUserViews();
        }

        return rootView;
    }

    @Override
    public void applyTheme() {
        ViewStyler.setStatusBarDark(getActivity(), ColorKeeper.getOrGenerateColor(getCanvasContext()));
    }

    private void setupUserViews() {
        if(user != null){
            name.setText(user.getName());

            ProfileUtils.loadAvatarForUser(userAvatar, user);

            //show the bio if one exists
            if(!TextUtils.isEmpty(user.getBio()) && !user.getBio().equals("null")) {
                bioText.setVisibility(View.VISIBLE);
                bioText.setText(user.getBio());
            }

            String roles = "";
            for(Enrollment enrollment : user.getEnrollments()) {
                roles += enrollment.getType() + " ";
            }
            userRole.setText(roles);

            userBackground.setBackgroundColor(ColorKeeper.getOrGenerateColor(getCanvasContext()));
        }

    }

    private void setupCallbacks() {

        getCourseUserByIdCallback = new StatusCallback<User>() {
            @Override
            public void onResponse(@NonNull Response<User> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                PeopleDetailsFragment.this.user = user;
                setupUserViews();
            }
        };
    }

    @BeforePageView
    @Override
    public void handleIntentExtras(Bundle extras) {
        super.handleIntentExtras(extras);

        if (extras.containsKey(Const.USER)) {
            user = (User) extras.getParcelable(Const.USER);
            userId = user.getId();
        } else if (getUrlParams() != null) {
            userId = parseLong(getUrlParams().get(Param.USER_ID), -1);
        }
    }

    public static Bundle createBundle(User user, CanvasContext canvasContext) {
        Bundle extras = createBundle(canvasContext);
        extras.putParcelable(Const.USER, user);
        return extras;
    }

    @Override
    @NonNull
    public HashMap<String, String> getParamForBookmark() {
        HashMap<String, String> map = super.getParamForBookmark();
        if(user != null) {
            map.put(Param.USER_ID, Long.toString(user.getId()));
        } else if(userId != -1) {
            map.put(Param.USER_ID, Long.toString(userId));
        }
        return map;
    }

    @Override
    public boolean allowBookmarking() {
        return (getCanvasContext() instanceof Course);
    }
}
