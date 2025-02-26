package io.github.internlm.lagent4j.schema;

/**
 * 模型状态码
 * <p>
 * 用于表示模型响应的状态
 */
public enum ModelStatusCode {
    /**
     * 开始生成
     */
    START,
    
    /**
     * 生成中
     */
    GENERATING,
    
    /**
     * 生成结束
     */
    END,
    
    /**
     * 发生错误
     */
    ERROR
} 