package com.example.lagent4j.prompts;

import com.alibaba.fastjson2.JSONObject;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * JSON格式工具调用解析器
 */
public class JsonToolParser implements OutputFormat {
    private final String toolType;
    private final String begin;
    private final String end;
    private final Pattern pattern;
    
    public JsonToolParser(String toolType, String begin, String end) {
        this.toolType = toolType;
        this.begin = begin;
        this.end = end;
        this.pattern = Pattern.compile(
            Pattern.quote(begin) + "(.*?)" + Pattern.quote(end),
            Pattern.DOTALL
        );
    }

    @Override
    public String getToolType() {
        return toolType;
    }

    @Override
    public String getBeginPattern() {
        return begin;
    }

    @Override
    public String getEndPattern() {
        return end;
    }

    @Override
    public Object parseResponse(String response) {
        // 首先尝试解析工具调用格式
        try {
            JSONObject resp = JSONObject.parseObject(response);
            if (resp.containsKey("tool_calls")) {
                JSONObject toolCall = resp.getJSONArray("tool_calls").getJSONObject(0);
                return new JSONObject()
                    .fluentPut("tool_type", toolCall.getString("function_name"))
                    .fluentPut("thought", resp.getString("thought") != null ? 
                        resp.getString("thought") : "Using tool to complete the task")
                    .fluentPut("action", toolCall.getJSONObject("function_call").getString("arguments"))
                    .fluentPut("status", 1);
            }
        } catch (Exception e) {
            // 如果不是工具调用格式，继续使用原有解析逻辑
        }

        // 使用正则表达式提取JSON内容
        Matcher matcher = pattern.matcher(response);
        if (!matcher.find()) {
            return null;
        }
        
        String content = matcher.group(1).trim();
        if (!validate(content)) {
            return null;
        }

        // 提取思考过程
        String thought = response.substring(0, matcher.start()).trim();
        
        // 构建结果对象
        return new JSONObject()
            .fluentPut("tool_type", toolType)
            .fluentPut("thought", thought)
            .fluentPut("action", content)
            .fluentPut("status", 1);
    }

    @Override
    public boolean validate(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }

        try {
            JSONObject.parseObject(content);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
} 