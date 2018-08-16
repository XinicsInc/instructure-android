/*
* Copyright (C) 2017 - present Instructure, Inc.
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
package com.instructure.teacher.fragments

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import com.bumptech.glide.Glide
import com.instructure.canvasapi2.utils.APIHelper
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.pandautils.fragments.BaseFragment
import com.instructure.pandautils.utils.*
import com.instructure.teacher.R
import com.instructure.teacher.dialog.NoInternetConnectionDialog
import com.instructure.interactions.router.Route
import com.instructure.teacher.router.RouteMatcher
import com.instructure.teacher.utils.adoptToolbarStyle
import com.instructure.teacher.utils.isTablet
import com.instructure.teacher.utils.setupBackButtonAsBackPressedOnly
import com.instructure.teacher.utils.setupMenu
import kotlinx.android.synthetic.main.fragment_profile.*

class ProfileFragment : BaseFragment() {

    override fun layoutResId() = R.layout.fragment_profile

    override fun onCreateView(view: View) {}

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        profileBanner.setImageResource(
                if(isTablet) R.drawable.teacher_profile_banner_image_tablet
                else R.drawable.teacher_profile_banner_image_phone)
    }

    override fun onResume() {
        super.onResume()
        setupToolbar()
        setupViewableData()
    }

    private fun setupToolbar() {
        toolbar.setupMenu(R.menu.menu_settings_edit, menuItemCallback)
        toolbar.setupBackButtonAsBackPressedOnly(this)
        titleTextView.adoptToolbarStyle(toolbar)
        ViewStyler.themeToolbar(activity, toolbar, ThemePrefs.primaryColor, ThemePrefs.primaryTextColor)
        toolbar.requestAccessibilityFocus()
    }

    private fun setupViewableData() {
        val user = ApiPrefs.user

        if(ProfileUtils.shouldLoadAltAvatarImage(user?.avatarUrl)) {
            val initials = ProfileUtils.getUserInitials(user?.shortName ?: "")
            val color = ContextCompat.getColor(context, R.color.canvas_default_tab_unselected)
            val drawable = TextDrawable.builder()
                    .beginConfig()
                    .height(context.resources.getDimensionPixelSize(R.dimen.profileAvatarSize))
                    .width(context.resources.getDimensionPixelSize(R.dimen.profileAvatarSize))
                    .toUpperCase()
                    .useFont(Typeface.DEFAULT_BOLD)
                    .textColor(color)
                    .endConfig()
                    .buildRound(initials, Color.WHITE)
            usersAvatar.borderColor = ContextCompat.getColor(context, R.color.canvas_default_tab_unselected)
            usersAvatar.borderWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6F, context.resources.displayMetrics).toInt()
            usersAvatar.setImageDrawable(drawable)
        } else {
            Glide.with(context).load(user?.avatarUrl).into(usersAvatar)
        }

        usersName.text = user?.shortName
        usersEmail.text = user?.primaryEmail
        usersBio.text = user?.bio
    }

    val menuItemCallback: (MenuItem) -> Unit = {
        when(it.itemId) {
            R.id.menu_edit -> {
                if(APIHelper.hasNetworkConnection()) {
                    RouteMatcher.route(context, Route(ProfileEditFragment::class.java, ApiPrefs.user))
                } else {
                    NoInternetConnectionDialog.show(fragmentManager)
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(bundle: Bundle) = ProfileFragment().apply {
            arguments = bundle
        }
    }
}
