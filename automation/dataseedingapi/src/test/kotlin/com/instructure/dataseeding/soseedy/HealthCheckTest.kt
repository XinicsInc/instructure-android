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

package com.instructure.dataseeding.soseedy

import com.instructure.dataseeding.InProcessServer
import com.instructure.soseedy.HealthCheck
import com.instructure.soseedy.HealthCheckRequest
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthCheckTest {
    @Test
    fun getHealthCheck() {
        val request = HealthCheckRequest.getDefaultInstance()
        val check = InProcessServer.generalClient.getHealthCheck(request)
        assertThat(check, instanceOf(HealthCheck::class.java))
        assertTrue(check.healthy)
    }
}
