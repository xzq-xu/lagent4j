package io.github.internlm.lagent4j.actions;

import io.github.internlm.lagent4j.schema.ActionReturn;
import io.github.internlm.lagent4j.schema.FunctionCall;

import java.util.Map;

/**
 * 动作接口
 * <p>
 * 定义了工具的基本行为，包括名称、描述、参数和执行方法
 */
public interface Action {
    /**
     * 获取动作名称
     *
     * @return 动作名称
     */
    String getName();
    
    /**
     * 获取动作描述
     *
     * @return 动作描述
     */
    String getDescription();
    
    /**
     * 获取动作参数描述
     *
     * @return 参数描述，通常是JSON Schema格式
     */
    Map<String, Object> getParameters();
    
    /**
     * 执行动作
     *
     * @param functionCall 函数调用信息
     * @return 动作执行结果
     */
    ActionReturn execute(FunctionCall functionCall);
} 