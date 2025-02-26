package io.github.internlm.lagent4j.actions;

import io.github.internlm.lagent4j.schema.ActionReturn;
import io.github.internlm.lagent4j.schema.ActionStatusCode;
import io.github.internlm.lagent4j.schema.FunctionCall;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 网页浏览器工具
 * <p>
 * 用于获取网页内容
 */
@Slf4j
public class WebBrowser implements Action {
    /**
     * HTTP客户端
     */
    private final OkHttpClient httpClient;
    
    /**
     * 创建一个网页浏览器工具
     */
    public WebBrowser() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }
    
    @Override
    public String getName() {
        return "web_browser";
    }
    
    @Override
    public String getDescription() {
        return "用于获取网页内容的工具，可以访问指定URL并返回网页内容";
    }
    
    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> parameters = new HashMap<>();
        
        Map<String, Object> urlProperty = new HashMap<>();
        urlProperty.put("type", "string");
        urlProperty.put("description", "要访问的URL");
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("url", urlProperty);
        
        parameters.put("type", "object");
        parameters.put("properties", properties);
        parameters.put("required", List.of("url"));
        
        return parameters;
    }
    
    @Override
    public ActionReturn execute(FunctionCall functionCall) {
        // 获取URL参数
        String url = null;
        if (functionCall.isParametersMap()) {
            Map<String, Object> params = functionCall.getParametersAsMap();
            if (params != null && params.containsKey("url")) {
                url = params.get("url").toString();
            }
        } else {
            // 尝试从字符串参数中解析
            String paramsStr = functionCall.getParametersAsString();
            if (paramsStr != null && !paramsStr.isEmpty()) {
                // 简单解析，假设格式为 "url=http://example.com"
                if (paramsStr.startsWith("url=")) {
                    url = paramsStr.substring(4).trim();
                } else {
                    url = paramsStr.trim();
                }
            }
        }
        
        if (url == null || url.isEmpty()) {
            return ActionReturn.builder()
                    .type(getName())
                    .state(ActionStatusCode.ARGS_ERROR)
                    .errmsg("URL parameter is required")
                    .build();
        }
        
        // 如果URL不以http或https开头，添加http://
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        
        try {
            // 发送HTTP请求
            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "Lagent4j/1.0.0")
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ActionReturn.builder()
                            .type(getName())
                            .state(ActionStatusCode.HTTP_ERROR)
                            .errmsg("HTTP error: " + response.code())
                            .url(url)
                            .build();
                }
                
                String content = response.body() != null ? response.body().string() : "";
                
                // 截取内容，避免过长
                if (content.length() > 8000) {
                    content = content.substring(0, 8000) + "... (content truncated)";
                }
                
                // 构建结果
                List<Map<String, String>> result = new ArrayList<>();
                Map<String, String> item = new HashMap<>();
                item.put("type", "text");
                item.put("content", content);
                result.add(item);
                
                return ActionReturn.builder()
                        .type(getName())
                        .state(ActionStatusCode.SUCCESS)
                        .url(url)
                        .result(result)
                        .build();
            }
        } catch (IOException e) {
            log.error("Error accessing URL: {}", url, e);
            return ActionReturn.builder()
                    .type(getName())
                    .state(ActionStatusCode.HTTP_ERROR)
                    .errmsg("Error accessing URL: " + e.getMessage())
                    .url(url)
                    .build();
        }
    }
} 