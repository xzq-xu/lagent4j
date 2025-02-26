package io.github.internlm.lagent4j.schema;

/**
 * 动作状态码枚举
 * <p>
 * 用于表示动作执行的状态，包括成功、进行中和各种错误状态
 */
public enum ActionStatusCode {
    /**
     * 动作执行中
     */
    ING(1),
    
    /**
     * 动作执行成功
     */
    SUCCESS(0),
    
    /**
     * HTTP错误
     */
    HTTP_ERROR(-1000),
    
    /**
     * 参数错误
     */
    ARGS_ERROR(-1001),
    
    /**
     * API错误
     */
    API_ERROR(-1002);
    
    private final int value;
    
    ActionStatusCode(int value) {
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
    public static ActionStatusCode fromValue(int value) {
        for (ActionStatusCode code : ActionStatusCode.values()) {
            if (code.value == value) {
                return code;
            }
        }
        return null;
    }
} 