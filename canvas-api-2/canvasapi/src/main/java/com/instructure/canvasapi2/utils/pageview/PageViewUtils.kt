/*
 * Copyright (C) 2018 - present Instructure, Inc.
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
@file:Suppress("unused")

package com.instructure.canvasapi2.utils.pageview

import android.view.ViewTreeObserver
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.canvasapi2.utils.Logger
import com.instructure.canvasapi2.utils.weave.weave
import io.paperdb.Paper
import java.lang.ref.WeakReference

object PageViewUtils {

    private const val MIN_INTERACTION_SECONDS = 0.6

    private const val ENABLED = false

    private val book = Paper.book("pageViewEvents")

    @JvmStatic
    @Suppress("EXPERIMENTAL_FEATURE_WARNING")
    fun startEvent(eventName: String, url: String): PageViewEvent? {
        if (!ENABLED || ApiPrefs.token.isBlank()) return null
        val event = PageViewEvent(eventName, url, ApiPrefs.user?.id ?: return null)
        Logger.d("PageView: Event STARTED $url ($eventName)")
        weave { inBackground { book.write(event.key, event) } }
        return event
    }

    @JvmStatic
    @Suppress("EXPERIMENTAL_FEATURE_WARNING")
    fun stopEvent(event: PageViewEvent?) {
        if (!ENABLED || event == null || event.eventDuration > 0) return
        event.eventDuration = (System.currentTimeMillis() - event.timestamp.time) / 1000.0
        Logger.d("PageView: Event STOPPED ${event.url} (${event.eventName}) - ${event.eventDuration} seconds")
        weave {
            inBackground {
                if (event.eventDuration < MIN_INTERACTION_SECONDS) {
                    book.delete(event.key)
                    Logger.d("PageView: Event DROPPED ${event.url} (${event.eventName}) - ${event.eventDuration} seconds, TOO SHORT")
                } else {
                    book.write(event.key, event)
                }
            }
        }
    }

    @JvmStatic
    @Suppress("EXPERIMENTAL_FEATURE_WARNING")
    fun saveSingleEvent(event: PageViewEvent) {
        if (!ENABLED) return
        weave { inBackground { book.write(event.key, event) } }
        Logger.d("PageView: Event SAVED ${event.url} (${event.eventName})")
    }

    @JvmStatic
    @Suppress("EXPERIMENTAL_FEATURE_WARNING")
    fun clearEvents(events: List<PageViewEvent>) {
        if (!ENABLED) return
        weave { inBackground { events.forEach { book.delete(it.key) } } }
    }

    @JvmStatic
    @Suppress("EXPERIMENTAL_FEATURE_WARNING")
    fun clearAllEvents() {
        if (!ENABLED) return
        weave { inBackground { book.destroy() } }
    }

    @JvmStatic
    @Suppress("EXPERIMENTAL_FEATURE_WARNING")
    fun uploadData(loggingOut: Boolean = false) {
        if (!ENABLED) return
        // TODO: Upload page views
    }

}

class PageViewVisibilityTracker() {

    private var isResumed = true
    private var isUserHint = true
    private var isShowing = true
    private val customConditions = mutableMapOf<String, Boolean>()

    constructor(vararg conditions: String) : this() {
        conditions.forEach { customConditions[it] = false }
    }

    fun isVisible() = isResumed && isUserHint && isShowing && customConditions.values.all { it == true }

    fun trackResume(resumed: Boolean): Boolean {
        isResumed = resumed
        return isVisible()
    }

    fun trackUserHint(userHint: Boolean): Boolean {
        isUserHint = userHint
        return isVisible()
    }

    fun trackHidden(hidden: Boolean): Boolean {
        isShowing = !hidden
        return isVisible()
    }

    fun trackCustom(name: String, value: Boolean): Boolean {
        customConditions[name] = value
        return isVisible()
    }

}

interface PageViewWindowFocus {
    fun onPageViewWindowFocusChanged(hasFocus: Boolean)
}

class PageViewWindowFocusListener(focusInterface: PageViewWindowFocus) : ViewTreeObserver.OnWindowFocusChangeListener {

    private val ref: WeakReference<PageViewWindowFocus> = WeakReference(focusInterface)

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        ref.get()?.onPageViewWindowFocusChanged(hasFocus)
    }

}
