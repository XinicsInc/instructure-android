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
package com.instructure.teacher.ui.pages

import com.instructure.teacher.R
import com.instructure.teacher.ui.utils.WaitForViewWithId
import com.instructure.teacher.ui.utils.pageAssert.PageAssert
import com.instructure.teacher.ui.utils.pageAssert.SimplePageAssert

class NotATeacherPage : BasePage(), PageAssert by SimplePageAssert() {

    private val notATeacherTitle by WaitForViewWithId(R.id.not_a_teacher_header, autoAssert = true)
    private val explanation by WaitForViewWithId(R.id.explanation)
    private val studentLink by WaitForViewWithId(R.id.studentLink)
    private val parentLink by WaitForViewWithId(R.id.parentLink)
}