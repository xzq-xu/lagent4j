package com.example.lagent4j.prompts;

/**
 * 输出格式化接口
 * 用于解析LLM响应中的工具调用指令
 */
public interface OutputFormat {
    /**
     * 获取工具类型
     */
    String getToolType();

    /**
     * 获取开始标记
     */
    String getBeginPattern();

    /**
     * 获取结束标记
     */
    String getEndPattern();

    /**
     * 解析响应内容
     * @param response LLM的原始响应
     * @return 解析后的结构化数据
     */
    Object parseResponse(String response);

    /**
     * 验证内容格式是否正确
     */
    boolean validate(String content);
} 