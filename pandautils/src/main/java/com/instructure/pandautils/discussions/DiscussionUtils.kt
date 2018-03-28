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
package com.instructure.pandautils.discussions

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.*
import android.net.Uri
import android.support.v4.content.ContextCompat
import android.util.Base64
import android.widget.ScrollView
import android.widget.Toast
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.Course
import com.instructure.canvasapi2.models.DiscussionEntry
import com.instructure.canvasapi2.models.DiscussionTopicHeader
import com.instructure.canvasapi2.utils.DateHelper
import com.instructure.canvasapi2.utils.Logger
import com.instructure.pandautils.R
import com.instructure.pandautils.utils.*
import com.instructure.pandautils.views.CanvasWebView
import java.io.*
import java.net.URLEncoder
import java.util.regex.Pattern

object DiscussionUtils {

    /* TODO: Support caching edited items. Comms - 868
    *  TODO: This has not yet been added due to a permissions issue.
    *  TODO: What needs to be done is to check the DiscussionUpdateFragment's response to ensure the last updated time stamp is correct.
    *  TODO: Then do a check here to not remove but replace discussion entries whos timestamps are behind the timestamps of cached items.
    * */

    //region Cache Cleaning

    private fun cleanDiscussionCache(discussionTopicHeaderId: Long, discussionEntries: List<DiscussionEntry>) {
        //Cleans up any duplicate entries in the discussion tree
        val cachingManager = DiscussionCaching(discussionTopicHeaderId)
        val mutableDiscussionEntries = discussionEntries.toMutableList()

        mutableDiscussionEntries.forEach {
            recursivelyClean(cachingManager, it, it.replies)
        }
    }

    private fun recursivelyClean(cachingManager: DiscussionCaching, parentEntry: DiscussionEntry, discussionEntries: MutableList<DiscussionEntry>) {
        var cachedEntries = cachingManager.loadEntries()

        //Clean up the parent if necessary
        cachedEntries.firstOrNull { it.id == parentEntry.id }?.let {
            cachingManager.removeEntry(it.id)
            cachedEntries = cachingManager.loadEntries()
        }

        discussionEntries.forEach { discussionEntry ->
            cachedEntries.firstOrNull { it.id == discussionEntry.id }?.let {
                cachingManager.removeEntry(it.id)
                cachedEntries = cachingManager.loadEntries()
            }
            recursivelyClean(cachingManager, discussionEntry, discussionEntry.replies)
        }
    }

    //endregion

    //region Cache and Discussion Unification

    private fun unifyDiscussionEntries(discussionTopicHeaderId: Long, discussionEntries: MutableList<DiscussionEntry>): List<DiscussionEntry> {
        val cachingManager = DiscussionCaching(discussionTopicHeaderId)
        val cachedEntries = cachingManager.loadEntries()

        if(cachedEntries.isEmpty()) return discussionEntries // Nothing in the cache

        // Situation where the first set of replies is cached. We setup the parent ID as -1
        discussionEntries.addAll(cachedEntries.filter { it.parentId == -1L })

        //Sort the discussion entries
        discussionEntries.sortBy { it.createdAt }

        //Loop through the highest level entries and add cached items where necessary for all children of parent
        discussionEntries.forEach { parentEntry ->
            parentEntry.replies = recursiveUnify(cachingManager, parentEntry, parentEntry.replies)
        }

        return discussionEntries
    }

    private fun recursiveUnify(cachingManager: DiscussionCaching, parentEntry: DiscussionEntry, discussionEntries: MutableList<DiscussionEntry>): List<DiscussionEntry>? {
        val cachedEntries = cachingManager.loadEntries()

        //cached entries may not get added to the parent...
        if(cachedEntries.isNotEmpty()) {

            //Add cached items belonging to this entry
            cachedEntries.filter { it.parentId == parentEntry.id }.let {
                discussionEntries.addAll(it)
                parentEntry.totalChildren += it.size
                if(it.isNotEmpty()) discussionEntries.sortBy { it.createdAt }
            }

            discussionEntries.forEach { discussionEntry ->
                discussionEntry.replies = recursiveUnify(cachingManager, discussionEntry, discussionEntry.replies)
            }
        }

        return discussionEntries
    }

    //endregion

    //region Find Entry and Sub Entry

    private fun findSubEntry(startEntryId: Long, discussionEntries: List<DiscussionEntry>): List<DiscussionEntry> {
        discussionEntries.forEach {
            val foundEntries = recursiveFind(startEntryId, it.replies)
            if (foundEntries != null) {
                return foundEntries
            }
        }
        return discussionEntries
    }

    private fun recursiveFind(startEntryId: Long, replies: List<DiscussionEntry>): List<DiscussionEntry>? {
        replies.forEach {
            if (it.id == startEntryId) {
                //Creates a list of replies based on the entry the user clicked on. This will not show siblings of the parent.
                val formalReplies = ArrayList<DiscussionEntry>(1)
                formalReplies.add(it)
                return formalReplies
            } else {
                val items = recursiveFind(startEntryId, it.replies)
                if (items != null) {
                    return items
                }
            }
        }
        return null
    }

    private fun recursiveFindEntry(startEntryId: Long, replies: List<DiscussionEntry>): DiscussionEntry? {
        replies.forEach {
            if (it.id == startEntryId) {
                return it
            } else {
                val items = recursiveFindEntry(startEntryId, it.replies)
                if (items != null) return items
            }
        }
        return null
    }

    fun findEntry(entryId: Long, replies: List<DiscussionEntry>): DiscussionEntry? {
        replies.forEach { discussionEntry ->
            if (discussionEntry.id == entryId) {
                return discussionEntry
            }

            val entry = recursiveFindEntry(entryId, discussionEntry.replies)
            if (entry != null) return entry
        }
        return null
    }

    //endregion

    //region Discussion Topic Header

    fun createDiscussionTopicHeaderHtml(context: Context, isTablet: Boolean, contentHtml: String, ltiToolUrl: String? = null): String {
        val document = getAssetsFile(context, "discussion_topic_header_html_template.html")
        Logger.d("LTIURL: $ltiToolUrl")
        val html = addLaunchLtiToolButton(context, contentHtml, ltiToolUrl)
        var result = document.replace("__HEADER_CONTENT__", html)
        result = result.replace("__LTI_BUTTON_WIDTH__", if(isTablet) "320px" else "100%")
        result = result.replace("__LTI_BUTTON_MARGIN__", if(isTablet) "0px" else "auto")
        return CanvasWebView.applyWorkAroundForDoubleSlashesAsUrlSource(result)
    }

    /**
     * Adds a Launch External Tools button below an iframe if we know the iframe is an External Tool
     * At the time of writing we made a decision not to add the launch to browser button for discussion details.
     */
    private fun addLaunchLtiToolButton(context: Context, contentHtml: String, ltiToolUrl: String? = null): String {
        if(ltiToolUrl == null) return contentHtml
        var html = contentHtml
        //append a open in browser button for the LTI tool
        val iframeMatcher = Pattern.compile("<iframe(.|\\n)*?iframe>").matcher(html)
        iframeMatcher.find()
        try {
            for (index in 0..iframeMatcher.groupCount()) {
                val iframe = iframeMatcher.group(index)
                if (iframe.contains("external_tools")) {
                    //Append the html button for viewing externally
                    val ltiUrl = URLEncoder.encode(ltiToolUrl, "UTF-8")
                    val button = "</br><p><div class=\"lti_button\" onClick=\"onLtiToolButtonPressed('%s')\">%s</div></p>"
                    val htmlButton = String.format(button, ltiUrl, context.resources.getString(R.string.utils_launchExternalTool))
                    html = html.replace(iframe, iframe + htmlButton)
                }
            }
        } catch (e: Throwable) {
            //Pattern match not found.
        }
        return html
    }

    //endregion

    //region Loading the Discussion Topic

    /**
     * This function should only be called from a background thread as it can be very expensive to execute
     */
    fun createDiscussionTopicHtml(
            context: Context,
            isTablet: Boolean,
            canvasContext: CanvasContext,
            discussionTopicHeader: DiscussionTopicHeader,
            discussionEntries: List<DiscussionEntry>,
            startEntryId: Long): String {

        val builder = StringBuilder()
        val brandColor = ThemePrefs.brandColor
        val likeColor = ContextCompat.getColor(context, R.color.utils_discussion_liking)
        val converter = DiscussionEntryHtmlConverter()
        val template = getAssetsFile(context, "discussion_html_template_item.html")
        val likeImage = makeBitmapForWebView(brandColor, getBitmapFromAssets(context, "discussion_liked.png"))
        val replyButtonWidth = if (isTablet && context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) "260px" else "220px"

        builder.append(getAssetsFile(context, "discussion_html_header_item.html"))

        cleanDiscussionCache(discussionTopicHeader.id, discussionEntries)

        val discussionEntryList: List<DiscussionEntry> = if (startEntryId == 0L) unifyDiscussionEntries(discussionTopicHeader.id, discussionEntries.toMutableList())
        else { // We are looking for a subentry of discussions to display. This finds a subentry, and uses that as the initial entry for display.
            findSubEntry(startEntryId, unifyDiscussionEntries(discussionTopicHeader.id, discussionEntries.toMutableList()))
        }

        //This loops through each of the direct replies and for each child up to 3 or 5 based on if tablet or phone.
        //General rule of thumb is to pass in any values that need calculation so we don't repeat those within the loop.
        //We also filter out any deleted discussions and by nature their children so they don't get displayed.
        discussionEntryList.forEach { discussionEntry ->
            builder.append(build(context, isTablet, canvasContext, discussionTopicHeader, discussionEntry, converter, template,
                    makeAvatarForWebView(context, discussionEntry), 0, false,
                    brandColor, likeColor, likeImage, replyButtonWidth))

            discussionEntry.replies.forEach { discussionEntry ->
                builder.append(build(context, isTablet, canvasContext, discussionTopicHeader, discussionEntry, converter, template,
                        makeAvatarForWebView(context, discussionEntry), 1, false,
                        brandColor, likeColor, likeImage, replyButtonWidth))

                if (isTablet) {
                    discussionEntry.replies.forEach { discussionEntry ->
                        builder.append(build(context, isTablet, canvasContext, discussionTopicHeader, discussionEntry, converter, template,
                                makeAvatarForWebView(context, discussionEntry), 2, false,
                                brandColor, likeColor, likeImage, replyButtonWidth))

                        discussionEntry.replies.forEach { discussionEntry ->
                            builder.append(build(context, isTablet, canvasContext, discussionTopicHeader, discussionEntry, converter, template,
                                    makeAvatarForWebView(context, discussionEntry), 3, false,
                                    brandColor, likeColor, likeImage, replyButtonWidth))

                            discussionEntry.replies.forEach { discussionEntry ->
                                builder.append(build(context, isTablet, canvasContext, discussionTopicHeader, discussionEntry, converter, template,
                                        makeAvatarForWebView(context, discussionEntry), 4, false,
                                        brandColor, likeColor, likeImage, replyButtonWidth))

                                discussionEntry.replies.forEach { discussionEntry ->
                                    val reachedViewableEnd = (discussionEntry.totalChildren > 0)
                                    builder.append(build(context, isTablet, canvasContext, discussionTopicHeader, discussionEntry, converter, template,
                                            makeAvatarForWebView(context, discussionEntry), 5, reachedViewableEnd,
                                            brandColor, likeColor, likeImage, replyButtonWidth))
                                }
                            }
                        }
                    }
                } else {
                    discussionEntry.replies.forEach { discussionEntry ->
                        val reachedViewableEnd = (discussionEntry.totalChildren > 0)
                        builder.append(build(context, isTablet, canvasContext, discussionTopicHeader, discussionEntry, converter, template,
                                makeAvatarForWebView(context, discussionEntry), 2, reachedViewableEnd,
                                brandColor, likeColor, likeImage, replyButtonWidth))
                    }
                }
            }
        }

        //Append Footer - Don't do this in the loop to avoid String.replace() more than necessary
        builder.append(getAssetsFile(context, "discussion_html_footer_item.html"))
        return CanvasWebView.applyWorkAroundForDoubleSlashesAsUrlSource(builder.toString())
    }

    private fun build(
            context: Context,
            isTablet: Boolean,
            canvasContext: CanvasContext,
            discussionTopicHeader: DiscussionTopicHeader,
            discussionEntry: DiscussionEntry,
            converter: DiscussionEntryHtmlConverter,
            template: String,
            avatarImage: String,
            indent: Int,
            reachedViewableEnd: Boolean,
            brandColor: Int,
            likeColor: Int,
            likeImage: String,
            replyButtonWidth: String): String {

        return converter.buildHtml(
                context,
                isTablet,
                brandColor,
                likeColor,
                discussionEntry,
                template,
                avatarImage,
                allowReplies(canvasContext, discussionTopicHeader),
                allowEditing(canvasContext, discussionTopicHeader),
                allowLiking(canvasContext, discussionTopicHeader),
                allowDeleting(canvasContext, discussionTopicHeader),
                reachedViewableEnd,
                indent,
                likeImage,
                replyButtonWidth,
                formatDeletedInfoText(context, discussionEntry))
    }

    //endregion

    //region Discussion Display Helpers

    fun launchIntent(context: Context, result: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result))
        // Make sure we can handle the intent
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(context, R.string.utils_unableToViewInBrowser, Toast.LENGTH_SHORT).show()
        }
    }

    fun getNewHTML(html: String, authenticatedSessionUrl: String?): String {
        if(authenticatedSessionUrl == null) return html
        //now we need to swap out part of the old url for this new authenticated url
        val matcher = Pattern.compile("src=\"([^;]+)").matcher(html)
        var newHTML: String = html
        if(matcher.find()) {
            //we only want to change the urls that are part of an external tool, not everything (like avatars)
            for (index in 0..matcher.groupCount()) {
                val newUrl = matcher.group(index)
                if (newUrl.contains("external_tools")) {
                    newHTML = html.replace(newUrl, authenticatedSessionUrl)
                }
            }
        }
        return newHTML
    }

    /**
     * Checks to see if the webview element is within the viewable bounds of the scrollview.
     */
    fun isElementInViewPortWithinScrollView(context: Context, scrollView: ScrollView, webViewHeight: Int, scrollViewContentWrapperHeight: Int,  elementHeight: Int, topOffset: Int): Boolean {
        val scrollBounds = Rect().apply{ scrollView.getDrawingRect(this) }

        val otherContentHeight = scrollViewContentWrapperHeight - webViewHeight
        val top = context.DP(topOffset) + otherContentHeight
        val bottom = top + context.DP(elementHeight)

        return scrollBounds.top < top && scrollBounds.bottom > bottom
    }

    private fun allowReplies(canvasContext: CanvasContext?, header: DiscussionTopicHeader): Boolean {
        /*
            There are three related scenarios in which we don't want users to be able to reply.
               so we check that none of these conditions exist
            1.) The discussion is locked for an unknown reason.
            2.) It's locked due to a module/etc.
            3.) User is an Observer in a course.
            4.) IF it's a teacher we bag the entire rule book and let them reply.
        */

        val isCourse = canvasContext?.isCourse == true

        if (isCourse && (canvasContext as Course).isTeacher) return true

        val isLocked = header.isLocked
        val lockInfoEmpty = header.lockInfo == null || header.lockInfo.isEmpty
        val isObserver = isCourse && (canvasContext as Course).isObserver
        val hasPermission = header.permissions.canReply()

        //If we are not locked, do not have lock info, have permission, is a course, and not an observer...
        // - I suspect this can all be replaced with hasPermission, need to verify.
        return !isLocked && lockInfoEmpty && hasPermission && (isCourse || canvasContext?.isGroup == true) && !isObserver
    }

    private fun allowEditing(canvasContext: CanvasContext?, header: DiscussionTopicHeader): Boolean {
        // TODO - Update this when COMMS-868 is completed
        return if (canvasContext?.type == CanvasContext.Type.COURSE) {
            (canvasContext as Course).isTeacher
        } else {
            false
        }
    }

    private fun allowLiking(canvasContext: CanvasContext?, header: DiscussionTopicHeader): Boolean {
        if (header.isAllowRating) {
            if (header.isOnlyGradersCanRate) {
                if (canvasContext?.type == CanvasContext.Type.COURSE) {
                    return (canvasContext as Course).isTeacher || canvasContext.isTA
                }
            } else {
                return true
            }
        }
        return false
    }

    private fun allowDeleting(canvasContext: CanvasContext?, header: DiscussionTopicHeader): Boolean {
        // TODO - Update this when COMMS-868 is completed
        return if (canvasContext?.type == CanvasContext.Type.COURSE) {
            (canvasContext as Course).isTeacher
        } else {
            false
        }
    }

    fun getAssetsFile(context: Context, fileName: String): String {
        try {
            var file = ""
            val reader = BufferedReader(
                    InputStreamReader(context.assets.open(fileName)))
            var line: String? = ""
            while (line != null) {
                file += line
                line = reader.readLine()
            }
            reader.close()
            return file
        } catch (e: Exception) {
            return ""
        }
    }

    fun getBitmapFromAssets(context: Context, filePath: String): Bitmap? {
        val assetManager = context.assets
        val inputStream: InputStream
        var bitmap: Bitmap? = null
        try {
            inputStream = assetManager.open(filePath)
            bitmap = BitmapFactory.decodeStream(inputStream)
        } catch (e: IOException) {
            return null
        }
        return bitmap
    }

    fun makeBitmapForWebView(color: Int, bitmap: Bitmap?): String {
        if (bitmap == null) return ""
        val coloredBitmap = colorIt(color, bitmap)
        val outputStream = ByteArrayOutputStream()
        coloredBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        val imageBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT)
        coloredBitmap.recycle()
        return "data:image/png;base64," + imageBase64
    }

    fun colorIt(color: Int, map: Bitmap): Bitmap {
        val mutableBitmap = map.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val paint = Paint()
        paint.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        canvas.drawBitmap(mutableBitmap, 0f, 0f, paint)
        return mutableBitmap
    }

    fun getRGBColorString(color: Int): String {
        val r = color shr 16 and 0xFF
        val g = color shr 8 and 0xFF
        val b = color shr 0 and 0xFF
        return "rgb($r,$g,$b)"
    }

    fun getHexColorString(color: Int): String {
        return String.format ("#%06X", (0xFFFFFF and color))
    }

    fun formatDeletedInfoText(context: Context, discussionEntry: DiscussionEntry): String {
        if(discussionEntry.isDeleted) {
            val atSeparator = context.getString(R.string.at)
            val deletedText = String.format(context.getString(R.string.utils_discussionsDeleted),
                    DateHelper.getMonthDayAtTime(context, discussionEntry.updatedAt, atSeparator))
            return String.format("<div class=\"deleted_info\">%s</div>", deletedText)
        } else {
            return ""
        }
    }

    /**
     * If the avatar is valid then returns an empty string. Otherwise...
     * Returns an avatar bitmap converted into a base64 string for webviews.
     */
    private fun makeAvatarForWebView(context: Context, discussionEntry: DiscussionEntry): String {
        if(discussionEntry.author != null && ProfileUtils.shouldLoadAltAvatarImage(discussionEntry.author.avatarImageUrl)) {
            val avatarBitmap = ProfileUtils.getInitialsAvatarBitMap(
                    context, discussionEntry.author.displayName,
                    Color.TRANSPARENT,
                    ContextCompat.getColor(context, R.color.utils_defaultTextDark),
                    ContextCompat.getColor(context, R.color.utils_profileBorderColor))
            val outputStream = ByteArrayOutputStream()
            avatarBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val byteArray = outputStream.toByteArray()
            val imageBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT)
            avatarBitmap.recycle()
            return "data:image/png;base64," + imageBase64
        } else {
            if(discussionEntry.author == null || discussionEntry.author.avatarImageUrl.isNullOrBlank()) {
                //Unknown author
                val avatarBitmap = ProfileUtils.getInitialsAvatarBitMap(
                        context, "?",
                        Color.TRANSPARENT,
                        ContextCompat.getColor(context, R.color.utils_defaultTextDark),
                        ContextCompat.getColor(context, R.color.utils_profileBorderColor))
                val outputStream = ByteArrayOutputStream()
                avatarBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                val byteArray = outputStream.toByteArray()
                val imageBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT)
                avatarBitmap.recycle()
                return "data:image/png;base64," + imageBase64
            }
            return discussionEntry.author?.avatarImageUrl ?: ""
        }
    }

    //endregion
}