package com.cookandroid.app.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    // TODO: 실제 서버 IP로 변경 필요 (local.properties 또는 BuildConfig 사용 권장)
    private static final String BASE_URL = "http://YOUR_SERVER_IP:8000/";
    private static Retrofit retrofit;

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
