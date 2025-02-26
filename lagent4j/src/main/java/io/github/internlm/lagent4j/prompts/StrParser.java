package io.github.internlm.lagent4j.prompts;

/**
 * 字符串解析器接口
 * <p>
 * 用于解析模型输出的字符串，将其转换为特定格式
 */
public interface StrParser {
    /**
     * 解析响应
     *
     * @param response 模型响应文本
     * @return 解析后的对象
     */
    Object parseResponse(String response);
} 