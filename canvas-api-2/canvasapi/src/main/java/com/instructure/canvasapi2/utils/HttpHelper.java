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
package com.instructure.canvasapi2.utils;

import android.annotation.TargetApi;
import android.content.Context;

import com.instructure.canvasapi2.models.ApiHttpResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpHelper {

    /**
     * externalHttpGet is a way to make  HTTPRequests to APIs other than the CanvasAPI.
     * The ENTIRE url must be specified including domain.
     *
     * @param context
     * @param getURL
     * @param includeAuthentication whether or not the should be authenticated using the CanvasToken saved.
     * @return
     */
    public static ApiHttpResponse externalHttpGet(Context context, String getURL, boolean includeAuthentication) {
        //Explicit check for null.
        if (context == null) {
            return new ApiHttpResponse();
        }

        try {
            getURL = MasqueradeHelper.addMasqueradeId(getURL);
            //Remove spaces from the URL
            getURL = getURL.replace(" ", "%20");

            String api_protocol = ApiPrefs.getProtocol();
            //Make sure the URL begins with http(s)://
            if (!getURL.startsWith("https://") && !getURL.startsWith("http://")) {
                getURL = api_protocol + "://" + getURL;
            }

            final HttpURLConnection urlConnection = (HttpURLConnection) new URL(getURL).openConnection();
            urlConnection.setRequestMethod("GET");

            if (includeAuthentication) {
                String token = ApiPrefs.getToken();
                if (token.length() > 0) {
                    String headerValue = String.format("Bearer %s", token);
                    urlConnection.setRequestProperty("Authorization", headerValue);
                }
            }

            return parseLinkHeaderResponse(urlConnection);
        } catch (Exception e) {
            Logger.e("Error externalHttpGet: " + e.getMessage());
            return new ApiHttpResponse();
        }
    }

    /**
     * redirectURL tries its best to follow http redirects until there are no more.
     *
     * @param urlConnection
     * @return
     */
    @TargetApi(9)
    public static HttpURLConnection redirectURL(HttpURLConnection urlConnection) {
        HttpURLConnection.setFollowRedirects(true);
        try {
            urlConnection.connect();

            String currentURL = urlConnection.getURL().toString();
            do {
                urlConnection.getResponseCode();
                currentURL = urlConnection.getURL().toString();
                urlConnection = (HttpURLConnection) new URL(currentURL).openConnection();
            }
            while (!urlConnection.getURL().toString().equals(currentURL));
        } catch (Exception E) {
        }
        return urlConnection;

    }

    private static ApiHttpResponse parseLinkHeaderResponse(HttpURLConnection urlConnection) {
        ApiHttpResponse httpResponse = new ApiHttpResponse();
        InputStream inputStream = null;
        try {
            httpResponse.responseCode = urlConnection.getResponseCode();

            // Check if response is supposed to have a body
            if (httpResponse.responseCode != 204) {
                inputStream = urlConnection.getInputStream();
                InputStreamReader isReader = new InputStreamReader(inputStream);
                BufferedReader br = new BufferedReader(isReader);
                StringBuilder sb = new StringBuilder();
                String inputLine = "";
                while ((inputLine = br.readLine()) != null) {
                    sb.append(inputLine);
                }
                httpResponse.responseBody = sb.toString();
            }

            httpResponse.linkHeaders = APIHelper.parseLinkHeaderResponse(urlConnection.getHeaderField("link"));
        } catch (Exception e) {
            Logger.e("Failed to get response: " + e.getMessage());
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Logger.e("Could not close input stream: " + e.getMessage());
                }
            }
        }

        return httpResponse;
    }

}
