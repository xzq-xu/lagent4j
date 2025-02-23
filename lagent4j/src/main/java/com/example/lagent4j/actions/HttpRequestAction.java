package com.example.lagent4j.actions;

import com.alibaba.fastjson2.JSONObject;
import com.example.lagent4j.agent.ActionResult;
import okhttp3.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class HttpRequestAction implements Action {
    private final OkHttpClient client = new OkHttpClient();


    @Override
    public String getName() {
        return "httpRequestAction";
    }

    @Override
    public CompletableFuture<ActionResult> executeAsync(Map<String, Object> parameters) {


        return CompletableFuture.supplyAsync(() -> {
            JSONObject params = JSONObject.from(parameters);
            String url = params.getString("url");
            String method = params.getString("method");
            JSONObject headers = params.getJSONObject("headers");
            String body = params.getString("body");

            Request.Builder requestBuilder = new Request.Builder().url(url);

            if (headers != null) {
                for (String key : headers.keySet()) {
                    requestBuilder.addHeader(key, headers.getString(key));
                }
            }

            if ("POST".equalsIgnoreCase(method) && body != null) {
                requestBuilder.post(RequestBody.create(body, MediaType.get("application/json")));
            }

            try (Response response = client.newCall(requestBuilder.build()).execute()) {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                return ActionResult.builder().status(ActionResult.Status.SUCCESS)
                        .output(response.body().string()).build();
            }catch (Exception ex){
                return  ActionResult.builder().status(ActionResult.Status.FAILURE)
                        .error(ex.getMessage()).build();
            }
        }).exceptionally(ex -> {
            return ActionResult.builder().status(ActionResult.Status.FAILURE).error(ex.getMessage()).build();
        });
    }
}
