package com.example.lagent4j.agent;

import java.util.Deque;

public interface Memory {
    void addMessage(Integer sessionId, AgentMessage message);
    Deque<AgentMessage> getMessages(Integer sessionId);
    void clear(Integer sessionId);
}

