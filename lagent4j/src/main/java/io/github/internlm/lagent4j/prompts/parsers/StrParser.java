package io.github.internlm.lagent4j.prompts.parsers;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import io.github.internlm.lagent4j.prompts.Parser;

/**
 * 字符串解析器
 * <p>
 * 用于解析模型响应，支持普通文本和JSON格式
 */
public class StrParser implements Parser {
    /**
     * 解析响应文本
     *
     * @param response 响应文本
     * @return 解析结果，如果是JSON则返回Map，否则返回null
     */
    public Object parseResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return null;
        }

        try {
            // 尝试解析为JSON
            if (response.trim().startsWith("{")) {
                JSONObject json = JSON.parseObject(response);
                if (json.containsKey("function_call") || json.containsKey("action")) {
                    return json;
                }
            }
        } catch (Exception ignored) {
            // 解析失败，说明不是JSON格式
        }

        return null;
    }
} 