package com.example.lagent4j.actions;

import com.example.lagent4j.agent.ActionResult;
import com.example.lagent4j.constants.ToolConstants;
import com.example.lagent4j.exceptions.ToolException;
import okhttp3.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class HttpRequestAction implements Action {
    private final String name;
    private final OkHttpClient client;
    
    public HttpRequestAction(String name, int timeoutSeconds) {
        this.name = name;
        this.client = new OkHttpClient.Builder()
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .build();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public CompletableFuture<ActionResult> executeAsync(Map<String, Object> parameters) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = (String) parameters.get(ToolConstants.URL_KEY);
                String method = (String) parameters.getOrDefault(ToolConstants.METHOD_KEY, "GET");
                
                Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .method(method, method.equals("GET") ? null : RequestBody.create(new byte[0], null));
                
                try (Response response = client.newCall(requestBuilder.build()).execute()) {
                    if (!response.isSuccessful()) {
                        throw new ToolException("HTTP request failed: " + response.code());
                    }
                    
                    String responseBody = response.body().string();
                    return ActionResult.builder()
                        .status(ActionResult.Status.SUCCESS)
                        .output(responseBody)
                        .build();
                }
            } catch (Exception e) {
                return ActionResult.builder()
                    .status(ActionResult.Status.FAILURE)
                    .error(e.getMessage())
                    .build();
            }
        });
    }
}
