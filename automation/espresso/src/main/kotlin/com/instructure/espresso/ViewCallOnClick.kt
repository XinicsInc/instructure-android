package com.instructure.espresso


import android.support.test.espresso.UiController
import android.support.test.espresso.ViewAction
import android.support.test.espresso.matcher.ViewMatchers.*
import android.view.View
import com.instructure.espresso.WaitForViewMatcher.waitForView
import org.hamcrest.CoreMatchers.*
import org.hamcrest.Matcher

/**
 * Espresso click randomly turns into long press. view.callOnClick enables reliable clicking
 *
 * https://issuetracker.google.com/issues/37078920
 * https://stackoverflow.com/questions/32330671/android-espresso-performs-longclick-instead-of-click
 */
class ViewCallOnClick : ViewAction {
    override fun getConstraints(): Matcher<View> {
        return isDisplayingAtLeast(90)
    }

    override fun getDescription(): String {
        return "callOnClick"
    }

    override fun perform(uiController: UiController, view: View) {
        uiController.loopMainThreadUntilIdle()
        view.callOnClick()
        uiController.loopMainThreadForAtLeast(1000)
    }

    companion object {
        /**
         * Clickable views are either themself clickable or a descendant of a clickable view.
         * This will first attempt to find the view assuming it is clickable using [matcher].
         * If unsuccessful this will attempt to find the clickable parent of the view.
         */
        fun callOnClick(matcher: Matcher<View>) {
            val target = both(isClickable()).and(either(matcher).or(hasDescendant(matcher)))
            waitForView(target).perform(ViewCallOnClick())
        }
    }
}
