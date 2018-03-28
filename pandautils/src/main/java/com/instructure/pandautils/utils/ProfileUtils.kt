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

package com.instructure.pandautils.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import com.instructure.canvasapi2.models.*
import com.instructure.pandautils.R
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView


object ProfileUtils {

    private val noPictureUrls = listOf(
            "images/dotted_pic.png",
            "images%2Fmessages%2Favatar-50.png",
            "images/messages/avatar-50.png",
            "images/messages/avatar-group-50.png"
    )

    @JvmStatic
    fun shouldLoadAltAvatarImage(avatarUrl: String?): Boolean {
        return avatarUrl.isNullOrBlank() || avatarUrl?.indexOfAny(noPictureUrls) ?: -1 >= 0
    }

    @JvmStatic
    fun getUserInitials(username: String?): String {
        val name: String = username.takeUnless { it.isNullOrBlank() } ?: return "?"
        val initials = name.trim().split(Regex("\\s+")).map { it.toUpperCase()[0] }
        return if (initials.size == 2) {
            initials.joinToString("")
        } else {
            initials[0].toString()
        }
    }

    @JvmStatic
    fun loadAvatarForUser(avatar: CircleImageView, user: User) {
        loadAvatarForUser(avatar, user.name, user.avatarUrl)
    }

    @JvmStatic
    fun loadAvatarForUser(avatar: CircleImageView, user: BasicUser) {
        loadAvatarForUser(avatar, user.name, user.avatarUrl)
    }

    @JvmStatic
    fun loadAvatarForUser(avatar: CircleImageView, user: Author) {
        loadAvatarForUser(avatar, user.displayName, user.avatarImageUrl)
    }

    @JvmStatic
    fun loadAvatarForUser(avatar: CircleImageView, name: String?, url: String?) {
        val context = avatar.context
        if (shouldLoadAltAvatarImage(url)) {
            Picasso.with(context).cancelRequest(avatar)
            avatar.setAvatarImage(context, name)
        } else {
            Picasso.with(context)
                    .load(url)
                    .fit()
                    .placeholder(R.drawable.recipient_avatar_placeholder)
                    .centerCrop()
                    .into(avatar, object : Callback {
                        override fun onSuccess() {}

                        override fun onError() {
                            avatar.setAvatarImage(context, name)
                        }
                    })
        }
    }

    @JvmStatic
    fun loadAvatarsForConversation(avatar: CircleImageView, conversation: Conversation, onUserClicked: (BasicUser, CanvasContext) -> Unit) {
        val users = conversation.participants

        if (users.size == 0) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            avatar.transitionName = com.instructure.pandautils.utils.Const.MESSAGE + conversation.id.toString()
        }

        val firstUser = users[0]
        firstUser.avatarUrl = conversation.avatarUrl

        // Reset click listener
        avatar.setOnClickListener(null)

        if (users.size > 2) {
            avatar.setImageResource(R.drawable.vd_group)
            avatar.setVisible()
        } else {
            loadAvatarForUser(avatar, firstUser.name, firstUser.avatarUrl)

            // Set click listener to show context card
            val canvasContext = CanvasContext.fromContextCode(conversation.contextCode)
            if (canvasContext is Course) {
                avatar.setupAvatarA11y(firstUser.name)
                avatar.onClick { onUserClicked(firstUser, canvasContext) }
            }
        }
    }

    @JvmStatic
    @JvmOverloads
    fun getInitialsAvatarBitMap(
            context: Context,
            username: String,
            backgroundColor: Int = Color.WHITE,
            textColor: Int = Color.GRAY,
            borderColor: Int = Color.GRAY
    ): Bitmap {
        val initials = ProfileUtils.getUserInitials(username)
        val drawable = TextDrawable.builder()
                .beginConfig()
                .height(context.resources.getDimensionPixelSize(R.dimen.avatar_size))
                .width(context.resources.getDimensionPixelSize(R.dimen.avatar_size))
                .toUpperCase()
                .textColor(textColor)
                .useFont(Typeface.DEFAULT_BOLD)
                .withBorderColor(borderColor)
                .withBorder(context.DP(1).toInt())
                .endConfig()
                .buildRound(initials, backgroundColor)

        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable.draw(canvas)
        return bitmap
    }
}
