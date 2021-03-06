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

package com.instructure.canvasapi2.unit

import com.instructure.canvasapi2.models.QuizSubmissionTime
import com.instructure.canvasapi2.utils.parse
import junit.framework.Assert
import org.intellij.lang.annotations.Language
import org.junit.Test

class QuizSubmissionTimeUnitTest : Assert() {

    @Test
    fun testQuizSubmissionTime() {
        val quizSubmissionTime: QuizSubmissionTime = quizSubmissionTimeJSON.parse()

        Assert.assertNotNull(quizSubmissionTime)
        Assert.assertNotNull(quizSubmissionTime.endAt)
    }

    @Language("JSON")
    private var quizSubmissionTimeJSON = """
      {
        "end_at": "2015-05-19T18:30:22Z",
        "time_left": -16
      }"""
}
