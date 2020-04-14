/*
 * This file is part of BOINC.
 * http://boinc.berkeley.edu
 * Copyright (C) 2020 University of California
 *
 * BOINC is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * BOINC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with BOINC.  If not, see <http://www.gnu.org/licenses/>.
 */
package edu.berkeley.boinc

import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.core.IsInstanceOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class SplashActivityLongPressTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(SplashActivity::class.java)

    @Test
    fun splashActivityLongPressTest() {
        val imageView = onView(
                allOf(withId(R.id.logo), withContentDescription("Starting…"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                0),
                        isDisplayed()))
        imageView.perform(longClick())

        val textView = onView(
                allOf(withId(R.id.refresh), withContentDescription("Refresh"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.action_bar),
                                        2),
                                0),
                        isDisplayed()))
        textView.check(matches(isDisplayed()))

        val textView2 = onView(
                allOf(withId(R.id.email_to), withContentDescription("Send as Email"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.action_bar),
                                        2),
                                1),
                        isDisplayed()))
        textView2.check(matches(isDisplayed()))

        val textView3 = onView(
                allOf(withId(R.id.copy), withContentDescription("Copy to Clipboard"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.action_bar),
                                        2),
                                2),
                        isDisplayed()))
        textView3.check(matches(isDisplayed()))

        val textView4 = onView(
                allOf(withText("CLIENT MESSAGES"),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.instanceOf(androidx.appcompat.widget.LinearLayoutCompat::class.java),
                                        0),
                                0),
                        isDisplayed()))
        textView4.check(matches(isDisplayed()))

        val textView5 = onView(
                allOf(withText("GUI MESSAGES"),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.instanceOf(androidx.appcompat.widget.LinearLayoutCompat::class.java),
                                        1),
                                0),
                        isDisplayed()))
        textView5.check(matches(isDisplayed()))
    }

    private fun childAtPosition(
            parentMatcher: Matcher<View>, position: Int): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}
