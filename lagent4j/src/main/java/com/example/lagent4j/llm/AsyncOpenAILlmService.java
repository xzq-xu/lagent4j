package com.example.lagent4j.llm;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONArray;
import com.example.lagent4j.agent.AgentMessage;
import com.example.lagent4j.exceptions.LlmException;
import okhttp3.*;
import okio.BufferedSource;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class AsyncOpenAILlmService implements AsyncLlmService {
    private String endpoint = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    private final String apiKey;
    private final OkHttpClient client;
    private long timeoutMillis = 30000;
    
    public AsyncOpenAILlmService(String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    }

    @Override
    public CompletableFuture<String> chatAsync(List<AgentMessage> messages) {
        CompletableFuture<String> future = new CompletableFuture<>();
        
        JSONObject requestBody = buildRequestBody(messages, false);
        Request request = buildRequest(requestBody);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(new LlmException("API call failed", e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        future.completeExceptionally(
                            new LlmException("Unexpected code " + response));
                        return;
                    }
                    
                    String content = extractContentFromResponse(responseBody.string());
                    future.complete(content);
                }
            }
        });

        return future.orTimeout(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public CompletableFuture<Void> chatStreamAsync(
            List<AgentMessage> messages,
            Consumer<String> onResponse,
            Consumer<Throwable> onError) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        JSONObject requestBody = buildRequestBody(messages, true);
        Request request = buildRequest(requestBody);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                onError.accept(new LlmException("API call failed", e));
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        LlmException ex = new LlmException("Unexpected code " + response);
                        onError.accept(ex);
                        future.completeExceptionally(ex);
                        return;
                    }

                    BufferedSource source = responseBody.source();
                    while (!source.exhausted()) {
                        String line = source.readUtf8Line();
                        if (line == null || line.isEmpty()) continue;
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6);
                            String content = extractStreamContent(data);
                            if (content != null) {
                                onResponse.accept(content);
                            }
                        }
                    }
                    future.complete(null);
                } catch (Exception e) {
                    onError.accept(e);
                    future.completeExceptionally(e);
                }
            }
        });

        return future;
    }

    private JSONObject buildRequestBody(List<AgentMessage> messages, boolean stream) {
        return new JSONObject()
            .fluentPut("model", "qwen-max-latest")
            .fluentPut("messages", convertMessages(messages))
            .fluentPut("stream", stream);
    }

    private Request buildRequest(JSONObject body) {
        return new Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(
                body.toJSONString(),
                MediaType.parse("application/json")
            ))
            .build();
    }

    private List<JSONObject> convertMessages(List<AgentMessage> messages) {
        return messages.stream()
            .map(msg -> {
                JSONObject obj = new JSONObject();
                obj.put("role", convertRole(msg.getSender()));
                obj.put("content", msg.getContent());
                
                if (msg.getType() == AgentMessage.AgentMessageType.TOOL) {
                    obj.put("name", msg.getToolName());
                    obj.put("tool_call_id", msg.getToolCallId());
                    obj.put("tool_args", msg.getToolArgs());
                }
                
                return obj;
            })
            .toList();
    }

    private String convertRole(String sender) {
        return switch (sender.toLowerCase()) {
            case "user" -> "user";
            case "system" -> "system";
            case "tool" -> "tool";
            default -> "assistant";
        };
    }

    private String extractContentFromResponse(String response) {
        JSONObject json = JSONObject.parseObject(response);
        return json.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content");
    }

    private String extractStreamContent(String data) {
        if ("[DONE]".equals(data)) {
            return null;
        }
        
        try {
            JSONObject response = JSONObject.parseObject(data);
            JSONObject choice = response.getJSONArray("choices").getJSONObject(0);
            JSONObject delta = choice.getJSONObject("delta");
            
            if (delta.containsKey("content")) {
                return delta.getString("content");
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public void setTimeout(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }
} 