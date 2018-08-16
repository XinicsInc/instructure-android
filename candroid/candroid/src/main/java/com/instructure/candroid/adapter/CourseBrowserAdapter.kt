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
package com.instructure.candroid.adapter

import android.graphics.drawable.Drawable
import android.support.graphics.drawable.VectorDrawableCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.instructure.candroid.R
import com.instructure.candroid.util.TabHelper
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.Tab
import com.instructure.pandautils.utils.color
import kotlinx.android.synthetic.main.adapter_course_browser.view.*
import kotlinx.android.synthetic.main.adapter_course_browser_home.view.*
import kotlinx.android.synthetic.main.adapter_course_browser_web_view.view.*

class CourseBrowserAdapter(val items: List<Tab>, val canvasContext: CanvasContext, val callback: (Tab) -> Unit, private val homePageTitle: String? = null) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
        return if(viewType == HOME) {
            CourseBrowserHomeViewHolder(LayoutInflater.from(parent?.context)
                    .inflate(CourseBrowserHomeViewHolder.holderResId, parent, false), canvasContext.color, canvasContext, homePageTitle)
        } else if (viewType == WEB_VIEW_ITEM) {
            CourseBrowserWebViewHolder(LayoutInflater.from(parent?.context)
                    .inflate(CourseBrowserWebViewHolder.holderResId, parent, false), canvasContext.color)
        } else {
            CourseBrowserViewHolder(LayoutInflater.from(parent?.context)
                    .inflate(CourseBrowserViewHolder.holderResId, parent, false), canvasContext.color)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        val tab = items[position]
        if(tab.tabId == Tab.HOME_ID && holder is CourseBrowserHomeViewHolder) {
            holder.bind(holder, tab, callback)
        } else if(holder is CourseBrowserViewHolder) {
            holder.bind(tab, callback)
        } else if(holder is CourseBrowserWebViewHolder) {
            holder.bind(tab, callback)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val tabId = items[position].tabId
        return if(tabId == Tab.HOME_ID) HOME else {
            if(tabId == Tab.COLLABORATIONS_ID || tabId == Tab.CONFERENCES_ID || tabId == Tab.OUTCOMES_ID) WEB_VIEW_ITEM
            else ITEM
        }
    }

    companion object {
        const val HOME = 0
        const val ITEM = 1
        const val WEB_VIEW_ITEM = 2
    }
}

class CourseBrowserHomeViewHolder(view: View, val color: Int, val canvasContext: CanvasContext, private val homePageTitle: String? = null) : RecyclerView.ViewHolder(view) {

    fun bind(holder: CourseBrowserHomeViewHolder, tab: Tab, clickedCallback: (Tab) -> Unit) {
        holder.itemView.homeLabel.text = tab.label
        if(TabHelper.isHomeTabAPage(canvasContext)) holder.itemView.homeSubLabel.text = homePageTitle
        else holder.itemView.homeSubLabel.text = TabHelper.getHomePageDisplayString(canvasContext, tab)
        holder.itemView.setOnClickListener {
            clickedCallback(tab)
        }
    }

    companion object {
        val holderResId = R.layout.adapter_course_browser_home
    }
}

class CourseBrowserWebViewHolder(view: View, val color: Int) : RecyclerView.ViewHolder(view) {

    fun bind(tab: Tab, clickedCallback: (Tab) -> Unit) {

        val res: Int = when (tab.tabId) {
            Tab.OUTCOMES_ID -> R.drawable.vd_outcomes
            Tab.CONFERENCES_ID -> R.drawable.vd_conferences
            else /* Tab.COLLABORATIONS_ID */ -> R.drawable.vd_collaborations
        }

        var d = VectorDrawableCompat.create(itemView.context.resources, res, null)
        d = DrawableCompat.wrap(d!!) as VectorDrawableCompat?
        DrawableCompat.setTint(d!!, color)

        setupTab(tab, d, clickedCallback)
    }

    /**
     * Fill in the view with tabby goodness
     *
     * @param tab The tab (Assignment, Discussions, etc)
     * @param res The image resource for the tab
     * @param callback What we do when the user clicks this tab
     */
    private fun setupTab(tab: Tab, drawable: Drawable, callback: (Tab) -> Unit) {
        itemView.unsupportedLabel.text = tab.label
        itemView.unsupportedIcon.setImageDrawable(drawable)

        if(tab.tabId == Tab.CONFERENCES_ID) {
            itemView.unsupportedSubLabel.setText(R.string.unsupportedTabLabel)
        } else {
            itemView.unsupportedSubLabel.setText(R.string.opensInWebView)
            itemView.setOnClickListener {
                callback(tab)
            }
        }
    }

    companion object {
        val holderResId = R.layout.adapter_course_browser_web_view
    }
}

class CourseBrowserViewHolder(view: View, val color: Int) : RecyclerView.ViewHolder(view) {

    fun bind(tab: Tab, clickedCallback: (Tab) -> Unit) {
        val res: Int = when (tab.tabId) {
            Tab.ASSIGNMENTS_ID -> R.drawable.vd_assignment
            Tab.QUIZZES_ID -> R.drawable.vd_quiz
            Tab.DISCUSSIONS_ID -> R.drawable.vd_discussion
            Tab.ANNOUNCEMENTS_ID -> R.drawable.vd_announcement
            Tab.PEOPLE_ID -> R.drawable.vd_people
            Tab.FILES_ID -> R.drawable.vd_files
            Tab.PAGES_ID -> R.drawable.vd_pages
            Tab.MODULES_ID -> R.drawable.vd_modules
            Tab.SYLLABUS_ID -> R.drawable.vd_syllabus
            Tab.OUTCOMES_ID -> R.drawable.vd_outcomes
            Tab.GRADES_ID -> R.drawable.vd_grades
            Tab.HOME_ID -> R.drawable.vd_home
            Tab.CONFERENCES_ID -> R.drawable.vd_conferences
            Tab.COLLABORATIONS_ID -> R.drawable.vd_collaborations
            Tab.SETTINGS_ID -> R.drawable.vd_settings
            else -> {
                //Determine if its the attendance tool
                if(tab.type == Tab.TYPE_EXTERNAL) {
                    R.drawable.vd_lti
                } else R.drawable.vd_canvas_logo
            }
        }

        var d = VectorDrawableCompat.create(itemView.context.resources, res, null)
        d = DrawableCompat.wrap(d!!) as VectorDrawableCompat?
        DrawableCompat.setTint(d!!, color)

        setupTab(tab, d, clickedCallback)
    }

    /**
     * Fill in the view with tabby goodness
     *
     * @param tab The tab (Assignment, Discussions, etc)
     * @param res The image resource for the tab
     * @param callback What we do when the user clicks this tab
     */
    private fun setupTab(tab: Tab, drawable: Drawable, callback: (Tab) -> Unit) {
        itemView.label.text = tab.label
        itemView.icon.setImageDrawable(drawable)
        itemView.setOnClickListener {
            callback(tab)
        }
    }

    companion object {
        val holderResId = R.layout.adapter_course_browser
    }
}
