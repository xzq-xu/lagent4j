package io.github.internlm.lagent4j.llms;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.BufferedSource;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * OpenAI模型实现
 * <p>
 * 通过OpenAI API实现大型语言模型功能
 */
@Slf4j
public class OpenAIModel extends AbstractLLM {
    /**
     * OpenAI API密钥
     */
    private final String apiKey;
    
    /**
     * API基础URL
     */
    private final String baseUrl;
    
    /**
     * 模型名称
     */
    private final String model;
    
    /**
     * HTTP客户端
     */
    private final OkHttpClient httpClient;
    
    /**
     * 创建一个OpenAI模型实例
     *
     * @param apiKey OpenAI API密钥
     */
    public OpenAIModel(String apiKey) {
        this(apiKey, "https://api.openai.com/v1", "gpt-3.5-turbo");
    }
    
    /**
     * 创建一个OpenAI模型实例
     *
     * @param apiKey  OpenAI API密钥
     * @param model   模型名称
     */
    public OpenAIModel(String apiKey, String model) {
        this(apiKey, "https://api.openai.com/v1", model);
    }
    
    /**
     * 创建一个OpenAI模型实例
     *
     * @param apiKey  OpenAI API密钥
     * @param baseUrl API基础URL
     * @param model   模型名称
     */
    public OpenAIModel(String apiKey, String baseUrl, String model) {
        super(createDefaultParams());
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }
    
    /**
     * 创建默认参数
     *
     * @return 默认参数Map
     */
    private static Map<String, Object> createDefaultParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("temperature", 0.7);
        params.put("max_tokens", 1024);
        params.put("top_p", 1.0);
        params.put("frequency_penalty", 0.0);
        params.put("presence_penalty", 0.0);
        return params;
    }
    
    @Override
    public String chat(List<Map<String, String>> messages, Map<String, Object> params) {
        Map<String, Object> mergedParams = mergeParams(params);
        
        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>(mergedParams);
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        
        // 创建HTTP请求
        Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(
                        JSON.toJSONString(requestBody),
                        MediaType.parse("application/json")))
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                log.error("OpenAI API error: {}", errorBody);
                throw new RuntimeException("OpenAI API error: " + errorBody);
            }
            
            String responseBody = response.body().string();
            JSONObject jsonResponse = JSON.parseObject(responseBody);
            
            return jsonResponse.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
        } catch (IOException e) {
            log.error("Error calling OpenAI API", e);
            throw new RuntimeException("Error calling OpenAI API", e);
        }
    }
    
    @Override
    public void chatStream(List<Map<String, String>> messages, 
                          Map<String, Object> params,
                          Consumer<String> chunkConsumer, 
                          Consumer<Throwable> errorConsumer, 
                          Runnable doneConsumer) {
        Map<String, Object> mergedParams = mergeParams(params);
        
        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>(mergedParams);
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("stream", true);
        
        // 创建HTTP请求
        Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(
                        JSON.toJSONString(requestBody),
                        MediaType.parse("application/json")))
                .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                errorConsumer.accept(e);
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    errorConsumer.accept(new RuntimeException("OpenAI API error: " + errorBody));
                    return;
                }
                
                try (ResponseBody responseBody = response.body()) {
                    if (responseBody == null) {
                        errorConsumer.accept(new RuntimeException("Empty response body"));
                        return;
                    }
                    
                    BufferedSource source = responseBody.source();
                    while (!source.exhausted()) {
                        String line = source.readUtf8Line();
                        if (line == null || line.isEmpty()) {
                            continue;
                        }
                        
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6);
                            if ("[DONE]".equals(data)) {
                                break;
                            }
                            
                            try {
                                JSONObject jsonData = JSON.parseObject(data);
                                JSONObject choice = jsonData.getJSONArray("choices").getJSONObject(0);
                                JSONObject delta = choice.getJSONObject("delta");
                                
                                if (delta.containsKey("content")) {
                                    String content = delta.getString("content");
                                    chunkConsumer.accept(content);
                                }
                            } catch (Exception e) {
                                log.warn("Error parsing stream data: {}", data, e);
                            }
                        }
                    }
                    
                    doneConsumer.run();
                } catch (Exception e) {
                    errorConsumer.accept(e);
                }
            }
        });
    }
} 