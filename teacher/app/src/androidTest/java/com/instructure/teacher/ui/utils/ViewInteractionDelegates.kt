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
package com.instructure.teacher.ui.utils

import android.support.annotation.IdRes
import android.support.annotation.StringRes
import android.support.test.espresso.Espresso
import android.support.test.espresso.EspressoException
import android.support.test.espresso.ViewInteraction
import android.support.test.espresso.matcher.ViewMatchers
import com.instructure.espresso.WaitForViewMatcher
import com.instructure.teacher.R
import com.instructure.teacher.ui.utils.pageAssert.PageAssert
import org.hamcrest.Matchers
import org.hamcrest.Matchers.equalToIgnoringCase
import java.lang.RuntimeException
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Abstract class for implementing concrete delegate classes for [ViewInteraction] properties.
 *
 * If the class hosting the delegated property delegate is an instance of [PageAssert]
 * and [true] is passed for the [autoAssert] parameter, the delegate provider will attempt to
 * register the [ViewInteraction] with the asserter so that it may automatically assert that the
 * view is displayed whenever [assertPageObjects] is invoked.
 *
 * If [true] is passed for [autoAssert] and the hosting class is NOT an instance of [PageAssert]
 * then the delegate provider will throw an exception.
 */
abstract class ViewInteractionDelegate(val autoAssert: Boolean) : ReadOnlyProperty<Any, ViewInteraction> {

    operator fun provideDelegate(thisRef: Any, prop: KProperty<*>): ReadOnlyProperty<Any, ViewInteraction> {
        if (autoAssert) {
            if (thisRef is PageAssert) thisRef.registerPropertyInfo(Pair(this, prop))
            else throw PageAsserterException("Unable to register property ${prop.name} in class ${thisRef.javaClass.simpleName}. ${thisRef.javaClass.simpleName} is not an instance of PageObjectAsserter")
        }
        return this
    }

    abstract fun onProvide(): ViewInteraction

    override fun getValue(thisRef: Any, property: KProperty<*>): ViewInteraction = onProvide()
}

class PageAsserterException(message: String) : RuntimeException(message), EspressoException

class OnViewWithId(@IdRes val viewId: Int, autoAssert: Boolean = true) : ViewInteractionDelegate(autoAssert) {
    override fun onProvide(): ViewInteraction = Espresso.onView(ViewMatchers.withId(viewId))
}

class OnViewWithContentDescription(@StringRes val stringResId: Int, autoAssert: Boolean = true) : ViewInteractionDelegate(autoAssert) {
    override fun onProvide(): ViewInteraction = Espresso.onView(ViewMatchers.withContentDescription(stringResId))
}

class OnViewWithStringContentDescription(val text: String, autoAssert: Boolean = true) : ViewInteractionDelegate(autoAssert) {
    override fun onProvide(): ViewInteraction = Espresso.onView(ViewMatchers.withContentDescription(text))
}

class OnViewWithText(@StringRes val stringResId: Int, autoAssert: Boolean = true) : ViewInteractionDelegate(autoAssert) {
    override fun onProvide(): ViewInteraction = Espresso.onView(ViewMatchers.withText(stringResId))
}

class OnViewWithStringText(val text: String, autoAssert: Boolean = true) : ViewInteractionDelegate(autoAssert) {
    override fun onProvide(): ViewInteraction = Espresso.onView(ViewMatchers.withText(text))
}

class OnViewWithStringTextIgnoreCase(val text: String, autoAssert: Boolean = true) : ViewInteractionDelegate(autoAssert) {
    override fun onProvide(): ViewInteraction = Espresso.onView(ViewMatchers.withText(equalToIgnoringCase(text)))
}

class WaitForViewWithId(@IdRes val viewId: Int, autoAssert: Boolean = false) : ViewInteractionDelegate(autoAssert) {
    override fun onProvide(): ViewInteraction = WaitForViewMatcher.waitForView(ViewMatchers.withId(viewId))
}

class WaitForViewWithContentDescription(@StringRes val stringResId: Int, autoAssert: Boolean = false) : ViewInteractionDelegate(autoAssert) {
    override fun onProvide(): ViewInteraction = WaitForViewMatcher.waitForView(ViewMatchers.withContentDescription(stringResId))
}

class WaitForViewWithStringContentDescription(val text: String, autoAssert: Boolean = false) : ViewInteractionDelegate(autoAssert) {
    override fun onProvide(): ViewInteraction = WaitForViewMatcher.waitForView(ViewMatchers.withContentDescription(text))
}

class WaitForViewWithText(@StringRes val stringResId: Int, autoAssert: Boolean = false) : ViewInteractionDelegate(autoAssert) {
    override fun onProvide(): ViewInteraction = WaitForViewMatcher.waitForView(ViewMatchers.withText(stringResId))
}

class WaitForViewWithStringTextIgnoreCase(val text: String, autoAssert: Boolean = false) : ViewInteractionDelegate(autoAssert) {
    override fun onProvide(): ViewInteraction = WaitForViewMatcher.waitForView(ViewMatchers.withText(equalToIgnoringCase(text)))
}

class WaitForViewWithStringText(val text: String, autoAssert: Boolean = false) : ViewInteractionDelegate(autoAssert) {
    override fun onProvide(): ViewInteraction = WaitForViewMatcher.waitForView(ViewMatchers.withText(text))
}

/**
 *  The toolbar's title text view's resource id is the same as the course text view in course cards.
 *  Use this to narrow the matcher to the toolbar itself.
 */
class WaitForToolbarTitle(val text: Int, autoAssert: Boolean = true) : ViewInteractionDelegate(autoAssert) {
    override fun onProvide(): ViewInteraction = Espresso.onView(Matchers.allOf(ViewMatchers.withText(text), ViewMatchers.withParent(ViewMatchers.withId(R.id.toolbar))))
}
