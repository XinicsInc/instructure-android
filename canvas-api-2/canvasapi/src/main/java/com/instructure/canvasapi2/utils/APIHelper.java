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

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.instructure.canvasapi2.builders.RestParams;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Response;

public class APIHelper {

    private final static String SHARED_PREFERENCES_AIRWOLF_DOMAIN = "airwolf_domain";

    public static boolean hasNetworkConnection() {
        ConnectivityManager cm = (ConnectivityManager) ContextKeeper.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    /**
     * parseLinkHeaderResponse parses HTTP headers to return the first, next, prev, and last urls. Used for pagination.
     *
     * @param headers List of headers
     * @return A LinkHeaders object
     */
    public static
    @NonNull
    LinkHeaders parseLinkHeaderResponse(Headers headers) {
        LinkHeaders linkHeaders = new LinkHeaders();

        Map<String, List<String>> map = headers.toMultimap();

        for (String name : map.keySet()) {
            if ("link".equalsIgnoreCase(name)) {
                for (String value : map.get(name)) {
                    String[] split = value.split(",");
                    for (String segment : split) {
                        int index = segment.indexOf(">");
                        String url = segment.substring(0, index);
                        url = url.substring(1);

                        //Remove the domain.
                        try {
                            url = URLDecoder.decode(removeDomainFromUrl(url), "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            url = removeDomainFromUrl(url);
                        }

                        if (segment.contains("rel=\"next\"")) {
                            linkHeaders.nextUrl = url;
                        } else if (segment.contains("rel=\"prev\"")) {
                            linkHeaders.prevUrl = url;
                        } else if (segment.contains("rel=\"first\"")) {
                            linkHeaders.firstUrl = url;
                        } else if (segment.contains("rel=\"last\"")) {
                            linkHeaders.lastUrl = url;
                        }
                    }
                }
                break;
            }
        }

        return linkHeaders;
    }

    @NonNull
    static LinkHeaders parseLinkHeaderResponse(String linkField) {
        LinkHeaders linkHeaders = new LinkHeaders();
        if (TextUtils.isEmpty(linkField)) {
            return linkHeaders;
        }

        String[] split = linkField.split(",");
        for (String segment : split) {
            int index = segment.indexOf(">");
            String url = segment.substring(0, index);
            url = url.substring(1);

            //Remove the domain.
            url = removeDomainFromUrl(url);

            if (segment.contains("rel=\"next\"")) {
                linkHeaders.nextUrl = url;
            } else if (segment.contains("rel=\"prev\"")) {
                linkHeaders.prevUrl = url;
            } else if (segment.contains("rel=\"first\"")) {
                linkHeaders.firstUrl = url;
            } else if (segment.contains("rel=\"last\"")) {
                linkHeaders.lastUrl = url;
            }
        }
        return linkHeaders;
    }

    /**
     * removeDomainFromUrl is a helper function for removing the domain from a url. Used for pagination/routing
     *
     * @param url A url
     * @return a String without a domain
     */
    public static String removeDomainFromUrl(String url) {
        if (url == null) {
            return null;
        }

        String prefix = "/api/v1/";
        int index = url.indexOf(prefix);
        if (index != -1) {
            url = url.substring(index + prefix.length());
        }
        return url;
    }

    public static boolean isCachedResponse(@NonNull okhttp3.Response response) {
        return response.cacheResponse() != null;
    }

    public static boolean isCachedResponse(@NonNull Response response) {
        return isCachedResponse(response.raw());
    }

    public static boolean paramIsNull(Object... args) {
        if(args == null) return true;
        for (Object arg : args) {
            if (arg == null) {
                return true;
            }
        }
        return false;
    }

    public static Date stringToDate(final String iso8601string) {
        try {
            String s = iso8601string.replace("Z", "+00:00");
            s = s.substring(0, 22) + s.substring(23);
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    public static String dateToString(final Date date) {
        if (date == null) {
            return null;
        }

        String formatted = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).format(date);
        return formatted.substring(0, 22) + ":" + formatted.substring(22);
    }

    public static String dateToString(final GregorianCalendar date) {
        if (date == null) {
            return null;
        }

        String formatted = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).format(new Date(date.getTimeInMillis()));
        return formatted.substring(0, 22) + ":" + formatted.substring(22);
    }

    /**
     * booleanToInt is a Helper function for Converting boolean to URL booleans (ints)
     */
    public static int booleanToInt(boolean bool) {
        if (bool) {
            return 1;
        }
        return 0;
    }

    /*
     * The fromHTML method can cause a character that looks like [obj]
     * to show up. This is undesired behavior most of the time.
     *
     * Replace the [obj] with an empty space
     * [obj] is char 65532 and an empty space is char 32
     * @param sequence The fromHTML typically
     * @return The modified charSequence
     */
    public static String simplifyHTML(CharSequence sequence) {
        if(sequence != null) {
            CharSequence toReplace = sequence;
            toReplace = toReplace.toString().replace(((char) 65532), (char) 32).trim();
            return toReplace.toString();
        }
        return "";
    }

    public static Map<String,String> getAuthenticatedURL() {
        String token = ApiPrefs.getToken();
        String headerValue = String.format("Bearer %s", token);
        Map<String,String> map = new HashMap<>();
        map.put("Authorization", headerValue);
        return map;
    }

    public static Map<String, String> getReferrer(){
        Map<String, String> extraHeaders = new HashMap<>();
        //Spelled as it should, misspelled...
        extraHeaders.put("Referer", ApiPrefs.getDomain());
        return extraHeaders;
    }

    public static RestParams paramsWithDomain(String domain, RestParams params) {
        RestParams.Builder builder = new RestParams.Builder(params).withDomain(domain);
        return builder.build();
    }

    public static RequestBody makeRequestBody(String part) {
        if (part == null) {
            return RequestBody.create(MediaType.parse("multipart/form-data"), new byte[0]);
        } else {
            return RequestBody.create(MediaType.parse("multipart/form-data"), part);
        }
    }

}
