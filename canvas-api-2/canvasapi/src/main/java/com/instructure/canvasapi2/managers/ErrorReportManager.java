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

package com.instructure.canvasapi2.managers;

import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.apis.ErrorReportAPI;
import com.instructure.canvasapi2.models.ErrorReportResult;
import com.instructure.canvasapi2.utils.APIHelper;


public class ErrorReportManager extends BaseManager {

    private static final Boolean mTesting = false;

    public static void postErrorReport(String subject, String url, String email, String comments, String userPerceivedSeverity, String name, String userRoles, String becomeUser, StatusCallback<ErrorReportResult> callback) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            if(APIHelper.paramIsNull(callback, subject, url, email, comments, userPerceivedSeverity, name, userRoles, becomeUser)) return;
            ErrorReportAPI.postErrorReport(subject, url, email, comments, userPerceivedSeverity, name, userRoles, becomeUser, callback);
        }
    }

    public static void postGenericErrorReport(String subject, String url, String email, String comments, String userPerceivedSeverity, String name, String userRoles, String becomeUser, StatusCallback<ErrorReportResult> callback) {
        if (isTesting() || mTesting) {
            // TODO
        } else {
            if(APIHelper.paramIsNull(callback, subject, url, email, comments, userPerceivedSeverity, name, userRoles, becomeUser)) return;
            ErrorReportAPI.postGenericErrorReport(subject, url, email, comments, userPerceivedSeverity, name, userRoles, becomeUser, callback);
        }
    }

}
