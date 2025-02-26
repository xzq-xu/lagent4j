package io.github.internlm.lagent4j.schema;

/**
 * 动作有效性状态码枚举
 * <p>
 * 用于表示动作的有效性状态，包括完成、开放、关闭、无效和缺失等状态
 */
public enum ActionValidCode {
    /**
     * 动作已完成
     */
    FINISH(1),
    
    /**
     * 动作开放可用
     */
    OPEN(0),
    
    /**
     * 动作已关闭
     */
    CLOSED(-1),
    
    /**
     * 动作无效
     */
    INVALID(-2),
    
    /**
     * 动作缺失（无动作）
     */
    ABSENT(-3);
    
    private final int value;
    
    ActionValidCode(int value) {
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
    public static ActionValidCode fromValue(int value) {
        for (ActionValidCode code : ActionValidCode.values()) {
            if (code.value == value) {
                return code;
            }
        }
        return null;
    }
} 