package com.example.lagent4j.actions;

import com.example.lagent4j.agent.ActionResult;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 工具接口
 * 定义了工具的基本操作
 */
public interface Action {
    /**
     * 获取工具名称
     */
    String getName();

    /**
     * 异步执行工具操作
     * @param parameters 操作参数
     * @return 执行结果
     */
    CompletableFuture<ActionResult> executeAsync(Map<String, Object> parameters);
}
