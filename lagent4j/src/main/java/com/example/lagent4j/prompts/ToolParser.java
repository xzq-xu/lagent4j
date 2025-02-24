package com.example.lagent4j.prompts;

import com.alibaba.fastjson2.JSONObject;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 工具输出解析器
 * 用于解析LLM响应中的工具调用指令
 */
public class ToolParser implements OutputFormat {
    private final String toolType;
    private final String begin;
    private final String end;
    private final Pattern pattern;
    
    public ToolParser(String toolType, String begin, String end) {
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
        Matcher matcher = pattern.matcher(response);
        if (!matcher.find()) {
            return null;
        }
        
        String content = matcher.group(1).trim();
        if (!validate(content)) {
            return null;
        }

        // 提取思考过程和动作
        String thought = response.substring(0, matcher.start()).trim();
        
        return new JSONObject()
            .fluentPut("tool_type", toolType)
            .fluentPut("thought", thought)
            .fluentPut("action", content)
            .fluentPut("status", 1);
    }

    @Override
    public boolean validate(String content) {
        return content != null && !content.isEmpty();
    }
} 