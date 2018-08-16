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
@file:Suppress("unused")

package com.instructure.teacher.ui.utils

import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso
import android.support.test.espresso.ViewInteraction
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions
import android.support.test.espresso.matcher.ViewMatchers
import android.view.View
import com.instructure.espresso.ViewCallOnClick
import com.instructure.espresso.WaitForViewMatcher
import com.instructure.teacher.R
import com.instructure.teacher.ui.pages.BasePage
import org.hamcrest.Matcher
import org.hamcrest.Matchers


fun ViewInteraction.assertVisible(): ViewInteraction
        = check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

fun ViewInteraction.assertInvisible(): ViewInteraction
        = check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.INVISIBLE)))

fun ViewInteraction.assertGone(): ViewInteraction
        = check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE)))

fun ViewInteraction.assertDisplayed(): ViewInteraction
        = check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

fun ViewInteraction.assertCompletelyDisplayed(): ViewInteraction
        = check(ViewAssertions.matches(ViewMatchers.isCompletelyDisplayed()))

fun ViewInteraction.assertNotDisplayed(): ViewInteraction
        = check(ViewAssertions.matches(Matchers.not(ViewMatchers.isDisplayed())))

fun ViewInteraction.assertHasText(text: String): ViewInteraction
        = check(ViewAssertions.matches(ViewMatchers.withText(text)))

fun ViewInteraction.assertHasText(stringId: Int): ViewInteraction
        = check(ViewAssertions.matches(ViewMatchers.withText(stringId)))

fun ViewInteraction.assertNotHasText(stringId: Int): ViewInteraction
        = check(ViewAssertions.matches(Matchers.not(ViewMatchers.withText(stringId))))

fun ViewInteraction.assertContainsText(text: String): ViewInteraction
        = check(ViewAssertions.matches(ViewMatchers.withText(Matchers.containsString(text))))

fun ViewInteraction.assertHasContentDescription(text: String): ViewInteraction
        = check(ViewAssertions.matches(ViewMatchers.withContentDescription(text)))

fun ViewInteraction.assertHasContentDescription(stringId: Int): ViewInteraction
        = check(ViewAssertions.matches(ViewMatchers.withContentDescription(stringId)))

// Extensions for ViewActions

fun ViewInteraction.typeText(arg0: String): ViewInteraction = perform(ViewActions.typeText(arg0))

fun ViewInteraction.pressKey(arg0: android.support.test.espresso.action.EspressoKey): ViewInteraction = perform(ViewActions.pressKey(arg0))

fun ViewInteraction.pressKey(arg0: Int): ViewInteraction = perform(ViewActions.pressKey(arg0))

fun ViewInteraction.swipeUp(): ViewInteraction = perform(ViewActions.swipeUp())

fun ViewInteraction.click(): ViewInteraction = perform(ViewActions.click())

fun ViewInteraction.click(arg0: android.support.test.espresso.ViewAction): ViewInteraction = perform(ViewActions.click(arg0))

fun ViewInteraction.actionWithAssertions(arg0: android.support.test.espresso.ViewAction): ViewInteraction = perform(ViewActions.actionWithAssertions(arg0))

fun ViewInteraction.clearText(): ViewInteraction = perform(ViewActions.clearText())

fun ViewInteraction.swipeLeft(): ViewInteraction = perform(ViewActions.swipeLeft())

fun ViewInteraction.swipeRight(): ViewInteraction = perform(ViewActions.swipeRight())

fun ViewInteraction.swipeDown(): ViewInteraction = perform(ViewActions.swipeDown())

fun ViewInteraction.closeSoftKeyboard(): ViewInteraction = perform(ViewActions.closeSoftKeyboard())

fun ViewInteraction.pressImeActionButton(): ViewInteraction = perform(ViewActions.pressImeActionButton())

fun ViewInteraction.pressBack(): ViewInteraction = perform(ViewActions.pressBack())

fun ViewInteraction.pressMenuKey(): ViewInteraction = perform(ViewActions.pressMenuKey())

fun ViewInteraction.doubleClick(): ViewInteraction = perform(ViewActions.doubleClick())

fun ViewInteraction.longClick(): ViewInteraction = perform(ViewActions.longClick())

fun ViewInteraction.scrollTo(): ViewInteraction = perform(ViewActions.scrollTo())

fun ViewInteraction.typeTextIntoFocusedView(arg0: String): ViewInteraction = perform(ViewActions.typeTextIntoFocusedView(arg0))

fun ViewInteraction.replaceText(arg0: String): ViewInteraction = perform(ViewActions.replaceText(arg0))

fun ViewInteraction.openLinkWithText(arg0: org.hamcrest.Matcher<String>): ViewInteraction = perform(ViewActions.openLinkWithText(arg0))

fun ViewInteraction.openLinkWithText(arg0: String): ViewInteraction = perform(ViewActions.openLinkWithText(arg0))

fun ViewInteraction.openLink(arg0: org.hamcrest.Matcher<String>, arg1: org.hamcrest.Matcher<android.net.Uri>): ViewInteraction = perform(ViewActions.openLink(arg0, arg1))

fun ViewInteraction.openLinkWithUri(arg0: org.hamcrest.Matcher<android.net.Uri>): ViewInteraction = perform(ViewActions.openLinkWithUri(arg0))

fun ViewInteraction.openLinkWithUri(arg0: String): ViewInteraction = perform(ViewActions.openLinkWithUri(arg0))

fun ViewInteraction.pageToItem(pageNumber: Int): ViewInteraction = perform(SetViewPagerCurrentItemAction(pageNumber))

// OnView extensions

fun BasePage.onViewWithParent(arg0: Matcher<View>): ViewInteraction = Espresso.onView(ViewMatchers.withParent(arg0))

fun BasePage.onViewWithText(arg0: String): ViewInteraction = Espresso.onView(ViewMatchers.withText(arg0))

fun BasePage.onViewWithText(arg0: Matcher<String>): ViewInteraction = Espresso.onView(ViewMatchers.withText(arg0))

fun BasePage.onViewWithText(arg0: Int): ViewInteraction = Espresso.onView(ViewMatchers.withText(arg0))

fun BasePage.onViewWithId(arg0: Int): ViewInteraction = Espresso.onView(ViewMatchers.withId(arg0))

fun BasePage.onViewWithId(arg0: Matcher<Int>): ViewInteraction = Espresso.onView(ViewMatchers.withId(arg0))

fun BasePage.onViewWithClassName(arg0: Matcher<String>): ViewInteraction = Espresso.onView(ViewMatchers.withClassName(arg0))

fun BasePage.onViewWithContentDescription(arg0: Int): ViewInteraction = Espresso.onView(ViewMatchers.withContentDescription(arg0))

fun BasePage.onViewWithContentDescription(arg0: Matcher<out CharSequence>): ViewInteraction = Espresso.onView(ViewMatchers.withContentDescription(arg0))

fun BasePage.onViewWithContentDescription(arg0: String): ViewInteraction = Espresso.onView(ViewMatchers.withContentDescription(arg0))

fun BasePage.onViewWithResourceName(arg0: String): ViewInteraction = Espresso.onView(ViewMatchers.withResourceName(arg0))

fun BasePage.onViewWithResourceName(arg0: Matcher<String>): ViewInteraction = Espresso.onView(ViewMatchers.withResourceName(arg0))

fun BasePage.onViewWithTagKey(arg0: Int, arg1: Matcher<Any>): ViewInteraction = Espresso.onView(ViewMatchers.withTagKey(arg0, arg1))

fun BasePage.onViewWithTagKey(arg0: Int): ViewInteraction = Espresso.onView(ViewMatchers.withTagKey(arg0))

fun BasePage.onViewWithTagValue(arg0: Matcher<Any>): ViewInteraction = Espresso.onView(ViewMatchers.withTagValue(arg0))

fun BasePage.onViewWithHint(arg0: Int): ViewInteraction = Espresso.onView(ViewMatchers.withHint(arg0))

fun BasePage.onViewWithHint(arg0: Matcher<String>): ViewInteraction = Espresso.onView(ViewMatchers.withHint(arg0))

fun BasePage.onViewWithHint(arg0: String): ViewInteraction = Espresso.onView(ViewMatchers.withHint(arg0))

fun BasePage.onViewWithEffectiveVisibility(arg0: android.support.test.espresso.matcher.ViewMatchers.Visibility): ViewInteraction = Espresso.onView(ViewMatchers.withEffectiveVisibility(arg0))

fun BasePage.onViewWithChild(arg0: Matcher<View>): ViewInteraction = Espresso.onView(ViewMatchers.withChild(arg0))

fun BasePage.onViewWithSpinnerText(arg0: String): ViewInteraction = Espresso.onView(ViewMatchers.withSpinnerText(arg0))

fun BasePage.onViewWithSpinnerText(arg0: Matcher<String>): ViewInteraction = Espresso.onView(ViewMatchers.withSpinnerText(arg0))

fun BasePage.onViewWithSpinnerText(arg0: Int): ViewInteraction = Espresso.onView(ViewMatchers.withSpinnerText(arg0))

fun BasePage.onViewWithInputType(arg0: Int): ViewInteraction = Espresso.onView(ViewMatchers.withInputType(arg0))


// WaitForView extensions

fun BasePage.waitForViewWithParent(arg0: Matcher<View>): ViewInteraction = WaitForViewMatcher.waitForView(ViewMatchers.withParent(arg0))

fun BasePage.waitForViewWithText(arg0: String): ViewInteraction = WaitForViewMatcher.waitForView(ViewMatchers.withText(arg0))

fun BasePage.waitForViewWithText(arg0: Matcher<String>): ViewInteraction = WaitForViewMatcher.waitForView(ViewMatchers.withText(arg0))

fun BasePage.waitForViewWithText(arg0: Int): ViewInteraction = WaitForViewMatcher.waitForView(ViewMatchers.withText(arg0))

fun BasePage.waitForViewWithId(arg0: Int): ViewInteraction = WaitForViewMatcher.waitForView(ViewMatchers.withId(arg0))

fun BasePage.waitForViewWithId(arg0: Matcher<Int>): ViewInteraction = WaitForViewMatcher.waitForView(ViewMatchers.withId(arg0))

fun BasePage.waitForViewWithClassName(arg0: Matcher<String>): ViewInteraction = WaitForViewMatcher.waitForView(ViewMatchers.withClassName(arg0))

fun BasePage.waitForViewWithContentDescription(arg0: Int): ViewInteraction = WaitForViewMatcher.waitForView(ViewMatchers.withContentDescription(arg0))

fun BasePage.waitForViewWithContentDescription(arg0: Matcher<out CharSequence>): ViewInteraction = WaitForViewMatcher.waitForView(ViewMatchers.withContentDescription(arg0))

fun BasePage.waitForViewWithContentDescription(arg0: String): ViewInteraction = WaitForViewMatcher.waitForView(ViewMatchers.withContentDescription(arg0))

fun BasePage.waitForViewWithResourceName(arg0: String): ViewInteraction = WaitForViewMatcher.waitForView(ViewMatchers.withResourceName(arg0))

fun BasePage.waitForViewWithResourceName(arg0: Matcher<String>): ViewInteraction = WaitForViewMatcher.waitForView(ViewMatchers.withResourceName(arg0))

fun BasePage.waitForViewWithTagKey(arg0: Int, arg1: Matcher<Any>): ViewInteraction = WaitForViewMatcher.waitForView(ViewMatchers.withTagKey(arg0, arg1))

fun BasePage.waitForViewWithTagKey(arg0: Int): ViewInteraction = WaitForViewMatcher.waitForView(ViewMatchers.withTagKey(arg0))

fun BasePage.waitForViewWithTagValue(arg0: Matcher<Any>): ViewInteraction = WaitForViewMatcher.waitForView(ViewMatchers.withTagValue(arg0))

fun BasePage.waitForViewWithHint(arg0: Int): ViewInteraction = WaitForViewMatcher.waitForView(ViewMatchers.withHint(arg0))

fun BasePage.waitForViewWithHint(arg0: Matcher<String>): ViewInteraction = WaitForViewMatcher.waitForView(ViewMatchers.withHint(arg0))

fun BasePage.waitForViewWithHint(arg0: String): ViewInteraction = WaitForViewMatcher.waitForView(ViewMatchers.withHint(arg0))

fun BasePage.waitForViewWithEffectiveVisibility(arg0: android.support.test.espresso.matcher.ViewMatchers.Visibility): ViewInteraction = WaitForViewMatcher.waitForView(ViewMatchers.withEffectiveVisibility(arg0))

fun BasePage.waitForViewWithChild(arg0: Matcher<View>): ViewInteraction = WaitForViewMatcher.waitForView(ViewMatchers.withChild(arg0))

fun BasePage.waitForViewWithSpinnerText(arg0: String): ViewInteraction = WaitForViewMatcher.waitForView(ViewMatchers.withSpinnerText(arg0))

fun BasePage.waitForViewWithSpinnerText(arg0: Matcher<String>): ViewInteraction = WaitForViewMatcher.waitForView(ViewMatchers.withSpinnerText(arg0))

fun BasePage.waitForViewWithSpinnerText(arg0: Int): ViewInteraction = WaitForViewMatcher.waitForView(ViewMatchers.withSpinnerText(arg0))

fun BasePage.waitForViewWithInputType(arg0: Int): ViewInteraction = WaitForViewMatcher.waitForView(ViewMatchers.withInputType(arg0))


// Navigation Extensions

fun BasePage.clickCoursesTab() { waitForViewWithId(R.id.tab_courses).perform(click()) }

fun BasePage.clickInboxTab() { waitForViewWithId(R.id.tab_inbox).perform(click()) }

fun BasePage.clickProfileMenu() { waitForViewWithContentDescription(R.string.navigation_drawer_open).perform(click()) }

fun BasePage.getStringFromResource(stringResource: Int): String{
    val targetContext = InstrumentationRegistry.getTargetContext()
    return targetContext.resources.getString(stringResource)
}

fun BasePage.callOnClick(matcher: Matcher<View>) = ViewCallOnClick.callOnClick(matcher)
