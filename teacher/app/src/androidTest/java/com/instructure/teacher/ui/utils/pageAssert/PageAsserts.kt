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
package com.instructure.teacher.ui.utils.pageAssert

import android.support.annotation.IdRes
import android.support.test.espresso.Espresso
import android.support.test.espresso.ViewInteraction
import android.support.test.espresso.matcher.ViewMatchers
import com.instructure.teacher.ui.utils.assertDisplayed
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface PageAssert {
    fun assertPageObjects()
    fun registerPropertyInfo(info: Pair<ReadOnlyProperty<Any, ViewInteraction>, KProperty<*>>)
}

interface ComposedPageAssert : PageAssert, PropertyRegistry, PropertyAssert {
    override fun assertPageObjects() = assertProperties(getRegisteredProperties())
}

class SimplePageAssert :
        ComposedPageAssert,
        PropertyRegistry by SimplePropertyRegistry(),
        PropertyAssert by SimplePropertyAssert()

class SkipPageAssert :
        ComposedPageAssert,
        PropertyRegistry by SkipPropertyRegistry(),
        PropertyAssert by SkipPropertyAssert()

class PageWithIdAssert(@IdRes val pageResId: Int) :
        ComposedPageAssert,
        PropertyRegistry by SimplePropertyRegistry(),
        PropertyAssert by SimplePropertyAssert() {
    override fun assertPageObjects() {
        Espresso.onView(ViewMatchers.withId(pageResId)).assertDisplayed()
        super.assertPageObjects()
    }
}

class SingleIdAssert(@IdRes val pageResId: Int) :
        ComposedPageAssert,
        PropertyRegistry by SkipPropertyRegistry(),
        PropertyAssert by SkipPropertyAssert() {
    override fun assertPageObjects() {
        Espresso.onView(ViewMatchers.withId(pageResId)).assertDisplayed()
    }
}