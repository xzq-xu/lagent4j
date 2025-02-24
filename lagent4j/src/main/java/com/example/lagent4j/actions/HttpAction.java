package com.example.lagent4j.actions;

import com.alibaba.fastjson2.JSONObject;
import com.example.lagent4j.agent.ActionResult;
import okhttp3.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 通用HTTP请求工具
 * 支持GET、POST等常见HTTP方法，可配置请求头、超时等参数
 */
public class HttpAction implements Action {
    private final OkHttpClient client;
    private final String name;
    
    public HttpAction(String name, int timeoutSeconds) {
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
                String url = (String) parameters.get("url");
                String method = (String) parameters.getOrDefault("method", "GET");
                @SuppressWarnings("unchecked")
                Map<String, String> headers = (Map<String, String>) parameters.getOrDefault("headers", Map.of());
                String body = (String) parameters.get("body");

                Request.Builder requestBuilder = new Request.Builder()
                    .url(url);

                // 添加请求头
                headers.forEach(requestBuilder::addHeader);

                // 设置请求方法和请求体
                switch (method.toUpperCase()) {
                    case "GET":
                        requestBuilder.get();
                        break;
                    case "POST":
                        requestBuilder.post(RequestBody.create(
                            body,
                            MediaType.parse("application/json")
                        ));
                        break;
                    case "PUT":
                        requestBuilder.put(RequestBody.create(
                            body,
                            MediaType.parse("application/json")
                        ));
                        break;
                    case "DELETE":
                        requestBuilder.delete();
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported HTTP method: " + method);
                }

                // 执行请求
                try (Response response = client.newCall(requestBuilder.build()).execute()) {
                    if (!response.isSuccessful()) {
                        return ActionResult.builder()
                            .status(ActionResult.Status.FAILURE)
                            .error("HTTP " + response.code() + ": " + response.message())
                            .build();
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