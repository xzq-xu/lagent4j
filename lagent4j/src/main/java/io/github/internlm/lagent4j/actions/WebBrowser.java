package io.github.internlm.lagent4j.actions;

import io.github.internlm.lagent4j.schema.ActionReturn;
import io.github.internlm.lagent4j.schema.ActionStatusCode;
import io.github.internlm.lagent4j.schema.FunctionCall;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 网页浏览器工具
 * <p>
 * 用于获取和解析网页内容，支持以下功能：
 * 1. 获取网页原始内容
 * 2. 提取网页正文内容
 * 3. 提取网页标题
 * 4. 提取网页元数据
 */
@Slf4j
public class WebBrowser implements Action {
    /**
     * HTTP客户端
     */
    private final OkHttpClient httpClient;
    
    /**
     * 默认超时时间（秒）
     */
    private static final int DEFAULT_TIMEOUT = 30;
    
    /**
     * 默认最大内容长度
     */
    private static final int MAX_CONTENT_LENGTH = 8000;
    
    /**
     * 创建一个网页浏览器工具
     */
    public WebBrowser() {
        this(DEFAULT_TIMEOUT);
    }
    
    /**
     * 创建一个网页浏览器工具
     *
     * @param timeoutSeconds 超时时间（秒）
     */
    public WebBrowser(int timeoutSeconds) {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .followRedirects(true)
                .build();
    }
    
    @Override
    public String getName() {
        return "web_browser";
    }
    
    @Override
    public String getDescription() {
        return "用于获取和解析网页内容的工具，可以访问指定URL并返回网页内容，支持提取正文、标题等信息";
    }
    
    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> parameters = new HashMap<>();
        
        // URL参数
        Map<String, Object> urlProperty = new HashMap<>();
        urlProperty.put("type", "string");
        urlProperty.put("description", "要访问的URL");
        
        // 提取模式参数
        Map<String, Object> modeProperty = new HashMap<>();
        modeProperty.put("type", "string");
        modeProperty.put("description", "内容提取模式：raw（原始内容）、text（正文内容）、title（标题）、meta（元数据）");
        modeProperty.put("enum", List.of("raw", "text", "title", "meta"));
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("url", urlProperty);
        properties.put("mode", modeProperty);
        
        parameters.put("type", "object");
        parameters.put("properties", properties);
        parameters.put("required", List.of("url"));
        
        return parameters;
    }
    
    @Override
    public ActionReturn execute(FunctionCall functionCall) {
        // 获取参数
        String url = null;
        String mode = "text"; // 默认提取正文
        
        if (functionCall.isParametersMap()) {
            Map<String, Object> params = functionCall.getParametersAsMap();
            if (params != null) {
                if (params.containsKey("url")) {
                    url = params.get("url").toString();
                }
                if (params.containsKey("mode")) {
                    mode = params.get("mode").toString();
                }
            }
        } else {
            // 尝试从字符串参数中解析
            String paramsStr = functionCall.getParametersAsString();
            if (paramsStr != null && !paramsStr.isEmpty()) {
                if (paramsStr.startsWith("url=")) {
                    url = paramsStr.substring(4).trim();
                } else {
                    url = paramsStr.trim();
                }
            }
        }
        
        // 验证URL参数
        if (url == null || url.isEmpty()) {
            return ActionReturn.builder()
                    .type(getName())
                    .state(ActionStatusCode.ARGS_ERROR)
                    .errmsg("URL参数不能为空")
                    .build();
        }
        
        // 规范化URL
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        
        try {
            // 发送HTTP请求
            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (compatible; Lagent4j/1.0.0)")
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ActionReturn.builder()
                            .type(getName())
                            .state(ActionStatusCode.HTTP_ERROR)
                            .errmsg("HTTP错误: " + response.code())
                            .url(url)
                            .build();
                }
                
                String content = response.body() != null ? response.body().string() : "";
                
                // 使用Jsoup解析HTML
                Document doc = Jsoup.parse(content);
                
                // 根据模式提取内容
                String extractedContent;
                switch (mode) {
                    case "raw":
                        extractedContent = content;
                        break;
                    case "text":
                        extractedContent = doc.body().text();
                        break;
                    case "title":
                        extractedContent = doc.title();
                        break;
                    case "meta":
                        StringBuilder meta = new StringBuilder();
                        doc.select("meta").forEach(element -> {
                            String name = element.attr("name");
                            String property = element.attr("property");
                            String content1 = element.attr("content");
                            if (!name.isEmpty() && !content1.isEmpty()) {
                                meta.append(name).append(": ").append(content1).append("\n");
                            } else if (!property.isEmpty() && !content1.isEmpty()) {
                                meta.append(property).append(": ").append(content1).append("\n");
                            }
                        });
                        extractedContent = meta.toString();
                        break;
                    default:
                        extractedContent = doc.body().text();
                }
                
                // 截取内容，避免过长
                if (extractedContent.length() > MAX_CONTENT_LENGTH) {
                    extractedContent = extractedContent.substring(0, MAX_CONTENT_LENGTH) + "... (内容已截断)";
                }
                
                // 构建结果
                List<Map<String, String>> result = new ArrayList<>();
                Map<String, String> item = new HashMap<>();
                item.put("type", mode);
                item.put("content", extractedContent);
                result.add(item);
                
                return ActionReturn.builder()
                        .type(getName())
                        .state(ActionStatusCode.SUCCESS)
                        .url(url)
                        .result(result)
                        .build();
            }
        } catch (IOException e) {
            log.error("访问URL时发生错误: {}", url, e);
            return ActionReturn.builder()
                    .type(getName())
                    .state(ActionStatusCode.HTTP_ERROR)
                    .errmsg("访问URL时发生错误: " + e.getMessage())
                    .url(url)
                    .build();
        }
    }
} 