package io.github.internlm.lagent4j.prompts;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import io.github.internlm.lagent4j.prompts.parsers.ToolParser;

/**
 * 字符串解析器
 * <p>
 * 用于解析模型响应，支持普通文本和JSON格式
 */
public interface Parser {
    /**
     * 解析响应文本
     *
     * @param response 响应文本
     * @return 解析结果，如果是JSON则返回Map，否则返回null
     */
    Object parseResponse(String response) ;

} 