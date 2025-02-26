package io.github.internlm.lagent4j.llms;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.BufferedSource;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * OpenAI语言模型实现
 */
@Slf4j
public class OpenAIModel implements BaseLLM {
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient client;
    private final String baseUrl;
    private final String apiKey;
    private final Map<String, Object> defaultParams;

    /**
     * 私有构造函数，通过Builder创建实例
     */
    private OpenAIModel(Builder builder) {
        // 创建HTTP客户端
        this.client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(builder.timeoutSeconds))
                .callTimeout(Duration.ofSeconds(builder.timeoutSeconds))
                .readTimeout(Duration.ofSeconds(builder.timeoutSeconds))
                .writeTimeout(Duration.ofSeconds(builder.timeoutSeconds))
                .build();

        // 设置基础URL和API密钥
        this.baseUrl = builder.baseUrl != null && !builder.baseUrl.isEmpty() 
                ? builder.baseUrl.replaceAll("/$", "") 
                : "https://api.openai.com/v1";
        this.apiKey = builder.apiKey;

        // 设置默认参数
        this.defaultParams = new HashMap<>();
        this.defaultParams.put("model", builder.model);
        this.defaultParams.put("temperature", builder.temperature);
        this.defaultParams.put("max_tokens", builder.maxTokens);
        this.defaultParams.put("top_p", builder.topP);
        this.defaultParams.put("frequency_penalty", builder.frequencyPenalty);
        this.defaultParams.put("presence_penalty", builder.presencePenalty);
    }

    @Override
    public String chat(List<Map<String, String>> messages) {
        return chat(messages, defaultParams);
    }

    @Override
    public String chat(List<Map<String, String>> messages, Map<String, Object> params) {
        try {
            // 构建请求体
            Map<String, Object> requestBody = buildRequestBody(messages, params);
            String jsonBody = JSON.toJSONString(requestBody);

            // 构建请求
            Request request = new Request.Builder()
                    .url(baseUrl + "/chat/completions")
                    .post(RequestBody.create(jsonBody, JSON_MEDIA_TYPE))
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .build();

            // 发送请求
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    throw new IOException("Unexpected response: " + response);
                }

                // 解析响应
                String responseBody = response.body().string();
                JSONObject jsonResponse = JSON.parseObject(responseBody);
                return jsonResponse.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");
            }
        } catch (Exception e) {
            log.error("Error in chat request: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get chat response", e);
        }
    }

    @Override
    public CompletableFuture<String> chatAsync(List<Map<String, String>> messages) {
        return chatAsync(messages, defaultParams);
    }

    @Override
    public CompletableFuture<String> chatAsync(List<Map<String, String>> messages, Map<String, Object> params) {
        return CompletableFuture.supplyAsync(() -> chat(messages, params));
    }

    @Override
    public void chatStream(List<Map<String, String>> messages,
                         Consumer<String> onChunk,
                         Consumer<Throwable> onError,
                         Runnable onComplete) {
        chatStream(messages, defaultParams, onChunk, onError, onComplete);
    }

    @Override
    public void chatStream(List<Map<String, String>> messages,
                         Map<String, Object> params,
                         Consumer<String> onChunk,
                         Consumer<Throwable> onError,
                         Runnable onComplete) {
        try {
            // 构建请求体
            Map<String, Object> requestBody = buildRequestBody(messages, params);
            requestBody.put("stream", true);
            String jsonBody = JSON.toJSONString(requestBody);

            // 构建请求
            Request request = new Request.Builder()
                    .url(baseUrl + "/chat/completions")
                    .post(RequestBody.create(jsonBody, JSON_MEDIA_TYPE))
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .build();

            // 发送请求
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    onError.accept(e);
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try {
                        if (!response.isSuccessful() || response.body() == null) {
                            throw new IOException("Unexpected response: " + response);
                        }

                        // 读取SSE流
                        try (ResponseBody responseBody = response.body()) {
                            BufferedSource source = responseBody.source();
                            while (!source.exhausted()) {
                                String line = source.readUtf8Line();
                                if (line == null || line.isEmpty()) continue;
                                if (line.startsWith("data: ")) {
                                    String data = line.substring(6);
                                    if (data.equals("[DONE]")) {
                                        onComplete.run();
                                        break;
                                    }

                                    // 解析数据
                                    JSONObject jsonChunk = JSON.parseObject(data);
                                    String content = jsonChunk.getJSONArray("choices")
                                            .getJSONObject(0)
                                            .getJSONObject("delta")
                                            .getString("content");

                                    if (content != null) {
                                        onChunk.accept(content);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        onError.accept(e);
                    }
                }
            });
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    private Map<String, Object> buildRequestBody(List<Map<String, String>> messages, Map<String, Object> params) {
        Map<String, Object> mergedParams = new HashMap<>(defaultParams);
        if (params != null) {
            mergedParams.putAll(params);
        }

        Map<String, Object> requestBody = new HashMap<>(mergedParams);
        requestBody.put("messages", messages);
        return requestBody;
    }

    /**
     * OpenAIModel的构建器
     */
    public static class Builder {
        private String apiKey;
        private String baseUrl;
        private String model = "gpt-3.5-turbo";
        private double temperature = 0.7;
        private int maxTokens = 2048;
        private double topP = 1.0;
        private double frequencyPenalty = 0.0;
        private double presencePenalty = 0.0;
        private int timeoutSeconds = 120;

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder topP(double topP) {
            this.topP = topP;
            return this;
        }

        public Builder frequencyPenalty(double frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
            return this;
        }

        public Builder presencePenalty(double presencePenalty) {
            this.presencePenalty = presencePenalty;
            return this;
        }

        public Builder timeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        public OpenAIModel build() {
            if (apiKey == null || apiKey.isEmpty()) {
                throw new IllegalStateException("API key must be provided");
            }
            return new OpenAIModel(this);
        }
    }
} 