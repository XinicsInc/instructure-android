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

package com.instructure.canvasapi2.apis;

import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.builders.RestBuilder;
import com.instructure.canvasapi2.builders.RestParams;
import com.instructure.canvasapi2.models.AccountDomain;
import com.instructure.canvasapi2.models.XinicsXmlAccountDomain;
import org.simpleframework.xml.Element;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.Url;

public class XinicsAccountDomainAPI {

    private static final String DEFAULT_DOMAIN = "";

    public interface XinicsAccountDomainInterface {

        @GET("learningX/site_list.xml")
        Call<XinicsXmlAccountDomain> getSiteList();

    }

    public static void getAllAccountDomains(final Callback<XinicsXmlAccountDomain> callback) {
        try {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(DEFAULT_DOMAIN)
                    .addConverterFactory(SimpleXmlConverterFactory.create())
                    .build();
            XinicsAccountDomainInterface service = retrofit.create(XinicsAccountDomainInterface.class);

            Call<XinicsXmlAccountDomain> request = service.getSiteList();
            request.enqueue(callback);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}



