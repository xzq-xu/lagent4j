package io.github.internlm.lagent4j.actions;

import com.alibaba.fastjson2.JSON;
import io.github.internlm.lagent4j.schema.ActionReturn;
import io.github.internlm.lagent4j.schema.ActionStatusCode;
import io.github.internlm.lagent4j.schema.FunctionCall;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 网页浏览工具
 * <p>
 * 用于搜索和获取网页内容的工具
 */
@Slf4j
public class WebSearch implements Action {
    private static final String SEARCH_API = "http://www.bing.com/search?";
//    private final String apiKey;

    public WebSearch() {
//        this.apiKey = System.getenv("BING_API_KEY");
//        if (apiKey == null || apiKey.isEmpty()) {
//            throw new IllegalStateException("BING_API_KEY environment variable not set");
//        }
    }

    @Override
    public String getName() {
        return "web_search";
    }

    @Override
    public String getDescription() {
        return "搜索网页内容，获取最新信息";
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("query", "搜索关键词");
        parameters.put("count", "返回结果数量（可选，默认为5）");
        return parameters;
    }

    @Override
    public ActionReturn execute(FunctionCall functionCall) {
        try {
            // 获取参数
            Map<String, Object> params = functionCall.getArguments();
            String query = (String) params.get("query");
            int count = params.containsKey("count") ? ((Number) params.get("count")).intValue() : 5;

            // 构建URL
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            URL url = new URL(SEARCH_API + "?q=" + encodedQuery + "&count=" + count);

            // 发送请求
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
//            conn.setRequestProperty("Ocp-Apim-Subscription-Key", apiKey);

            // 读取响应
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // 解析响应
            Map<String, Object> result = JSON.parseObject(response.toString());
            List<Map<String, Object>> webPages = (List<Map<String, Object>>) result.get("webPages");
            if (webPages == null || webPages.isEmpty()) {
                return ActionReturn.builder()
                        .type(getName())
                        .state(ActionStatusCode.API_ERROR)
                        .errmsg("No results found")
                        .build();
            }

            // 提取搜索结果
            List<Map<String, String>> searchResults = new ArrayList<>();
            for (Map<String, Object> page : webPages) {
                Map<String, String> searchResult = new HashMap<>();
                searchResult.put("title", (String) page.get("name"));
                searchResult.put("snippet", (String) page.get("snippet"));
                searchResult.put("url", (String) page.get("url"));
                searchResults.add(searchResult);
            }

            // 返回结果
            return ActionReturn.builder()
                    .type(getName())
                    .state(ActionStatusCode.SUCCESS)
                    .result(searchResults)
                    .build();

        } catch (Exception e) {
            log.error("Error executing web search: {}", e.getMessage(), e);
            return ActionReturn.builder()
                    .type(getName())
                    .state(ActionStatusCode.API_ERROR)
                    .errmsg("Error executing web search: " + e.getMessage())
                    .build();
        }
    }
} 