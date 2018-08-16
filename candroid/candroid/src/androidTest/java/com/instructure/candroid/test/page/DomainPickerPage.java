/*
 * Copyright (C) 2016 - present Instructure, Inc.
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

package com.instructure.candroid.test.page;

import android.support.test.espresso.Espresso;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.web.webdriver.Locator;
import android.view.View;
import android.widget.ListView;

import com.instructure.candroid.R;
import static com.instructure.candroid.test.utils.UserProfile.DOMAIN;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import java.util.concurrent.TimeUnit;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isClickable;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isFocusable;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withSpinnerText;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.support.test.espresso.web.assertion.WebViewAssertions.webMatches;
import static android.support.test.espresso.web.sugar.Web.onWebView;
import static android.support.test.espresso.web.webdriver.DriverAtoms.findElement;
import static android.support.test.espresso.web.webdriver.DriverAtoms.getText;
import static com.instructure.espresso.WaitForCheckMatcher.waitFor;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.AllOf.allOf;

public class DomainPickerPage {

    private static String H1_TAG = "H1";
    private static String H1_TEXT = "How do I find my institution's URL to access Canvas apps on my mobile device?";

    /******************* UI Element Locator Methods ***************************/

    // Domain Picker Page
    private ViewInteraction canvasLogo() { return onView(withId(R.id.canvas_logo)); }
    private ViewInteraction connectButton() { return onView(withId(R.id.connect)); }
    private ViewInteraction domainField() { return onView(withId(R.id.enterURL)); }
    private ViewInteraction domainListView() { return onView(withId(R.id.listview)); }

    // Help Menu
    private ViewInteraction helpMenuButton() { return onView(withId(R.id.help_button)); }
    private ViewInteraction helpMenuImageButton() { return onView(withId(R.id.image)); }
    private ViewInteraction canvasGuidesButton() { return onView(withId(R.id.searchGuides)); }
    private ViewInteraction reportProblemButton() { return onView(withId(R.id.reportProblem)); }

    // Canvas Guides
    private ViewInteraction navigateUpButton() { return onView(withContentDescription("Navigate up")); }
    private ViewInteraction canvasGuidesTitle() { return onView(withText("Canvas Guides")); }

    // Report Problem
    private ViewInteraction reportProblemTitle() { return onView(withId(R.id.dialog_custom_title)); }
    private ViewInteraction reportSubject() { return onView(withId(R.id.subject)); }
    private ViewInteraction reportSubjectField() { return onView(withId(R.id.subjectEditText)); }
    private ViewInteraction reportEmail() { return onView(withId(R.id.emailAddress)); }
    private ViewInteraction reportEmailField() { return onView(withId(R.id.emailAddressEditText)); }
    private ViewInteraction reportDescription() { return onView(withId(R.id.description)); }
    private ViewInteraction reportDescriptionField() { return onView(withId(R.id.descriptionEditText)); }
    private ViewInteraction reportSeverityPrompt() { return onView(withId(R.id.severityPrompt)); }
    private ViewInteraction reportSeveritySpinner() { return onView(withId(R.id.severitySpinner)); }
    private ViewInteraction reportCancel() { return onView(withId(R.id.dialog_custom_cancel)); }
    private ViewInteraction reportSend() { return onView(withId(R.id.dialog_custom_confirm)); }

    // Severity Strings
    private String reportSeverityCasualString() { return "Just a casual question, comment, idea, suggestion…"; }
    private String reportSeverityHelpString() { return "I need some help but it's not urgent."; }
    private String reportSeverityBrokenString() { return "Something's broken but I can work around it to get what I need done."; }
    private String reportSeverityUrgentString() { return "I can't get things done until I hear back from you."; }
    private String reportSeverityCriticalString() { return "EXTREME CRITICAL EMERGENCY!!"; }

    // Severity Elements
    private ViewInteraction reportSeverityCasual() { return onView(withText(reportSeverityCasualString())); }
    private ViewInteraction reportSeverityHelp() { return onView(withText(reportSeverityHelpString())); }
    private ViewInteraction reportSeverityBroken() { return onView(withText(reportSeverityBrokenString())); }
    private ViewInteraction reportSeverityUrgent() { return onView(withText(reportSeverityUrgentString())); }
    private ViewInteraction reportSeverityCritical() { return onView(withText(reportSeverityCriticalString())); }

    /************************ Assertion Helpers *******************************/

    public void assertDomainListView() {
        onView(withId(R.id.listview))
                .check(matches(waitFor(isDisplayed())))
                .check(matches(waitFor(new TypeSafeMatcher<View>() {
                    @Override
                    protected boolean matchesSafely(View item) {
                        ListView listView = (ListView) item;
                        if (listView.getCount() >= 4) {
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public void describeTo(Description description) {
                        description.appendText("domain listview to have at least 4 domains listed");
                    }
                })));
    }

    public void assertPageObjects() {
        canvasLogo().check(matches(isDisplayed()));
        domainField().check(matches(isDisplayed()));
        helpMenuButton().check(matches(isDisplayed()));
    }

    public void assertClickableElement(ViewInteraction element) {
        element.check(matches(isDisplayed()));
        element.check(matches(isClickable()));
        element.check(matches(isFocusable()));
    }

    public void assertReportProblem() {
        // Labels
        reportProblemTitle().check(matches(isDisplayed()));
        reportSubject().check(matches(isDisplayed()));
        reportEmail().check(matches(isDisplayed()));
        reportDescription().check(matches(isDisplayed()));
        reportSeverityPrompt().check(matches(isDisplayed()));

        // Text Fields
        assertClickableElement(reportSubjectField());
        assertClickableElement(reportEmailField());
        assertClickableElement(reportDescriptionField());

        // Spinner
        assertClickableElement(reportSeveritySpinner());

        // Form Controls
        assertClickableElement(reportCancel());
        assertClickableElement(reportSend());
    }

    public void assertReportSeverities() {
        ViewInteraction spinner = reportSeveritySpinner();
        spinner.check(matches(withSpinnerText(containsString(reportSeverityCasualString()))));

        // checks spinner's default setting and opens the spinner
        onView(allOf(withId(R.id.text), withParent(withId(R.id.severitySpinner))))
                .check(matches(isDisplayed()))
                .check(matches(withText(reportSeverityCasualString())));

        // TODO: add test converage for selecting severity option
    }

    public void assertCanvasGuides() {
        navigateUpButton().check(matches(isDisplayed()));
        canvasGuidesTitle().check(matches(isDisplayed()));
        // Fix TimeoutException by using 3 minute timeout instead of default 10 seconds.
        onWebView(withId(R.id.internal_webview))
                .withTimeout(3, TimeUnit.MINUTES)
                .withElement(findElement(Locator.CSS_SELECTOR, H1_TAG))
                .check(webMatches(getText(), containsString(H1_TEXT)));
    }

    /************************ UI Action Helpers *******************************/

    public void openHelpMenu() {
        helpMenuButton().perform(click());
    }

    public void openHelpMenuFromDomainList() {
        helpMenuImageButton().perform(click());
    }

    public void clickCanvasGuides() {
        canvasGuidesButton().perform(click());
    }

    public void clickReportProblem() {
        reportProblemButton().perform(click());
        // Keyboard hides elements so close it until we're ready to type something.
        Espresso.closeSoftKeyboard();
    }

    public void tapReportCancel() {
        // Opening Report A Problem will automatically open the keyboard which hides the cancel button.
        // When the keyboard is open the DomainPicker page hides the Canvas logo & help button
        // Close the keyboard so report cancel can be clicked and the logo & help button are visible.
        Espresso.closeSoftKeyboard();
        reportCancel().perform(click());
    }

    public void tapNavigateUp() {
        navigateUpButton().perform(click());
    }

    public void enterSchool(String domain) {
        domainField().perform(replaceText(domain));
    }

    public void loadSchool(String domain) {
        enterSchool(domain);
        connectButton().perform(click());
    }

    public void loadDefaultSchool() {
        loadSchool(DOMAIN);
    }
}
