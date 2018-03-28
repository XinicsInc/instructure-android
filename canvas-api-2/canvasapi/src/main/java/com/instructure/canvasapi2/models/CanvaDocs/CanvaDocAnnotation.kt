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
 */

package com.instructure.canvasapi2.models.CanvaDocs

import android.graphics.Color
import com.google.gson.annotations.SerializedName
import com.instructure.canvasapi2.models.CanvasModel
import paperparcel.PaperParcel
import paperparcel.PaperParcelable
import java.util.*

@JvmSuppressWildcards
@PaperParcel
data class CanvaDocAnnotation(
        @SerializedName("id")
        var annotationId: String,
        @SerializedName("ctx_and_id")
        var ctxId: String? = null,
        @SerializedName("user_id")
        var userId: String? = null,
        @SerializedName("user_name")
        var userName: String? = null,
        @SerializedName("created_at")
        var createdAt: String? = null,
        @SerializedName("modified_at")
        var modifiedAt: String? = null,
        @SerializedName("document_id")
        var documentId: String? = null,
        var subject: String? = null,
        var flags: List<String>? = null,
        var page: Int,
        var context: String? = null,
        var width: Float? = null,
        @SerializedName("type")
        var annotationType: AnnotationType? = null,
        var rect: ArrayList<ArrayList<Float>>? = null,
        var opacity: Float? = null,
        var coords: ArrayList<ArrayList<ArrayList<Float>>>? = null,
        var color: String? = null,
        var icon: String? = null,
        var iconColor: String? = null,
        var contents: String? = null,
        var inklist: CanvaDocInkList? = null,
        @SerializedName("inreplyto")
        var inReplyTo: String? = null,
        var isEditable: Boolean
) : PaperParcelable, CanvasModel<CanvaDocAnnotation>() {

    override fun getId() = annotationId.hashCode().toLong()
    override fun getComparisonDate() = null
    override fun getComparisonString() = contents
    override fun describeContents() = 0

    enum class AnnotationType {
        @SerializedName("ink")
        INK,

        @SerializedName("highlight")
        HIGHLIGHT,

        @SerializedName("strikeout")
        STRIKEOUT,

        @SerializedName("square")
        SQUARE,

        @SerializedName("freetext")
        FREE_TEXT,

        @SerializedName("text")
        TEXT,

        @SerializedName("commentReply")
        COMMENT_REPLY
    }

    fun getColorInt(defaultColor: Int): Int {
        try { return Color.parseColor(color) }
        catch(e: Exception) { return defaultColor }
    }

    fun getIconColorInt(defaultColor: Int): Int {
        try { return Color.parseColor(iconColor) }
        catch(e: Exception) { return defaultColor }
    }

    companion object {
        val SQUARE_SUBJECT = "Rectangle"
        val HIGHLIGHT_SUBJECT = "Highlight"
        val INK_SUBJECT = "Free Hand"
        val TEXT_SUBJECT = "Comment"
        val FREE_TEXT_SUBJECT = "FeeText"
        val STRIKEOUT_SUBJECT = "Strikeout"
        val COMMENT_REPLY_SUBJECT = "Comment"
        @Suppress("unresolved_reference")
        @JvmField val CREATOR = PaperParcelCanvaDocAnnotation.CREATOR
    }
}