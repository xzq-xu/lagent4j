package io.github.internlm.lagent4j.schema;

/**
 * 代理状态码枚举
 * <p>
 * 用于表示代理当前的状态，包括流式输出状态、错误状态和工具调用状态等
 */
public enum AgentStatusCode {
    /**
     * 流式输出结束
     */
    END(0),
    
    /**
     * 正在进行流式输出
     */
    STREAM_ING(1),
    
    /**
     * 服务器错误
     */
    SERVER_ERR(-1),
    
    /**
     * 会话已关闭
     */
    SESSION_CLOSED(-2),
    
    /**
     * 会话超出限制
     */
    SESSION_OUT_OF_LIMIT(-3),
    
    /**
     * 会话参数无效
     */
    SESSION_INVALID_ARG(-4),
    
    /**
     * 会话准备就绪
     */
    SESSION_READY(2),
    
    /**
     * 开始使用工具
     */
    PLUGIN_START(3),
    
    /**
     * 工具使用结束
     */
    PLUGIN_END(4),
    
    /**
     * 工具返回结果
     */
    PLUGIN_RETURN(5),
    
    /**
     * 开始执行代码
     */
    CODING(6),
    
    /**
     * 代码执行结束
     */
    CODE_END(7),
    
    /**
     * 代码执行返回结果
     */
    CODE_RETURN(8);
    
    private final int value;
    
    AgentStatusCode(int value) {
        this.value = value;
    }
    
    /**
     * 获取状态码的整数值
     *
     * @return 状态码的整数值
     */
    public int getValue() {
        return value;
    }
    
    /**
     * 根据整数值获取对应的状态码枚举
     *
     * @param value 整数值
     * @return 对应的状态码枚举，如果没有找到则返回null
     */
    public static AgentStatusCode fromValue(int value) {
        for (AgentStatusCode code : AgentStatusCode.values()) {
            if (code.value == value) {
                return code;
            }
        }
        return null;
    }
} 