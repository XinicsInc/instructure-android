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

@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.instructure.candroid.activity

import android.annotation.TargetApi
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.design.internal.BottomNavigationItemView
import android.support.design.internal.BottomNavigationMenuView
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v4.view.MenuItemCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.instructure.candroid.R
import com.instructure.candroid.dialog.BookmarkCreationDialog
import com.instructure.candroid.events.CoreDataFinishedLoading
import com.instructure.candroid.events.ShowGradesToggledEvent
import com.instructure.candroid.events.UserUpdatedEvent
import com.instructure.candroid.fragment.*
import com.instructure.candroid.model.PushNotification
import com.instructure.candroid.tasks.LogoutAsyncTask
import com.instructure.candroid.tasks.SwitchUsersAsyncTask
import com.instructure.candroid.util.*
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.LaunchDefinition
import com.instructure.canvasapi2.models.User
import com.instructure.canvasapi2.utils.APIHelper
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.canvasapi2.utils.Logger
import com.instructure.canvasapi2.utils.MasqueradeHelper
import com.instructure.canvasapi2.utils.weave.weave
import com.instructure.interactions.FragmentInteractions
import com.instructure.interactions.Navigation
import com.instructure.loginapi.login.dialog.MasqueradingDialog
import com.instructure.pandautils.dialogs.UploadFilesDialog
import com.instructure.pandautils.receivers.PushExternalReceiver
import com.instructure.pandautils.utils.*
import com.instructure.pandautils.utils.Const
import com.instructure.pandautils.utils.Const.LANGUAGES_PENDING_INTENT_ID
import com.instructure.pandautils.utils.Const.LANGUAGES_PENDING_INTENT_KEY
import kotlinx.android.synthetic.main.activity_navigation.*
import kotlinx.android.synthetic.main.loading_canvas_view.*
import kotlinx.android.synthetic.main.navigation_drawer.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class NavigationActivity : BaseRouterActivity(), Navigation, MasqueradingDialog.OnMasqueradingSet {

    private var debounceJob: Job? = null
    private var drawerItemSelectedJob: Job? = null
    private var mDrawerToggle: ActionBarDrawerToggle? = null

    //endregion

    override fun contentResId(): Int {
        return R.layout.activity_navigation
    }

    private val isDrawerOpen: Boolean
        get() = !(drawerLayout == null || navigationDrawer == null) && drawerLayout.isDrawerOpen(navigationDrawer)

    private val mNavigationDrawerItemClickListener = View.OnClickListener { v ->
        drawerItemSelectedJob = weave {
            closeNavigationDrawer()
            delay(250)
            when (v.id) {
                R.id.navigationDrawerItem_files -> {
                    val user = ApiPrefs.user
                    if (user != null) {
                        val bundle = FileListFragment.createBundle(0, CanvasContext.currentUserContext(user))
                        addFragment(FragUtils.getFrag(FileListFragment::class.java, bundle))
                    }
                }
                R.id.navigationDrawerItem_gauge -> {
                    val launchDefinition = v.tag as? LaunchDefinition
                    if (launchDefinition != null) startActivity(GaugeActivity.createIntent(this@NavigationActivity, launchDefinition))
                }
                R.id.navigationDrawerItem_bookmarks -> addFragment(BookmarksFragment.newInstance {
                    RouterUtils.routeUrl(this@NavigationActivity, it.url, true)
                }, true)
                R.id.navigationDrawerItem_changeUser -> SwitchUsersAsyncTask().execute()
                R.id.navigationDrawerItem_logout -> {
                    AlertDialog.Builder(this@NavigationActivity)
                            .setTitle(R.string.logout_warning)
                            .setPositiveButton(android.R.string.yes) { _, _ -> LogoutAsyncTask().execute() }
                            .setNegativeButton(android.R.string.no, null)
                            .create()
                            .show()
                }
                R.id.navigationDrawerItem_startMasquerading -> {
                    MasqueradingDialog.show(supportFragmentManager, ApiPrefs.domain, null, !isTablet)
                }
                R.id.navigationDrawerItem_stopMasquerading -> {
                    MasqueradeHelper.stopMasquerading(NavigationActivity.startActivityClass)
                }
                R.id.navigationDrawerSettings -> startActivity(Intent(applicationContext, SettingsActivity::class.java))
            }
        }
    }

    private val onBackStackChangedListener = FragmentManager.OnBackStackChangedListener {
        currentFragment?.let {
            //Sends a broadcast event to notify the backstack has changed and which fragment class is on top.
            OnBackStackChangedEvent(it::class.java).post()
            applyCurrentFragmentTheme()
        }
    }

    override fun onResume() {
        super.onResume()
        applyCurrentFragmentTheme()
    }

    private fun applyCurrentFragmentTheme() {
        Handler().post {
            (currentFragment as? FragmentInteractions)?.let {
                it.applyTheme()
                setBottomBarItemSelected(it as Fragment, it)
            }
        }
    }

    private val mLocaleChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Intent.ACTION_LOCALE_CHANGED == intent.action) {
                //Locale changed, finish the app so it starts fresh when they come back.
                //We do this to stop a Toolbar bug which causes the toolbar to become unresponsive when a locale is changed.
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportFragmentManager.addOnBackStackChangedListener(onBackStackChangedListener)

        if (savedInstanceState == null) {
            if (hasUnreadPushNotification(intent.extras)) {
                handlePushNotification(hasUnreadPushNotification(intent.extras))
            }
        }

        AppShortcutManager.make(this)
    }

    override fun initialCoreDataLoadingComplete() {
        //We are ready to load our UI
        if (currentFragment == null) {
            loadLandingPage(true)
        }

        if(ApiPrefs.user == null) {
            /* Hard case to repo but it's possible for a user to force exit the app
               before we finish saving the user but they will still launch into the app.
               if that happens, log out. */
            LogoutAsyncTask().execute()
        }
        setupBottomNavigation()

        //There is a chance our fragment may attach before we have our core data back.
        EventBus.getDefault().post(CoreDataFinishedLoading)
        applyCurrentFragmentTheme()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
        registerReceiver(mLocaleChangeReceiver, IntentFilter(Intent.ACTION_LOCALE_CHANGED))
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
        unregisterReceiver(mLocaleChangeReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        debounceJob?.cancel()
        drawerItemSelectedJob?.cancel()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == UploadFilesDialog.CAMERA_PIC_REQUEST ||
            requestCode == UploadFilesDialog.PICK_FILE_FROM_DEVICE ||
            requestCode == UploadFilesDialog.PICK_IMAGE_GALLERY) {
            //File Dialog Fragment will not be notified of onActivityResult(), alert manually
            OnActivityResults(ActivityResult(requestCode, resultCode, data), null).postSticky()
        }
    }

    override fun loadLandingPage(clearBackStack: Boolean) {
        if(clearBackStack) clearBackStack(DashboardFragment::class.java)
        addFragment(FragUtils.getFrag(DashboardFragment::class.java))

        if (intent.extras?.containsKey(AppShortcutManager.APP_SHORTCUT_PLACEMENT) == true) {
            // Launch to the app shortcut placement
            val placement = intent.extras.getString(AppShortcutManager.APP_SHORTCUT_PLACEMENT)

            // Remove the extra so we don't accidentally launch into the shortcut again.
            intent.extras.remove(AppShortcutManager.APP_SHORTCUT_PLACEMENT)

            when (placement) {
                AppShortcutManager.APP_SHORTCUT_BOOKMARKS -> {
                    addFragment(BookmarksFragment.newInstance { RouterUtils.routeUrl(this, it.url, true) },true)
                }
                AppShortcutManager.APP_SHORTCUT_CALENDAR -> {
                    addFragment(FragUtils.getFrag(CalendarListViewFragment::class.java), true)
                }
                AppShortcutManager.APP_SHORTCUT_TODO -> {
                    addFragment(FragUtils.getFrag(ToDoListFragment::class.java), true)
                }
                AppShortcutManager.APP_SHORTCUT_NOTIFICATIONS -> {
                    addFragment(FragUtils.getFrag(NotificationListFragment::class.java), true)
                }
                AppShortcutManager.APP_SHORTCUT_INBOX -> {
                    addFragment(FragUtils.getFrag(InboxFragment::class.java), true)
                }
                else -> {
                    addFragment(FragUtils.getFrag(DashboardFragment::class.java), true)
                }
            }
        }
    }

    override fun showHomeAsUp(): Boolean {
        return false
    }

    override fun showTitleEnabled(): Boolean {
        return true
    }

    override fun onUpPressed() {}

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        //Switching languages will trigger this, so we check for our Pending intent id
        if (intent.hasExtra(LANGUAGES_PENDING_INTENT_KEY) && intent.getIntExtra(LANGUAGES_PENDING_INTENT_KEY, 0) != LANGUAGES_PENDING_INTENT_ID) {
            handlePushNotification(hasUnreadPushNotification(intent.extras))
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        //Setup the actionbar but make sure we call super last so the fragments can override it as needed.
        mDrawerToggle?.onConfigurationChanged(newConfig)
        super.onConfigurationChanged(newConfig)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle?.syncState()
    }

    //region Navigation Drawer

    private fun setupUserDetails(user: User?) {
        if (user != null) {
            navigationDrawerUserName.text = user.shortName
            navigationDrawerUserEmail.text = user.primaryEmail

            if(ProfileUtils.shouldLoadAltAvatarImage(user.avatarUrl)) {
                val initials = ProfileUtils.getUserInitials(user.shortName ?: "")
                val color = ContextCompat.getColor(context, R.color.avatarGray)
                val drawable = TextDrawable.builder()
                        .beginConfig()
                        .height(context.resources.getDimensionPixelSize(R.dimen.profileAvatarSize))
                        .width(context.resources.getDimensionPixelSize(R.dimen.profileAvatarSize))
                        .toUpperCase()
                        .useFont(Typeface.DEFAULT_BOLD)
                        .textColor(color)
                        .endConfig()
                        .buildRound(initials, Color.WHITE)
                navigationDrawerProfileImage.setImageDrawable(drawable)
            } else {
                Glide.with(context).load(user.avatarUrl).into(navigationDrawerProfileImage)
            }
        }
    }

    private fun closeNavigationDrawer() {
        drawerLayout?.closeDrawer(navigationDrawer)
    }

    private fun openNavigationDrawer() {
        drawerLayout?.openDrawer(navigationDrawer)
    }

    override fun <F> attachNavigationDrawer(fragment: F, toolbar: Toolbar) where F : Fragment, F : FragmentInteractions {
        ColorUtils.colorIt(ThemePrefs.primaryColor, navigationDrawerInstitutionImage.background)
        navigationDrawerInstitutionImage.loadUri(Uri.parse(ThemePrefs.logoUrl), R.mipmap.ic_launcher_foreground)

        //Navigation items
        navigationDrawerItem_files.setOnClickListener(mNavigationDrawerItemClickListener)
        navigationDrawerItem_gauge.setOnClickListener(mNavigationDrawerItemClickListener)
        navigationDrawerItem_bookmarks.setOnClickListener(mNavigationDrawerItemClickListener)
        navigationDrawerItem_changeUser.setOnClickListener(mNavigationDrawerItemClickListener)
        navigationDrawerItem_logout.setOnClickListener(mNavigationDrawerItemClickListener)
        navigationDrawerSettings.setOnClickListener(mNavigationDrawerItemClickListener)
        navigationDrawerItem_startMasquerading.setOnClickListener(mNavigationDrawerItemClickListener)
        navigationDrawerItem_stopMasquerading.setOnClickListener(mNavigationDrawerItemClickListener)

        //Load Show Grades
        navigationDrawerShowGradesSwitch.isChecked = StudentPrefs.showGradesOnCard
        navigationDrawerShowGradesSwitch.setOnCheckedChangeListener { _, isChecked ->
            StudentPrefs.showGradesOnCard = isChecked
            EventBus.getDefault().post(ShowGradesToggledEvent)
        }
        ViewStyler.themeSwitch(this@NavigationActivity, navigationDrawerShowGradesSwitch, ThemePrefs.brandColor)

        //Load version
        try {
            val navigationDrawerVersion = findViewById<TextView>(R.id.navigationDrawerVersion)
            navigationDrawerVersion.text = String.format(getString(R.string.version),
                    packageManager.getPackageInfo(applicationInfo.packageName, 0).versionName)
        } catch (e: PackageManager.NameNotFoundException) {
            Logger.e("Error getting version: " + e)
        }

        toolbar.setNavigationIcon(R.drawable.vd_hamburger)
        toolbar.navigationContentDescription = getString(R.string.navigation_drawer_open)
        toolbar.setNavigationOnClickListener {
            openNavigationDrawer()
        }

        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START)

        mDrawerToggle = object : ActionBarDrawerToggle(this@NavigationActivity, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            override fun onDrawerOpened(drawerView: View?) {
                super.onDrawerOpened(drawerView)
                invalidateOptionsMenu()
            }

            override fun onDrawerClosed(drawerView: View?) {
                super.onDrawerClosed(drawerView)
                invalidateOptionsMenu()
                //make the scrollview that is inside the drawer scroll to the top
                navigationDrawer.scrollTo(0, 0)
            }
        }

        drawerLayout.post { mDrawerToggle!!.syncState() }
        drawerLayout.addDrawerListener(mDrawerToggle!!)

        setupUserDetails(ApiPrefs.user)

        ViewStyler.themeToolbar(this, toolbar, ThemePrefs.primaryColor, ThemePrefs.primaryTextColor)

        navigationDrawerItem_startMasquerading.setVisible(!ApiPrefs.isMasquerading && ApiPrefs.canMasquerade == true)
        navigationDrawerItem_stopMasquerading.setVisible(ApiPrefs.isMasquerading)
    }

    override fun onStartMasquerading(domain: String, userId: Long) {
        MasqueradeHelper.startMasquerading(userId, domain, NavigationActivity::class.java)
    }

    override fun onStopMasquerading() {
        MasqueradeHelper.stopMasquerading(NavigationActivity::class.java)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onUserUpdatedEvent(event: UserUpdatedEvent){
        event.once(javaClass.simpleName) {
            setupUserDetails(it)
        }
    }

    //endregion

    //region Bottom Bar Navigation

    private val bottomBarItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item: MenuItem ->
        when (item.itemId) {
            R.id.bottomNavigationCourses -> addFragment(FragUtils.getFrag(DashboardFragment::class.java))
            R.id.bottomNavigationCalendar -> addFragment(FragUtils.getFrag(CalendarListViewFragment::class.java))
            R.id.bottomNavigationToDo -> addFragment(FragUtils.getFrag(ToDoListFragment::class.java))
            R.id.bottomNavigationNotifications -> addFragment(FragUtils.getFrag(NotificationListFragment::class.java))
            R.id.bottomNavigationInbox -> addFragment(FragUtils.getFrag(InboxFragment::class.java))
        }
        true
    }

    private val bottomBarItemReselectedListener = BottomNavigationView.OnNavigationItemReselectedListener { item: MenuItem ->
        //if the top fragment != courses, calendar, to-do, notifications, inbox then load the item

        var abortReselect = true
        topFragment?.let {
            val currentFragmentClass = it::class.java
            when (item.itemId) {
                R.id.bottomNavigationCourses -> abortReselect = currentFragmentClass.isAssignableFrom(DashboardFragment::class.java)
                R.id.bottomNavigationCalendar -> abortReselect = currentFragmentClass.isAssignableFrom(CalendarListViewFragment::class.java)
                R.id.bottomNavigationToDo -> abortReselect = currentFragmentClass.isAssignableFrom(ToDoListFragment::class.java)
                R.id.bottomNavigationNotifications -> abortReselect = currentFragmentClass.isAssignableFrom(NotificationListFragment::class.java)
                R.id.bottomNavigationInbox -> abortReselect = currentFragmentClass.isAssignableFrom(InboxFragment::class.java)
            }
        }

        if(!abortReselect) {
            when (item.itemId) {
                R.id.bottomNavigationCourses -> addFragment(FragUtils.getFrag(DashboardFragment::class.java))
                R.id.bottomNavigationCalendar -> addFragment(FragUtils.getFrag(CalendarListViewFragment::class.java))
                R.id.bottomNavigationToDo -> addFragment(FragUtils.getFrag(ToDoListFragment::class.java))
                R.id.bottomNavigationNotifications -> addFragment(FragUtils.getFrag(NotificationListFragment::class.java))
                R.id.bottomNavigationInbox -> addFragment(FragUtils.getFrag(InboxFragment::class.java))
            }
        }
    }

    private fun setupBottomNavigation() {
        Logger.d("NavigationActivity:setupBottomNavigation()")
        bottomBar.applyTheme(ThemePrefs.brandColor, ContextCompat.getColor(this, R.color.bottomBarUnselectedItemColor))
        bottomBar.setOnNavigationItemSelectedListener(bottomBarItemSelectedListener)
        bottomBar.setOnNavigationItemReselectedListener(bottomBarItemReselectedListener)
        updateBottomBarContentDescriptions()
    }

    private fun setBottomBarItemSelected(itemId: Int) {
        bottomBar.setOnNavigationItemReselectedListener(null)
        bottomBar.setOnNavigationItemSelectedListener(null)
        bottomBar.selectedItemId = itemId
        bottomBar.setOnNavigationItemSelectedListener(bottomBarItemSelectedListener)
        bottomBar.setOnNavigationItemReselectedListener(bottomBarItemReselectedListener)
        updateBottomBarContentDescriptions(itemId)
        drawerLayout.hideKeyboard()
    }

    private fun updateBottomBarContentDescriptions(itemId: Int = -1) {
        /* Manually apply content description on each MenuItem since BottomNavigationView won't
        automatically set it from either the title or content description specified in the menu xml */
        loop@ bottomBar.menu.items.forEach {
            var title = if (it.itemId == itemId) getString(R.string.selected) + " " + it.title else it.title
            // skip inbox, we set it with the unread count even if there are no new messages
            if(it.itemId != R.id.bottomNavigationInbox) {
                MenuItemCompat.setContentDescription(it, title)
            }
        }
    }

    /**
     * Determines which tab is highlighted in the bottom navigation bar.
     */
    private fun setBottomBarItemSelected(fragment: Fragment, interactions: FragmentInteractions) {
        when(fragment) {
            //Calendar
            is CalendarListViewFragment -> setBottomBarItemSelected(R.id.bottomNavigationCalendar)
            is CalendarEventFragment -> setBottomBarItemSelected(R.id.bottomNavigationCalendar)
            //To-do
            is ToDoListFragment -> setBottomBarItemSelected(R.id.bottomNavigationToDo)
            //Notifications
            is NotificationListFragment-> {
                setBottomBarItemSelected(if(interactions.canvasContext.isCourseOrGroup) R.id.bottomNavigationCourses
                                         else R.id.bottomNavigationNotifications)
            }
            //Inbox
            is InboxFragment,
            is InboxConversationFragment,
            is InboxComposeMessageFragment,
            is InboxRecipientsFragment -> setBottomBarItemSelected(R.id.bottomNavigationInbox)
            //courses
            else -> setBottomBarItemSelected(R.id.bottomNavigationCourses)
        }
    }

    //endregion

    //region Actionbar

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (mDrawerToggle?.onOptionsItemSelected(item) == true) return true

        if (item.itemId == R.id.bookmark) {
            if (!APIHelper.hasNetworkConnection()) {
                Toast.makeText(context, context.getString(R.string.notAvailableOffline), Toast.LENGTH_SHORT).show()
                return true
            }
            addBookmark()
            return true
        } else if (item.itemId == android.R.id.home) {
            //if we hit the x while we're on a detail fragment, we always want to close the top fragment
            //and not have it trigger an actual "back press"
            val topFragment = topFragment
            if (supportFragmentManager.backStackEntryCount > 0) {
                if (topFragment != null) {
                    supportFragmentManager.beginTransaction().remove(topFragment).commit()
                }
                super.onBackPressed()
            } else if (topFragment == null) {
                super.onBackPressed()
            }
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    //endregion

    //region Adding/Removing Fragments

    override fun popCurrentFragment() {
        try {
            supportFragmentManager.popBackStack()
        } catch (e: Exception) {
            Logger.e("Unable to pop current fragment." + e)
        }
    }

    override fun <F> addFragment(fragment: F) where F : Fragment, F : FragmentInteractions {
        addFragment(fragment, false)
    }

    override fun <F> addFragment(fragment: F, ignoreDebounce: Boolean) where F : Fragment, F : FragmentInteractions {
        addFragment(fragment, R.anim.fade_in_quick, R.anim.fade_out_quick, null, null, ignoreDebounce)
    }

    override fun <F> addFragment(fragment: F, inAnimation: Int, outAnimation: Int) where F : Fragment, F : FragmentInteractions {
        addFragment(fragment, inAnimation, outAnimation, null, null, false)
    }

    override fun <F> addFragment(fragment: F, transitionId: Int, sharedElement: View?) where F : Fragment, F : FragmentInteractions {
        addFragment(fragment, R.anim.fade_in_quick, R.anim.fade_out_quick, transitionId, sharedElement, false)
    }

    private fun <F> addFragment(fragment: F,
            inAnimation: Int, outAnimation: Int,
            transitionId: Int?, sharedElement: View?,
            ignoreDebounce: Boolean) where F : Fragment, F : FragmentInteractions {

        if (!ignoreDebounce && debounceJob?.isActive == true) {
            Logger.e("FAILED TO addFragmentToSomething. Too many fragment transactions...")
            return
        }

        debounceJob = weave { delay(200) }

        try {

            val placement = fragment.getFragmentPlacement()

            val ft = supportFragmentManager.beginTransaction()
            setSharedElement(ft, sharedElement)
            when (placement) {
                FragmentInteractions.Placement.MASTER -> {
                    //Check if the fragment is from the navigation drawer and if it's already on top.

                    //If the navigation drawer is open...
                    //AND
                    //The current fragment is NOT a course fragment
                    //AND
                    //if the current fragment is the same as the fragment behind added

                    val currentFragment = supportFragmentManager.findFragmentById(R.id.fullscreen) as? FragmentInteractions?
                    if (isDrawerOpen &&
                            currentFragment != null &&
                            !currentFragment.canvasContext.isCourseOrGroup &&
                            currentFragment::class.java.isAssignableFrom(fragment::class.java)) {
                        closeNavigationDrawer()
                        return
                    }

                    ft.setCustomAnimations(inAnimation, outAnimation)
                    if(currentFragment is Fragment) {
                        ft.hide(currentFragment)
                    }
                    ft.add(R.id.fullscreen, fragment, fragment::class.java.name)
                    ft.addToBackStack(fragment::class.java.name)
                    ft.commitAllowingStateLoss()
                }
                FragmentInteractions.Placement.DETAIL -> {
                    ft.setCustomAnimations(inAnimation, outAnimation)
                    if(currentFragment is Fragment) {
                        ft.hide(currentFragment)
                    }
                    ft.add(R.id.fullscreen, fragment, fragment::class.java.name)
                    ft.addToBackStack(fragment::class.java.name)
                    ft.commitAllowingStateLoss()
                }
                FragmentInteractions.Placement.DIALOG -> {
                    if(fragment is DialogFragment) {
                        if(isTablet) {
                            ft.addToBackStack(fragment::class.java.name)
                            (fragment as DialogFragment).show(ft, fragment::class.java.name)
                        } else {
                            ft.setCustomAnimations(inAnimation, outAnimation)
                            if(currentFragment is Fragment) {
                                ft.hide(currentFragment)
                            }
                            ft.add(R.id.fullscreen, fragment, fragment::class.java.name)
                            ft.addToBackStack(fragment::class.java.name)
                            ft.commitAllowingStateLoss()
                        }
                    }
                }
                FragmentInteractions.Placement.FULLSCREEN -> {
                    if(fragment is DialogFragment) {
                        ft.addToBackStack(fragment::class.java.name)
                        (fragment as DialogFragment).show(ft, fragment::class.java.name)
                    }
                }
            }
            //Tracks the flow of screens in Google Analytics
            Analytics.trackAppFlow(this@NavigationActivity, fragment::class.java)
        } catch (e: IllegalStateException) {
            Logger.e("Could not commit fragment transaction: " + e)
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun setSharedElement(ft: FragmentTransaction, sharedElement: View?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && sharedElement != null) {
            ft.addSharedElement(sharedElement, sharedElement.transitionName)
        }
    }

    //endregion

    //region Back Stack

    override fun onBackPressed() {
        if(isDrawerOpen) {
            closeNavigationDrawer()
            return
        }

        if (supportFragmentManager.backStackEntryCount == 1) {
            //Exits if we only have one fragment
            finish()
            return
        }

        val topFragment = topFragment
        if (topFragment is ParentFragment) {
            if (!topFragment.handleBackPressed()) {
                super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }

    override fun getTopFragment(): Fragment? {
        val stackSize = supportFragmentManager.backStackEntryCount
        if(stackSize > 0) {
            val fragmentTag = supportFragmentManager.getBackStackEntryAt(stackSize - 1)?.name
            return supportFragmentManager.findFragmentByTag(fragmentTag)
        }
        return null
    }

    override fun getPeekingFragment(): Fragment? {
        val stackSize = supportFragmentManager.backStackEntryCount
        if(stackSize > 1) {
            val fragmentTag = supportFragmentManager.getBackStackEntryAt(stackSize - 2)?.name
            return supportFragmentManager.findFragmentByTag(fragmentTag)
        }
        return null
    }

    override fun getCurrentFragment(): Fragment? {
        return supportFragmentManager.findFragmentById(R.id.fullscreen)
    }

    private fun clearBackStack(cls: Class<*>?) {
        val fragment = topFragment
        if (fragment != null && cls != null && fragment::class.java.isAssignableFrom(cls)) {
            return
        }
        try {
            supportFragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        } catch (e: Exception) {
            Logger.e("NavigationActivity: clearBackStack() - Unable to clear backstack. " + e)
        }
    }

    //endregion

    //region Routing

    override fun routeFragment(fragment: ParentFragment) {
        addFragment(fragment, true)
    }

    override fun existingFragmentCount(): Int {
        return supportFragmentManager.backStackEntryCount
    }

    override fun showLoadingIndicator() {
        loadingRoute.visibility = View.VISIBLE
    }

    override fun hideLoadingIndicator() {
        loadingRoute.visibility = View.GONE
    }

    //endregion

    //region Push Notifications

    private fun handlePushNotification(hasUnreadNotifications: Boolean) {
        val intent = intent
        if (intent != null) {
            val extras = intent.extras
            if (extras != null) {
                if (hasUnreadNotifications) {
                    setPushNotificationAsRead()
                }

                val htmlUrl = extras.getString(PushNotification.HTML_URL, "")

                if (!RouterUtils.canRouteInternally(this, htmlUrl, ApiPrefs.domain, true)) {
                    routeFragment(FragUtils.getFrag(NotificationListFragment::class.java))
                }
            }
        }
    }

    private fun hasUnreadPushNotification(extras: Bundle?): Boolean {
        return (extras != null && extras.containsKey(PushExternalReceiver.NEW_PUSH_NOTIFICATION)
                && extras.getBoolean(PushExternalReceiver.NEW_PUSH_NOTIFICATION, false))
    }

    private fun setPushNotificationAsRead() {
        intent.putExtra(PushExternalReceiver.NEW_PUSH_NOTIFICATION, false)
        PushNotification.clearPushHistory(applicationContext)
    }

    //endregion

    override fun gotLaunchDefinitions(launchDefinition: LaunchDefinition?) {
        val gauge = findViewById<View>(R.id.navigationDrawerItem_gauge)
        gauge.visibility = if (launchDefinition != null) View.VISIBLE else View.GONE
        gauge.tag = launchDefinition
    }

    override fun updateCalendarStartDay() {
        //Restarts the CalendarListViewFragment to update the changed start day of the week
        val fragment = supportFragmentManager.findFragmentByTag(CalendarListViewFragment::class.java.name) as? ParentFragment
        if (fragment != null) {
            supportFragmentManager.beginTransaction().remove(fragment).commit()
        }
        addFragment(FragUtils.getFrag(CalendarListViewFragment::class.java))
    }

    override fun addBookmark() {
        val dialog = BookmarkCreationDialog.newInstance(this, topFragment as? FragmentInteractions, peekingFragment as? FragmentInteractions)
        dialog?.show(supportFragmentManager, BookmarkCreationDialog::class.java.simpleName)
    }

    override fun updateUnreadCount(unreadCount: String) {
        // get the view
        val bottomBarNavView = bottomBar?.getChildAt(0)
        // get the inbox item
        val view = (bottomBarNavView as BottomNavigationMenuView).getChildAt(4)

        // create the badge, set the text and color it
        val unreadCountValue = unreadCount.toInt()
        var unreadCountDisplay = unreadCount
        if(unreadCountValue > 99) {
            unreadCountDisplay = getString(R.string.moreThan99)
        } else if(unreadCountValue <= 0) {
            //don't set the badge or display it, remove any badge
            if(view.children.size > 2 && view.children[2] is TextView) {
                (view as BottomNavigationItemView).removeViewAt(2)
            }
            // update content description with no unread count number
            bottomBar.menu.items.find { it.itemId == R.id.bottomNavigationInbox }.let {
                val title = it?.title
                MenuItemCompat.setContentDescription(it, title)
            }
            return
        }

        // update content description
        bottomBar.menu.items.find { it.itemId == R.id.bottomNavigationInbox }.let {
            var title: String = it?.title as String
            title += "$unreadCountValue  "  + getString(R.string.unread)
            MenuItemCompat.setContentDescription(it, title)
        }

        // check to see if we already have a badge created
        with((view as BottomNavigationItemView)) {
            // first child is the imageView that we use for the bottom bar, second is a layout for the label
            if(childCount > 2 && getChildAt(2) is TextView) {
                (getChildAt(2) as TextView).text = unreadCountDisplay
            } else {
                // no badge, we need to create one
                val badge = LayoutInflater.from(context)
                        .inflate(R.layout.unread_count, bottomBar, false)
                (badge as TextView).text = unreadCountDisplay

                ColorUtils.colorIt(resources.getColor(R.color.electricBlueBadge), badge.background)
                addView(badge)
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, NavigationActivity::class.java)
        }

        fun createIntent(context: Context, extras: Bundle): Intent {
            val intent = Intent(context, NavigationActivity::class.java)
            intent.putExtra(Const.EXTRAS, extras)
            return intent
        }

        fun createIntent(context: Context, message: String, messageType: Int): Intent {
            val intent = createIntent(context)
            intent.putExtra(Const.MESSAGE, message)
            intent.putExtra(Const.MESSAGE_TYPE, messageType)
            return intent
        }

        val startActivityClass: Class<out Activity>
            get() = NavigationActivity::class.java
    }
}
