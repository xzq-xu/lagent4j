package io.github.internlm.lagent4j.hooks;

import io.github.internlm.lagent4j.agents.Agent;
import io.github.internlm.lagent4j.schema.AgentMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 钩子管理器
 * <p>
 * 用于管理代理的钩子
 */
public class HookManager {
    /**
     * 钩子列表
     */
    private final List<Hook> hooks;
    
    /**
     * 钩子ID生成器
     */
    private final AtomicInteger hookIdGenerator;
    
    /**
     * 创建一个钩子管理器
     */
    public HookManager() {
        this.hooks = new CopyOnWriteArrayList<>();
        this.hookIdGenerator = new AtomicInteger(0);
    }
    
    /**
     * 注册钩子
     *
     * @param hook 钩子
     * @return 钩子ID
     */
    public int registerHook(Hook hook) {
        if (hook == null) {
            throw new IllegalArgumentException("Hook cannot be null");
        }
        
        int hookId = hookIdGenerator.incrementAndGet();
        hooks.add(hook);
        return hookId;
    }
    
    /**
     * 移除钩子
     *
     * @param hookId 钩子ID
     * @return 是否成功移除
     */
    public boolean removeHook(int hookId) {
        if (hookId <= 0 || hookId > hooks.size()) {
            return false;
        }
        
        return hooks.remove(hookId - 1) != null;
    }
    
    /**
     * 清空所有钩子
     */
    public void clearHooks() {
        hooks.clear();
    }
    
    /**
     * 执行代理处理前的钩子
     *
     * @param agent     代理
     * @param messages  消息列表
     * @param sessionId 会话ID
     * @return 处理后的消息列表
     */
    public List<AgentMessage> executeBeforeAgent(Agent agent, List<AgentMessage> messages, int sessionId) {
        List<AgentMessage> result = new ArrayList<>(messages);
        
        for (Hook hook : hooks) {
            List<AgentMessage> hookResult = hook.beforeAgent(agent, result, sessionId);
            if (hookResult != null) {
                result = hookResult;
            }
        }
        
        return result;
    }
    
    /**
     * 执行代理处理后的钩子
     *
     * @param agent     代理
     * @param message   响应消息
     * @param sessionId 会话ID
     * @return 处理后的响应消息
     */
    public AgentMessage executeAfterAgent(Agent agent, AgentMessage message, int sessionId) {
        AgentMessage result = message;
        
        for (Hook hook : hooks) {
            AgentMessage hookResult = hook.afterAgent(agent, result, sessionId);
            if (hookResult != null) {
                result = hookResult;
            }
        }
        
        return result;
    }
    
    /**
     * 获取钩子数量
     *
     * @return 钩子数量
     */
    public int getHookCount() {
        return hooks.size();
    }
} 