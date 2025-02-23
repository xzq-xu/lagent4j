package com.example.lagent4j.llm;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.example.lagent4j.agent.AgentMessage;
import okhttp3.*;


public class OpenAILlmService implements LlmService {
    private String endpoint = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    private final String apiKey;
    private final OkHttpClient client = new OkHttpClient();

    public OpenAILlmService(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public CompletableFuture<String> chatAsync(List<AgentMessage> messages) {
        CompletableFuture<String> future = new CompletableFuture<>();
        
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "qwen-max-latest");
        JSONArray messagesArray = new JSONArray();
        
        for (AgentMessage msg : messages) {
            JSONObject msgObj = new JSONObject();
            msgObj.put("role", msg.getSender());
            msgObj.put("content", msg.getContent());
            messagesArray.add(msgObj);
        }
        
        requestBody.put("messages", messagesArray);

        RequestBody body = RequestBody.create(
            requestBody.toJSONString(), 
            MediaType.get("application/json")
        );

        Request request = new Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer " + apiKey)
            .post(body)
            .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {

                    if (!response.isSuccessful()) {
                        future.completeExceptionally(
                            new IOException("Unexpected code " + response));
                        return;
                    }
                    
                    JSONObject jsonResponse = JSONObject.parseObject(responseBody.string());
                    String content = jsonResponse.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");
                        
                    future.complete(content);
                }
            }
        });

        return future;
    }

    @Override
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}
