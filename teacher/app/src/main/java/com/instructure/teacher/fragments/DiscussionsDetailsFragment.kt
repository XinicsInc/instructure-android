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
package com.instructure.teacher.fragments

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.view.MenuItem
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.instructure.canvasapi2.managers.OAuthManager
import com.instructure.canvasapi2.models.*
import com.instructure.canvasapi2.utils.*
import com.instructure.canvasapi2.utils.weave.WeaveJob
import com.instructure.canvasapi2.utils.weave.awaitApi
import com.instructure.canvasapi2.utils.weave.catch
import com.instructure.canvasapi2.utils.weave.tryWeave
import com.instructure.pandautils.dialogs.AttachmentPickerDialog
import com.instructure.pandautils.discussions.DiscussionUtils
import com.instructure.pandautils.fragments.BasePresenterFragment
import com.instructure.pandautils.utils.*
import com.instructure.pandautils.views.CanvasWebView
import com.instructure.teacher.BuildConfig
import com.instructure.teacher.R
import com.instructure.teacher.adapters.StudentContextFragment
import com.instructure.teacher.dialog.NoInternetConnectionDialog
import com.instructure.teacher.events.*
import com.instructure.teacher.factory.DiscussionsDetailsPresenterFactory
import com.instructure.interactions.FullScreenInteractions
import com.instructure.interactions.Identity
import com.instructure.interactions.MasterDetailInteractions
import com.instructure.teacher.presenters.AssignmentSubmissionListPresenter
import com.instructure.teacher.presenters.DiscussionsDetailsPresenter
import com.instructure.interactions.router.Route
import com.instructure.teacher.router.RouteMatcher
import com.instructure.teacher.utils.*
import com.instructure.teacher.viewinterface.DiscussionsDetailsView
import instructure.androidblueprint.PresenterFactory
import kotlinx.android.synthetic.main.fragment_discussions_details.*
import kotlinx.android.synthetic.main.view_submissions_donut_group.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList

class DiscussionsDetailsFragment : BasePresenterFragment<
        DiscussionsDetailsPresenter,
        DiscussionsDetailsView>(), DiscussionsDetailsView, Identity {

    //region Member Variables

    private var mCanvasContext: CanvasContext by ParcelableArg(Course())
    private var mDiscussionTopicHeader: DiscussionTopicHeader by ParcelableArg(DiscussionTopicHeader())
    private var mDiscussionTopic: DiscussionTopic by ParcelableArg(DiscussionTopic())
    private var mDiscussionEntryId: Long by LongArg(default = 0L)
    private var mDiscussionTopicHeaderId: Long by LongArg(default = 0L)
    private var mSkipIdentityCheck: Boolean by BooleanArg(default = false)
    private var mSkipId: String by StringArg(default = "")

    private var mIsAnnouncements: Boolean = false
    private var mIsNestedDetail: Boolean = false

    private var mSessionAuthJob: Job? = null
    private var mAuthenticatedSessionURL: String? = null

    private var loadDiscussionJob: WeaveJob? = null

    //endregion

    override fun layoutResId(): Int = R.layout.fragment_discussions_details

    override fun onRefreshFinished() {}
    override fun onRefreshStarted() {}

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        mSessionAuthJob?.cancel()
        loadDiscussionJob?.cancel()
    }

    override val identity: Long? get() = if(mDiscussionTopicHeaderId != 0L) mDiscussionTopicHeaderId else mDiscussionTopicHeader.id
    override val skipCheck: Boolean get() = mSkipIdentityCheck

    override fun getPresenterFactory(): PresenterFactory<DiscussionsDetailsPresenter> =
            DiscussionsDetailsPresenterFactory(mCanvasContext, mDiscussionTopicHeader, mDiscussionTopic, mDiscussionEntryId,
                    if(mSkipId.isEmpty()) DiscussionsDetailsFragment::class.java.simpleName + UUID.randomUUID().toString() else mSkipId, mIsAnnouncements)

    override fun onPresenterPrepared(presenter: DiscussionsDetailsPresenter?) {}

    override fun onReadySetGo(presenter: DiscussionsDetailsPresenter) {

        EventBus.getDefault().getStickyEvent(DiscussionEntryUpdatedEvent::class.java)?.once(javaClass.simpleName) { discussionEntry ->
            presenter.updateDiscussionEntryToDiscussionTopic(discussionEntry)
        }

        val discussionTopicEvent = EventBus.getDefault().getStickyEvent(DiscussionTopicEvent::class.java)

        if(discussionTopicEvent != null) {
             discussionTopicEvent.only(presenter.getSkipId()) { discussionTopic ->
                //A The Discussion Topic was changed in some way. Usually from a nested situation where something was added.
                presenter.updateDiscussionTopic(discussionTopic)
                if (!mIsNestedDetail) {
                    EventBus.getDefault().removeStickyEvent(discussionTopicEvent)
                }
            }
        } else {
            if (mDiscussionTopicHeaderId == 0L && presenter.discussionTopicHeader.id != 0L) {
                //We were given a valid DiscussionTopicHeader, no need to fetch from the API
                populateDiscussionTopicHeader(presenter.discussionTopicHeader, false)
            } else if (mDiscussionTopicHeaderId != 0L) {
                //results of this GET will call populateDiscussionTopicHeader()
                presenter.getDiscussionTopicHeader(mDiscussionTopicHeaderId, false)
            }
        }

        EventBus.getDefault().getStickyEvent(DiscussionTopicHeaderDeletedEvent::class.java)?.once(javaClass.simpleName + ".onResume()") {
            if (it == presenter.discussionTopicHeader.id) {
                if (activity is MasterDetailInteractions) {
                    (activity as MasterDetailInteractions).popFragment(mCanvasContext)
                } else if(activity is FullScreenInteractions) {
                    activity.finish()
                }
            }
        }
    }

    override fun populateAsForbidden() {
        //TODO: when we add support for students
    }

    override fun populateDiscussionTopicHeader(discussionTopicHeader: DiscussionTopicHeader, forceNetwork: Boolean) {
        if(discussionTopicHeader.assignment != null) {
            setupAssignmentDetails(discussionTopicHeader.assignment)
            presenter.getSubmissionData(forceNetwork)
            setupListeners()
        }

        // Publish status if discussion
        if(!mIsAnnouncements) {
            if (discussionTopicHeader.isPublished) {
                publishStatusIconView.setImageResource(R.drawable.vd_published)
                publishStatusIconView.setColorFilter(context.getColorCompat(R.color.publishedGreen))
                publishStatusTextView.setText(R.string.published)
                publishStatusTextView.setTextColor(ContextCompat.getColor(context, R.color.published_green))
            } else {
                publishStatusIconView.setImageResource(R.drawable.vd_unpublished)
                publishStatusIconView.setColorFilter(context.getColorCompat(R.color.defaultTextGray))
                publishStatusTextView.setText(R.string.not_published)
                publishStatusTextView.setTextColor(ContextCompat.getColor(context, R.color.defaultTextGray))
            }
        } else {
            pointsPublishedLayout.setGone()
            pointsPublishedDivider.setGone()
            dueLayoutDivider.setGone()
            submissionDivider.setGone()
        }

        loadDiscussionTopicHeader(discussionTopicHeader)
        repliesBack.setVisible(mIsNestedDetail)
        repliesBack.onClick { activity.onBackPressed() }
        attachmentIcon.setVisible(!discussionTopicHeader.attachments.isEmpty())
        attachmentIcon.onClick {
            val remoteFiles = presenter?.discussionTopicHeader?.attachments
            if(remoteFiles != null) {
                viewAttachments(remoteFiles)
            }
        }

        if(presenter.discussionTopic.views.isEmpty()) {
            //Loading data will eventually call, upon success, populateDiscussionTopic()
            presenter.loadData(true)
        } else {
            populateDiscussionTopic(discussionTopicHeader, presenter.discussionTopic)
        }
    }

    override fun populateDiscussionTopic(discussionTopicHeader: DiscussionTopicHeader, discussionTopic: DiscussionTopic) {
        //Check if we have permissions and if we have any discussions to display.

        loadDiscussionJob = tryWeave {

            swipeRefreshLayout.isRefreshing = false

            if(discussionTopic.views.isEmpty()) {
                //Nothing to display
                discussionRepliesHeaderWrapper.setGone()
                return@tryWeave
            }

            discussionRepliesHeaderWrapper.setVisible()

            val html = inBackground {
                DiscussionUtils.createDiscussionTopicHtml(
                        activity,
                        isTablet,
                        mCanvasContext,
                        discussionTopicHeader,
                        discussionTopic.views,
                        mDiscussionEntryId)
            }

            discussionRepliesWebView.setInvisible()
            if(CanvasWebView.containsArcLTI(html, "utf-8")) {
                getAuthenticatedURL(html, { loadHTMLReplies(it) })

            } else {
                discussionRepliesWebView.loadDataWithBaseURL(CanvasWebView.getReferrer(), html, "text/html", "utf-8", null)
            }

            delay(300)
            discussionsScrollView.post {
                discussionsScrollView.scrollTo(0, presenter?.scrollPosition ?: 0)
                discussionRepliesWebView.setVisible()
            }
        } catch { Logger.e("Error loading discussion " + it.message) }
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

        dueLayout.setVisible()
        submissionsLayout.setVisible(!mCanvasContext.isDesigner())

        //set these as gone and make them visible if we have data for them
        availabilityLayout.setGone()
        availableFromLayout.setGone()
        availableToLayout.setGone()
        dueForLayout.setGone()
        dueDateLayout.setGone()
        otherDueDateTextView.setGone()

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
                    DateHelper.getMonthDayAtTime(context, unlockAt, atSeparator) else getString(R.string.no_date_filler)
                availableToTextView.text = if (lockAt!= null)
                    DateHelper.getMonthDayAtTime(context, lockAt, atSeparator) else getString(R.string.no_date_filler)
            }
        }

        // Due date(s)
        if (allDates.size > 1) {
            otherDueDateTextView.setVisible()
            otherDueDateTextView.setText(R.string.multiple_due_dates)
        } else {
            if (allDates.size == 0 || allDates[0].dueAt == null) {
                otherDueDateTextView.setVisible()
                otherDueDateTextView.setText(R.string.no_due_date)

                dueForLayout.setVisible()
                dueForTextView.text = if (allDates.size == 0 || allDates[0].isBase) getString(R.string.everyone) else allDates[0].title ?: ""

            } else with(allDates[0]) {
                dueDateLayout.setVisible()
                dueDateTextView.text = DateHelper.getMonthDayAtTime(context, dueAt, atSeparator)

                dueForLayout.setVisible()
                dueForTextView.text = if (isBase) getString(R.string.everyone) else title ?: ""
            }
        }

    }

    override fun updateSubmissionDonuts(totalStudents: Int, gradedStudents: Int, needsGradingCount: Int, notSubmitted: Int) {
        // Submission section
        gradedChart.setSelected(gradedStudents)
        gradedChart.setTotal(totalStudents)
        gradedChart.setSelectedColor(ThemePrefs.brandColor)
        gradedChart.setCenterText(gradedStudents.toString())
        gradedWrapper.contentDescription = context.getString(R.string.content_description_submission_donut_graded).format(gradedStudents, totalStudents)
        gradedProgressBar.setGone()
        gradedChart.invalidate()

        ungradedChart.setSelected(needsGradingCount)
        ungradedChart.setTotal(totalStudents)
        ungradedChart.setSelectedColor(ThemePrefs.brandColor)
        ungradedChart.setCenterText(needsGradingCount.toString())
        ungradedLabel.text = context.resources.getQuantityText(R.plurals.needsGradingNoQuantity, needsGradingCount)
        ungradedWrapper.contentDescription = context.getString(R.string.content_description_submission_donut_needs_grading).format(needsGradingCount, totalStudents)
        ungradedProgressBar.setGone()
        ungradedChart.invalidate()

        notSubmittedChart.setSelected(notSubmitted)
        notSubmittedChart.setTotal(totalStudents)
        notSubmittedChart.setSelectedColor(ThemePrefs.brandColor)
        notSubmittedChart.setCenterText(notSubmitted.toString())
        notSubmittedWrapper.contentDescription = context.getString(R.string.content_description_submission_donut_unsubmitted).format(notSubmitted, totalStudents)
        notSubmittedProgressBar.setGone()
        notSubmittedChart.invalidate()
    }

    private fun setupListeners() {
        dueLayout.setOnClickListener {
            val args = DueDatesFragment.makeBundle(presenter.discussionTopicHeader.assignment)
            RouteMatcher.route(context, Route(null, DueDatesFragment::class.java, mCanvasContext, args))
        }
        submissionsLayout.setOnClickListener {
            navigateToSubmissions(mCanvasContext as Course, presenter.discussionTopicHeader.assignment, AssignmentSubmissionListPresenter.SubmissionListFilter.ALL)
        }
        viewAllSubmissions.onClick { submissionsLayout.performClick() } // Separate click listener for a11y
        gradedWrapper.setOnClickListener {
            navigateToSubmissions(mCanvasContext as Course, presenter.discussionTopicHeader.assignment, AssignmentSubmissionListPresenter.SubmissionListFilter.GRADED)
        }
        ungradedWrapper.setOnClickListener {
            navigateToSubmissions(mCanvasContext as Course, presenter.discussionTopicHeader.assignment, AssignmentSubmissionListPresenter.SubmissionListFilter.NOT_GRADED)
        }
        notSubmittedWrapper.setOnClickListener {
            navigateToSubmissions(mCanvasContext as Course, presenter.discussionTopicHeader.assignment, AssignmentSubmissionListPresenter.SubmissionListFilter.MISSING)
        }
    }

    private fun navigateToSubmissions(course: Course, assignment: Assignment, filter: AssignmentSubmissionListPresenter.SubmissionListFilter) {
        val args = AssignmentSubmissionListFragment.makeBundle(assignment, filter)
        RouteMatcher.route(context, Route(null, AssignmentSubmissionListFragment::class.java, course, args))
    }

    private fun loadDiscussionTopicHeader(discussionTopicHeader: DiscussionTopicHeader) {
        val displayName = discussionTopicHeader.author?.displayName
        ProfileUtils.loadAvatarForUser(authorAvatar, displayName, discussionTopicHeader.author?.avatarImageUrl)
        authorAvatar.setupAvatarA11y(discussionTopicHeader.author?.displayName)
        authorAvatar.onClick {
            val bundle = StudentContextFragment.makeBundle(discussionTopicHeader.author?.id ?: 0, mCanvasContext.id)
            RouteMatcher.route(context, Route(StudentContextFragment::class.java, null, bundle))
        }
        authorName?.text = displayName
        authoredDate?.text = DateHelper.getMonthDayAtTime(context, discussionTopicHeader.postedAt, context.getString(R.string.at))
        discussionTopicTitle?.text = discussionTopicHeader.title

        replyToDiscussionTopic.setTextColor(ThemePrefs.buttonColor)
        replyToDiscussionTopic.setVisible(discussionTopicHeader.permissions.canReply())
        replyToDiscussionTopic.onClick {
            showReplyView(presenter.discussionTopicHeader.id)
        }

        //if the html has an arc lti url, we want to authenticate so the user doesn't have to login again
        if (CanvasWebView.containsArcLTI(discussionTopicHeader.message.orEmpty(), "UTF-8")) {

            getAuthenticatedURL(discussionTopicHeader.message.orEmpty(), this::loadHTMLTopic)

        } else {
            discussionTopicHeaderWebView.loadHtml(discussionTopicHeader.message, discussionTopicHeader.title)
        }
        discussionRepliesWebView.loadHtml("", "")
    }

    private fun loadHTMLTopic(html: String) {
        discussionTopicHeaderWebView.loadHtml(html, presenter.discussionTopicHeader.title)
    }

    private fun loadHTMLReplies(html: String) {
        discussionRepliesWebView.loadDataWithBaseURL(CanvasWebView.getReferrer(true), html, "text/html", "utf-8", null)
    }

    /**
     * Method to put an authenticated URL in place of a non-authenticated URL (like when we try to load Arc LTI in a webview)
     */
    private fun getAuthenticatedURL(html: String, loadHtml: (newUrl: String) -> Unit) {
        if(mAuthenticatedSessionURL.isNullOrBlank()) {
            //get the url
            mSessionAuthJob = tryWeave {
                //get the url from html
                val matcher = Pattern.compile("src=\"([^\"]+)\"").matcher(presenter.discussionTopicHeader.message)
                matcher.find()
                val url = matcher.group(1)

                // Get an authenticated session so the user doesn't have to log in
                mAuthenticatedSessionURL = awaitApi<AuthenticatedSession> { OAuthManager.getAuthenticatedSession(url, it) }.sessionUrl
                loadHtml(getNewHTML(html))
            } catch {
                //couldn't get the authenticated session, try to load it without it
                loadHtml(html)
            }

        } else {
            loadHtml(getNewHTML(html))
        }
    }

    private fun getNewHTML(html: String): String {
        //now we need to swap out part of the old url for this new authenticated url
        val matcher = Pattern.compile("src=\"([^;]+)").matcher(html)
        matcher.find()
        var newHTML: String = html
        // We only want to change the urls that are part of an external tool, not everything (like avatars)
        (1..matcher.groupCount())
            .map { matcher.group(it) }
            .filter { it.contains("external_tools") }
            .forEach { newHTML = html.replace(it, mAuthenticatedSessionURL!!) }
        return newHTML
    }

    override fun onPause() {
        super.onPause()
        presenter?.scrollPosition = discussionsScrollView.scrollY
        discussionTopicHeaderWebView.onPause()
        discussionRepliesWebView.onPause()
    }

    override fun onResume() {
        super.onResume()
        setupToolbar()

        swipeRefreshLayout.setOnRefreshListener {
            presenter.loadData(true)
            presenter.getSubmissionData(true)

            // Send out bus events to trigger a refresh for discussion list and submission list
            DiscussionUpdatedEvent(presenter.discussionTopicHeader, javaClass.simpleName).post()
            presenter.discussionTopicHeader.assignment?.let {
                AssignmentGradedEvent(it.id, javaClass.simpleName).post()
            }
        }

        discussionTopicHeaderWebView.onResume()
        discussionRepliesWebView.onResume()

        setupWebView(discussionTopicHeaderWebView, false)
        setupWebView(discussionRepliesWebView, true)
    }

    private fun setupToolbar() {
        toolbar.setupBackButtonWithExpandCollapseAndBack(this) {
            toolbar.updateToolbarExpandCollapseIcon(this)
            ViewStyler.themeToolbar(activity, toolbar, (mCanvasContext as Course).color, Color.WHITE)
            (activity as MasterDetailInteractions).toggleExpandCollapse()
        }
        toolbar.setupMenu(R.menu.menu_edit_generic, menuItemCallback)
        toolbar.title = if(mIsAnnouncements) getString(R.string.announcementDetails) else getString(R.string.discussion_details)
        if(!isTablet) {
            toolbar.subtitle = mCanvasContext.name
        }
        ViewStyler.themeToolbar(activity, toolbar, (mCanvasContext as Course).color, Color.WHITE)
    }

    val menuItemCallback: (MenuItem) -> Unit = { item ->
        when (item.itemId) {
            R.id.menu_edit -> {
                if(APIHelper.hasNetworkConnection()) {
                    if(mIsAnnouncements) {
                        val args = CreateOrEditAnnouncementFragment.newInstanceEdit(presenter.canvasContext, presenter.discussionTopicHeader).arguments
                        RouteMatcher.route(context, Route(CreateOrEditAnnouncementFragment::class.java, null, args))
                    } else {
                        // If we have an assignment, set the topic header to null to prevent cyclic reference
                        presenter.discussionTopicHeader.assignment?.setDiscussionTopic(null)
                        val args = CreateDiscussionFragment.makeBundle(presenter.canvasContext, presenter.discussionTopicHeader)
                        RouteMatcher.route(context, Route(CreateDiscussionFragment::class.java, mCanvasContext, args))
                    }
                } else {
                    NoInternetConnectionDialog.show(fragmentManager)
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(webView: CanvasWebView, addJSSupport: Boolean) {
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        webView.setBackgroundColor(Color.WHITE)
        webView.settings.javaScriptEnabled = true
        if(addJSSupport) webView.addJavascriptInterface(JSDiscussionInterface(), "accessor")
        webView.settings.useWideViewPort = true
        webView.settings.allowFileAccess = true
        webView.settings.loadWithOverviewMode = true
        CookieManager.getInstance().acceptThirdPartyCookies(webView)
        webView.canvasWebViewClientCallback = object: CanvasWebView.CanvasWebViewClientCallback {
            override fun routeInternallyCallback(url: String?) {
                if (url != null) {
                    if (!RouteMatcher.canRouteInternally(activity, url, ApiPrefs.domain, true)) {
                        val bundle = InternalWebViewFragment.makeBundle(url, url, false, "")
                        RouteMatcher.route(context, Route(FullscreenInternalWebViewFragment::class.java, presenter?.canvasContext, bundle))
                    }
                }
            }
            override fun canRouteInternallyDelegate(url: String?): Boolean {
                return url != null
            }
            override fun openMediaFromWebView(mime: String?, url: String?, filename: String?) {
                showToast(R.string.downloadingFile)
                RouteMatcher.openMedia(activity, url ?: "")
            }
            override fun onPageStartedCallback(webView: WebView?, url: String?) {}
            override fun onPageFinishedCallback(webView: WebView?, url: String?) {}
        }

        webView.addVideoClient(activity)
    }


    @Suppress("unused")
    private inner class JSDiscussionInterface {

        @Suppress("UNUSED_PARAMETER")
        @JavascriptInterface
        fun onItemPressed(id: String) {
            //do nothing for now
        }

        @JavascriptInterface
        fun onAvatarPressed(id: String) {
            presenter.findEntry(id.toLong())?.let { entry ->
                val bundle = StudentContextFragment.makeBundle(entry.author.id, mCanvasContext.id)
                RouteMatcher.route(context, Route(StudentContextFragment::class.java, null, bundle))
            }
        }

        @JavascriptInterface
        fun onAttachmentPressed(id: String) {
            val entry = presenter.findEntry(id.toLong())
            if(entry != null && entry.attachments.isNotEmpty()) {
                viewAttachments(entry.attachments)
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
            deleteDiscussionEntry(id.toLong())
        }

        @JavascriptInterface
        fun onLikePressed(id: String) {
            presenter?.likeDiscussionPressed(id.toLong())
        }

        @JavascriptInterface
        fun onMoreRepliesPressed(id: String) {
            val args = DiscussionsDetailsFragment.makeBundle(presenter.discussionTopicHeader, presenter.discussionTopic, id.toLong(), presenter.getSkipId())
            RouteMatcher.route(context, Route(null, DiscussionsDetailsFragment::class.java, mCanvasContext, args))
        }

        @JavascriptInterface
        fun getInViewPort(): String {
            return presenter?.discussionTopic?.unreadEntries?.joinToString() ?: ""
        }

        @JavascriptInterface
        fun inViewPortAndUnread(idList: String) {
            if(idList.isNotEmpty()) {
                presenter?.markAsRead(idList.split(",").map(String::toLong))
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
        fun logMessage(message: String) { Logger.d(message) }

        /**
         * Calculates the offset of the scrollview and it's content as compared to the elements position within the webview.
         * A scrollview's visible window can be between 0 and the size of the scrollview's height. This looks at the content on top
         * of the discussion replies webview and adds that to the elements position to come up with a relative position for the element
         * withing the scrollview. In sort we are finding the elements position withing a scrollview.
         */
        @Suppress("UNUSED_PARAMETER")
        @JavascriptInterface
        fun calculateActualOffset(elementId: String, elementHeight: String, elementTopOffset: String): Boolean {
            // Javascript passes us back a number, which could be either a float or an int, so we'll need to convert the string first to a float, then an int
            return isElementInViewPortWithinScrollView(elementHeight.toFloat().toInt(), elementTopOffset.toFloat().toInt())
        }
    }

    override fun updateDiscussionLiked(discussionEntry: DiscussionEntry) {
        updateDiscussionLikedState(discussionEntry, "setLiked"/*Constant found in the JS files*/)
    }

    override fun updateDiscussionUnliked(discussionEntry: DiscussionEntry) {
        updateDiscussionLikedState(discussionEntry, "setUnliked" /*Constant found in the JS files*/)
    }

    private fun updateDiscussionLikedState(discussionEntry: DiscussionEntry, methodName: String) {
        val likingSum = if(discussionEntry.ratingSum == 0) "" else "(" + discussionEntry.ratingSum + ")"
        val likingColor = DiscussionUtils.getHexColorString(
                if(discussionEntry.hasRated()) ThemePrefs.brandColor else ContextCompat.getColor(context, R.color.utils_discussion_liking))
        activity.runOnUiThread {
            discussionRepliesWebView.loadUrl("javascript:" + methodName + "('" + discussionEntry.id.toString() + "')")
            discussionRepliesWebView.loadUrl("javascript:updateLikedCount('" + discussionEntry.id.toString() + "','" + likingSum + "','" + likingColor + "')")
        }
    }

    override fun updateDiscussionEntry(discussionEntry: DiscussionEntry) {
        activity.runOnUiThread {
            discussionRepliesWebView.loadUrl("javascript:updateEntry('${discussionEntry.id}', '${discussionEntry.message}')")
            if (discussionEntry.attachments == null && discussionEntry.attachments.size < 1)
                discussionRepliesWebView.loadUrl("javascript:hideAttachmentIcon('${discussionEntry.id}'")
        }
    }

    private fun showReplyView(id: Long) {
        if(APIHelper.hasNetworkConnection()) {
            val args = DiscussionsReplyFragment.makeBundle(presenter.discussionTopicHeader.id, id, mIsAnnouncements)
            RouteMatcher.route(context, Route(DiscussionsReplyFragment::class.java, presenter.canvasContext, args))
        } else {
            NoInternetConnectionDialog.show(fragmentManager)
        }
    }

    private fun showUpdateReplyView(id: Long) {
        if(APIHelper.hasNetworkConnection()) {
            val args = DiscussionsUpdateFragment.makeBundle(presenter.discussionTopicHeader.id, presenter.findEntry(id), mIsAnnouncements, presenter.discussionTopic)
            RouteMatcher.route(context, Route(DiscussionsUpdateFragment::class.java, presenter.canvasContext, args))
        } else {
            NoInternetConnectionDialog.show(fragmentManager)
        }
    }

    private fun deleteDiscussionEntry(id: Long) {
        if(APIHelper.hasNetworkConnection()) {
            val builder = AlertDialog.Builder(context)
            builder.setMessage(R.string.discussions_delete_warning)
            builder.setPositiveButton(android.R.string.yes) { _, _ ->
                presenter?.deleteDiscussionEntry(id)
            }
            builder.setNegativeButton(android.R.string.no) { _, _ -> }
            val dialog = builder.create()
            dialog.setOnShowListener {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ThemePrefs.buttonColor)
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ThemePrefs.buttonColor)
            }
            dialog.show()
        } else {
            NoInternetConnectionDialog.show(fragmentManager)
        }
    }

    override fun updateDiscussionAsDeleted(discussionEntry: DiscussionEntry) {
        val deletedText = DiscussionUtils.formatDeletedInfoText(context, discussionEntry)
        discussionRepliesWebView.post { discussionRepliesWebView.loadUrl(
                "javascript:markAsDeleted" + "('" + discussionEntry.id.toString() + "','" + deletedText + "')") }
    }

    override fun updateDiscussionsMarkedAsReadCompleted(markedAsReadIds: List<Long>) {
        markedAsReadIds.forEach {
            discussionRepliesWebView.post { discussionRepliesWebView.loadUrl("javascript:markAsRead" + "('" + it.toString() + "')") }
        }
    }

    /**
     * Checks to see if the webview element is within the viewable bounds of the scrollview.
     */
    private fun isElementInViewPortWithinScrollView(elementHeight: Int, topOffset: Int): Boolean {
        if(discussionsScrollView == null) return false
        val scrollBounds = Rect().apply{ discussionsScrollView.getDrawingRect(this) }

        val discussionRepliesHeight = discussionRepliesWebView.height
        val discussionScrollViewContentHeight = discussionsScrollViewContentWrapper.height
        val otherContentHeight = discussionScrollViewContentHeight - discussionRepliesHeight
        val top = context.DP(topOffset) + otherContentHeight
        val bottom = top + context.DP(elementHeight)

        return scrollBounds.top < top && scrollBounds.bottom > bottom
    }

    private fun createLoaderBundle(header: DiscussionTopicHeader, topic: DiscussionTopic, discussionEntryId: Long): Bundle {
        val loaderBundle = Bundle()
        loaderBundle.putParcelable(DISCUSSION_TOPIC_HEADER, header)
        loaderBundle.putParcelable(DISCUSSION_TOPIC, topic)
        loaderBundle.putLong(DISCUSSION_ENTRY_ID, discussionEntryId)
        return loaderBundle
    }

    private fun viewAttachments(remoteFiles: List<RemoteFile>) {
        val attachments = ArrayList<Attachment>()
        remoteFiles.forEach { attachments.add(it.mapToAttachment()) }
        if(attachments.isNotEmpty()) {
            if(attachments.size > 1) {
                AttachmentPickerDialog.show(fragmentManager, attachments, { attachment ->
                    AttachmentPickerDialog.hide(fragmentManager)
                    attachment.view(context)
                })
            } else if(attachments.size == 1) {
                attachments[0].view(context)
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onDiscussionUpdated(event: DiscussionUpdatedEvent) {
        event.once(javaClass.simpleName) {
            presenter.discussionTopicHeader = it
            populateDiscussionTopicHeader(it, false)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onDiscussionTopicHeaderDeleted(event: DiscussionTopicHeaderDeletedEvent) {
        //Depending on the device and where we delete the discussion topic header from we handle this in two places.
        //This situation handles when we delete from discussions list, the other found in readySetGo handles the create discussion fragment.
        event.once(javaClass.simpleName + ".onPost()") {
            if (it == presenter.discussionTopicHeader.id) {
                if(activity is MasterDetailInteractions) {
                    (activity as MasterDetailInteractions).popFragment(mCanvasContext)
                }
            } else if(activity is FullScreenInteractions) {
                activity.finish()
            }
        }
    }

    companion object {
        const val DISCUSSION_TOPIC_HEADER = "discussion_topic_header"
        const val DISCUSSION_TOPIC_HEADER_ID = "discussion_topic_header_id"
        const val DISCUSSION_TOPIC = "discussion_topic"
        const val DISCUSSION_ENTRY_ID = "discussion_entry_id"
        private const val SKIP_IDENTITY_CHECK = "skip_identity_check"
        private const val IS_NESTED_DETAIL = "is_nested_detail"
        private const val SKIP_ID = "skipId"
        private const val IS_ANNOUNCEMENT = "is_announcement"

        @JvmStatic fun makeBundle(discussionTopicHeader: DiscussionTopicHeader): Bundle = Bundle().apply {
            putParcelable(DISCUSSION_TOPIC_HEADER, discussionTopicHeader)
        }

        @JvmStatic fun makeBundle(discussionTopicHeader: DiscussionTopicHeader, isAnnouncement: Boolean): Bundle = Bundle().apply {
            putParcelable(DISCUSSION_TOPIC_HEADER, discussionTopicHeader)
            putBoolean(IS_ANNOUNCEMENT, isAnnouncement)
        }

        @JvmStatic fun makeBundle(discussionTopicHeaderId: Long): Bundle = Bundle().apply {
            putLong(DISCUSSION_TOPIC_HEADER_ID, discussionTopicHeaderId)
        }

        @JvmStatic fun makeBundle(discussionTopicHeaderId: Long, entryId: Long): Bundle = Bundle().apply {
            putLong(DISCUSSION_TOPIC_HEADER_ID, discussionTopicHeaderId)
            putLong(DISCUSSION_ENTRY_ID, entryId)
        }

        @JvmStatic fun makeBundle(
                discussionTopicHeader: DiscussionTopicHeader,
                discussionTopic: DiscussionTopic,
                discussionEntryId: Long,
                skipId: String): Bundle = Bundle().apply {

            //Used for viewing more entries, beyond the default nesting
            putParcelable(DISCUSSION_TOPIC_HEADER, discussionTopicHeader)
            putParcelable(DISCUSSION_TOPIC, discussionTopic)
            putLong(DISCUSSION_ENTRY_ID, discussionEntryId)
            putBoolean(SKIP_IDENTITY_CHECK, true)
            putBoolean(IS_NESTED_DETAIL, true)
            putString(SKIP_ID, skipId)
        }

        @JvmStatic fun newInstance(canvasContext: CanvasContext, args: Bundle) = DiscussionsDetailsFragment().apply {
            if (args.containsKey(DISCUSSION_TOPIC_HEADER)) {
                mDiscussionTopicHeader = args.getParcelable(DISCUSSION_TOPIC_HEADER)
            }
            if (args.containsKey(DISCUSSION_TOPIC)) {
                mDiscussionTopic = args.getParcelable(DISCUSSION_TOPIC)
            }

            mDiscussionEntryId = args.getLong(DISCUSSION_ENTRY_ID, 0L)
            mDiscussionTopicHeaderId = args.getLong(DISCUSSION_TOPIC_HEADER_ID, 0L)
            mSkipIdentityCheck = args.getBoolean(SKIP_IDENTITY_CHECK, false)
            mIsNestedDetail = args.getBoolean(IS_NESTED_DETAIL, false)
            mSkipId = args.getString(SKIP_ID, "")
            mCanvasContext = canvasContext
            mIsAnnouncements = args.getBoolean(IS_ANNOUNCEMENT, false)
        }
    }
}
