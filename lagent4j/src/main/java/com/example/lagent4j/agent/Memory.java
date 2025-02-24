package com.example.lagent4j.agent;

import java.util.List;

public interface Memory {
    /**
     * 获取指定会话的消息历史
     */
    List<AgentMessage> get(long sessionId);

    /**
     * 添加消息到指定会话
     */
    void add(long sessionId, AgentMessage message);

    /**
     * 清除指定会话的消息历史
     */
    void clear(long sessionId);
}

