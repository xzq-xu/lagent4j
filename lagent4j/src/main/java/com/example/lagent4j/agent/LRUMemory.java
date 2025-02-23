package com.example.lagent4j.agent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LRUMemory implements Memory {
    private final int maxSize;
    private final Map<Integer, Deque<AgentMessage>> sessions = new ConcurrentHashMap<>();

    public LRUMemory(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public void addMessage(Integer sessionId, AgentMessage message) {
        sessions.compute(sessionId, (k, v) -> {
            if (v == null) {
                v = new ArrayDeque<>();
            }
            v.addLast(message);
            while (v.size() > maxSize) {
                v.removeFirst();
            }
            return v;
        });
    }

    @Override
    public Deque<AgentMessage> getMessages(Integer sessionId) {
        return sessions.getOrDefault(sessionId, new ArrayDeque<>());
    }

    @Override
    public void clear(Integer sessionId) {
        sessions.remove(sessionId);
    }
}
