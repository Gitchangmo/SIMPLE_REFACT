package com.cookandroid.app.api;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface ApiService {
    @Headers({
            "Authorization: Bearer YOUR_HUGGINGFACE_TOKEN_HERE",
            "Content-Type: application/json"
    })
    @POST("models/facebook/blenderbot-3B")
    Call<Map<String, Object>> query(@Body Map<String, String> body);
}
