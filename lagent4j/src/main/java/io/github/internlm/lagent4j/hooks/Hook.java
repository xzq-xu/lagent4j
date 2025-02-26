package io.github.internlm.lagent4j.hooks;

import io.github.internlm.lagent4j.agents.Agent;
import io.github.internlm.lagent4j.schema.AgentMessage;

import java.util.List;

/**
 * 钩子接口
 * <p>
 * 定义了代理处理过程中的钩子函数
 */
public interface Hook {
    /**
     * 代理处理前的钩子
     *
     * @param agent     代理
     * @param messages  消息列表
     * @param sessionId 会话ID
     * @return 处理后的消息列表，如果返回null则使用原始消息
     */
    default List<AgentMessage> beforeAgent(Agent agent, List<AgentMessage> messages, int sessionId) {
        return null;
    }
    
    /**
     * 代理处理后的钩子
     *
     * @param agent     代理
     * @param message   响应消息
     * @param sessionId 会话ID
     * @return 处理后的响应消息，如果返回null则使用原始响应
     */
    default AgentMessage afterAgent(Agent agent, AgentMessage message, int sessionId) {
        return null;
    }
} 