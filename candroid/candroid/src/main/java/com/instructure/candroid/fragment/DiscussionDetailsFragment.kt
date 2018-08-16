/*
 * Copyright (C) 2018 - present Instructure, Inc.
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
package com.instructure.candroid.fragment

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.instructure.candroid.BuildConfig
import com.instructure.candroid.R
import com.instructure.candroid.events.DiscussionTopicHeaderEvent
import com.instructure.candroid.events.DiscussionUpdatedEvent
import com.instructure.candroid.events.post
import com.instructure.candroid.util.Const
import com.instructure.candroid.util.Param
import com.instructure.candroid.util.RouterUtils
import com.instructure.canvasapi2.StatusCallback
import com.instructure.canvasapi2.managers.DiscussionManager
import com.instructure.canvasapi2.managers.DiscussionManager.deleteDiscussionEntry
import com.instructure.canvasapi2.managers.OAuthManager
import com.instructure.canvasapi2.models.*
import com.instructure.canvasapi2.utils.*
import com.instructure.canvasapi2.utils.pageview.BeforePageView
import com.instructure.canvasapi2.utils.pageview.PageView
import com.instructure.canvasapi2.utils.pageview.PageViewUrlParam
import com.instructure.canvasapi2.utils.pageview.PageViewUrlQuery
import com.instructure.canvasapi2.utils.weave.*
import com.instructure.interactions.FragmentInteractions
import com.instructure.loginapi.login.dialog.NoInternetConnectionDialog
import com.instructure.pandautils.discussions.DiscussionCaching
import com.instructure.pandautils.discussions.DiscussionUtils
import com.instructure.pandautils.utils.*
import com.instructure.pandautils.views.CanvasWebView
import kotlinx.android.synthetic.main.fragment_discussions_details.*
import kotlinx.coroutines.experimental.Job
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import retrofit2.Response
import java.net.URLDecoder
import java.util.*
import java.util.regex.Pattern

@PageView(url = "{canvasContext}/discussion_topics/{topicId}")
class DiscussionDetailsFragment : ParentFragment() {

    private var fetchData: Job? = null
    private var fetchDetailedDiscussion: Job? = null
    private var sessionAuthJob: Job? = null
    private var discussionMarkAsReadJob: Job? = null
    private var discussionLikeJob: Job? = null
    private var discussionsLoadingJob: WeaveJob? = null

    private var discussionTopicHeader: DiscussionTopicHeader by ParcelableArg(DiscussionTopicHeader())
    private var discussionTopic: DiscussionTopic? by NullableParcelableArg()
    private var discussionEntryId: Long by LongArg(default = 0L)
    private var discussionTopicHeaderId: Long by LongArg(default = 0L)
    private var isAnnouncements: Boolean = false
    private var isNestedDetail: Boolean = false
    private var scrollPosition: Int = 0
    private var authenticatedSessionURL: String? = null

    @PageViewUrlParam("topicId")
    private fun getTopicId() = discussionTopicHeader.id

    @PageViewUrlQuery("module_item_id")
    private fun pageViewModuleItemId() = getModuleItemId()

    override fun getFragmentPlacement(): FragmentInteractions.Placement = FragmentInteractions.Placement.DETAIL

    //region Lifecycle

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return layoutInflater.inflate(R.layout.fragment_discussions_details, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        populateDiscussionData()
        swipeRefreshLayout.setOnRefreshListener {
            authenticatedSessionURL = null
            populateDiscussionData(true)
            // Send out bus events to trigger a refresh for discussion list
            DiscussionUpdatedEvent(discussionTopicHeader, javaClass.simpleName).post()
        }
    }

    override fun onPause() {
        super.onPause()
        scrollPosition = discussionsScrollView.scrollY
        discussionTopicHeaderWebView.onPause()
        discussionRepliesWebView.onPause()
    }

    override fun onResume() {
        super.onResume()
        discussionTopicHeaderWebView.onResume()
        discussionRepliesWebView.onResume()

        /* TODO - Comms - 868
        EventBus.getDefault().getStickyEvent(DiscussionTopicHeaderDeletedEvent::class.java)?.once(javaClass.simpleName + ".onResume()") {
            if (it == presenter.discussionTopicHeader.id) {
                if (activity is MasterDetailInteractions) {
                    (activity as MasterDetailInteractions).popFragment(mCanvasContext)
                } else if(activity is FullScreenInteractions) {
                    activity.finish()
                }
            }
        }
        */
    }

    override fun onDestroy() {
        super.onDestroy()
        fetchData?.cancel()
        fetchDetailedDiscussion?.cancel()
        sessionAuthJob?.cancel()
        discussionMarkAsReadJob?.cancel()
        discussionLikeJob?.cancel()
        discussionsLoadingJob?.cancel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        discussionTopicHeaderWebView?.destroy()
        discussionRepliesWebView?.destroy()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    //endregion

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onDiscussionReplyCreated(event: DiscussionEntryEvent) {
        event.once(discussionTopicHeader.id.toString(), {
            populateDiscussionData()

            discussionTopicHeader.incrementDiscussionSubentryCount() //Update subentry count
            discussionTopicHeader.lastReplyAt?.time = Date().time //Update last post time
            DiscussionTopicHeaderEvent(discussionTopicHeader).post()
        })
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBackStackChangedEvent(event: OnBackStackChangedEvent) {
        event.get { clazz ->
            if (clazz?.isAssignableFrom(DiscussionDetailsFragment::class.java) == true) {
                discussionRepliesWebView.onResume()
                discussionTopicHeaderWebView.onResume()
            } else {
                discussionRepliesWebView.onPause()
                discussionTopicHeaderWebView.onPause()
            }
        }
    }

    override fun title(): String {
        return if(discussionTopicHeaderId == 0L) return discussionTopicHeader.title ?: getString(R.string.discussion) else getString(R.string.discussion)
    }

    override fun applyTheme() {
        toolbar.title = title()
        setupToolbarMenu(toolbar)
        toolbar.setupAsBackButton(this)
        /* TODO - Blocked by COMMS - 868
        if(!isAnnouncements && discussionTopicHeader.author.id == ApiPrefs.user?.id && hasEditPermissions) {
            toolbar.setMenu(R.menu.menu_edit_generic, menuItemCallback)
        }
        */
        ViewStyler.themeToolbar(activity, toolbar, canvasContext)
    }

    private val menuItemCallback: (MenuItem) -> Unit = { item ->
        when (item.itemId) {
            R.id.menu_edit -> {
                if(APIHelper.hasNetworkConnection()) {
                    if(!isAnnouncements) {
                        val args = CreateDiscussionFragment.makeBundle(canvasContext, discussionTopicHeader)
                        navigation?.addFragment(CreateDiscussionFragment.newInstance(args))
                    } else {
                        val args = CreateAnnouncementFragment.makeBundle(canvasContext, discussionTopicHeader)
                        navigation?.addFragment(CreateAnnouncementFragment.newInstance(args))
                    }
                } else {
                    NoInternetConnectionDialog.show(fragmentManager)
                }
            }
        }
    }

    private fun determinePermissions() {
        // Might still be needed once COMMS-868 is implemented, TBD
        //TODO: determine what permissions are available to student relative to course and discussion.
    }

    private fun populateDiscussionData(forceRefresh: Boolean = false) {
        discussionsLoadingJob = tryWeave {
            discussionProgressBar.setVisible()
            discussionRepliesWebView.loadHtml("", "")
            discussionRepliesWebView.setInvisible()
            discussionTopicRepliesTitle.setInvisible()
            postBeforeViewingRepliesTextView.setGone()

            // Do we have a discussion topic header? if not fetch it, or if forceRefresh is true force a fetch
            if(forceRefresh) {
                val discussionTopicHeaderId = if(discussionTopicHeaderId == 0L && discussionTopicHeader.id != 0L) discussionTopicHeader.id else discussionTopicHeaderId
                discussionTopicHeader = awaitApi { DiscussionManager.getDetailedDiscussion(canvasContext, discussionTopicHeaderId, it) }
            } else {
                if(discussionTopicHeader.id == 0L) {
                    discussionTopicHeader = awaitApi { DiscussionManager.getDetailedDiscussion(canvasContext, discussionTopicHeaderId, it) }
                }
            }

            determinePermissions()

            loadDiscussionTopicHeaderViews(discussionTopicHeader)

            if(forceRefresh || discussionTopic == null) {
                // forceRefresh is true, fetch the discussion topic
                discussionTopic = awaitApi<DiscussionTopic> { DiscussionManager.getFullDiscussionTopic(canvasContext, discussionTopicHeader.id, true, it) }
                inBackground { discussionTopic?.views?.forEach { it.init(discussionTopic, it) } }
            }

            if (discussionTopic == null || discussionTopic?.views?.isEmpty() == true && DiscussionCaching(discussionTopicHeader.id).isEmpty()) {
                //Nothing to display
                discussionProgressBar.setGone()
                discussionTopicRepliesTitle.setGone()
                swipeRefreshLayout.isRefreshing = false
            } else {
                val html = inBackground {
                    DiscussionUtils.createDiscussionTopicHtml(
                            this@DiscussionDetailsFragment.context,
                            isTablet,
                            canvasContext,
                            discussionTopicHeader,
                            discussionTopic!!.views,
                            discussionEntryId)
                }

                loadDiscussionTopicViews(html)
                discussionsScrollView.post { discussionsScrollView?.scrollTo(0, scrollPosition) }
            }
        } catch {
            Logger.e("Error loading discussion topic " + it.message)
        }
    }

    @BeforePageView
    private fun loadDiscussionTopicHeaderViews(discussionTopicHeader: DiscussionTopicHeader) {
        if (discussionTopicHeader.assignment != null) {
            setupAssignmentDetails(discussionTopicHeader.assignment)
        }

        if(discussionTopicHeader.isRequireInitialPost && !discussionTopicHeader.userCanSeePosts) {
            // User must post before seeing replies
            discussionTopicRepliesTitle.setGone()
            postBeforeViewingRepliesTextView.setVisible()
            discussionProgressBar.setGone()
        } else {
            postBeforeViewingRepliesTextView.setGone()
        }

        val displayName = discussionTopicHeader.author?.displayName
        ProfileUtils.loadAvatarForUser(authorAvatar, displayName, discussionTopicHeader.author?.avatarImageUrl)
        authorAvatar.setupAvatarA11y(discussionTopicHeader.author?.displayName)
        authorName?.text = displayName
        authoredDate?.text = DateHelper.getMonthDayAtTime(this@DiscussionDetailsFragment.context, discussionTopicHeader.postedAt, getString(R.string.at))
        discussionTopicTitle?.text = discussionTopicHeader.title

        replyToDiscussionTopic.setTextColor(ThemePrefs.buttonColor)
        replyToDiscussionTopic.setVisible(discussionTopicHeader.permissions.canReply())
        replyToDiscussionTopic.onClick { showReplyView(discussionTopicHeader.id) }

        //if the html has an arc lti url, we want to authenticate so the user doesn't have to login again
        if (CanvasWebView.containsArcLTI(discussionTopicHeader.message.orEmpty(), "UTF-8")) {
            //We are only handling ARC because there is not a predictable way for use to determine if a URL is and LTI launch
            getAuthenticatedURL(discussionTopicHeader.message.orEmpty(), { authenticatedHtml, originalUrl -> loadHTMLTopic(authenticatedHtml, originalUrl)})
        } else {
            loadHTMLTopic(discussionTopicHeader.message ?: "")
        }

        attachmentIcon.setVisible(!discussionTopicHeader.attachments.isEmpty())
        attachmentIcon.onClick {
            discussionTopicHeader.attachments?.let { viewAttachments(it) }
        }
    }

    private fun loadDiscussionTopicViews(html: String) {
        discussionRepliesWebView.setVisible()
        discussionProgressBar.setGone()
        //We are only handling ARC because there is not a predictable way for use to determine if a URL is and LTI launch
        if (CanvasWebView.containsArcLTI(html, "UTF-8")) getAuthenticatedURL(html, { authenticatedHtml, _ -> loadHTMLReplies(authenticatedHtml) })
        else discussionRepliesWebView.loadDataWithBaseURL(CanvasWebView.getReferrer(), html, "text/html", "UTF-8", null)
        swipeRefreshLayout.isRefreshing = false
        discussionTopicRepliesTitle.setVisible()
        postBeforeViewingRepliesTextView.setGone()

        setupRepliesWebView()
    }

    private fun setupAssignmentDetails(assignment: Assignment) = with(assignment) {

        pointsTextView.setVisible()
        // Points possible
        pointsTextView.text = resources.getQuantityString(
                R.plurals.quantityPointsAbbreviated,
                pointsPossible.toInt(),
                NumberHelper.formatDecimal(pointsPossible, 1, true)
        )
        pointsTextView.contentDescription = resources.getQuantityString(
                R.plurals.quantityPointsFull,
                pointsPossible.toInt(),
                NumberHelper.formatDecimal(pointsPossible, 1, true))

        //set these as gone and make them visible if we have data for them
        availabilityLayout.setGone()
        availableFromLayout.setGone()
        availableToLayout.setGone()
        dueDateLayout.setGone()

        // Lock status
        val atSeparator = getString(R.string.at)

        allDates.singleOrNull()?.apply {
            if (lockAt?.before(Date()) == true) {
                availabilityLayout.setVisible()
                availabilityTextView.setText(R.string.closed)
            } else {
                availableFromLayout.setVisible()
                availableToLayout.setVisible()
                availableFromTextView.text = if (unlockAt != null)
                    DateHelper.getMonthDayAtTime(context, unlockAt, atSeparator) else getString(R.string.utils_noDateFiller)
                availableToTextView.text = if (lockAt!= null)
                    DateHelper.getMonthDayAtTime(context, lockAt, atSeparator) else getString(R.string.utils_noDateFiller)
            }
        }

        dueLayout.setVisible(allDates.singleOrNull() != null)

        dueAt?.let {
            dueDateLayout.setVisible()
            dueDateTextView.text = DateHelper.getMonthDayAtTime(context, it, atSeparator)
        }
    }

    private fun loadHTMLTopic(html: String, ltiUrl: String? = null) {
        setupHeaderWebView()
        discussionTopicHeaderWebView.loadHtml(DiscussionUtils.createDiscussionTopicHeaderHtml(context, isTablet, html, ltiUrl), discussionTopicHeader.title)
    }

    private fun loadHTMLReplies(html: String) {
        discussionRepliesWebView.loadDataWithBaseURL(CanvasWebView.getReferrer(true), html, "text/html", "UTF-8", null)
    }

    //region Discussion Actions

    private fun viewAttachments(remoteFiles: List<RemoteFile>) {
        val attachments = ArrayList<Attachment>()
        remoteFiles.forEach { attachments.add(it.mapToAttachment()) }
        if (attachments.isNotEmpty()) {
            // You can only attach one file to a discussion
            openMedia(attachments[0].contentType, attachments[0].url, attachments[0].filename)
        }
    }

    private fun showReplyView(discussionEntryId: Long) {
        if (APIHelper.hasNetworkConnection()) {
            scrollPosition = discussionsScrollView.scrollY
            val args = DiscussionsReplyFragment.makeBundle(discussionTopicHeader.id, discussionEntryId, isAnnouncements, discussionTopicHeader.permissions.isAttach)
            navigation?.addFragment(DiscussionsReplyFragment.newInstance(canvasContext, args))
        } else {
            NoInternetConnectionDialog.show(fragmentManager)
        }
    }

    @Suppress("EXPERIMENTAL_FEATURE_WARNING")
    fun markAsRead(discussionEntryIds: List<Long>) {
        if (discussionMarkAsReadJob?.isActive == true) return
        discussionMarkAsReadJob = tryWeave {
            val successfullyMarkedAsReadIds: MutableList<Long> = ArrayList(discussionEntryIds.size)
            discussionEntryIds.forEach { entryId ->
                val response = awaitApiResponse<Void?> { DiscussionManager.markDiscussionTopicEntryRead(canvasContext, discussionTopicHeader.id, entryId, it) }
                if(response.isSuccessful) {
                    successfullyMarkedAsReadIds.add(entryId)
                    discussionTopic?.let {
                        val entry = DiscussionUtils.findEntry(entryId, it.views)
                        entry?.isUnread = false
                        it.unreadEntriesMap.remove(entryId)
                        it.unreadEntries.remove(entryId)
                        if (discussionTopicHeader.unreadCount > 0) discussionTopicHeader.unreadCount -= 1
                    }
                }
            }

            successfullyMarkedAsReadIds.forEach {
                discussionRepliesWebView.post { discussionRepliesWebView.loadUrl("javascript:markAsRead" + "('" + it.toString() + "')") }
            }
            DiscussionTopicHeaderEvent(discussionTopicHeader).post()
        } catch {
            Logger.e("Error with DiscussionDetailsFragment:markAsRead() " + it.message)
        }
    }

    private fun askToDeleteDiscussionEntry(discussionEntryId: Long) {
        if (APIHelper.hasNetworkConnection()) {
            val builder = AlertDialog.Builder(context)
            builder.setMessage(R.string.utils_discussionsDeleteWarning)
            builder.setPositiveButton(android.R.string.yes) { _, _ ->
                deleteDiscussionEntry(discussionEntryId)
            }
            builder.setNegativeButton(android.R.string.no) { _, _ -> }
            val dialog = builder.create()
            dialog.setOnShowListener {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ThemePrefs.buttonColor)
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ThemePrefs.buttonColor)
            }
            dialog.show()
        } else NoInternetConnectionDialog.show(fragmentManager)
    }

    private fun updateDiscussionAsDeleted(discussionEntry: DiscussionEntry) {
        val deletedText = DiscussionUtils.formatDeletedInfoText(context, discussionEntry)
        discussionRepliesWebView.post { discussionRepliesWebView.loadUrl(
                "javascript:markAsDeleted" + "('" + discussionEntry.id.toString() + "','" + deletedText + "')") }
    }

    private fun deleteDiscussionEntry(entryId: Long) {
        deleteDiscussionEntry(canvasContext, discussionTopicHeader.id, entryId, object: StatusCallback<Void>() {
            override fun onResponse(response: Response<Void>, linkHeaders: LinkHeaders, type: ApiType) {
                if(response.code() in 200..299) {
                    discussionTopic?.let {
                        DiscussionUtils.findEntry(entryId, it.views)?.let { entry ->
                            entry.isDeleted = true
                            updateDiscussionAsDeleted(entry)
                            discussionTopicHeader.decrementDiscussionSubentryCount()
                            DiscussionTopicHeaderEvent(discussionTopicHeader).post()
                        }
                    }
                }
            }
        })
    }

    private fun showUpdateReplyView(discussionEntryId: Long) {
        if (APIHelper.hasNetworkConnection()) {
            discussionTopic?.let {
                val args = DiscussionsUpdateFragment.makeBundle(discussionTopicHeader.id, DiscussionUtils.findEntry(discussionEntryId, it.views), isAnnouncements, it)
                navigation?.addFragment(DiscussionsUpdateFragment.newInstance(canvasContext, args))
            }
        } else NoInternetConnectionDialog.show(fragmentManager)
    }

    //endregion

    //region Liking

    private fun likeDiscussionPressed(discussionEntryId: Long) {
        discussionTopic?.let { discussionTopic ->
            if (discussionLikeJob?.isActive == true) return

            DiscussionUtils.findEntry(discussionEntryId, discussionTopic.views)?.let { entry ->
                discussionLikeJob = tryWeave {
                    val rating = if (discussionTopic.entryRatings.containsKey(discussionEntryId)) discussionTopic.entryRatings[discussionEntryId] else 0
                    val newRating = if (rating == 1) 0 else 1
                    val response = awaitApiResponse<Void> { DiscussionManager.rateDiscussionEntry(canvasContext, discussionTopicHeader.id, discussionEntryId, newRating, it) }

                    if (response.code() in 200..299) {
                        discussionTopic.entryRatings[discussionEntryId] = newRating

                        if (newRating == 1) {
                            entry.ratingSum += 1
                            entry.setHasRated(true)
                            updateDiscussionLiked(entry)
                        } else if (entry.ratingSum > 0) {
                            entry.ratingSum -= 1
                            entry.setHasRated(false)
                            updateDiscussionUnliked(entry)
                        }
                    }
                } catch {
                    //Maybe a permissions issue?
                    Logger.e("Error liking discussion entry: " + it.message)
                }
            }
        }
    }

    private fun updateDiscussionLiked(discussionEntry: DiscussionEntry) {
        updateDiscussionLikedState(discussionEntry, JS_CONST_SET_LIKED/*Constant found in the JS files*/)
    }

    private fun updateDiscussionUnliked(discussionEntry: DiscussionEntry) {
        updateDiscussionLikedState(discussionEntry, JS_CONST_SET_UNLIKED /*Constant found in the JS files*/)
    }

    private fun updateDiscussionLikedState(discussionEntry: DiscussionEntry, methodName: String) {
        val likingSum = if(discussionEntry.ratingSum == 0) "" else "(" + discussionEntry.ratingSum + ")"
        val likingColor = DiscussionUtils.getHexColorString(if(discussionEntry.hasRated()) ThemePrefs.brandColor else ContextCompat.getColor(context, R.color.utils_discussion_liking))
        activity?.runOnUiThread {
            discussionRepliesWebView.loadUrl("javascript:" + methodName + "('" + discussionEntry.id.toString() + "')")
            discussionRepliesWebView.loadUrl("javascript:updateLikedCount('" + discussionEntry.id.toString() + "','" + likingSum + "','" + likingColor + "')")
        }
    }

    //endregion

    //region WebView And Javascript

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(webView: CanvasWebView) {
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        webView.setBackgroundColor(Color.WHITE)
        webView.settings.javaScriptEnabled = true
        webView.settings.useWideViewPort = true
        webView.settings.allowFileAccess = true
        webView.settings.loadWithOverviewMode = true
        CookieManager.getInstance().acceptThirdPartyCookies(webView)
        webView.canvasWebViewClientCallback = object : CanvasWebView.CanvasWebViewClientCallback {
            override fun routeInternallyCallback(url: String?) {
                if (url != null) {
                    if (!RouterUtils.canRouteInternally(activity, url, ApiPrefs.domain, true)) {
                        val bundle = InternalWebviewFragment.createBundle(url, url, false, "")
                        navigation?.addFragment(ParentFragment.createFragment(InternalWebviewFragment::class.java, bundle))
                    }
                }
            }

            override fun canRouteInternallyDelegate(url: String?): Boolean {
                return url != null
            }

            override fun openMediaFromWebView(mime: String?, url: String?, filename: String?) {
                openMedia(canvasContext, url ?: "")
            }
            override fun onPageStartedCallback(webView: WebView?, url: String?) {}
            override fun onPageFinishedCallback(webView: WebView?, url: String?) {}
        }

        webView.addVideoClient(activity)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupHeaderWebView() {
        setupWebView(discussionTopicHeaderWebView)
        discussionTopicHeaderWebView.addJavascriptInterface(JSDiscussionHeaderInterface(), "accessor")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupRepliesWebView() {
        setupWebView(discussionRepliesWebView)
        discussionRepliesWebView.addJavascriptInterface(JSDiscussionInterface(), "accessor")
    }

    @Suppress("unused")
    private inner class JSDiscussionHeaderInterface {

        @JavascriptInterface
        fun onLtiToolButtonPressed(id: String) {
            val ltiUrl = URLDecoder.decode(id, "UTF-8")
            getAuthenticatedURL(ltiUrl, { authenticatedUrl, _ ->
                DiscussionUtils.launchIntent(this@DiscussionDetailsFragment.context, authenticatedUrl)
            })
        }

        //A helper to log out messages from the JS code
        @JavascriptInterface
        fun logMessage(message: String) {
            Logger.d(message)
        }
    }

    @Suppress("unused")
    private inner class JSDiscussionInterface {

        @JavascriptInterface
        fun onItemPressed(id: String) {
            //do nothing for now
        }

        @JavascriptInterface
        fun onAvatarPressed(id: String) {
            //not supporting avatar events at this time
        }

        @JavascriptInterface
        fun onAttachmentPressed(id: String) {
            discussionTopic?.let {
                DiscussionUtils.findEntry(id.toLong(), it.views)?.let {entry ->
                    if (entry.attachments.isNotEmpty()) viewAttachments(entry.attachments)
                }
            }
        }

        @JavascriptInterface
        fun onReplyPressed(id: String) {
            showReplyView(id.toLong())
        }

        @JavascriptInterface
        fun onEditPressed(id: String) {
            showUpdateReplyView(id.toLong())
        }

        @JavascriptInterface
        fun onDeletePressed(id: String) {
            askToDeleteDiscussionEntry(id.toLong())
        }

        @JavascriptInterface
        fun onLikePressed(id: String) {
            likeDiscussionPressed(id.toLong())
        }

        @JavascriptInterface
        fun onMoreRepliesPressed(id: String) {
            discussionTopic?.let {
                val args = DiscussionDetailsFragment.makeBundle(canvasContext, discussionTopicHeader, it, id.toLong())
                navigation?.addFragment(DiscussionDetailsFragment.newInstance(canvasContext, args))
            }
        }

        @JavascriptInterface
        fun getInViewPort(): String {
            return discussionTopic?.unreadEntries?.joinToString() ?: ""
        }

        @JavascriptInterface
        fun inViewPortAndUnread(idList: String) {
            if (idList.isNotEmpty()) {
                markAsRead(idList.split(",").map { it.toLong() })
            }
        }

        @JavascriptInterface
        fun getLikedImage(): String {
            //Returns a string of a bitmap colored for the thumbs up (like) image.
            val likeImage = DiscussionUtils.getBitmapFromAssets(context, "discussion_liked.png")
            return DiscussionUtils.makeBitmapForWebView(ThemePrefs.brandColor, likeImage)
        }

        //A helper to log out messages from the JS code
        @JavascriptInterface
        fun logMessage(message: String) {
            Logger.d(message)
        }

        /**
         * Calculates the offset of the scrollview and it's content as compared to the elements position within the webview.
         * A scrollview's visible window can be between 0 and the size of the scrollview's height. This looks at the content on top
         * of the discussion replies webview and adds that to the elements position to come up with a relative position for the element
         * withing the scrollview. In sort we are finding the elements position withing a scrollview.
         */
        @JavascriptInterface
        fun calculateActualOffset(elementId: String, elementHeight: String, elementTopOffset: String): Boolean {
            return DiscussionUtils.isElementInViewPortWithinScrollView(
                    this@DiscussionDetailsFragment.context,
                    discussionsScrollView,
                    discussionRepliesWebView.height,
                    discussionsScrollViewContentWrapper.height,
                    // Javascript passes us back a number, which could be either a float or an int, so we'll need to convert the string first to a float, then an int
                    elementHeight.toFloat().toInt(), elementTopOffset.toFloat().toInt())
        }
    }

    //endregion

    //region Display Helpers

    /**
     * Method to put an authenticated URL in place of a non-authenticated URL (like when we try to load Arc LTI in a webview)
     */
    private fun getAuthenticatedURL(html: String, loadHtml: (newUrl: String, originalUrl: String?) -> Unit) {
        if (authenticatedSessionURL.isNullOrBlank()) {
            //get the url
            sessionAuthJob = tryWeave {
                //get the url from html
                val matcher = Pattern.compile("src=\"([^\"]+)\"").matcher(discussionTopicHeader.message)
                matcher.find()
                val url = matcher.group(1)

                // Get an authenticated session so the user doesn't have to log in
                authenticatedSessionURL = awaitApi<AuthenticatedSession> { OAuthManager.getAuthenticatedSession(url, it) }.sessionUrl
                loadHtml(DiscussionUtils.getNewHTML(html, authenticatedSessionURL), url)
            } catch {
                //couldn't get the authenticated session, try to load it without it
                loadHtml(html, null)
            }

        } else {
            loadHtml(DiscussionUtils.getNewHTML(html, authenticatedSessionURL), null)
        }
    }

    //endregion

    override fun handleIntentExtras(extras: Bundle?) {
        super.handleIntentExtras(extras)

        extras?.let {
            if(it.containsKey(DISCUSSION_TOPIC_HEADER)) discussionTopicHeader = it.getParcelable(DISCUSSION_TOPIC_HEADER)
            if(it.containsKey(DISCUSSION_TOPIC_HEADER_ID)) discussionTopicHeaderId = it.getLong(DISCUSSION_TOPIC_HEADER_ID)
            if(it.containsKey(DISCUSSION_TOPIC)) discussionTopic = it.getParcelable(DISCUSSION_TOPIC)
            if(it.containsKey(IS_ANNOUNCEMENT)) isAnnouncements = it.getBoolean(IS_ANNOUNCEMENT)
        }

        urlParams?.let {
            //Use Case for routing
            if(it.containsKey(Param.MESSAGE_ID)) discussionTopicHeaderId = it[Param.MESSAGE_ID]?.toLong() ?: 0L
        }
    }

    override fun allowBookmarking(): Boolean {
        return !isNestedDetail
    }

    override fun getParamForBookmark(): HashMap<String, String> {
        if (discussionTopic == null || discussionTopicHeader.id == 0L) {
            return super.getParamForBookmark()
        }
        val map = super.getParamForBookmark()
        map[Param.MESSAGE_ID] = discussionTopicHeader.id.toString()
        return map
    }

    companion object {
        private const val DISCUSSION_TOPIC_HEADER = "discussion_topic_header"
        private const val DISCUSSION_TOPIC_HEADER_ID = "discussion_topic_header_id"
        private const val DISCUSSION_TOPIC = "discussion_topic"
        private const val DISCUSSION_ENTRY_ID = "discussion_entry_id"
        private const val IS_NESTED_DETAIL = "is_nested_detail"
        private const val IS_ANNOUNCEMENT = "is_announcement"

        private const val JS_CONST_SET_LIKED = "setLiked"
        private const val JS_CONST_SET_UNLIKED = "setUnliked"

        @JvmStatic
        fun makeBundle(canvasContext: CanvasContext, discussionTopicHeader: DiscussionTopicHeader): Bundle = Bundle().apply {
            putParcelable(Const.CANVAS_CONTEXT, canvasContext)
            putParcelable(DISCUSSION_TOPIC_HEADER, discussionTopicHeader)
        }

        @JvmStatic
        fun makeBundle(canvasContext: CanvasContext, discussionTopicHeader: DiscussionTopicHeader, isAnnouncement: Boolean): Bundle = Bundle().apply {
            putParcelable(Const.CANVAS_CONTEXT, canvasContext)
            putParcelable(DISCUSSION_TOPIC_HEADER, discussionTopicHeader)
            putBoolean(IS_ANNOUNCEMENT, isAnnouncement)
        }

        @JvmStatic
        fun makeBundle(canvasContext: CanvasContext, discussionTopicHeaderId: Long, isAnnouncement: Boolean): Bundle = Bundle().apply {
            putParcelable(Const.CANVAS_CONTEXT, canvasContext)
            putLong(DISCUSSION_TOPIC_HEADER_ID, discussionTopicHeaderId)
            putBoolean(IS_ANNOUNCEMENT, isAnnouncement)
        }

        @JvmStatic
        fun makeBundle(canvasContext: CanvasContext, discussionTopicHeaderId: Long): Bundle = Bundle().apply {
            putParcelable(Const.CANVAS_CONTEXT, canvasContext)
            putLong(DISCUSSION_TOPIC_HEADER_ID, discussionTopicHeaderId)
        }

        @JvmStatic
        fun makeBundle(
                canvasContext: CanvasContext,
                discussionTopicHeader: DiscussionTopicHeader,
                discussionTopic: DiscussionTopic,
                discussionEntryId: Long): Bundle = Bundle().apply {

            //Used for viewing more entries, beyond the default nesting
            putParcelable(Const.CANVAS_CONTEXT, canvasContext)
            putParcelable(DISCUSSION_TOPIC_HEADER, discussionTopicHeader)
            putParcelable(DISCUSSION_TOPIC, discussionTopic)
            putLong(DISCUSSION_ENTRY_ID, discussionEntryId)
            putBoolean(IS_NESTED_DETAIL, true)
        }

        @JvmStatic
        fun newInstance(canvasContext: CanvasContext, args: Bundle) = DiscussionDetailsFragment().apply {
            args.putParcelable(Const.CANVAS_CONTEXT, canvasContext)
            arguments = args

            if (args.containsKey(DISCUSSION_TOPIC_HEADER)) {
                discussionTopicHeader = args.getParcelable<DiscussionTopicHeader>(DISCUSSION_TOPIC_HEADER)
            }
            if (args.containsKey(DISCUSSION_TOPIC)) {
                discussionTopic = args.getParcelable<DiscussionTopic>(DISCUSSION_TOPIC)
            }

            discussionEntryId = args.getLong(DISCUSSION_ENTRY_ID, 0L)
            discussionTopicHeaderId = args.getLong(DISCUSSION_TOPIC_HEADER_ID, 0L)
            isNestedDetail = args.getBoolean(IS_NESTED_DETAIL, false)
            isAnnouncements = args.getBoolean(IS_ANNOUNCEMENT, false)
        }
    }
}
