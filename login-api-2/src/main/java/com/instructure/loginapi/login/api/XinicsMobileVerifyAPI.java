package com.instructure.loginapi.login.api;

import android.content.pm.PackageInfo;
import android.util.Log;

import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.utils.APIHelper;
import com.instructure.loginapi.login.model.DomainVerificationResult;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class XinicsMobileVerifyAPI {
    public interface XinicsMobileVerifyInterface {

        @GET("api/v1/mobileverify")
        Call<DomainVerificationResult> mobileVerify (
                @Query(value = "app_name") String appName,
                @Query(value = "platform") String platform,
                @Query(value = "canvas_url") String domain);
    }

    public static void mobileVerify(String domain, String learningXDomain, StatusCallback<DomainVerificationResult> callback, String appName) {
        if (APIHelper.paramIsNull(callback, learningXDomain)) {
            return;
        }

        try {
            // 가끔 타임 아웃 오류가 발생해, 타임 아웃 시간을 늘려주었다.
            OkHttpClient.Builder client = new OkHttpClient.Builder();
            client.connectTimeout(5, TimeUnit.SECONDS);
            client.readTimeout(5, TimeUnit.SECONDS);
            client.writeTimeout(5, TimeUnit.SECONDS);

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(learningXDomain)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client.build())
                    .build();
            XinicsMobileVerifyAPI.XinicsMobileVerifyInterface service = retrofit.create(XinicsMobileVerifyAPI.XinicsMobileVerifyInterface.class);
            service.mobileVerify(appName, "android", domain).enqueue(callback);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
